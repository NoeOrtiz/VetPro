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
        em.getTransaction().begin();
        em.persist(visita);
        em.getTransaction().commit();
        auditoriaService.registrar("CREATE", "Visita", (visita != null ? Long.valueOf(visita.getIdVisita()) : null), "VisitaDAO", "Alta de visita", AuditoriaService.RESULT_OK, null, null, null);
        em.close();
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

    public List<Visita> obtenerVisitasActivas() {

        EntityManager em = getEntityManager();

        try {

            return em.createQuery(
                    "SELECT v FROM Visita v "
                    + "WHERE v.estado = 'ATENDIENDO' "
                    + "ORDER BY v.fecha DESC, v.hora DESC",
                    Visita.class)
                    .getResultList();

        } finally {
            em.close();
        }
    }

}
