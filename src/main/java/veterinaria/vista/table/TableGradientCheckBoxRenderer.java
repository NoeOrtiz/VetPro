package veterinaria.vista.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/**
 * Mantiene el checkbox FlatLaf y extiende el degradado de selección
 * hasta la primera columna, con las mismas coordenadas que TableGradientCell.
 */
public final class TableGradientCheckBoxRenderer extends JPanel implements TableCellRenderer {

    private final JCheckBox check = new JCheckBox();
    private boolean highlighted;
    private int offsetX;
    private int tableWidth;
    private Color start;
    private Color end;
    private Color background;

    public TableGradientCheckBoxRenderer() {
        super(new BorderLayout());
        setOpaque(false);
        check.setOpaque(false);
        check.setHorizontalAlignment(JCheckBox.CENTER);
        check.setFocusable(false);
        add(check, BorderLayout.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        highlighted = Boolean.TRUE.equals(value);
        check.setSelected(highlighted);
        check.setEnabled(table.isEnabled());
        Rectangle cell = table.getCellRect(row, column, true);
        offsetX = -cell.x;
        tableWidth = Math.max(1, table.getWidth());
        background = TableRowAppearance.fondo(table, row);
        start = TableRowAppearance.inicio(table);
        end = TableRowAppearance.fin(table);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        TableRowAppearance.pintar(g, getWidth(), getHeight(),
                highlighted, offsetX, tableWidth, start, end,
                background == null ? getBackground() : background);
        super.paintComponent(g);
    }
}
