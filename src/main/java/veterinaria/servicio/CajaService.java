package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaMovimiento.TipoMovimiento;
import veterinaria.entidad.Usuario;

public class CajaService {

    private final TxRunner tx = new TxRunner();

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

    private CajaMovimiento findLast(EntityManager em, CajaMovimiento.TipoMovimiento tipo) {
        TypedQuery<CajaMovimiento> q = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.eliminado = false " +
                "ORDER BY c.fecha DESC, c.idMovimiento DESC",
                CajaMovimiento.class
        );
        q.setParameter("tipo", tipo);
        q.setMaxResults(1);
        try {
            return q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal sumByDateAndTipo(EntityManager em, Date fecha, CajaMovimiento.TipoMovimiento tipo) {
        TypedQuery<BigDecimal> q = em.createQuery(
                "SELECT COALESCE(SUM(c.monto), 0) FROM CajaMovimiento c " +
                "WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                BigDecimal.class
        );
        q.setParameter("tipo", tipo);
        q.setParameter("fecha", onlyDate(fecha));
        BigDecimal r = q.getSingleResult();
        return r == null ? BigDecimal.ZERO : r;
    }

    private CajaMovimiento findAperturaByDate(EntityManager em, Date fecha) {
        TypedQuery<CajaMovimiento> q = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                CajaMovimiento.class
        );
        q.setParameter("tipo", CajaMovimiento.TipoMovimiento.APERTURA);
        q.setParameter("fecha", onlyDate(fecha));
        q.setMaxResults(1);
        try {
            return q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCajaAbiertaSinCierre(EntityManager em) {
        CajaMovimiento lastApertura = findLast(em, CajaMovimiento.TipoMovimiento.APERTURA);
        if (lastApertura == null) return false;

        CajaMovimiento lastCierre = findLast(em, CajaMovimiento.TipoMovimiento.CIERRE);
        if (lastCierre == null) return true;

        return lastApertura.getFecha().after(lastCierre.getFecha());
    }

    private Date getFechaCajaPendiente(EntityManager em) {
        CajaMovimiento lastApertura = findLast(em, CajaMovimiento.TipoMovimiento.APERTURA);
        if (lastApertura == null) return null;
        CajaMovimiento lastCierre = findLast(em, CajaMovimiento.TipoMovimiento.CIERRE);
        if (lastCierre == null) return onlyDate(lastApertura.getFecha());
        if (lastApertura.getFecha().after(lastCierre.getFecha())) {
            return onlyDate(lastApertura.getFecha());
        }
        return null;
    }

    public boolean registrarMovimiento(CajaMovimiento movimiento) {
        return tx.runInTx(em -> {
            CajaMovimiento m = movimiento;

            if (m.getUsuario() != null && m.getUsuario().getIdUsuario() != null) {
                Usuario usuarioMG = em.find(Usuario.class, m.getUsuario().getIdUsuario());
                m.setUsuario(usuarioMG);
            }

            em.persist(m);
            return true;
        });
    }

    public boolean modificarMovimiento(CajaMovimiento movimiento) {
        return tx.runInTx(em -> {
            CajaMovimiento existente = em.find(CajaMovimiento.class, movimiento.getIdMovimiento());
            if (existente == null) {
                return false;
            }
            existente.setMonto(movimiento.getMonto());
            existente.setTipoMovimiento(movimiento.getTipoMovimiento());
            existente.setFecha(movimiento.getFecha());
            existente.setDescripcion(movimiento.getDescripcion());
            existente.setEliminado(movimiento.isEliminado());

            if (movimiento.getUsuario() != null && movimiento.getUsuario().getIdUsuario() != null) {
                Usuario usuarioMG = em.find(Usuario.class, movimiento.getUsuario().getIdUsuario());
                existente.setUsuario(usuarioMG);
            } else {
                existente.setUsuario(null);
            }

            em.merge(existente);
            return true;
        });
    }

    public boolean existeMovimientoEnFecha(Date fecha, TipoMovimiento tipo) {
        return tx.runInTx(em -> existeMovimientoEnFecha(em, onlyDate(fecha), tipo));
    }

    private boolean existeMovimientoEnFecha(EntityManager em, Date fecha, TipoMovimiento tipo) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM CajaMovimiento c " +
                "WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                Long.class
        );
        query.setParameter("tipo", tipo);
        query.setParameter("fecha", onlyDate(fecha));
        return query.getSingleResult() > 0;
    }

