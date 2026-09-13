package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Producto;
import veterinaria.entidad.Proveedor;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class ProductoRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.PRODUCTO_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE PRODUCTO";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Producto p = request.get("producto", Producto.class);
        if (p == null) {
            throw new Exception("Producto no proporcionado.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Producto p = request.get("producto", Producto.class);
        String emisor = request.get("emisor", String.class);

        float x = pdf.getMargin();

        // Reducimos saltos y agrupamos datos lógicos para ahorrar espacio vertical en la página
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "ID Producto: " + safe(p.getIdProducto()));
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Código: " + safe(p.getCodigo()));
        pdf.down(12f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Nombre: " + safe(p.getNombre()));
        pdf.down(12f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Rubro: " + safe(p.getRubro()));
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Descripción: " + safe(p.getDescripcion()));
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Precio: " + safe(p.getPrecio()) + "  |  IVA: " + safe(p.getIva()) + "  |  Descuento: " + safe(p.getDescuento()));
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Unidad de medida: " + safe(p.getUmedida()) + "  |  Stock: " + safe(p.getStock()));
        pdf.down(12f);

        Proveedor prov = p.getProveedor();
        String provTxt = "";
        if (prov != null) {
            String n = safe(prov.getPersona().getNombre() + " " + prov.getPersona().getApellido());
            String rs = "";
            try {
                rs = safe(prov.getRazonSocial());
            } catch (Exception ignore) {
                rs = "";
            }
            provTxt = !rs.isBlank() ? rs : n;
        }
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Proveedor: " + provTxt);
        pdf.down(12f);

        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Estado: " + safe(p.getEstado()));
        pdf.down(14f);

        pdf.line();
        pdf.down(10f);

        if (emisor != null && !emisor.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 8, x, pdf.getY(), "Emitido por: " + emisor);
            pdf.down(10f);
            pdf.text(PdfStyle.TXT_FONT, 8, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
            pdf.down(10f);
        }
    }

    private String safe(Object o) {
        if (o == null) {
            return "";
        }
        String s = String.valueOf(o);
        return (s == null) ? "" : s;
    }
}
