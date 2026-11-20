package veterinaria.reportes.impl;

import javax.swing.JTable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.table.TableModel;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import veterinaria.entidad.Mascota;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

/** Listado clínico legible: cada evento tiene su propio bloque de información. */
public class HistoriaClinicaListadoPdfReporte extends AbstractPdfReporte {

    @Override public ReporteTipo tipo() { return ReporteTipo.HISTORIA_CLINICA_LISTADO; }
    @Override protected String titulo() { return "LISTADO DE HISTORIA CLÍNICA"; }
    @Override protected boolean isLandscape() { return true; }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getModel().getRowCount() == 0) {
            throw new Exception("No hay registros de historia clínica para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        Mascota mascota = request.get("mascota", Mascota.class);
        TableModel m = tabla.getModel();
        float x = pdf.getMargin();
        float ancho = pdf.getUsableWidth();
        float separacion = 22f;
        float colResumen = ancho * 0.39f;
        float xObs = x + colResumen + separacion;
        float colObs = ancho - colResumen - separacion;

        pdf.down(8f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(),
                "Paciente: " + (mascota == null ? "No registrado" : safe(mascota.getNombre())));
        pdf.down(19f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Eventos: " + m.getRowCount());
        pdf.down(19f);

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, 1));
            String fecha = safe(m.getValueAt(r, 2));
            String tipo = safe(m.getValueAt(r, 3));
            String profesional = safe(m.getValueAt(r, 4));
            String resumen = safe(m.getValueAt(r, 5));
            String observaciones = formatearImporte(tipo, safe(m.getValueAt(r, 6)));

            java.util.List<String> lr = pdf.wrapText(PdfStyle.TXT_FONT, 9,
                    resumen, colResumen - 12f);
            java.util.List<String> lo = pdf.wrapText(PdfStyle.TXT_FONT, 9,
                    observaciones, colObs - 12f);
            int renglones = Math.max(lr.size(), lo.size());
            float alto = 18f + 17f + 17f + (renglones * 14f) + 17f;
            if (pdf.getY() - alto < pdf.getMargin() + 20f) {
                pdf.nuevaPagina();
                renderHeader(pdf, request);
                pdf.down(12f);
            }

            float arriba = pdf.getY();
            pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, arriba,
                    "N.º " + id + "  |  " + tipo + "  |  " + fecha);
            pdf.text(PdfStyle.TXT_FONT, 9, xObs, arriba,
                    "Profesional: " + (profesional.isBlank() ? "No registrado" : profesional));
            pdf.down(18f);
            pdf.text(PDType1Font.HELVETICA_BOLD, 8, x, pdf.getY(), "RESUMEN");
            pdf.text(PDType1Font.HELVETICA_BOLD, 8, xObs, pdf.getY(), "OBSERVACIONES");
            pdf.down(17f);
            float yTexto = pdf.getY();
            for (int i = 0; i < lr.size(); i++) {
                pdf.text(PdfStyle.TXT_FONT, 9, x + 6f, yTexto - i * 14f, lr.get(i));
            }
            for (int i = 0; i < lo.size(); i++) {
                pdf.text(PdfStyle.TXT_FONT, 9, xObs + 6f, yTexto - i * 14f, lo.get(i));
            }
            pdf.down(renglones * 14f + 9f);
            pdf.line();
            pdf.down(8f);
        }
    }

    /** Solo formatear como importe los valores monetarios completos;
     * nunca convertir pesos corporales, dosis u observaciones clínicas. */
    private String formatearImporte(String tipo, String valor) {
        if (tipo == null || !tipo.toUpperCase(Locale.ROOT).contains("PELUQUER")
                || valor == null || !valor.trim().matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            return valor;
        }
        try {
            NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);
            return "$ " + formato.format(new BigDecimal(valor.trim()));
        } catch (Exception ex) {
            return valor;
        }
    }

    private String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
