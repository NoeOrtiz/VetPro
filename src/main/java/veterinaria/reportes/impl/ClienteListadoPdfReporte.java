package veterinaria.reportes.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;

public class ClienteListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CLIENTE_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE CLIENTES";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay clientes para imprimir.");
        }
    }

    @Override
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        renderHeaderConLogo(pdf, 90f, 90f);
    }

    @Override
    protected boolean mostrarFechaEnPie() {
        return false;
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        TableModel modelo = request.get("tabla", JTable.class).getModel();
        String estadoFiltro = safe(request.get("estadoFiltro", String.class)).toUpperCase(Locale.ROOT);
        if (estadoFiltro.isBlank()) estadoFiltro = "TODOS";

        int[] columnas = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        List<String[]> filas = new ArrayList<>();
        int activos = 0;
        int inactivos = 0;
        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            String estado = safe(modelo.getValueAt(fila, 10)).toUpperCase(Locale.ROOT);
            if (!"TODOS".equals(estadoFiltro) && !estadoFiltro.equals(estado)) continue;

            String[] datos = new String[columnas.length];
            for (int i = 0; i < columnas.length; i++) {
                datos[i] = safe(modelo.getValueAt(fila, columnas[i]));
            }
            filas.add(datos);
            if ("ACTIVO".equals(estado)) activos++;
            if ("INACTIVO".equals(estado)) inactivos++;
        }

        UsuarioPdfPresentacion.resumen(pdf,
                new String[]{"Clientes", String.valueOf(filas.size())},
                new String[]{"Activos", String.valueOf(activos)},
                new String[]{"Inactivos", String.valueOf(inactivos)},
                new String[]{"Filtro", estadoFiltro});
        UsuarioPdfPresentacion.tabla(pdf,
                new String[]{"Nombre", "Apellido", "DNI", "Dirección", "E-mail", "Tel.",
                        "Razón social", "CUIT", "Estado", "F. alta"},
                new float[]{8f, 8f, 7f, 15f, 19f, 9f, 12f, 9f, 7f, 8f}, filas);
    }

    private static String safe(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }
}
