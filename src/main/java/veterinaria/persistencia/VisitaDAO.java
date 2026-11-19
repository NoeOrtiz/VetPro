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
                // La recepción solo emite el ticket EN_ESPERA. La evaluación
                // clínica y los cambios de estado siguen siendo del veterinario.
                if (sesion == null || !sesion.puede("FormConsulta.NUEVO"))
                    throw new SecurityException("No tiene permiso para registrar tickets.");
                if (!"EMERGENCIA".equals(visita.getPrioridadAtencion())
                        && !"URGENCIA".equals(visita.getPrioridadAtencion()))
                    throw new IllegalStateException(
                            "La atención provisoria solo admite URGENCIA o EMERGENCIA.");
                if (!visita.isAtencionProvisional())
                    throw new IllegalStateException(
                            "El paciente está inactivo. Se requiere registro urgente provisorio.");
                // El paciente está bloqueado FOR UPDATE: entre instancias no se crean
                // solicitudes duplicadas ni se permite registrar un ticket sin vínculo.
                java.util.List<Object[]> pendientes = em.createNativeQuery(
                        "SELECT idSolicitud, prioridad FROM mascota_solicitud_reactivacion "
                        + "WHERE idMascota=? AND estado='PENDIENTE' "
                        + "ORDER BY fechaSolicitud DESC, idSolicitud DESC LIMIT 1 FOR UPDATE")
                        .setParameter(1, paciente.getIdMascota()).getResultList();
                Integer solicitudId;
                if (!pendientes.isEmpty()) {
                    solicitudId = ((Number) pendientes.get(0)[0]).intValue();
                    // Una solicitud normal existente se prioriza como urgente,
                    // preservando su identidad y su justificación original.
                    if (!"URGENTE".equalsIgnoreCase(String.valueOf(pendientes.get(0)[1]))) {
                        em.createNativeQuery(
                                "UPDATE mascota_solicitud_reactivacion "
                                + "SET prioridad='URGENTE' WHERE idSolicitud=?")
                                .setParameter(1, solicitudId).executeUpdate();
                    }
                } else {
                    veterinaria.util.SesionUsuario usuarioActual =
                            veterinaria.vista.application.Application.getSesionUsuario();
                    if (usuarioActual.getUsuario() == null
                            || usuarioActual.getUsuario().getIdUsuario() == null)
                        throw new SecurityException("La recepción debe tener un usuario identificado.");
                    String motivoBaja = paciente.getMotivoBaja();
                    String tipoSolicitud = "Fallecimiento".equalsIgnoreCase(
                            motivoBaja == null ? "" : motivoBaja.trim())
                            ? "RECTIFICACION" : "REACTIVACION";
                    String justificacion = "Solicitud urgente automática al registrar ticket "
                            + "de " + visita.getPrioridadAtencion() + ". Motivo: "
                            + (visita.getMotivoVisita() == null ? "Sin detallar"
                                    : visita.getMotivoVisita());
                    if (justificacion.length() > 1000)
                        justificacion = justificacion.substring(0, 1000);
                    em.createNativeQuery(
                            "INSERT INTO mascota_solicitud_reactivacion "
                            + "(idMascota,idUsuarioSolicitante,tipo,prioridad,justificacion,motivoBajaOriginal) "
                            + "VALUES (?,?,?,'URGENTE',?,?)")
                            .setParameter(1, paciente.getIdMascota())
                            .setParameter(2, usuarioActual.getUsuario().getIdUsuario())
                            .setParameter(3, tipoSolicitud)
                            .setParameter(4, justificacion)
                            .setParameter(5, motivoBaja)
                            .executeUpdate();
                    solicitudId = ((Number) em.createNativeQuery(
                            "SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
                }
                visita.setIdSolicitudReactivacion(solicitudId);
            } else if (visita.isAtencionProvisional()) {
                throw new IllegalStateException("La atención provisional solo corresponde a pacientes inactivos.");
            }
            if (visita.getIdConsultaOrigen() != null) {
                Visita origen = em.find(Visita.class, visita.getIdConsultaOrigen());
                if (origen == null || origen.getMascota() == null
                        || !paciente.getIdMascota().equals(origen.getMascota().getIdMascota())
                        || !"FINALIZADO".equalsIgnoreCase(origen.getEstado()))
                    throw new IllegalArgumentException(
                            "El seguimiento debe vincularse a una consulta finalizada del mismo paciente.");
            }
            if (!"EN_ESPERA".equals(visita.getEstado()))
                throw new IllegalStateException("La consulta debe registrarse inicialmente EN ESPERA.");
            if ((visita.getTratamiento() != null && !visita.getTratamiento().isBlank())
                    || (visita.getPatologia() != null && !visita.getPatologia().isBlank()))
                throw new IllegalStateException("El diagnóstico y tratamiento se registran durante la atención veterinaria.");
            if (visita.getPesoConsulta() != null
                    && visita.getPesoConsulta().signum() <= 0)
                throw new IllegalArgumentException("El peso debe ser mayor que cero.");
            visita.setEspecieConsulta(paciente.getEspecie());
            visita.setRazaConsulta(paciente.getRaza());
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

    /**
     * Asigna atómicamente la próxima consulta al profesional que se libera.
     * Emergencia > urgencia > control y, a igual prioridad, orden de llegada.
     * Nunca reserva un paciente mientras el profesional sigue atendiendo otro.
     */
    public Visita tomarSiguiente(Integer idUsuario) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            veterinaria.util.SesionUsuario sesionActual =
                    veterinaria.vista.application.Application.getSesionUsuario();
            if (idUsuario == null || sesionActual == null
                    || sesionActual.getUsuario() == null
                    || !idUsuario.equals(sesionActual.getUsuario().getIdUsuario())
                    || !sesionActual.puede("FormConsulta.EDITAR"))
                throw new SecurityException("No tiene autorización para llamar pacientes.");
            veterinaria.entidad.Usuario u = em.find(veterinaria.entidad.Usuario.class, idUsuario);
            if (u == null || !u.isActivo() || u.getRol() == null)
                throw new SecurityException("El profesional debe tener una sesión activa.");
            // Serializar asignaciones del mismo veterinario, incluso entre varias ventanas.
            em.createNativeQuery("SELECT idUsuario FROM usuario WHERE idUsuario=? FOR UPDATE")
                    .setParameter(1, idUsuario).getSingleResult();
            String rol = u.getRol().getNombreRol();
            if (!"Veterinario".equalsIgnoreCase(rol) && !"Administrador".equalsIgnoreCase(rol))
                throw new SecurityException("Solo el veterinario puede llamar al siguiente paciente.");
            String profesional = u.getPersona() != null
                    ? (String.valueOf(u.getPersona().getNombre()) + " "
                    + String.valueOf(u.getPersona().getApellido())).trim()
                    : u.getNombreUsuario();

            Number ocupadas = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM visita WHERE estado='ATENDIENDO' AND usuarioAtiende=?")
                    .setParameter(1, profesional).getSingleResult();
            if (ocupadas.longValue() > 0)
                throw new IllegalStateException("Todavía tiene una consulta en atención. Finalícela antes de llamar a otro paciente.");

            // Incluye tickets anteriores asignados pero nunca iniciados:
            // la asignación efectiva se realiza al comenzar la atención.
            java.util.List<?> ids = em.createNativeQuery(
                    "SELECT idVisita FROM visita WHERE estado='EN_ESPERA' "
                    + "ORDER BY CASE prioridad_atencion "
                    + "WHEN 'EMERGENCIA' THEN 0 WHEN 'URGENCIA' THEN 1 ELSE 2 END, "
                    + "fecha ASC, hora ASC, idVisita ASC LIMIT 1 FOR UPDATE")
                    .getResultList();
            if (ids.isEmpty()) {
                em.getTransaction().commit();
                return null;
            }
            Integer id = ((Number) ids.get(0)).intValue();
            Visita siguiente = em.find(Visita.class, id);
            if (siguiente == null || !"EN_ESPERA".equals(siguiente.getEstado()))
                throw new IllegalStateException("La cola cambió. Vuelva a intentar.");
            siguiente.setUsuarioAtiende(profesional);
            siguiente.setEstado("ATENDIENDO");
            em.flush();
            em.getTransaction().commit();
            auditoriaService.registrar("LLAMAR_PACIENTE", "Visita", Long.valueOf(id),
                    "VisitaDAO", "Turno asignado automáticamente a " + profesional,
                    AuditoriaService.RESULT_OK, null, null, null);
            return siguiente;
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
                    || ("ATENDIENDO".equals(actual) && ("FINALIZADO".equals(nuevoEstado)
                            || "OBSERVACION".equals(nuevoEstado)))
                    || ("OBSERVACION".equals(actual) && ("ATENDIENDO".equals(nuevoEstado)
                            || "FINALIZADO".equals(nuevoEstado)))))
                throw new IllegalStateException("Solo se puede avanzar al siguiente estado de atención.");
            veterinaria.util.SesionUsuario sesion =
                    veterinaria.vista.application.Application.getSesionUsuario();
            String rol = sesion == null || sesion.getRol() == null ? "" : sesion.getRol().getNombreRol();
            if (!"Veterinario".equalsIgnoreCase(rol) && !"Administrador".equalsIgnoreCase(rol))
                throw new SecurityException("Solo el veterinario puede iniciar o finalizar una atención.");
            if ("Veterinario".equalsIgnoreCase(rol) &&
                    (sesion.getNombreApellido() == null || managed.getUsuarioAtiende() == null ||
                     !sesion.getNombreApellido().trim().equalsIgnoreCase(managed.getUsuarioAtiende().trim())))
                throw new SecurityException("Esta consulta fue asignada a otro veterinario. Consulte al administrador.");
            if ("OBSERVACION".equals(actual) && "ATENDIENDO".equals(nuevoEstado)) {
                String profesional = managed.getUsuarioAtiende();
                Number ocupadas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM visita WHERE estado='ATENDIENDO' AND usuarioAtiende=?")
                        .setParameter(1, profesional).getSingleResult();
                if (ocupadas.longValue() > 0)
                    throw new IllegalStateException("Finalice o deje en observación la consulta actual antes de retomar esta.");
            }
            if ("FINALIZADO".equals(nuevoEstado)) {
                if (managed.getPatologia() == null || managed.getPatologia().isBlank()
                        || managed.getTratamiento() == null || managed.getTratamiento().isBlank())
                    throw new IllegalStateException("Antes de finalizar, el veterinario debe registrar diagnóstico y tratamiento.");
            }
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

        em.close();

        return visitas;
    }

    public void actualizar(Visita visita) {
        actualizar(visita, false);
    }

    /** Guarda evolución y, opcionalmente, pasa a observación en una sola transacción. */
    public void actualizar(Visita visita, boolean dejarEnObservacion) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            if (visita == null || visita.getIdVisita() == null)
                throw new IllegalArgumentException("Seleccione una consulta.");
            Visita actual = em.find(Visita.class, visita.getIdVisita(), LockModeType.PESSIMISTIC_WRITE);
            if (actual == null) throw new IllegalStateException("No se encontró la consulta.");
            String estado = actual.getEstado() == null ? "" : actual.getEstado().trim().toUpperCase();
            if ("FINALIZADO".equals(estado) || "CANCELADO".equals(estado))
                throw new IllegalStateException("Una consulta cerrada no puede modificarse.");
            if (!estado.equals(visita.getEstado()))
                throw new IllegalStateException("Para cambiar el estado use Cambiar estado.");
            if (!"ATENDIENDO".equals(estado))
                throw new IllegalStateException("Primero debe comenzar la atención veterinaria.");
            veterinaria.util.SesionUsuario sesion =
                    veterinaria.vista.application.Application.getSesionUsuario();
            String rol = sesion == null || sesion.getRol() == null ? "" : sesion.getRol().getNombreRol();
            if (!"Veterinario".equalsIgnoreCase(rol) && !"Administrador".equalsIgnoreCase(rol))
                throw new SecurityException("Solo el veterinario puede registrar información clínica.");
            if ("Veterinario".equalsIgnoreCase(rol) &&
                    (sesion.getNombreApellido() == null || actual.getUsuarioAtiende() == null ||
                     !sesion.getNombreApellido().trim().equalsIgnoreCase(actual.getUsuarioAtiende().trim())))
                throw new SecurityException("La consulta está asignada a otro veterinario. Consulte al administrador.");
            // Próximo control: solo el profesional que atiende puede recomendarlo.
            if (visita.getProximoControlRecomendado() != null
                    && visita.getProximoControlRecomendado().isBefore(java.time.LocalDate.now()))
                throw new IllegalArgumentException("El próximo control no puede tener fecha pasada.");
            actual.setProximoControlRecomendado(visita.getProximoControlRecomendado());
            // Actualizar exclusivamente los campos clínicos autorizados; mantener la identidad y asignación.
            actual.setPatologia(visita.getPatologia());
            actual.setTratamiento(visita.getTratamiento());
            // La medición de recepción queda histórica; solo el profesional puede corregirla.
            if (visita.getPesoConsulta() != null) {
                if (visita.getPesoConsulta().signum() <= 0)
                    throw new IllegalArgumentException("El peso de consulta debe ser mayor que cero.");
                actual.setPesoConsulta(visita.getPesoConsulta());
            }
            if (dejarEnObservacion) actual.setEstado("OBSERVACION");
            em.flush();
            em.getTransaction().commit();
            auditoriaService.registrar("UPDATE", "Visita", Long.valueOf(visita.getIdVisita()),
                    "VisitaDAO", dejarEnObservacion ? "Guardado clínico: en observación"
                            : "Modificación de tratamiento", AuditoriaService.RESULT_OK,
                    null, null, null);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
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
            Visita managed = em.find(Visita.class, visita.getIdVisita(),
                    LockModeType.PESSIMISTIC_WRITE);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            veterinaria.util.SesionUsuario sesionActual =
                    veterinaria.vista.application.Application.getSesionUsuario();
            if (sesionActual == null || !sesionActual.puede("FormConsulta.CANCELAR"))
                throw new SecurityException("No tiene permiso para cancelar consultas.");
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
                    + "WHERE v.estado IN ('EN_ESPERA', 'ATENDIENDO', 'OBSERVACION') "
                    + "ORDER BY v.fecha ASC, v.hora ASC",
                    Visita.class)
                    .getResultList();

        } finally {
            em.close();
        }
    }

}
