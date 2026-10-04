package veterinaria.util.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import veterinaria.vista.table.TableColumnAdjuster;

public final class TableLayoutSupport {

    private TableLayoutSupport() {
    }

    public static void prepare(Component root) {
        if (root == null) {
            return;
        }
        normalizeRecursively(root);
        if (root instanceof JComponent) {
            ((JComponent) root).revalidate();
        }
        root.repaint();
        SwingUtilities.invokeLater(() -> {
            normalizeRecursively(root);
            if (root instanceof JComponent) {
                ((JComponent) root).revalidate();
            }
            root.repaint();
        });
    }

    private static void normalizeRecursively(Component component) {
        if (component instanceof JTable) {
            normalizeTable((JTable) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                normalizeRecursively(child);
            }
        }
    }

    private static void normalizeTable(JTable table) {
        table.setPreferredSize(null);
        table.setMinimumSize(new Dimension(0, 0));
        table.setMaximumSize(null);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Un único criterio de ancho para todas las tablas de VetPRO.
        // Se reaplica después de que Swing termina de montar/cargar el modelo.
        SwingUtilities.invokeLater(() -> {
            if (table.getColumnCount() > 0) {
                TableColumnAdjuster.ajustarAnchoColumnas(table);
                table.revalidate();
                table.repaint();
            }
        });

        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (scrollPane != null) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setWheelScrollingEnabled(true);

            JViewport viewport = scrollPane.getViewport();
            if (viewport != null && viewport.getView() != table) {
                viewport.setView(table);
            }

            scrollPane.revalidate();
            scrollPane.repaint();
        }
    }
}
