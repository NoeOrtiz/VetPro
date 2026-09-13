package veterinaria.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupManager {

    public static boolean realizarRespaldo(String rutaCarpetaDestino) {
        try {
            // Crear la carpeta de destino si no existe
            File carpeta = new File(rutaCarpetaDestino);
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            // Generar un nombre único con la fecha y hora actual
            String fechaHora = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String archivoBackup = rutaCarpetaDestino + File.separator + "vetpro_backup_" + fechaHora + ".sql";

            // NOTA: Ajusta "root" y tu contraseña según corresponda. 
            // Si mysqldump no está en el PATH global de Windows, debes poner la ruta completa:
            // "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe"
            String user = "root";
            String password = ""; // Reemplaza con tu contraseña de BD
            String dbName = "veterinaria";  // Nombre de tu base de datos

            // Comando para Windows (usando cmd /c)
            String comando = String.format("mysqldump -u %s -p%s %s -r \"%s\"", user, password, dbName, archivoBackup);

            Process proceso = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", comando});
            int codigoSalida = proceso.waitFor();

            if (codigoSalida == 0) {
                System.out.println("[BACKUP] Copia creada con éxito en: " + archivoBackup);
                return true;
            } else {
                System.err.println("[BACKUP] Error al generar el respaldo. Código de salida: " + codigoSalida);
                return false;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
