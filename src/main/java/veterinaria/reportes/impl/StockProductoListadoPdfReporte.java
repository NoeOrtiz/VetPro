package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class StockProductoListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.STOCK_PRODUCTO_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTADO DE STOCK DE PRODUCTOS";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay productos en stock para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();
        String emisor = safe(request.get("emisor", String.class));
        String filtros = safe(request.get("filtros", String.class));

        if (!emisor.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Emitido por: " + emisor);
            pdf.down(12f);
        }

        if (!filtros.isBlank()) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 10, pdf.getMargin(), pdf.getY(), "Filtros aplicados:");
            pdf.down(12f);
            for (String line : filtros.split("\\r?\\n")) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                List<String> wrapped = pdf.wrapText(PdfStyle.TXT_FONT, 9, line.trim(), pdf.getPageW() - (pdf.getMargin() * 2));
                for (String wl : wrapped) {
                    pdf.ensureSpace(12f);
                    pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), wl);
                    pdf.down(11f);
                }
            }
            pdf.down(4f);
        }

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Productos listados: " + safe(request.get("totalProductos", String.class))
                + " | Unidades en stock: " + safe(request.get("totalUnidades", String.class))
                + " | Valor total inventario: $" + safe(request.get("valorInventario", String.class)));
        pdf.down(12f);
        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Stock mínimo: " + safe(request.get("cantidadStockMinimo", String.class))
                + " | Sin stock: " + safe(request.get("cantidadSinStock", String.class)));
        pdf.down(16f);

        int colId = 0;
        int colCodigo = 1;
        int colProducto = 2;
        int colCategoria = 3;
        int colProveedor = 4;
        int colPrecioCompra = 5;
        int colPrecioVenta = 6;
        int colStockActual = 7;
        int colStockMinimo = 8;
        int colEstado = 9;
        int colValor = 10;

        final int fontSize = 8;
        final float lineH = 10f;
        final float[] w = new float[] {
                28f,
                52f,
                115f,
                72f,
                115f,
                58f,
                58f,
                42f,
                44f,
                58f,
                70f
        };

        float x = pdf.getMargin();
        final float[] cx = new float[w.length];
        cx[0] = x;
        for (int i = 1; i < w.length; i++) {
            cx[i] = cx[i - 1] + w[i - 1];
        }

        pdf.ensureSpace(60f);
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "ID");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "CÓDIGO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "PRODUCTO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "CATEGORÍA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "PROVEEDOR");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "P. COMPRA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "P. VENTA");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[7], pdf.getY(), "STOCK");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[8], pdf.getY(), "MÍNIMO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[9], pdf.getY(), "ESTADO");
        pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[10], pdf.getY(), "VALOR");
        pdf.down(10f);
        pdf.line();

        for (int r = 0; r < m.getRowCount(); r++) {
            String id = safe(m.getValueAt(r, colId));
            String codigo = safe(m.getValueAt(r, colCodigo));
            String producto = safe(m.getValueAt(r, colProducto));
            String categoria = safe(m.getValueAt(r, colCategoria));
            String proveedor = safe(m.getValueAt(r, colProveedor));
            String precioCompra = safe(m.getValueAt(r, colPrecioCompra));
            String precioVenta = safe(m.getValueAt(r, colPrecioVenta));
            String stockActual = safe(m.getValueAt(r, colStockActual));
            String stockMinimo = safe(m.getValueAt(r, colStockMinimo));
            String estado = safe(m.getValueAt(r, colEstado));
            String valor = safe(m.getValueAt(r, colValor));

            List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, id, w[0] - 2);
            List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, codigo, w[1] - 2);
            List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, producto, w[2] - 2);
            List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, categoria, w[3] - 2);
            List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, proveedor, w[4] - 2);
            List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, precioCompra, w[5] - 2);
            List<String> l6 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, precioVenta, w[6] - 2);
            List<String> l7 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, stockActual, w[7] - 2);
            List<String> l8 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, stockMinimo, w[8] - 2);
            List<String> l9 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, estado, w[9] - 2);
            List<String> l10 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, valor, w[10] - 2);

            int maxLines = max(l0.size(), l1.size(), l2.size(), l3.size(), l4.size(), l5.size(), l6.size(), l7.size(), l8.size(), l9.size(), l10.size());
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
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[7], yy, pick(l7, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[8], yy, pick(l8, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[9], yy, pick(l9, i));
                pdf.text(PdfStyle.TXT_FONT, fontSize, cx[10], yy, pick(l10, i));
            }
            pdf.down(rowH + 2f);
        }

        pdf.down(8f);
        pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(),
                "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String pick(List<String> lines, int idx) {
        if (lines == null || idx < 0 || idx >= lines.size()) {
            return "";
        }
        String value = lines.get(idx);
        return value == null ? "" : value;
    }

    private int max(int... v) {
        int m = 0;
        if (v == null) {
            return 0;
        }
        for (int n : v) {
            if (n > m) {
                m = n;
            }
        }
        return m;
    }
}
