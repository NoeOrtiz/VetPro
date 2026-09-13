
package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import veterinaria.controlador.CajaMovimientoControlador;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.MetodoPagoControlador;
import veterinaria.controlador.ProductoControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.Usuario;
import veterinaria.util.SesionUsuario;
import veterinaria.util.VentaProducto;
import static veterinaria.util.Constantes.*;
import veterinaria.util.enums.MetodoPagoTipo;

public class CajaRegistradoraFacade {

    private static final String PERM_REGISTRAR_VENTA = "FormCajaRegistradora.REGISTRAR_VENTA";
    private static final String PERM_REGISTRAR_COBRO_CC = "FormCajaRegistradora.REGISTRAR_COBRO_CC";

    
private void requirePermiso(String permiso, String accionHumana) {
    SesionUsuario sesion = SesionUsuario.getInstancia();
    if (sesion == null || sesion.getUsuario() == null) {
        throw new IllegalStateException("Sesión no iniciada. No se puede: " + accionHumana);
    }
    if (!sesion.puede(permiso)) {
        throw new IllegalStateException("Acceso restringido. No tenés permiso para: " + accionHumana);
    }
}

    private void validarPagosMixtos(List<FormaPagoInput> formasPago, BigDecimal totalRecibo) {
        if (totalRecibo == null) {
            throw new IllegalArgumentException("Total del recibo inválido.");
        }
        if (formasPago == null || formasPago.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una forma de pago.");
        }

        BigDecimal totalPagos = BigDecimal.ZERO;
        BigDecimal montoEfectivo = BigDecimal.ZERO;

        for (FormaPagoInput fp : formasPago) {
            if (fp == null) continue;
            if (fp.getNombreMetodoPago() == null || fp.getNombreMetodoPago().trim().isEmpty()) {
                throw new IllegalArgumentException("Forma de pago inválida (método vacío).");
            }
            BigDecimal monto = (fp.getMonto() == null) ? BigDecimal.ZERO : fp.getMonto();
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("No se permiten montos menores o iguales a 0 en formas de pago.");
            }
            totalPagos = totalPagos.add(monto);
            if (MetodoPagoTipo.EFECTIVO == MetodoPagoTipo.fromEtiqueta(fp.getNombreMetodoPago())) {
                montoEfectivo = montoEfectivo.add(monto);
            }
        }

        if (totalPagos.compareTo(totalRecibo) < 0) {
            throw new IllegalArgumentException(
                    "Pagos insuficientes. Total a cobrar: " + totalRecibo + " / Total pagado: " + totalPagos);
        }

