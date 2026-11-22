package veterinaria.quality;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import veterinaria.vista.menu.MenuItem;
import veterinaria.vista.menu.PopupSubmenu;

/** Menú lateral sin cuadros alrededor de los submenús, en ambos temas. */
class MenuVerdeSubmenusSinCuadrosTest {

    @Test
    void temaClaroConservaVerdeOriginalYSeleccionVerdeOscuro() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        SwingUtilities.invokeAndWait(() -> {
            FlatMacLightLaf.setup();
            assertEquals(new Color(21, 110, 71), UIManager.getColor("Menu.background"),
                    "El menú claro debe conservar el verde elegido, no verde petróleo");
            Color seleccionado = UIManager.getColor("Menu.button.selectedBackground");
            assertNotNull(seleccionado);
            assertTrue(seleccionado.getGreen() > seleccionado.getRed());
            assertTrue(seleccionado.getGreen() > seleccionado.getBlue());
        });
    }

    @Test
    void submenusIntegradosSinRectangulosNiFondosClarosEnAmbosTemas() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        for (boolean oscuro : new boolean[]{false, true}) {
            SwingUtilities.invokeAndWait(() -> {
                if (oscuro) FlatMacDarkLaf.setup();
                else FlatMacLightLaf.setup();
                MenuItem menu = new MenuItem(null,
                        new String[]{"MASCOTA", "Gestión de Mascota", "Historia Clínica"},
                        1, java.util.List.of());
                assertEquals(3, menu.getComponentCount());
                JButton principal = (JButton) menu.getComponent(0);
                assertFalse(principal.isBorderPainted(),
                        "No debe mostrarse el borde rectangular de los botones principales");
                for (int i=1; i<menu.getComponentCount(); i++) {
                    JButton submenu = (JButton) menu.getComponent(i);
                    verificarSinRectangulo(submenu);
                }
                PopupSubmenu desplegable = new PopupSubmenu(
                        ComponentOrientation.LEFT_TO_RIGHT, null, 1,
                        new String[]{"MASCOTA", "Gestión de Mascota", "Historia Clínica"});
                assertEquals(2, desplegable.getComponentCount());
                for (Component c : desplegable.getComponents()) {
                    verificarSinRectangulo((JButton)c);
                }
            });
        }
    }

    private static void verificarSinRectangulo(JButton boton) {
        assertFalse(boton.isBorderPainted(), "El submenú dibuja un marco");
        assertFalse(boton.isContentAreaFilled(), "El submenú dibuja un fondo");
        assertFalse(boton.isOpaque(), "El submenú tapa el color original del lateral");
        boton.setSelected(true);
        boton.setSize(220, 35);
        BufferedImage img = new BufferedImage(220, 35, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            boton.paint(g);
        } finally {
            g.dispose();
        }
        int esquina = (img.getRGB(212, 20) >>> 24);
        assertEquals(0, esquina,
                "Un submenú seleccionado deja una placa rectangular pintada");
        assertTrue(boton.isSelected(), "El submenú debe mantener su estado");
    }
}
