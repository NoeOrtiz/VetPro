package veterinaria.util;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Paginación y búsqueda reutilizables para JTable. Mantiene el modelo completo
 * y limita únicamente las filas visibles; no modifica los registros persistidos.
 */
public final class PaginacionTablas {
    private final JTable tabla;
    private final JTextField busqueda;
    private final int[] columnas;
    private final int tamano;
    private boolean adaptable;
    private boolean recalculoPendiente;
    private final JPanel controles = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
    private final JLabel estado = new JLabel();
    private final JButton primera = new JButton("«");
    private final JButton anterior = new JButton("‹");
    private final JButton siguiente = new JButton("›");
    private final JButton ultima = new JButton("»");
    private final JButton limpiar = new JButton("×");
    private int pagina;
    private boolean actualizando;

    public PaginacionTablas(JTable tabla, JTextField busqueda, int tamano, int... columnasModelo) {
        if (tabla == null || busqueda == null || tamano < 1) {
            throw new IllegalArgumentException("Tabla, búsqueda y tamaño válidos son obligatorios");
        }
        this.tabla = tabla;
        this.busqueda = busqueda;
        this.tamano = tamano;
        this.columnas = Arrays.copyOf(columnasModelo, columnasModelo.length);
        primera.setToolTipText("Primera página");
        anterior.setToolTipText("Página anterior");
        siguiente.setToolTipText("Página siguiente");
        ultima.setToolTipText("Última página");
        limpiar.setToolTipText("Limpiar búsqueda");
        for (JButton boton : new JButton[]{primera, anterior, siguiente, ultima, limpiar}) {
            boton.setFocusable(false);
        }
        controles.add(primera);
        controles.add(anterior);
        controles.add(estado);
        controles.add(siguiente);
        controles.add(ultima);
        primera.addActionListener(e -> irA(0));
        anterior.addActionListener(e -> irA(pagina - 1));
        siguiente.addActionListener(e -> irA(pagina + 1));
        ultima.addActionListener(e -> irA(Integer.MAX_VALUE));
        limpiar.addActionListener(e -> busqueda.setText(""));
        limpiar.setVisible(false);
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { buscar(); }
            public void removeUpdate(DocumentEvent e) { buscar(); }
            public void changedUpdate(DocumentEvent e) { buscar(); }
        });
        tabla.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
                SwingUtilities.invokeLater(() -> actualizar());
            }
        });
        actualizar();
    }

    /**
     * Activa paginación por altura visible. Es optativa para no modificar
     * el comportamiento de las pantallas que siguen usando tamaño fijo.
     */
    public void activarTamanoAdaptable() {
        if (adaptable) return;
        adaptable = true;
        javax.swing.JViewport viewport = (javax.swing.JViewport)
                SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, tabla);
        if (viewport == null) {
            throw new IllegalStateException("La tabla debe estar dentro de un JScrollPane");
        }
        viewport.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                programarRecalculo();
            }
        });
        tabla.addPropertyChangeListener("rowHeight", e -> programarRecalculo());
        programarRecalculo();
    }

    private void programarRecalculo() {
        if (recalculoPendiente) return;
        recalculoPendiente = true;
        SwingUtilities.invokeLater(() -> {
            recalculoPendiente = false;
            actualizar();
        });
    }

    private int filasPorPagina() {
        if (!adaptable) return tamano;
        javax.swing.JViewport viewport = (javax.swing.JViewport)
                SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, tabla);
        if (viewport == null || viewport.getExtentSize().height <= 0) return tamano;
        int alturaFila = Math.max(1, tabla.getRowHeight() + tabla.getRowMargin());
        return Math.max(1, viewport.getExtentSize().height / alturaFila);
    }

    public JPanel getControles() { return controles; }
    public JButton getBotonLimpiar() { return limpiar; }
    public void actualizar() {
        if (actualizando) return;
        actualizando = true;
        try {
            TableModel modelo = tabla.getModel();
            TableRowSorter<TableModel> sorter;
            if (tabla.getRowSorter() instanceof TableRowSorter) {
                @SuppressWarnings("unchecked")
                TableRowSorter<TableModel> existente = (TableRowSorter<TableModel>) tabla.getRowSorter();
                sorter = existente;
            } else {
                sorter = new TableRowSorter<>(modelo);
                tabla.setRowSorter(sorter);
            }
            String consulta = busqueda.getText().trim().toLowerCase(Locale.ROOT);
            List<Integer> coincidencias = new ArrayList<>();
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (consulta.isEmpty() || coincide(modelo, fila, consulta)) coincidencias.add(fila);
            }
            int total = coincidencias.size();
            int capacidad = filasPorPagina();
            int paginas = Math.max(1, (total + capacidad - 1) / capacidad);
            pagina = Math.max(0, Math.min(pagina, paginas - 1));
            int inicio = pagina * capacidad;
            int fin = Math.min(inicio + capacidad, total);
            java.util.Set<Integer> visibles = new java.util.HashSet<>(coincidencias.subList(inicio, fin));
            // Nunca mantener una selección de checkbox en una página oculta.
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (!visibles.contains(fila) && modelo.getColumnCount() > 0
                        && modelo.getColumnClass(0) == Boolean.class
                        && Boolean.TRUE.equals(modelo.getValueAt(fila, 0))) {
                    modelo.setValueAt(false, fila, 0);
                }
            }
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> e) {
                    return visibles.contains(e.getIdentifier());
                }
            });
            estado.setText("Página " + (pagina + 1) + " de " + paginas);
            primera.setEnabled(pagina > 0);
            anterior.setEnabled(pagina > 0);
            siguiente.setEnabled(pagina < paginas - 1);
            ultima.setEnabled(pagina < paginas - 1);
            limpiar.setVisible(!consulta.isEmpty());
            ManejoTablas.resaltarBusqueda(tabla, busqueda.getText(), indices());
        } finally {
            actualizando = false;
        }
    }

    private List<Integer> indices() {
        List<Integer> lista = new ArrayList<>();
        for (int col : columnas) lista.add(col);
        return lista;
    }

    private boolean coincide(TableModel modelo, int fila, String consulta) {
        for (int columna : columnas) {
            if (columna >= 0 && columna < modelo.getColumnCount()) {
                Object valor = modelo.getValueAt(fila, columna);
                if (valor != null && valor.toString().toLowerCase(Locale.ROOT).contains(consulta)) return true;
            }
        }
        return false;
    }

    private void buscar() {
        pagina = 0;
        SwingUtilities.invokeLater(this::actualizar);
    }

    private void irA(int destino) {
        pagina = destino;
        actualizar();
    }
}
