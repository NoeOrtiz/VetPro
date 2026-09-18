
package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Proveedor;
import veterinaria.servicio.ProveedorService;

public class ProveedorDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(Persona persona, Proveedor proveedor) throws Exception {
        return new ProveedorService().crear(persona, proveedor);
    }
    
    public List<Proveedor> buscarTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Proveedor> query = em.createQuery("SELECT u FROM Proveedor u", Proveedor.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public Proveedor buscarPorId(Integer idProveedor){
        EntityManager em = getEntityManager();
        try {
            return em.find(Proveedor.class, idProveedor);
        } finally {
            if (em != null) {
                em.close();
            }
        }        
    }    
    
    public boolean actualizar(Persona persona, Proveedor proveedor) throws Exception {
        return new ProveedorService().actualizar(persona, proveedor);
    }
    
    public boolean eliminar(Proveedor proveedor) throws Exception {
        return new ProveedorService().desactivar(proveedor);
    }

    public boolean desactivar(Proveedor proveedor) throws Exception {
        return new ProveedorService().desactivar(proveedor);
    }

    public boolean reactivar(Proveedor proveedor) throws Exception {
        return new ProveedorService().reactivar(proveedor);
    }
}
