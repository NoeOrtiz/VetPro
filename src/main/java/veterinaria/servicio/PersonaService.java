package veterinaria.servicio;

import javax.persistence.EntityManager;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.Usuario;

public class PersonaService {

    public Persona persistOrAttach(EntityManager em, Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser null");
        }

        if (persona.getIdPersona() == null) {
            em.persist(persona);
            return persona; // queda managed
        }

        Persona managed = em.find(Persona.class, persona.getIdPersona());
        if (managed == null) {
            throw new IllegalArgumentException("La persona con id " + persona.getIdPersona() + " no existe");
        }
        return managed;
    }

    public Persona merge(EntityManager em, Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser null");
        }
        if (persona.getIdPersona() == null) {
            em.persist(persona);
            return persona;
        }
        return em.merge(persona);
    }

    public void deleteIfUnused(EntityManager em, Integer idPersona) {
        if (idPersona == null) return;

        em.flush();

        long usuarios = countByPersona(em, Usuario.class, idPersona);
        long clientes = countByPersona(em, Cliente.class, idPersona);
        long proveedores = countByPersona(em, Proveedor.class, idPersona);

        if (usuarios == 0 && clientes == 0 && proveedores == 0) {
            Persona p = em.find(Persona.class, idPersona);
            if (p != null) {
                em.remove(p);
            }
        }
    }

    private long countByPersona(EntityManager em, Class<?> entityClass, Integer idPersona) {
        String entityName = entityClass.getSimpleName();
        Long cnt = em.createQuery(
                "SELECT COUNT(e) FROM " + entityName + " e WHERE e.persona.idPersona = :idPersona", Long.class)
                .setParameter("idPersona", idPersona)
                .getSingleResult();
        return cnt == null ? 0 : cnt.longValue();
    }
}
