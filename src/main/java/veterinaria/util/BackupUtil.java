package veterinaria.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupUtil {

    private static final String CARPETA_BACKUP = "C:\\CarpetaDeBackups";
    private static final String MYSQLDUMP = "C:\\xampp\\mysql\\bin\\mysqldump.exe";

    private BackupUtil() {
    }

    public static String generarBackup() throws IOException, InterruptedException {

        File carpeta = new File(CARPETA_BACKUP);

        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        String fechaHora = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String rutaArchivo = CARPETA_BACKUP
                + "\\VetPRO_Backup_"
                + fechaHora
                + ".sql";

        ProcessBuilder pb = new ProcessBuilder(
                MYSQLDUMP,
                "-u", "root",
                "veterinaria",
                "-r", rutaArchivo
        );

        pb.redirectErrorStream(true);

        Process proceso = pb.start();

        int resultado = proceso.waitFor();

        if (resultado != 0) {
            throw new IOException("No se pudo generar el backup. Código: " + resultado);
        }

        return rutaArchivo;
    }
}