    public boolean registrarApertura(BigDecimal montoInicial, Usuario usuario, Date fecha) {
        return tx.runInTx(em -> {
            Date f = onlyDate(fecha);

            if (isCajaAbiertaSinCierre(em)) {
                Date pendiente = getFechaCajaPendiente(em);
                throw new IllegalStateException(
                        "Existe una caja ABIERTA sin cierre (" + pendiente + "). Debe cerrar la caja antes de abrir nuevamente."
                );
            }

            if (existeMovimientoEnFecha(em, f, TipoMovimiento.APERTURA)) {
                throw new IllegalStateException("La caja ya fue abierta en la fecha indicada.");
            }
            if (existeMovimientoEnFecha(em, f, TipoMovimiento.CIERRE)) {
                throw new IllegalStateException("La caja ya fue cerrada en la fecha indicada.");
            }

            CajaMovimiento apertura = new CajaMovimiento();
            apertura.setMonto(montoInicial);
            apertura.setTipoMovimiento(TipoMovimiento.APERTURA);
            apertura.setFecha(f);
            apertura.setDescripcion("Apertura de caja");

            if (usuario != null && usuario.getIdUsuario() != null) {
                Usuario usuarioMG = em.find(Usuario.class, usuario.getIdUsuario());
                apertura.setUsuario(usuarioMG);
            } else {
                apertura.setUsuario(null);
            }

            em.persist(apertura);
            return true;
        });
    }

    public boolean registrarCierre(BigDecimal montoFinal, Usuario usuario, Date fecha) {
        return tx.runInTx(em -> {
            Date f = onlyDate(fecha);

            if (existeMovimientoEnFecha(em, f, TipoMovimiento.CIERRE)) {
                throw new IllegalStateException("La caja ya fue cerrada en la fecha indicada.");
            }

            CajaMovimiento apertura = findAperturaByDate(em, f);
            if (apertura == null) {
                throw new IllegalStateException("No existe apertura de caja para la fecha indicada. No se puede cerrar.");
            }

            BigDecimal ingresos = sumByDateAndTipo(em, f, CajaMovimiento.TipoMovimiento.CREDITO);
            BigDecimal egresos = sumByDateAndTipo(em, f, CajaMovimiento.TipoMovimiento.DEBITO);
            BigDecimal esperado = apertura.getMonto().add(ingresos).subtract(egresos);

            if (montoFinal == null) {
                throw new IllegalStateException("Debe ingresar el monto físico de cierre.");
            }
            if (montoFinal.compareTo(esperado) != 0) {
                BigDecimal diferencia = montoFinal.subtract(esperado);
                throw new IllegalStateException(
                        "El monto físico NO coincide con el esperado. Esperado: " + esperado +
                        " | Ingresado: " + montoFinal +
                        " | Diferencia: " + diferencia +
                        ". Corrija el monto antes de cerrar la caja."
                );
            }

            CajaMovimiento cierre = new CajaMovimiento();
            cierre.setMonto(montoFinal);
            cierre.setTipoMovimiento(TipoMovimiento.CIERRE);
            cierre.setFecha(f);
            cierre.setDescripcion("Cierre de caja");

            if (usuario != null && usuario.getIdUsuario() != null) {
                Usuario usuarioMG = em.find(Usuario.class, usuario.getIdUsuario());
                cierre.setUsuario(usuarioMG);
            } else {
                cierre.setUsuario(null);
            }

            em.persist(cierre);
            return true;
        });
    }

