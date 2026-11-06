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
        background = table.getBackground();
        Color panel = UIManager.getColor("Panel.background");
        boolean dark = panel != null
                && (panel.getRed() * 299 + panel.getGreen() * 587 + panel.getBlue() * 114) / 1000 < 128;
        start = dark ? new Color(169, 65, 44) : new Color(255, 210, 157);
        end = dark ? new Color(44, 105, 177) : new Color(174, 207, 245);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (highlighted) {
            g2.setPaint(new GradientPaint(offsetX, 0, start,
                    offsetX + tableWidth, 0, end));
        } else {
            g2.setColor(background == null ? getBackground() : background);
        }
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
