package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import veterinaria.vista.application.form.MiPerfilDialog;

/** Regresión visual del diálogo de perfil sin acceder a MySQL. */
class MiPerfilCompactoSinScrollTest {

    @Test
    void usaFichaCompactaConChipYBotonPrincipalSinScroll() throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            MiPerfilDialog dialogo = null;
            try {
                // Sesión ausente evita cualquier consulta a la base de datos.
                dialogo = new MiPerfilDialog((Window) null, null);
                assertEquals("Ficha de usuario", dialogo.getTitle());
                assertFalse(contiene(dialogo.getContentPane(), JScrollPane.class),
                        "La ficha debe mostrar todos los campos sin barras de desplazamiento");
                assertTrue(contieneTexto(dialogo.getContentPane(), "Datos personales"));
                assertTrue(contieneTexto(dialogo.getContentPane(), "Correo electrónico"));
                assertTrue(contieneClase(dialogo.getContentPane(), "EstadoBadge"),
                        "El estado debe verse como pastilla de color, no un símbolo de fuente");
                JButton cerrar = boton(dialogo.getContentPane(), "Cerrar");
                assertNotNull(cerrar);
                assertEquals("hibridoPrimario",
                        cerrar.getClientProperty("FlatLaf.styleClass"));
                assertTrue(dialogo.getWidth() >= 620);
                assertTrue(dialogo.getHeight() >= 370);
            } catch (Throwable ex) {
                fallo.set(ex);
            } finally {
                if (dialogo != null) dialogo.dispose();
            }
        });
        if (fallo.get() != null) throw new AssertionError(fallo.get());
    }

    private static boolean contiene(Component component, Class<?> target) {
        if (target.isInstance(component)) return true;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (contiene(child, target)) return true;
            }
        }
        return false;
    }

    private static boolean contieneClase(Component c, String simpleName) {
        if (c.getClass().getSimpleName().equals(simpleName)) return true;
        if (c instanceof Container container)
            for (Component child : container.getComponents())
                if (contieneClase(child, simpleName)) return true;
        return false;
    }

    private static boolean contieneTexto(Component c, String value) {
        if (c instanceof JLabel label && value.equals(label.getText())) return true;
        if (c instanceof Container container)
            for (Component child : container.getComponents())
                if (contieneTexto(child, value)) return true;
        return false;
    }

    private static JButton boton(Component c, String text) {
        if (c instanceof JButton button && text.equals(button.getText())) return button;
        if (c instanceof Container container)
            for (Component child : container.getComponents()) {
                JButton found = boton(child, text);
                if (found != null) return found;
            }
        return null;
    }
}
