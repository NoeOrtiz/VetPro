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

        int colId = 0;
        int colFecha = 1;
        int colTipo = 2;
        int colMonto = 3;
        int colUsuario = 4;
        int colMetodo = 5;
        int colRecibo = 6;
        int colDesc = 7;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[] {
            45f,   // ID
            65f,   // Fecha
            65f,   // Tipo
            70f,   // Monto
            110f,  // Usuario
            90f,   // Método
            60f,   // Recibo
            235f   // Descripción
        };

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "ID");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "FECHA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "TIPO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "MONTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "USUARIO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "MÉTODO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "RECIBO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[7], pdf.getY(), "DESCRIPCIÓN");
        pdf.down(10f);
        pdf.line();

        BigDecimal totalCreditos = BigDecimal.ZERO;
        BigDecimal totalDebitos = BigDecimal.ZERO;

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String fecha = safe(m.getValueAt(r, colFecha));
            String tipo = safe(m.getValueAt(r, colTipo));
            String montoStr = safe(m.getValueAt(r, colMonto));
            String usuario = safe(m.getValueAt(r, colUsuario));
            String metodo = safe(m.getValueAt(r, colMetodo));
            String recibo = safe(m.getValueAt(r, colRecibo));
            String desc = safe(m.getValueAt(r, colDesc));

            java.util.List<String> descLines = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, desc, w[7] - 4);
            int rows = Math.max(1, descLines.size());

            float needed = (rows * lineH) + 10f;
            pdf.ensureSpace(needed);

            for (int i = 0; i < rows; i++) {
                String dline = i < descLines.size() ? descLines.get(i) : "";
                if (i == 0) {
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[0], pdf.getY(), id);
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[1], pdf.getY(), fecha);
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[2], pdf.getY(), tipo);
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[3], pdf.getY(), montoStr);
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4], pdf.getY(), usuario);
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[5], pdf.getY(), metodo);
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[6], pdf.getY(), recibo);
                }
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[7], pdf.getY(), dline);
                pdf.down(lineH);
            }
            pdf.down(2f);

            BigDecimal monto = parseMoneySafe(montoStr);
            String t = tipo.toUpperCase(Locale.ROOT);
            if (t.contains("CREDITO")) totalCreditos = totalCreditos.add(monto);
            if (t.contains("DEBITO")) totalDebitos = totalDebitos.add(monto);
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
