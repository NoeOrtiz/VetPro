package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.HistoriaEvento;

public class HistoriaEventoDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void crear(HistoriaEvento evento) {
        if (evento == null) return;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(evento);
            em.getTransaction().commit();
        } finally {
            if (em != null) em.close();
        }
    }

    public HistoriaEvento buscarPorId(Integer idEvento) {
        if (idEvento == null) return null;
        EntityManager em = getEntityManager();
        try {
            return em.find(HistoriaEvento.class, idEvento);
        } finally {
            if (em != null) em.close();
        }
    }

    public List<HistoriaEvento> listarPorMascota(Integer idMascota) {
        if (idMascota == null) return null;
        EntityManager em = getEntityManager();
        try {
            TypedQuery<HistoriaEvento> q = em.createQuery(
                    "SELECT h FROM HistoriaEvento h WHERE h.mascota.idMascota = :idMascota ORDER BY h.fecha DESC, h.idEvento DESC",
                    HistoriaEvento.class
            );
            q.setParameter("idMascota", idMascota);
            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean existePorReferencia(String refTabla, Integer refId) {
        if (refTabla == null || refTabla.trim().isEmpty() || refId == null) return false;
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(h) FROM HistoriaEvento h WHERE h.refTabla = :refTabla AND h.refId = :refId",
                    Long.class
            );
            q.setParameter("refTabla", refTabla);
            q.setParameter("refId", refId);
            Long c = q.getSingleResult();
            return c != null && c > 0;
        } finally {
            if (em != null) em.close();
        }
    }

}
