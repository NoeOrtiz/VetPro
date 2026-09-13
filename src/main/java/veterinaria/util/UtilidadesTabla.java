package veterinaria.util;

import java.awt.FontMetrics;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public class UtilidadesTabla {
    public static void ajustarAnchoColumnas(JTable table) {
        JTableHeader header = table.getTableHeader();
        FontMetrics fm = header.getFontMetrics(header.getFont());

        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            String headerText = table.getColumnName(i);
            int ancho = fm.stringWidth(headerText) + 10; // Espacio adicional para estética
            column.setPreferredWidth(ancho);
        }
    }
}
