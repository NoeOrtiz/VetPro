
package veterinaria.persistencia;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import veterinaria.config.PersistenceConfig;

public final class JPAUtil {

    private static final String PERSISTENCE_UNIT_NAME = "veterinariaPU";

    private static class EMFHolder {
        private static final EntityManagerFactory EMF = buildEntityManagerFactory();
    }

    private JPAUtil() {
    }

    private static EntityManagerFactory buildEntityManagerFactory() {
        Map<String, String> overrides = PersistenceConfig.asJpaOverrides();
        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, overrides);
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMFHolder.EMF;
    }

    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static void close() {
        EntityManagerFactory emf = EMFHolder.EMF;
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
