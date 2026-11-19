package veterinaria.util;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

/** Regresión: Consultas solo busca prefijos de Paciente, Dueño y Veterinario. */
class PaginacionTablasPrefijoTest {

    @Test
    void buscaSoloInicioDeLosTresNombresYCombinaEstado() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Paciente", "Dueño", "Veterinario", "Motivo", "Estado"}, 0);
            model.addRow(new Object[]{"Nina", "Ana López", "Carlos", "Control", "EN_ESPERA"});
            model.addRow(new Object[]{"Bruno", "Noelia Ortiz", "Carlos", "Control", "EN_ESPERA"});
            model.addRow(new Object[]{"Milo", "Laura", "Natalia", "Control", "FINALIZADO"});
            model.addRow(new Object[]{"Bruno", "Carolina", "Romina", "Náuseas", "EN_ESPERA"});
            JTable table = new JTable(model);
            JTextField query = new JTextField();
            PaginacionTablas paginas = new PaginacionTablas(table, query, 15,
                    new int[]{0, 1, 2}, new int[]{0, 1, 2});
            paginas.activarBusquedaPorPrefijo();

            query.setText("n");
            paginas.actualizar();
            assertEquals(3, table.getRowCount(), "Nina, Noelia (dueño) y Natalia (veterinaria)");
            query.setText("ar");
            paginas.actualizar();
            assertEquals(0, table.getRowCount(), "No debe encontrar Carolina ni Carlos");
            query.setText("rom");
            paginas.actualizar();
            assertEquals(1, table.getRowCount(), "Debe encontrar Romina como veterinaria");
            query.setText("náu");
            paginas.actualizar();
            assertEquals(0, table.getRowCount(), "Motivo está fuera de las tres columnas");

            paginas.establecerFiltroAdicional(row -> "EN_ESPERA".equals(model.getValueAt(row, 4)));
            query.setText("n");
            paginas.actualizar();
            assertEquals(2, table.getRowCount(), "Estado y prefijo deben combinarse");
            query.setText("noe");
            paginas.actualizar();
            assertEquals(1, table.getRowCount(), "Noelia coincide como dueña");
        });
    }

    @Test
    void aceptaAcentosSinBuscarEnMedio() {
        assertTrue(ManejoTablas.coincideBusqueda("María Ortiz", "maria", true));
        assertTrue(ManejoTablas.coincideBusqueda("Natalia", "N", true));
        assertFalse(ManejoTablas.coincideBusqueda("Carolina", "n", true));
        assertFalse(ManejoTablas.coincideBusqueda("Villamil", "illa", true));
    }
}
