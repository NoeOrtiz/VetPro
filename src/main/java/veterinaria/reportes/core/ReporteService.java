package veterinaria.reportes.core;

import java.awt.Desktop;
import java.io.File;
import veterinaria.reportes.pdf.ReporteSelectorUtil;

public class ReporteService {

    // 🌟 REGLA DEL PROFESOR: Única instancia estática y privada
    private static ReporteService instance;
    private final ReporteRegistry registry = new ReporteRegistry();

    // 🌟 Constructor privado para que nadie use "new ReporteService()" fuera de acá
    private ReporteService() {
    }

    // 🌟 Método global para obtener la instancia única
    public static synchronized ReporteService getInstance() {
        if (instance == null) {
            instance = new ReporteService();
        }
        return instance;
    }

    public File generar(ReporteTipo tipo, ReporteRequest request) throws Exception {
        // 1. Extraemos el formato solicitado por el usuario
        ReporteSelectorUtil.Formato formato = request.get("FORMATO_REPORTE", ReporteSelectorUtil.Formato.class);

        if (formato == null) {
            formato = ReporteSelectorUtil.Formato.PDF; // Por las dudas, un fallback seguro
        }

        // 2. Buscamos el reporte específico pasándole Tipo Y Formato
        Reporte reporte = registry.get(tipo, formato);

        // 3. Generamos y devolvemos el archivo al ejecutor (sin abrirlo de prepo)
        return reporte.generar(request);
    }
}
