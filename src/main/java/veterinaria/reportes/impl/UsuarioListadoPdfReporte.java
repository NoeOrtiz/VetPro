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
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();
        
  
        float x = pdf.getMargin();
      
        
        
        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de usuarios listados: " + m.getRowCount());
        pdf.down(16f);

        pdf.line();
        
        int colNombre = 1;
        int colApellido = 2;
        int colDni = 3;
        int colTelefono = 4;
        int colRol = 6;
        int colUsuario = 7;
        int colEmail = 8;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[] {
            95f,  // Nombre
            95f,  // Apellido
            55f,  // DNI
            70f,  // Teléfono
            80f,  // Rol
            85f,  // Usuario
            130f  // E-mail
        };

        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "NOMBRE");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "APELLIDO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "DNI");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "TEL.");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "ROL");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "USUARIO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "E-MAIL");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            String nombre = safe(m.getValueAt(r, colNombre));
            String apellido = safe(m.getValueAt(r, colApellido));
            String dni = safe(m.getValueAt(r, colDni));
            String tel = safe(m.getValueAt(r, colTelefono));
            String rol = safe(m.getValueAt(r, colRol));
            String usuario = safe(m.getValueAt(r, colUsuario));
            String email = safe(m.getValueAt(r, colEmail));

            java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, nombre, w[0] - 2);
            java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, apellido, w[1] - 2);
            java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, dni, w[2] - 2);
            java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, tel, w[3] - 2);
            java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, rol, w[4] - 2);
            java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, usuario, w[5] - 2);
            java.util.List<String> l6 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, email, w[6] - 2);

            int maxLines = max(l0.size(), l1.size(), l2.size(), l3.size(), l4.size(), l5.size(), l6.size());
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
