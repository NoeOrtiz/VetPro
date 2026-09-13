package veterinaria.reportes.impl;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.persistencia.ReciboDAO;
import veterinaria.reportes.core.ReportePaths;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class ReciboVentaPdfReporte extends AbstractPdfReporte {

    private static final DecimalFormat MONEDA = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));
    private static final SimpleDateFormat FECHA_HORA = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TS_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final float MARGIN = 50f;
    private static final float BOX_H = 56f;
    private static final int MAX_PRODUCTO_LEN = 38;
    private static final int MAX_CODIGO_LEN = 10;

    private final ReciboDAO reciboDAO = new ReciboDAO();

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.RECIBO_VENTA;
    }

    @Override
    protected String titulo() {
        return "COMPROBANTE DE VENTA";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Long idRecibo = resolveIdRecibo(request);
        if (idRecibo == null || idRecibo <= 0) {
            throw new Exception("No se proporcionó un idRecibo válido.");
        }
    }

    @Override
    protected File buildOutputFile(ReporteRequest request) {
        Long idRecibo = resolveIdRecibo(request);
        if (idRecibo == null || idRecibo <= 0) {
            return ReportePaths.enDescargas("RECIBO_VENTA_" + LocalDateTime.now().format(TS_LOCAL) + ".pdf");
        }
        String nroVisual = "RC-" + String.format("%06d", idRecibo);
        return ReportePaths.enDescargas("Recibo_Venta_" + nroVisual + ".pdf");
    }

    @Override
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        Recibo recibo = getRecibo(request);
        String nroVisual = nroVisual(recibo);

        float pageW = pdf.getPageW();
        float y = pdf.getY();

        float logoWidth = 140;
        float logoHeight = 140;
        float logoX = (pageW - logoWidth) / 2;

        renderLogo(pdf, logoX, y - 90, logoWidth, logoHeight);

        String tituloReporte = titulo();
        float tituloWidth = pdf.textWidth(PDType1Font.HELVETICA_BOLD, 13, tituloReporte);
        pdf.text(PDType1Font.HELVETICA_BOLD, 13, (pageW - tituloWidth) / 2, y - 128, tituloReporte);

        float nroWidth = pdf.textWidth(PDType1Font.HELVETICA_BOLD, 16, nroVisual);
        pdf.text(PDType1Font.HELVETICA_BOLD, 16, (pageW - nroWidth) / 2, y - 148, nroVisual);

        pdf.setY(y - 175);
        pdf.line();
        pdf.down(10f);
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Recibo recibo = getRecibo(request);
        float pageW = pdf.getPageW();
        float y = pdf.getY();

        String nroVisual = nroVisual(recibo);
        String fecha = recibo.getFecha() != null ? FECHA_HORA.format(recibo.getFecha()) : "";
        String clienteTxt = clienteTexto(recibo.getCliente());

        float boxW = pageW - 2 * MARGIN;
        pdf.rect(MARGIN, y - BOX_H, boxW, BOX_H);
        pdf.text(PdfStyle.TXT_FONT, 10, MARGIN + 10f, y - 18f, "Recibo: " + nroVisual);
        pdf.text(PdfStyle.TXT_FONT, 10, MARGIN + 10f, y - 32f, "Fecha: " + fecha);
        pdf.text(PdfStyle.TXT_FONT, 10, MARGIN + 260f, y - 18f, "Cliente: " + clienteTxt);
        pdf.setY(y - BOX_H - 16f);

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, MARGIN, pdf.getY(), "DETALLE DE PRODUCTOS");
        pdf.down(16f);

        float xCod = MARGIN;
        float xProd = MARGIN + 40f;
        float xCant = MARGIN + 280f;
        float xUnit = MARGIN + 320f;
        float xIva = MARGIN + 370f;
        float xTot = MARGIN + 440f;

        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xCod, pdf.getY(), "COD");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xProd, pdf.getY(), "PRODUCTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xCant, pdf.getY(), "CANT");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xUnit, pdf.getY(), "P.BRUTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xIva, pdf.getY(), "IVA");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xTot, pdf.getY(), "TOTAL");
        pdf.down(10f);
        pdf.line();

        BigDecimal totalCalculado = BigDecimal.ZERO;
        List<ReciboProductos> productos = recibo.getProductos();
        if (productos != null) {
            for (ReciboProductos rp : productos) {
                String cod = rp.getProducto() != null ? safe(rp.getProducto().getCodigo()) : "";
                String nom = rp.getProducto() != null
                        ? safe(rp.getProducto().getNombre() != null ? rp.getProducto().getNombre() : rp.getProducto().getDescripcion())
                        : "";

                BigDecimal cant = bd(rp.getCantidad());
                BigDecimal unit = bd(rp.getPrecioUnitario());
                BigDecimal iva = bd(rp.getIvaUnitario());
                BigDecimal ganancia = bd(rp.getGananciaUnitario());
                BigDecimal precioBruto = unit.add(ganancia);
                BigDecimal precioTotal = unit.add(iva).add(ganancia);
                BigDecimal totalLinea = precioTotal.multiply(cant);
                totalCalculado = totalCalculado.add(totalLinea);

                pdf.text(PdfStyle.TXT_FONT, 9, xCod, pdf.getY(), recortar(cod, MAX_CODIGO_LEN));
                pdf.text(PdfStyle.TXT_FONT, 9, xProd, pdf.getY(), recortar(nom, MAX_PRODUCTO_LEN));
                pdf.text(PdfStyle.TXT_FONT, 9, xCant, pdf.getY(), MONEDA.format(cant));
                pdf.text(PdfStyle.TXT_FONT, 9, xUnit, pdf.getY(), MONEDA.format(precioBruto));
                pdf.text(PdfStyle.TXT_FONT, 9, xIva, pdf.getY(), MONEDA.format(iva));
                pdf.text(PdfStyle.TXT_FONT, 9, xTot, pdf.getY(), MONEDA.format(totalLinea));
                pdf.down(12f);

                if (pdf.getY() < 140f) {
                    pdf.text(PdfStyle.TXT_FONT, 9, MARGIN, pdf.getY(),
                            "(...) Continuación no soportada en 1 página. Reduzca items o solicite multipágina.");
                    pdf.down(14f);
                    break;
                }
            }
        }

        pdf.down(6f);
        pdf.line();
        pdf.down(8f);

        List<ReciboMetodoPago> metodosPago = recibo.getMetodosPago();
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, MARGIN, pdf.getY(), "MÉTODOS DE PAGO");
        pdf.down(16f);

        if (metodosPago != null) {
            for (ReciboMetodoPago mp : metodosPago) {
                String metodo = mp.getMetodoPago() != null ? safe(mp.getMetodoPago().getNombre()) : "N/D";
                BigDecimal monto = bd(mp.getMonto());
                pdf.text(PdfStyle.TXT_FONT, 10, MARGIN, pdf.getY(), "• " + metodo);
                pdf.textRight(PdfStyle.TXT_FONT, 10, pageW - MARGIN, MONEDA.format(monto));
                pdf.down(14f);
            }
        }

        pdf.down(2f);
        pdf.line();
        pdf.down(16f);

        BigDecimal totalRecibo = recibo.getTotalRecibo() != null ? recibo.getTotalRecibo() : totalCalculado;
        pdf.text(PDType1Font.HELVETICA_BOLD, 14, MARGIN, pdf.getY(), "TOTAL A PAGAR");
        pdf.textRight(PDType1Font.HELVETICA_BOLD, 14, pageW - MARGIN, MONEDA.format(totalRecibo));
        pdf.down(22f);

        pdf.down(40f);

        String linea = "______________________";
        float anchoLinea = pdf.textWidth(PDType1Font.HELVETICA, 10, linea);
        pdf.text(PDType1Font.HELVETICA, 10, (pdf.getPageW() - anchoLinea) / 2, pdf.getY(), linea);
        pdf.down(15f);

        String firma = "Dra. Carolina Paola Boede";
        float anchoFirma = pdf.textWidth(PDType1Font.HELVETICA_BOLD, 10, firma);
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, (pdf.getPageW() - anchoFirma) / 2, pdf.getY(), firma);
        pdf.down(12f);

        String cargo = "Médica Veterinaria";
        float anchoCargo = pdf.textWidth(PDType1Font.HELVETICA, 9, cargo);
        pdf.text(PDType1Font.HELVETICA, 9, (pdf.getPageW() - anchoCargo) / 2, pdf.getY(), cargo);
        pdf.down(25f);

        String mensaje = "Gracias por confiar en Dogtor Cat";
        float anchoMensaje = pdf.textWidth(PDType1Font.HELVETICA, 9, mensaje);
        pdf.text(PDType1Font.HELVETICA, 9, (pdf.getPageW() - anchoMensaje) / 2, pdf.getY(), mensaje);
    }

    private Recibo getRecibo(ReporteRequest request) throws Exception {
        Recibo recibo = request.get("recibo", Recibo.class);
        if (recibo != null) {
            return recibo;
        }
        Long idRecibo = resolveIdRecibo(request);
        if (idRecibo == null) {
            throw new Exception("No se encontró el idRecibo para generar el comprobante.");
        }
        recibo = reciboDAO.obtenerReciboConDetalles(idRecibo);
        if (recibo == null) {
            throw new Exception("No se encontró el recibo id=" + idRecibo);
        }
        request.put("recibo", recibo);
        return recibo;
    }

    private Long resolveIdRecibo(ReporteRequest request) {
        Object value = request.raw().get("idRecibo");
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String && !((String) value).isBlank()) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        Recibo recibo = request.get("recibo", Recibo.class);
        // Retorna de manera segura validando que recibo no sea nulo
        return (recibo != null && recibo.getIdRecibo() != null) ? recibo.getIdRecibo().longValue() : null;
    }

    private static String nroVisual(Recibo recibo) {
        Object id = recibo != null ? recibo.getIdRecibo() : null;
        long idLong = 0L;
        if (id instanceof Long) {
            idLong = (Long) id;
        } else if (id instanceof Integer) {
            idLong = ((Integer) id).longValue();
        }
        return "RC-" + String.format("%06d", idLong);
    }

    // 🌟 MÉTODO CORREGIDO: Acceso seguro a getPersona() respetando el principio de encapsulamiento
    private static String clienteTexto(Cliente cli) {
        if (cli == null || cli.getPersona() == null) {
            return "";
        }
        String nombre = cli.getPersona().getNombre();
        String apellido = cli.getPersona().getApellido();
        return (safe(nombre) + " " + safe(apellido)).trim();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private static BigDecimal bd(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String recortar(String value, int max) {
        String txt = safe(value);
        if (txt.length() <= max) {
            return txt;
        }
        return txt.substring(0, Math.max(0, max - 1)) + "…";
    }
}
