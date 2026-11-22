package veterinaria.quality;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import veterinaria.vista.menu.mode.LightDarkMode;

/** El selector usa una sola pastilla, sin panel rectangular de fondo. */
class SelectorTemaPastillaTest {

    @Test
    void claroYOscuroSoloResaltanLaOpcionElegida() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        for (boolean dark : new boolean[]{false, true}) {
            SwingUtilities.invokeAndWait(() -> {
                if (dark) FlatMacDarkLaf.setup();
                else FlatMacLightLaf.setup();

                LightDarkMode panel = new LightDarkMode();
                panel.setMenuFull(true);
                panel.setSize(228, 38);
                panel.doLayout();
                assertFalse(panel.isOpaque(),
                        "No debe existir un rectángulo detrás de Claro/Oscuro");

                JButton claro = (JButton) panel.getComponent(0);
                JButton oscuro = (JButton) panel.getComponent(1);
                assertEquals("Claro", claro.getText());
                assertEquals("Oscuro", oscuro.getText());
                assertEquals("roundRect",
                        claro.getClientProperty(FlatClientProperties.BUTTON_TYPE));
                assertEquals("roundRect",
                        oscuro.getClientProperty(FlatClientProperties.BUTTON_TYPE));
                assertFalse(claro.isBorderPainted());
                assertFalse(oscuro.isBorderPainted());
                assertEquals(!dark, claro.isContentAreaFilled(),
                        "En claro solo Claro debe tener fondo");
                assertEquals(dark, oscuro.isContentAreaFilled(),
                        "En oscuro solo Oscuro debe tener fondo");

                JButton inactivo = dark ? claro : oscuro;
                String estiloInactivo = String.valueOf(
                        inactivo.getClientProperty(FlatClientProperties.STYLE));
                assertFalse(estiloInactivo.contains("background:"),
                        "La opción inactiva debe dejar ver el verde original");
                JButton activo = dark ? oscuro : claro;
                assertTrue(String.valueOf(
                        activo.getClientProperty(FlatClientProperties.STYLE))
                        .contains("background:$Menu.lightdark.button.background"));

                BufferedImage imagen = new BufferedImage(
                        228, 38, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = imagen.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                assertEquals(0, imagen.getRGB(0, 0) >>> 24,
                        "El borde exterior del selector debe ser transparente");
            });
        }
    }

    @Test
    void modoColapsadoConservaBotonYAccion() throws Exception {
        FlatMacLightLaf.setup();
        SwingUtilities.invokeAndWait(() -> {
            LightDarkMode panel = new LightDarkMode();
            panel.setMenuFull(false);
            assertFalse(panel.getComponent(0).isVisible());
            assertFalse(panel.getComponent(1).isVisible());
            assertTrue(panel.getComponent(2).isVisible());
            JButton alternar = (JButton) panel.getComponent(2);
            assertTrue(alternar.getActionListeners().length > 0);
            assertEquals("roundRect",
                    alternar.getClientProperty(FlatClientProperties.BUTTON_TYPE));
        });
    }
}
