
package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.servicio.MascotaService;

public class MascotaDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(Mascota mascota) throws Exception {
        return new MascotaService().crear(mascota);
    }
    
    public Mascota buscarPorCliente(Cliente cliente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Mascota> query = em.createQuery(
                    "SELECT m FROM Mascota m "
                    + "LEFT JOIN FETCH m.cliente c "
                    + "LEFT JOIN FETCH c.persona "
                    + "WHERE m.cliente = :cliente",
                    Mascota.class
            );
            query.setParameter("cliente", cliente);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;  
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public List<Mascota> buscarTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Mascota> query = em.createQuery(
                    "SELECT DISTINCT m FROM Mascota m "
                    + "JOIN FETCH m.cliente c "
                    + "JOIN FETCH c.persona "
                    + "WHERE m.activo = true AND c.activo = true "
                    + "ORDER BY m.nombre",
                    Mascota.class
            );
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public Mascota buscarPorId(Integer idMascota){
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Mascota> query = em.createQuery(
                    "SELECT m FROM Mascota m "
                    + "LEFT JOIN FETCH m.cliente c "
                    + "LEFT JOIN FETCH c.persona "
                    + "WHERE m.idMascota = :id",
                    Mascota.class
            );
            query.setParameter("id", idMascota);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }        
    }
    
    public boolean actualizar(Mascota mascota) throws Exception {
        return new MascotaService().actualizar(mascota);
    }
    
    public boolean eliminar(Mascota mascota) throws Exception {
        return new MascotaService().desactivar(mascota);
    }
}
