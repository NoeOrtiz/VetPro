package veterinaria.reportes.impl;

import java.math.BigDecimal;
import java.util.Locale;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class CajaMovimientosListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CAJA_MOVIMIENTOS_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - MOVIMIENTOS DE CAJA";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay movimientos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        String filtros = request.get("filtros", String.class);
        if (filtros != null && !filtros.trim().isEmpty()) {
            pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), filtros);
            pdf.down(16f);
        }

        // El ID es técnico: no se muestra en el documento.
        final int[] columnas = {1, 2, 3, 4, 5, 6, 7};
        final String[] titulos = {"FECHA", "TIPO", "MONTO", "USUARIO", "MÉTODO", "RECIBO", "DESCRIPCIÓN"};
        final float[] pesos = {75f, 65f, 85f, 95f, 105f, 75f, 210f};
        final int fontSize = 8;
        final float lineH = 11f;
        final float disponible = pdf.getUsableWidth();
        float suma = 0f;
        for (float peso : pesos) suma += peso;
        final float[] w = new float[pesos.length];
        final float[] cx = new float[pesos.length];
        float cursor = pdf.getMargin();
        for (int i = 0; i < pesos.length; i++) {
            w[i] = pesos[i] * disponible / suma;
            cx[i] = cursor;
            cursor += w[i];
        }
        pdf.ensureSpace(60f);
        for (int i = 0; i < titulos.length; i++) {
            pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[i], pdf.getY(), titulos[i]);
        }
        pdf.down(13f);
        pdf.line();

        BigDecimal totalCreditos = BigDecimal.ZERO;
        BigDecimal totalDebitos = BigDecimal.ZERO;

        for (int r = 0; r < m.getRowCount(); r++) {
            java.util.List<java.util.List<String>> celdas = new java.util.ArrayList<>();
            int lineas = 1;
            for (int i = 0; i < columnas.length; i++) {
                java.util.List<String> lineasCelda = pdf.wrapText(
                        PdfStyle.TXT_FONT, fontSize, safe(m.getValueAt(r, columnas[i])),
                        Math.max(20f, w[i] - 6f));
                celdas.add(lineasCelda);
                lineas = Math.max(lineas, lineasCelda.size());
            }
            pdf.ensureSpace(lineas * lineH + 9f);
            for (int linea = 0; linea < lineas; linea++) {
                for (int i = 0; i < columnas.length; i++) {
                    if (linea < celdas.get(i).size()) {
                        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[i], pdf.getY(),
                                celdas.get(i).get(linea));
                    }
                }
                pdf.down(lineH);
            }
            pdf.down(3f);
            BigDecimal monto = parseMoneySafe(safe(m.getValueAt(r, 3)));
            String tipo = safe(m.getValueAt(r, 2));
            String normalizado = java.text.Normalizer.normalize(tipo, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
            if (normalizado.contains("CREDITO")) totalCreditos = totalCreditos.add(monto);
            if (normalizado.contains("DEBITO")) totalDebitos = totalDebitos.add(monto);
        }

        pdf.line();
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, pdf.getMargin(), pdf.getY(), "Totales:");
        pdf.down(12f);
        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Ventas (Créditos): $ " + formatMoney(totalCreditos) + "   |   Compras (Débitos): $ " + formatMoney(totalDebitos));
    }

    private static String safe(Object o) {
        return o != null ? o.toString() : "";
    }

    private static String formatMoney(BigDecimal v) {
        try {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new Locale("es", "AR"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(v != null ? v : BigDecimal.ZERO);
        } catch (Exception e) {
            return String.valueOf(v != null ? v : BigDecimal.ZERO);
        }
    }

    private static BigDecimal parseMoneySafe(String s) {
        if (s == null) return BigDecimal.ZERO;
        try {
            String clean = s.replace("$", "").trim();
            if (clean.contains(",") && clean.contains(".")) {
                clean = clean.replace(".", "").replace(",", ".");
            } else if (clean.contains(",")) {
                clean = clean.replace(",", ".");
            }
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
