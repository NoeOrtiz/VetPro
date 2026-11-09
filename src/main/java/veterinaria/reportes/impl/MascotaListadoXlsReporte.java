package veterinaria.reportes.impl;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;

public class MascotaListadoXlsReporte implements Reporte {
    @Override public ReporteTipo tipo() { return ReporteTipo.MASCOTA_LISTADO; }

    @Override public File generar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null) throw new IllegalArgumentException("No se proporcionaron mascotas.");
        String filtro = request.get("estadoFiltro", String.class);
        if (filtro == null) filtro = "TODOS";
        filtro = filtro.toUpperCase();
        TableModel modelo = tabla.getModel();
        int[] cols = {2, 6, 4, 3, 5, 7, 8, 10, 11, 12};
        String[] titulos = {"Nombre", "Especie", "Raza", "Sexo", "Edad", "Dueño", "Castrado", "Tamaño", "Peso", "Estado"};
        File archivo = File.createTempFile("mascotas_", ".xls");
        archivo.deleteOnExit();
        try (Workbook libro = new HSSFWorkbook(); FileOutputStream out = new FileOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Mascotas");
            CellStyle cabecera = libro.createCellStyle();
            Font fuente = libro.createFont(); fuente.setBold(true); cabecera.setFont(fuente);
            cabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(titulos[i]); celda.setCellStyle(cabecera);
            }
            int salida = 1;
            for (int r = 0; r < modelo.getRowCount(); r++) {
                String estado = String.valueOf(modelo.getValueAt(r, 12));
                if (!"TODOS".equals(filtro) && !filtro.equalsIgnoreCase(estado)) continue;
                Row fila = hoja.createRow(salida++);
                for (int c = 0; c < cols.length; c++) {
                    Object valor = modelo.getValueAt(r, cols[c]);
                    fila.createCell(c).setCellValue(valor == null ? "" : valor.toString());
                }
            }
            hoja.createFreezePane(0, 1);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, salida - 1), 0, cols.length - 1));
            for (int c = 0; c < cols.length; c++) hoja.autoSizeColumn(c);
            libro.write(out);
        }
        return archivo;
    }
}
