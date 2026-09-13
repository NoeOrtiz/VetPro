
package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class CuentaCorrienteMovimientosPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CC_MOVIMIENTOS;
    }

    @Override
    protected String titulo() {
        return "MOVIMIENTOS DE CUENTA CORRIENTE";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay movimientos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        String cliente = request.get("cliente", String.class);
        String cuenta = request.get("cuenta", String.class);
        String filtros = request.get("filtros", String.class);
        String usuario = request.get("usuario", String.class);
        String rol = request.get("rol", String.class);
        String saldo = request.get("saldo", String.class);
        String limite = request.get("limite", String.class);

        if (cliente != null && !cliente.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Cliente: " + cliente);
            pdf.down(14f);
        }
        if (cuenta != null && !cuenta.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Cuenta: " + cuenta);
            pdf.down(14f);
        }
        if (filtros != null && !filtros.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), "Filtros: " + filtros);
            pdf.down(14f);
        }
        if ((usuario != null && !usuario.isBlank()) || (rol != null && !rol.isBlank())) {
            String u = (usuario != null ? usuario : "");
            String r = (rol != null && !rol.isBlank()) ? (" (" + rol + ")") : "";
            pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), "Emitido por: " + u + r);
            pdf.down(14f);
        }

        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        int IDX_DESC = 2;
        int IDX_FECHA = 3;
        int IDX_TIPO = 4;
        int IDX_MONTO = 5;

        float x = pdf.getMargin();

        pdf.ensureSpace(80f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), "FECHA");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 95, pdf.getY(), "TIPO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 200, pdf.getY(), "MONTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 280, pdf.getY(), "DESCRIPCIÓN");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            pdf.ensureSpace(20f);

            String fecha = safe(m.getValueAt(r, IDX_FECHA));
            String tipo = safe(m.getValueAt(r, IDX_TIPO));
            String monto = safe(m.getValueAt(r, IDX_MONTO));
            String desc = safe(m.getValueAt(r, IDX_DESC));

            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), recortar(fecha, 14));
            pdf.text(PdfStyle.TXT_FONT, 9, x + 95, pdf.getY(), recortar(tipo, 14));
            pdf.textRight(PdfStyle.TXT_FONT, 9, x + 260, monto);
            pdf.text(PdfStyle.TXT_FONT, 9, x + 280, pdf.getY(), recortar(desc, 60));

            pdf.down(12f);
        }

        if ((saldo != null && !saldo.isBlank()) || (limite != null && !limite.isBlank())) {
            pdf.down(6f);
            pdf.line();
            pdf.ensureSpace(30f);
            if (saldo != null && !saldo.isBlank()) {
                pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "SALDO:");
                pdf.textRight(PdfStyle.TXT_FONT, 10, x + 260, saldo);
                pdf.down(14f);
            }
            if (limite != null && !limite.isBlank()) {
                pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "LÍMITE:");
                pdf.textRight(PdfStyle.TXT_FONT, 10, x + 260, limite);
                pdf.down(14f);
            }
        }
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String recortar(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\n", " ").trim();
        return t.length() <= max ? t : t.substring(0, Math.max(0, max - 3)) + "...";
    }
}
