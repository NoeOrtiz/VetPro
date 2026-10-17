package veterinaria.vista.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/** Degradado continuo a lo largo de toda la fila seleccionada, compatible con FlatLaf. */
public class TableGradientCell extends DefaultTableCellRenderer {
    private int offsetX;
    private int tableWidth;
    private boolean selected;

    public TableGradientCell() {
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        Rectangle rect = table.getCellRect(row, column, true);
        offsetX = -rect.x;
        tableWidth = table.getWidth();
        selected = isSelected;
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (selected) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color start = UIManager.getColor("Table.selectionBackground");
            if (start == null) start = getBackground();
            Color end = new Color(
                    Math.min(255, start.getRed() + 28),
                    Math.min(255, start.getGreen() + 28),
                    Math.min(255, start.getBlue() + 28));
            g2.setPaint(new GradientPaint(offsetX, 0, start,
                    offsetX + Math.max(1, tableWidth), 0, end));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
