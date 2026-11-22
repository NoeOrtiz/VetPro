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
    private final int columnaCheckbox;
    private boolean highlighted;
    private int offsetX;
    private int tableWidth;
    private Color start;
    private Color end;

    public TableGradientCell() {
        this(0);
    }

    public TableGradientCell(int columnaCheckbox) {
        this.columnaCheckbox = columnaCheckbox;
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        // El renderer no debe heredar el fondo de selección de JTable
        // cuando la casilla de esta fila está desmarcada.
        boolean tieneCheckbox = columnaCheckbox >= 0
                && columnaCheckbox < table.getColumnCount()
                && table.getColumnClass(table.convertColumnIndexToModel(columnaCheckbox)) == Boolean.class;
        highlighted = tieneCheckbox
                ? Boolean.TRUE.equals(table.getValueAt(row, columnaCheckbox))
                : isSelected;
        super.getTableCellRendererComponent(table, value,
                highlighted, hasFocus && highlighted, row, column);
        setBackground(TableRowAppearance.fondo(table, row));
        Rectangle bounds = table.getCellRect(row, column, true);
        offsetX = -bounds.x;
        tableWidth = Math.max(1, table.getWidth());
        start = TableRowAppearance.inicio(table);
        end = TableRowAppearance.fin(table);
        boolean dark = TableRowAppearance.temaOscuro();
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
            TableRowAppearance.pintar(g, getWidth(), getHeight(),
                    true, offsetX, tableWidth, start, end, getBackground());
        } else {
            TableRowAppearance.pintar(g, getWidth(), getHeight(),
                    false, offsetX, tableWidth, start, end, getBackground());
        }
        super.paintComponent(g);
    }
}
