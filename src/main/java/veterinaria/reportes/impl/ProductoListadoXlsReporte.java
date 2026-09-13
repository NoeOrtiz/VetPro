package veterinaria.reportes.impl;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JTable;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;

public class ProductoListadoXlsReporte implements Reporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.PRODUCTO_LISTADO;
    }

    @Override
    public File generar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null) {
            throw new IllegalArgumentException("No se proporcionó la tabla de datos para generar el Excel.");
        }

        // 🌟 1. Obtener el filtro por rubro enviado desde el request
        String rubroFiltro = request.get("rubroFiltro", String.class);
        if (rubroFiltro == null || rubroFiltro.isBlank()) {
            rubroFiltro = "TODOS";
        } else {
            rubroFiltro = rubroFiltro.trim();
        }

        File archivoTemporal = File.createTempFile("reporte_productos_", ".xls");
        archivoTemporal.deleteOnExit();

        try (Workbook libro = new HSSFWorkbook(); FileOutputStream fos = new FileOutputStream(archivoTemporal)) {

            Sheet hoja = libro.createSheet("Lista de Productos");

            // --- Estilos para los encabezados ---
            Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 2. Armar los encabezados (saltando columnas de control si es necesario)
            Row filaEncabezado = hoja.createRow(0);
            int colExcel = 0; // Índice propio para las columnas en Excel

            // Buscamos dónde está la columna "rubro" en la JTable para usarla en el filtro de filas
            int colRubroIndex = -1;

            for (int colVis = 0; colVis < tabla.getColumnCount(); colVis++) {
                String nombreColumna = tabla.getColumnName(colVis).toLowerCase();

                if (nombreColumna.contains("rubro")) {
                    colRubroIndex = colVis;
                }

                // 🌟 FILTRO: Saltamos columnas de control si aplica
                if (nombreColumna.contains("seleccionar")) {
                    continue;
                }

                Cell celda = filaEncabezado.createCell(colExcel);
                celda.setCellValue(tabla.getColumnName(colVis));
                celda.setCellStyle(estiloEncabezado);
                colExcel++;
            }

            // 3. Volcar los datos aplicando el filtro de rubro
            int filaExcelIndice = 1; // Contador de filas creadas en el Excel

            for (int filaVis = 0; filaVis < tabla.getRowCount(); filaVis++) {
                int modelFila = tabla.convertRowIndexToModel(filaVis);

                // 🌟 EVALUACIÓN DEL FILTRO: Si no es "TODOS", filtramos por el rubro de la celda
                if (!rubroFiltro.equalsIgnoreCase("TODOS") && colRubroIndex != -1) {
                    int modelColRubro = tabla.convertColumnIndexToModel(colRubroIndex);
                    Object valRubro = tabla.getModel().getValueAt(modelFila, modelColRubro);
                    String rubroTexto = (valRubro != null) ? valRubro.toString().trim() : "";

                    if (!rubroTexto.equalsIgnoreCase(rubroFiltro)) {
                        continue; // Saltamos esta fila si no coincide con el rubro elegido
                    }
                }

                Row filaExcel = hoja.createRow(filaExcelIndice++);
                colExcel = 0; // Reiniciamos el contador de columnas para esta fila

                for (int colVis = 0; colVis < tabla.getColumnCount(); colVis++) {
                    String nombreColumna = tabla.getColumnName(colVis).toLowerCase();

                    // 🌟 FILTRO IDÉNTICO: Saltamos datos de columnas no deseadas
                    if (nombreColumna.contains("seleccionar")) {
                        continue;
                    }

                    Cell celda = filaExcel.createCell(colExcel);

                    int modelCol = tabla.convertColumnIndexToModel(colVis);
                    Object valor = tabla.getModel().getValueAt(modelFila, modelCol);

                    if (valor != null) {
                        if (valor instanceof Number) {
                            celda.setCellValue(((Number) valor).doubleValue());
                        } else {
                            celda.setCellValue(valor.toString());
                        }
                    }
                    colExcel++;
                }
            }

            // 4. Autoajustar las columnas creadas en Excel
            for (int i = 0; i < colExcel; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(fos);
        }

        return archivoTemporal;
    }
}
