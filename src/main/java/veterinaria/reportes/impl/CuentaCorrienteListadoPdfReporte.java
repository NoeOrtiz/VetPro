package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class CuentaCorrienteListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CC_LISTA_CUENTAS;
    }

    
    @Override
    protected boolean isLandscape() {
        return true;
    }
@Override
    protected String titulo() {
        return "LISTADO DE CUENTAS CORRIENTES";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay cuentas corrientes para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {

        String usuario = request.get("usuario", String.class);
        String rol = request.get("rol", String.class);

        if (usuario != null && !usuario.trim().isEmpty()) {
            pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Usuario: " + usuario + ((rol != null && !rol.trim().isEmpty()) ? " (" + rol + ")" : ""));
            pdf.down(14f);
        }

        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de cuentas: " + m.getRowCount());
        pdf.down(16f);

        int colCuenta = 1;
        int colCliente = 2;
        int colSaldo = 3;
        int colUltConsumo = 4;
        int colUltPago = 5;
        int colEstado = 6;

        float x = pdf.getMargin();

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), "CUENTA");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 70, pdf.getY(), "CLIENTE");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 290, pdf.getY(), "SALDO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 350, pdf.getY(), "ULT. CONSUMO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 450, pdf.getY(), "ULT. PAGO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 530, pdf.getY(), "ESTADO");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            pdf.ensureSpace(20f);

            String cuenta = safe(m.getValueAt(r, colCuenta));
            String cliente = safe(m.getValueAt(r, colCliente));
            String saldo = safe(m.getValueAt(r, colSaldo));
            String ultConsumo = safe(m.getValueAt(r, colUltConsumo));
            String ultPago = safe(m.getValueAt(r, colUltPago));
            String estado = safe(m.getValueAt(r, colEstado));

            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), recortar(cuenta, 10));
            pdf.text(PdfStyle.TXT_FONT, 9, x + 70, pdf.getY(), recortar(cliente, 38));
            pdf.textRight(PdfStyle.TXT_FONT, 9, x + 340, saldo);
            pdf.text(PdfStyle.TXT_FONT, 9, x + 350, pdf.getY(), recortar(ultConsumo, 14));
            pdf.text(PdfStyle.TXT_FONT, 9, x + 450, pdf.getY(), recortar(ultPago, 14));
            pdf.text(PdfStyle.TXT_FONT, 9, x + 530, pdf.getY(), recortar(estado, 12));

            pdf.down(12f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String recortar(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : (s.substring(0, Math.max(0, max - 3)) + "...");
    }
}
