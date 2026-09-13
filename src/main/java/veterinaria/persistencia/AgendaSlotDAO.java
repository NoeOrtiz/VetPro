package veterinaria.persistencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.Usuario;

public class AgendaSlotDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public long contarPorFechaYVeterinario(LocalDate fecha, Usuario vet) {
        EntityManager em = getEntityManager();
        try {
            Long c = em.createQuery(
                    "SELECT COUNT(s) FROM AgendaSlot s WHERE s.fecha = :f AND s.veterinario = :v",
                    Long.class
            ).setParameter("f", fecha)
             .setParameter("v", vet)
             .getSingleResult();
            return c == null ? 0L : c;
        } finally {
            em.close();
        }
    }

    public List<AgendaSlot> listarPorFechaYVeterinario(LocalDate fecha, Usuario vet) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<AgendaSlot> q = em.createQuery(
                    "SELECT s FROM AgendaSlot s WHERE s.fecha = :f AND s.veterinario = :v ORDER BY s.horaInicio",
                    AgendaSlot.class
            );
            q.setParameter("f", fecha);
            q.setParameter("v", vet);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public List<AgendaSlot> listarLibresPorFechaYVeterinario(LocalDate fecha, Usuario vet) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<AgendaSlot> q = em.createQuery(
                    "SELECT s FROM AgendaSlot s WHERE s.fecha = :f AND s.veterinario = :v AND s.estadoSlot = :e " +
                    "AND s.idSlot NOT IN (SELECT t.slot.idSlot FROM Peluqueria t WHERE t.slot IS NOT NULL) " +
                    "AND s.idSlot NOT IN (SELECT l.slot.idSlot FROM Laboratorio l WHERE l.slot IS NOT NULL) " +
                    "AND s.idSlot NOT IN (SELECT h.slot.idSlot FROM Hospitalizacion h WHERE h.slot IS NOT NULL) " +
                    "ORDER BY s.horaInicio",
                    AgendaSlot.class
            );
            q.setParameter("f", fecha);
            q.setParameter("v", vet);
            q.setParameter("e", AgendaSlot.EstadoSlot.LIBRE);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public AgendaSlot findByIdForUpdate(EntityManager em, Long idSlot) {
        return em.find(AgendaSlot.class, idSlot, LockModeType.PESSIMISTIC_WRITE);
    }

    public AgendaSlot findSlotQueContieneHoraForUpdate(EntityManager em, LocalDate fecha, Usuario vet, LocalTime h) {
        if (em == null || fecha == null || vet == null || h == null) return null;
        TypedQuery<AgendaSlot> q = em.createQuery(
                "SELECT s FROM AgendaSlot s WHERE s.fecha = :f AND s.veterinario = :v "
                        + "AND :h >= s.horaInicio AND :h < s.horaFin ORDER BY s.horaInicio",
                AgendaSlot.class
        );
        q.setParameter("f", fecha);
        q.setParameter("v", vet);
        q.setParameter("h", h);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<AgendaSlot> res = q.setMaxResults(1).getResultList();
        return (res == null || res.isEmpty()) ? null : res.get(0);
    }

    public int reconciliarEstadosParaFechaYVeterinario(EntityManager em, LocalDate fecha, Usuario vet) {
        if (em == null || fecha == null || vet == null) {
            return 0;
        }

        int total = 0;

        total += em.createQuery(
                "UPDATE AgendaSlot s SET s.estadoSlot = :res "
                + "WHERE s.fecha = :f AND s.veterinario = :v "
                + "AND ("
                + "EXISTS (SELECT 1 FROM Peluqueria p WHERE p.slot = s) "
                + "OR EXISTS (SELECT 1 FROM Laboratorio l WHERE l.slot = s) "
                + "OR EXISTS (SELECT 1 FROM Hospitalizacion h WHERE h.slot = s)"
                + ") "
                + "AND s.estadoSlot <> :res"
        )
                .setParameter("res", AgendaSlot.EstadoSlot.RESERVADO)
                .setParameter("f", fecha)
                .setParameter("v", vet)
                .executeUpdate();

        total += em.createQuery(
                "UPDATE AgendaSlot s "
                + "SET s.estadoSlot = :lib "
                + "WHERE s.fecha = :f "
                + "AND s.veterinario = :v "
                + "AND s.estadoSlot = :res "
                + "AND NOT EXISTS (SELECT 1 FROM Peluqueria p WHERE p.slot = s) "
                + "AND NOT EXISTS (SELECT 1 FROM Laboratorio l WHERE l.slot = s) "
                + "AND NOT EXISTS (SELECT 1 FROM Hospitalizacion h WHERE h.slot = s)"
        )
                .setParameter("lib", AgendaSlot.EstadoSlot.LIBRE)
                .setParameter("res", AgendaSlot.EstadoSlot.RESERVADO)
                .setParameter("f", fecha)
                .setParameter("v", vet)
                .executeUpdate();

        return total;
    }

    public int reconciliarEstadosParaFechaYVeterinario(LocalDate fecha, Usuario vet) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            int c = reconciliarEstadosParaFechaYVeterinario(em, fecha, vet);
            em.getTransaction().commit();
            return c;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public long contarLibresPorFechaYVeterinario(LocalDate fecha, Usuario vet) {
        EntityManager em = getEntityManager();
        try {
            Long c = em.createQuery(
                    "SELECT COUNT(s) FROM AgendaSlot s WHERE s.fecha = :f AND s.veterinario = :v "
                    + "AND s.estadoSlot = :e "
                    + "AND NOT EXISTS (SELECT 1 FROM Peluqueria p WHERE p.slot = s) "
                    + "AND NOT EXISTS (SELECT 1 FROM Laboratorio l WHERE l.slot = s) "
                    + "AND NOT EXISTS (SELECT 1 FROM Hospitalizacion h WHERE h.slot = s)",
                    Long.class
            ).setParameter("f", fecha)
             .setParameter("v", vet)
             .setParameter("e", AgendaSlot.EstadoSlot.LIBRE)
             .getSingleResult();
            return c == null ? 0L : c;
        } finally {
            em.close();
        }
    }

    public List<LocalTime> listarHorasInicioPorFechaYVeterinario(EntityManager em, LocalDate fecha, Usuario vet) {
        if (em == null || fecha == null || vet == null) return java.util.Collections.emptyList();
        TypedQuery<LocalTime> q = em.createQuery(
                "SELECT s.horaInicio FROM AgendaSlot s WHERE s.fecha = :f AND s.veterinario = :v",
                LocalTime.class
        );
        q.setParameter("f", fecha);
        q.setParameter("v", vet);
        return q.getResultList();
    }

    public int eliminarLibresDesdeFecha(EntityManager em, LocalDate desdeInclusive) {
        if (em == null || desdeInclusive == null) return 0;

        return em.createQuery(
                "DELETE FROM AgendaSlot s "
                + "WHERE s.fecha >= :d "
                + "AND s.estadoSlot = :e "
                + "AND NOT EXISTS (SELECT 1 FROM Peluqueria p WHERE p.slot = s) "
                + "AND NOT EXISTS (SELECT 1 FROM Laboratorio l WHERE l.slot = s) "
                + "AND NOT EXISTS (SELECT 1 FROM Hospitalizacion h WHERE h.slot = s)"
        )
                .setParameter("d", desdeInclusive)
                .setParameter("e", AgendaSlot.EstadoSlot.LIBRE)
                .executeUpdate();
    }

    public int eliminarLibresDesdeFecha(LocalDate desdeInclusive) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            int c = eliminarLibresDesdeFecha(em, desdeInclusive);
            em.getTransaction().commit();
            return c;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
