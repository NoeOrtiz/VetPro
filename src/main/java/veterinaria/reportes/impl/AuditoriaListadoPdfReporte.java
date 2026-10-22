package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class AuditoriaListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.AUDITORIA_LISTADO;
    }

    @Override
    protected String titulo() {
        return "AUDITORIA - LISTADO DE EVENTOS";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay eventos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {

        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        String filtros = request.get("filtros", String.class);
        if (filtros == null) {
            filtros = "";
        }

        if (!filtros.isEmpty()) {
            pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(),
                    "Filtros: " + filtros);
            pdf.down(12f);
        }

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Total de eventos: " + m.getRowCount());

        pdf.down(14f);

        final String[] titulos = {"FECHA/HORA", "USUARIO", "ACCIÓN", "ENTIDAD", "MÓDULO", "DESCRIPCIÓN", "RESULTADO"};
        final float[] pesos = {125f, 95f, 70f, 75f, 75f, 230f, 65f};
        final int fontSize = 8;
        final float lineH = 11f;
        float suma = 0f;
        for (float peso : pesos) suma += peso;
        final float[] anchos = new float[pesos.length];
        final float[] posiciones = new float[pesos.length];
        float x = pdf.getMargin();
        for (int i = 0; i < pesos.length; i++) {
            anchos[i] = pesos[i] * pdf.getUsableWidth() / suma;
            posiciones[i] = x;
            x += anchos[i];
        }
        pdf.ensureSpace(40f);
        for (int i = 0; i < titulos.length; i++) {
            pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, posiciones[i], pdf.getY(), titulos[i]);
        }
        pdf.down(13f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            java.util.List<java.util.List<String>> celdas = new java.util.ArrayList<>();
            int lineas = 1;
            for (int c = 0; c < titulos.length; c++) {
                java.util.List<String> partes = pdf.wrapText(
                        PdfStyle.TXT_FONT, fontSize, safe(m.getValueAt(r, c)),
                        Math.max(20f, anchos[c] - 5f));
                celdas.add(partes);
                lineas = Math.max(lineas, partes.size());
            }
            pdf.ensureSpace(lineas * lineH + 8f);
            for (int linea = 0; linea < lineas; linea++) {
                for (int c = 0; c < titulos.length; c++) {
                    if (linea < celdas.get(c).size()) {
                        pdf.text(PdfStyle.TXT_FONT, fontSize, posiciones[c], pdf.getY(),
                                celdas.get(c).get(linea));
                    }
                }
                pdf.down(lineH);
            }
            pdf.down(3f);
        }

        pdf.down(8f);

        pdf.text(
                PdfStyle.TXT_FONT,
                9,
                pdf.getMargin(),
                pdf.getY(),
                "Generado: "
                + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    private String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
