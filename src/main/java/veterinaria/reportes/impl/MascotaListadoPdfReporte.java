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
        TableModel modelo = tabla.getModel();
        String filtro = request.get("estadoFiltro", String.class);
        if (filtro == null || filtro.isBlank()) filtro = "TODOS";
        filtro = filtro.toUpperCase();

        int[] columnas = {2, 6, 4, 3, 5, 7, 12};
        String[] titulos = {"NOMBRE", "ESPECIE", "RAZA", "SEXO", "EDAD", "DUEÑO", "ESTADO"};
        float[] proporciones = {0.15f, 0.11f, 0.16f, 0.09f, 0.13f, 0.25f, 0.11f};
        float disponible = pdf.getPageW() - 2 * pdf.getMargin();
        float[] anchos = new float[proporciones.length];
        float[] posiciones = new float[proporciones.length];
        float cursor = pdf.getMargin();
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = disponible * proporciones[i];
            posiciones[i] = cursor;
            cursor += anchos[i];
        }

        int total = 0, activos = 0, inactivos = 0;
        for (int r = 0; r < modelo.getRowCount(); r++) {
            String estado = String.valueOf(modelo.getValueAt(r, 12));
            if (!"TODOS".equals(filtro) && !filtro.equalsIgnoreCase(estado)) continue;
            total++;
            if ("ACTIVO".equalsIgnoreCase(estado)) activos++;
            else inactivos++;
        }
        int totalGeneral = modelo.getRowCount();
        int activosGeneral = 0;
        for (int r = 0; r < modelo.getRowCount(); r++)
            if ("ACTIVO".equalsIgnoreCase(String.valueOf(modelo.getValueAt(r, 12)))) activosGeneral++;
        // Separar el resumen de la línea del título, como en Clientes.
        String resumen = "Total listados: " + total + "    |    Activos: " + activos
                + "    |    Inactivos: " + inactivos;
        if (!"TODOS".equals(filtro)) {
            resumen += "    |    Filtro: " + filtro;
        }
        pdf.down(10f);
        pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), resumen);
        pdf.down(22f);
        dibujarEncabezadoTabla(pdf, titulos, posiciones);

        for (int r = 0; r < modelo.getRowCount(); r++) {
            String estado = String.valueOf(modelo.getValueAt(r, 12));
            if (!"TODOS".equals(filtro) && !filtro.equalsIgnoreCase(estado)) continue;
            java.util.List<java.util.List<String>> celdas = new java.util.ArrayList<>();
            int lineas = 1;
            for (int i = 0; i < columnas.length; i++) {
                Object valor = modelo.getValueAt(r, columnas[i]);
                java.util.List<String> texto = pdf.wrapText(PdfStyle.TXT_FONT, 9,
                        valor == null ? "" : valor.toString(), anchos[i] - 10f);
                celdas.add(texto);
                lineas = Math.max(lineas, texto.size());
            }
            float altura = lineas * 13f + 9f;
            int paginaAnterior = pdf.getCurrentPage();
            pdf.ensureSpace(altura + 12f);
            if (pdf.getCurrentPage() != paginaAnterior) {
                renderHeader(pdf, request);
                pdf.down(22f);
                dibujarEncabezadoTabla(pdf, titulos, posiciones);
            }
            float y = pdf.getY();
            for (int i = 0; i < columnas.length; i++) {
                java.util.List<String> texto = celdas.get(i);
                for (int j = 0; j < texto.size(); j++)
                    pdf.text(PdfStyle.TXT_FONT, 9, posiciones[i] + 3f, y - j * 13f, texto.get(j));
            }
            pdf.down(altura);
        }
    }

    private void dibujarEncabezadoTabla(PdfDocument pdf, String[] titulos, float[] posiciones)
            throws Exception {
        pdf.ensureSpace(30f);
        for (int i = 0; i < titulos.length; i++)
            pdf.text(PDType1Font.HELVETICA_BOLD, 9, posiciones[i] + 3f, pdf.getY(), titulos[i]);
        pdf.down(22f);
        pdf.line();
        pdf.down(10f);
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
