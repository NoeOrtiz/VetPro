package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import javax.persistence.LockModeType;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Usuario;

/**
 * Registra ingresos y egresos manuales dentro de una sesion de caja.
 * No se utiliza para ventas ni cobros de cuenta corriente.
 */
public class CajaMovimientoService {

    private final TxRunner tx = new TxRunner();
    private final AuditoriaService auditoria = new AuditoriaService();

    public CajaMovimiento registrarIngreso(BigDecimal monto, Integer idMetodoPago,
            String motivo, Usuario usuario) {
        return registrar(monto, idMetodoPago, motivo, usuario,
                CajaMovimiento.TipoMovimiento.CREDITO);
    }

    public CajaMovimiento registrarEgreso(BigDecimal monto, Integer idMetodoPago,
            String motivo, Usuario usuario) {
        return registrar(monto, idMetodoPago, motivo, usuario,
                CajaMovimiento.TipoMovimiento.DEBITO);
    }

    public CajaMovimiento anularYRevertir(Long idMovimiento, String motivo, Usuario usuario) {
        if (idMovimiento == null) {
            throw new IllegalArgumentException("Debe indicar el movimiento.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de anulacion es obligatorio.");
        }
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario que realiza la anulacion.");
        }

        return tx.runInTx(em -> {
            CajaMovimiento original = em.find(
                    CajaMovimiento.class, idMovimiento, LockModeType.PESSIMISTIC_WRITE);
            if (original == null) {
                throw new IllegalStateException("El movimiento no existe.");
            }
            if (original.isAnulado()) {
                throw new IllegalStateException("El movimiento ya fue anulado.");
            }
            if (original.getTipoMovimiento() != CajaMovimiento.TipoMovimiento.CREDITO
                    && original.getTipoMovimiento() != CajaMovimiento.TipoMovimiento.DEBITO) {
                throw new IllegalStateException("Aperturas y cierres no se anulan como movimientos manuales.");
            }
            if (original.getRecibo() != null) {
                throw new IllegalStateException(
                        "El movimiento pertenece a una venta o cobro y debe revertirse desde su operacion de origen.");
            }
            if (original.getCajaSesion() == null) {
                throw new IllegalStateException("El movimiento heredado no pertenece a una sesion de caja.");
            }

            CajaSesion sesion = em.find(CajaSesion.class,
                    original.getCajaSesion().getIdCajaSesion(), LockModeType.PESSIMISTIC_WRITE);
            if (sesion == null || sesion.getEstado() != CajaSesion.Estado.ABIERTA) {
                throw new IllegalStateException(
                        "Solo se pueden anular movimientos mientras la sesion de caja esta abierta.");
            }

            Usuario managedUsuario = em.find(Usuario.class, usuario.getIdUsuario());
            if (managedUsuario == null || !managedUsuario.isActivo()) {
                throw new IllegalStateException("El usuario no existe o esta inactivo.");
            }

            Long reversiones = em.createQuery(
                    "SELECT COUNT(m) FROM CajaMovimiento m WHERE m.movimientoReversion.idMovimiento = :id",
                    Long.class)
                    .setParameter("id", idMovimiento)
                    .getSingleResult();
            if (reversiones != null && reversiones.longValue() > 0L) {
                throw new IllegalStateException("El movimiento ya posee una reversion.");
            }

            Date ahora = new Date();
            original.setAnulado(true);
            original.setFechaAnulacion(ahora);
            original.setMotivoAnulacion(motivo.trim());
            original.setUsuarioAnulacion(managedUsuario);

            CajaMovimiento reversion = new CajaMovimiento();
            reversion.setCajaSesion(sesion);
            reversion.setMonto(original.getMonto());
            reversion.setTipoMovimiento(
                    original.getTipoMovimiento() == CajaMovimiento.TipoMovimiento.CREDITO
                            ? CajaMovimiento.TipoMovimiento.DEBITO
                            : CajaMovimiento.TipoMovimiento.CREDITO);
            reversion.setFecha(ahora);
            reversion.setFechaHora(ahora);
            reversion.setUsuario(managedUsuario);
            reversion.setRecibo(original.getRecibo());
            reversion.setMetodoPago(original.getMetodoPago());
            reversion.setDescripcion("REVERSIÓN: " + motivo.trim());
            reversion.setMovimientoReversion(original);
            reversion.setClaveOperacion("REV-" + idMovimiento + "-" + UUID.randomUUID().toString());
            reversion.setAnulado(false);
            reversion.setEliminado(false);
            em.persist(reversion);
            em.flush();
            auditoria.registrarConUsuario(managedUsuario, "REVERTIR_CAJA", "CajaMovimiento",
                    original.getIdMovimiento(), "CAJA",
                    "Reversion de movimiento " + original.getIdMovimiento() + ". Motivo: " + motivo.trim(),
                    AuditoriaService.RESULT_OK, null, null, null);
            return reversion;
        });
    }

    private CajaMovimiento registrar(BigDecimal monto, Integer idMetodoPago,
            String motivo, Usuario usuario, CajaMovimiento.TipoMovimiento tipo) {

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (idMetodoPago == null) {
            throw new IllegalArgumentException("Debe indicar el medio de pago.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo es obligatorio.");
        }
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario.");
        }

        return tx.runInTx(em -> {
            CajaSesion sesion = em.createQuery(
                    "SELECT s FROM CajaSesion s WHERE s.estado = :estado ORDER BY s.fechaApertura DESC",
                    CajaSesion.class)
                    .setParameter("estado", CajaSesion.Estado.ABIERTA)
                    .setMaxResults(1)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Debe abrir la caja antes de registrar el movimiento."));

            Usuario managedUsuario = em.find(Usuario.class, usuario.getIdUsuario());
            if (managedUsuario == null || !managedUsuario.isActivo()) {
                throw new IllegalStateException("El usuario no existe o esta inactivo.");
            }

            MetodoPago metodo = em.find(MetodoPago.class, idMetodoPago);
            if (metodo == null || !metodo.isActivo()) {
                throw new IllegalStateException("El medio de pago no esta activo.");
            }

            CajaMovimiento movimiento = new CajaMovimiento();
            movimiento.setCajaSesion(sesion);
            movimiento.setMonto(monto);
            movimiento.setTipoMovimiento(tipo);
            movimiento.setFecha(new Date());
            movimiento.setFechaHora(new Date());
            movimiento.setUsuario(managedUsuario);
            movimiento.setMetodoPago(metodo);
            movimiento.setDescripcion(motivo.trim());
            movimiento.setClaveOperacion("MAN-" + UUID.randomUUID().toString());
            movimiento.setAnulado(false);
            movimiento.setEliminado(false);
            em.persist(movimiento);
            em.flush();
            String accion = tipo == CajaMovimiento.TipoMovimiento.CREDITO ? "INGRESO_CAJA" : "EGRESO_CAJA";
            auditoria.registrarConUsuario(managedUsuario, accion, "CajaMovimiento",
                    movimiento.getIdMovimiento(), "CAJA",
                    motivo.trim() + ". Importe: " + monto,
                    AuditoriaService.RESULT_OK, null, null, null);
            return movimiento;
        });
    }
}
