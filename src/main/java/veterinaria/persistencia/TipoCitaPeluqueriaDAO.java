package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.TipoCitaPeluqueria;

public class TipoCitaPeluqueriaDAO {

    private EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public long countAll() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery("SELECT COUNT(t) FROM TipoCitaPeluqueria t", Long.class);
            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<TipoCitaPeluqueria> findActivosOrderByDescripcion() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<TipoCitaPeluqueria> q = em.createQuery(
                    "SELECT t FROM TipoCitaPeluqueria t WHERE t.activo = true ORDER BY t.orden, t.descripcion",
                    TipoCitaPeluqueria.class
            );
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public List<TipoCitaPeluqueria> findAllOrderByDescripcion() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<TipoCitaPeluqueria> q = em.createQuery(
                    "SELECT t FROM TipoCitaPeluqueria t ORDER BY t.orden, t.descripcion",
                    TipoCitaPeluqueria.class
            );
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public TipoCitaPeluqueria findByDescripcion(String descripcion) {
        if (descripcion == null) return null;
        EntityManager em = getEntityManager();
        try {
            TypedQuery<TipoCitaPeluqueria> q = em.createQuery(
                    "SELECT t FROM TipoCitaPeluqueria t WHERE t.descripcion = :d",
                    TipoCitaPeluqueria.class
            );
            q.setParameter("d", descripcion.trim());
            return q.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        } finally {
            em.close();
        }
    }

    public TipoCitaPeluqueria findById(Integer id) {
        if (id == null) return null;
        EntityManager em = getEntityManager();
        try {
            return em.find(TipoCitaPeluqueria.class, id);
        } finally {
            em.close();
        }
    }

    public TipoCitaPeluqueria save(TipoCitaPeluqueria tipo) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(tipo);
            em.getTransaction().commit();
            return tipo;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public TipoCitaPeluqueria update(TipoCitaPeluqueria tipo) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            TipoCitaPeluqueria merged = em.merge(tipo);
            em.getTransaction().commit();
            return merged;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public boolean crear(TipoCitaPeluqueria tipo) {
        save(tipo);
        return true;
    }

    public boolean actualizar(TipoCitaPeluqueria tipo) {
        update(tipo);
        return true;
    }

    public boolean eliminarLogico(Integer id) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            TipoCitaPeluqueria t = em.find(TipoCitaPeluqueria.class, id);
            if (t == null) {
                em.getTransaction().rollback();
                return false;
            }
            t.setActivo(false);
            em.merge(t);
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public List<TipoCitaPeluqueria> buscarActivos() {
        return findActivosOrderByDescripcion();
    }

    public List<TipoCitaPeluqueria> buscarTodos() {
        return findAllOrderByDescripcion();
    }

    public TipoCitaPeluqueria buscarPorDescripcion(String d) {
        return findByDescripcion(d);
    }

    public TipoCitaPeluqueria buscarPorId(Integer id) {
        return findById(id);
    }

    public boolean swapOrden(Integer idA, Integer idB) {
        if (idA == null || idB == null) return false;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            TipoCitaPeluqueria a = em.find(TipoCitaPeluqueria.class, idA);
            TipoCitaPeluqueria b = em.find(TipoCitaPeluqueria.class, idB);
            if (a == null || b == null) {
                em.getTransaction().rollback();
                return false;
            }
            Integer oa = a.getOrden() == null ? 0 : a.getOrden();
            Integer ob = b.getOrden() == null ? 0 : b.getOrden();
            a.setOrden(ob);
            b.setOrden(oa);
            em.merge(a);
            em.merge(b);
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public boolean setOrden(Integer id, Integer orden) {
        if (id == null) return false;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            TipoCitaPeluqueria t = em.find(TipoCitaPeluqueria.class, id);
            if (t == null) {
                em.getTransaction().rollback();
                return false;
            }
            t.setOrden(orden);
            em.merge(t);
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }
}
