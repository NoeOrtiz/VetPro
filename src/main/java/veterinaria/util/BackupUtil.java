package veterinaria.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import veterinaria.config.PersistenceConfig;

/**
 * Respaldo MySQL sin shell ni credenciales en la línea de comandos.
 * La base y el usuario se obtienen de la configuración real de VetPRO.
 */
public final class BackupUtil {
    private static final long TIMEOUT_MINUTOS = 10;
    private BackupUtil() {}

    public static String generarBackup() throws IOException, InterruptedException {
        String configurada = System.getenv("VETPRO_BACKUP_DIR");
        Path destino = configurada == null || configurada.isBlank()
                ? Path.of(System.getProperty("user.home"), ".vetpro", "backups")
                : Path.of(configurada);
        return generarBackup(destino);
    }

    public static String generarBackup(Path destino) throws IOException, InterruptedException {
        if (destino == null) throw new IllegalArgumentException("Falta la carpeta de destino.");
        Files.createDirectories(destino);
        String jdbc = PersistenceConfig.getDbUrl();
        URI uri;
        try {
            if (jdbc == null || !jdbc.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException("URL JDBC no compatible con respaldos MySQL.");
            }
            uri = URI.create(jdbc.substring("jdbc:".length()));
        } catch (RuntimeException ex) {
            throw new IOException("La conexión MySQL configurada no tiene una URL válida.", ex);
        }
        String base = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (!base.matches("[A-Za-z0-9_]+")) {
            throw new IOException("El nombre de la base de datos no es válido para el respaldo.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new IOException("No se encontró el servidor MySQL.");
        int puerto = uri.getPort() > 0 ? uri.getPort() : 3306;
        String usuario = PersistenceConfig.getDbUser();
        if (usuario == null || usuario.isBlank()) throw new IOException("Falta el usuario de MySQL.");
        String binario = System.getenv("VETPRO_MYSQLDUMP");
        if (binario == null || binario.isBlank()) binario = "mysqldump";

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        Path salida = destino.resolve("VetPRO_Backup_" + fecha + ".sql");
        List<String> args = new ArrayList<>();
        args.add(binario);
        args.add("--host=" + host);
        args.add("--port=" + puerto);
        args.add("--user=" + usuario);
        args.add("--single-transaction");
        args.add("--routines");
        args.add("--triggers");
        args.add(base);
        ProcessBuilder procesoBuilder = new ProcessBuilder(args);
        procesoBuilder.redirectOutput(salida.toFile());
        procesoBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        String password = PersistenceConfig.getDbPassword();
        if (password != null && !password.isEmpty()) {
            // Evitar -pCLAVE en el comando. En entornos multiusuario conviene
            // reemplazar MYSQL_PWD por un archivo protegido de opciones MySQL.
            procesoBuilder.environment().put("MYSQL_PWD", password);
        }
        boolean completado = false;
        try {
            Process proceso = procesoBuilder.start();
            if (!proceso.waitFor(TIMEOUT_MINUTOS, TimeUnit.MINUTES)) {
                proceso.destroyForcibly();
                throw new IOException("El respaldo superó el tiempo máximo permitido.");
            }
            if (proceso.exitValue() != 0 || !Files.isRegularFile(salida) || Files.size(salida) == 0) {
                throw new IOException("Falló mysqldump. No se conservará el archivo incompleto.");
            }
            completado = true;
            return salida.toString();
        } finally {
            if (!completado) Files.deleteIfExists(salida);
        }
    }
}
