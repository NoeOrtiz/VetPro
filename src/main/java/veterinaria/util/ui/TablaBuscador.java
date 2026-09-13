package veterinaria.util.ui; 

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import veterinaria.vista.table.ResaltarCoincidenciasRenderer;

public class TablaBuscador {

    public static void aplicarFiltroYResaltado(JTable table, JTextField txtBusqueda, List<Integer> columnasObjetivo) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        ResaltarCoincidenciasRenderer renderer = new ResaltarCoincidenciasRenderer(columnasObjetivo);

        // Aplica el renderizador a columnas que no sean de tipo Boolean (CheckBoxes)
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnClass(i) != Boolean.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
        }

        txtBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            private void actualizar() {
                String query = txtBusqueda.getText();
                renderer.setQuery(query);

                if (query.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    int[] cols = columnasObjetivo.stream().mapToInt(Integer::intValue).toArray();
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, cols));

                    if (sorter.getViewRowCount() == 0) {
                        JOptionPane.showMessageDialog(null, "No hay resultados para la búsqueda", "Búsqueda", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                table.repaint();
            }

            @Override public void insertUpdate(DocumentEvent e) { actualizar(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizar(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizar(); }
        });
    }
}