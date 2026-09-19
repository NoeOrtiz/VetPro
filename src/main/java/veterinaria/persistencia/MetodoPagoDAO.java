
package veterinaria.persistencia;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.MetodoPago;

public class MetodoPagoDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }
    
    public boolean crear(MetodoPago metodoPago) {
        EntityManager em = getEntityManager();
        boolean retornar = false;
        try {
            em.getTransaction().begin();
            em.persist(metodoPago);
            em.getTransaction().commit();
            retornar = true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Error al crear un nuevo Metodo de Pago: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return retornar;
    }
    
    public MetodoPago buscarPorId(Integer idMetodoPago) {
        EntityManager em = getEntityManager();
        try {
            return em.find(MetodoPago.class, idMetodoPago);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }    

    public List<MetodoPago> buscarTodos() {
        EntityManager em = getEntityManager();
        List<MetodoPago> listaMetodoPago = new ArrayList<>();
        try {
            TypedQuery<MetodoPago> query = em.createQuery("SELECT u FROM MetodoPago u", MetodoPago.class);
            listaMetodoPago = query.getResultList();
        } catch (Exception e) {
            System.out.println("Error al buscar todas las cuentas corrientes: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return listaMetodoPago;
    }
    
    public MetodoPago buscarPorNombre(String nombre) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<MetodoPago> query = em.createQuery("SELECT m FROM MetodoPago m WHERE m.nombre = :nombre", MetodoPago.class);
            query.setParameter("nombre", nombre);
            return query.getSingleResult();
        } catch (Exception e) {
            System.out.println("Error al buscar el método de pago por nombre: " + e.getMessage());
            return null; // Retorna null si no encuentra ningún resultado
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean actualizar(MetodoPago metodoPago) {
        EntityManager em = getEntityManager();
        boolean retornar = false;
        try {
            em.getTransaction().begin();
            em.merge(metodoPago);
            em.getTransaction().commit();
            retornar = true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Error al actualizar Metodo de Pago: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return retornar;
    }

    /** @deprecated Los metodos de pago historicos no se eliminan. */
    @Deprecated
    public boolean eliminarPorId(Integer idMetodoPago) {
        return desactivar(idMetodoPago);
    }

    public boolean desactivar(Integer idMetodoPago) {
        return cambiarEstado(idMetodoPago, false);
    }

    public boolean reactivar(Integer idMetodoPago) {
        return cambiarEstado(idMetodoPago, true);
    }

    private boolean cambiarEstado(Integer idMetodoPago, boolean activo) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            MetodoPago mp = em.find(MetodoPago.class, idMetodoPago);
            if (mp == null) {
                em.getTransaction().rollback();
                return false;
            }
            mp.setActivo(activo);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            if (em != null) em.close();
        }
    }

    public List<MetodoPago> buscarActivos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT m FROM MetodoPago m WHERE m.activo = true ORDER BY m.nombre",
                    MetodoPago.class).getResultList();
        } finally {
            if (em != null) em.close();
        }
    }
}
