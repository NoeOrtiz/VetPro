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
            em.persist(cuentaCorriente);
            return true;
        });
    }

    public boolean actualizarCuentaCorriente(CuentaCorriente cuentaCorriente) {
        return tx.runInTx(em -> {
            em.merge(cuentaCorriente);
            return true;
        });
    }

    public boolean desactivarCuentaCorriente(Integer idCuentaCorriente) {
        return tx.runInTx(em -> {
            CuentaCorriente cc = em.find(CuentaCorriente.class, idCuentaCorriente);
            if (cc == null) return false;
            cc.setEstado("INACTIVO");
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

    public boolean actualizarMovimientoYRecalcular(CuentaCorrienteMovimiento movimiento) {
        return tx.runInTx(em -> {
            if (movimiento == null || movimiento.getIdMovimiento() == null) {
                return false;
            }

            CuentaCorrienteMovimiento managed = em.find(CuentaCorrienteMovimiento.class, movimiento.getIdMovimiento());
            if (managed == null) {
                return false;
            }

            CuentaCorriente cc = resolveCuentaCorrienteManaged(em, movimiento);
            validarCuentaActiva(cc);
            validarMovimiento(movimiento);

            managed.setCuentaCorriente(cc);
            managed.setFechaMovimiento(movimiento.getFechaMovimiento() != null ? movimiento.getFechaMovimiento() : LocalDate.now());
            managed.setDescripcion(movimiento.getDescripcion());
            managed.setTipoMovimiento(movimiento.getTipoMovimiento());
            managed.setMonto(normalizarMonto(movimiento.getMonto()));
            managed.setRecibo(movimiento.getRecibo());

            em.merge(managed);
            em.flush();

            recomputarSaldosCuenta(em, cc);
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
