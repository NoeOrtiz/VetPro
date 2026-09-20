
package veterinaria.persistencia;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.CompraProveedor;
import veterinaria.entidad.CompraProveedorPago;
import veterinaria.entidad.CompraProveedorEstado;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.Usuario;
import veterinaria.util.Constantes;
import veterinaria.util.enums.MetodoPagoTipo;

public class CompraProveedorDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
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

    private static Date onlyDate(Date d) {
        if (d == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private CompraProveedor prepararCompraManaged(EntityManager em, CompraProveedor compra) {
        if (em == null) throw new IllegalArgumentException("EntityManager null");
        if (compra == null) throw new IllegalArgumentException("Compra null");
        Integer idProveedor = resolveProveedorId(compra.getProveedor());
        if (compra.getProveedor() == null || idProveedor == null) {
            throw new IllegalArgumentException("Proveedor inválido");
        }
        if (compra.getUsuario() == null || compra.getUsuario().getIdUsuario() == null) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        CompraProveedor managed = new CompraProveedor();
        managed.setEstado(compra.getEstado());
        managed.setFecha(compra.getFecha());
        managed.setFechaAnulacion(compra.getFechaAnulacion());
        managed.setMotivoAnulacion(compra.getMotivoAnulacion());
        managed.setNumeroFactura(compra.getNumeroFactura());
        managed.setFechaVencimiento(compra.getFechaVencimiento());
        managed.setTotalFactura(compra.getTotalFactura());
        managed.setSaldoPendiente(compra.getSaldoPendiente());
        managed.setProveedor(em.getReference(Proveedor.class, idProveedor));
        managed.setUsuario(em.getReference(Usuario.class, compra.getUsuario().getIdUsuario()));
        return managed;
    }

    public boolean existeFacturaProveedor(int idProveedor, String numeroFactura) {
        if (idProveedor <= 0) return false;
        if (numeroFactura == null) return false;
        String nf = numeroFactura.trim();
        if (nf.isEmpty()) return false;

        EntityManager em = getEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(c) FROM CompraProveedor c "
                    + "WHERE c.proveedor.idProveedor = :idProv "
                    + "AND c.numeroFactura = :nf "
                    + "AND c.estado <> :anulada",
                    Long.class
            );
            q.setParameter("idProv", idProveedor);
            q.setParameter("nf", nf);
            q.setParameter("anulada", CompraProveedorEstado.ANULADA);
            Long count = q.getSingleResult();
            return count != null && count.longValue() > 0;
        } finally {
            if (em != null) em.close();
        }
    }

    public void crearCompraEfectivo(CompraProveedor compra, CompraProveedorPago pago, Usuario usuario) throws Exception {
        throw new UnsupportedOperationException("crearCompraEfectivo heredado deshabilitado: use crearCompraConPagosReales.");
    }

    public void crearCompraCuentaCorriente(
            CompraProveedor compra,
            CompraProveedorPago pago,
            CuentaCorrienteProveedorDAO ccpDAO,
            CuentaCorrienteProveedorMovimientoDAO movDAO
    ) throws Exception {
        throw new UnsupportedOperationException("crearCompraCuentaCorriente heredado deshabilitado: use crearCompraConPagosReales.");
    }

    public void crearCompraConPagos(
            CompraProveedor compra,
            List<CompraProveedorPago> pagos,
            Usuario usuario,
            CuentaCorrienteProveedorDAO ccpDAO,
            CuentaCorrienteProveedorMovimientoDAO movDAO
    ) throws Exception {
        throw new UnsupportedOperationException("crearCompraConPagos heredado deshabilitado: use crearCompraConPagosReales.");
    }

    /**
     * Nuevo flujo: los pagos contienen solo medios de pago reales y saldoPendiente
     * representa deuda con el proveedor. Cuenta Corriente deja de ser un medio de pago.
     */
    public void crearCompraConPagosReales(CompraProveedor compra, List<CompraProveedorPago> pagos,
            BigDecimal saldoPendiente, Usuario usuario, CuentaCorrienteProveedorDAO ccpDAO,
            CuentaCorrienteProveedorMovimientoDAO movDAO) throws Exception {
        if (compra == null) throw new IllegalArgumentException("Compra null");
        if (pagos == null) throw new IllegalArgumentException("Pagos null");
        BigDecimal deuda = saldoPendiente == null ? BigDecimal.ZERO : saldoPendiente;
        if (deuda.signum() < 0) throw new IllegalArgumentException("El saldo pendiente no puede ser negativo");
        if (compra.getTotalFactura() == null || compra.getTotalFactura().signum() <= 0)
            throw new IllegalArgumentException("El total de la factura debe ser mayor a cero");
        if (usuario == null || usuario.getIdUsuario() == null)
            throw new IllegalArgumentException("Usuario inválido");
        BigDecimal totalPagado = BigDecimal.ZERO;
        for (CompraProveedorPago pago : pagos) {
            if (pago != null && pago.getMonto() != null) totalPagado = totalPagado.add(pago.getMonto());
        }
        if (totalPagado.add(deuda).compareTo(compra.getTotalFactura()) != 0)
            throw new IllegalArgumentException("Los pagos reales más el saldo pendiente deben coincidir con el total de la factura");

        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Integer idProveedor = resolveProveedorId(compra.getProveedor());
            String numeroFactura = compra.getNumeroFactura() == null ? "" : compra.getNumeroFactura().trim();
            if (idProveedor == null || numeroFactura.isEmpty())
                throw new IllegalArgumentException("Proveedor y número de factura son obligatorios");
            Long duplicadas = em.createQuery(
                    "SELECT COUNT(c) FROM CompraProveedor c WHERE c.proveedor.idProveedor = :idProv "
                    + "AND c.numeroFactura = :nf AND c.estado <> :anulada", Long.class)
                    .setParameter("idProv", idProveedor)
                    .setParameter("nf", numeroFactura)
                    .setParameter("anulada", CompraProveedorEstado.ANULADA)
                    .getSingleResult();
            if (duplicadas != null && duplicadas > 0)
                throw new IllegalStateException("Ya existe una compra activa con esa factura para el proveedor");
            compra.setNumeroFactura(numeroFactura);

            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            if (usuarioManaged == null || !usuarioManaged.isActivo())
                throw new IllegalStateException("El usuario no está activo");

            CompraProveedor compraManaged = prepararCompraManaged(em, compra);
            compraManaged.setUsuario(usuarioManaged);
            compraManaged.setSaldoPendiente(deuda);
            compraManaged.getPagos().clear();

            for (CompraProveedorPago pago : pagos) {
                if (pago == null || pago.getMetodoPago() == null || pago.getMetodoPago().getIdMetodoPago() == null
                        || pago.getMonto() == null || pago.getMonto().signum() <= 0) {
                    throw new IllegalArgumentException("Pago real inválido");
                }
                MetodoPago mp = em.find(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago());
                if (mp == null || !mp.isActivo()) throw new IllegalStateException("El medio de pago no está activo");
                if (MetodoPagoTipo.CUENTA_CORRIENTE == MetodoPagoTipo.fromEtiqueta(mp.getNombre())) {
                    throw new IllegalArgumentException("Cuenta Corriente no es un medio de pago de la compra");
                }
                CajaSesion sesion = buscarCajaAbierta(em);
                pago.setMetodoPago(mp);
                pago.setCompra(compraManaged);
                CajaMovimiento mov = new CajaMovimiento();
                Date ahora = new Date();
                mov.setCajaSesion(sesion); mov.setTipoMovimiento(CajaMovimiento.TipoMovimiento.DEBITO);
                mov.setMonto(pago.getMonto()); mov.setFecha(onlyDate(ahora)); mov.setFechaHora(ahora);
                mov.setMetodoPago(mp); mov.setAfectaEfectivo(mp.isAfectaEfectivo());
                mov.setDescripcion("Pago compra proveedor - Factura " + compraManaged.getNumeroFactura());
                mov.setUsuario(usuarioManaged);
                em.persist(mov); pago.setCajaMovimiento(mov); compraManaged.getPagos().add(pago);
            }

            em.persist(compraManaged); em.flush();
            if (deuda.signum() > 0) {
                ccpDAO.crearSiNoExiste(em, compraManaged.getProveedor());
                movDAO.registrarDeudaPorCompra(em, compraManaged, deuda, compraManaged.getFecha());
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { if (em != null) em.close(); }
    }

    private CajaSesion buscarCajaAbierta(EntityManager em) {
        return em.createQuery(
                "SELECT s FROM CajaSesion s WHERE s.estado = :estado ORDER BY s.fechaApertura DESC", CajaSesion.class)
                .setParameter("estado", CajaSesion.Estado.ABIERTA)
                .setMaxResults(1)
                .getResultStream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de registrar un pago real de una compra."));
    }

    public void crearCompraConPagos(
            CompraProveedor compra,
            List<CompraProveedorPago> pagos,
            Usuario usuario,
            CuentaCorrienteProveedorDAO ccpDAO,
            CuentaCorrienteProveedorMovimientoDAO movDAO,
            String nombreMetodoEfectivo,
            String nombreMetodoCuentaCorriente
    ) throws Exception {
        throw new UnsupportedOperationException("crearCompraEfectivo heredado deshabilitado: use crearCompraConPagosReales.");
    }
}
