package veterinaria.reportes.pdf;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;

public abstract class AbstractPdfReporte implements Reporte {

    protected static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public final File generar(ReporteRequest request) throws Exception {
        validar(request);

        File out = buildOutputFile(request);

        try (PdfDocument pdf = new PdfDocument(pageSize(), isLandscape())) {
            renderHeader(pdf, request);
            renderBody(pdf, request);

            // 🌟 Dibuja el pie de página en TODAS las hojas generadas
            renderFooterAllPages(pdf, request);

            pdf.save(out);
        }

        return out;
    }

    protected void renderLogo(PdfDocument pdf, float x, float y, float width, float height) {
        try (InputStream logoStream = getClass().getResourceAsStream("/imagenes/logo.png")) {
            if (logoStream != null) {
                File tempLogo = File.createTempFile("logo_reporte_", ".png");
                tempLogo.deleteOnExit();

                java.nio.file.Files.copy(
                        logoStream,
                        tempLogo.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                PDImageXObject logo = PDImageXObject.createFromFile(
                        tempLogo.getAbsolutePath(),
                        pdf.getDocument()
                );

                pdf.image(logo, x, y, width, height);
            }
        } catch (Exception e) {
            System.err.println("No se pudo renderizar el logo: " + e.getMessage());
        }
    }

    protected PDRectangle pageSize() {
        return PDRectangle.A4;
    }

    protected boolean isLandscape() {
        return false;
    }

    protected void validar(ReporteRequest request) throws Exception {
    }

    protected File buildOutputFile(ReporteRequest request) {
        String fileName = tipo().name() + "_" + LocalDateTime.now().format(TS) + ".pdf";
        return veterinaria.reportes.core.ReportePaths.enDescargas(fileName);
    }

    /**
     * Encabezado uniforme con marca, logo, fecha y acento azul petróleo,
     * inspirado en el informe compartido por la usuaria.
     */
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        renderHeaderConLogo(pdf, 74f, 60f);
    }

    protected final void renderHeaderConLogo(PdfDocument pdf, float ancho, float alto) throws Exception {
        float top = pdf.getY();
        float m = pdf.getMargin();
        float right = pdf.getPageW() - m;
        renderLogo(pdf, m, top - alto, ancho, alto);
        float tx = m + ancho + 15f;
        float extraAlto = alto - 60f;
        top -= extraAlto / 2f;
        pdf.text(PdfStyle.TXT_BOLD_FONT, 7, tx, top - 11f,
                "CLÍNICA VETERINARIA DOGTOR CAT", PdfStyle.ACCENT_DARK);
        int size = 13;
        while (size > 9 && pdf.textWidth(PdfStyle.H1_FONT, size, titulo()) > right - tx - 117f) {
            size--;
        }
        pdf.text(PdfStyle.H1_FONT, size, tx, top - 28f, titulo(), PdfStyle.INK);
        pdf.text(PdfStyle.TXT_FONT, 8, tx, top - 43f,
                "Itaembé Guazú - Sistema de gestión VetPRO", PdfStyle.MUTED);
        String ts = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String label = "FECHA DE EMISIÓN";
        pdf.text(PdfStyle.TXT_BOLD_FONT, 7,
                right - pdf.textWidth(PdfStyle.TXT_BOLD_FONT, 7, label),
                top - 14f, label, PdfStyle.MUTED);
        pdf.text(PdfStyle.TXT_BOLD_FONT, 10,
                right - pdf.textWidth(PdfStyle.TXT_BOLD_FONT, 10, ts),
                top - 32f, ts, PdfStyle.INK);
        float linea = top - 71f - extraAlto / 2f;
        pdf.hline(m, right, linea, PdfStyle.ACCENT, 1.5f);
        pdf.setY(linea - 22f);
    }

