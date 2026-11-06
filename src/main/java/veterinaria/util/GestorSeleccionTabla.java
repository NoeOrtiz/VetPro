package veterinaria.util;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

/**
 * Operaciones de seleccion compartidas entre tablas paginadas y no paginadas.
 * No instala listeners globales ni modifica renderizadores o datos persistidos.
 */
public final class GestorSeleccionTabla {
    private GestorSeleccionTabla() {}

    /** Limpia el checkbox de seleccion, cuando existe, y la seleccion de JTable. */
    public static void limpiar(JTable tabla) {
        if (tabla == null || tabla.getModel() == null) return;
        if (tabla.isEditing() && tabla.getCellEditor() != null) {
            tabla.getCellEditor().stopCellEditing();
        }
        TableModel modelo = tabla.getModel();
        int columna = columnaSeleccion(modelo);
        if (columna >= 0) {
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (Boolean.TRUE.equals(modelo.getValueAt(fila, columna))) {
                    modelo.setValueAt(Boolean.FALSE, fila, columna);
                }
            }
        }
        tabla.clearSelection();
        tabla.repaint();
    }

    /** Busca solo una columna explicita de seleccion para no borrar otros booleanos. */
    private static int columnaSeleccion(TableModel modelo) {
        for (int col = 0; col < modelo.getColumnCount(); col++) {
            String nombre = modelo.getColumnName(col);
            if (nombre != null && ("Seleccionar".equalsIgnoreCase(nombre.trim())
                    || "Seleccion".equalsIgnoreCase(nombre.trim())
                    || "Selección".equalsIgnoreCase(nombre.trim()))) {
                return col;
            }
        }
        return -1;
    }
}
