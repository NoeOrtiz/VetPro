
package veterinaria.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.EstadoPeluqueriaEnum;
import veterinaria.entidad.Peluqueria;
import veterinaria.entidad.PeluqueriaHistorial;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.PeluqueriaDAO;
import veterinaria.persistencia.AgendaSlotDAO;
import veterinaria.persistencia.JPAUtil;

public class PeluqueriaService {

    private final AgendaSlotDAO slotDAO = new AgendaSlotDAO();
    private final PeluqueriaDAO turnoDAO = new PeluqueriaDAO();

    public List<Peluqueria> obtenerTurnosPorFechaYEstados(LocalDate fecha, List<EstadoPeluqueriaEnum> estados) {
        return turnoDAO.buscarPorFechaYEstados(fecha, estados);
    }

    public List<Peluqueria> obtenerTurnosPorEstados(List<EstadoPeluqueriaEnum> estados) {
        return turnoDAO.buscarPorEstados(estados);
    }

    private boolean isConstraintViolation(Throwable t) {
        Throwable c = t;
        while (c != null) {
            String cn = c.getClass().getName();
            if (cn != null) {
                String s = cn.toLowerCase();
                if (s.contains("constraintviolation")
                        || s.contains("integrityconstraint")
                        || s.contains("sqlintegrityconstraint")
                        || s.contains("rollbackexception")) {
                    return true;
                }
            }
            c = c.getCause();
        }
        return false;
    }

    public boolean reservarTurno(Peluqueria turno, Long idSlot) {
        if (turno == null || idSlot == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            AgendaSlot slot = slotDAO.findByIdForUpdate(em, idSlot);
            if (slot == null) {
                em.getTransaction().rollback();
                return false;
            }

            if (slot.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE) {
                em.getTransaction().rollback();
                return false;
            }

            slot.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
            em.merge(slot);

            turno.setSlot(slot);
            turno.setFecha(slot.getFecha());
            turno.setHora(slot.getHoraInicio());
            turno.setVeterinario(slot.getVeterinario());

            em.persist(turno);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();

            if (isConstraintViolation(e)) {
                return false;
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean actualizarTurnoReasignandoSlot(Peluqueria turno, Long idNuevoSlot) {
        if (turno == null || turno.getIdTurno() == null || idNuevoSlot == null) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, turno.getIdTurno());
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
            }

            AgendaSlot nuevo = slotDAO.findByIdForUpdate(em, idNuevoSlot);
            if (nuevo == null || nuevo.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE) {
                em.getTransaction().rollback();
                return false;
            }
            nuevo.setEstadoSlot(AgendaSlot.EstadoSlot.RESERVADO);
            em.merge(nuevo);

            managed.setSlot(nuevo);
            managed.setFecha(nuevo.getFecha());
            managed.setHora(nuevo.getHoraInicio());
            managed.setVeterinario(nuevo.getVeterinario());

            managed.setMascota(turno.getMascota());
            managed.setTipoDeCita(turno.getTipoDeCita());
            managed.setPresupuesto(turno.getPresupuesto());
            managed.setUsuarioGestion(turno.getUsuarioGestion());

            em.merge(managed);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (isConstraintViolation(e)) {
                return false;
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean cancelarTurno(Integer idTurno) {
        return cancelarTurno(idTurno, "", null);
    }

    public boolean confirmarTurno(Integer idTurno) {
        if (idTurno == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, idTurno);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            EstadoPeluqueriaEnum estadoActual = managed.getEstado();
            if (estadoActual == EstadoPeluqueriaEnum.ELIMINADO || estadoActual == EstadoPeluqueriaEnum.CANCELADO) {
                em.getTransaction().rollback();
                return false;
            }

            managed.setEstado(EstadoPeluqueriaEnum.CONFIRMADO);
            em.merge(managed);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (isConstraintViolation(e)) {
                return false;
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean cancelarTurnoSinHistorial(Integer idTurno) {
        if (idTurno == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, idTurno);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            EstadoPeluqueriaEnum estadoActual = managed.getEstado();
            if (estadoActual == EstadoPeluqueriaEnum.ELIMINADO) {
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

            managed.setEstado(EstadoPeluqueriaEnum.CANCELADO);
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

    public boolean completarTurno(Integer idTurno, Integer idUsuario) {
        if (idTurno == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, idTurno);
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            EstadoPeluqueriaEnum estadoActual = managed.getEstado();
            if (estadoActual != EstadoPeluqueriaEnum.CONFIRMADO) {
                em.getTransaction().rollback();
                return false;
            }

            managed.setEstado(EstadoPeluqueriaEnum.COMPLETADO);
            em.merge(managed);

            Usuario u = (idUsuario != null) ? em.find(Usuario.class, idUsuario) : null;
            registrarHistorial(em, managed, "COMPLETADO", u, "");

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private void registrarHistorial(EntityManager em, Peluqueria turno, String evento, Usuario usuario, String motivo) {
        try {
            PeluqueriaHistorial h = new PeluqueriaHistorial();
            h.setTurno(turno);
            h.setEvento(evento);
            h.setUsuario(usuario);
            h.setMotivo(motivo != null ? motivo.trim() : "");
            h.setFechaEvento(LocalDateTime.now());
            em.persist(h);
        } catch (Exception ignore) {
        }
    }

    public boolean cancelarTurno(Integer idTurno, String motivo, Integer idUsuario) {
        if (idTurno == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, idTurno);
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

            managed.setEstado(EstadoPeluqueriaEnum.CANCELADO);
            em.merge(managed);

            Usuario u = (idUsuario != null) ? em.find(Usuario.class, idUsuario) : null;

            registrarHistorial(em, managed, "CANCELADO", u, motivo);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean eliminarTurno(Integer idTurno, String motivo, Integer idUsuario) {
        if (idTurno == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, idTurno);
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

            managed.setEstado(EstadoPeluqueriaEnum.ELIMINADO);
            em.merge(managed);

            Usuario u = (idUsuario != null) ? em.find(Usuario.class, idUsuario) : null;

            registrarHistorial(em, managed, "ELIMINADO", u, motivo);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

}
