package veterinaria.persistencia;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.servicio.CuentaCorrienteService;

public class CuentaCorrienteMovimientoDAO {

    private final CuentaCorrienteService cuentaCorrienteService = new CuentaCorrienteService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(CuentaCorrienteMovimiento movimientoCC) {
        try {
            return cuentaCorrienteService.registrarMovimientoYActualizarSaldo(movimientoCC);
        } catch (Exception e) {
            System.out.println("Error al crear nuevo movimiento de cuenta corriente: " + e.getMessage());
            return false;
        }
    }

    public CuentaCorrienteMovimiento buscarPorId(Integer idCC) {
        EntityManager em = getEntityManager();
        try {
            return em.find(CuentaCorrienteMovimiento.class, idCC);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CuentaCorrienteMovimiento> buscarTodos() {
        EntityManager em = getEntityManager();
        List<CuentaCorrienteMovimiento> listaCuentas = new ArrayList<>();
        try {
            TypedQuery<CuentaCorrienteMovimiento> query = em.createQuery(
                    "SELECT u FROM CuentaCorrienteMovimiento u ORDER BY u.fechaMovimiento ASC, u.idMovimiento ASC",
                    CuentaCorrienteMovimiento.class
            );
            listaCuentas = query.getResultList();
        } catch (Exception e) {
            System.out.println("Error al buscar todos los movimientos de cuenta corriente: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return listaCuentas;
    }

    public boolean actualizar(CuentaCorrienteMovimiento movimientoCC) {
        try {
            return cuentaCorrienteService.actualizarMovimientoYRecalcular(movimientoCC);
        } catch (Exception e) {
            System.out.println("Error al actualizar un movimiento de cuenta corriente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(CuentaCorrienteMovimiento movimientoCC) {
        try {
            return cuentaCorrienteService.eliminarMovimientoYRecalcular(movimientoCC.getIdMovimiento());
        } catch (Exception e) {
            System.out.println("Error al eliminar movimiento de cuenta corriente: " + e.getMessage());
            return false;
        }
    }

    public BigDecimal sumarDebitos(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<BigDecimal> query = em.createQuery(
                "SELECT COALESCE(SUM(m.monto), 0) FROM CuentaCorrienteMovimiento m WHERE m.tipoMovimiento = :tipo AND m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente",
                BigDecimal.class
            );
            query.setParameter("idCuentaCorriente", idCuentaCorriente);
            query.setParameter("tipo", CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
            return query.getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public BigDecimal sumarCreditos(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<BigDecimal> query = em.createQuery(
                "SELECT COALESCE(SUM(m.monto), 0) FROM CuentaCorrienteMovimiento m WHERE m.tipoMovimiento = :tipo AND m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente",
                BigDecimal.class
            );
            query.setParameter("idCuentaCorriente", idCuentaCorriente);
            query.setParameter("tipo", CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
            return query.getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CuentaCorrienteMovimiento obtenerUltimoCredito(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorrienteMovimiento> query = em.createQuery(
                "SELECT m FROM CuentaCorrienteMovimiento m WHERE m.tipoMovimiento = :tipo AND m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente ORDER BY m.fechaMovimiento DESC, m.idMovimiento DESC",
                CuentaCorrienteMovimiento.class
            );
            query.setParameter("idCuentaCorriente", idCuentaCorriente);
            query.setParameter("tipo", CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CuentaCorrienteMovimiento obtenerUltimoDebito(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CuentaCorrienteMovimiento> query = em.createQuery(
                "SELECT m FROM CuentaCorrienteMovimiento m WHERE m.tipoMovimiento = :tipo AND m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente ORDER BY m.fechaMovimiento DESC, m.idMovimiento DESC",
                CuentaCorrienteMovimiento.class
            );
            query.setParameter("idCuentaCorriente", idCuentaCorriente);
            query.setParameter("tipo", CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CuentaCorrienteMovimiento> obtenerMovimientosEnRango(int idCuentaCorriente, java.sql.Date fechaDesde, java.sql.Date fechaHasta) {
        EntityManager em = getEntityManager();
        List<CuentaCorrienteMovimiento> movimientos = new ArrayList<>();
        try {
            StringBuilder queryStr = new StringBuilder("SELECT m FROM CuentaCorrienteMovimiento m WHERE m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente");

            if (fechaDesde != null) {
                queryStr.append(" AND m.fechaMovimiento >= :fechaDesde");
            }

            if (fechaHasta != null) {
                queryStr.append(" AND m.fechaMovimiento <= :fechaHasta");
            }

            queryStr.append(" ORDER BY m.fechaMovimiento ASC, m.idMovimiento ASC");

            TypedQuery<CuentaCorrienteMovimiento> query = em.createQuery(queryStr.toString(), CuentaCorrienteMovimiento.class);
            query.setParameter("idCuentaCorriente", idCuentaCorriente);

            if (fechaDesde != null) {
                query.setParameter("fechaDesde", fechaDesde.toLocalDate());
            }
            if (fechaHasta != null) {
                query.setParameter("fechaHasta", fechaHasta.toLocalDate());
            }

            movimientos = query.getResultList();
        } catch (Exception e) {
            System.out.println("Error al obtener movimientos en rango: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return movimientos;
    }

    public List<CuentaCorrienteMovimiento> obtenerMovimientosEnRango(int idCuentaCorriente, java.sql.Date fechaDesde, java.sql.Date fechaHasta, String tipoMovimiento) {
        EntityManager em = getEntityManager();
        List<CuentaCorrienteMovimiento> movimientos = new ArrayList<>();
        try {
            StringBuilder queryStr = new StringBuilder("SELECT m FROM CuentaCorrienteMovimiento m WHERE m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente");

            if (fechaDesde != null) {
                queryStr.append(" AND m.fechaMovimiento >= :fechaDesde");
            }
            if (fechaHasta != null) {
                queryStr.append(" AND m.fechaMovimiento <= :fechaHasta");
            }

            if (tipoMovimiento != null && !tipoMovimiento.trim().isEmpty() && !"TODOS".equalsIgnoreCase(tipoMovimiento.trim())) {
                queryStr.append(" AND m.tipoMovimiento = :tipoMovimiento");
            }

            queryStr.append(" ORDER BY m.fechaMovimiento ASC, m.idMovimiento ASC");

            TypedQuery<CuentaCorrienteMovimiento> query = em.createQuery(queryStr.toString(), CuentaCorrienteMovimiento.class);
            query.setParameter("idCuentaCorriente", idCuentaCorriente);

            if (fechaDesde != null) {
                query.setParameter("fechaDesde", fechaDesde.toLocalDate());
            }
            if (fechaHasta != null) {
                query.setParameter("fechaHasta", fechaHasta.toLocalDate());
            }

            if (tipoMovimiento != null && !tipoMovimiento.trim().isEmpty() && !"TODOS".equalsIgnoreCase(tipoMovimiento.trim())) {
                query.setParameter("tipoMovimiento", CuentaCorrienteMovimiento.TipoMovimiento.valueOf(tipoMovimiento.trim().toUpperCase()));
            }

            movimientos = query.getResultList();
        } catch (Exception e) {
            System.out.println("Error al obtener movimientos en rango (con filtro tipo): " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return movimientos;
    }

    public BigDecimal obtenerSaldoActual(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<BigDecimal> query = em.createQuery(
                "SELECT m.saldoResultante FROM CuentaCorrienteMovimiento m " +
                "WHERE m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente " +
                "ORDER BY m.fechaMovimiento DESC, m.idMovimiento DESC",
                BigDecimal.class
            );
            query.setParameter("idCuentaCorriente", idCuentaCorriente);
            query.setMaxResults(1);

            return query.getSingleResult();
        } catch (NoResultException e) {
            return BigDecimal.ZERO;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public BigDecimal obtenerSaldoActualSumando(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<BigDecimal> q = em.createQuery(
                    "SELECT "
                    + "COALESCE(SUM(CASE WHEN m.tipoMovimiento = :tc THEN m.monto ELSE 0 END), 0) "
                    + "- COALESCE(SUM(CASE WHEN m.tipoMovimiento = :td THEN m.monto ELSE 0 END), 0) "
                    + "FROM CuentaCorrienteMovimiento m "
                    + "WHERE m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente",
                    BigDecimal.class
            );
            q.setParameter("idCuentaCorriente", idCuentaCorriente);
            q.setParameter("tc", CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
            q.setParameter("td", CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
            return q.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CuentaCorrienteMovimiento> obtenerMovimientosPorCuentaCorriente(Integer idCuentaCorriente) {
        EntityManager em = getEntityManager();
        List<CuentaCorrienteMovimiento> movimientos = new ArrayList<>();
        try {
            TypedQuery<CuentaCorrienteMovimiento> query = em.createQuery(
                "SELECT m FROM CuentaCorrienteMovimiento m "
                        + "WHERE m.idCuentaCorriente.idCuentaCorriente = :idCuentaCorriente "
                        + "ORDER BY m.fechaMovimiento ASC, m.idMovimiento ASC",
                CuentaCorrienteMovimiento.class
            );
            query.setParameter("idCuentaCorriente", idCuentaCorriente);
            movimientos = query.getResultList();
        } catch (Exception e) {
            System.out.println("Error al obtener movimientos de la cuenta corriente: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return movimientos;
    }
}
