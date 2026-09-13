package veterinaria.reportes.impl;

import javax.swing.JOptionPane;
import javax.swing.JTable;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;

public final class HistorialTurnosImpresion {

    private HistorialTurnosImpresion() {
    }

    public static void imprimir(JTable tabla, String filtrosResumen) {
        if (tabla == null || tabla.getModel() == null || tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null,
                    "No hay datos para imprimir.",
                    "Imprimir Historial de Turnos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object[] options = {"Compacta", "Detallada", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Seleccioná el formato de impresión:",
                "Imprimir Historial de Turnos",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        // Si eligen Cancelar o cierran la ventana, frenamos acá
        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        // Evaluamos el tipo de reporte según el botón que tocaron
        ReporteTipo tipo = (choice == 0)
                ? ReporteTipo.HISTORIAL_TURNOS_LISTADO
                : ReporteTipo.HISTORIAL_TURNOS;

        // Armamos los parámetros del reporte tal como los tenías
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tabla)
                .put("filtros", filtrosResumen != null ? filtrosResumen : "");

        // 🚀 MANDAMOS AL EJECUTOR: Limpio de "new", usa Singleton y mete la barra animada
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(tabla, tipo, req);
    }
}
