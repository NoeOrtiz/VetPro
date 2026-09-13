
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
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            CompraProveedor compraManaged = prepararCompraManaged(em, compra);
            pago.setMetodoPago(em.getReference(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago()));

            CajaMovimiento mov = new CajaMovimiento();
            mov.setTipoMovimiento(CajaMovimiento.TipoMovimiento.DEBITO);
            mov.setMonto(pago.getMonto());
            mov.setFecha(onlyDate(compraManaged.getFecha() != null ? compraManaged.getFecha() : new Date()));
            mov.setMetodoPago(pago.getMetodoPago());
            mov.setDescripcion("Compra proveedor - Factura " + compraManaged.getNumeroFactura());
            mov.setUsuario(em.getReference(Usuario.class, usuario.getIdUsuario()));
            em.persist(mov);

            pago.setCajaMovimiento(mov);
            pago.setCompra(compraManaged);
            compraManaged.getPagos().clear();
            compraManaged.getPagos().add(pago);
            em.persist(compraManaged);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    public void crearCompraCuentaCorriente(
            CompraProveedor compra,
            CompraProveedorPago pago,
            CuentaCorrienteProveedorDAO ccpDAO,
            CuentaCorrienteProveedorMovimientoDAO movDAO
    ) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            CompraProveedor compraManaged = prepararCompraManaged(em, compra);
            pago.setMetodoPago(em.getReference(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago()));

            pago.setCompra(compraManaged);
            compraManaged.getPagos().clear();
            compraManaged.getPagos().add(pago);
            em.persist(compraManaged);
            em.flush();

            ccpDAO.crearSiNoExiste(em, compraManaged.getProveedor());
            movDAO.registrarDeudaPorCompra(em, compraManaged, pago.getMonto(), compraManaged.getFecha());

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    public void crearCompraConPagos(
            CompraProveedor compra,
            List<CompraProveedorPago> pagos,
            Usuario usuario,
            CuentaCorrienteProveedorDAO ccpDAO,
            CuentaCorrienteProveedorMovimientoDAO movDAO
    ) throws Exception {
        if (compra == null) throw new IllegalArgumentException("Compra null");
        if (pagos == null || pagos.isEmpty()) throw new IllegalArgumentException("Pagos vacíos");

        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            CompraProveedor compraManaged = prepararCompraManaged(em, compra);

            BigDecimal montoCC = BigDecimal.ZERO;

            compraManaged.getPagos().clear();
            for (CompraProveedorPago p : pagos) {
                if (p == null) continue;

                if (p.getMetodoPago() == null || p.getMetodoPago().getIdMetodoPago() == null) {
                    throw new IllegalArgumentException("Pago sin método de pago");
                }
                MetodoPago mpManaged = em.getReference(MetodoPago.class, p.getMetodoPago().getIdMetodoPago());
                p.setMetodoPago(mpManaged);

                p.setCompra(compraManaged);

                String nombreMP = (mpManaged.getNombre() == null) ? "" : mpManaged.getNombre().trim();

                if (MetodoPagoTipo.EFECTIVO == MetodoPagoTipo.fromEtiqueta(nombreMP)) {
                    CajaMovimiento mov = new CajaMovimiento();
                    mov.setTipoMovimiento(CajaMovimiento.TipoMovimiento.DEBITO);
                    mov.setMonto(p.getMonto());
                    mov.setFecha(onlyDate(compraManaged.getFecha() != null ? compraManaged.getFecha() : new Date()));
                    mov.setMetodoPago(mpManaged);
                    mov.setDescripcion("Compra proveedor - Factura " + compraManaged.getNumeroFactura());
                    mov.setUsuario(em.getReference(Usuario.class, usuario.getIdUsuario()));
                    em.persist(mov);
                    p.setCajaMovimiento(mov);
                }

                if (MetodoPagoTipo.CUENTA_CORRIENTE == MetodoPagoTipo.fromEtiqueta(nombreMP)) {
                    BigDecimal m = (p.getMonto() == null) ? BigDecimal.ZERO : p.getMonto();
                    montoCC = montoCC.add(m);
                }

                compraManaged.getPagos().add(p);
            }

            em.persist(compraManaged);
            em.flush();

            if (montoCC.compareTo(BigDecimal.ZERO) > 0) {
                ccpDAO.crearSiNoExiste(em, compraManaged.getProveedor());
                movDAO.registrarDeudaPorCompra(em, compraManaged, montoCC, compraManaged.getFecha());
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            if (em != null) em.close();
        }
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
        if (compra == null) throw new IllegalArgumentException("Compra null");
        if (pagos == null || pagos.isEmpty()) throw new IllegalArgumentException("Pagos vacíos");

        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            CompraProveedor compraManaged = prepararCompraManaged(em, compra);

            BigDecimal totalCC = BigDecimal.ZERO;

            compraManaged.getPagos().clear();
            for (CompraProveedorPago pago : pagos) {
                if (pago == null || pago.getMetodoPago() == null) {
                    throw new IllegalArgumentException("Pago inválido");
                }

                MetodoPago mp = em.getReference(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago());
                pago.setMetodoPago(mp);
                pago.setCompra(compraManaged);

                String nombreMp = (mp.getNombre() == null) ? "" : mp.getNombre().trim();
                String efectivoKey = (nombreMetodoEfectivo == null) ? "" : nombreMetodoEfectivo.trim();
                String cuentaKey = (nombreMetodoCuentaCorriente == null) ? "" : nombreMetodoCuentaCorriente.trim();

                if (!efectivoKey.isEmpty() && efectivoKey.equalsIgnoreCase(nombreMp)) {
                    CajaMovimiento mov = new CajaMovimiento();
                    mov.setTipoMovimiento(CajaMovimiento.TipoMovimiento.DEBITO);
                    mov.setMonto(pago.getMonto());
                    mov.setFecha(onlyDate(compraManaged.getFecha() != null ? compraManaged.getFecha() : new Date()));
                    mov.setMetodoPago(mp);
                    mov.setDescripcion("Compra proveedor - Factura " + compraManaged.getNumeroFactura());
                    mov.setUsuario(em.getReference(Usuario.class, usuario.getIdUsuario()));
                    em.persist(mov);
                    pago.setCajaMovimiento(mov);
                }

                if (!cuentaKey.isEmpty() && cuentaKey.equalsIgnoreCase(nombreMp)) {
                    BigDecimal m = (pago.getMonto() == null) ? BigDecimal.ZERO : pago.getMonto();
                    totalCC = totalCC.add(m);
                }

                compraManaged.getPagos().add(pago);
            }

            em.persist(compraManaged);
            em.flush();

            if (totalCC.compareTo(BigDecimal.ZERO) > 0) {
                ccpDAO.crearSiNoExiste(em, compraManaged.getProveedor());
                movDAO.registrarDeudaPorCompra(em, compraManaged, totalCC, compraManaged.getFecha());
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }
}
