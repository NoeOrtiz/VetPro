package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class VisitaListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.VISITA_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE VISITAS";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay visitas para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de visitas listadas: " + m.getRowCount());
        pdf.down(16f);

        int colId = 1;
        int colPaciente = 2;
        int colCliente = 3;
        int colMotivo = 4;
        int colPatologia = 5;
        int colTratamiento = 6;
        int colFecha = 7;
        int colEstado = 8;
        int colVet = 9;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[]{
            15f,   // ID
            45f,   // Paciente
            60f,  // Cliente
            140f,  // Motivo
            110f,  // Patología
            160f,  // Tratamiento
            55f,   // Fecha
            60f,   // Estado
            70f    // Veterinario
        };

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "Nº");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "PACIENTE");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "CLIENTE");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "MOTIVO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "PATOLOGÍA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "TRATAMIENTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "FECHA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[7], pdf.getY(), "ESTADO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[8], pdf.getY(), "VETERINARIO");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String paciente = safe(m.getValueAt(r, colPaciente));
            String cliente = safe(m.getValueAt(r, colCliente));
            String motivo = safe(m.getValueAt(r, colMotivo));
            String patologia = safe(m.getValueAt(r, colPatologia));
            String tratamiento = safe(m.getValueAt(r, colTratamiento));
            String fecha = safe(m.getValueAt(r, colFecha));
            String estado = safe(m.getValueAt(r, colEstado));
            String vet = safe(m.getValueAt(r, colVet));

            java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, id, w[0] - 2);
            java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, paciente, w[1] - 2);
            java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, cliente, w[2] - 2);
            java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, motivo, w[3] - 2);
            java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, patologia, w[4] - 2);
            java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, tratamiento, w[5] - 2);
            java.util.List<String> l6 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fecha, w[6] - 2);
            java.util.List<String> l7 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, estado, w[7] - 2);
            java.util.List<String> l8 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, vet, w[8] - 2);

            int maxLines = max(l0.size(), l1.size(), l2.size(), l3.size(), l4.size(), l5.size(), l6.size(), l7.size(), l8.size());
            float rowH = Math.max(1, maxLines) * lineH;
            pdf.ensureSpace(rowH + 6f);

            float yTop = pdf.getY();
            for (int i = 0; i < maxLines; i++) {
                float yy = yTop - (i * lineH);
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[0], yy, pick(l0, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[1], yy, pick(l1, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[2], yy, pick(l2, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[3], yy, pick(l3, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4], yy, pick(l4, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[5], yy, pick(l5, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[6], yy, pick(l6, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[7], yy, pick(l7, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[8], yy, pick(l8, i));
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