    protected void renderTable(
            PdfDocument pdf,
            String[] headers,
            float[] widths,
            java.util.List<String[]> rows,
            int[] wrapColumns, // Índices de las columnas que deben hacer wrapText (ej: descripción)
            int fontSize,
            float lineH
    ) throws Exception {

        float x = pdf.getMargin();
        final float[] cx = new float[widths.length];
        cx[0] = x;
        for (int i = 1; i < widths.length; i++) {
            cx[i] = cx[i - 1] + widths[i - 1];
        }

        // Renderizar Cabeceras
        pdf.ensureSpace(40f);
        for (int i = 0; i < headers.length; i++) {
            pdf.text(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 8, cx[i], pdf.getY(), headers[i]);
        }
        pdf.down(10f);
        pdf.line();

        // Renderizar Filas Dinámicas
        for (String[] row : rows) {
            // Calcular cuántas líneas requiere cada celda de esta fila
            int maxLines = 1;
            java.util.Map<Integer, java.util.List<String>> wrappedCells = new java.util.HashMap<>();

            for (int colIdx : wrapColumns) {
                if (colIdx >= 0 && colIdx < row.length) {
                    String cellText = row[colIdx] != null ? row[colIdx] : "";
                    java.util.List<String> lines = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, cellText, widths[colIdx]);
                    wrappedCells.put(colIdx, lines);
                    maxLines = Math.max(maxLines, lines.size());
                }
            }

            float rowHeight = maxLines * lineH + 4f;
            pdf.ensureSpace(rowHeight + 10f);
            float startY = pdf.getY();

            // Imprimir columnas normales (sin wrap) y columnas con wrap
            for (int i = 0; i < row.length; i++) {
                String val = row[i] != null ? row[i] : "";

                if (wrappedCells.containsKey(i)) {
                    // Imprimir texto envuelto (multilínea) bajando el cursor correctamente
                    java.util.List<String> lines = wrappedCells.get(i);
                    float originalY = pdf.getY(); // Guardamos dónde empieza la fila
                    for (int l = 0; l < lines.size(); l++) {
                        if (l > 0) {
                            pdf.down(lineH); // Bajamos el cursor de la página para la siguiente línea
                        }
                        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[i], pdf.getY(), lines.get(l));
                    }
                    pdf.setY(originalY); // Restauramos la altura para no desacomodar las demás columnas

                } else {
                    // Imprimir texto de una sola línea normal
                    pdf.text(PdfStyle.TXT_FONT, fontSize, cx[i], startY, val);
                }
            }

            pdf.down(rowHeight);
        }
        //pdf.line();
    }

    protected boolean mostrarFechaEnPie() {
        return true;
    }

    /** Pie de todas las páginas con emisor y numeración. */
    private void renderFooterAllPages(PdfDocument pdf, ReporteRequest request) throws Exception {
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String usuario = request.get("emisor", String.class);
        if (usuario == null || usuario.isBlank()) usuario = "SISTEMA";
        int total = pdf.getDocument().getNumberOfPages();
        for (int i = 0; i < total; i++) {
            var page = pdf.getDocument().getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(
                    pdf.getDocument(), page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                float m = pdf.getMargin(), right = pdf.getPageW() - m, y = 33f;
                cs.setStrokingColor(PdfStyle.HAIR);
                cs.setLineWidth(0.6f);
                cs.moveTo(m, y);
                cs.lineTo(right, y);
                cs.stroke();
                pie(cs, PdfStyle.TXT_BOLD_FONT, 7.5f, m, y - 15f,
                        "VetPRO - Dogtor Cat", PdfStyle.ACCENT_DARK);
                String centro = "Emitido por " + usuario.trim();
                if (mostrarFechaEnPie()) centro += " - " + fecha;
                float wm = PdfStyle.TXT_FONT.getStringWidth(centro) / 1000f * 7.5f;
                pie(cs, PdfStyle.TXT_FONT, 7.5f, (pdf.getPageW() - wm) / 2f,
                        y - 15f, centro, PdfStyle.MUTED);
                String pag = "Página " + (i + 1) + " de " + total;
                float wp = PdfStyle.TXT_FONT.getStringWidth(pag) / 1000f * 7.5f;
                pie(cs, PdfStyle.TXT_FONT, 7.5f, right - wp, y - 15f, pag, PdfStyle.MUTED);
            }
        }
    }

    private static void pie(PDPageContentStream cs,
            org.apache.pdfbox.pdmodel.font.PDFont font, float size,
            float x, float y, String content, java.awt.Color color) throws java.io.IOException {
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(content);
        cs.endText();
    }

    protected abstract String titulo();

    protected abstract void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception;
}
