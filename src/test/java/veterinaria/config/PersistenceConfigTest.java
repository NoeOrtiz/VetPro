package veterinaria.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PersistenceConfigTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(PersistenceConfig.PROP_DB_URL);
        System.clearProperty(PersistenceConfig.PROP_DB_USER);
        System.clearProperty(PersistenceConfig.PROP_DB_PASSWORD);
        System.clearProperty(PersistenceConfig.PROP_HBM2DDL);
        System.clearProperty(PersistenceConfig.PROP_SHOW_SQL);
        System.clearProperty(PersistenceConfig.PROP_CONFIG_FILE);
        System.clearProperty(PersistenceConfig.PROP_IGNORE_CLASSPATH);
    }

    @Test
    void debeExponerDefaultsInternosSiSeAislaDeArchivosLocales() {
        System.setProperty(PersistenceConfig.PROP_IGNORE_CLASSPATH, "true");

        assertEquals("jdbc:mysql://localhost:3306/veterinaria?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", PersistenceConfig.getDbUrl());
        assertEquals("root", PersistenceConfig.getDbUser());
        assertEquals("", PersistenceConfig.getDbPassword());
        assertEquals("validate", PersistenceConfig.getHbm2ddlAuto());
        assertEquals("false", PersistenceConfig.getShowSql());
    }

    @Test
    void debePermitirSobrescribirConSystemProperties() {
        System.setProperty(PersistenceConfig.PROP_DB_URL, "jdbc:mysql://localhost:3306/testdb");
        System.setProperty(PersistenceConfig.PROP_DB_USER, "tester");
        System.setProperty(PersistenceConfig.PROP_DB_PASSWORD, "secret");
        System.setProperty(PersistenceConfig.PROP_HBM2DDL, "none");
        System.setProperty(PersistenceConfig.PROP_SHOW_SQL, "true");

        assertEquals("jdbc:mysql://localhost:3306/testdb", PersistenceConfig.getDbUrl());
        assertEquals("tester", PersistenceConfig.getDbUser());
        assertEquals("secret", PersistenceConfig.getDbPassword());
        assertEquals("none", PersistenceConfig.getHbm2ddlAuto());
        assertEquals("true", PersistenceConfig.getShowSql());
    }

    @Test
    void debeConstruirOverridesJpaConsistentes() {
        System.setProperty(PersistenceConfig.PROP_DB_URL, "jdbc:mysql://localhost:3306/demo");
        System.setProperty(PersistenceConfig.PROP_DB_USER, "demo_user");
        System.setProperty(PersistenceConfig.PROP_DB_PASSWORD, "demo_pass");
        System.setProperty(PersistenceConfig.PROP_HBM2DDL, "validate");
        System.setProperty(PersistenceConfig.PROP_SHOW_SQL, "false");

        Map<String, String> props = PersistenceConfig.asJpaOverrides();

        assertEquals("jdbc:mysql://localhost:3306/demo", props.get("javax.persistence.jdbc.url"));
        assertEquals("demo_user", props.get("javax.persistence.jdbc.user"));
        assertEquals("demo_pass", props.get("javax.persistence.jdbc.password"));
        assertEquals("validate", props.get("hibernate.hbm2ddl.auto"));
        assertEquals("false", props.get("hibernate.show_sql"));
        assertTrue(PersistenceConfig.describeSafe().contains("hbm2ddl=validate"));
    }
}
