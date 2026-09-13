package veterinaria.bootstrap;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import veterinaria.persistencia.JPAUtil;

public final class AgendaBootstrap {

    private static final Logger LOG = Logger.getLogger(AgendaBootstrap.class.getName());

    private AgendaBootstrap() {}

    public static void warmupAgendaSchema() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.createQuery("SELECT COUNT(s) FROM AgendaSlot s", Long.class).getSingleResult();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[BOOTSTRAP] Error validando esquema de agenda.", ex);
            throw new IllegalStateException("No se pudo validar/inicializar el esquema de agenda.", ex);
        } finally {
            em.close();
        }
    }
}
