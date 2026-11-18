package veterinaria.reportes.impl;

import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

/** Reporte legible para impresión A4 apaisada, independiente de la página visible de la tabla. */
public class VisitaListadoPdfReporte extends AbstractPdfReporte {

    @Override public ReporteTipo tipo() { return ReporteTipo.VISITA_LISTADO; }
    @Override protected String titulo() { return "LISTADO DE CONSULTAS"; }
    @Override protected boolean isLandscape() { return true; }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        List<?> filas = request.get("filas", List.class);
        if (filas == null || filas.isEmpty())
            throw new Exception("No hay consultas para el informe seleccionado.");
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        List<?> fuente = request.get("filas", List.class);
        String filtro = request.get("filtro", String.class);
        pdf.down(7f);
        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Consultas incluidas: " + (filtro == null ? "Todas" : filtro)
                + "    |    Total: " + fuente.size());
        pdf.down(24f);

        String[] titulos = {"N.º", "PACIENTE", "RESPONSABLE", "MOTIVO",
                "ESTADO", "VETERINARIO", "PRIORIDAD", "FECHA"};
        float[] pesos = {5f, 12f, 15f, 19f, 12f, 16f, 10f, 11f};
        float total = 0;
        for (float peso : pesos) total += peso;
        float[] ancho = new float[pesos.length];
        float[] x = new float[pesos.length];
        float cursor = pdf.getMargin();
        for (int i = 0; i < pesos.length; i++) {
            ancho[i] = (pesos[i] / total) * pdf.getUsableWidth();
            x[i] = cursor;
            cursor += ancho[i];
        }
        encabezadoColumnas(pdf, titulos, x);

        for (Object entrada : fuente) {
            if (!(entrada instanceof String[])) continue;
            String[] original = (String[]) entrada;
            // Entrada: ID, Paciente, Cliente, Motivo, Estado, Veterinario, Prioridad, Fecha.
            List<List<String>> celdas = new ArrayList<>();
            int maxLineas = 1;
            for (int i = 0; i < ancho.length; i++) {
                String valor = i < original.length && original[i] != null ? original[i].trim() : "";
                if (i == 7) valor = fechaLegible(valor);
                List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 8, valor,
                        Math.max(8f, ancho[i] - 7f));
                celdas.add(lineas);
                maxLineas = Math.max(maxLineas, lineas.size());
            }
            float alto = maxLineas * 12f + 12f;
            int paginaAnterior = pdf.getCurrentPage();
            // Reservar espacio para repetir el título y las columnas si esta fila salta de página.
            if (pdf.getY() - alto - 5f < pdf.getMargin()) {
                pdf.nuevaPagina();
            }
            if (pdf.getCurrentPage() != paginaAnterior) {
                encabezadoContinuacion(pdf, filtro, fuente.size(), titulos, x);
            }
            float arriba = pdf.getY();
            for (int col = 0; col < ancho.length; col++) {
                List<String> lineas = celdas.get(col);
                for (int l = 0; l < lineas.size(); l++) {
                    pdf.text(PdfStyle.TXT_FONT, 8, x[col], arriba - l * 12f, lineas.get(l));
                }
            }
            pdf.down(alto);
            pdf.line();
            pdf.down(3f);
        }
    }
    private void encabezadoColumnas(PdfDocument pdf, String[] titulos, float[] x) throws Exception {
        pdf.ensureSpace(40f);
        for (int i = 0; i < titulos.length; i++) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 8, x[i], pdf.getY(), titulos[i]);
        }
        pdf.down(13f);
        pdf.line();
        pdf.down(6f);
    }

    private void encabezadoContinuacion(PdfDocument pdf, String filtro,
            int total, String[] titulos, float[] x) throws Exception {
        String titulo = titulo();
        float anchoTitulo = pdf.textWidth(PdfStyle.H2_FONT, 12, titulo);
        pdf.text(PdfStyle.H2_FONT, 12, (pdf.getPageW() - anchoTitulo) / 2f,
                pdf.getY(), titulo);
        pdf.down(19f);
        pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(),
                "Consultas incluidas: " + (filtro == null ? "Todas" : filtro)
                        + "    |    Total: " + total);
        pdf.down(22f);
        encabezadoColumnas(pdf, titulos, x);
    }

    private String fechaLegible(String fecha) {
        if (fecha == null || fecha.isBlank()) return "-";
        String limpio = fecha.replace(" a las ", " ");
        try {
            java.time.LocalDateTime valor = java.time.LocalDateTime.parse(
                    limpio.trim().replace(' ', 'T'));
            return valor.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"));
        } catch (java.time.format.DateTimeParseException ex) {
            return limpio;
        }
    }

}
