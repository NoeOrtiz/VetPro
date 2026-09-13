package veterinaria.reportes.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.Usuario;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class CajaMovimientoPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DTF_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CAJA_MOVIMIENTO;
    }

    @Override
    protected String titulo() {
        return "MOVIMIENTO DE CAJA - DETALLE";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        CajaMovimiento mov = request.get("movimiento", CajaMovimiento.class);
        if (mov == null) {
            throw new Exception("Movimiento no proporcionado.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        CajaMovimiento mov = request.get("movimiento", CajaMovimiento.class);

        float x = pdf.getMargin();

        String id = mov.getIdMovimiento() != null ? mov.getIdMovimiento().toString() : "-";
        String tipo = mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : "-";
        String fecha = formatFecha(mov.getFecha());

        BigDecimal monto = mov.getMonto() != null ? mov.getMonto() : BigDecimal.ZERO;

        Usuario u = mov.getUsuario();
        String usuario = nombreUsuario(u);

        MetodoPago mp = mov.getMetodoPago();
        String metodo = mp != null ? safe(mp.getNombre()) : "N/A";

        Recibo r = mov.getRecibo();
        String recibo = (r != null && r.getIdRecibo() != null) ? ("#" + r.getIdRecibo()) : "N/A";

        String desc = safe(mov.getDescripcion());

        pdf.ensureSpace(220f);

        pdf.text(PDType1Font.HELVETICA_BOLD, 12, x, pdf.getY(), "ID Movimiento: " + id);
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 11, x, pdf.getY(), "Fecha: " + fecha);
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 11, x, pdf.getY(), "Tipo: " + tipo);
        pdf.down(12f);

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Monto: $ " + formatMoney(monto));
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 11, x, pdf.getY(), "Usuario: " + usuario);
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 11, x, pdf.getY(), "Método de Pago: " + metodo);
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 11, x, pdf.getY(), "Recibo/Fuente: " + recibo);
        pdf.down(18f);

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Descripción:");
        pdf.down(12f);

        List<String> lines = pdf.wrapText(PdfStyle.TXT_FONT, 10, desc, pdf.getPageW() - 2 * pdf.getMargin());
        for (String line : lines) {
            pdf.ensureSpace(18f);
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), line);
            pdf.down(12f);
        }

        pdf.down(10f);
        pdf.line();

        pdf.text(PDType1Font.HELVETICA_OBLIQUE, 9, x, pdf.getY(), "Generado: " + LocalDateTime.now().format(DTF_TS));
    }

    private static String formatFecha(java.util.Date fecha) {
        if (fecha == null) {
            return "-";
        }
        try {
            if (fecha instanceof java.sql.Date) {
                return ((java.sql.Date) fecha).toLocalDate().format(DTF_FECHA);
            }
            if (fecha instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) fecha).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DTF_FECHA);
            }
            return java.time.Instant.ofEpochMilli(fecha.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DTF_FECHA);
        } catch (Exception e) {
            return String.valueOf(fecha);
        }
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String nombreUsuario(Usuario u) {
        if (u == null) return "-";
        Persona p = u.getPersona();
        if (p == null) {
            return u.getNombreUsuario() != null ? u.getNombreUsuario() : "-";
        }
        String nombre = (p.getNombre() != null ? p.getNombre() : "") +
                (p.getApellido() != null ? " " + p.getApellido() : "");
        nombre = nombre.trim();
        if (nombre.isEmpty()) {
            return u.getNombreUsuario() != null ? u.getNombreUsuario() : "-";
        }
        return nombre;
    }

    private static String formatMoney(BigDecimal v) {
        try {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "AR"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(v != null ? v : BigDecimal.ZERO);
        } catch (Exception e) {
            return String.valueOf(v != null ? v : BigDecimal.ZERO);
        }
    }
}
