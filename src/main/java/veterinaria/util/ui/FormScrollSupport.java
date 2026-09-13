package veterinaria.util.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public final class FormScrollSupport {

    private static final String KEY_WHEEL_INSTALLED = "form.scroll.wheel.installed";
    private static final String KEY_MODEL_PROPERTY_BOUND = "form.scroll.model.property.bound";
    private static final String KEY_MODEL_REF = "form.scroll.model.ref";
    private static final String KEY_MODEL_LISTENER = "form.scroll.model.listener";

    private FormScrollSupport() {
    }

    public static JScrollPane wrapForm(Component form) {
        ViewportFillPanel content = new ViewportFillPanel();
        content.setOpaque(false);
        content.add(form, BorderLayout.CENTER);

        JScrollPane outer = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outer.setBorder(new EmptyBorder(0, 0, 0, 0));
        outer.setWheelScrollingEnabled(true);
        outer.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        outer.getVerticalScrollBar().setUnitIncrement(24);
        outer.getVerticalScrollBar().setBlockIncrement(120);
        outer.getHorizontalScrollBar().setUnitIncrement(24);

        prepareComponentTree(form);
        prepareScrollPane(outer);
        SwingUtilities.invokeLater(() -> {
            prepareComponentTree(form);
            content.revalidate();
            content.repaint();
        });
        return outer;
    }

    public static void prepareComponentTree(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof JScrollPane) {
            prepareScrollPane((JScrollPane) component);
        }
        if (component instanceof JTable) {
            prepareTable((JTable) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                prepareComponentTree(child);
            }
        }
    }

    private static void prepareScrollPane(JScrollPane scrollPane) {
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setMinimumSize(new Dimension(0, 0));
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(120);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
            scrollPane.getHorizontalScrollBar().setBlockIncrement(120);
        }
    }

    private static void prepareTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setMinimumSize(new Dimension(0, 0));
        installWheelRedirect(table);
        bindTableModel(table);
        refreshContainerLayout(table);
    }

    private static void bindTableModel(JTable table) {
        if (!(table instanceof JComponent)) {
            return;
        }

        JComponent jc = (JComponent) table;
        if (!Boolean.TRUE.equals(jc.getClientProperty(KEY_MODEL_PROPERTY_BOUND))) {
            PropertyChangeListener propertyListener = (PropertyChangeEvent evt) -> {
                if ("model".equals(evt.getPropertyName())) {
                    bindModelListener(table);
                    SwingUtilities.invokeLater(() -> refreshContainerLayout(table));
                }
            };
            table.addPropertyChangeListener("model", propertyListener);
            jc.putClientProperty(KEY_MODEL_PROPERTY_BOUND, Boolean.TRUE);
        }

        bindModelListener(table);
    }

    private static void bindModelListener(JTable table) {
        if (!(table instanceof JComponent)) {
            return;
        }

        JComponent jc = (JComponent) table;
        TableModel model = table.getModel();
        if (model == null) {
            return;
        }

        Object previousModel = jc.getClientProperty(KEY_MODEL_REF);
        Object previousListener = jc.getClientProperty(KEY_MODEL_LISTENER);
        if (previousModel instanceof TableModel && previousListener instanceof TableModelListener) {
            ((TableModel) previousModel).removeTableModelListener((TableModelListener) previousListener);
        }

        TableModelListener listener = (TableModelEvent e) -> SwingUtilities.invokeLater(() -> refreshContainerLayout(table));
        model.addTableModelListener(listener);
        jc.putClientProperty(KEY_MODEL_REF, model);
        jc.putClientProperty(KEY_MODEL_LISTENER, listener);
    }

    private static void refreshContainerLayout(JTable table) {
        JScrollPane innerScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (innerScroll != null) {
            prepareScrollPane(innerScroll);
            innerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            innerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            innerScroll.setMinimumSize(new Dimension(0, 0));
            innerScroll.revalidate();
            innerScroll.repaint();
        }

        table.revalidate();
        table.repaint();

        Component current = table;
        while (current != null) {
            current.revalidate();
            current.repaint();
            current = current.getParent();
        }
    }

    private static void installWheelRedirect(JTable table) {
        if (!(table instanceof JComponent)) {
            return;
        }
        JComponent jc = table;
        if (Boolean.TRUE.equals(jc.getClientProperty(KEY_WHEEL_INSTALLED))) {
            return;
        }
        jc.putClientProperty(KEY_WHEEL_INSTALLED, Boolean.TRUE);

        MouseWheelListener redirect = (MouseWheelEvent e) -> {
            JScrollPane scrollPane = resolveBestScrollPane(table);
            if (scrollPane == null) {
                return;
            }
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            if (bar == null || !bar.isEnabled()) {
                return;
            }
            int amount = e.getUnitsToScroll() * Math.max(12, bar.getUnitIncrement());
            int oldValue = bar.getValue();
            int newValue = oldValue + amount;
            int max = bar.getMaximum() - bar.getVisibleAmount();
            if (newValue < bar.getMinimum()) {
                newValue = bar.getMinimum();
            } else if (newValue > max) {
                newValue = max;
            }
            if (newValue != oldValue) {
                bar.setValue(newValue);
                e.consume();
            }
        };

        table.addMouseWheelListener(redirect);
        if (table.getTableHeader() != null) {
            table.getTableHeader().addMouseWheelListener(redirect);
        }
    }

    private static JScrollPane resolveBestScrollPane(JTable table) {
        JScrollPane inner = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (inner != null && isScrollable(inner)) {
            return inner;
        }

        Component current = inner != null ? inner.getParent() : table.getParent();
        while (current != null) {
            if (current instanceof JScrollPane) {
                JScrollPane candidate = (JScrollPane) current;
                if (isScrollable(candidate)) {
                    return candidate;
                }
            }
            current = current.getParent();
        }
        return inner;
    }

    private static boolean isScrollable(JScrollPane scrollPane) {
        if (scrollPane == null || scrollPane.getVerticalScrollBar() == null) {
            return false;
        }
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        return bar.isEnabled() && (bar.getMaximum() - bar.getVisibleAmount() > bar.getMinimum());
    }

    private static final class ViewportFillPanel extends JPanel implements Scrollable {

        private ViewportFillPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            Container parent = getParent();
            if (parent instanceof JViewport) {
                Dimension extent = ((JViewport) parent).getExtentSize();
                pref = new Dimension(
                        Math.max(pref.width, extent.width),
                        Math.max(pref.height, extent.height)
                );
            }
            return pref;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL
                    ? Math.max(visibleRect.height - 24, 24)
                    : Math.max(visibleRect.width - 24, 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
