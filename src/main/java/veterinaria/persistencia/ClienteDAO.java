package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.servicio.ClienteService;

public class ClienteDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(Persona persona, Cliente cliente) throws Exception {
        return new ClienteService().crear(persona, cliente);
    }

    public Cliente buscarPorId(Integer idCliente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> q = em.createQuery(
                    "SELECT c FROM Cliente c "
                    + "JOIN FETCH c.persona "
                    + "WHERE c.idCliente = :id",
                    Cliente.class
            );
            q.setParameter("id", idCliente);
            return q.getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Cliente buscarPorRazonSocial(String razonSocial) {
        if (razonSocial == null) {
            return null;
        }
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> q = em.createQuery(
                    "SELECT c FROM Cliente c JOIN FETCH c.persona WHERE LOWER(c.razonSocial) = LOWER(:rs)",
                    Cliente.class
            );
            q.setParameter("rs", razonSocial.trim());
            List<Cliente> res = q.setMaxResults(1).getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Cliente buscarPorEmail(String email) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery("SELECT c FROM Cliente c WHERE c.email = :email", Cliente.class);
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Cliente> buscarTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                    "SELECT c FROM Cliente c JOIN FETCH c.persona",
                    Cliente.class
            );
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    // 🌟 NUEVO MÉTODO: Filtrar solo los clientes que estén activos
    // Método para filtrar solo los clientes que estén activos
    public List<Cliente> buscarActivos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                    "SELECT c FROM Cliente c JOIN FETCH c.persona WHERE c.activo = true",
                    Cliente.class
            );
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Boolean actualizar(Persona persona, Cliente cliente) {
        return new ClienteService().actualizar(persona, cliente);
    }

    public boolean eliminar(Persona persona, Cliente cliente) throws Exception {
        return new ClienteService().eliminar(persona, cliente);
    }

    public List<Cliente> buscarClientesSinCuentaCorriente() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> q = em.createQuery(
                    "SELECT c FROM Cliente c "
                    + "JOIN FETCH c.persona p "
                    + "WHERE NOT EXISTS ("
                    + "   SELECT 1 FROM CuentaCorriente cc WHERE cc.cliente = c"
                    + ") "
                    + "ORDER BY p.apellido, p.nombre",
                    Cliente.class
            );
            return q.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Cliente> buscarPorEstado(String estadoFiltro) {
        EntityManager em = getEntityManager();
        try {
            String jpql = "SELECT c FROM Cliente c JOIN FETCH c.persona p";

            if ("ACTIVO".equalsIgnoreCase(estadoFiltro)) {
                jpql += " WHERE c.activo = true";
            } else if ("INACTIVO".equalsIgnoreCase(estadoFiltro)
                    || "ELIMINADO".equalsIgnoreCase(estadoFiltro)) {
                jpql += " WHERE c.activo = false";
            }
            // Para TODOS no se agrega condición.

            jpql += " ORDER BY p.apellido ASC, p.nombre ASC";

            return em.createQuery(jpql, Cliente.class).getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
