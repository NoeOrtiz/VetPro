package veterinaria.persistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CuentaCorrienteProveedor;
import veterinaria.entidad.Proveedor;

public class CuentaCorrienteProveedorDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public CuentaCorrienteProveedor buscarPorIdProveedor(Integer idProveedor) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorrienteProveedor> q = em.createQuery(
                    "SELECT c FROM CuentaCorrienteProveedor c WHERE c.proveedor.idProveedor = :id",
                    CuentaCorrienteProveedor.class
            );
            q.setParameter("id", idProveedor);
            List<CuentaCorrienteProveedor> res = q.getResultList();
            return res.isEmpty() ? null : res.get(0);
        } finally {
            if (em != null) em.close();
        }
    }

    /** Cuentas activas con deuda pendiente, para la pantalla de pagos a proveedores. */
    public List<CuentaCorrienteProveedor> listarConDeuda() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT c FROM CuentaCorrienteProveedor c JOIN FETCH c.proveedor p "
                    + "WHERE UPPER(c.estado) = 'ACTIVA' AND COALESCE(c.saldoActual, 0) > 0 "
                    + "ORDER BY c.idCuentaCorrienteProveedor",
                    CuentaCorrienteProveedor.class)
                    .getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    private Integer resolveProveedorId(Proveedor proveedor) {
        if (proveedor == null) {
            return null;
        }
        Integer id = proveedor.getIdProveedor();
        if (id != null) {
            return id;
        }
        try {
            EntityManagerFactory emf = JPAUtil.getEntityManagerFactory();
            PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
            Object identifier = util.getIdentifier(proveedor);
            if (identifier instanceof Integer) {
                return (Integer) identifier;
            }
            if (identifier instanceof Number) {
                return ((Number) identifier).intValue();
            }
        } catch (Exception ignored) {
        }
        try {
            Object identifier = proveedor.getClass().getMethod("getIdProveedor").invoke(proveedor);
            if (identifier instanceof Integer) {
                return (Integer) identifier;
            }
            if (identifier instanceof Number) {
                return ((Number) identifier).intValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public CuentaCorrienteProveedor crearSiNoExiste(EntityManager em, Proveedor proveedor) {
        if (em == null) throw new IllegalArgumentException("EntityManager null");
        Integer idProveedor = resolveProveedorId(proveedor);
        if (proveedor == null || idProveedor == null) {
            throw new IllegalArgumentException("Proveedor inválido");
        }

        TypedQuery<CuentaCorrienteProveedor> q = em.createQuery(
                "SELECT c FROM CuentaCorrienteProveedor c WHERE c.proveedor.idProveedor = :id",
                CuentaCorrienteProveedor.class
        );
        q.setParameter("id", idProveedor);
        List<CuentaCorrienteProveedor> res = q.getResultList();
        if (!res.isEmpty()) return res.get(0);

        CuentaCorrienteProveedor ccp = new CuentaCorrienteProveedor();
        ccp.setProveedor(em.getReference(Proveedor.class, idProveedor));
        ccp.setEstado("ACTIVA");
        ccp.setFechaCreacion(LocalDate.now());
        ccp.setUltimaEdicion(LocalDate.now());
        ccp.setSaldoInicial(BigDecimal.ZERO);
        ccp.setSaldoActual(BigDecimal.ZERO);
        em.persist(ccp);
        return ccp;
    }
}
