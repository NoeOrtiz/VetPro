package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.Usuario;

/**
 * Adaptador temporal para la pantalla heredada de Caja.
 * La unica implementacion de negocio para ventas es VentaService.
 * Cuenta Corriente no es un medio de pago: se separa como saldo financiado.
 */
@Deprecated
public class CajaRegistradoraTxService {

    private final VentaService ventaService = new VentaService();

    public static class ResultadoVenta {
        private final boolean ok;
        private final Long idRecibo;
        private final String error;

        public ResultadoVenta(boolean ok, Long idRecibo, String error) {
            this.ok = ok;
            this.idRecibo = idRecibo;
            this.error = error;
        }

        public boolean isOk() { return ok; }
        public Long getIdRecibo() { return idRecibo; }
        public String getError() { return error; }
    }

    public ResultadoVenta registrarVenta(Recibo recibo,
            List<ReciboProductos> productos,
            List<ReciboMetodoPago> metodosPago,
            Usuario usuario) {
        try {
            if (recibo == null) throw new IllegalArgumentException("Recibo nulo.");
            recibo.setUsuario(usuario);

            List<ReciboMetodoPago> pagosReales = new ArrayList<>();
            BigDecimal montoACuenta = BigDecimal.ZERO;
            if (metodosPago != null) {
                for (ReciboMetodoPago pago : metodosPago) {
                    if (pago == null || pago.getMetodoPago() == null) continue;
                    String nombre = pago.getMetodoPago().getNombre();
                    if (esCuentaCorriente(nombre)) {
                        if (pago.getMonto() != null) montoACuenta = montoACuenta.add(pago.getMonto());
                    } else {
                        pagosReales.add(pago);
                    }
                }
            }

            String clave = "VENTA-" + UUID.randomUUID();
            Recibo guardado = ventaService.confirmarVenta(
                    recibo, productos, pagosReales, montoACuenta, clave);
            return new ResultadoVenta(true, guardado.getIdRecibo(), null);
        } catch (Exception e) {
            return new ResultadoVenta(false, null, e.getMessage());
        }
    }

    private boolean esCuentaCorriente(String nombre) {
        if (nombre == null) return false;
        String n = java.text.Normalizer.normalize(nombre.trim().toLowerCase(),
                java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.contains("cuenta corriente") || n.contains("cta cte");
    }
}
