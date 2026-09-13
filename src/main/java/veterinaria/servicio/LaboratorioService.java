package veterinaria.servicio;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.HistoriaEvento;
import veterinaria.entidad.Laboratorio;
import veterinaria.persistencia.AgendaSlotDAO;
import veterinaria.persistencia.LaboratorioDAO;
import veterinaria.persistencia.JPAUtil;
import veterinaria.util.Constantes;
import veterinaria.util.enums.EstadoLaboratorio;

public class LaboratorioService {

    private final AgendaSlotDAO slotDAO = new AgendaSlotDAO();
    private final LaboratorioDAO laboratorioDAO = new LaboratorioDAO();

    public boolean reservarExtraccion(Laboratorio lab, Long idSlot) {
        if (lab == null || idSlot == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            AgendaSlot slot = slotDAO.findByIdForUpdate(em, idSlot);
            if (slot == null || slot.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE) {
                em.getTransaction().rollback();
                return false;
            }

            

Laboratorio existente = laboratorioDAO.buscarPorSlotId(em, idSlot);
if (existente != null) {
    em.getTransaction().rollback();
    return false;
}
slot.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
            em.merge(slot);

            lab.setSlot(slot);
            lab.setFechaExtraccion(slot.getFecha());
            lab.setHoraExtraccion(slot.getHoraInicio());
            lab.setVeterinario(slot.getVeterinario());

            if (lab.getEstado() == null || lab.getEstado().isBlank()) {
                lab.setEstado(EstadoLaboratorio.PENDIENTE.getEtiqueta());
            }

            em.persist(lab);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean actualizarExtraccionReasignandoSlot(Laboratorio lab, Long idNuevoSlot) {
        if (lab == null || lab.getIdLaboratorio() == null || idNuevoSlot == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Laboratorio managed = em.find(Laboratorio.class, lab.getIdLaboratorio());
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
            nuevo.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
            em.merge(nuevo);

            managed.setSlot(nuevo);
            managed.setFechaExtraccion(nuevo.getFecha());
            managed.setHoraExtraccion(nuevo.getHoraInicio());
            managed.setVeterinario(nuevo.getVeterinario());

            managed.setMascota(lab.getMascota());
            managed.setCliente(lab.getCliente());
            managed.setMotivoExtraccion(lab.getMotivoExtraccion());
            managed.setTipoAnalisis(lab.getTipoAnalisis());
            managed.setDiagnostico(lab.getDiagnostico());
            managed.setUsuarioGestion(lab.getUsuarioGestion());

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

    public boolean cancelarExtraccion(Integer idLaboratorio) {
        if (idLaboratorio == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Laboratorio managed = em.find(Laboratorio.class, idLaboratorio);
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

            managed.setEstado(EstadoLaboratorio.CANCELADO.getEtiqueta());
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

    public boolean completarExtraccion(Integer idLaboratorio, String informeFinal, String usuarioAtiende) {
        if (idLaboratorio == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Laboratorio managed = em.find(Laboratorio.class, idLaboratorio);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            if (informeFinal != null) {
                managed.setDiagnostico(informeFinal);
            }
            if (usuarioAtiende != null && !usuarioAtiende.isBlank()) {
                managed.setUsuarioAtiende(usuarioAtiende);
            }

            if (managed.getSlot() != null) {
                AgendaSlot slot = slotDAO.findByIdForUpdate(em, managed.getSlot().getIdSlot());
                if (slot != null) {
                    slot.setEstadoSlot(AgendaSlot.EstadoSlot.LIBRE);
                    em.merge(slot);
                }
                managed.setSlot(null);
            }

            managed.setEstado(EstadoLaboratorio.COMPLETADO.getEtiqueta());
            em.merge(managed);

            registrarHistoriaEventoLaboratorioSiCorresponde(em, managed);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private void registrarHistoriaEventoLaboratorioSiCorresponde(EntityManager em, Laboratorio lab) {
        if (em == null || lab == null) {
            return;
        }
        if (lab.getIdLaboratorio() == null) {
            return;
        }
        if (lab.getMascota() == null) {
            return;
        }

        String estado = safe(lab.getEstado()).trim();
        boolean completado
                = estado.equalsIgnoreCase("COMPLETADO")
                || estado.equalsIgnoreCase(safe(EstadoLaboratorio.COMPLETADO.getEtiqueta()).trim());

        if (!completado) {
            return;
        }

        try {
            Long c = em.createQuery(
                    "SELECT COUNT(h) FROM HistoriaEvento h WHERE h.refTabla = :refTabla AND h.refId = :refId",
                    Long.class
            )
                    .setParameter("refTabla", "laboratorio")
                    .setParameter("refId", lab.getIdLaboratorio())
                    .getSingleResult();

            if (c != null && c > 0) {
                return;
            }
        } catch (Exception ignore) {
        }

        LocalDateTime dt = LocalDateTime.now();
        String tipoAnalisis = safe(lab.getTipoAnalisis());
        String motivo = safe(lab.getMotivoExtraccion());
        String resumen = (tipoAnalisis + " - " + motivo).trim();
        if (resumen.isEmpty() || resumen.isBlank()) {
            resumen = "Laboratorio";
        }

        HistoriaEvento evento = new HistoriaEvento(
                lab.getMascota(),
                dt,
                "LABORATORIO",
                safe(lab.getUsuarioAtiende()),
                resumen,
                safe(lab.getDiagnostico()),
                "laboratorio",
                lab.getIdLaboratorio()
        );

        try {
            em.persist(evento);
        } catch (Exception e) {
        }
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
