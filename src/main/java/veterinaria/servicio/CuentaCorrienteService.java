package veterinaria.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;

public class CuentaCorrienteService {

    private final TxRunner tx = new TxRunner();

    public boolean crearCuentaCorriente(CuentaCorriente cuentaCorriente) {
        return tx.runInTx(em -> {
            if (cuentaCorriente == null || cuentaCorriente.getCliente() == null
                    || cuentaCorriente.getCliente().getIdCliente() == null) {
                throw new IllegalArgumentException("Debe seleccionar un cliente.");
            }
            veterinaria.entidad.Cliente cliente = em.find(veterinaria.entidad.Cliente.class,
                    cuentaCorriente.getCliente().getIdCliente());
            if (cliente == null || !cliente.isActivo()) {
                throw new IllegalStateException("No se puede abrir una cuenta corriente para un cliente inactivo.");
            }
            em.persist(cuentaCorriente);
            return true;
        });
    }

    public boolean actualizarCuentaCorriente(CuentaCorriente cuentaCorriente) {
        if (cuentaCorriente == null || cuentaCorriente.getIdCuentaCorriente() == null) {
            throw new IllegalArgumentException("Debe indicar una cuenta corriente existente.");
        }
        return tx.runInTx(em -> {
            CuentaCorriente actual = em.find(CuentaCorriente.class,
                    cuentaCorriente.getIdCuentaCorriente(), LockModeType.PESSIMISTIC_WRITE);
            if (actual == null) {
                return false;
            }
            // La edición general no puede cambiar el estado ni el saldo inicial:
            // ambos afectan el historial y las reglas de desactivación.
            BigDecimal nuevoSaldoInicial = cuentaCorriente.getSaldoInicial();
            BigDecimal saldoInicialActual = actual.getSaldoInicial();
            if (nuevoSaldoInicial == null || saldoInicialActual == null
                    || nuevoSaldoInicial.compareTo(saldoInicialActual) != 0) {
                throw new IllegalStateException("El saldo inicial no puede modificarse desde la edición.");
            }
            if (cuentaCorriente.getEstado() == null
                    || !cuentaCorriente.getEstado().equalsIgnoreCase(actual.getEstado())) {
                throw new IllegalStateException("Para cambiar el estado use Desactivar o Reactivar.");
            }
            if (cuentaCorriente.getCliente() == null
                    || !actual.getCliente().getIdCliente().equals(
                            cuentaCorriente.getCliente().getIdCliente())) {
                throw new IllegalStateException("No se puede cambiar el cliente de una cuenta existente.");
            }
            veterinaria.util.SesionUsuario sesion = veterinaria.util.SesionUsuario.getInstancia();
            veterinaria.entidad.Usuario usuario = sesion.getUsuario();
            if (usuario == null || usuario.getRol() == null
                    || !("Administrador".equalsIgnoreCase(usuario.getRol().getNombreRol())
                    || Integer.valueOf(1).equals(usuario.getRol().getIdRol()))) {
                throw new SecurityException("Solo un usuario con rol Administrador puede modificar el límite de crédito.");
            }
            BigDecimal saldo = recalcularSaldoDesdeMovimientos(em, actual.getIdCuentaCorriente());
            if (saldo.signum() < 0) {
                throw new IllegalStateException("No se puede editar la cuenta seleccionada porque posee deuda pendiente.");
            }
            if (saldo.signum() > 0) {
                throw new IllegalStateException("No se puede editar la cuenta seleccionada porque tiene saldo a favor.");
            }
            BigDecimal limite = cuentaCorriente.getLimiteCredito();
            if (limite == null || limite.signum() < 0) {
                throw new IllegalArgumentException("El límite de crédito debe ser mayor o igual a cero.");
            }
            if (!"ACTIVO".equalsIgnoreCase(actual.getEstado())
                    && !"ACTIVA".equalsIgnoreCase(actual.getEstado())) {
                throw new IllegalStateException("Debe reactivar la cuenta antes de modificar su límite.");
            }
            BigDecimal limiteAnterior = actual.getLimiteCredito();
            if (limiteAnterior == null || limiteAnterior.compareTo(limite) != 0) {
                veterinaria.entidad.Auditoria auditoria = new veterinaria.entidad.Auditoria();
                auditoria.setFechaHora(java.time.LocalDateTime.now());
                auditoria.setUsuario(em.getReference(veterinaria.entidad.Usuario.class, usuario.getIdUsuario()));
                auditoria.setAccion("MODIFICAR_LIMITE_CC");
                auditoria.setEntidad("CuentaCorriente");
                auditoria.setEntidadId(actual.getIdCuentaCorriente().longValue());
                auditoria.setModulo("Cuenta Corriente");
                auditoria.setDescripcion("Modificación del límite de crédito de la cuenta N° "
                        + actual.getIdCuentaCorriente());
                auditoria.setResultado("OK");
                auditoria.setDatosAntes("limiteCredito=" + limiteAnterior);
                auditoria.setDatosDespues("limiteCredito=" + limite);
                em.persist(auditoria);
            }
            actual.setLimiteCredito(limite);
            actual.setUltimaEdicion(LocalDate.now());
            return true;
        });
    }

