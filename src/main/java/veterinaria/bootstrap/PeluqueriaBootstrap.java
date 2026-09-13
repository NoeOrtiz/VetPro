package veterinaria.bootstrap;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import veterinaria.entidad.TipoCitaPeluqueria;
import veterinaria.persistencia.JPAUtil;

public final class PeluqueriaBootstrap {

    private static final Logger LOG = Logger.getLogger(PeluqueriaBootstrap.class.getName());

    private PeluqueriaBootstrap() {}

    private static class TipoSeed {
        final String nombre;
        final BigDecimal precio;

        TipoSeed(String nombre, BigDecimal precio) {
            this.nombre = nombre;
            this.precio = precio;
        }
    }

    private static final List<TipoSeed> TIPOS = Arrays.asList(
        new TipoSeed("Baño", new BigDecimal("10000.00")),
        new TipoSeed("Corte", new BigDecimal("8000.00")),
        new TipoSeed("Corte Sanitario", new BigDecimal("6500.00")),
        new TipoSeed("Limpieza Dental", new BigDecimal("8000.00")),
        new TipoSeed("Baño y Corte", new BigDecimal("15000.00")),
        new TipoSeed("Baño Sanitario", new BigDecimal("7500.00")),
        new TipoSeed("Baño Sanitario y Corte", new BigDecimal("12000.00")),
        new TipoSeed("Otros Estética", new BigDecimal("20000.00"))
    );

    public static void ensureTiposBase() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT COUNT(t) FROM TipoCitaPeluqueria t", Long.class)
                    .getSingleResult();

            if (count != null && count == 0L) {
                em.getTransaction().begin();

                int orden = 1;
                for (TipoSeed seed : TIPOS) {
                    TipoCitaPeluqueria t = new TipoCitaPeluqueria();
                    t.setDescripcion(seed.nombre);
                    t.setPrecio(seed.precio);
                    t.setOrden(orden++);
                    t.setActivo(true);

                    em.persist(t);
                }

                em.getTransaction().commit();
                LOG.info("[BOOTSTRAP] Tipos de cita de peluquería creados: " + TIPOS.size());
            }

        } catch (Exception ex) {
            try {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception rollbackEx) {
                LOG.log(Level.WARNING, "[BOOTSTRAP] Error adicional al rollback de peluquería.", rollbackEx);
            }
            LOG.log(Level.SEVERE, "[BOOTSTRAP] Error seed tipos de cita peluquería.", ex);
            throw new IllegalStateException("No se pudo inicializar la configuración base de peluquería.", ex);
        } finally {
            em.close();
        }
    }

    public static void warmupPeluqueriaSchema() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.createQuery("SELECT COUNT(t) FROM TipoCitaPeluqueria t", Long.class).getSingleResult();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[BOOTSTRAP] Error validando esquema de peluquería.", ex);
            throw new IllegalStateException("No se pudo validar/inicializar el esquema de peluquería.", ex);
        } finally {
            em.close();
        }
    }
}
