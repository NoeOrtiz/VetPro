package veterinaria.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import veterinaria.entidad.Usuario;

public final class AuditoriaLogger {

    private static final Object LOCK = new Object();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditoriaLogger() {}

    public static void evento(String accion, String detalle, Usuario usuario) {
        String user = (usuario == null)
                ? "N/D"
                : (usuario.getIdUsuario() + "|" + safe(usuario.getNombreUsuario()));

        String line = String.format(
                "%s\t%s\t%s\t%s",
                LocalDateTime.now().format(TS),
                safe(accion),
                user,
                safe(detalle)
        );

        File file = getLogFile();
        synchronized (LOCK) {
            ensureParent(file);
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                bw.write(line);
                bw.newLine();
            } catch (IOException ignored) {
            }
        }
    }

    private static File getLogFile() {
        String base = System.getProperty("user.dir");
        return new File(base + File.separator + "logs" + File.separator + "auditoria-caja.log");
    }

    private static void ensureParent(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replace("\r", " ").trim();
    }
}
