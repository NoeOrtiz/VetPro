package veterinaria.reportes.impl;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;

public class ProductoListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.PRODUCTO_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE PRODUCTOS";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay productos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        // 🌟 Recuperamos el filtro de rubro enviado desde la vista (ej: "TODOS" o el nombre de un rubro)
        String rubroFiltro = request.get("rubroFiltro", String.class);
        if (rubroFiltro == null || rubroFiltro.trim().isEmpty()) {
            rubroFiltro = "TODOS";
        }

        int colCodigo = 2, colNombre = 3, colRubro = 4, colDesc = 5;
        int colCosto = 6, colVenta = 7, colUM = 8, colStock = 9;

        // Recorremos la tabla y filtramos las filas según corresponda
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < m.getRowCount(); r++) {
            String rubroFila = safe(m.getValueAt(r, colRubro)).trim();

            // Si se seleccionó "TODOS" o coincide exactamente con el rubro de la fila, lo agregamos
            if (rubroFiltro.equalsIgnoreCase("TODOS") || rubroFila.equalsIgnoreCase(rubroFiltro.trim())) {
                rows.add(new String[]{
                    safe(m.getValueAt(r, colCodigo)),
                    safe(m.getValueAt(r, colNombre)),
                    rubroFila,
                    safe(m.getValueAt(r, colDesc)),
                    safe(m.getValueAt(r, colCosto)),
                    safe(m.getValueAt(r, colVenta)),
                    safe(m.getValueAt(r, colUM)),
                    safe(m.getValueAt(r, colStock))
                });
            }
        }

        // Validación por si el rubro seleccionado no tiene productos asociados en la tabla
        if (rows.isEmpty()) {
            throw new Exception("No hay productos registrados para el rubro: " + rubroFiltro);
        }

        pdf.down(8f);
        // Mostramos en el PDF el total y el filtro aplicado
        pdf.text(veterinaria.reportes.pdf.PdfStyle.TXT_FONT, 12, pdf.getMargin(), pdf.getY(),
                "Filtro aplicado: " + rubroFiltro.toUpperCase() + " | Total de productos: " + rows.size());
        pdf.down(14f);

        String[] headers = {"CÓD.", "NOMBRE", "RUBRO", "DESCRIPCIÓN", "PR. COSTO", "PR. VENTA", "U.M.", "STOCK"};
        float[] widths = {50f, 110f, 75f, 215f, 85f, 85f, 65f, 45f};

        // Indicamos que la columna 3 (Descripción) es la única que necesita salto de línea automático
        int[] wrapColumns = {3};

        // Llamamos al motor genérico centralizado con la lista ya filtrada
        renderTable(pdf, headers, widths, rows, wrapColumns, 7, 11f);
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
