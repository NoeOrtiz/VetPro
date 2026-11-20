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
    @Override protected String titulo() { return "RESUMEN DE ATENCIONES CLÍNICAS"; }
    @Override protected boolean isLandscape() { return true; }

    /**
     * Encabezado exclusivo de Historia Clínica: logo protagonista,
     * sin modificar la presentación de los demás reportes de VetPRO.
     */
    @Override
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        float anchoPagina = pdf.getPageW();
        float ySuperior = pdf.getY();
        float anchoLogo = 160f;
        float altoLogo = 116f;
        renderLogo(pdf, (anchoPagina - anchoLogo) / 2f,
                ySuperior - altoLogo, anchoLogo, altoLogo);

        pdf.setY(ySuperior - altoLogo - 22f);
        String textoTitulo = titulo();
        float anchoTitulo = pdf.textWidth(PdfStyle.H2_FONT, 14, textoTitulo);
        pdf.text(PdfStyle.H2_FONT, 14,
                (anchoPagina - anchoTitulo) / 2f, pdf.getY(), textoTitulo);
        pdf.down(19f);
        pdf.line();
        pdf.down(9f);
    }

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
        float margen = pdf.getMargin();
        float ancho = pdf.getUsableWidth();

        pdf.down(6f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, margen, pdf.getY(),
                "Paciente: " + (mascota == null ? "No registrado" : safe(mascota.getNombre())));
        pdf.text(PdfStyle.TXT_FONT, 9, margen + ancho - 95f, pdf.getY(),
                "Registros: " + m.getRowCount());
        pdf.down(26f);

        float[] proporciones = {0.16f, 0.14f, 0.17f, 0.23f, 0.30f};
        float[] x = new float[5];
        float[] w = new float[5];
        float actual = margen;
        for (int i = 0; i < 5; i++) {
            x[i] = actual;
            w[i] = ancho * proporciones[i];
            actual += w[i];
        }
        encabezadoTabla(pdf, x);

        for (int fila = 0; fila < m.getRowCount(); fila++) {
            String tipo = safe(m.getValueAt(fila, 3));
            String[] valores = {
                safe(m.getValueAt(fila, 2)),
                tipo,
                safe(m.getValueAt(fila, 4)),
                safe(m.getValueAt(fila, 5)),
                formatearImporte(tipo, safe(m.getValueAt(fila, 6)))
            };
            java.util.List<java.util.List<String>> celdas = new java.util.ArrayList<>();
            int maxLineas = 1;
            for (int i = 0; i < valores.length; i++) {
                java.util.List<String> lineas = pdf.wrapText(
                        PdfStyle.TXT_FONT, 9, valores[i], w[i] - 16f);
                celdas.add(lineas);
                maxLineas = Math.max(maxLineas, lineas.size());
            }
            float alto = 12f + maxLineas * 15f;
            if (pdf.getY() - alto < pdf.getMargin() + 15f) {
                pdf.nuevaPagina();
                renderHeader(pdf, request);
                pdf.down(8f);
                encabezadoTabla(pdf, x);
            }
            float arriba = pdf.getY();
            for (int col = 0; col < celdas.size(); col++) {
                java.util.List<String> lineas = celdas.get(col);
                for (int i = 0; i < lineas.size(); i++) {
                    pdf.text(PdfStyle.TXT_FONT, 9, x[col] + 7f,
                            arriba - 7f - i * 15f, lineas.get(i));
                }
            }
            pdf.down(alto);
            pdf.line();
            pdf.down(3f);
        }
    }

    private void encabezadoTabla(PdfDocument pdf, float[] x) throws Exception {
        String[] titulos = {"FECHA", "TIPO", "PROFESIONAL", "RESUMEN", "OBSERVACIONES"};
        pdf.fillRect(pdf.getMargin(), pdf.getY() - 20f,
                pdf.getUsableWidth(), 29f, new java.awt.Color(231, 236, 241));
        for (int i = 0; i < titulos.length; i++) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 9,
                    x[i] + 7f, pdf.getY() - 8f, titulos[i]);
        }
        pdf.down(37f);
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
