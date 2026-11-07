package veterinaria.servicio;

import java.time.LocalDate;
import javax.persistence.EntityManager;
import veterinaria.persistencia.JPAUtil;

/**
 * Indicadores de solo lectura para el inicio. No modifica registros.
 */
public class ResumenInicioService {

    public static final class Indicadores {
        public final long consultasHoy;
        public final long pacientesEnEspera;
        public final long turnosPeluqueriaHoy;
        public final long productosStockCritico;

        public Indicadores(long consultasHoy, long pacientesEnEspera, long turnosPeluqueriaHoy, long productosStockCritico) {
            this.consultasHoy = consultasHoy;
            this.pacientesEnEspera = pacientesEnEspera;
            this.turnosPeluqueriaHoy = turnosPeluqueriaHoy;
            this.productosStockCritico = productosStockCritico;
        }
    }

    public Indicadores obtener(LocalDate fecha) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            long consultas = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM visita WHERE fecha = :fecha AND (estado IS NULL OR UPPER(TRIM(estado)) <> 'CANCELADO')")
                    .setParameter("fecha", java.sql.Date.valueOf(fecha))
                    .getSingleResult()).longValue();

            long espera = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM visita WHERE UPPER(TRIM(estado)) IN ('EN_ESPERA', 'EN ESPERA')")
                    .getSingleResult()).longValue();

            long turnos = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM peluqueria WHERE fecha = :fecha "
                    + "AND (estado IS NULL OR UPPER(TRIM(estado)) NOT IN ('CANCELADO', 'ELIMINADO'))")
                    .setParameter("fecha", java.sql.Date.valueOf(fecha))
                    .getSingleResult()).longValue();

            long stockCritico = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM producto WHERE UPPER(TRIM(estado)) = 'ACTIVO' "
                    + "AND stock_minimo > 0 AND stock <= stock_minimo")
                    .getSingleResult()).longValue();

            return new Indicadores(consultas, espera, turnos, stockCritico);
        } finally {
            em.close();
        }
    }
}
