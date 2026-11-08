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

public class UsuarioListadoXlsReporte implements Reporte {
    @Override public ReporteTipo tipo() { return ReporteTipo.USUARIO_LISTADO; }

    @Override public File generar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null) throw new IllegalArgumentException("No hay datos de usuarios.");
        TableModel modelo = tabla.getModel();
        String filtro = request.get("estadoFiltro", String.class);
        if (filtro == null || filtro.isBlank()) filtro = "TODOS";
        filtro = filtro.trim().toUpperCase(java.util.Locale.ROOT);
        int[] columnas = {1,2,3,4,5,6,7,8,9};
        String[] titulos = {"Nombre","Apellido","DNI","Teléfono","Dirección","Rol","Usuario","E-mail","Estado"};
        File archivo = File.createTempFile("usuarios_", ".xls");
        archivo.deleteOnExit();
        try (Workbook libro = new HSSFWorkbook(); FileOutputStream salida = new FileOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Usuarios");
            Font fuente = libro.createFont(); fuente.setBold(true);
            CellStyle encabezado = libro.createCellStyle(); encabezado.setFont(fuente);
            encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row cab = hoja.createRow(0);
            for (int i=0; i<titulos.length; i++) {
                Cell celda=cab.createCell(i); celda.setCellValue(titulos[i]); celda.setCellStyle(encabezado);
            }
            int fila=1;
            for (int r=0; r<modelo.getRowCount(); r++) {
                String estado=String.valueOf(modelo.getValueAt(r,9)).trim().toUpperCase(java.util.Locale.ROOT);
                if (!"TODOS".equals(filtro) && !filtro.equals(estado)) continue;
                Row destino=hoja.createRow(fila++);
                for (int i=0; i<columnas.length; i++) {
                    Object dato=modelo.getValueAt(r,columnas[i]);
                    destino.createCell(i).setCellValue(dato==null ? "" : String.valueOf(dato));
                }
            }
            hoja.createFreezePane(0,1);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,fila-1),0,titulos.length-1));
            for(int i=0;i<titulos.length;i++) { hoja.autoSizeColumn(i); hoja.setColumnWidth(i,Math.min(hoja.getColumnWidth(i)+600,14000)); }
            libro.write(salida);
        }
        return archivo;
    }
}
