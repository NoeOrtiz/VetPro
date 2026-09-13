package veterinaria.persistencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.EstadoPeluqueriaEnum;
import veterinaria.entidad.Peluqueria;

public class PeluqueriaDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(Peluqueria turno) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(turno);
            em.getTransaction().commit();
            return true;
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
    }

    public boolean existeTurnoEnFechaYHora(LocalDate fecha, LocalTime hora, int minutosRango) {
        EntityManager em = getEntityManager();
        try {
            if (minutosRango < 0) minutosRango = 0;
            LocalTime horaInicio = hora.minusMinutes(minutosRango);
            LocalTime horaFin = hora.plusMinutes(minutosRango);

            String consulta = "SELECT COUNT(t) FROM Peluqueria t WHERE t.fecha = :fecha AND t.hora BETWEEN :horaInicio AND :horaFin";
            Long count = em.createQuery(consulta, Long.class)
                    .setParameter("fecha", fecha)
                    .setParameter("horaInicio", horaInicio)
                    .setParameter("horaFin", horaFin)
                    .getSingleResult();

            return count > 0;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<LocalTime> obtenerHorasPorFecha(LocalDate fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<LocalTime> query = em.createQuery(
                    "SELECT t.hora FROM Peluqueria t WHERE t.fecha = :fecha ORDER BY t.hora",
                    LocalTime.class
            );
            query.setParameter("fecha", fecha);
            return query.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public long contarPorFecha(LocalDate fecha) {
        EntityManager em = getEntityManager();
        try {
            String consulta = "SELECT COUNT(t) FROM Peluqueria t WHERE t.fecha = :fecha";
            Long count = em.createQuery(consulta, Long.class)
                    .setParameter("fecha", fecha)
                    .getSingleResult();
            return count == null ? 0L : count;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public long contarPorFechaYFranja(LocalDate fecha, LocalTime desdeInclusive, LocalTime hastaExclusive) {
        EntityManager em = getEntityManager();
        try {
            if (desdeInclusive == null) desdeInclusive = LocalTime.MIN;
            if (hastaExclusive == null) hastaExclusive = LocalTime.MAX;
            String consulta = "SELECT COUNT(t) FROM Peluqueria t WHERE t.fecha = :fecha AND t.hora >= :desde AND t.hora < :hasta";
            Long count = em.createQuery(consulta, Long.class)
                    .setParameter("fecha", fecha)
                    .setParameter("desde", desdeInclusive)
                    .setParameter("hasta", hastaExclusive)
                    .getSingleResult();
            return count == null ? 0L : count;
        } finally {
            if (em != null) em.close();
        }
    }

    public List<Peluqueria> buscarTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Peluqueria> query = em.createQuery(
                    "SELECT DISTINCT t FROM Peluqueria t "
                    + " LEFT JOIN FETCH t.veterinario v "
                    + " LEFT JOIN FETCH v.persona vp "
                    + " LEFT JOIN FETCH t.mascota m "
                    + " LEFT JOIN FETCH m.cliente c "
                    + " LEFT JOIN FETCH c.persona cp "
                    + " LEFT JOIN FETCH t.slot s "
                    + " ORDER BY t.fecha DESC, t.hora DESC",
                    Peluqueria.class
            );
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Peluqueria> buscarPorFechaYEstados(LocalDate fecha, List<EstadoPeluqueriaEnum> estados) {
        EntityManager em = getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT t FROM Peluqueria t "
                    + " LEFT JOIN FETCH t.veterinario v "
                    + " LEFT JOIN FETCH v.persona vp "
                    + " LEFT JOIN FETCH t.mascota m "
                    + " LEFT JOIN FETCH m.cliente c "
                    + " LEFT JOIN FETCH c.persona cp "
                    + " LEFT JOIN FETCH t.slot s "
                    + " WHERE t.fecha = :fecha ");

            boolean filtrarEstados = estados != null && !estados.isEmpty();
            if (filtrarEstados) {
                jpql.append(" AND t.estado IN :estados ");
            }

            jpql.append(" ORDER BY t.fecha DESC, t.hora DESC ");

            TypedQuery<Peluqueria> query = em.createQuery(jpql.toString(), Peluqueria.class);
            query.setParameter("fecha", fecha);
            if (filtrarEstados) {
                query.setParameter("estados", estados);
            }
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Peluqueria> buscarPorEstados(List<EstadoPeluqueriaEnum> estados) {
        EntityManager em = getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT t FROM Peluqueria t "
                    + " LEFT JOIN FETCH t.veterinario v "
                    + " LEFT JOIN FETCH v.persona vp "
                    + " LEFT JOIN FETCH t.mascota m "
                    + " LEFT JOIN FETCH m.cliente c "
                    + " LEFT JOIN FETCH c.persona cp "
                    + " LEFT JOIN FETCH t.slot s ");

            boolean filtrarEstados = estados != null && !estados.isEmpty();
            if (filtrarEstados) {
                jpql.append(" WHERE t.estado IN :estados ");
            }

            jpql.append(" ORDER BY t.fecha DESC, t.hora DESC ");

            TypedQuery<Peluqueria> query = em.createQuery(jpql.toString(), Peluqueria.class);
            if (filtrarEstados) {
                query.setParameter("estados", estados);
            }
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Peluqueria buscarPorId(Long idTurno) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Peluqueria> q = em.createQuery(
                    "SELECT t FROM Peluqueria t "
                    + " LEFT JOIN FETCH t.veterinario v "
                    + " LEFT JOIN FETCH v.persona vp "
                    + " LEFT JOIN FETCH t.mascota m "
                    + " LEFT JOIN FETCH m.cliente c "
                    + " LEFT JOIN FETCH c.persona cp "
                    + " LEFT JOIN FETCH t.slot s "
                    + " WHERE t.idTurno = :id",
                    Peluqueria.class
            );
            q.setParameter("id", idTurno != null ? idTurno.intValue() : null);
            List<Peluqueria> res = q.getResultList();
            return res.isEmpty() ? null : res.get(0);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean actualizar(Peluqueria turno) throws Exception {
        boolean retornar = false;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(turno);
            em.getTransaction().commit();
            retornar = true;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return retornar;
    }

    public boolean eliminar(Peluqueria turno) throws Exception {
        EntityManager em = getEntityManager();
        boolean state = false;
        try {
            em.getTransaction().begin();
            Peluqueria turnoManaged = em.find(Peluqueria.class, turno.getIdTurno());
            if (turnoManaged != null) {
                EstadoPeluqueriaEnum estado = turnoManaged.getEstado();
                if (estado == EstadoPeluqueriaEnum.PENDIENTE || estado == EstadoPeluqueriaEnum.CANCELADO) {
                    liberarSlotSiCorresponde(em, turnoManaged);
                    em.remove(turnoManaged);
                    em.getTransaction().commit();
                    state = true;
                } else {
                    em.getTransaction().rollback();
                    System.out.println("No se puede eliminar un turno en estado: " + (estado != null ? estado.getLabel() : ""));
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

    public boolean eliminarPorId(Long idTurno) throws Exception {
        if (idTurno == null) {
            return false;
        }

        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Peluqueria managed = em.find(Peluqueria.class, idTurno.intValue());
            if (managed == null) {
                em.getTransaction().rollback();
                return false;
            }

            EstadoPeluqueriaEnum estado = managed.getEstado();
            if (!(estado == EstadoPeluqueriaEnum.PENDIENTE || estado == EstadoPeluqueriaEnum.CANCELADO)) {
                em.getTransaction().rollback();
                return false;
            }

            liberarSlotSiCorresponde(em, managed);
            em.remove(managed);
            em.getTransaction().commit();
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private void liberarSlotSiCorresponde(EntityManager em, Peluqueria turnoManaged) {
        if (em == null || turnoManaged == null || turnoManaged.getSlot() == null) {
            return;
        }
        AgendaSlot slot = em.find(AgendaSlot.class, turnoManaged.getSlot().getIdSlot());
        if (slot != null) {
            slot.setEstadoSlot(AgendaSlot.EstadoSlot.LIBRE);
            em.merge(slot);
        }
        turnoManaged.setSlot(null);
        em.merge(turnoManaged);
    }

    public List<Peluqueria> buscarVisibles() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT DISTINCT t FROM Peluqueria t " +
                          "LEFT JOIN FETCH t.veterinario v " +
                          "LEFT JOIN FETCH v.persona " +
                          "LEFT JOIN FETCH t.mascota m " +
                          "LEFT JOIN FETCH m.cliente c " +
                          "LEFT JOIN FETCH c.persona " +
                          "LEFT JOIN FETCH t.slot s " +
                          "WHERE (t.estado IS NULL OR (t.estado <> :cancelado AND t.estado <> :eliminado)) " +
                          "ORDER BY t.fecha DESC, t.hora DESC";
            return em.createQuery(jpql, Peluqueria.class)
                    .setParameter("cancelado", EstadoPeluqueriaEnum.CANCELADO)
                    .setParameter("eliminado", EstadoPeluqueriaEnum.ELIMINADO)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
