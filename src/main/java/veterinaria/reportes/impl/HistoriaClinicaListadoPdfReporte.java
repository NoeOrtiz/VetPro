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

        int colId = 0;
        int colFecha = 1;
        int colTipo = 2;
        int colProf = 3;
        int colResumen = 4;
        int colObs = 5;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[]{
            25f,   // ID
            70f,   // Fecha
            55f,   // Tipo
            85f,   // Profesional
            170f,  // Resumen
            210f   // Observaciones
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
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "PROFESIONAL");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "RESUMEN");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "OBSERVACIONES");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String fecha = safe(m.getValueAt(r, colFecha));
            String tipo = safe(m.getValueAt(r, colTipo));
            String prof = safe(m.getValueAt(r, colProf));
            String resumen = safe(m.getValueAt(r, colResumen));
            String obs = safe(m.getValueAt(r, colObs));

            java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, id, w[0] - 2);
            java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fecha, w[1] - 2);
            java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, tipo, w[2] - 2);
            java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, prof, w[3] - 2);
            java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, resumen, w[4] - 2);
            java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, obs, w[5] - 2);

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
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4], yy, pick(l4, i));
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
