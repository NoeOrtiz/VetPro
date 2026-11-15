package veterinaria.persistencia;

import javax.persistence.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.config.PersistenceConfig;
import veterinaria.entidad.Visita;
import veterinaria.servicio.AuditoriaService;

public class VisitaDAO {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void crear(Visita visita) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            if (visita == null || visita.getMascota() == null || visita.getMascota().getIdMascota() == null)
                throw new IllegalArgumentException("Debe seleccionar un paciente.");
            veterinaria.entidad.Mascota paciente = em.find(
                    veterinaria.entidad.Mascota.class, visita.getMascota().getIdMascota(),
                    LockModeType.PESSIMISTIC_WRITE);
            if (paciente == null) throw new IllegalStateException("No se encontró el paciente.");
            if (!paciente.isActivo()) {
                veterinaria.util.SesionUsuario sesion =
                        veterinaria.vista.application.Application.getSesionUsuario();
                String nombreRol = sesion == null || sesion.getRol() == null ? ""
                        : sesion.getRol().getNombreRol();
                if (!"Veterinario".equalsIgnoreCase(nombreRol)
                        && !"Administrador".equalsIgnoreCase(nombreRol))
                    throw new SecurityException("La consulta urgente provisional requiere un veterinario o administrador.");
                if (!visita.isAtencionProvisional() || visita.getIdSolicitudReactivacion() == null)
                    throw new IllegalStateException("El paciente está inactivo. Requiere atención provisional autorizada.");
                Number n = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM mascota_solicitud_reactivacion "
                  + "WHERE idSolicitud=? AND idMascota=? AND prioridad='URGENTE' AND estado='PENDIENTE'")
                    .setParameter(1, visita.getIdSolicitudReactivacion())
                    .setParameter(2, paciente.getIdMascota()).getSingleResult();
                if (n.longValue() != 1)
                    throw new IllegalStateException("La solicitud urgente ya no está pendiente.");
            } else if (visita.isAtencionProvisional()) {
                throw new IllegalStateException("La atención provisional solo corresponde a pacientes inactivos.");
            }
            em.persist(visita);
            em.getTransaction().commit();
            auditoriaService.registrar(visita.isAtencionProvisional() ? "CONSULTA_PROVISIONAL" : "CREATE",
                    "Visita", (visita.getIdVisita() == null ? null : Long.valueOf(visita.getIdVisita())),
                    "VisitaDAO", visita.isAtencionProvisional()
                        ? "Atención urgente provisional; solicitud #"+visita.getIdSolicitudReactivacion()
                        : "Alta de visita",
                    AuditoriaService.RESULT_OK, null, null, null);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    /** Cambio de estado atómico, sin editar datos clínicos ni crear otra visita. */
    public Visita avanzarEstado(Integer idVisita, String esperado, String nuevoEstado) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Visita managed = em.find(Visita.class, idVisita, LockModeType.PESSIMISTIC_WRITE);
            if (managed == null) throw new IllegalArgumentException("No se encontró la consulta.");
            String actual = managed.getEstado() == null ? "" : managed.getEstado().trim().toUpperCase();
            if (!actual.equals(esperado))
                throw new IllegalStateException("El estado de la consulta cambió. Actualice la lista.");
            if (!(("EN_ESPERA".equals(actual) && "ATENDIENDO".equals(nuevoEstado))
                    || ("ATENDIENDO".equals(actual) && "FINALIZADO".equals(nuevoEstado))))
                throw new IllegalStateException("Solo se puede avanzar al siguiente estado de atención.");
            managed.setEstado(nuevoEstado);
            em.flush();
            em.getTransaction().commit();
            auditoriaService.registrar("CAMBIAR_ESTADO", "Visita", Long.valueOf(idVisita),
                    "VisitaDAO", "Consulta: " + actual + " → " + nuevoEstado,
                    AuditoriaService.RESULT_OK, null, null, null);
            return managed;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public Visita buscarPorId(Integer idVisita) {
        EntityManager em = getEntityManager();
        Visita visita = em.find(Visita.class, idVisita);
        em.close();
        return visita;
    }

    public List<Visita> obtenerTodas() {

        EntityManager em = getEntityManager();

        List<Visita> visitas = em.createNativeQuery(
                "SELECT * FROM visita",
                Visita.class
        ).getResultList();

        System.out.println(PersistenceConfig.describeSafe());
        System.out.println("Cantidad visitas: " + visitas.size());

        for (Visita v : visitas) {
            System.out.println("ID=" + v.getIdVisita()
                    + " Estado=" + v.getEstado());
        }

        em.close();

        return visitas;
    }

    public void actualizar(Visita visita) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.merge(visita);
        em.getTransaction().commit();

        auditoriaService.registrar("UPDATE", "Visita", (visita != null ? Long.valueOf(visita.getIdVisita()) : null), "VisitaDAO", "Actualización de visita", AuditoriaService.RESULT_OK, null, null, null);
        em.close();
    }

    /** @deprecated Las visitas clínicas son históricas y nunca se eliminan físicamente. */
    @Deprecated
    public void eliminar(Integer idVisita) {
        if (idVisita == null) return;
        Visita visita = buscarPorId(idVisita);
        if (visita == null) return;
        try {
            cancelar(visita);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cancelar la visita.", ex);
        }
    }

    public boolean eliminar(Visita visita) throws Exception {
        EntityManager em = getEntityManager();
        boolean state = false;
        try {
            em.getTransaction().begin();
            Visita visitaManaged = em.find(Visita.class, visita.getIdVisita());
            if (visitaManaged != null) {
                String estado = (visitaManaged.getEstado() != null) ? visitaManaged.getEstado().trim().toUpperCase() : "";
                if (!"FINALIZADO".equals(estado) && !"CANCELADO".equals(estado)) {
                    visitaManaged.setEstado("CANCELADO");
                    em.merge(visitaManaged);
                    em.getTransaction().commit();

                    auditoriaService.registrar("CANCELAR", "Visita", (visita != null ? Long.valueOf(visita.getIdVisita()) : null), "VisitaDAO", "Cancelación lógica de visita", AuditoriaService.RESULT_OK, null, null, null);
                    state = true;
                } else {
                    em.getTransaction().rollback();
                }
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return state;
    }

    public boolean cancelar(Visita visita) throws Exception {
        if (visita == null || visita.getIdVisita() == null) {
            return false;
        }
        EntityManager em = getEntityManager();
        boolean ok = false;
        try {
            em.getTransaction().begin();
            Visita managed = em.find(Visita.class, visita.getIdVisita());
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            String estado = (managed.getEstado() != null) ? managed.getEstado().trim().toUpperCase() : "";
            if ("FINALIZADO".equals(estado) || "CANCELADO".equals(estado)) {
                em.getTransaction().rollback();
                return false;
            }

            managed.setEstado("CANCELADO");
            em.merge(managed);
            em.getTransaction().commit();

            auditoriaService.registrar("UPDATE", "Visita", (visita != null ? Long.valueOf(visita.getIdVisita()) : null), "VisitaDAO", "Cancelación de visita", AuditoriaService.RESULT_OK, null, null, null);
            ok = true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return ok;
    }

    public long contarEnEspera() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(v) FROM Visita v WHERE v.estado = :estado", Long.class)
                    .setParameter("estado", "EN_ESPERA")
                    .getSingleResult();
        } finally {
            if (em != null) em.close();
        }
    }

    public List<Visita> obtenerVisitasActivas() {

        EntityManager em = getEntityManager();

        try {

            return em.createQuery(
                    "SELECT v FROM Visita v "
                    + "WHERE v.estado IN ('EN_ESPERA', 'ATENDIENDO') "
                    + "ORDER BY v.fecha ASC, v.hora ASC",
                    Visita.class)
                    .getResultList();

        } finally {
            em.close();
        }
    }

}
