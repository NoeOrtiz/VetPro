package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import com.formdev.flatlaf.FlatClientProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.junit.jupiter.api.Test;
import veterinaria.util.ui.EstiloHibrido;

class ClientesHomeEsteticaTest {

    @Test
    void conservaSeleccionYColumnasAlCambiarEstiloDeTabla() {
        Object[][] rows = {{Boolean.TRUE, 1, "Alma", "Cliente", "123", "Calle",
                "correo@example.test", "123", "Razon", "20-1", "Activo", "21/11/2026"}};
        String[] columnas = {"Seleccionar", "ID", "Nombre", "Apellido", "DNI",
                "Dirección", "Correo", "Teléfono", "Razón social", "CUIT",
                "Estado", "Alta"};
        JTable tabla = new JTable(rows, columnas);
        int columnasOriginales = tabla.getColumnCount();
        TableCellRenderer rendererOriginal = tabla.getColumnModel().getColumn(0).getCellRenderer();
        EstiloHibrido.tablaClientes(tabla, 10);
        assertEquals(columnasOriginales, tabla.getColumnCount());
        assertSame(rendererOriginal, tabla.getColumnModel().getColumn(0).getCellRenderer());
        assertTrue(tabla.getRowHeight() >= 32);
        assertEquals(Boolean.TRUE, tabla.getValueAt(0, 0));
        assertEquals("Activo", tabla.getValueAt(0, 10));
        assertNotNull(tabla.getColumnModel().getColumn(10).getCellRenderer());
    }

    @Test
    void aplicaBotonesYRolesVisualesSinCambiarAcciones() {
        JButton nuevo = new JButton("Nuevo cliente");
        JButton imprimir = new JButton("Imprimir listado");
        JButton desactivar = new JButton("Desactivar");
        EstiloHibrido.botonesPrimarios(nuevo);
        EstiloHibrido.botonesSecundarios(imprimir);
        EstiloHibrido.botonesPeligro(desactivar);
        assertEquals("hibridoPrimario", nuevo.getClientProperty(
                FlatClientProperties.STYLE_CLASS));
        assertEquals("hibridoSecundario", imprimir.getClientProperty(
                FlatClientProperties.STYLE_CLASS));
        assertEquals("hibridoPeligro", desactivar.getClientProperty(
                FlatClientProperties.STYLE_CLASS));
        assertEquals("Nuevo cliente", nuevo.getText());
        assertEquals(0, nuevo.getActionListeners().length);
    }

    @Test
    void homeTieneLosCuatroAccesosOriginales() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/veterinaria/vista/FormBienvenida.java"));
        assertTrue(source.contains("acceso(\"Nueva Consulta\""));
        assertTrue(source.contains("acceso(\"Agenda / Calendario\""));
        assertTrue(source.contains("acceso(\"Pacientes (Fichas)\""));
        assertTrue(source.contains("acceso(\"Caja\""));
        assertTrue(source.contains("hibridoAcceso"));
    }

    @Test
    void clientesConservaCheckboxPaginacionYPermisos() throws Exception {
        String s = Files.readString(Path.of(
                "src/main/java/veterinaria/vista/FormCliente.java"));
        assertTrue(s.contains("COL_SELECCION = 0"));
        assertTrue(s.contains("paginacionCompartida.activarBusquedaPorPrefijo()"));
        assertTrue(s.contains("puedeCambiarEstadoCliente()"));
        assertTrue(s.contains("EstiloHibrido.tablaClientes(tableClientes, COL_ESTADO)"));
        assertTrue(s.contains("aplicarEstiloVisualClientes()"));
    }

    @Test
    void temaRealmenteEstilizaBotonesYTablas() throws Exception {
        String css = Files.readString(Path.of(
                "src/main/resources/veterinaria/theme/FlatLaf.properties"));
        assertTrue(css.contains("TableHeader.height=34"));
        assertTrue(css.contains("Table.rowHeight=32"));
        assertTrue(css.contains("[style]Button.hibridoPrimario"));
        assertTrue(css.contains("[style]Button.hibridoSecundario"));
        assertTrue(css.contains("[style]Button.hibridoPeligro"));
        assertTrue(css.contains("[style]Button.hibridoAcceso"));
    }
}
