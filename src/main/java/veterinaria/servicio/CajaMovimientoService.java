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
            return movimiento;
        });
    }
}
