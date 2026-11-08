package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class UsuarioListadoPdfReporte extends AbstractPdfReporte {
    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.USUARIO_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE USUARIOS";
    }

    @Override
    protected boolean isLandscape() {
        return true; // A4 horizontal
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay usuarios para imprimir.");
        }
    }

    @Override
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        float y = pdf.getY();
        renderLogo(pdf, (pdf.getPageW() - 160f) / 2f, y - 103f, 160f, 103f);
        pdf.setY(y - 116f);
        String t = titulo();
        pdf.text(PdfStyle.H2_FONT, 14, pdf.centerX(pdf.textWidth(PdfStyle.H2_FONT, 14, t)), pdf.getY(), t);
        pdf.down(22f);
        pdf.line();
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();
        String filtro = request.get("estadoFiltro", String.class);
        if (filtro == null || filtro.isBlank()) filtro = "TODOS";
        filtro = filtro.trim().toUpperCase(java.util.Locale.ROOT);
        java.util.List<Integer> filas = new java.util.ArrayList<>();
        for (int r = 0; r < m.getRowCount(); r++) {
            String estado = safe(m.getValueAt(r, 9)).toUpperCase(java.util.Locale.ROOT);
            if ("TODOS".equals(filtro) || estado.equals(filtro)) filas.add(r);
        }
        pdf.down(6f);
        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Usuarios: " + filas.size() + "    |    Estado: " +
                ("TODOS".equals(filtro) ? "Todos" : "ACTIVO".equals(filtro) ? "Activos" : "Inactivos"));
        pdf.down(24f);
        String[] headers = {"NOMBRE", "APELLIDO", "DNI", "TELÉFONO", "ROL", "USUARIO", "E-MAIL", "ESTADO"};
        int[] indices = {1, 2, 3, 4, 6, 7, 8, 9};
        float[] pesos = {12, 12, 8, 11, 12, 12, 24, 9};
        float[] anchos = new float[pesos.length];
        float[] xs = new float[pesos.length];
        float x = pdf.getMargin();
        for (int i = 0; i < pesos.length; i++) {
            anchos[i] = pdf.getUsableWidth() * pesos[i] / 100f;
            xs[i] = x + 4f;
            x += anchos[i];
        }
        dibujarCabecera(pdf, headers, xs);
        for (int r : filas) {
            java.util.List<java.util.List<String>> celdas = new java.util.ArrayList<>();
            int lineas = 1;
            for (int c = 0; c < indices.length; c++) {
                java.util.List<String> l = pdf.wrapText(PdfStyle.TXT_FONT, 8,
                        safe(m.getValueAt(r, indices[c])), anchos[c] - 9f);
                celdas.add(l);
                lineas = Math.max(lineas, l.size());
            }
            float alto = lineas * 13f + 10f;
            if (pdf.getY() - alto < pdf.getMargin() + 18f) {
                pdf.nuevaPagina();
                dibujarCabecera(pdf, headers, xs);
            }
            float y = pdf.getY();
            for (int c = 0; c < indices.length; c++) {
                for (int j = 0; j < celdas.get(c).size(); j++) {
                    pdf.text(PdfStyle.TXT_FONT, 8, xs[c], y - j * 13f, celdas.get(c).get(j));
                }
            }
            pdf.down(alto);
        }
    }

    private void dibujarCabecera(PdfDocument pdf, String[] headers, float[] xs) throws Exception {
        pdf.ensureSpace(40f);
        for (int i = 0; i < headers.length; i++) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 8, xs[i], pdf.getY(), headers[i]);
        }
        pdf.down(16f);
        pdf.line();
        pdf.down(7f);
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
