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
        return registrarCobroRecibo(idRecibo, pagos, usuario, idCuentaCorriente,
                montoAplicadoADeuda, null);
    }

    public boolean registrarCobroRecibo(Long idRecibo, List<ReciboMetodoPago> pagos,
            Usuario usuario, Integer idCuentaCorriente, BigDecimal montoAplicadoADeuda,
            String claveOperacion) {

        if (idRecibo == null) throw new IllegalArgumentException("El recibo es requerido.");
        if (pagos == null || pagos.isEmpty()) throw new IllegalArgumentException("Debe indicar al menos un medio de pago.");
        if (usuario == null || usuario.getIdUsuario() == null) throw new IllegalArgumentException("El usuario es requerido.");
        String claveBase = normalizarClaveOperacion(claveOperacion);

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

            // Un mismo recibo puede recibir varios pagos parciales en fechas distintas.
            // La proteccion contra doble clic/reintento se hace por claveOperacion,
            // no bloqueando todos los movimientos posteriores del recibo.
            if (claveBase != null) {
                Long mismaOperacion = em.createQuery(
                        "SELECT COUNT(m) FROM CajaMovimiento m WHERE m.claveOperacion = :base OR m.claveOperacion LIKE :prefijo",
                        Long.class)
                        .setParameter("base", claveBase)
                        .setParameter("prefijo", claveBase + "-P%")
                        .getSingleResult();
                if (mismaOperacion != null && mismaOperacion > 0L) {
                    throw new IllegalStateException("Esta operacion de cobro ya fue registrada.");
                }
            }

            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            if (usuarioManaged == null || !usuarioManaged.isActivo()) {
                throw new IllegalStateException("El usuario no esta activo.");
            }
            BigDecimal totalPagado = BigDecimal.ZERO;

            int indicePago = 0;
            for (ReciboMetodoPago pago : pagos) {
                if (pago == null || pago.getMetodoPago() == null || pago.getMetodoPago().getIdMetodoPago() == null
                        || pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Los medios de pago y sus importes deben ser válidos.");
                }

                MetodoPago metodo = em.find(MetodoPago.class, pago.getMetodoPago().getIdMetodoPago());
                if (metodo == null || !metodo.isActivo()) {
                    throw new IllegalStateException("El medio de pago seleccionado no está activo.");
                }
                if (esCuentaCorriente(metodo.getNombre())) {
                    throw new IllegalArgumentException("Cuenta Corriente no es un medio de pago.");
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
                movimiento.setAfectaEfectivo(metodo.isAfectaEfectivo());
                movimiento.setDescripcion("Cobro recibo N.º " + recibo.getIdRecibo());
                movimiento.setClaveOperacion(claveBase == null ? null
                        : claveBase + "-P" + (++indicePago));
                em.persist(movimiento);

                // El detalle del medio de pago también debe quedar asociado al recibo.
                // Sin esta persistencia, Caja registraba el ingreso pero el recibo podía
                // quedar sin el desglose histórico de cómo fue cobrado.
                ReciboMetodoPago detalle = new ReciboMetodoPago();
                detalle.setRecibo(recibo);
                detalle.setMetodoPago(metodo);
                detalle.setMonto(pago.getMonto());
                em.persist(detalle);

                totalPagado = totalPagado.add(pago.getMonto());
            }

            BigDecimal aplicado = montoAplicadoADeuda == null ? BigDecimal.ZERO : montoAplicadoADeuda;
            if (aplicado.compareTo(BigDecimal.ZERO) < 0 || aplicado.compareTo(totalPagado) > 0) {
                throw new IllegalArgumentException("El importe aplicado a deuda es invalido.");
            }
            if (idCuentaCorriente != null && aplicado.compareTo(totalPagado) != 0) {
                throw new IllegalArgumentException("En un pago de deuda, todo el importe cobrado debe aplicarse a la cuenta corriente.");
            }

            if (aplicado.compareTo(BigDecimal.ZERO) > 0) {
                if (idCuentaCorriente == null) throw new IllegalArgumentException("Debe indicar la cuenta corriente.");
                CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente, LockModeType.PESSIMISTIC_WRITE);
                if (cc == null || "INACTIVO".equalsIgnoreCase(cc.getEstado())) {
                    throw new IllegalStateException("La cuenta corriente no está disponible.");
                }

                if (recibo.getCliente() == null || cc.getCliente() == null
                        || !cc.getCliente().getIdCliente().equals(recibo.getCliente().getIdCliente())) {
                    throw new IllegalStateException("La cuenta corriente no corresponde al cliente del recibo.");
                }

                BigDecimal saldo = cc.getSaldoActual() == null ? BigDecimal.ZERO : cc.getSaldoActual();
                BigDecimal deudaActual = saldo.signum() < 0 ? saldo.abs() : BigDecimal.ZERO;
                if (aplicado.compareTo(deudaActual) > 0) {
                    throw new IllegalStateException("El pago supera el saldo adeudado actual.");
                }

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

    /** Registra un pago parcial o total de deuda sin inventar un recibo nuevo. */
    public Integer registrarPagoCuentaCorriente(Integer idCuentaCorriente, Integer idMetodoPago,
            BigDecimal monto, Usuario usuario, String claveOperacion) {
        if (idCuentaCorriente == null) throw new IllegalArgumentException("La cuenta corriente es requerida.");
        if (idMetodoPago == null) throw new IllegalArgumentException("El medio de pago es requerido.");
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        if (usuario == null || usuario.getIdUsuario() == null) throw new IllegalArgumentException("El usuario es requerido.");
        String clave = normalizarClaveOperacion(claveOperacion);

        Object[] resultado = tx.runInTx(em -> {
            if (clave != null) {
                Long repetida = em.createQuery("SELECT COUNT(m) FROM CajaMovimiento m WHERE m.claveOperacion = :clave", Long.class)
                        .setParameter("clave", clave).getSingleResult();
                if (repetida != null && repetida > 0L) throw new IllegalStateException("Esta operación de cobro ya fue registrada.");
            }
            CajaSesion sesion = em.createQuery("SELECT s FROM CajaSesion s WHERE s.estado = :estado ORDER BY s.fechaApertura DESC", CajaSesion.class)
                    .setParameter("estado", CajaSesion.Estado.ABIERTA).setMaxResults(1).getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de registrar un cobro."));
            Usuario u = em.find(Usuario.class, usuario.getIdUsuario());
            if (u == null || !u.isActivo()) throw new IllegalStateException("El usuario no está activo.");
            MetodoPago metodo = em.find(MetodoPago.class, idMetodoPago);
            if (metodo == null || !metodo.isActivo()) throw new IllegalStateException("El medio de pago no está activo.");
            if (esCuentaCorriente(metodo.getNombre())) throw new IllegalArgumentException("Cuenta Corriente no es un medio de pago.");
            CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente, LockModeType.PESSIMISTIC_WRITE);
            if (cc == null || "INACTIVO".equalsIgnoreCase(cc.getEstado())) throw new IllegalStateException("La cuenta corriente no está disponible.");
            BigDecimal saldo = cc.getSaldoActual() == null ? BigDecimal.ZERO : cc.getSaldoActual();
            BigDecimal deuda = saldo.signum() < 0 ? saldo.abs() : BigDecimal.ZERO;
            if (deuda.signum() == 0) throw new IllegalStateException("La cuenta corriente no registra deuda.");
            if (monto.compareTo(deuda) > 0) throw new IllegalStateException("El pago supera el saldo adeudado actual.");

            CuentaCorrienteMovimiento movCC = new CuentaCorrienteMovimiento();
            movCC.setCuentaCorriente(cc); movCC.setFechaMovimiento(LocalDate.now());
            movCC.setDescripcion("Pago de cuenta corriente"); movCC.setTipoMovimiento(CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
            movCC.setMonto(monto); movCC.setSaldoResultante(saldo.add(monto)); em.persist(movCC); em.flush();

            CajaMovimiento movCaja = new CajaMovimiento();
            movCaja.setCajaSesion(sesion); movCaja.setMonto(monto); movCaja.setTipoMovimiento(CajaMovimiento.TipoMovimiento.CREDITO);
            movCaja.setFecha(new Date()); movCaja.setFechaHora(new Date()); movCaja.setUsuario(u); movCaja.setMetodoPago(metodo);
            movCaja.setAfectaEfectivo(metodo.isAfectaEfectivo()); movCaja.setDescripcion("Cobro cuenta corriente N.º " + movCC.getIdMovimiento());
            movCaja.setClaveOperacion(clave); em.persist(movCaja);

            cc.setSaldoActual(saldo.add(monto)); cc.setUltimaEdicion(LocalDate.now()); em.merge(cc);
            Integer idMovimiento = movCC.getIdMovimiento();
            return new Object[]{idMovimiento, metodo.getNombre()};
        });

        Integer idMovimiento = (Integer) resultado[0];
        String metodoPago = (String) resultado[1];
        registrarAuditoriaCobro(idMovimiento, idCuentaCorriente, monto, metodoPago, usuario);
        return idMovimiento;
    }

    private void registrarAuditoriaCobro(Integer idMovimiento, Integer idCuentaCorriente,
            BigDecimal monto, String metodoPago, Usuario usuario) {
        try {
            veterinaria.util.AuditoriaLogger.evento("CUENTA_CORRIENTE_COBRO",
                    "movimientoId=" + idMovimiento + " cuentaId=" + idCuentaCorriente
                    + " monto=" + monto + " medioPago=" + metodoPago, usuario);
        } catch (Exception ignore) {
            // La auditoría auxiliar nunca debe romper una operación financiera ya confirmada.
        }
    }

    private boolean esCuentaCorriente(String nombre) {
        if (nombre == null) return false;
        String n = java.text.Normalizer.normalize(nombre.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.contains("cuenta corriente") || n.contains("cta cte");
    }

    private String normalizarClaveOperacion(String claveOperacion) {
        if (claveOperacion == null || claveOperacion.trim().isEmpty()) {
            return null;
        }
        String clave = claveOperacion.trim();
        if (clave.length() > 48) {
            throw new IllegalArgumentException("La clave de operacion es demasiado larga.");
        }
        if (!clave.matches("[A-Za-z0-9-]+")) {
            throw new IllegalArgumentException("La clave de operacion contiene caracteres no permitidos.");
        }
        return clave;
    }
}
