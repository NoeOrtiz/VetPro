package veterinaria.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import javax.persistence.LockModeType;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.CuentaCorrienteProveedor;
import veterinaria.entidad.CuentaCorrienteProveedorMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Usuario;
import veterinaria.util.AuditoriaLogger;
import veterinaria.util.enums.MetodoPagoTipo;

/** Pago real de deuda a proveedor: reduce la deuda y registra el egreso de Caja en una sola transacción. */
public class PagoProveedorService {
    private final TxRunner tx = new TxRunner();

    public Integer registrarPago(Integer idCuenta, Integer idMetodoPago, BigDecimal monto,
            Usuario usuario, String claveOperacion) {
        if (idCuenta == null || idMetodoPago == null || monto == null || monto.signum() <= 0)
            throw new IllegalArgumentException("Datos de pago inválidos.");
        if (usuario == null || usuario.getIdUsuario() == null)
            throw new IllegalArgumentException("Usuario inválido.");
        String clave = normalizarClave(claveOperacion);

        Integer idMov = tx.runInTx(em -> {
            CajaMovimiento existente = em.createQuery("SELECT m FROM CajaMovimiento m WHERE m.claveOperacion = :c", CajaMovimiento.class)
                    .setParameter("c", clave).setMaxResults(1).getResultStream().findFirst().orElse(null);
            if (existente != null) return null;

            CuentaCorrienteProveedor cc = em.find(CuentaCorrienteProveedor.class, idCuenta, LockModeType.PESSIMISTIC_WRITE);
            if (cc == null || !"ACTIVA".equalsIgnoreCase(cc.getEstado())) throw new IllegalStateException("La cuenta del proveedor no está activa.");
            BigDecimal deuda = cc.getSaldoActual() == null ? BigDecimal.ZERO : cc.getSaldoActual();
            if (deuda.signum() <= 0) throw new IllegalStateException("El proveedor no registra deuda pendiente.");
            if (monto.compareTo(deuda) > 0) throw new IllegalStateException("El pago supera la deuda pendiente.");

            MetodoPago mp = em.find(MetodoPago.class, idMetodoPago);
            if (mp == null || !mp.isActivo()) throw new IllegalStateException("El medio de pago no está activo.");
            if (MetodoPagoTipo.CUENTA_CORRIENTE == MetodoPagoTipo.fromEtiqueta(mp.getNombre()))
                throw new IllegalArgumentException("Cuenta Corriente no es un medio de pago.");
            Usuario u = em.find(Usuario.class, usuario.getIdUsuario());
            if (u == null || !u.isActivo()) throw new IllegalStateException("El usuario no está activo.");
            CajaSesion sesion = em.createQuery("SELECT s FROM CajaSesion s WHERE s.estado = :e ORDER BY s.fechaApertura DESC", CajaSesion.class)
                    .setParameter("e", CajaSesion.Estado.ABIERTA).setMaxResults(1).getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de pagar una deuda a proveedor."));

            BigDecimal nuevoSaldo = deuda.subtract(monto);
            CuentaCorrienteProveedorMovimiento movCC = new CuentaCorrienteProveedorMovimiento();
            movCC.setCuentaCorrienteProveedor(cc); movCC.setFechaMovimiento(LocalDate.now());
            movCC.setDescripcion("Pago de deuda a proveedor"); movCC.setTipoMovimiento(CuentaCorrienteProveedorMovimiento.TipoMovimiento.CREDITO);
            movCC.setMonto(monto); movCC.setSaldoResultante(nuevoSaldo); em.persist(movCC);
            cc.setSaldoActual(nuevoSaldo); cc.setUltimaEdicion(LocalDate.now()); em.merge(cc);

            CajaMovimiento caja = new CajaMovimiento(); Date ahora = new Date();
            caja.setCajaSesion(sesion); caja.setTipoMovimiento(CajaMovimiento.TipoMovimiento.DEBITO); caja.setMonto(monto);
            caja.setFecha(ahora); caja.setFechaHora(ahora); caja.setUsuario(u); caja.setMetodoPago(mp);
            caja.setAfectaEfectivo(mp.isAfectaEfectivo()); caja.setDescripcion("Pago de deuda a proveedor"); caja.setClaveOperacion(clave);
            em.persist(caja); em.flush();
            return movCC.getIdMovimiento();
        });
        AuditoriaLogger.evento("PAGO_DEUDA_PROVEEDOR", "cuentaId="+idCuenta+" monto="+monto+" movimientoId="+idMov, usuario);
        return idMov;
    }

    private String normalizarClave(String clave) {
        String c = clave == null ? "" : clave.trim();
        if (c.isEmpty() || c.length() > 48 || !c.matches("[A-Za-z0-9-]+"))
            throw new IllegalArgumentException("Clave de operación inválida.");
        return c;
    }
}
