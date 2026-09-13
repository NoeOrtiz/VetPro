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

    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        float pageW = pdf.getPageW();
        float y = pdf.getY();

        // 🌟 Logo redimensionado para ganar espacio vertical
        float logoW = 100f;
        float logoH = 80f;
        float logoX = (pageW - logoW) / 2;

        renderLogo(pdf, logoX, y - logoH, logoW, logoH);

        pdf.setY(y - logoH - 15f);

        String tituloReporte = titulo();
        float tWidth = pdf.textWidth(PdfStyle.H2_FONT, 14, tituloReporte);
        pdf.text(PdfStyle.H2_FONT, 14, (pageW - tWidth) / 2, pdf.getY(), tituloReporte);
        pdf.down(16f);

        pdf.line();
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

    // 🌟 Genera el pie de página en CADA hoja del documento
    private void renderFooterAllPages(PdfDocument pdf, ReporteRequest request) throws Exception {
        String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        String emisor = request.get("emisor", String.class);
        emisor = (emisor == null || emisor.isBlank()) ? "SISTEMA" : emisor.trim();

        int totalPaginas = pdf.getDocument().getNumberOfPages();

        for (int i = 0; i < totalPaginas; i++) {
            var page = pdf.getDocument().getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(pdf.getDocument(), page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                float margin = pdf.getMargin();
                float pageW = pdf.getPageW();
                float y = 35f;

                cs.moveTo(margin, y);
                cs.lineTo(pageW - margin, y);
                cs.stroke();

                float footerY = y - 15f;

                // Página X de Y
                String pagStr = "Pág. " + (i + 1) + " de " + totalPaginas;
                cs.beginText();
                cs.setFont(PdfStyle.TXT_FONT, 8);
                cs.newLineAtOffset(margin, footerY);
                cs.showText(pagStr);
                cs.endText();

                // Usuario
                String usrStr = "Usuario: " + emisor;
                float usrW = PdfStyle.TXT_FONT.getStringWidth(usrStr) / 1000f * 8;
                cs.beginText();
                cs.setFont(PdfStyle.TXT_FONT, 8);
                cs.newLineAtOffset((pageW - usrW) / 2f, footerY);
                cs.showText(usrStr);
                cs.endText();

                // Fecha y Hora
                float dateW = PdfStyle.TXT_FONT.getStringWidth(fechaHora) / 1000f * 8;
                cs.beginText();
                cs.setFont(PdfStyle.TXT_FONT, 8);
                cs.newLineAtOffset(pageW - margin - dateW, footerY);
                cs.showText(fechaHora);
                cs.endText();
            }
        }
    }

    protected abstract String titulo();

    protected abstract void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception;
}
