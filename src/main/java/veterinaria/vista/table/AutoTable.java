package veterinaria.vista.table;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Component;
import javax.swing.SwingUtilities;

public class AutoTable extends JTable {

    private TableModelListener tableModelListener;

    public AutoTable() {
        super();
        initAutoResize();
    }

    public AutoTable(TableModel dm) {
        super(dm);
        initAutoResize();
    }

    private void initAutoResize() {
        TableModel model = getModel();
        if (model == null) {
            return;
        }

        if (tableModelListener != null) {
            model.removeTableModelListener(tableModelListener);
        }

        tableModelListener = (TableModelEvent e) -> {
            SwingUtilities.invokeLater(this::reajustarColumnasYFilasSeguro);
        };

        model.addTableModelListener(tableModelListener);
    }

    public void reajustarColumnasYFilasSeguro() {
        if (getColumnModel() == null || getRowCount() == 0) {
            return;
        }

        // 1. Ajuste de Ancho de Columnas
        for (int column = 0; column < getColumnModel().getColumnCount(); column++) {
            TableColumn tableColumn = getColumnModel().getColumn(column);

            if (column == 0) {
                tableColumn.setPreferredWidth(85); // Aumentado para que entre la palabra completa
                tableColumn.setMaxWidth(110);
                tableColumn.setMinWidth(75);
                continue;
            }

            int preferredWidth = tableColumn.getMinWidth();
            int maxWidth = tableColumn.getMaxWidth();

            try {
                TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
                if (headerRenderer == null && getTableHeader() != null) {
                    headerRenderer = getTableHeader().getDefaultRenderer();
                }
                if (headerRenderer != null) {
                    Component headerComp = headerRenderer.getTableCellRendererComponent(
                            this, tableColumn.getHeaderValue(), false, false, 0, column);
                    if (headerComp != null) {
                        preferredWidth = Math.max(preferredWidth, headerComp.getPreferredSize().width + 15);
                    }
                }

                int rowCount = Math.min(getRowCount(), 50);
                for (int row = 0; row < rowCount; row++) {
                    TableCellRenderer cellRenderer = getCellRenderer(row, column);
                    if (cellRenderer != null) {
                        Component c = prepareRenderer(cellRenderer, row, column);
                        if (c != null) {
                            preferredWidth = Math.max(preferredWidth, c.getPreferredSize().width + 15);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            tableColumn.setPreferredWidth(Math.min(preferredWidth, maxWidth));
        }

        // 2. Ajuste dinámico de Altura de Filas para evitar que los textos se corten verticalmente
        for (int row = 0; row < getRowCount(); row++) {
            int maxHeightFila = getRowHeight(); // Altura base actual
            for (int column = 0; column < getColumnCount(); column++) {
                try {
                    TableCellRenderer cellRenderer = getCellRenderer(row, column);
                    Component comp = prepareRenderer(cellRenderer, row, column);
                    if (comp != null) {
                        maxHeightFila = Math.max(maxHeightFila, comp.getPreferredSize().height + 6);
                    }
                } catch (Exception ignored) {
                }
            }
            // Aplicamos la altura calculada a la fila si es mayor a la estándar
            if (maxHeightFila > getRowHeight()) {
                setRowHeight(row, maxHeightFila);
            }
        }
    }

    @Override
    public void setModel(TableModel dataModel) {
        if (getModel() != null && tableModelListener != null) {
            getModel().removeTableModelListener(tableModelListener);
        }
        super.setModel(dataModel);
        if (dataModel != null) {
            initAutoResize();
        }
    }
}