        if (totalPagos.compareTo(totalRecibo) > 0) {
            BigDecimal vuelto = totalPagos.subtract(totalRecibo);
            if (montoEfectivo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Hay vuelto (pagos > total) pero no se incluyó EFECTIVO.\n" +
                        "Para dar vuelto, el pago en efectivo es obligatorio.");
            }
            if (vuelto.compareTo(montoEfectivo) > 0) {
                throw new IllegalArgumentException(
                        "El vuelto (" + vuelto + ") no puede ser mayor al efectivo entregado (" + montoEfectivo + ").");
            }
        }
    }

    public static class FormaPagoInput {
        private final String nombreMetodoPago;
        private final BigDecimal monto;

        public FormaPagoInput(String nombreMetodoPago, BigDecimal monto) {
            this.nombreMetodoPago = nombreMetodoPago;
            this.monto = monto;
        }

        public String getNombreMetodoPago() {
            return nombreMetodoPago;
        }

        public BigDecimal getMonto() {
            return monto;
        }
    }

    public static class MontosReciboInput {
        private final BigDecimal subtotal;
        private final BigDecimal descuento;
        private final BigDecimal iva;
        private final BigDecimal total;

        public MontosReciboInput(BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total) {
            this.subtotal = subtotal;
            this.descuento = descuento;
            this.iva = iva;
            this.total = total;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getDescuento() {
            return descuento;
        }

        public BigDecimal getIva() {
            return iva;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }

    private final CajaMovimientoControlador cajaCtrl = new CajaMovimientoControlador();
    private final CajaService cajaCtrl2 = new CajaService();
    private final ClienteControlador clienteCtrl = new ClienteControlador();
    private final ProductoControlador productoCtrl = new ProductoControlador();
    private final MetodoPagoControlador metodoPagoCtrl = new MetodoPagoControlador();

    private final CajaRegistradoraTxService ventaTx = new CajaRegistradoraTxService();
    private final CobroCuentaCorrienteTxService cobroTx = new CobroCuentaCorrienteTxService();

    public String estadoCaja() {
        return cajaCtrl.estadoCaja();
    }

    private void assertCajaAbierta() {
        String estado = estadoCaja();
        if (!"ABIERTA".equals(estado)) {
            throw new IllegalStateException("La caja no está ABIERTA. Estado actual: " + estado);
        }
    }

    public CajaRegistradoraTxService.ResultadoVenta registrarVenta(Integer idCliente,
                                                                  List<VentaProducto> items,
                                                                  List<FormaPagoInput> formasPago,
                                                                  MontosReciboInput montos,
                                                                  Usuario usuario) {
        
        requirePermiso(PERM_REGISTRAR_VENTA, "registrar una venta");
assertCajaAbierta();

        if (idCliente == null) throw new IllegalArgumentException("Debe seleccionar un cliente.");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Debe agregar productos a la venta.");
        if (montos == null) throw new IllegalArgumentException("Montos de recibo inválidos.");
        validarPagosMixtos(formasPago, montos.getTotal());

        Cliente cliente = clienteCtrl.buscarPorId(idCliente);
        if (cliente == null) throw new IllegalArgumentException("Cliente inexistente (ID " + idCliente + ").");

        Recibo recibo = new Recibo();
        recibo.setFecha(new Date());
        recibo.setTipo("Venta");
        recibo.setCliente(cliente);
        recibo.setUsuario(usuario);
        recibo.setSubtotalRecibo(montos.getSubtotal());
        recibo.setTotalDescuento(montos.getDescuento());
        recibo.setTotalIva(montos.getIva());
        recibo.setTotalRecibo(montos.getTotal());

        List<ReciboProductos> listaProductos = new ArrayList<>();
        for (VentaProducto vp : items) {
            if (vp == null) continue;

            ReciboProductos rp = new ReciboProductos();
            rp.setProducto(productoCtrl.buscarProductoPorId(vp.getIdProducto()));
            rp.setCantidad(vp.getCantidadProducto());
            rp.setPrecioUnitario(vp.getPrecioBase());
            rp.setIvaUnitario(vp.getMontoIVAUnitario());
            rp.setDescuentoUnitario(vp.getMontoDescuentoUnitario());
            rp.setGananciaUnitario(vp.getMontoGananciaUnitario());
            rp.setTotalUnitario(vp.getMontoTotalUnitario());
            BigDecimal totalCantidad = BigDecimal.valueOf(vp.getCantidadProducto())
                    .multiply(vp.getMontoTotalUnitario())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            rp.setTotalCantidad(totalCantidad);
            rp.setRecibo(recibo);

            listaProductos.add(rp);
        }

        List<ReciboMetodoPago> listaMetodosPago = new ArrayList<>();
        for (FormaPagoInput fp : formasPago) {
            if (fp == null) continue;
            MetodoPago metodoPago = metodoPagoCtrl.obtenerMetodoDePagoPorNombre(fp.getNombreMetodoPago());
            if (metodoPago == null) {
                throw new IllegalArgumentException("Método de pago inexistente: " + fp.getNombreMetodoPago());
            }
            ReciboMetodoPago rmp = new ReciboMetodoPago();
            rmp.setMetodoPago(metodoPago);
            rmp.setMonto(fp.getMonto());
            rmp.setRecibo(recibo);
            listaMetodosPago.add(rmp);
        }

        return ventaTx.registrarVenta(recibo, listaProductos, listaMetodosPago, usuario);
    }

    public CobroCuentaCorrienteTxService.ResultadoCobro registrarCobroCuentaCorriente(Integer idCliente,
                                                                                      BigDecimal monto,
                                                                                      MetodoPago metodoPago,
                                                                                      Usuario usuario,
                                                                                      String descripcion) {
        
        requirePermiso(PERM_REGISTRAR_COBRO_CC, "registrar un cobro de Cuenta Corriente");
assertCajaAbierta();
        return cobroTx.registrarCobro(idCliente, monto, metodoPago, usuario, descripcion);
    }

    public boolean registrarApertura(BigDecimal montoInicial, Usuario usuario) {
        return cajaCtrl.registrarApertura(montoInicial, usuario);
    }

    public boolean registrarCierreConAjuste(BigDecimal montoFisico, Usuario usuario, Date fecha, String motivo) {
        return cajaCtrl2.registrarCierreConAjuste(montoFisico, usuario, fecha, motivo);
    }

    public String getUltimoErrorCaja() {
        return cajaCtrl.getUltimoError();
    }
}
