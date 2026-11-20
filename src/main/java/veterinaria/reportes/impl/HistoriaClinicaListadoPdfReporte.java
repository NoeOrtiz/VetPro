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
        TableModel modelo = tabla.getModel();
        float x = pdf.getMargin();
        float ancho = pdf.getUsableWidth();

        pdf.down(9f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(),
                "Paciente: " + (mascota == null ? "No registrado" : safe(mascota.getNombre())));
        pdf.down(19f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Total de eventos: " + modelo.getRowCount());
        pdf.down(24f);

        // El modelo conserva ID, Fecha, Tipo, Profesional, Resumen y Observaciones
        // aunque ID esté oculto y la tabla en pantalla esté filtrada/paginada.
        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            String id = safe(modelo.getValueAt(fila, 1));
            String fecha = safe(modelo.getValueAt(fila, 2));
            String tipo = safe(modelo.getValueAt(fila, 3));
            String profesional = safe(modelo.getValueAt(fila, 4));
            String resumen = safe(modelo.getValueAt(fila, 5));
            String observaciones = safe(modelo.getValueAt(fila, 6));

            java.util.List<String> lResumen = pdf.wrapText(
                    PdfStyle.TXT_FONT, 10, resumen, ancho - 22f);
            java.util.List<String> lObservaciones = pdf.wrapText(
                    PdfStyle.TXT_FONT, 10, observaciones, ancho - 22f);
            float alto = 52f + 18f * (lResumen.size() + lObservaciones.size()) + 26f;
            if (pdf.getY() - alto < pdf.getMargin() + 18f) {
                pdf.nuevaPagina();
                renderHeader(pdf, request);
                pdf.down(14f);
            }

            pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(),
                    "EVENTO N.º " + id);
            pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 145f, pdf.getY(),
                    "Fecha: " + fecha);
            pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 330f, pdf.getY(),
                    "Tipo: " + tipo);
            pdf.down(20f);
            pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(),
                    "Profesional: " + (profesional.isBlank() ? "No registrado" : profesional));
            pdf.down(24f);

            bloque(pdf, "RESUMEN", lResumen);
            pdf.down(10f);
            bloque(pdf, "OBSERVACIONES", lObservaciones);
            pdf.down(13f);
            pdf.line();
            pdf.down(20f);
        }
    }

    private void bloque(PdfDocument pdf, String etiqueta,
            java.util.List<String> lineas) throws Exception {
        float x = pdf.getMargin();
        pdf.ensureSpace(36f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), etiqueta);
        pdf.down(18f);
        for (int i = 0; i < lineas.size(); i++) {
            pdf.ensureSpace(23f);
            String texto = lineas.get(i);
            pdf.textJustified(PdfStyle.TXT_FONT, 10, x + 11f,
                    pdf.getY(), texto, pdf.getUsableWidth() - 22f,
                    i < lineas.size() - 1 && !texto.isBlank());
            pdf.down(18f);
        }
    }

    private String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
