package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import veterinaria.vista.menu.MenuHoverButton;
import veterinaria.vista.menu.MenuItem;
import veterinaria.vista.menu.PopupSubmenu;

/** Hover perceptible, redondeado y no permanente en los dos temas. */
class SubmenusHoverRedondeadoTest {

    @Test
    void hoverVisibleSoloAlPasarMouseYConEsquinasTransparentes() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        for (boolean oscuro : new boolean[]{false, true}) {
            SwingUtilities.invokeAndWait(() -> {
                if (oscuro) FlatMacDarkLaf.setup();
                else FlatMacLightLaf.setup();
                assertNotNull(UIManager.getColor("Menu.button.hoverBackground"));
                MenuItem lateral = new MenuItem(null, new String[]{
                    "CLIENTE", "Gestión de Cliente", "Reclamos"},
                    1, java.util.List.of());
                PopupSubmenu flotante = new PopupSubmenu(
                        ComponentOrientation.LEFT_TO_RIGHT, null, 1,
                        new String[]{"CLIENTE", "Gestión de Cliente", "Reclamos"});
                for (JButton boton : new JButton[]{
                        (JButton) lateral.getComponent(1),
                        (JButton) flotante.getComponent(0)}) {
                    assertInstanceOf(MenuHoverButton.class, boton);
                    assertFalse(boton.isOpaque());
                    assertFalse(boton.isBorderPainted());
                    assertFalse(boton.isContentAreaFilled());
                    boton.setSize(220, 35);

                    BufferedImage normal = pintar(boton);
                    assertEquals(0, normal.getRGB(209, 24) >>> 24,
                            "Submenú en reposo no debe mostrar recuadro");

                    boton.getModel().setRollover(true);
                    BufferedImage hover = pintar(boton);
                    assertTrue((hover.getRGB(209, 24) >>> 24) > 0,
                            "Debe verse el fondo suave al pasar el mouse");
                    assertEquals(0, hover.getRGB(0, 0) >>> 24,
                            "No pintar un cuadrado de esquina a esquina");
                    boton.getModel().setRollover(false);

                    BufferedImage restablecido = pintar(boton);
                    assertEquals(0, restablecido.getRGB(209, 24) >>> 24,
                            "El hover desaparece al retirar el mouse");
                }
            });
        }
    }

    private static BufferedImage pintar(JButton boton) {
        BufferedImage img = new BufferedImage(
                boton.getWidth(), boton.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { boton.paint(g); } finally { g.dispose(); }
        return img;
    }
}
