package veterinaria.reportes.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class MascotaListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.MASCOTA_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE MASCOTAS";
    }

    @Override
    protected boolean isLandscape() {
        return true; // A4 horizontal
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay mascotas para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de mascotas listadas: " + m.getRowCount());
        pdf.down(16f);

        int colId = 1;
        int colNombre = 2;
        int colEspecie = 6;
        int colRaza = 4;
        int colSexo = 3;
        int colEdad = 5;
        int colDueno = 7;
        int colCastrado = 8;
        int colTam = 10;
        int colPeso = 11;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[]{
            40f, // ID
            85f, // Nombre
            70f, // Especie
            90f, // Raza
            55f, // Sexo
            60f, // Edad
            135f, // Dueño
            60f, // Castrado
            65f, // Tamaño
            55f // Peso
        };

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "ID");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "NOMBRE");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "ESPECIE");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "RAZA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "SEXO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "EDAD");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "DUEÑO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[7], pdf.getY(), "CASTR.");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[8], pdf.getY(), "TAMAÑO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[9], pdf.getY(), "PESO");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String nombre = safe(m.getValueAt(r, colNombre));
            String especie = safe(m.getValueAt(r, colEspecie));
            String raza = safe(m.getValueAt(r, colRaza));
            String sexo = safe(m.getValueAt(r, colSexo));
            String edad = safe(m.getValueAt(r, colEdad));
            String dueno = safe(m.getValueAt(r, colDueno));
            String castrado = safe(m.getValueAt(r, colCastrado));
            String tam = safe(m.getValueAt(r, colTam));
            String peso = safe(m.getValueAt(r, colPeso));

            java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, id, w[0] - 2);
            java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, nombre, w[1] - 2);
            java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, especie, w[2] - 2);
            java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, raza, w[3] - 2);
            java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, sexo, w[4] - 2);
            java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, edad, w[5] - 2);
            java.util.List<String> l6 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, dueno, w[6] - 2);
            java.util.List<String> l7 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, castrado, w[7] - 2);
            java.util.List<String> l8 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, tam, w[8] - 2);
            java.util.List<String> l9 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, peso, w[9] - 2);

            int maxLines = max(l0.size(), l1.size(), l2.size(), l3.size(), l4.size(), l5.size(), l6.size(), l7.size(), l8.size(), l9.size());
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
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[9], yy, pick(l9, i));
            }

            pdf.down(rowH + 2f);
        }
    }

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

                // Línea separadora
                cs.moveTo(margin, y);
                cs.lineTo(pageW - margin, y);
                cs.stroke();

                float footerY = y - 15f;

                // Número de página
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

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String pick(java.util.List<String> lines, int idx) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        if (idx < 0 || idx >= lines.size()) {
            return "";
        }
        return lines.get(idx) != null ? lines.get(idx) : "";
    }

    private int max(int... v) {
        int m = 0;
        if (v == null) {
            return 0;
        }
        for (int n : v) {
            if (n > m) {
                m = n;
            }
        }
        return m;
    }
}
