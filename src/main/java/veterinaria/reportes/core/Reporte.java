package veterinaria.reportes.core;

import java.io.File;

public interface Reporte {
    ReporteTipo tipo();
    File generar(ReporteRequest request) throws Exception;
}
