package veterinaria.reportes.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;

/** Exporta las mismas consultas y el mismo filtro que el informe PDF. */
public class VisitaListadoXlsReporte implements Reporte {
    @Override
    public ReporteTipo tipo() { return ReporteTipo.VISITA_LISTADO; }

    @Override
    public File generar(ReporteRequest request) throws Exception {
        List<?> datos = request.get("filas", List.class);
        if (datos == null || datos.isEmpty())
            throw new IllegalArgumentException("No hay consultas para exportar.");

        File archivo = File.createTempFile("consultas_vetpro_", ".xls");
        archivo.deleteOnExit();
        try (Workbook libro = new HSSFWorkbook(); FileOutputStream salida = new FileOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Consultas");
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle titulo = libro.createCellStyle();
            titulo.setFont(negrita);
            titulo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle normal = libro.createCellStyle();
            normal.setWrapText(true);

            Row meta = hoja.createRow(0);
            meta.createCell(0).setCellValue("VetPRO - Listado de consultas");
            meta.createCell(1).setCellValue("Filtro: " + request.get("filtro", String.class));
            Row encabezado = hoja.createRow(2);
            String[] headers = {"N.º", "Paciente", "Responsable", "Motivo",
                    "Estado", "Veterinario", "Prioridad", "Fecha"};
            for (int i = 0; i < headers.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(headers[i]);
                celda.setCellStyle(titulo);
                hoja.setColumnWidth(i, (i == 3 ? 28 : i == 2 ? 25 : i == 5 ? 22 : 17) * 256);
            }
            int n = 3;
            for (Object item : datos) {
                if (!(item instanceof String[])) continue;
                String[] valores = (String[]) item;
                Row fila = hoja.createRow(n++);
                for (int i = 0; i < headers.length; i++) {
                    Cell celda = fila.createCell(i);
                    celda.setCellValue(i < valores.length && valores[i] != null ? valores[i] : "");
                    celda.setCellStyle(normal);
                }
            }
            hoja.createFreezePane(0, 3);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(2, n - 1), 0, headers.length - 1));
            libro.write(salida);
        }
        return archivo;
    }
}
