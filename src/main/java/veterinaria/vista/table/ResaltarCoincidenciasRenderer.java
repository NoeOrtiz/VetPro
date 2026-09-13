package veterinaria.vista.table;

import java.awt.Component;
import java.util.List;
import javax.swing.JTable;

public class ResaltarCoincidenciasRenderer extends TableGradientCell {

    private String query = "";
    private List<Integer> columnasObjetivo;

    public ResaltarCoincidenciasRenderer(List<Integer> columnasObjetivo) {
        this.columnasObjetivo = columnasObjetivo;
    }

    public void setQuery(String query) {
        this.query = (query != null) ? query.trim() : "";
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value != null && !query.isEmpty() && columnasObjetivo.contains(column)) {
            String cellValue = value.toString();
            String lowerCellValue = cellValue.toLowerCase();
            String lowerQuery = query.toLowerCase();

            if (lowerCellValue.startsWith(lowerQuery)) {
                String highlightedText = "<html><span style='color:#FFA500; font-weight:bold;'>"
                        + cellValue.substring(0, query.length()) + "</span>" 
                        + cellValue.substring(query.length()) + "</html>";
                setText(highlightedText);
            } else {
                setText(cellValue);
            }
        } else {
            setText(value != null ? value.toString() : "");
        }

        return c;
    }
}