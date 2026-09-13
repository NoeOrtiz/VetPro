package veterinaria.util;

import java.util.prefs.Preferences;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.swing.JOptionPane;

public class BackupAlertaUtil {

    private static final Preferences prefs = Preferences.userNodeForPackage(BackupAlertaUtil.class);
    private static final String PREF_ULTIMO_BACKUP = "ultimo_backup_fecha";
    private static final long DIAS_LIMITE = 30; // Cada cuántos días se sugiere el backup

    // Método que se ejecuta al iniciar sesión o abrir el panel principal
    public static void verificarEstadoBackup() {
        String fechaUltimaStr = prefs.get(PREF_ULTIMO_BACKUP, null);

        if (fechaUltimaStr == null) {
            // Si nunca se hizo un backup, guardamos la fecha de hoy como referencia inicial
            registrarNuevoBackup();
            return;
        }

        LocalDate ultimaFecha = LocalDate.parse(fechaUltimaStr);
        LocalDate hoy = LocalDate.now();

        // Calcular cuántos días pasaron
        long diasTranscurridos = ChronoUnit.DAYS.between(ultimaFecha, hoy);
        long diasRestantes = DIAS_LIMITE - diasTranscurridos;

        if (diasRestantes <= 0) {

            JOptionPane.showMessageDialog(
                    null,
                    "⚠️ Han transcurrido más de " + DIAS_LIMITE + " días desde la última copia de seguridad.\n\n"
                    + "Para preservar la información almacenada en VetPRO,\n"
                    + "se recomienda generar un nuevo respaldo de la base de datos.",
                    "Recordatorio de Copia de Seguridad",
                    JOptionPane.WARNING_MESSAGE);

        } else if (diasRestantes <= 5) {

            JOptionPane.showMessageDialog(
                    null,
                    "ℹ️ Restan " + diasRestantes + " día(s) para alcanzar el plazo recomendado de "
                    + DIAS_LIMITE + " días.\n\n"
                    + "Se recomienda generar una nueva copia de seguridad de la base de datos.",
                    "Próximo Respaldo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
// Este método debes llamarlo *justo después* de que el backup se genere con éxito
    public static void registrarNuevoBackup() {
        prefs.put(PREF_ULTIMO_BACKUP, LocalDate.now().toString());
    }
}
