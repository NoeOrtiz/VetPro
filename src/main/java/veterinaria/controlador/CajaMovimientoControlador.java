package veterinaria.controlador;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaMovimiento.TipoMovimiento;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.CajaMovimientoDAO;
import veterinaria.servicio.CajaSesionService;
import veterinaria.entidad.CajaSesion;
import veterinaria.util.AuditoriaLogger;

public class CajaMovimientoControlador {

    private final CajaMovimientoDAO cajaDAO = new CajaMovimientoDAO();
    private final CajaSesionService cajaSesionService = new CajaSesionService();

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
        try {
            ultimoError = null;
            cajaSesionService.abrir(montoInicial, usuario);
            return true;
        } catch (RuntimeException e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    /** La apertura pertenece a una sesión, no a una fecha contable artificial. */
    @Deprecated
    public boolean registrarApertura(BigDecimal montoInicial, Usuario usuario, Date fecha) {
        return registrarApertura(montoInicial, usuario);
    }

    public boolean registrarCierre(BigDecimal montoFinal, Usuario usuario) {
        try {
            ultimoError = null;
            CajaSesion sesion = cajaSesionService.obtenerSesionAbierta();
            if (sesion == null) {
                throw new IllegalStateException("No existe una sesión de caja abierta.");
            }
            cajaSesionService.cerrar(sesion.getIdCajaSesion(), montoFinal, null, usuario);
            return true;
        } catch (RuntimeException e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    @Deprecated
    public boolean registrarCierre(BigDecimal montoFinal, Usuario usuario, Date fecha) {
        return registrarCierre(montoFinal, usuario);
    }

    @Deprecated
    public boolean registrarCierrePorAperturaId(BigDecimal montoFinal, Usuario usuario, Long idApertura) {
        return registrarCierre(montoFinal, usuario);
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
        return cajaSesionService.obtenerSesionAbierta() != null ? "ABIERTA" : "CERRADA";
    }

    public CajaSesion obtenerSesionAbierta() {
        return cajaSesionService.obtenerSesionAbierta();
    }

    public BigDecimal obtenerEfectivoEsperadoSesionAbierta() {
        CajaSesion sesion = cajaSesionService.obtenerSesionAbierta();
        return sesion == null ? BigDecimal.ZERO
                : cajaSesionService.calcularEfectivoEsperado(sesion.getIdCajaSesion());
    }

    public boolean cerrarSesionAbierta(BigDecimal efectivoContado, String motivoDiferencia, Usuario usuario) {
        try {
            ultimoError = null;
            CajaSesion sesion = cajaSesionService.obtenerSesionAbierta();
            if (sesion == null) throw new IllegalStateException("No existe una sesión de caja abierta.");
            cajaSesionService.cerrar(sesion.getIdCajaSesion(), efectivoContado, motivoDiferencia, usuario);
            return true;
        } catch (RuntimeException e) {
            ultimoError = e.getMessage();
            return false;
        }
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
