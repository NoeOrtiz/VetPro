package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import veterinaria.vista.table.TableGradientCell;
import veterinaria.vista.table.TableGradientCheckBoxRenderer;

/**
 * Regresión de la barra de Usuarios: iconos del compañero,
 * selección con degradado, datos y permisos de VetPRO original.
 * No consulta MySQL ni modifica datos reales.
 */
class UsuariosToolbarIconosTest {

    private String fuente() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/veterinaria/vista/FormUsuario.java"));
    }

    @Test
    void accionesAlineadasEnDosFilasSinPerderComportamiento() throws Exception {
        String s = fuente();
        assertTrue(s.contains("filtros.add(btnNuevo, java.awt.BorderLayout.EAST)"));
        assertTrue(s.contains("acciones.add(btnVerFicha)"));
        assertTrue(s.contains("acciones.add(btnEditar)"));
        assertTrue(s.contains("acciones.add(btnEliminar)"));
        assertTrue(s.contains("acciones.add(btnImprimir)"));
        assertTrue(s.contains("acciones.add(btnImprimirLista)"));
        assertTrue(s.contains("btnNuevo.setText(\"Nuevo usuario\")"));
        assertTrue(s.contains("btnImprimir.setText(\"Imprimir ficha\")"));
        assertTrue(s.contains("btnImprimirLista.setText(\"Imprimir listado\")"));
        assertTrue(s.contains("btnVerFicha.addActionListener"));
        assertTrue(s.contains("btnImprimirLista.addActionListener"));
        assertTrue(s.contains("PermisoUI.aplicar"));
        assertTrue(s.contains("permisoUsuario(\"EDITAR\")"));
        assertTrue(s.contains("permisoUsuario(\"ELIMINAR\")"));
        assertTrue(s.contains("permisoUsuario(\"IMPRIMIR\")"));
    }

    @Test
    void busquedaIncluyeRolYUsuarioYPreservaPrefijoYPaginacion() throws Exception {
        String s = fuente();
        assertTrue(s.contains("new int[]{1, 2, 3, 6, 7}"));
        assertTrue(s.contains("paginacionCompartida.activarBusquedaPorPrefijo()"));
        assertTrue(s.contains("Nombre, apellido, DNI, usuario o rol"));
        assertTrue(s.contains("paginacionCompartida.limpiarBusqueda()"));
        assertTrue(s.contains("new veterinaria.vista.table.TableGradientCheckBoxRenderer()"));
        assertTrue(s.contains("table.putClientProperty(\"vetpro.gradienteCompanero\", Boolean.TRUE)"));
        assertTrue(s.contains("columnaCheckbox"));
    }

    @Test
    void iconosExistenYSePuedenDibujar() throws Exception {
        for (String ruta : new String[]{
                "nuevo_usuario.svg", "editar_usuario.svg", "desactivar_usuario.svg",
                "limpiar_usuario.svg", "ver_recibo.svg", "imprimir_recibo.svg"}) {
            String recurso = "veterinaria/icon/svg/" + ruta;
            assertNotNull(getClass().getClassLoader().getResource(recurso),
                    "Falta recurso para la barra: " + recurso);
            FlatSVGIcon icon = new FlatSVGIcon(recurso, 0.70f);
            assertTrue(icon.getIconWidth() >= 13);
            BufferedImage img = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(new JLabel(), img.getGraphics(), 0, 0);
            boolean hayTinta = false;
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++)
                    if ((img.getRGB(x, y) >>> 24) > 0) hayTinta = true;
            assertTrue(hayTinta, "Icono vacío: " + recurso);
        }
    }

    @Test
    void seleccionMuestraDegradadoPeroNoSustituyeCasilla() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Seleccionar","Nombre","Estado"}, 0) {
                @Override public Class<?> getColumnClass(int col) {
                    return col == 0 ? Boolean.class : String.class;
                }
                @Override public boolean isCellEditable(int row, int col) {
                    return col == 0;
                }
            };
            model.addRow(new Object[]{true, "Usuario de prueba", "Activo"});
            JTable table = new JTable(model);
            table.setSize(500, 35);
            table.doLayout();
            table.putClientProperty("vetpro.gradienteCompanero", true);
            TableGradientCell texto = new TableGradientCell(0);
            Component renderer = texto.getTableCellRendererComponent(
                    table,"Usuario de prueba",false,false,0,1);
            assertNotNull(renderer);
            assertInstanceOf(JLabel.class, renderer);
            TableGradientCheckBoxRenderer checkbox = new TableGradientCheckBoxRenderer();
            assertNotNull(checkbox.getTableCellRendererComponent(
                    table,true,false,false,0,0));
            assertEquals(Boolean.TRUE, table.getValueAt(0,0));
            table.putClientProperty("vetpro.gradienteCompanero", false);
            assertNotNull(texto.getTableCellRendererComponent(
                    table,"Usuario de prueba",false,false,0,1));
        });
    }
}
