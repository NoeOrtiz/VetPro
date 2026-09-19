package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
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

        validarEntrada(recibo, items, pagos);

        return tx.runInTx(em -> {
            Usuario usuario = em.find(Usuario.class, recibo.getUsuario().getIdUsuario());
            if (usuario == null || !usuario.isActivo()) {
                throw new IllegalStateException("El usuario no esta activo.");
            }

            CajaSesion sesion = buscarCajaAbierta(em);

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
            if (totalPagos.compareTo(recibo.getTotalRecibo()) != 0) {
                throw new IllegalStateException("La suma de los medios de pago no coincide con el total de la venta.");
            }

            recibo.setUsuario(usuario);
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
                cm.setDescripcion("Venta - recibo N. " + recibo.getIdRecibo());
                em.persist(cm);
            }

            return recibo;
        });
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
            List<ReciboMetodoPago> pagos) {
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
        if (pagos == null || pagos.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un medio de pago.");
        }
    }
}
