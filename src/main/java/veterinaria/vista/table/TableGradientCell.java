package veterinaria.vista.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/** Texto multilínea con selección degradada; hereda la tipografía de JTable. */
public class TableGradientCell extends JTextArea implements TableCellRenderer {
    private boolean selected;
    private int offsetX;
    private int tableWidth;

    public TableGradientCell() {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        setFont(table.getFont());
        setText(value == null ? "" : String.valueOf(value));
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        selected = isSelected;
        Rectangle cell = table.getCellRect(row, column, true);
        offsetX = -cell.x;
        tableWidth = table.getWidth();
        setSize(Math.max(1, cell.width - table.getColumnModel().getColumnMargin()), Short.MAX_VALUE);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (selected) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color start = UIManager.getColor("Table.selectionBackground");
            if (start == null) start = getBackground();
            Color end = new Color(
                    Math.min(255, start.getRed() + 22),
                    Math.min(255, start.getGreen() + 22),
                    Math.min(255, start.getBlue() + 22));
            g2.setPaint(new GradientPaint(offsetX, 0, start,
                    offsetX + Math.max(1, tableWidth), 0, end));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
