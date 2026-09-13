package veterinaria.persistencia;

import java.time.LocalDateTime; // <-- Cambiado a LocalDateTime
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import veterinaria.entidad.Auditoria;

public class AuditoriaDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManagerFactory().createEntityManager();
    }

    public void guardar(Auditoria a) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(a);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    // Cambiado el parámetro a LocalDateTime
    public int purgarAntiguos(LocalDateTime fechaLimite) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            int deleted = em.createQuery("DELETE FROM Auditoria a WHERE a.fechaHora < :limite")
                    .setParameter("limite", fechaLimite)
                    .executeUpdate();
            em.getTransaction().commit();
            return deleted;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    // Cambiados los parámetros "desde" y "hasta" a LocalDateTime
    public List<Auditoria> buscar(LocalDateTime desde, LocalDateTime hasta, Integer usuarioId, String accion, String entidad) {
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Auditoria> cq = cb.createQuery(Auditoria.class);
            Root<Auditoria> root = cq.from(Auditoria.class);

            Join<Auditoria, ?> usuarioJoin = root.join("usuario", JoinType.LEFT);
            root.fetch("usuario", JoinType.LEFT);
            cq.distinct(true);

            List<Predicate> preds = new ArrayList<>();

            if (desde != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
            }
            if (hasta != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("fechaHora"), hasta));
            }
            if (usuarioId != null) {
                preds.add(cb.equal(usuarioJoin.get("idUsuario"), usuarioId));
            }
            if (accion != null && !accion.trim().isEmpty()) {
                preds.add(cb.equal(root.get("accion"), accion.trim()));
            }
            if (entidad != null && !entidad.trim().isEmpty()) {
                preds.add(cb.equal(root.get("entidad"), entidad.trim()));
            }

            cq.select(root);
            if (!preds.isEmpty()) {
                cq.where(cb.and(preds.toArray(new Predicate[0])));
            }
            cq.orderBy(cb.desc(root.get("fechaHora")), cb.desc(root.get("idAuditoria")));

            TypedQuery<Auditoria> q = em.createQuery(cq);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Auditoria obtenerPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Auditoria> q = em.createQuery(
                    "SELECT a FROM Auditoria a LEFT JOIN FETCH a.usuario u WHERE a.idAuditoria = :id",
                    Auditoria.class
            );
            q.setParameter("id", id);
            List<Auditoria> res = q.getResultList();
            return (res == null || res.isEmpty()) ? null : res.get(0);
        } finally {
            em.close();
        }
    }
}
