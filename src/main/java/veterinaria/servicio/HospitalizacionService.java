package veterinaria.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.HistoriaEvento;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.entidad.Procedimiento;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.AgendaSlotDAO;
import veterinaria.persistencia.HospitalizacionDAO;
import veterinaria.persistencia.JPAUtil;
import veterinaria.util.Constantes;
import veterinaria.util.enums.EstadoHospitalizacion;
import veterinaria.util.enums.EstadoProcedimiento;

public class HospitalizacionService {

    private final AgendaSlotDAO slotDAO = new AgendaSlotDAO();
    private final HospitalizacionDAO hospitalizacionDAO = new HospitalizacionDAO();

    private boolean existeOtroInternado(EntityManager em, Integer idMascota, Integer idExcluir) {
        if (em == null || idMascota == null) return false;
        Long c = em.createQuery(
                "SELECT COUNT(h) FROM Hospitalizacion h "
                + "WHERE h.mascota.idMascota = :idMascota "
                + "AND LOWER(COALESCE(h.estado, '')) = :estadoInternado "
                + "AND (:idExcluir IS NULL OR h.idHospitalizacion <> :idExcluir)",
                Long.class
        )
                .setParameter("idMascota", idMascota)
                .setParameter("estadoInternado", EstadoHospitalizacion.INTERNADO.getEtiqueta().toLowerCase())
                .setParameter("idExcluir", idExcluir)
                .getSingleResult();

        return c != null && c.longValue() > 0L;
    }

