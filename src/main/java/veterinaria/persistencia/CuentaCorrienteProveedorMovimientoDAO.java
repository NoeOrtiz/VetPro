package veterinaria.persistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CompraProveedor;
import veterinaria.entidad.CuentaCorrienteProveedor;
import veterinaria.entidad.CuentaCorrienteProveedorMovimiento;

public class CuentaCorrienteProveedorMovimientoDAO {

    private Integer resolveProveedorId(CompraProveedor compra) {
        if (compra == null || compra.getProveedor() == null) {
            return null;
        }
        Integer id = compra.getProveedor().getIdProveedor();
        if (id != null) {
            return id;
        }
        try {
            EntityManagerFactory emf = JPAUtil.getEntityManagerFactory();
            PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
            Object identifier = util.getIdentifier(compra.getProveedor());
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

    public BigDecimal obtenerSaldoActual(EntityManager em, Integer idCuentaCorrienteProveedor) {
        if (idCuentaCorrienteProveedor == null) return BigDecimal.ZERO;
        try {
            TypedQuery<BigDecimal> q = em.createQuery(
                    "SELECT COALESCE(c.saldoActual, 0) FROM CuentaCorrienteProveedor c WHERE c.idCuentaCorrienteProveedor = :id",
                    BigDecimal.class
            );
            q.setParameter("id", idCuentaCorrienteProveedor);
            BigDecimal r = q.getSingleResult();
            return r == null ? BigDecimal.ZERO : r;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public void registrarDeudaPorCompra(EntityManager em, CompraProveedor compra, BigDecimal monto, Date fechaFactura) {
        if (em == null) throw new IllegalArgumentException("EntityManager null");
        if (compra == null || compra.getProveedor() == null) throw new IllegalArgumentException("Compra inválida");
        Integer idProveedor = resolveProveedorId(compra);
        if (idProveedor == null) throw new IllegalArgumentException("Proveedor inválido en compra");

        CuentaCorrienteProveedor ccp = em.createQuery(
                "SELECT c FROM CuentaCorrienteProveedor c WHERE c.proveedor.idProveedor = :id",
                CuentaCorrienteProveedor.class
        )
                .setParameter("id", idProveedor)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe cuenta corriente para el proveedor"));

        BigDecimal saldoActual = (ccp.getSaldoActual() == null) ? BigDecimal.ZERO : ccp.getSaldoActual();
        BigDecimal m = (monto == null) ? BigDecimal.ZERO : monto;
        BigDecimal saldoResultante = saldoActual.add(m);

        CuentaCorrienteProveedorMovimiento mov = new CuentaCorrienteProveedorMovimiento();
        mov.setCuentaCorrienteProveedor(ccp);

        LocalDate fechaMov = (fechaFactura == null)
                ? LocalDate.now()
                : fechaFactura.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        mov.setFechaMovimiento(fechaMov);
        mov.setDescripcion("Compra a CC - Factura " + compra.getNumeroFactura());
        mov.setTipoMovimiento(CuentaCorrienteProveedorMovimiento.TipoMovimiento.DEBITO);
        mov.setMonto(m);
        mov.setSaldoResultante(saldoResultante);
        mov.setCompraProveedor(compra);
        em.persist(mov);

        ccp.setSaldoActual(saldoResultante);
        ccp.setUltimaEdicion(LocalDate.now());
        em.merge(ccp);
    }
}
