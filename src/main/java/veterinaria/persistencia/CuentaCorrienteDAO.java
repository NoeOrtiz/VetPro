package veterinaria.persistencia;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.servicio.CuentaCorrienteService;

public class CuentaCorrienteDAO {

    private final CuentaCorrienteService cuentaCorrienteService = new CuentaCorrienteService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(CuentaCorriente cuentaCorriente) {
        try {
            return cuentaCorrienteService.crearCuentaCorriente(cuentaCorriente);
        } catch (Exception e) {
            System.out.println("Error al crear CuentaCorriente: " + e.getMessage());
            return false;
        }
    }

    public CuentaCorriente buscarPorId(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorriente> query = em.createQuery(
                    "SELECT cc FROM CuentaCorriente cc "
                    + "JOIN FETCH cc.cliente c "
                    + "JOIN FETCH c.persona p "
                    + "WHERE cc.idCuentaCorriente = :id",
                    CuentaCorriente.class
            );
            query.setParameter("id", idCuentaCorriente);
            return query.getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CuentaCorriente buscarPorIdCliente(Integer idCliente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorriente> query = em.createQuery(
                    "SELECT c FROM CuentaCorriente c WHERE c.cliente.idCliente = :idCliente",
                    CuentaCorriente.class
            );
            query.setParameter("idCliente", idCliente);
            return query.getSingleResult();
        } catch (NoResultException e) {
            System.out.println("No se encontró ninguna CuentaCorriente con idCliente: " + idCliente);
            return null;
        } catch (Exception e) {
            System.out.println("Error al buscar CuentaCorriente por idCliente: " + e.getMessage());
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CuentaCorriente buscarActivaPorIdCliente(Integer idCliente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorriente> query = em.createQuery(
                    "SELECT c FROM CuentaCorriente c WHERE c.cliente.idCliente = :idCliente AND UPPER(c.estado) IN ('ACTIVO','ACTIVA')",
                    CuentaCorriente.class
            );
            query.setParameter("idCliente", idCliente);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CuentaCorriente> buscarTodas() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT cc FROM CuentaCorriente cc "
                    + "JOIN FETCH cc.cliente c "
                    + "JOIN FETCH c.persona p "
                    + "ORDER BY p.apellido, p.nombre",
                    CuentaCorriente.class
            ).getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean actualizar(CuentaCorriente cuentaCorriente) {
        try {
            return cuentaCorrienteService.actualizarCuentaCorriente(cuentaCorriente);
        } catch (Exception e) {
            System.out.println("Error al actualizar Cuenta Corriente: " + e.getMessage());
            return false;
        }
    }

    public boolean desactivar(CuentaCorriente cuentaCorriente) {
        try {
            return cuentaCorrienteService.desactivarCuentaCorriente(cuentaCorriente.getIdCuentaCorriente());
        } catch (Exception e) {
            System.out.println("Error al desactivar CuentaCorriente: " + e.getMessage());
            return false;
        }
    }

    public List<CuentaCorriente> buscarDeudores() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorriente> query = em.createQuery(
                    "SELECT c FROM CuentaCorriente c WHERE c.saldoActual < 0", CuentaCorriente.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
