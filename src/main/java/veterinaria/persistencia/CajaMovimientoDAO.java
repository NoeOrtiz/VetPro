
package veterinaria.persistencia;

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaMovimiento.TipoMovimiento;
import veterinaria.servicio.CajaService;

public class CajaMovimientoDAO {

    private final CajaService cajaService = new CajaService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean registrarMovimiento(CajaMovimiento movimiento) {
        return cajaService.registrarMovimiento(movimiento);
    }

    public boolean modificarMovimiento(CajaMovimiento movimiento) {
        return cajaService.modificarMovimiento(movimiento);
    }

    public List<CajaMovimiento> obtenerTodosLosMovimientos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery("SELECT c FROM CajaMovimiento c ORDER BY c.fecha DESC", CajaMovimiento.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private Date onlyDate(Date d) {
        if (d == null) return null;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public List<CajaMovimiento> obtenerMovimientosPorFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.fecha = :fecha AND c.eliminado = false ORDER BY c.fecha ASC",
                CajaMovimiento.class
            );
            query.setParameter("fecha", onlyDate(fecha));
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CajaMovimiento> obtenerIngresosPorFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                CajaMovimiento.class
            );
            query.setParameter("tipo", CajaMovimiento.TipoMovimiento.CREDITO);
            query.setParameter("fecha", onlyDate(fecha));
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CajaMovimiento> obtenerEgresosPorFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                CajaMovimiento.class
            );
            query.setParameter("tipo", CajaMovimiento.TipoMovimiento.DEBITO);
            query.setParameter("fecha", onlyDate(fecha));
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CajaMovimiento obtenerAperturaDeCajaPorFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                CajaMovimiento.class
            );
            query.setParameter("tipo", CajaMovimiento.TipoMovimiento.APERTURA);
            query.setParameter("fecha", onlyDate(fecha));
            query.setMaxResults(1);
            return query.getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CajaMovimiento obtenerCierreDeCajaPorFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                CajaMovimiento.class
            );
            query.setParameter("tipo", CajaMovimiento.TipoMovimiento.CIERRE);
            query.setParameter("fecha", onlyDate(fecha));
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (Exception e) {
            System.out.println("No se encontró ningún cierre de caja para la fecha especificada.");
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CajaMovimiento obtenerUltimoCierreDeCaja() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = 'CIERRE' AND c.eliminado = false ORDER BY c.fecha DESC, c.idMovimiento DESC",
                CajaMovimiento.class
            );
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            if (em != null) em.close();
        }
    }

    public CajaMovimiento obtenerUltimaAperturaDeCaja() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CajaMovimiento> query = em.createQuery(
                    "SELECT c FROM CajaMovimiento c WHERE c.tipoMovimiento = 'APERTURA' AND c.eliminado = false ORDER BY c.fecha DESC, c.idMovimiento DESC",
                    CajaMovimiento.class
            );
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            if (em != null) em.close();
        }
    }

    public CajaMovimiento obtenerAperturaPendienteCierre() {
        CajaMovimiento lastApertura = obtenerUltimaAperturaDeCaja();
        if (lastApertura == null) return null;
        CajaMovimiento lastCierre = obtenerUltimoCierreDeCaja();
        if (lastCierre == null) return lastApertura;

        if (lastApertura.getFecha() != null && lastCierre.getFecha() != null
                && lastApertura.getFecha().after(lastCierre.getFecha())) {
            return lastApertura;
        }
        return null;
    }

    public boolean existeAperturaEnFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                Long.class
            );
            query.setParameter("tipo", CajaMovimiento.TipoMovimiento.APERTURA);
            query.setParameter("fecha", onlyDate(fecha));
            return query.getSingleResult() > 0;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean existeCierreEnFecha(Date fecha) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM CajaMovimiento c WHERE c.tipoMovimiento = :tipo AND c.fecha = :fecha AND c.eliminado = false",
                Long.class
            );
            query.setParameter("tipo", CajaMovimiento.TipoMovimiento.CIERRE);
            query.setParameter("fecha", onlyDate(fecha));
            return query.getSingleResult() > 0;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CajaMovimiento> obtenerMovimientosFiltrados(TipoMovimiento tipo, Date desde, Date hasta) {
        EntityManager em = getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT c FROM CajaMovimiento c "
                    + "LEFT JOIN FETCH c.usuario u "
                    + "LEFT JOIN FETCH u.persona p "
                    + "LEFT JOIN FETCH c.metodoPago mp "
                    + "LEFT JOIN FETCH c.recibo r "
                    + "WHERE c.eliminado = false"
            );
            if (tipo != null) {
                jpql.append(" AND c.tipoMovimiento = :tipo");
            }
            if (desde != null) {
                jpql.append(" AND c.fecha >= :desde");
            }
            if (hasta != null) {
                jpql.append(" AND c.fecha <= :hasta");
            }
            jpql.append(" ORDER BY c.fecha DESC, c.idMovimiento DESC");

            TypedQuery<CajaMovimiento> query = em.createQuery(jpql.toString(), CajaMovimiento.class);
            if (tipo != null) query.setParameter("tipo", tipo);
            if (desde != null) query.setParameter("desde", desde);
            if (hasta != null) query.setParameter("hasta", hasta);

            return query.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

}
