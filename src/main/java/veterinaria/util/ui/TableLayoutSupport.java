package veterinaria.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Aplica únicamente la cuadrícula visual común.
 * Respeta tamaños, renderizadores, editores y ajustes propios de cada formulario.
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
            JTable table = (JTable) component;
            table.setShowHorizontalLines(false);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new java.awt.Dimension(0, 0));
            table.repaint();
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                normalizeRecursively(child);
            }
        }
    }
}