    public boolean registrarCierrePorAperturaId(BigDecimal montoFinal, Usuario usuario, Long idApertura) {
        return tx.runInTx(em -> {
            if (idApertura == null) {
                throw new IllegalStateException("No se indicó la apertura a cerrar.");
            }

            CajaMovimiento apertura = em.find(CajaMovimiento.class, idApertura);
            if (apertura == null || apertura.isEliminado() || apertura.getTipoMovimiento() != TipoMovimiento.APERTURA) {
                throw new IllegalStateException("No existe una apertura válida para cerrar (id=" + idApertura + ").");
            }

            Date f = onlyDate(apertura.getFecha());

            if (existeMovimientoEnFecha(em, f, TipoMovimiento.CIERRE)) {
                throw new IllegalStateException("La caja ya fue cerrada en la fecha indicada.");
            }

            BigDecimal ingresos = sumByDateAndTipo(em, f, CajaMovimiento.TipoMovimiento.CREDITO);
            BigDecimal egresos = sumByDateAndTipo(em, f, CajaMovimiento.TipoMovimiento.DEBITO);
            BigDecimal esperado = apertura.getMonto().add(ingresos).subtract(egresos);

            if (montoFinal == null) {
                throw new IllegalStateException("Debe ingresar el monto físico de cierre.");
            }
            if (montoFinal.compareTo(esperado) != 0) {
                BigDecimal diferencia = montoFinal.subtract(esperado);
                throw new IllegalStateException(
                        "El monto físico NO coincide con el esperado. Esperado: " + esperado +
                        " | Ingresado: " + montoFinal +
                        " | Diferencia: " + diferencia +
                        ". Corrija el monto antes de cerrar la caja."
                );
            }

            CajaMovimiento cierre = new CajaMovimiento();
            cierre.setMonto(montoFinal);
            cierre.setTipoMovimiento(TipoMovimiento.CIERRE);
            cierre.setFecha(f);
            cierre.setDescripcion("Cierre de caja");

            if (usuario != null && usuario.getIdUsuario() != null) {
                Usuario usuarioMG = em.find(Usuario.class, usuario.getIdUsuario());
                cierre.setUsuario(usuarioMG);
            } else {
                cierre.setUsuario(null);
            }

            em.persist(cierre);
            return true;
        });
    }

    public boolean registrarCierreConAjuste(BigDecimal montoFisico, Usuario usuario, Date fecha, String motivo) {
        return tx.runInTx(em -> {
            Date f = onlyDate(fecha);

            if (existeMovimientoEnFecha(em, f, TipoMovimiento.CIERRE)) {
                throw new IllegalStateException("La caja ya fue cerrada en la fecha indicada.");
            }

            CajaMovimiento apertura = findAperturaByDate(em, f);
            if (apertura == null) {
                throw new IllegalStateException("No existe apertura de caja para la fecha indicada. No se puede cerrar.");
            }

            if (montoFisico == null) {
                throw new IllegalStateException("Debe ingresar el monto físico de cierre.");
            }

            String mot = motivo == null ? "" : motivo.trim();
            if (mot.isEmpty()) {
                throw new IllegalStateException("Debe ingresar un motivo para el ajuste de arqueo.");
            }

            BigDecimal ingresos = sumByDateAndTipo(em, f, CajaMovimiento.TipoMovimiento.CREDITO);
            BigDecimal egresos = sumByDateAndTipo(em, f, CajaMovimiento.TipoMovimiento.DEBITO);
            BigDecimal esperado = apertura.getMonto().add(ingresos).subtract(egresos);

            BigDecimal diferencia = montoFisico.subtract(esperado);

            if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
                CajaMovimiento ajuste = new CajaMovimiento();
                ajuste.setFecha(f);
                ajuste.setDescripcion("AJUSTE ARQUEO: " + mot);

                if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                    ajuste.setTipoMovimiento(TipoMovimiento.CREDITO);
                    ajuste.setMonto(diferencia);
                } else {
                    ajuste.setTipoMovimiento(TipoMovimiento.DEBITO);
                    ajuste.setMonto(diferencia.abs());
                }

                if (usuario != null && usuario.getIdUsuario() != null) {
                    Usuario usuarioMG = em.find(Usuario.class, usuario.getIdUsuario());
                    ajuste.setUsuario(usuarioMG);
                } else {
                    ajuste.setUsuario(null);
                }

                ajuste.setMetodoPago(null);
                ajuste.setRecibo(null);

                em.persist(ajuste);
            }

            CajaMovimiento cierre = new CajaMovimiento();
            cierre.setMonto(montoFisico);
            cierre.setTipoMovimiento(TipoMovimiento.CIERRE);
            cierre.setFecha(f);
            cierre.setDescripcion("Cierre de caja");

            if (usuario != null && usuario.getIdUsuario() != null) {
                Usuario usuarioMG = em.find(Usuario.class, usuario.getIdUsuario());
                cierre.setUsuario(usuarioMG);
            } else {
                cierre.setUsuario(null);
            }

            em.persist(cierre);
            return true;
        });
    }
}
