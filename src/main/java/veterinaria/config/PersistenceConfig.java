package veterinaria.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class PersistenceConfig {

    public static final String PROP_DB_URL = "vetpro.db.url";
    public static final String PROP_DB_USER = "vetpro.db.user";
    public static final String PROP_DB_PASSWORD = "vetpro.db.password";
    public static final String PROP_HBM2DDL = "vetpro.hibernate.hbm2ddl";
    public static final String PROP_SHOW_SQL = "vetpro.hibernate.show_sql";

    public static final String PROP_CONFIG_FILE = "vetpro.config.file";
    public static final String PROP_IGNORE_CLASSPATH = "vetpro.config.ignoreClasspath";

    public static final String ENV_DB_URL = "VETPRO_DB_URL";
    public static final String ENV_DB_USER = "VETPRO_DB_USER";
    public static final String ENV_DB_PASSWORD = "VETPRO_DB_PASSWORD";
    public static final String ENV_HBM2DDL = "VETPRO_HIBERNATE_HBM2DDL";
    public static final String ENV_SHOW_SQL = "VETPRO_HIBERNATE_SHOW_SQL";

    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/veterinaria?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "";
    private static final String DEFAULT_HBM2DDL = "validate";
    private static final String DEFAULT_SHOW_SQL = "false";

    private PersistenceConfig() {
    }

    public static Map<String, String> asJpaOverrides() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.url", getDbUrl());
        props.put("javax.persistence.jdbc.user", getDbUser());
        props.put("javax.persistence.jdbc.password", getDbPassword());
        props.put("hibernate.hbm2ddl.auto", getHbm2ddlAuto());
        props.put("hibernate.show_sql", getShowSql());
        return props;
    }

    public static String getDbUrl() {
        return resolve(PROP_DB_URL, ENV_DB_URL, DEFAULT_DB_URL);
    }

    public static String getDbUser() {
        return resolve(PROP_DB_USER, ENV_DB_USER, DEFAULT_DB_USER);
    }

    public static String getDbPassword() {
        return resolve(PROP_DB_PASSWORD, ENV_DB_PASSWORD, DEFAULT_DB_PASSWORD);
    }

    public static String getHbm2ddlAuto() {
        return resolve(PROP_HBM2DDL, ENV_HBM2DDL, DEFAULT_HBM2DDL);
    }

    public static String getShowSql() {
        return resolve(PROP_SHOW_SQL, ENV_SHOW_SQL, DEFAULT_SHOW_SQL);
    }

    public static String describeSafe() {
        return "url=" + getDbUrl()
                + ", user=" + getDbUser()
                + ", password=" + (getDbPassword().isEmpty() ? "<vacío>" : "<configurada>")
                + ", hbm2ddl=" + getHbm2ddlAuto()
                + ", show_sql=" + getShowSql();
    }

    static String resolve(String systemPropertyKey, String envKey, String defaultValue) {
        String fromSystem = System.getProperty(systemPropertyKey);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = loadProperties().getProperty(systemPropertyKey);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return defaultValue;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();

        String explicitPath = System.getProperty(PROP_CONFIG_FILE);
        if (explicitPath != null && !explicitPath.isBlank()) {
            loadFromPath(props, Path.of(explicitPath.trim()));
        }

        loadFromPath(props, Path.of("config", "vetpro.properties"));
        loadFromPath(props, Path.of("vetpro.properties"));

        boolean ignoreClasspath = Boolean.parseBoolean(System.getProperty(PROP_IGNORE_CLASSPATH, "false"));
        if (!ignoreClasspath) {
            try (InputStream in = PersistenceConfig.class.getResourceAsStream("/vetpro.properties")) {
                if (in != null) {
                    Properties classpathProps = new Properties();
                    classpathProps.load(in);
                    classpathProps.forEach((k, v) -> props.putIfAbsent(k, v));
                }
            } catch (IOException ignored) {
            }
        }
        return props;
    }

    private static void loadFromPath(Properties props, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream in = Files.newInputStream(path)) {
            Properties ext = new Properties();
            ext.load(in);
            ext.forEach(props::put);
        } catch (IOException ignored) {
        }
    }
}
