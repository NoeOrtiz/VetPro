
package veterinaria.persistencia;

import javax.persistence.EntityManager;
import veterinaria.entidad.Persona;

public class PersonaDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void crear(Persona persona) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(persona);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }    
    
    public void actualizar(Persona persona){
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(persona);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
            
    public Persona obtenerPorId(int idPersona) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Persona.class, idPersona);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public void eliminar(Integer idPersona) {
        System.out.println("Eliminando: Id:" + idPersona);
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Persona persona = em.find(Persona.class, idPersona);
            if (persona != null) {
                em.remove(persona);
                System.out.println("Persona eliminada correctamente");
            } else {
                System.out.println("La persona con Id " + idPersona + " no existe");
            }
            em.getTransaction().commit(); 
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
    }
}
