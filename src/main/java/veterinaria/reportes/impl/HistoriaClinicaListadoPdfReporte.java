package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Mascota;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class HistoriaClinicaListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.HISTORIA_CLINICA_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE HISTORIA CLÍNICA";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay registros de historia clínica para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        Mascota mascota = request.get("mascota", Mascota.class);
        TableModel m = tabla.getModel();

        String cabecera = "";
        if (mascota != null) {
            cabecera = "Paciente: " + safe(mascota.getNombre()) + " (#" + safe(mascota.getIdMascota()) + ")";
        }

        if (!cabecera.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), cabecera);
            pdf.down(14f);
        }

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de eventos listados: " + m.getRowCount());
        pdf.down(16f);

        int colId = 1;
        int colFecha = 2;
        int colTipo = 3;
        int colProf = 4;
        int colResumen = 5;
        int colObs = 6;

        final int fontSize = 9;
        final float lineH = 14f;

        float[] pesos = {4f, 13f, 12f, 16f, 23f, 32f};
        float[] w = new float[pesos.length];
        for (int i = 0; i < pesos.length; i++) {
            w[i] = pdf.getUsableWidth() * pesos[i] / 100f;
        }

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        dibujarEncabezado(pdf, cx, fontSize);

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String fecha = safe(m.getValueAt(r, colFecha));
            String tipo = safe(m.getValueAt(r, colTipo));
            String prof = safe(m.getValueAt(r, colProf));
            String resumen = safe(m.getValueAt(r, colResumen));
            String obs = safe(m.getValueAt(r, colObs));

            java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, id, w[0] - 9);
            java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fecha, w[1] - 9);
            java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, tipo, w[2] - 9);
            java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, prof, w[3] - 9);
            java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, resumen, w[4] - 9);
            java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, obs, w[5] - 9);

            int maxLines = max(l0.size(), l1.size(), l2.size(), l3.size(), l4.size(), l5.size());
            float rowH = Math.max(1, maxLines) * lineH + 14f;
            if (pdf.getY() - rowH < pdf.getMargin() + 14f) {
                pdf.nuevaPagina();
                renderHeader(pdf, request);
                dibujarEncabezado(pdf, cx, fontSize);
            }

            float yTop = pdf.getY();
            for (int i = 0; i < maxLines; i++) {
                float yy = yTop - (i * lineH);
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[0] + 3f, yy, pick(l0, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[1] + 3f, yy, pick(l1, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[2] + 3f, yy, pick(l2, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[3] + 3f, yy, pick(l3, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4] + 3f, yy, pick(l4, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[5] + 3f, yy, pick(l5, i));
            }
            pdf.down(rowH);
            pdf.line();
            pdf.down(3f);
        }
    }

    private void dibujarEncabezado(PdfDocument pdf, float[] cx, int fontSize) throws Exception {
        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0] + 3f, pdf.getY(), "ID");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1] + 3f, pdf.getY(), "FECHA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2] + 3f, pdf.getY(), "TIPO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3] + 3f, pdf.getY(), "PROFESIONAL");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4] + 3f, pdf.getY(), "RESUMEN");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5] + 3f, pdf.getY(), "OBSERVACIONES");
        pdf.down(17f);
        pdf.line();

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
