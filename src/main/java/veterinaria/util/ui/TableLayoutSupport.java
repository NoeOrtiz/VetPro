package veterinaria.util.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import veterinaria.vista.table.TableGradientCell;

/**
 * Solo incorpora tooltips de contenido y degradado a las celdas de texto.
 * Conserva anchos, alturas, fuentes, editores, checkboxes y scroll originales.
 */
public final class TableLayoutSupport {
    private TableLayoutSupport() {}

    public static void prepare(Component root) {
        if (root == null) return;
        normalizeRecursively(root);
        SwingUtilities.invokeLater(() -> {
            normalizeRecursively(root);
            if (root instanceof JComponent) ((JComponent) root).revalidate();
            root.repaint();
        });
    }

    private static void normalizeRecursively(Component component) {
        if (component instanceof JTable) {
            prepareTable((JTable) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                normalizeRecursively(child);
            }
        }
    }

    private static void prepareTable(JTable table) {
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));

        if (!Boolean.TRUE.equals(table.getClientProperty("vetpro.tooltip.content"))) {
            table.putClientProperty("vetpro.tooltip.content", Boolean.TRUE);
            // JTable.getToolTipText se consulta con la posición real del mouse.
            // El listener ya existente de ManejoTablas sigue funcionando.
            table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent event) {
                    int row = table.rowAtPoint(event.getPoint());
                    int col = table.columnAtPoint(event.getPoint());
                    if (row < 0 || col < 0) {
                        table.setToolTipText(null);
                        return;
                    }
                    Object value = table.getValueAt(row, col);
                    if (value != null && !(value instanceof Boolean)) {
                        table.setToolTipText(String.valueOf(value));
                    } else {
                        table.setToolTipText(null);
                    }
                }
            });
        }

        // No se reemplazan renderizadores personalizados: solamente los
        // renderizadores por defecto de texto que no poseen uno de columna.
        for (int col = 0; col < table.getColumnModel().getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            if (column.getCellRenderer() != null) continue;
            int modelColumn = column.getModelIndex();
            if (modelColumn < 0 || modelColumn >= table.getModel().getColumnCount()) continue;
            Class<?> type = table.getModel().getColumnClass(modelColumn);
            if (type == Boolean.class || type == boolean.class || Component.class.isAssignableFrom(type)) continue;
            column.setCellRenderer(new TableGradientCell());
        }
        table.repaint();
    }
}
