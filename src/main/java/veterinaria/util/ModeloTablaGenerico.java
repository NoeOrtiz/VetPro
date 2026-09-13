package veterinaria.util;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class ModeloTablaGenerico<T> extends AbstractTableModel {

    private final String[] columnas;
    private List<T> listaDatos;
    private boolean[] selecciones;
    private final IColumnResolver<T> columnResolver;

    // Interfaz funcional para resolver los datos de cada columna
    public interface IColumnResolver<T> {

        Object obtenerValorColumna(T item, int columnIndex);
    }

    public ModeloTablaGenerico(String[] columnas, IColumnResolver<T> columnResolver) {
        this.columnas = columnas;
        this.columnResolver = columnResolver;
        this.listaDatos = new ArrayList<>();
        this.selecciones = new boolean[0];
    }

    // Nombre limpio y estándar para refrescar o cargar los datos
    public void cargarDatos(List<T> nuevosDatos) {
        this.listaDatos = nuevosDatos != null ? nuevosDatos : new ArrayList<>();
        this.selecciones = new boolean[this.listaDatos.size()];
        fireTableDataChanged();
    }

    public T obtenerObjetoEn(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < listaDatos.size()) {
            return listaDatos.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return listaDatos.size();
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnas[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class; // Columna 0 siempre es el Checkbox
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0; // Solo se edita el checkbox
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return selecciones[rowIndex];
        }
        T item = listaDatos.get(rowIndex);
        return columnResolver.obtenerValorColumna(item, columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            boolean seleccionar = (Boolean) aValue;

            // Si están marcando esta fila, desmarcamos todas las demás (Selección Única)
            if (seleccionar) {
                for (int i = 0; i < selecciones.length; i++) {
                    selecciones[i] = (i == rowIndex);
                }
                fireTableDataChanged(); // Refresca toda la tabla para actualizar los checkboxes visualmente
            } else {
                selecciones[rowIndex] = false;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
