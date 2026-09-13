package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class StockProductoRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.STOCK_PRODUCTO_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE STOCK DE PRODUCTO";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Object idProducto = request.raw().get("idProducto");
        if (idProducto == null) {
            throw new Exception("Producto no proporcionado.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        float x = pdf.getMargin();
        String emisor = safe(request.get("emisor", String.class));

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "ID Producto: " + safe(request.raw().get("idProducto")));
        pdf.down(16f);

        linea(pdf, x, "Código", request.get("codigo", String.class));
        linea(pdf, x, "Producto", request.get("producto", String.class));
        linea(pdf, x, "Categoría", request.get("categoria", String.class));
        linea(pdf, x, "Proveedor", request.get("proveedor", String.class));
        linea(pdf, x, "Precio compra", request.get("precioCompra", String.class));
        linea(pdf, x, "Precio venta", request.get("precioVenta", String.class));
        linea(pdf, x, "Stock actual", request.get("stockActual", String.class));
        linea(pdf, x, "Stock mínimo", request.get("stockMinimo", String.class));
        linea(pdf, x, "Estado", request.get("estado", String.class));
        linea(pdf, x, "Valor en stock (costo)", request.get("valorEnStock", String.class));

        pdf.line();

        if (!emisor.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + emisor);
            pdf.down(12f);
        }
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
        pdf.down(12f);
    }

    private void linea(PdfDocument pdf, float x, String label, String value) throws Exception {
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), label + ": " + safe(value));
        pdf.down(14f);
    }

    private String safe(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s == null ? "" : s;
    }
}
