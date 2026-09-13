package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class HistorialTurnosListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.HISTORIAL_TURNOS_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - HISTORIAL DE TURNOS (LISTADO)";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay turnos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {

        String filtros = request.get("filtros", String.class);
        if (filtros != null && !filtros.trim().isEmpty()) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 10, pdf.getMargin(), pdf.getY(), "Filtros aplicados:");
            pdf.down(12f);

            String[] lines = filtros.split("\\r?\\n");
            for (String line : lines) {
                if (line == null) continue;
                String s = line.trim();
                if (s.isEmpty()) continue;

                float maxW = pdf.getPageW() - (pdf.getMargin() * 2);
                java.util.List<String> wrapped = pdf.wrapText(PdfStyle.TXT_FONT, 9, s, maxW);
                for (String wl : wrapped) {
                    pdf.ensureSpace(14f);
                    pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), wl);
                    pdf.down(12f);
                }
            }
            pdf.down(6f);
            pdf.line();
        }

        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de turnos listados: " + m.getRowCount());
        pdf.down(16f);

        int colNro = 1;
        int colFechaTurno = 2;
        int colPaciente = 3;
        int colVet = 6;
        int colPrecio = 7;
        int colEstado = 8;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[] {
            30f,   // N°
            95f,   // Fecha
            140f,  // Paciente
            130f,  // Veterinario
            55f,   // Precio
            75f    // Estado
        };

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "N°");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "FECHA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "PACIENTE");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "VETERINARIO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "PRECIO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "ESTADO");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {

            String nro = safe(m.getValueAt(r, colNro));
            String fechaTurno = safe(m.getValueAt(r, colFechaTurno));
            String paciente = safe(m.getValueAt(r, colPaciente));
            String vet = safe(m.getValueAt(r, colVet));
            String precio = safe(m.getValueAt(r, colPrecio));
            String estado = safe(m.getValueAt(r, colEstado));

            java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, nro, w[0] - 2);
            java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fechaTurno, w[1] - 2);
            java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, paciente, w[2] - 2);
            java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, vet, w[3] - 2);
            java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, precio, w[4] - 2);
            java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, estado, w[5] - 2);

            int maxLines = max(l0.size(), l1.size(), l2.size(), l3.size(), l4.size(), l5.size());
            float rowH = Math.max(1, maxLines) * lineH;
            pdf.ensureSpace(rowH + 6f);

            float yTop = pdf.getY();
            for (int i = 0; i < maxLines; i++) {
                float yy = yTop - (i * lineH);
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[0], yy, pick(l0, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[1], yy, pick(l1, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[2], yy, pick(l2, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[3], yy, pick(l3, i));

                String p = pick(l4, i);
                float pw = pdf.textWidth(PdfStyle.TXT_FONT, fontSize, p);
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4] + (w[4] - 2) - pw, yy, p);

                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[5], yy, pick(l5, i));
            }

            pdf.down(rowH + 2f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String pick(java.util.List<String> lines, int idx) {
        if (lines == null || lines.isEmpty()) return "";
        if (idx < 0 || idx >= lines.size()) return "";
        return lines.get(idx) != null ? lines.get(idx) : "";
    }

    private int max(int... v) {
        int m = 0;
        if (v == null) return 0;
        for (int n : v) if (n > m) m = n;
        return m;
    }
}
