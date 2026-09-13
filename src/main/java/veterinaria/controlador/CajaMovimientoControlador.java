package veterinaria.controlador;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaMovimiento.TipoMovimiento;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.CajaMovimientoDAO;
import veterinaria.servicio.CajaService;
import veterinaria.util.AuditoriaLogger;

public class CajaMovimientoControlador {

    private final CajaMovimientoDAO cajaDAO = new CajaMovimientoDAO();
    private final CajaService cajaService = new CajaService();

    private String ultimoError = null;

    private static Date onlyDate(Date d) {
        if (d == null) return null;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void registrarMovimiento(BigDecimal monto, TipoMovimiento tipo, String descripcion, Usuario usuario) {
        CajaMovimiento movimiento = new CajaMovimiento();
        movimiento.setMonto(monto);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setDescripcion(descripcion);
        movimiento.setFecha(onlyDate(new Date()));
        movimiento.setUsuario(usuario);

        cajaDAO.registrarMovimiento(movimiento);
    }

    public List<CajaMovimiento> obtenerMovimientos() {
        return cajaDAO.obtenerTodosLosMovimientos();
    }

    public boolean registrarApertura(BigDecimal montoInicial, Usuario usuario) {
        return registrarApertura(montoInicial, usuario, new Date());
    }

    public boolean registrarApertura(BigDecimal montoInicial, Usuario usuario, Date fecha) {
        try {
            ultimoError = null;
            boolean ok = cajaService.registrarApertura(montoInicial, usuario, onlyDate(fecha));
            if (ok) {
                AuditoriaLogger.evento(
                        "CAJA_APERTURA",
                        "fecha=" + onlyDate(fecha) + " monto=" + montoInicial,
                        usuario
                );
            }
            return ok;
        } catch (IllegalStateException e) {
            ultimoError = e.getMessage();
            AuditoriaLogger.evento(
                    "CAJA_APERTURA_FALLA",
                    "fecha=" + onlyDate(fecha) + " monto=" + montoInicial + " error=" + ultimoError,
                    usuario
            );
            return false;
        }
    }

    public boolean registrarCierre(BigDecimal montoFinal, Usuario usuario) {
        return registrarCierre(montoFinal, usuario, new Date());
    }

    public boolean registrarCierre(BigDecimal montoFinal, Usuario usuario, Date fecha) {
        try {
            ultimoError = null;
            boolean ok = cajaService.registrarCierre(montoFinal, usuario, onlyDate(fecha));
            if (ok) {
                AuditoriaLogger.evento(
                        "CAJA_CIERRE",
                        "fecha=" + onlyDate(fecha) + " monto=" + montoFinal,
                        usuario
                );
            }
            return ok;
        } catch (IllegalStateException e) {
            ultimoError = e.getMessage();
            AuditoriaLogger.evento(
                    "CAJA_CIERRE_FALLA",
                    "fecha=" + onlyDate(fecha) + " monto=" + montoFinal + " error=" + ultimoError,
                    usuario
            );
            return false;
        }
    }

    public boolean registrarCierrePorAperturaId(BigDecimal montoFinal, Usuario usuario, Long idApertura) {
        try {
            ultimoError = null;
            boolean ok = cajaService.registrarCierrePorAperturaId(montoFinal, usuario, idApertura);
            if (ok) {
                AuditoriaLogger.evento(
                        "CAJA_CIERRE",
                        "aperturaId=" + idApertura + " monto=" + montoFinal,
                        usuario
                );
            }
            return ok;
        } catch (IllegalStateException e) {
            ultimoError = e.getMessage();
            AuditoriaLogger.evento(
                    "CAJA_CIERRE_FALLA",
                    "aperturaId=" + idApertura + " monto=" + montoFinal + " error=" + ultimoError,
                    usuario
            );
            return false;
        }
    }

    public CajaMovimiento obtenerAperturaDeCajaPorFecha(Date fecha) {
        try {
            return cajaDAO.obtenerAperturaDeCajaPorFecha(onlyDate(fecha));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean modificiarMovimiento(CajaMovimiento movimiento) {
        return cajaDAO.modificarMovimiento(movimiento);
    }

    public String estadoCaja() {
        String retornar = "Sin movimientos.";
        Date hoy = onlyDate(new Date());
        if (cajaDAO.existeAperturaEnFecha(hoy)) {
            retornar = "ABIERTA";
        }
        if (cajaDAO.existeCierreEnFecha(hoy)) {
            retornar = "CERRADA";
        }
        return retornar;
    }

    public CajaMovimiento obtenerAperturaPendienteCierre() {
        return cajaDAO.obtenerAperturaPendienteCierre();
    }

    public BigDecimal obtenerTotalIngresosDelDia() {
        return obtenerTotalIngresosPorFecha(new Date());
    }

    public BigDecimal obtenerTotalIngresosPorFecha(Date fecha) {
        List<CajaMovimiento> ingresos = cajaDAO.obtenerIngresosPorFecha(onlyDate(fecha));

        return ingresos.stream()
                .map(CajaMovimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal obtenerTotalEgresosDelDia() {
        return obtenerTotalEgresosPorFecha(new Date());
    }

    public BigDecimal obtenerTotalEgresosPorFecha(Date fecha) {
        List<CajaMovimiento> egresos = cajaDAO.obtenerEgresosPorFecha(onlyDate(fecha));

        return egresos.stream()
                .map(CajaMovimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal obtenerMontoAperturaDelDia() {
        return obtenerMontoAperturaPorFecha(new Date());
    }

    public BigDecimal obtenerMontoAperturaPorFecha(Date fecha) {
        CajaMovimiento apertura = cajaDAO.obtenerAperturaDeCajaPorFecha(onlyDate(fecha));
        return apertura != null ? apertura.getMonto() : BigDecimal.ZERO;
    }

    public CajaMovimiento obtenerCierreDeCajaDiario() {
        return cajaDAO.obtenerCierreDeCajaPorFecha(onlyDate(new Date()));
    }

    public CajaMovimiento obtenerAperturaDeCajaDiario() {
        return cajaDAO.obtenerAperturaDeCajaPorFecha(onlyDate(new Date()));
    }

    public CajaMovimiento obtenerUltimoCierreDeCaja() {
        return cajaDAO.obtenerUltimoCierreDeCaja();
    }
    

    public List<CajaMovimiento> obtenerMovimientosFiltrados(TipoMovimiento tipo, Date desde, Date hasta) {
        Date d = onlyDate(desde);
        Date h = onlyDate(hasta);
        return cajaDAO.obtenerMovimientosFiltrados(tipo, d, h);
    }

}
