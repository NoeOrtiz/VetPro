package veterinaria.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppLog {

    private AppLog() {
    }

    private static Logger logger(Class<?> source) {
        return Logger.getLogger(source.getName());
    }

    public static void info(Class<?> source, String message) {
        logger(source).log(Level.INFO, message);
    }

    public static void warn(Class<?> source, String message, Exception ex) {
        logger(source).log(Level.WARNING, message, ex);
    }

    public static void warning(Class<?> source, String message) {
        logger(source).log(Level.WARNING, message);
    }

    public static void warning(Class<?> source, String message, Exception ex) {
        logger(source).log(Level.WARNING, message, ex);
    }

    public static void ignored(Class<?> source, String message, Exception ex) {
        logger(source).log(Level.WARNING, message, ex);
    }

    public static void error(Class<?> source, String message, Exception ex) {
        logger(source).log(Level.SEVERE, message, ex);
    }
}
