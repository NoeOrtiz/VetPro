package veterinaria.reportes.impl;

import java.text.SimpleDateFormat;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.CompraRecepcionItem;
import veterinaria.entidad.Producto;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class CompraRecepcionPdfReporte extends AbstractPdfReporte {

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.COMPRA_RECEPCION;
    }

    @Override
    protected String titulo() {
        return "COMPROBANTE DE RECEPCIÓN DE COMPRA";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        CompraRecepcion r = request.get("recepcion", CompraRecepcion.class);
        if (r == null) {
            throw new Exception("Recepción no proporcionada.");
        }
        if (r.getItems() == null || r.getItems().isEmpty()) {
            throw new Exception("La recepción no tiene items.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        CompraRecepcion r = request.get("recepcion", CompraRecepcion.class);

        String usuario = request.get("usuario", String.class);
        String rol = request.get("rol", String.class);

        float x = pdf.getMargin();

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Recepción ID: " + safe(r.getIdCompraRecepcion()));
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha: " + (r.getFecha() != null ? DF.format(r.getFecha()) : ""));
        pdf.down(14f);

        String nroOC = (r.getOrden() != null) ? safe(r.getOrden().getNumeroOrden()) : "";
        String prov = (r.getOrden() != null && r.getOrden().getProveedor() != null)
                ? safe(r.getOrden().getProveedor().getRazonSocial())
                : "";

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Orden de Compra: " + nroOC);
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Proveedor: " + prov);
        pdf.down(14f);

        String remito = safe(r.getNumeroRemito());
        String factura = safe(r.getNumeroFactura());
        String estado = (r.getEstado() != null) ? r.getEstado().name() : "";

        if (!remito.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "N° Remito: " + remito);
            pdf.down(14f);
        }
        if (!factura.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "N° Factura: " + factura);
            pdf.down(14f);
        }
        if (!estado.isBlank()) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "Estado: " + estado);
            pdf.down(14f);
        }

        if (r.getEstado() == veterinaria.entidad.CompraRecepcionEstado.ANULADA) {
            String mot = safe(r.getMotivoAnulacion());
            if (!mot.isBlank()) {
                pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Motivo anulación: " + mot);
                pdf.down(14f);
            }
            if (r.getFechaAnulacion() != null) {
                pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha anulación: " + DF.format(r.getFechaAnulacion()));
                pdf.down(14f);
            }

            String anulador = "";
            if (r.getUsuarioAnulacion() != null) {
                if (r.getUsuarioAnulacion().getPersona() != null) {
                    anulador = safe(r.getUsuarioAnulacion().getPersona().getNombre()) + " " + safe(r.getUsuarioAnulacion().getPersona().getApellido());
                    anulador = anulador.trim();
                }
                if (anulador.isBlank()) {
                    anulador = safe(r.getUsuarioAnulacion().getNombreUsuario());
                }
            }
            if (!anulador.isBlank()) {
                pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Anulada por: " + anulador);
                pdf.down(14f);
            }
        }

        if ((usuario != null && !usuario.isBlank()) || (rol != null && !rol.isBlank())) {
            String u = (usuario != null ? usuario : "");
            String rr = (rol != null && !rol.isBlank()) ? (" (" + rol + ")") : "";
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + u + rr);
            pdf.down(14f);
        }

        pdf.line();

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), "CATEGORÍA");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 160, pdf.getY(), "PRODUCTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 450, pdf.getY(), "CANT.");
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x + 500, pdf.getY(), "COSTO");
        pdf.down(10f);
        pdf.line();

        for (CompraRecepcionItem it : r.getItems()) {
            if (it == null) continue;

            Producto p = it.getProducto();
            String cat = (p != null && p.getRubro() != null) ? safe(p.getRubro()) : "";
            String prod = (p != null) ? safe(p.getNombre()) : "";
            String cant = (it.getCantidadRecibida() != null) ? String.valueOf(it.getCantidadRecibida()) : "";
            String costo = (it.getPrecioCostoUnitario() != null) ? it.getPrecioCostoUnitario().toString() : "";

            pdf.ensureSpace(20f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), recortar(cat, 22));
            pdf.text(PdfStyle.TXT_FONT, 9, x + 160, pdf.getY(), recortar(prod, 45));
            pdf.textRight(PdfStyle.TXT_FONT, 9, x + 480, cant);
            pdf.textRight(PdfStyle.TXT_FONT, 9, x + 560, costo);
            pdf.down(12f);
        }

        pdf.down(6f);
        pdf.line();
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String recortar(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : (s.substring(0, Math.max(0, max - 3)) + "...");
    }
}
