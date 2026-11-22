package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import veterinaria.util.ui.EstiloHibrido;

class MenuUsuariosEsteticaTest {

    private String source(String path) throws Exception {
        return Files.readString(Path.of("src/main/java/veterinaria", path));
    }

    @Test
    void conservaControlesOriginalesUsuariosYPantallas() throws Exception {
        String u = source("vista/FormUsuario.java");
        assertTrue(u.contains("PermisoUI.aplicar"));
        assertTrue(u.contains("paginacionCompartida.activarBusquedaPorPrefijo()"));
        assertTrue(u.contains("confirmarCreacionUsuario()"));
        assertTrue(u.contains("confirmarEdicionUsuario()"));
        assertTrue(u.contains("btnEliminarActionPerformed"));
        assertTrue(u.contains("procesarYGuardarImagenPerfil()"));
        assertTrue(u.contains("imprimirListaUsuarios()"));
        assertTrue(u.contains("imprimirRegistroSeleccionado()"));
        assertTrue(u.contains("organizarVistasUsuario()"));
        assertTrue(u.contains("new java.awt.CardLayout()"));
        assertTrue(u.contains("mostrarVistaUsuario(true)"));
        assertTrue(u.contains("EstiloHibrido.tablaClientes(table, 9)"));
        assertTrue(u.contains("new int[]{1, 2, 3, 6, 7}"));
        assertTrue(u.contains("paginacionCompartida.activarBusquedaPorPrefijo()"));
        assertTrue(u.contains("[52!][52!][52!]"));
        assertTrue(u.contains("new java.awt.Dimension(780, 253)"));
    }

    @Test
    void dejaUnSoloSubmenuAbiertoYResaltaEnVerde() throws Exception {
        String menu = source("vista/menu/Menu.java");
        String item = source("vista/menu/MenuItem.java");
        assertTrue(menu.contains("toggleMenu(MenuItem selected)"));
        assertTrue(menu.contains("item.hideMenuItem()"));
        assertTrue(item.contains("menu.toggleMenu(MenuItem.this)"));
        assertTrue(item.contains("paintActiveBar(g)"));
        assertTrue(item.contains("App.brandColor"));
    }

    @Test
    void noIncluyeEstilosDeFlatLafQueArrojanErroresEnLaInterfaz() throws Exception {
        String p = Files.readString(Path.of(
                "src/main/resources/veterinaria/theme/FlatLaf.properties"));
        assertFalse(p.contains("foreground: $Label.foreground; arc: 16"));
        assertFalse(p.contains("alternateRowColor: $App.surfaceAlt.background"));
        assertFalse(p.contains("borderWidth:"), "Los botones FlatLaf no aceptan borderWidth en STYLE");
        String dialogo = source("vista/application/form/RecuperarContrasenaDialog.java");
        assertFalse(dialogo.contains("borderWidth:"), "El diálogo también debe omitir ese estilo");
        String menuItem = source("vista/menu/MenuItem.java");
        assertFalse(menuItem.contains("+ \"arc:10;\""));
    }

    @Test
    void estilosDeBotonesYTablasSePuedenInstalarEnSwing() throws Exception {
        List<String> errores = new ArrayList<>();
        Logger logger = Logger.getLogger("com.formdev.flatlaf");
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                String texto = String.valueOf(record.getMessage());
                if (texto.contains("unknown style") ||
                        (record.getThrown() != null && String.valueOf(
                                record.getThrown()).contains("UnknownStyleException"))) {
                    errores.add(texto);
                }
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
            FlatMacDarkLaf.setup();
            SwingUtilities.invokeAndWait(() -> {
                JButton primary = new JButton("Nuevo usuario");
                JButton secondary = new JButton("Imprimir");
                JButton danger = new JButton("Desactivar");
                JButton home = new JButton("Nueva Consulta");
                EstiloHibrido.botonesPrimarios(primary);
                EstiloHibrido.botonesSecundarios(secondary);
                EstiloHibrido.botonesPeligro(danger);
                home.putClientProperty("FlatLaf.styleClass", "hibridoAcceso");
                for (JButton boton : new JButton[]{primary,secondary,danger,home}) {
                    SwingUtilities.updateComponentTreeUI(boton);
                    assertNotNull(boton.getPreferredSize());
                }
                JTable t = new JTable(new Object[][]{
                    {Boolean.FALSE, "Usuario prueba", "Activo"},
                    {Boolean.TRUE, "Otro usuario", "Inactivo"}
                }, new Object[]{"Seleccionar","Nombre","Estado"});
                EstiloHibrido.tablaClientes(t, 2);
                // El texto recibe degradado de selección y el estado su badge.
                assertTrue(t.getColumnModel().getColumn(1).getCellRenderer()
                        instanceof veterinaria.vista.table.TableGradientCell);
                SwingUtilities.updateComponentTreeUI(t);
                assertNotNull(t.getColumnModel().getColumn(2).getCellRenderer());
            });
            assertTrue(errores.isEmpty(), "FlatLaf no debe registrar estilos desconocidos: " + errores);
        } finally {
            logger.removeHandler(handler);
            UIManager.getDefaults().remove("FlatLaf.styleClass");
        }
    }
}
