package veterinaria.reportes.impl;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class ClienteListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CLIENTE_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE CLIENTES";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay clientes para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        // 🌟 Leer el parámetro opcional de filtro por estado
        String estadoFiltro = request.get("estadoFiltro", String.class);
        if (estadoFiltro == null || estadoFiltro.isBlank()) {
            estadoFiltro = "TODOS";
        } else {
            estadoFiltro = estadoFiltro.trim().toUpperCase();
        }

        int colEstado = 10;

        // Filtrar índices de filas según lo seleccionado
        List<Integer> filasFiltradas = new ArrayList<>();
        for (int r = 0; r < m.getRowCount(); r++) {
            String est = safe(m.getValueAt(r, colEstado)).toUpperCase();
            if ("TODOS".equals(estadoFiltro) || est.equals(estadoFiltro)) {
                filasFiltradas.add(r);
            }
        }

        String infoHeader = "Total listados: " + filasFiltradas.size();
        if (!"TODOS".equals(estadoFiltro)) {
            infoHeader += "  |  Filtro Estado: " + estadoFiltro;
        }
        pdf.down(8f);
        pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), infoHeader);
        pdf.down(18f);

        int[] cols = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        String[] headers = {"NOMBRE", "APELLIDO", "DNI", "DIRECCIÓN", "E-MAIL", "TEL.", "RAZÓN SOCIAL", "CUIT", "ESTADO", "F. ALTA"};

        final int fontSize = 7;
        final float lineH = 11f; // Mayor espacio de línea

        // 🌟 Pesos asignados para dar MUCHO espacio a DIRECCIÓN y E-MAIL
        float[] propWeights = new float[]{8f, 8f, 7f, 18f, 18f, 9f, 12f, 9f, 6f, 7f};
        float totalWeight = 0;
        for (float w : propWeights) {
            totalWeight += w;
        }

        float usableWidth = pdf.getUsableWidth();
        float[] w = new float[headers.length];
        for (int i = 0; i < w.length; i++) {
            w[i] = (propWeights[i] / totalWeight) * usableWidth;
        }

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        for (int i = 0; i < w.length; i++) {
            cx[i] = x;
            x += w[i];
        }

        // Encabezados
        for (int i = 0; i < headers.length; i++) {
            pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[i], pdf.getY(), headers[i]);
        }
        pdf.down(10f);
        pdf.line();
        pdf.down(6f);

        // Renderizado de filas
        for (int r : filasFiltradas) {
            String[] data = new String[cols.length];
            for (int c = 0; c < cols.length; c++) {
                data[c] = safe(m.getValueAt(r, cols[c]));
            }

            List<List<String>> wrapped = new ArrayList<>();
            int maxLines = 1;
            for (int i = 0; i < w.length; i++) {
                List<String> lines = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, data[i], w[i] - 3f);
                wrapped.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }

            float neededSpace = (maxLines * lineH) + 6f;
            pdf.ensureSpace(neededSpace);

            float yTop = pdf.getY();
            for (int i = 0; i < maxLines; i++) {
                float yy = yTop - (i * lineH);
                for (int col = 0; col < w.length; col++) {
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[col], yy, pick(wrapped.get(col), i));
                }
            }
            // 🌟 Más separación entre filas para que no queden pegadas
            pdf.down((maxLines * lineH) + 8f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o).trim();
    }

    private String pick(List<String> lines, int idx) {
        return (lines == null || idx >= lines.size()) ? "" : lines.get(idx);
    }
}
