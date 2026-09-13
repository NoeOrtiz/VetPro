package veterinaria.util;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.function.Supplier;

public class ManejoTablas {

    public void asegurarSeleccionUnica(JTable table) {
        if (table == null || table.getModel() == null) {
            return;
        }

        table.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0) {  // Columna del checkbox (columna 0)
                int rowCount = table.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    if (i != e.getFirstRow() && Boolean.TRUE.equals(table.getValueAt(i, 0))) {
                        table.setValueAt(false, i, 0);  // Deselecciona cualquier otra fila
                    }
                }
            }
        });
    }

    public Integer comprobarElementoSeleccionado(JTable table) {
        if (table == null || table.getModel() == null) {
            return -1;
        }

        if (table.getColumnCount() <= 0 || table.getModel().getColumnCount() <= 0) {
            return -1;
        }

        final TableModel model = table.getModel();
        final int viewRowCount = table.getRowCount();

        for (int viewRow = 0; viewRow < viewRowCount; viewRow++) {
            int modelRow;
            try {
                modelRow = table.convertRowIndexToModel(viewRow);
            } catch (Exception ex) {
                modelRow = viewRow;
            }

            if (modelRow < 0 || modelRow >= model.getRowCount()) {
                continue;
            }

            try {
                Object val = model.getValueAt(modelRow, 0);
                if (Boolean.TRUE.equals(val)) {
                    return viewRow; // devolvemos índice en VISTA
                }
            } catch (IndexOutOfBoundsException ex) {
                continue;
            }
        }
        return -1;
    }

    public boolean isTableNotEmpty(JTable table) {
        return table != null && table.getRowCount() > 0;
    }

    public void mouseTooltipText(JTable table, List<Integer> indicesColumnasConTooltip) {
        tooltipValorEnColumnas(table, indicesColumnasConTooltip);
    }

    public static void tooltipValorEnColumnas(JTable table, List<Integer> indicesColumnasConTooltip) {
        if (table == null || indicesColumnasConTooltip == null || indicesColumnasConTooltip.isEmpty()) {
            return;
        }

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (row == -1 || column == -1) {
                    table.setToolTipText(null);
                    return;
                }

                if (indicesColumnasConTooltip.contains(column)) {
                    Object value = table.getValueAt(row, column);
                    table.setToolTipText(value != null ? value.toString() : null);
                } else {
                    table.setToolTipText(null);
                }
            }
        });
    }

    public static void aplicarTooltipsPorColumna(JTable table, Map<Integer, String> tooltipsPorColumnaModel) {
        if (table == null || tooltipsPorColumnaModel == null || tooltipsPorColumnaModel.isEmpty()) {
            return;
        }

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (row == -1 || viewCol == -1) {
                    table.setToolTipText(null);
                    return;
                }
                int modelCol = table.convertColumnIndexToModel(viewCol);
                String tip = tooltipsPorColumnaModel.get(modelCol);
                table.setToolTipText(tip);
            }
        });
    }

    public static void aplicarFiltroYResaltado(JTable table, JTextField txtBusqueda, List<Integer> columnasObjetivo) {
        if (table == null || txtBusqueda == null || columnasObjetivo == null) {
            return;
        }

        txtBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refrescarFiltro(table, txtBusqueda, columnasObjetivo);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refrescarFiltro(table, txtBusqueda, columnasObjetivo);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refrescarFiltro(table, txtBusqueda, columnasObjetivo);
            }
        });
    }

    public static void aplicarFiltroYResaltadoCombinado(JTable table, JTextField txtBusqueda, List<Integer> columnasObjetivo, Supplier<RowFilter<Object, Object>> filtroExtraSupplier) {
        if (table == null || txtBusqueda == null || columnasObjetivo == null) {
            return;
        }

        txtBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refrescarFiltroCombinado(table, txtBusqueda, columnasObjetivo, filtroExtraSupplier != null ? filtroExtraSupplier.get() : null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refrescarFiltroCombinado(table, txtBusqueda, columnasObjetivo, filtroExtraSupplier != null ? filtroExtraSupplier.get() : null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refrescarFiltroCombinado(table, txtBusqueda, columnasObjetivo, filtroExtraSupplier != null ? filtroExtraSupplier.get() : null);
            }
        });
    }

    public static void refrescarFiltroCombinado(JTable table, JTextField txtBusqueda, List<Integer> columnasObjetivo, RowFilter<Object, Object> filtroExtra) {
        if (table == null || txtBusqueda == null) {
            return;
        }
        filtrarYResaltarCombinado(table, txtBusqueda.getText(), columnasObjetivo, filtroExtra);
    }

    private static void filtrarYResaltarCombinado(JTable table, String query, List<Integer> columnasObjetivoVista, RowFilter<Object, Object> filtroExtra) {
        if (table == null || table.getModel() == null) {
            return;
        }

        TableRowSorter<TableModel> sorter;
        if (table.getRowSorter() instanceof TableRowSorter) {
            sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setModel(table.getModel());
        } else {
            sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);
        }

        String q = query != null ? query.trim() : "";

        int[] colsModel = toModelColumns(table, columnasObjetivoVista);

        RowFilter<Object, Object> filtroTexto = null;
        if (!q.isEmpty()) {
            filtroTexto = RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), colsModel);
        }

        RowFilter<Object, Object> filtroFinal;
        if (filtroTexto != null && filtroExtra != null) {
            List<RowFilter<Object, Object>> filtros = new ArrayList<>();
            filtros.add(filtroTexto);
            filtros.add(filtroExtra);
            filtroFinal = RowFilter.andFilter(filtros);
        } else if (filtroTexto != null) {
            filtroFinal = filtroTexto;
        } else {
            filtroFinal = filtroExtra; // puede ser null (sin filtros)
        }

        sorter.setRowFilter(filtroFinal);

        if (filtroTexto != null) {
            table.setDefaultRenderer(Object.class, new ResaltarCoincidenciasRenderer(q, columnasObjetivoVista));
        } else {
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
        }
    }

    public static void refrescarFiltro(JTable table, JTextField txtBusqueda, List<Integer> columnasObjetivo) {
        if (table == null || txtBusqueda == null) {
            return;
        }
        filtrarYResaltar(table, txtBusqueda.getText(), columnasObjetivo);
    }

    private static void filtrarYResaltar(JTable table, String query, List<Integer> columnasObjetivoVista) {
        if (table == null || table.getModel() == null) {
            return;
        }

        TableRowSorter<TableModel> sorter;
        if (table.getRowSorter() instanceof TableRowSorter) {
            sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setModel(table.getModel());
        } else {
            sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);
        }

        String q = query != null ? query.trim() : "";

        if (q.isEmpty()) {
            sorter.setRowFilter(null);
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
            return;
        }

        int[] colsModel = toModelColumns(table, columnasObjetivoVista);

        RowFilter<Object, Object> filtro = RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), colsModel);
        sorter.setRowFilter(filtro);

        table.setDefaultRenderer(Object.class, new ResaltarCoincidenciasRenderer(q, columnasObjetivoVista));
    }

    private static int[] toModelColumns(JTable table, List<Integer> viewCols) {
        List<Integer> result = new ArrayList<>();
        for (Integer viewCol : viewCols) {
            if (viewCol == null) {
                continue;
            }
            if (viewCol >= 0 && viewCol < table.getColumnCount()) {
                result.add(table.convertColumnIndexToModel(viewCol));
            }
        }
        return result.stream().mapToInt(i -> i).toArray();
    }

    private static class ResaltarCoincidenciasRenderer extends DefaultTableCellRenderer {

        private final String query;
        private final List<Integer> columnasObjetivoVista;

        public ResaltarCoincidenciasRenderer(String query, List<Integer> columnasObjetivoVista) {
            this.query = query;
            this.columnasObjetivoVista = columnasObjetivoVista;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (columnasObjetivoVista != null
                    && columnasObjetivoVista.contains(column)
                    && value != null
                    && query != null
                    && !query.isEmpty()) {

                String cellValue = value.toString();
                String lowerCellValue = cellValue.toLowerCase();
                String lowerQuery = query.toLowerCase();

                int idx = lowerCellValue.indexOf(lowerQuery);
                if (idx >= 0) {
                    String before = cellValue.substring(0, idx);
                    String match = cellValue.substring(idx, idx + query.length());
                    String after = cellValue.substring(idx + query.length());
                    String highlightedText = "<html>" + escapeHtml(before)
                            + "<span style='color:#FFA500; font-weight:bold;'>" + escapeHtml(match) + "</span>"
                            + escapeHtml(after) + "</html>";
                    setText(highlightedText);
                } else {
                    setText(cellValue);
                }
            } else {
                setText(value != null ? value.toString() : "");
            }

            return c;
        }

        private String escapeHtml(String s) {
            if (s == null) {
                return "";
            }
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }
// Agrega este método dentro de ManejoTablas.java

    public <T> ModeloTablaGenerico<T> configurarTablaGenerica(javax.swing.JTable tabla, String[] columnas, ModeloTablaGenerico.IColumnResolver<T> resolver) {
        ModeloTablaGenerico<T> modelo = new ModeloTablaGenerico<>(columnas, resolver);
        tabla.setModel(modelo);
        return modelo;
    }

    public void ocultarColumnaId(javax.swing.JTable tabla, int indiceColumnaId) {
        if (tabla.getColumnModel().getColumnCount() > indiceColumnaId) {
            tabla.getColumnModel().getColumn(indiceColumnaId).setMinWidth(0);
            tabla.getColumnModel().getColumn(indiceColumnaId).setMaxWidth(0);
            tabla.getColumnModel().getColumn(indiceColumnaId).setWidth(0);
        }
    }
}
