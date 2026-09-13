package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class ProveedorListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.PROVEEDOR_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE PROVEEDORES";
    }

    @Override
    protected boolean isLandscape() {
        return true; // A4 horizontal
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay proveedores para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de proveedores listados: " + m.getRowCount());
        pdf.down(16f);

        int colId = 1;
        int colRazonSocial = 2;
        int colCuit = 3;
        int colContacto = 4;
        int colTelefono = 5;
        int colDireccion = 6;
        int colEmail = 7;
        int colRubro = 8;
        int colEstado = 9;

        final int fontSize = 9;
        final float lineH = 11f;

        final float[] w = new float[] {
            35f,   // ID
            120f,  // Razón Social
            65f,   // CUIT
            95f,   // Contacto
            60f,   // Teléfono
            160f,  // Dirección
            120f,  // Email
            85f,   // Rubro
            55f    // Estado
        };

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "ID");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "RAZÓN SOCIAL");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "CUIT");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "CONTACTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "TEL.");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "DIRECCIÓN");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "E-MAIL");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[7], pdf.getY(), "RUBRO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[8], pdf.getY(), "ESTADO");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String rs = safe(m.getValueAt(r, colRazonSocial));
            String cuit = safe(m.getValueAt(r, colCuit));
            String contacto = safe(m.getValueAt(r, colContacto));
            String tel = safe(m.getValueAt(r, colTelefono));
            String dir = safe(m.getValueAt(r, colDireccion));
            String email = safe(m.getValueAt(r, colEmail));
            String rubro = safe(m.getValueAt(r, colRubro));
            String estado = safe(m.getValueAt(r, colEstado));

            pdf.ensureSpace(20f);

            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[0], pdf.getY(), truncate(id, 8));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[1], pdf.getY(), truncate(rs, 26));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[2], pdf.getY(), truncate(cuit, 14));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[3], pdf.getY(), truncate(contacto, 18));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4], pdf.getY(), truncate(tel, 14));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[5], pdf.getY(), truncate(dir, 36));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[6], pdf.getY(), truncate(email, 26));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[7], pdf.getY(), truncate(rubro, 16));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[8], pdf.getY(), truncate(estado, 10));

            pdf.down(lineH);
        }

        pdf.line();
    }

    private String safe(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return (s == null) ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }
}
