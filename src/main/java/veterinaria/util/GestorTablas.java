package veterinaria.util;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Punto de entrada de las tablas VetPRO. Los indices configurados son del modelo,
 * incluso cuando una columna (por ejemplo ID) esta oculta en la vista.
 */
public final class GestorTablas {
    private GestorTablas() {}

    public static final class Configuracion {
        private final int[] busqueda;
        private final int[] resaltado;
        private final int tamanoPagina;

        public Configuracion(int[] columnasBusqueda, int[] columnasResaltado, int tamanoPagina) {
            if (columnasBusqueda == null || columnasResaltado == null || tamanoPagina < 1) {
                throw new IllegalArgumentException("Configuracion de tabla invalida");
            }
            this.busqueda = Arrays.copyOf(columnasBusqueda, columnasBusqueda.length);
            this.resaltado = Arrays.copyOf(columnasResaltado, columnasResaltado.length);
            this.tamanoPagina = tamanoPagina;
        }
    }

    /** Configura paginacion, busqueda y resaltado con columnas independientes. */
    public static PaginacionTablas paginar(JTable tabla, JTextField buscador, Configuracion config) {
        return new PaginacionTablas(tabla, buscador, config.tamanoPagina,
                config.busqueda, config.resaltado);
    }

    /** Seleccion unica: un clic en una fila sincroniza checkbox y JTable. */
    public static void instalarSeleccion(JTable tabla, Runnable cambioSeleccion) {
        final String key = "vetpro.gestorTablas.seleccion";
        if (Boolean.TRUE.equals(tabla.getClientProperty(key))) return;
        tabla.putClientProperty(key, Boolean.TRUE);
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int vista = tabla.rowAtPoint(e.getPoint());
                if (vista < 0) {
                    limpiarSeleccion(tabla);
                    if (cambioSeleccion != null) cambioSeleccion.run();
                    return;
                }
                int modeloFila = tabla.convertRowIndexToModel(vista);
                int modeloColumna = tabla.columnAtPoint(e.getPoint()) < 0 ? -1
                        : tabla.convertColumnIndexToModel(tabla.columnAtPoint(e.getPoint()));
                int check = columnaSeleccion(tabla);
                if (check >= 0 && modeloColumna != check) {
                    tabla.getModel().setValueAt(Boolean.TRUE, modeloFila, check);
                    tabla.setRowSelectionInterval(vista, vista);
                }
                // El editor procesa el clic en el checkbox. Esperar su resultado.
                SwingUtilities.invokeLater(() -> {
                    int filaVista = tabla.convertRowIndexToView(modeloFila);
                    if (check >= 0 && !Boolean.TRUE.equals(tabla.getModel().getValueAt(modeloFila, check))) {
                        tabla.clearSelection();
                    } else if (filaVista >= 0) {
                        tabla.setRowSelectionInterval(filaVista, filaVista);
                    }
                    tabla.repaint();
                    if (cambioSeleccion != null) cambioSeleccion.run();
                });
            }
        });
        tabla.getModel().addTableModelListener(e -> SwingUtilities.invokeLater(() -> {
            tabla.repaint();
            if (cambioSeleccion != null) cambioSeleccion.run();
        }));
    }

    public static int columnaSeleccion(JTable tabla) {
        for (int i = 0; i < tabla.getModel().getColumnCount(); i++) {
            String nombre = tabla.getModel().getColumnName(i);
            if (nombre != null && ("Seleccionar".equalsIgnoreCase(nombre.trim())
                    || "Seleccion".equalsIgnoreCase(nombre.trim())
                    || "Selección".equalsIgnoreCase(nombre.trim()))) return i;
        }
        return -1;
    }

    public static boolean haySeleccion(JTable tabla) {
        int columna = columnaSeleccion(tabla);
        if (columna >= 0) {
            for (int i = 0; i < tabla.getModel().getRowCount(); i++) {
                if (Boolean.TRUE.equals(tabla.getModel().getValueAt(i, columna))
                        && tabla.convertRowIndexToView(i) >= 0) return true;
            }
            return false;
        }
        return tabla.getSelectedRow() >= 0;
    }

    public static void limpiarSeleccion(JTable tabla) {
        GestorSeleccionTabla.limpiar(tabla);
    }
}