    public boolean desactivarCuentaCorriente(Integer idCuentaCorriente) {
        return tx.runInTx(em -> {
            CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente);
            if (cc == null) return false;
            cc = em.find(CuentaCorriente.class, idCuentaCorriente, LockModeType.PESSIMISTIC_WRITE);
            BigDecimal saldo = recalcularSaldoDesdeMovimientos(em, idCuentaCorriente);
            if (saldo.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalStateException("No se puede desactivar: la cuenta tiene saldo pendiente o a favor.");
            }
            cc.setEstado("INACTIVO");
            cc.setUltimaEdicion(LocalDate.now());
            em.merge(cc);
            return true;
        });
    }

    public boolean reactivarCuentaCorriente(Integer idCuentaCorriente) {
        return tx.runInTx(em -> {
            CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente);
            if (cc == null) return false;
            veterinaria.entidad.Cliente cliente = em.find(veterinaria.entidad.Cliente.class,
                    cc.getCliente().getIdCliente());
            if (cliente == null || !cliente.isActivo()) {
                throw new IllegalStateException("No se puede reactivar una cuenta de un cliente inactivo.");
            }
            cc.setEstado("ACTIVO");
            cc.setUltimaEdicion(LocalDate.now());
            em.merge(cc);
            return true;
        });
    }

    public boolean registrarMovimientoYActualizarSaldo(CuentaCorrienteMovimiento movimiento) {
        return tx.runInTx(em -> {
            CuentaCorriente cc = resolveCuentaCorrienteManaged(em, movimiento);
            validarCuentaActiva(cc);
            validarMovimiento(movimiento);

            BigDecimal saldoActual = obtenerSaldoActualSeguro(em, cc);
            BigDecimal monto = normalizarMonto(movimiento.getMonto());
            CuentaCorrienteMovimiento.TipoMovimiento tipo = movimiento.getTipoMovimiento();

            BigDecimal saldoResultante;
            switch (tipo) {
                case DEBITO:
                    saldoResultante = saldoActual.subtract(monto);
                    validarLimiteCredito(cc, saldoResultante);
                    break;
                case CREDITO:
                    saldoResultante = saldoActual.add(monto);
                    break;
                default:
                    throw new IllegalArgumentException("tipoMovimiento inválido (DEBITO/CREDITO): " + tipo);
            }

            movimiento.setCuentaCorriente(cc);
            if (movimiento.getFechaMovimiento() == null) {
                movimiento.setFechaMovimiento(LocalDate.now());
            }
            movimiento.setSaldoResultante(saldoResultante);

            em.persist(movimiento);

            cc.setSaldoActual(saldoResultante);
            cc.setUltimaEdicion(LocalDate.now());
            em.merge(cc);

            return true;
        });
    }

    /**
     * Compatibilidad con las pantallas actuales. Un movimiento financiero no
     * se elimina: se genera un movimiento inverso que conserva la trazabilidad.
     */
    @Deprecated
    public boolean eliminarMovimientoYRecalcular(Integer idMovimiento) {
        return revertirMovimiento(idMovimiento, "Reversión solicitada desde operación de baja heredada");
    }

    public boolean revertirMovimiento(Integer idMovimiento, String motivo) {
        if (idMovimiento == null) {
            throw new IllegalArgumentException("El movimiento es requerido.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de la reversión es obligatorio.");
        }

        return tx.runInTx(em -> {
            CuentaCorrienteMovimiento original = em.find(
                    CuentaCorrienteMovimiento.class, idMovimiento, LockModeType.PESSIMISTIC_WRITE);
            if (original == null) {
                return false;
            }

            Long reversiones = em.createQuery(
                    "SELECT COUNT(m) FROM CuentaCorrienteMovimiento m "
                    + "WHERE m.movimientoRevertido.idMovimiento = :id",
                    Long.class)
                    .setParameter("id", idMovimiento)
                    .getSingleResult();
            if (reversiones != null && reversiones.longValue() > 0L) {
                throw new IllegalStateException("El movimiento ya fue revertido.");
            }

            CuentaCorriente cc = resolveCuentaCorrienteManaged(em, original);
            validarCuentaActiva(cc);

            CuentaCorrienteMovimiento reversion = new CuentaCorrienteMovimiento();
            reversion.setCuentaCorriente(cc);
            reversion.setFechaMovimiento(LocalDate.now());
            reversion.setDescripcion("REVERSIÓN: " + (original.getDescripcion() == null ? "" : original.getDescripcion()));
            reversion.setTipoMovimiento(
                    original.getTipoMovimiento() == CuentaCorrienteMovimiento.TipoMovimiento.DEBITO
                            ? CuentaCorrienteMovimiento.TipoMovimiento.CREDITO
                            : CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
            reversion.setMonto(normalizarMonto(original.getMonto()));
            reversion.setMovimientoRevertido(original);
            reversion.setMotivoReversion(motivo.trim());

            em.persist(reversion);
            em.flush();
            recomputarSaldosCuenta(em, cc);
            return true;
        });
    }

    /**
     * Los movimientos confirmados de cuenta corriente son inmutables.
     * Para corregirlos debe registrarse una reversion con motivo.
     */
    @Deprecated
    public boolean actualizarMovimientoYRecalcular(CuentaCorrienteMovimiento movimiento) {
        throw new UnsupportedOperationException(
                "Los movimientos confirmados no pueden editarse. Use revertirMovimiento().");
    }

    /**
     * Sincroniza el saldo almacenado con el saldo inicial y los movimientos.
     * No modifica ni elimina movimientos históricos.
     */
    public boolean sincronizarSaldo(Integer idCuentaCorriente) {
        if (idCuentaCorriente == null) {
            return false;
        }
        return tx.runInTx(em -> {
            CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente,
                    LockModeType.PESSIMISTIC_WRITE);
            if (cc == null) {
                return false;
            }
            BigDecimal saldo = recalcularSaldoDesdeMovimientos(em, idCuentaCorriente);
            cc.setSaldoActual(saldo);
            cc.setUltimaEdicion(LocalDate.now());
            return true;
        });
    }

    public BigDecimal recalcularSaldo(Integer idCuentaCorriente) {
        return tx.runInTx(em -> recalcularSaldoDesdeMovimientos(em, idCuentaCorriente));
    }

    private void validarCuentaActiva(CuentaCorriente cc) {
        if (cc == null) {
            throw new IllegalStateException("CuentaCorriente inexistente.");
        }
        String estado = cc.getEstado();
        if (estado != null && "INACTIVO".equalsIgnoreCase(estado.trim())) {
            throw new IllegalStateException("La cuenta corriente está inactiva.");
        }
    }

    private void validarLimiteCredito(CuentaCorriente cc, BigDecimal saldoResultante) {
        BigDecimal limite = cc.getLimiteCredito() == null ? BigDecimal.ZERO : cc.getLimiteCredito();
        if (limite.signum() < 0) {
            throw new IllegalStateException("El limite de credito no puede ser negativo.");
        }
        BigDecimal deuda = saldoResultante.signum() < 0 ? saldoResultante.abs() : BigDecimal.ZERO;
        if (deuda.compareTo(limite) > 0) {
            throw new IllegalStateException("El movimiento supera el limite de credito disponible.");
        }
    }

    private void validarMovimiento(CuentaCorrienteMovimiento movimiento) {
        if (movimiento == null) {
            throw new IllegalArgumentException("Movimiento requerido.");
        }
        if (movimiento.getTipoMovimiento() == null) {
            throw new IllegalArgumentException("tipoMovimiento inválido (DEBITO/CREDITO): null");
        }
        normalizarMonto(movimiento.getMonto());
    }

    private BigDecimal normalizarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("Monto requerido.");
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        return monto;
    }

    private void recomputarSaldosCuenta(EntityManager em, CuentaCorriente cc) {
        CuentaCorriente managedCc = em.find(CuentaCorriente.class, cc.getIdCuentaCorriente(), LockModeType.PESSIMISTIC_WRITE);
        if (managedCc == null) {
            throw new IllegalStateException("No existe CuentaCorriente con id=" + cc.getIdCuentaCorriente());
        }

        BigDecimal saldo = (managedCc.getSaldoInicial() != null) ? managedCc.getSaldoInicial() : BigDecimal.ZERO;

        TypedQuery<CuentaCorrienteMovimiento> q = em.createQuery(
                "SELECT m FROM CuentaCorrienteMovimiento m "
                + "WHERE m.idCuentaCorriente.idCuentaCorriente = :idCC "
                + "ORDER BY m.fechaMovimiento ASC, m.idMovimiento ASC",
                CuentaCorrienteMovimiento.class
        );
        q.setParameter("idCC", managedCc.getIdCuentaCorriente());

        for (CuentaCorrienteMovimiento m : q.getResultList()) {
            BigDecimal monto = (m.getMonto() == null) ? BigDecimal.ZERO : m.getMonto();
            if (m.getTipoMovimiento() == CuentaCorrienteMovimiento.TipoMovimiento.DEBITO) {
                saldo = saldo.subtract(monto);
            } else if (m.getTipoMovimiento() == CuentaCorrienteMovimiento.TipoMovimiento.CREDITO) {
                saldo = saldo.add(monto);
            }
            m.setSaldoResultante(saldo);
            em.merge(m);
        }

        managedCc.setSaldoActual(saldo);
        managedCc.setUltimaEdicion(LocalDate.now());
        em.merge(managedCc);
    }

    private CuentaCorriente resolveCuentaCorrienteManaged(EntityManager em, CuentaCorrienteMovimiento movimiento) {
        if (movimiento == null || movimiento.getCuentaCorriente() == null || movimiento.getCuentaCorriente().getIdCuentaCorriente() == null) {
            throw new IllegalArgumentException("Debe indicar cuentaCorriente.idCuentaCorriente en el movimiento.");
        }
        CuentaCorriente cc = em.find(CuentaCorriente.class, movimiento.getCuentaCorriente().getIdCuentaCorriente(), LockModeType.PESSIMISTIC_WRITE);
        if (cc == null) {
            throw new IllegalStateException("No existe CuentaCorriente con id=" + movimiento.getCuentaCorriente().getIdCuentaCorriente());
        }
        return cc;
    }

    private BigDecimal obtenerSaldoActualSeguro(EntityManager em, CuentaCorriente cc) {
        if (cc.getSaldoActual() != null) {
            return cc.getSaldoActual();
        }

        try {
            TypedQuery<BigDecimal> query = em.createQuery(
                "SELECT m.saldoResultante FROM CuentaCorrienteMovimiento m " +
                "WHERE m.idCuentaCorriente.idCuentaCorriente = :idCC " +
                "ORDER BY m.fechaMovimiento DESC, m.idMovimiento DESC",
                BigDecimal.class
            );
            query.setParameter("idCC", cc.getIdCuentaCorriente());
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return (cc.getSaldoInicial() != null) ? cc.getSaldoInicial() : BigDecimal.ZERO;
        }
    }

    private BigDecimal recalcularSaldoDesdeMovimientos(EntityManager em, Integer idCuentaCorriente) {
        CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente);
        BigDecimal saldoInicial = (cc != null && cc.getSaldoInicial() != null) ? cc.getSaldoInicial() : BigDecimal.ZERO;

        TypedQuery<BigDecimal> qDeb = em.createQuery(
                "SELECT COALESCE(SUM(m.monto), 0) FROM CuentaCorrienteMovimiento m " +
                "WHERE m.tipoMovimiento = :deb AND m.idCuentaCorriente.idCuentaCorriente = :idCC",
                BigDecimal.class
        );
        qDeb.setParameter("idCC", idCuentaCorriente);
        qDeb.setParameter("deb", CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
        BigDecimal debitos = qDeb.getSingleResult();

        TypedQuery<BigDecimal> qCre = em.createQuery(
                "SELECT COALESCE(SUM(m.monto), 0) FROM CuentaCorrienteMovimiento m " +
                "WHERE m.tipoMovimiento = :cre AND m.idCuentaCorriente.idCuentaCorriente = :idCC",
                BigDecimal.class
        );
        qCre.setParameter("idCC", idCuentaCorriente);
        qCre.setParameter("cre", CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
        BigDecimal creditos = qCre.getSingleResult();

        return saldoInicial.add(creditos).subtract(debitos);
    }
}
