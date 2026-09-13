package veterinaria.reportes.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Si usás .xlsx

import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReportePaths;

public abstract class AbstractXlsReporte implements Reporte {

    protected static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public final File generar(ReporteRequest request) throws Exception {
        // 1. Definimos el archivo físico de salida con extensión .xlsx o .xls
        String fileName = tipo().name() + "_" + LocalDateTime.now().format(TS) + ".xlsx";
        File out = ReportePaths.enDescargas(fileName);

        // 2. Creamos el libro de Excel real con Apache POI
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(titulo());
            
            // Llamamos al método abstracto para que cada reporte llene sus filas
            generarContenidoExcel(sheet, workbook, request);
            
            // Autoajustar columnas básicas
            for (int i = 0; i < 10; i++) {
                sheet.autoSizeColumn(i);
            }

            // 3. Escribimos el archivo en Descargas
            try (FileOutputStream fos = new FileOutputStream(out)) {
                workbook.write(fos);
            }
        }

        return out;
    }

    protected abstract String titulo();
    protected abstract void generarContenidoExcel(Sheet sheet, Workbook workbook, ReporteRequest request) throws Exception;
}
