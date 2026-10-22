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

public class CajaMovimientosListadoXlsReporte implements Reporte {
    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CAJA_MOVIMIENTOS_LISTADO;
    }

    @Override
    public File generar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new IllegalArgumentException("No hay movimientos para exportar.");
        }
        File archivo = File.createTempFile("vetpro_movimientos_caja_", ".xls");
        archivo.deleteOnExit();
        try (Workbook libro = new HSSFWorkbook(); FileOutputStream salida = new FileOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Movimientos de Caja");
            CellStyle titulo = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            titulo.setFont(fuente);
            Row encabezado = hoja.createRow(0);
            TableModel datos = tabla.getModel();
            for (int c = 0; c < datos.getColumnCount(); c++) {
                Cell celda = encabezado.createCell(c);
                celda.setCellValue(datos.getColumnName(c));
                celda.setCellStyle(titulo);
            }
            for (int f = 0; f < datos.getRowCount(); f++) {
                Row fila = hoja.createRow(f + 1);
                for (int c = 0; c < datos.getColumnCount(); c++) {
                    Object valor = datos.getValueAt(f, c);
                    Cell celda = fila.createCell(c);
                    if (valor instanceof Number) celda.setCellValue(((Number) valor).doubleValue());
                    else celda.setCellValue(valor == null ? "" : valor.toString());
                }
            }
            for (int c = 0; c < datos.getColumnCount(); c++) hoja.autoSizeColumn(c);
            libro.write(salida);
        }
        return archivo;
    }
}
