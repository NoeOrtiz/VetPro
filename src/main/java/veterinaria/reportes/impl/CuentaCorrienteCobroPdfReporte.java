package veterinaria.reportes.impl;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.persistencia.CuentaCorrienteMovimientoDAO;
import veterinaria.reportes.core.ReportePaths;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class CuentaCorrienteCobroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TS_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final CuentaCorrienteMovimientoDAO movimientoDAO = new CuentaCorrienteMovimientoDAO();

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CC_COBRO;
    }

    @Override
    protected String titulo() {
        return "COMPROBANTE DE COBRO - CUENTA CORRIENTE";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Integer idMovimiento = resolveIdMovimiento(request);
        if (idMovimiento == null || idMovimiento <= 0) {
            throw new Exception("No se proporcionó un idMovimiento válido.");
        }
    }

    @Override
    protected File buildOutputFile(ReporteRequest request) {
        Integer idMovimiento = resolveIdMovimiento(request);
        if (idMovimiento == null || idMovimiento <= 0) {
            return ReportePaths.enDescargas("CC_COBRO_" + LocalDateTime.now().format(TS_LOCAL) + ".pdf");
        }
        return ReportePaths.enDescargas("Comprobante_Cobro_CC_" + idMovimiento + ".pdf");
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        CuentaCorrienteMovimiento mov = getMovimiento(request);
        CuentaCorriente cc = mov.getCuentaCorriente();
        Cliente cli = cc != null ? cc.getCliente() : null;

        float x = pdf.getMargin();
        BigDecimal monto = mov.getMonto() != null ? mov.getMonto() : BigDecimal.ZERO;

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Movimiento #: " + safe(mov.getIdMovimiento()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha: " + (mov.getFechaMovimiento() != null ? mov.getFechaMovimiento().format(FECHA) : ""));
        pdf.down(14f);

        if (cli != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(),
                    "Cliente: " + nombreCliente(cli) + " (ID " + safe(cli.getIdCliente()) + ")");
            pdf.down(14f);
        }

        if (cc != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Cuenta Corriente ID: " + safe(cc.getIdCuentaCorriente()));
            pdf.down(14f);
        }

        pdf.down(8f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Detalle");
        pdf.down(16f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Concepto: " + safe(mov.getDescripcion()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Tipo: " + safe(mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : null));
        pdf.down(14f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Monto: " + formatMoney(monto));
        pdf.down(16f);

        if (mov.getSaldoResultante() != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Saldo resultante: " + formatMoney(mov.getSaldoResultante()));
            pdf.down(14f);
        } else if (cc != null && cc.getSaldoActual() != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Saldo actual: " + formatMoney(cc.getSaldoActual()));
            pdf.down(14f);
        }

        pdf.down(10f);
        pdf.line();
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Comprobante generado por el sistema.");
    }

    private CuentaCorrienteMovimiento getMovimiento(ReporteRequest request) throws Exception {
        CuentaCorrienteMovimiento mov = request.get("movimiento", CuentaCorrienteMovimiento.class);
        if (mov != null) {
            return mov;
        }

        Integer idMovimiento = resolveIdMovimiento(request);
        if (idMovimiento == null) {
            throw new Exception("No se encontró el idMovimiento para generar el comprobante.");
        }

        mov = movimientoDAO.buscarPorId(idMovimiento);
        if (mov == null) {
            throw new Exception("No se encontró el movimiento id=" + idMovimiento);
        }
        request.put("movimiento", mov);
        return mov;
    }

    private Integer resolveIdMovimiento(ReporteRequest request) {
        Object value = request.raw().get("idMovimiento");
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof String && !((String) value).isBlank()) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        CuentaCorrienteMovimiento mov = request.get("movimiento", CuentaCorrienteMovimiento.class);
        return mov != null ? mov.getIdMovimiento() : null;
    }

    private static String nombreCliente(Cliente cli) {
        if (cli == null || cli.getPersona() == null) {
            return "";
        }
        return (safe(cli.getPersona().getNombre()) + " " + safe(cli.getPersona().getApellido())).trim();
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String formatMoney(BigDecimal value) {
        try {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "AR"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(value != null ? value : BigDecimal.ZERO);
        } catch (Exception e) {
            return String.valueOf(value != null ? value : BigDecimal.ZERO);
        }
    }
}
