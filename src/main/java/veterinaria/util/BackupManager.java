package veterinaria.util;

import java.io.IOException;
import java.nio.file.Path;

/** API de compatibilidad: delega al mecanismo central de respaldo seguro. */
public final class BackupManager {
    private BackupManager() {}

    public static boolean realizarRespaldo(String rutaCarpetaDestino) {
        try {
            if (rutaCarpetaDestino == null || rutaCarpetaDestino.isBlank()) return false;
            BackupUtil.generarBackup(Path.of(rutaCarpetaDestino));
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            AppLog.warn(BackupManager.class, "Respaldo interrumpido", ex);
            return false;
        } catch (IOException | RuntimeException ex) {
            AppLog.warn(BackupManager.class, "No se pudo generar el respaldo", ex);
            return false;
        }
    }
}
