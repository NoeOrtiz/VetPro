package veterinaria.persistencia;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Laboratorio;
import veterinaria.util.AppLog;

public class LaboratorioDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public Laboratorio buscarPorSlotId(EntityManager em, Long idSlot) {
        if (em == null || idSlot == null) return null;
        try {
            return em.createQuery(
                    "SELECT l FROM Laboratorio l WHERE l.slot IS NOT NULL AND l.slot.idSlot = :id",
                    Laboratorio.class
            ).setParameter("id", idSlot)
             .setMaxResults(1)
             .getResultStream()
             .findFirst()
             .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public Laboratorio buscarPorSlotId(Long idSlot) {
        EntityManager em = getEntityManager();
        try {
            return buscarPorSlotId(em, idSlot);
        } finally {
            em.close();
        }
    }

    public boolean guardar(Laboratorio laboratorio) {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            em.persist(laboratorio);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            AppLog.error(LaboratorioDAO.class, "Error al guardar laboratorio", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean actualizar(Laboratorio laboratorio) {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            em.merge(laboratorio);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            AppLog.error(LaboratorioDAO.class, "Error al actualizar laboratorio id=" + (laboratorio != null ? laboratorio.getIdLaboratorio() : null), e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean eliminar(Laboratorio laboratorio) throws Exception {
        EntityManager em = getEntityManager();
        boolean state = false;
        try {
            em.getTransaction().begin();
            Laboratorio laboratorioManaged = em.find(Laboratorio.class, laboratorio.getIdLaboratorio());
            if (laboratorioManaged != null) {
                String estado = laboratorioManaged.getEstado();
                if ("Procesado".equals(estado)) {
                    em.remove(laboratorioManaged);
                    em.getTransaction().commit();
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

    public Laboratorio buscarPorId(Integer idLaboratorio) {
        EntityManager em = getEntityManager();
        try {
            if (idLaboratorio == null) return null;

            List<Laboratorio> res = em.createQuery(
                    "SELECT l FROM Laboratorio l "
                    + " JOIN FETCH l.cliente c "
                    + " JOIN FETCH c.persona cp "
                    + " JOIN FETCH l.mascota m "
                    + " LEFT JOIN FETCH m.cliente mc "
                    + " LEFT JOIN FETCH mc.persona mcp "
                    + " LEFT JOIN FETCH l.slot s "
                    + " LEFT JOIN FETCH s.veterinario sv "
                    + " LEFT JOIN FETCH sv.persona svp "
                    + " LEFT JOIN FETCH l.veterinario v "
                    + " LEFT JOIN FETCH v.persona vp "
                    + " WHERE l.idLaboratorio = :id",
                    Laboratorio.class
            ).setParameter("id", idLaboratorio).getResultList();

            return res.isEmpty() ? null : res.get(0);
        } catch (Exception e) {
            try {
                return em.find(Laboratorio.class, idLaboratorio);
            } catch (Exception ignore) {
                return null;
            }
        } finally {
            em.close();
        }
    }

    public List<Laboratorio> obtenerTodos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT l FROM Laboratorio l", Laboratorio.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Laboratorio> obtenerPorUsuarioGestion(String usuarioGestion) {
        EntityManager em = getEntityManager();
        try {
            if (usuarioGestion == null || usuarioGestion.trim().isEmpty()) {
                return java.util.Collections.emptyList();
            }
            return em.createQuery(
                    "SELECT l FROM Laboratorio l WHERE l.usuarioGestion = :ug ORDER BY l.idLaboratorio DESC",
                    Laboratorio.class
            ).setParameter("ug", usuarioGestion).getResultList();
        } finally {
            em.close();
        }
    }

    public long contarPorFechaExtraccion(LocalDate fecha) {
        EntityManager em = getEntityManager();
        try {
            if (fecha == null) return 0L;
            Long c = em.createQuery(
                    "SELECT COUNT(l) FROM Laboratorio l WHERE l.fechaExtraccion = :fecha",
                    Long.class
            ).setParameter("fecha", fecha).getSingleResult();
            return c != null ? c : 0L;
        } catch (Exception e) {
            return 0L;
        } finally {
            em.close();
        }
    }
}
