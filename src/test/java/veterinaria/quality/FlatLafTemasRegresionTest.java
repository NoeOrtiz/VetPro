package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.ComponentOrientation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import veterinaria.util.ui.EstiloHibrido;
import veterinaria.vista.menu.MenuItem;
import veterinaria.vista.menu.PopupSubmenu;
import veterinaria.vista.menu.mode.LightDarkMode;
import veterinaria.vista.componentes.ButtonAction;

/**
 * Verifica que los estilos efectivamente usados en Swing se instalen
 * sin errores tanto con FlatMacDarkLaf como con FlatMacLightLaf.
 * No crea usuarios ni conecta la base MySQL.
 */
class FlatLafTemasRegresionTest {

    @Test
    void estilosDeMenusBotonesTablasYTarjetasEnAmbosTemas() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        for (boolean oscuro : new boolean[]{true, false}) {
            List<String> errores = new ArrayList<>();
            Logger logger = Logger.getLogger("com.formdev.flatlaf");
            Handler handler = new Handler() {
                @Override public void publish(LogRecord record) {
                    String s = String.valueOf(record.getMessage());
                    if ((record.getLevel().intValue() >= Level.SEVERE.intValue())
                            || s.contains("unknown style")
                            || (record.getThrown() != null && String.valueOf(
                                  record.getThrown()).contains("UnknownStyleException"))) {
                        errores.add(s + (record.getThrown() == null
                                ? "" : ": " + record.getThrown()));
                    }
                }
                @Override public void flush() {}
                @Override public void close() {}
            };
            logger.addHandler(handler);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (oscuro) FlatMacDarkLaf.setup();
                    else FlatMacLightLaf.setup();

                    JPanel titulo = new JPanel();
                    titulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "tarjeta");
                    JPanel banda = new JPanel();
                    banda.putClientProperty(FlatClientProperties.STYLE_CLASS, "banda");
                    JPanel alerta = new JPanel();
                    alerta.putClientProperty(FlatClientProperties.STYLE_CLASS, "alerta");
                    for (JPanel panel : new JPanel[]{titulo, banda, alerta}) {
                        SwingUtilities.updateComponentTreeUI(panel);
                        assertNotNull(panel.getPreferredSize());
                    }

                    JButton primario = new JButton("Nuevo");
                    JButton secundario = new JButton("Imprimir");
                    JButton peligro = new JButton("Eliminar");
                    JButton sutil = new JButton("Limpiar");
                    EstiloHibrido.botonesPrimarios(primario);
                    EstiloHibrido.botonesSecundarios(secundario);
                    EstiloHibrido.botonesPeligro(peligro);
                    EstiloHibrido.botonesSutiles(sutil);
                    for (JButton boton : new JButton[]{primario,secundario,peligro,sutil}) {
                        SwingUtilities.updateComponentTreeUI(boton);
                        assertNotNull(boton.getPreferredSize());
                    }

                    String[] categorias = {"HOME", "Inicio"};
                    MenuItem item = new MenuItem(null, categorias, 0, List.of());
                    SwingUtilities.updateComponentTreeUI(item);
                    PopupSubmenu submenu = new PopupSubmenu(
                            ComponentOrientation.LEFT_TO_RIGHT, null, 0, categorias);
                    SwingUtilities.updateComponentTreeUI(submenu);

                    LightDarkMode selectorTema = new LightDarkMode();
                    SwingUtilities.updateComponentTreeUI(selectorTema);
                    assertNotNull(selectorTema.getPreferredSize());
                    ButtonAction accion = new ButtonAction();
                    SwingUtilities.updateComponentTreeUI(accion);
                    assertNotNull(accion.getPreferredSize());

                    JTable tabla = new JTable(new Object[][]{
                        {false,"Usuaria de prueba","Activo"},
                        {true,"Otro usuario","Inactivo"}
                    },new Object[]{"Seleccionar","Nombre","Estado"});
                    EstiloHibrido.tablaClientes(tabla,2);
                    SwingUtilities.updateComponentTreeUI(tabla);
                    assertNotNull(tabla.getColumnModel().getColumn(2).getCellRenderer());
                    assertEquals(3,tabla.getColumnCount());
                });
                assertTrue(errores.isEmpty(),
                        "Estilos FlatLaf inválidos para " + (oscuro?"oscuro":"claro")
                        + ": " + errores);
            } finally {
                logger.removeHandler(handler);
            }
        }
    }

    @Test
    void noQuedanPropiedadesInvalidasDeBotonEnElMenu() throws Exception {
        String a=Files.readString(Path.of(
                "src/main/java/veterinaria/vista/menu/MenuItem.java"));
        String b=Files.readString(Path.of(
                "src/main/java/veterinaria/vista/menu/PopupSubmenu.java"));
        String tema=Files.readString(Path.of(
                "src/main/resources/veterinaria/theme/FlatLaf.properties"));
        assertFalse(a.contains("\"borderWidth:"));
        assertFalse(b.contains("\"borderWidth:"));
        assertFalse(b.contains("\"arc:10;\""));
        for (String ruta : new String[]{
                "src/main/java/veterinaria/vista/application/form/MainForm.java",
                "src/main/java/veterinaria/vista/application/form/FormLogin.java",
                "src/main/java/veterinaria/vista/componentes/ButtonAction.java",
                "src/main/java/veterinaria/vista/menu/mode/LightDarkMode.java"}) {
            String codigo = Files.readString(Path.of(ruta));
            assertFalse(codigo.contains("\"borderWidth:"),
                    "Estilo no válido en " + ruta);
        }
        assertFalse(tema.contains("borderWidth:"));
        assertTrue(tema.contains("[style]Button.hibridoPrimario"));
    }
}
