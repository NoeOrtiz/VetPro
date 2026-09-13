package veterinaria.util.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public final class TableViewportHelper {

    private static final String KEY_WRAPPER = "adaptive.form.wrapper";
    private static final String KEY_INSTALLED = "adaptive.table.installed";
    private static final int MIN_TABLE_HEIGHT = 140;
    private static final int SINGLE_TABLE_MARGIN = 130;
    private static final int MULTI_TABLE_MARGIN = 180;
    private static final int GAP_BETWEEN_TABLES = 16;

    private TableViewportHelper() {
    }

    public static JComponent wrapForm(Component component) {
        if (!(component instanceof JComponent)) {
            throw new IllegalArgumentException("El formulario debe ser un JComponent");
        }

        JComponent form = (JComponent) component;
        Object existing = form.getClientProperty(KEY_WRAPPER);
        if (existing instanceof JComponent) {
            return (JComponent) existing;
        }

        JScrollPane wrapper = new JScrollPane(form);
        wrapper.setBorder(null);
        wrapper.getVerticalScrollBar().setUnitIncrement(18);
        wrapper.getHorizontalScrollBar().setUnitIncrement(18);
        wrapper.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        form.putClientProperty(KEY_WRAPPER, wrapper);

        installAdaptiveSizing(form, wrapper);
        return wrapper;
    }

    private static void installAdaptiveSizing(JComponent form, JScrollPane wrapper) {
        Runnable updater = () -> updateTableViewports(form, wrapper);

        wrapper.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(updater);
            }
        });

        form.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(updater);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(updater);
            }
        });

        installTableListeners(form, updater);
        SwingUtilities.invokeLater(updater);
    }

    private static void installTableListeners(Component root, Runnable updater) {
        for (JTable table : findTables(root)) {
            if (Boolean.TRUE.equals(table.getClientProperty(KEY_INSTALLED))) {
                continue;
            }

            table.putClientProperty(KEY_INSTALLED, Boolean.TRUE);
            table.setFillsViewportHeight(true);
            bindModelListener(table, updater);

            table.addPropertyChangeListener("model", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    bindModelListener(table, updater);
                    SwingUtilities.invokeLater(updater);
                }
            });
        }
    }

    private static void bindModelListener(JTable table, Runnable updater) {
        TableModel model = table.getModel();
        if (model == null) {
            return;
        }

        String listenerKey = TableViewportHelper.class.getName() + ".listener";
        Object previous = table.getClientProperty(listenerKey);
        if (previous instanceof TableModelListener) {
            model.removeTableModelListener((TableModelListener) previous);
        }

        TableModelListener listener = (TableModelEvent e) -> SwingUtilities.invokeLater(updater);
        model.addTableModelListener(listener);
        table.putClientProperty(listenerKey, listener);
    }

    private static void updateTableViewports(JComponent form, JScrollPane wrapper) {
        List<JScrollPane> tableScrolls = findTableScrollPanes(form);
        if (tableScrolls.isEmpty()) {
            return;
        }

        int viewportHeight = wrapper.getViewport().getExtentSize().height;
        if (viewportHeight <= 0) {
            viewportHeight = wrapper.getHeight();
        }
        if (viewportHeight <= 0) {
            return;
        }

        int tableCount = tableScrolls.size();
        int usableHeight;
        if (tableCount == 1) {
            usableHeight = Math.max(MIN_TABLE_HEIGHT, viewportHeight - SINGLE_TABLE_MARGIN);
        } else {
            usableHeight = Math.max(MIN_TABLE_HEIGHT,
                    viewportHeight - MULTI_TABLE_MARGIN - ((tableCount - 1) * GAP_BETWEEN_TABLES));
        }

        int perTableBudget = Math.max(MIN_TABLE_HEIGHT, usableHeight / tableCount);

        for (JScrollPane scrollPane : tableScrolls) {
            JTable table = extractTable(scrollPane);
            if (table == null) {
                continue;
            }

            int targetHeight = computeTargetHeight(table, tableCount == 1 ? usableHeight : perTableBudget);

            Dimension preferred = scrollPane.getPreferredSize();
            int preferredWidth = preferred != null && preferred.width > 0 ? preferred.width : scrollPane.getWidth();
            if (preferredWidth <= 0) {
                preferredWidth = 200;
            }

            Dimension newSize = new Dimension(preferredWidth, targetHeight);
            if (!newSize.equals(preferred)) {
                scrollPane.setPreferredSize(newSize);
            }

            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            if (verticalBar != null) {
                verticalBar.setUnitIncrement(Math.max(16, table.getRowHeight()));
            }
        }

        form.revalidate();
        form.repaint();
    }

    private static int computeTargetHeight(JTable table, int maxAllowedHeight) {
        int rowHeight = table.getRowHeight() > 0 ? table.getRowHeight() : 22;
        int rows = Math.max(1, table.getModel() != null ? table.getModel().getRowCount() : table.getRowCount());
        int headerHeight = table.getTableHeader() != null
                ? table.getTableHeader().getPreferredSize().height
                : 24;
        int borderExtra = 8;

        int contentHeight = headerHeight + (rows * rowHeight) + borderExtra;
        int target = Math.min(contentHeight, maxAllowedHeight);
        return Math.max(MIN_TABLE_HEIGHT, target);
    }

    private static List<JScrollPane> findTableScrollPanes(Component root) {
        List<JScrollPane> list = new ArrayList<>();
        collectTableScrollPanes(root, list);
        return list;
    }

    private static void collectTableScrollPanes(Component component, List<JScrollPane> list) {
        if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            if (extractTable(scrollPane) != null) {
                list.add(scrollPane);
            }
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectTableScrollPanes(child, list);
            }
        }
    }

    private static List<JTable> findTables(Component root) {
        List<JTable> list = new ArrayList<>();
        collectTables(root, list);
        return list;
    }

    private static void collectTables(Component component, List<JTable> list) {
        if (component instanceof JTable) {
            list.add((JTable) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectTables(child, list);
            }
        }
    }

    private static JTable extractTable(JScrollPane scrollPane) {
        if (scrollPane == null || scrollPane.getViewport() == null) {
            return null;
        }
        Component view = scrollPane.getViewport().getView();
        return (view instanceof JTable) ? (JTable) view : null;
    }
}
