package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Constructor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/**
 * Verifica la cápsula del usuario sin levantar la aplicación ni usar MySQL.
 * El cambio visual debe conservar las notificaciones y el menú de cuenta.
 */
class PerfilCabeceraHibridaTest {
    private String mainFormCode() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/veterinaria/vista/application/form/MainForm.java"));
    }

    @Test
    void conservaCampanaFotoNombreRolYAccesoSeguro() throws Exception {
        String src = mainFormCode();
        assertTrue(src.contains("JPanel controles = new TarjetaPerfil()"));
        assertTrue(src.contains("controles.add(notificationBell)"));
        assertTrue(src.contains("refrescarFotoPerfil()"));
        assertTrue(src.contains("controles.add(avatarPerfil)"));
        assertTrue(src.contains("cuenta.getFont().deriveFont(java.awt.Font.BOLD)"));
        assertTrue(src.contains("datos.add(rol)"));
        assertTrue(src.contains("new FlechaPerfil()"));
        assertTrue(src.contains("instalarMenuPerfil(controles"));
        assertTrue(src.contains("actual.puede(\"FormUsuario\")"));
        assertTrue(src.contains("showFormConPermiso(new FormUsuario())"));
        assertTrue(src.contains("new RecuperarContrasenaDialog("));
        assertTrue(src.contains("Application.logout()"));
    }

    @Test
    void capsulaYFlechaSePintanEnSwing() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> type = Class.forName(
                        "veterinaria.vista.application.form.MainForm$TarjetaPerfil");
                Constructor<?> ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                JPanel tarjeta = (JPanel) ctor.newInstance();
                tarjeta.setSize(245, 52);
                BufferedImage imagen = new BufferedImage(
                        245, 52, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = imagen.createGraphics();
                try {
                    tarjeta.paint(graphics);
                } finally {
                    graphics.dispose();
                }
                assertNotEquals(0, imagen.getRGB(80, 25) >>> 24,
                        "La tarjeta debe pintar una superficie visible");
                assertEquals(0, imagen.getRGB(0, 0) >>> 24,
                        "Las esquinas de la píldora son redondeadas");
                Class<?> flechaType = Class.forName(
                        "veterinaria.vista.application.form.MainForm$FlechaPerfil");
                Constructor<?> flechaCtor = flechaType.getDeclaredConstructor();
                flechaCtor.setAccessible(true);
                Component flecha = (Component) flechaCtor.newInstance();
                assertInstanceOf(JComponent.class, flecha);
                assertTrue(flecha.getPreferredSize().width >= 12);
            } catch (Exception ex) {
                throw new AssertionError("El perfil debe poder pintarse sin BD", ex);
            }
        });
    }
}
