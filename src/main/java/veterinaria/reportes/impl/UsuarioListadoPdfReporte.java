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

/**
 * Listado A4 horizontal con encabezado de marca, resumen y filas alternadas.
 * Conserva Estado y el alcance seleccionado por quien imprime.
 */
public class UsuarioListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.USUARIO_LISTADO;
    }

    @Override
    protected String titulo() {
        return "INFORME - LISTA DE USUARIOS";
    }

    @Override
    protected boolean isLandscape() {
        return true;
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
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getModel().getRowCount() == 0)
            throw new Exception("No hay usuarios para imprimir.");
        if (tabla.getModel().getColumnCount() < 10)
            throw new Exception("El listado de usuarios no contiene todas las columnas.");
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        TableModel modelo = tabla.getModel();
        String estado = safe(request.get("estadoFiltro", String.class))
                .toUpperCase(Locale.ROOT);
        if (estado.isBlank()) estado = "TODOS";

        List<String[]> registros = new ArrayList<>();
        int[] columnas = {1, 2, 3, 4, 6, 7, 8, 9};
        for (int r = 0; r < modelo.getRowCount(); r++) {
            String valorEstado = safe(modelo.getValueAt(r, 9)).toUpperCase(Locale.ROOT);
            if (!"TODOS".equals(estado) && !estado.equals(valorEstado)) continue;
            String[] datos = new String[columnas.length];
            for (int c = 0; c < columnas.length; c++) {
                datos[c] = safe(modelo.getValueAt(r, columnas[c]));
            }
            registros.add(datos);
        }

        UsuarioPdfPresentacion.resumen(pdf,
                new String[]{"Usuarios", String.valueOf(registros.size())},
                new String[]{"Estado", "TODOS".equals(estado) ? "Todos" :
                        "ACTIVO".equals(estado) ? "Activos" : "Inactivos"});
        UsuarioPdfPresentacion.tabla(pdf,
                new String[]{"Nombre", "Apellido", "DNI", "Teléfono",
                        "Rol", "Usuario", "E-mail", "Estado"},
                new float[]{12f, 12f, 8f, 11f, 12f, 11f, 25f, 9f},
                registros);
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