    public boolean registrarIngresoUrgencia(Hospitalizacion h) {
        if (h == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            if (h.getMascota() != null
                    && EstadoHospitalizacion.INTERNADO == EstadoHospitalizacion.fromEtiqueta(h.getEstado())
                    && existeOtroInternado(em, h.getMascota().getIdMascota(), null)) {
                em.getTransaction().rollback();
                return false;
            }

            h.setSlot(null);

            AgendaSlot slot = null;
            try {
                slot = slotDAO.findSlotQueContieneHoraForUpdate(em, h.getFechaIngreso(), h.getVeterinario(), h.getHora());
            } catch (Exception ignore) {
            }

            if (slot != null) {
                if (slot.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE) {
                    em.getTransaction().rollback();
                    return false;
                }
                Hospitalizacion existente = hospitalizacionDAO.buscarPorSlotId(em, slot.getIdSlot());
                if (existente != null) {
                    em.getTransaction().rollback();
                    return false;
                }
                slot.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
                em.merge(slot);

                h.setSlot(slot);
                h.setFechaIngreso(slot.getFecha());
                h.setHora(slot.getHoraInicio());
                h.setVeterinario(slot.getVeterinario());
            } else {
                java.time.LocalTime desde = h.getHora();
                java.time.LocalTime hasta = (desde != null) ? desde.plusMinutes(30) : null;
                if (desde != null && hasta != null && hasta.isAfter(desde)) {
                    AgendaSlot adhoc = new AgendaSlot(h.getFechaIngreso(), desde, hasta, h.getVeterinario(), AgendaSlot.EstadoSlot.RESERVADO);
                    em.persist(adhoc);
                    h.setSlot(adhoc);
                }
            }

            em.persist(h);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean reservarIngreso(Hospitalizacion h, Long idSlot) {
        if (h == null || idSlot == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            if (h.getMascota() != null
                    && EstadoHospitalizacion.INTERNADO == EstadoHospitalizacion.fromEtiqueta(h.getEstado())
                    && existeOtroInternado(em, h.getMascota().getIdMascota(), null)) {
                em.getTransaction().rollback();
                return false;
            }

            AgendaSlot slot = slotDAO.findByIdForUpdate(em, idSlot);
            if (slot == null || slot.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE) {
                em.getTransaction().rollback();
                return false;
            }
            Hospitalizacion existente = hospitalizacionDAO.buscarPorSlotId(em, idSlot);
            if (existente != null) {
                em.getTransaction().rollback();
                return false;
            }

            slot.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
            em.merge(slot);

            h.setSlot(slot);
            h.setFechaIngreso(slot.getFecha());
            h.setHora(slot.getHoraInicio());
            h.setVeterinario(slot.getVeterinario());

            em.persist(h);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean actualizarIngresoReasignandoSlot(Hospitalizacion h, Long idNuevoSlot) {
        if (h == null || h.getIdHospitalizacion() == null || idNuevoSlot == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Hospitalizacion managed = em.find(Hospitalizacion.class, h.getIdHospitalizacion());
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            if (managed.getSlot() != null) {
                AgendaSlot old = slotDAO.findByIdForUpdate(em, managed.getSlot().getIdSlot());
                if (old != null) {
                    old.setEstadoSlot(AgendaSlot.EstadoSlot.LIBRE);
                    em.merge(old);
                }
                managed.setSlot(null);
            }

            AgendaSlot nuevo = slotDAO.findByIdForUpdate(em, idNuevoSlot);
            if (nuevo == null || nuevo.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE) {
                em.getTransaction().rollback();
                return false;
            }
            Hospitalizacion existente = hospitalizacionDAO.buscarPorSlotId(em, idNuevoSlot);
            if (existente != null && !existingHospitalizacionEsLaMisma(existente, managed)) {
                em.getTransaction().rollback();
                return false;
            }
            nuevo.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
            em.merge(nuevo);

            managed.setSlot(nuevo);
            managed.setFechaIngreso(nuevo.getFecha());
            managed.setHora(nuevo.getHoraInicio());
            managed.setVeterinario(nuevo.getVeterinario());

            if (h.getMascota() != null) managed.setMascota(h.getMascota());
            if (h.getCliente() != null) managed.setCliente(h.getCliente());
            if (h.getFechaAlta() != null) managed.setFechaAlta(h.getFechaAlta());
            if (h.getMotivo() != null) managed.setMotivo(h.getMotivo());
            if (h.getDiagnostico() != null) managed.setDiagnostico(h.getDiagnostico());
            if (h.getTratamiento() != null) managed.setTratamiento(h.getTratamiento());
            if (h.getEstado() != null) managed.setEstado(h.getEstado());
            if (h.getUsuarioGestion() != null) managed.setUsuarioGestion(h.getUsuarioGestion());

            em.merge(managed);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private boolean existingHospitalizacionEsLaMisma(Hospitalizacion existente, Hospitalizacion managed) {
        if (existente == null || managed == null) return false;
        if (existente.getIdHospitalizacion() == null || managed.getIdHospitalizacion() == null) return false;
        return existente.getIdHospitalizacion().equals(managed.getIdHospitalizacion());
    }

    public boolean cancelarIngreso(Integer idHospitalizacion) {
        if (idHospitalizacion == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Hospitalizacion managed = em.find(Hospitalizacion.class, idHospitalizacion);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            if (managed.getSlot() != null) {
                AgendaSlot slot = slotDAO.findByIdForUpdate(em, managed.getSlot().getIdSlot());
                if (slot != null) {
                    slot.setEstadoSlot(AgendaSlot.EstadoSlot.LIBRE);
                    em.merge(slot);
                }
                managed.setSlot(null);
            }

            managed.setEstado(EstadoHospitalizacion.CANCELADO.getEtiqueta());
            em.merge(managed);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean registrarIngresoPendiente(Integer idHospitalizacion, String diagnostico, Integer idUsuarioGestion) {
        if (idHospitalizacion == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Hospitalizacion managed = em.find(Hospitalizacion.class, idHospitalizacion);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            if (managed.getEstado() != null && EstadoHospitalizacion.PENDIENTE
                    != EstadoHospitalizacion.fromEtiqueta(managed.getEstado())) {
                em.getTransaction().rollback();
                return false;
            }

            if (managed.getMascota() != null
                    && existeOtroInternado(em, managed.getMascota().getIdMascota(), managed.getIdHospitalizacion())) {
                em.getTransaction().rollback();
                return false;
            }

            managed.setDiagnostico(diagnostico);
            managed.setEstado(EstadoHospitalizacion.INTERNADO.getEtiqueta());
            if (idUsuarioGestion != null) {
                managed.setUsuarioGestion(em.getReference(veterinaria.entidad.Usuario.class, idUsuarioGestion));
            }

            em.merge(managed);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
public boolean registrarAltaMedica(Integer idHospitalizacion, LocalDateTime fechaHoraAlta, String tratamiento, Integer idUsuarioGestion) {
    if (idHospitalizacion == null) return false;

    EntityManager em = JPAUtil.getEntityManager();
    try {
        em.getTransaction().begin();

        Hospitalizacion managed = em.find(Hospitalizacion.class, idHospitalizacion);
        if (managed == null) {
            em.getTransaction().rollback();
            return false;
        }

        if (managed.getEstado() != null && EstadoHospitalizacion.INTERNADO
                != EstadoHospitalizacion.fromEtiqueta(managed.getEstado())) {
            em.getTransaction().rollback();
            return false;
        }

        if (managed.getSlot() != null) {
            AgendaSlot slot = slotDAO.findByIdForUpdate(em, managed.getSlot().getIdSlot());
            if (slot != null) {
                slot.setEstadoSlot(AgendaSlot.EstadoSlot.LIBRE);
                em.merge(slot);
            }
            managed.setSlot(null);
        }

        managed.setFechaAlta(fechaHoraAlta);
        managed.setTratamiento(tratamiento);
        managed.setEstado(EstadoHospitalizacion.ALTA.getEtiqueta());
        if (idUsuarioGestion != null) {
            managed.setUsuarioGestion(em.getReference(veterinaria.entidad.Usuario.class, idUsuarioGestion));
        }

        List<Procedimiento> procedimientos = obtenerProcedimientosPorHospitalizacion(em, managed.getIdHospitalizacion());
        if (procedimientos != null && !procedimientos.isEmpty()) {
            for (Procedimiento p : procedimientos) {
                if (p == null) continue;
                EstadoProcedimiento ep = EstadoProcedimiento.fromEtiqueta(p.getEstado());
                if (ep == EstadoProcedimiento.CANCELADO) {
                    continue;
                }
                p.setEstado(EstadoProcedimiento.DADO_DE_ALTA.getEtiqueta());
                em.merge(p);
            }
        }

        registrarHistoriaEventoHospitalizacionSiCorresponde(em, managed, procedimientos);

        em.merge(managed);
        em.getTransaction().commit();
        return true;
    } catch (Exception e) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        throw e;
    } finally {
        em.close();
    }
}

    private List<Procedimiento> obtenerProcedimientosPorHospitalizacion(EntityManager em, Integer idHospitalizacion) {
        if (em == null || idHospitalizacion == null) {
            return new ArrayList<>();
        }
        try {
            return em.createQuery(
                    "SELECT p FROM Procedimiento p WHERE p.hospitalizacion.idHospitalizacion = :idHosp",
                    Procedimiento.class
            )
                    .setParameter("idHosp", idHospitalizacion)
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void registrarHistoriaEventoHospitalizacionSiCorresponde(EntityManager em, Hospitalizacion h, List<Procedimiento> procedimientos) {
        if (em == null || h == null) {
            return;
        }
        if (h.getIdHospitalizacion() == null || h.getMascota() == null) {
            return;
        }

        String estado = safe(h.getEstado()).trim();
        boolean alta = estado.equalsIgnoreCase("ALTA")
                || estado.equalsIgnoreCase(safe(EstadoHospitalizacion.ALTA.getEtiqueta()).trim());
        if (!alta) {
            return;
        }

        HistoriaEvento existente = null;
        try {
            List<HistoriaEvento> list = em.createQuery(
                    "SELECT he FROM HistoriaEvento he WHERE he.refTabla = :refTabla AND he.refId = :refId",
                    HistoriaEvento.class
            )
                    .setParameter("refTabla", "hospitalizacion")
                    .setParameter("refId", h.getIdHospitalizacion())
                    .getResultList();
            if (list != null && !list.isEmpty()) {
                existente = list.get(0);
            }
        } catch (Exception ignore) {
        }

        String motivo = safe(h.getMotivo());
        String resumenBase = "Hospitalización";
        if (!motivo.isBlank()) {
            resumenBase = (resumenBase + " - " + motivo).trim();
        }

        String procResumen = construirResumenProcedimientos(procedimientos);
        String resumen = resumenBase;
        if (!procResumen.isBlank()) {
            resumen = (resumenBase + " | " + procResumen).trim();
        }

        StringBuilder obs = new StringBuilder();
        String diag = safe(h.getDiagnostico());
        String trat = safe(h.getTratamiento());
        if (!diag.isBlank()) {
            obs.append("Diagnóstico: ").append(diag).append("\n");
        }
        if (!trat.isBlank()) {
            obs.append("Tratamiento: ").append(trat).append("\n");
        }
        String procObs = construirDetalleProcedimientos(procedimientos);
        if (!procObs.isBlank()) {
            obs.append(procObs);
        }

        String profesional = obtenerNombreProfesional(h.getVeterinario());
        LocalDateTime fechaEvento = (h.getFechaAlta() != null) ? h.getFechaAlta() : LocalDateTime.now();

        try {
            if (existente == null) {
                HistoriaEvento evento = new HistoriaEvento(
                        h.getMascota(),
                        fechaEvento,
                        "HOSPITALIZACION",
                        profesional,
                        resumen,
                        obs.toString().trim(),
                        "hospitalizacion",
                        h.getIdHospitalizacion()
                );
                em.persist(evento);
            } else {
                existente.setFecha(fechaEvento);
                existente.setTipo("HOSPITALIZACION");
                if (!profesional.isBlank()) {
                    existente.setProfesional(profesional);
                }
                if (!resumen.isBlank()) {
                    existente.setResumen(resumen);
                }
                String obsStr = obs.toString().trim();
                if (!obsStr.isBlank()) {
                    existente.setObservaciones(obsStr);
                }
                em.merge(existente);
            }
        } catch (Exception ignore) {
        }
    }

    private String construirResumenProcedimientos(List<Procedimiento> procedimientos) {
        if (procedimientos == null || procedimientos.isEmpty()) {
            return "";
        }
        List<String> nombres = new ArrayList<>();
        for (Procedimiento p : procedimientos) {
            if (p == null) continue;
            String n = safe(p.getProcedimiento());
            if (!n.isBlank()) {
                nombres.add(n);
            }
        }
        if (nombres.isEmpty()) {
            return "";
        }
        if (nombres.size() == 1) {
            return "Procedimiento: " + nombres.get(0);
        }
        return "Procedimientos: " + String.join(", ", nombres);
    }

    private String construirDetalleProcedimientos(List<Procedimiento> procedimientos) {
        if (procedimientos == null || procedimientos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Procedimiento p : procedimientos) {
            if (p == null) continue;
            String n = safe(p.getProcedimiento());
            String m = safe(p.getMotivo());
            String cir = safe(p.getUsuarioAtiende());
            String an = safe(p.getAnestesiologo());
            String est = safe(p.getEstado());

            if (n.isBlank() && m.isBlank() && cir.isBlank() && an.isBlank() && est.isBlank()) {
                continue;
            }
            sb.append("Procedimiento: ").append(n.isBlank() ? "(sin especificar)" : n).append("\n");
            if (!m.isBlank()) sb.append("Motivo: ").append(m).append("\n");
            if (!cir.isBlank()) sb.append("Cirujano/Vet: ").append(cir).append("\n");
            if (!an.isBlank()) sb.append("Anestesiólogo: ").append(an).append("\n");
            if (!est.isBlank()) sb.append("Estado: ").append(est).append("\n");
            String ob = safe(p.getObservaciones());
            if (!ob.isBlank()) sb.append("Obs: ").append(ob).append("\n");
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String obtenerNombreProfesional(Usuario u) {
        if (u == null) return "";
        try {
            if (u.getPersona() != null) {
                String n = safe(u.getPersona().getNombre());
                String a = safe(u.getPersona().getApellido());
                String full = (n + " " + a).trim();
                if (!full.isBlank()) return full;
            }
        } catch (Exception ignore) {
        }
        return safe(u.getNombreUsuario());
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
