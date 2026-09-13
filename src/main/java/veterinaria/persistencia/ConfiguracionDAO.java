package veterinaria.persistencia;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;
import veterinaria.entidad.Configuracion;

public class ConfiguracionDAO {

    private EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public Configuracion buscarPorClave(String clave) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Configuracion> q = em.createQuery(
                    "SELECT c FROM Configuracion c WHERE c.clave = :clave",
                    Configuracion.class
            );
            q.setParameter("clave", clave);
            return q.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        } finally {
            if (em != null) em.close();
        }
    }

    public List<Configuracion> listarTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Configuracion> q = em.createQuery(
                    "SELECT c FROM Configuracion c",
                    Configuracion.class
            );
            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean upsert(String clave, String valor, String descripcion) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Configuracion cfg = null;
            try {
                cfg = em.createQuery(
                        "SELECT c FROM Configuracion c WHERE c.clave = :clave",
                        Configuracion.class
                ).setParameter("clave", clave).getSingleResult();
            } catch (NoResultException ignore) {
            }

            if (cfg == null) {
                cfg = new Configuracion(null, clave, valor, descripcion);
                em.persist(cfg);
            } else {
                cfg.setValor(valor);
                cfg.setDescripcion(descripcion);
                em.merge(cfg);
            }

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            if (em != null) em.close();
        }
    }
}
