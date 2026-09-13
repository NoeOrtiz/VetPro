package veterinaria.reportes.core;

import java.io.File;

public class ReportePaths {

    private ReportePaths() {}

    public static File enDescargas(String fileName) {
        String home = System.getProperty("user.home");
        File downloads = new File(home, "Downloads");
        if (!downloads.exists()) {
            downloads = new File(home, "Descargas");
        }
        if (!downloads.exists()) {
            downloads = new File(home);
        }
        return new File(downloads, fileName);
    }
}
