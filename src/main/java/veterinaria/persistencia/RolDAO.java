
package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Rol;

public class RolDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public List<Rol> obtenerRoles() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Rol> query = em.createQuery("SELECT r FROM Rol r", Rol.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public Rol obtenerPorId(int idRol) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Rol.class, idRol);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
