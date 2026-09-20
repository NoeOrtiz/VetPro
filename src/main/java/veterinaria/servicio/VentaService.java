package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.StockMovimiento;
import veterinaria.entidad.StockMovimientoTipo;
import veterinaria.entidad.Usuario;

/**
 * Confirma una venta completa en una sola transaccion:
 * recibo + detalle + medios de pago + stock + caja.
 */
public class VentaService {

    private final TxRunner tx = new TxRunner();

    public Recibo confirmarVenta(Recibo recibo, List<ReciboProductos> items,
            List<ReciboMetodoPago> pagos) {

        return confirmarVenta(recibo, items, pagos, BigDecimal.ZERO);
    }

    /**
     * Permite pago total, pago mixto y saldo a cuenta corriente.
     * montoACuenta es la parte NO cobrada ahora y que pasa a deuda del cliente.
     */
    public Recibo confirmarVenta(Recibo recibo, List<ReciboProductos> items,
            List<ReciboMetodoPago> pagos, BigDecimal montoACuenta) {
        return confirmarVenta(recibo, items, pagos, montoACuenta, null);
    }

    public Recibo confirmarVenta(Recibo recibo, List<ReciboProductos> items,
            List<ReciboMetodoPago> pagos, BigDecimal montoACuenta, String claveOperacion) {

        if (pagos == null) pagos = Collections.emptyList();
        if (montoACuenta == null) montoACuenta = BigDecimal.ZERO;
        validarEntrada(recibo, items, pagos, montoACuenta);
        String claveBase = normalizarClaveOperacion(claveOperacion);

        return tx.runInTx(em -> {
            Usuario usuario = em.find(Usuario.class, recibo.getUsuario().getIdUsuario());
            if (usuario == null || !usuario.isActivo()) {
                throw new IllegalStateException("El usuario no esta activo.");
            }

            Cliente cliente = em.find(Cliente.class, recibo.getCliente().getIdCliente());
            if (cliente == null || !cliente.isActivo()) {
                throw new IllegalStateException("El cliente no esta activo.");
            }
            recibo.setCliente(cliente);

            BigDecimal totalPagos = BigDecimal.ZERO;
            for (ReciboMetodoPago pago : pagos) {
                if (pago == null || pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0
                        || pago.getMetodoPago() == null || pago.getMetodoPago().getIdMetodoPago() == null) {
                    throw new IllegalArgumentException("Existe un medio de pago invalido.");
                }
                MetodoPago metodo = em.find(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago());
                if (metodo == null || !metodo.isActivo()) {
                    throw new IllegalStateException("El medio de pago no esta activo.");
                }
                totalPagos = totalPagos.add(pago.getMonto());
            }
            if (totalPagos.add(montoACuenta).compareTo(recibo.getTotalRecibo()) != 0) {
                throw new IllegalStateException("Pagos + cuenta corriente deben coincidir con el total de la venta.");
            }

            CajaSesion sesion = totalPagos.signum() > 0 ? buscarCajaAbierta(em) : null;

            recibo.setUsuario(usuario);
            recibo.setClaveOperacion(claveBase);
            if (recibo.getFecha() == null) recibo.setFecha(new Date());
            em.persist(recibo);
            em.flush();

            for (ReciboProductos item : items) {
                if (item == null || item.getProducto() == null || item.getProducto().getIdProducto() == null
                        || item.getCantidad() == null || item.getCantidad() <= 0) {
                    throw new IllegalArgumentException("Existe un producto o cantidad invalida.");
                }

                Producto producto = em.find(Producto.class, item.getProducto().getIdProducto(),
                        LockModeType.PESSIMISTIC_WRITE);
                if (producto == null || "Inactivo".equalsIgnoreCase(producto.getEstado())) {
                    throw new IllegalStateException("Uno de los productos no esta disponible.");
                }

                int stockAntes = producto.getStock() == null ? 0 : producto.getStock();
                if (stockAntes < item.getCantidad()) {
                    throw new IllegalStateException("Stock insuficiente para " + producto.getNombre()
                            + ". Disponible: " + stockAntes + ".");
                }

                int stockDespues = stockAntes - item.getCantidad();
                producto.setStock(stockDespues);

                item.setRecibo(recibo);
                item.setProducto(producto);
                em.persist(item);

                StockMovimiento sm = new StockMovimiento();
                sm.setFecha(new Date());
                sm.setTipo(StockMovimientoTipo.SALIDA_VENTA);
                sm.setCantidad(item.getCantidad());
                sm.setStockAntes(stockAntes);
                sm.setStockDespues(stockDespues);
                sm.setProducto(producto);
                sm.setUsuario(usuario);
                sm.setRecibo(recibo);
                sm.setObservacion("Salida por venta - recibo N. " + recibo.getIdRecibo());
                em.persist(sm);
            }

            int indicePago = 0;
            for (ReciboMetodoPago pago : pagos) {
                MetodoPago metodo = em.find(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago());
                pago.setRecibo(recibo);
                pago.setMetodoPago(metodo);
                em.persist(pago);

                CajaMovimiento cm = new CajaMovimiento();
                cm.setCajaSesion(sesion);
                cm.setMonto(pago.getMonto());
                cm.setTipoMovimiento(CajaMovimiento.TipoMovimiento.CREDITO);
                cm.setFecha(new Date());
                cm.setFechaHora(new Date());
                cm.setUsuario(usuario);
                cm.setRecibo(recibo);
                cm.setMetodoPago(metodo);
                cm.setAfectaEfectivo(metodo.isAfectaEfectivo());
                cm.setDescripcion("Venta - recibo N. " + recibo.getIdRecibo());
                cm.setClaveOperacion(claveBase == null ? null
                        : claveBase + "-P" + (++indicePago));
                em.persist(cm);
            }

            if (montoACuenta.signum() > 0) {
                CuentaCorriente cuenta = em.createQuery(
                        "SELECT c FROM CuentaCorriente c WHERE c.cliente.idCliente = :idCliente",
                        CuentaCorriente.class)
                        .setParameter("idCliente", cliente.getIdCliente())
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultStream().findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "El cliente no posee una cuenta corriente habilitada."));

                if (!"Activo".equalsIgnoreCase(cuenta.getEstado())) {
                    throw new IllegalStateException("La cuenta corriente del cliente no esta activa.");
                }

                BigDecimal saldoAnterior = cuenta.getSaldoActual() == null
                        ? BigDecimal.ZERO : cuenta.getSaldoActual();
                BigDecimal saldoNuevo = saldoAnterior.subtract(montoACuenta);
                BigDecimal limite = cuenta.getLimiteCredito() == null
                        ? BigDecimal.ZERO : cuenta.getLimiteCredito();
                if (limite.signum() < 0) {
                    throw new IllegalStateException("El limite de credito configurado no puede ser negativo.");
                }
                BigDecimal deudaResultante = saldoNuevo.signum() < 0
                        ? saldoNuevo.abs() : BigDecimal.ZERO;
                if (deudaResultante.compareTo(limite) > 0) {
                    BigDecimal disponible = limite.subtract(
                            saldoAnterior.signum() < 0 ? saldoAnterior.abs() : BigDecimal.ZERO);
                    if (disponible.signum() < 0) disponible = BigDecimal.ZERO;
                    throw new IllegalStateException(
                            "La venta supera el limite de cuenta corriente. Disponible: " + disponible + ".");
                }

                CuentaCorrienteMovimiento mov = new CuentaCorrienteMovimiento();
                mov.setCuentaCorriente(cuenta);
                mov.setFechaMovimiento(LocalDate.now());
                mov.setDescripcion("Venta a cuenta corriente - recibo N. " + recibo.getIdRecibo());
                mov.setTipoMovimiento(CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
                mov.setRecibo(recibo);
                mov.setMonto(montoACuenta);
                mov.setSaldoResultante(saldoNuevo);
                em.persist(mov);

                cuenta.setSaldoActual(saldoNuevo);
                cuenta.setUltimaEdicion(LocalDate.now());
            }

            return recibo;
        });
    }

    private String normalizarClaveOperacion(String claveOperacion) {
        if (claveOperacion == null || claveOperacion.trim().isEmpty()) {
            return null;
        }
        String clave = claveOperacion.trim();
        if (clave.length() > 48) {
            throw new IllegalArgumentException("La clave de operacion es demasiado larga.");
        }
        return clave;
    }

    private CajaSesion buscarCajaAbierta(EntityManager em) {
        return em.createQuery(
                "SELECT s FROM CajaSesion s WHERE s.estado = :estado ORDER BY s.fechaApertura DESC",
                CajaSesion.class)
                .setParameter("estado", CajaSesion.Estado.ABIERTA)
                .setMaxResults(1)
                .getResultStream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de confirmar la venta."));
    }

    private void validarEntrada(Recibo recibo, List<ReciboProductos> items,
            List<ReciboMetodoPago> pagos, BigDecimal montoACuenta) {
        if (recibo == null || recibo.getTotalRecibo() == null
                || recibo.getTotalRecibo().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El recibo y su total son obligatorios.");
        }
        if (recibo.getUsuario() == null || recibo.getUsuario().getIdUsuario() == null) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        if (recibo.getCliente() == null || recibo.getCliente().getIdCliente() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener al menos un producto.");
        }
        if (montoACuenta == null || montoACuenta.signum() < 0) {
            throw new IllegalArgumentException("El importe a cuenta corriente no puede ser negativo.");
        }
        if ((pagos == null || pagos.isEmpty()) && montoACuenta.signum() == 0) {
            throw new IllegalArgumentException("Debe indicar un medio de pago o un importe a cuenta corriente.");
        }
    }
}
