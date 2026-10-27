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

/**
 * Degradado de selección, inspirado en DJ-Raven.
 * No cambia fuente, ancho de columna ni altura de fila.
 */
public class TableGradientCell extends DefaultTableCellRenderer {
    private boolean highlighted;
    private int offsetX;
    private int tableWidth;
    private Color start;
    private Color end;

    public TableGradientCell() {
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        // El renderer no debe heredar el fondo de selección de JTable
        // cuando la casilla de esta fila está desmarcada.
        boolean tieneCheckbox = table.getColumnCount() > 0
                && table.getColumnClass(table.convertColumnIndexToModel(0)) == Boolean.class;
        highlighted = tieneCheckbox
                ? Boolean.TRUE.equals(table.getValueAt(row, 0))
                : isSelected;
        super.getTableCellRendererComponent(table, value,
                highlighted, hasFocus && highlighted, row, column);
        if (!highlighted) {
            setBackground(table.getBackground());
        }
        Rectangle bounds = table.getCellRect(row, column, true);
        offsetX = -bounds.x;
        tableWidth = Math.max(1, table.getWidth());
        boolean dark = UIManager.getColor("Panel.background") != null
                && luminance(UIManager.getColor("Panel.background")) < 128;
        start = dark ? new Color(169, 65, 44) : new Color(255, 210, 157);
        end = dark ? new Color(44, 105, 177) : new Color(174, 207, 245);
        if (highlighted) {
            setForeground(dark ? Color.WHITE : new Color(35, 39, 45));
        } else {
            setForeground(table.getForeground());
        }
        setFont(table.getFont());
        setOpaque(false);
        return this;
    }

    private static int luminance(Color color) {
        return (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (highlighted) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(offsetX, 0, start,
                    offsetX + tableWidth, 0, end));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        } else {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
