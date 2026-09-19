package veterinaria.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.LockModeType;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.Usuario;

/**
 * Orquesta un cobro confirmado para evitar registrar dinero dos veces.
 * Caja registra dinero efectivamente recibido. Cuenta corriente solo se
 * modifica cuando el cobro cancela deuda existente.
 */
public class CobroService {

    private final TxRunner tx = new TxRunner();

    public boolean registrarCobroRecibo(Long idRecibo, List<ReciboMetodoPago> pagos,
            Usuario usuario, Integer idCuentaCorriente, BigDecimal montoAplicadoADeuda) {

        if (idRecibo == null) throw new IllegalArgumentException("El recibo es requerido.");
        if (pagos == null || pagos.isEmpty()) throw new IllegalArgumentException("Debe indicar al menos un medio de pago.");
        if (usuario == null || usuario.getIdUsuario() == null) throw new IllegalArgumentException("El usuario es requerido.");

        return tx.runInTx(em -> {
            Recibo recibo = em.find(Recibo.class, idRecibo, LockModeType.PESSIMISTIC_WRITE);
            if (recibo == null) throw new IllegalStateException("El recibo no existe.");

            CajaSesion sesion = em.createQuery(
                    "SELECT s FROM CajaSesion s WHERE s.estado = :estado ORDER BY s.fechaApertura DESC",
                    CajaSesion.class)
                    .setParameter("estado", CajaSesion.Estado.ABIERTA)
                    .setMaxResults(1)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de registrar un cobro."));

            Long movimientosExistentes = em.createQuery(
                    "SELECT COUNT(m) FROM CajaMovimiento m WHERE m.recibo.idRecibo = :idRecibo AND m.anulado = false",
                    Long.class).setParameter("idRecibo", idRecibo).getSingleResult();
            if (movimientosExistentes != null && movimientosExistentes > 0L) {
                throw new IllegalStateException("El recibo ya posee movimientos de caja registrados.");
            }

            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            BigDecimal totalPagado = BigDecimal.ZERO;

            for (ReciboMetodoPago pago : pagos) {
                if (pago == null || pago.getMetodoPago() == null || pago.getMetodoPago().getIdMetodoPago() == null
                        || pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Los medios de pago y sus importes deben ser válidos.");
                }

                MetodoPago metodo = em.find(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago());
                if (metodo == null || !metodo.isActivo()) {
                    throw new IllegalStateException("El medio de pago seleccionado no está activo.");
                }

                CajaMovimiento movimiento = new CajaMovimiento();
                movimiento.setCajaSesion(sesion);
                movimiento.setMonto(pago.getMonto());
                movimiento.setTipoMovimiento(CajaMovimiento.TipoMovimiento.CREDITO);
                movimiento.setFecha(new Date());
                movimiento.setFechaHora(new Date());
                movimiento.setUsuario(usuarioManaged);
                movimiento.setRecibo(recibo);
                movimiento.setMetodoPago(metodo);
                movimiento.setDescripcion("Cobro recibo N.º " + recibo.getIdRecibo());
                em.persist(movimiento);
                totalPagado = totalPagado.add(pago.getMonto());
            }

            if (recibo.getTotalRecibo() != null && totalPagado.compareTo(recibo.getTotalRecibo()) != 0) {
                throw new IllegalStateException("La suma de los medios de pago debe coincidir con el total del recibo.");
            }

            BigDecimal aplicado = montoAplicadoADeuda == null ? BigDecimal.ZERO : montoAplicadoADeuda;
            if (aplicado.compareTo(BigDecimal.ZERO) < 0 || aplicado.compareTo(totalPagado) > 0) {
                throw new IllegalArgumentException("El importe aplicado a deuda es inválido.");
            }

            if (aplicado.compareTo(BigDecimal.ZERO) > 0) {
                if (idCuentaCorriente == null) throw new IllegalArgumentException("Debe indicar la cuenta corriente.");
                CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente, LockModeType.PESSIMISTIC_WRITE);
                if (cc == null || "INACTIVO".equalsIgnoreCase(cc.getEstado())) {
                    throw new IllegalStateException("La cuenta corriente no está disponible.");
                }

                BigDecimal saldo = cc.getSaldoActual() == null ? BigDecimal.ZERO : cc.getSaldoActual();
                CuentaCorrienteMovimiento movCC = new CuentaCorrienteMovimiento();
                movCC.setCuentaCorriente(cc);
                movCC.setFechaMovimiento(LocalDate.now());
                movCC.setDescripcion("Pago de deuda - recibo N.º " + recibo.getIdRecibo());
                movCC.setTipoMovimiento(CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
                movCC.setMonto(aplicado);
                movCC.setSaldoResultante(saldo.add(aplicado));
                movCC.setRecibo(recibo);
                em.persist(movCC);

                cc.setSaldoActual(saldo.add(aplicado));
                cc.setUltimaEdicion(LocalDate.now());
                em.merge(cc);
            }

            return true;
        });
    }
}
