package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import veterinaria.vista.application.form.RecuperarContrasenaDialog;

/** Reproduce y comprueba el diseño de la primera captura, sin MySQL ni correo. */
class CambiarClaveDisenoCompactoTest {

    @Test
    void formularioUsaAnchoCompletoYComienzaDebajoDelTitulo() throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            RecuperarContrasenaDialog dialog = null;
            try {
                dialog = new RecuperarContrasenaDialog((Window) null, "usuarioQA", true);
                dialog.validate();
                JTextField usuario = campo(dialog, "txtUsuario");
                JTextField email = campo(dialog, "txtEmail");
                JTextField dni = campo(dialog, "txtDni");
                JPanel form = panel(dialog, "panelFormulario");
                JLabel ayuda = label(dialog, "lblAyuda");
                JLabel titulo = buscarLabel(dialog.getContentPane(), "Restablecer acceso");
                JLabel logo = buscarLogo(dialog.getContentPane());
                assertNotNull(titulo, "Debe existir el título principal");
                assertNotNull(logo, "Debe existir el logo centrado");

                Rectangle bLogo = SwingUtilities.convertRectangle(
                        logo.getParent(), logo.getBounds(), dialog.getContentPane());
                Rectangle bTitulo = SwingUtilities.convertRectangle(
                        titulo.getParent(), titulo.getBounds(), dialog.getContentPane());
                Rectangle bAyuda = SwingUtilities.convertRectangle(
                        ayuda.getParent(), ayuda.getBounds(), dialog.getContentPane());
                assertEquals(JLabel.CENTER, titulo.getHorizontalAlignment(),
                        "El título no debe aparecer alineado a la izquierda");
                assertEquals(JLabel.CENTER, ayuda.getHorizontalAlignment());
                assertTrue(Math.abs(bLogo.getCenterX() - dialog.getContentPane().getWidth() / 2.0) < 24,
                        "Logo desplazado del centro: " + bLogo);
                assertTrue(Math.abs(bTitulo.getCenterX() - dialog.getContentPane().getWidth() / 2.0) < 24,
                        "Título desplazado del centro: " + bTitulo);
                assertTrue(bTitulo.y - (bLogo.y + bLogo.height) <= 22,
                        "Demasiado hueco entre logo y título: " + bLogo + " / " + bTitulo);
                assertTrue(bAyuda.y - (bTitulo.y + bTitulo.height) <= 22,
                        "Demasiado hueco entre título y explicación: " + bTitulo + " / " + bAyuda);
                Point posCampo = SwingUtilities.convertPoint(
                        usuario.getParent(), usuario.getLocation(), dialog.getContentPane());
                Point posAyuda = SwingUtilities.convertPoint(
                        ayuda.getParent(), ayuda.getLocation(), dialog.getContentPane());
                Rectangle boundsForm = SwingUtilities.convertRectangle(
                        form.getParent(), form.getBounds(), dialog.getContentPane());
                assertTrue(boundsForm.width >= 380, "El formulario debe ofrecer espacio para los datos");
                comprobarAnchoUtil(form, usuario, email, dni);
                assertTrue(posCampo.y < 330, "La primera fila no debe quedar centrada verticalmente");
                assertTrue(posCampo.y >= posAyuda.y + ayuda.getHeight());
                assertTrue(posCampo.y - (posAyuda.y + ayuda.getHeight()) < 80,
                        "La ayuda y los campos deben quedar próximos");
                assertFalse(contiene(dialog.getContentPane(), javax.swing.JScrollPane.class));
                assertEquals("usuarioQA", usuario.getText());
                assertFalse(usuario.isEditable(), "La cuenta conectada se mantiene fijada");

                // El segundo paso reutiliza la misma alineación, sin enviar correos.
                Method cambiarPaso = RecuperarContrasenaDialog.class
                        .getDeclaredMethod("cargarPaso2");
                cambiarPaso.setAccessible(true);
                cambiarPaso.invoke(dialog);
                dialog.validate();
                JTextField codigo = campo(dialog, "txtCodigo");
                comprobarAnchoUtil(form, codigo, campo(dialog, "txtNuevaContrasena"),
                        campo(dialog, "txtRepetirContrasena"));
            } catch (Throwable ex) {
                fallo.set(ex);
            } finally {
                if (dialog != null) dialog.dispose();
            }
        });
        if (fallo.get() != null) throw new AssertionError(fallo.get());
    }

    private static void comprobarAnchoUtil(JPanel form, JTextField... campos) {
        for (JTextField campo : campos) {
            assertTrue(campo.getWidth() >= form.getWidth() - 4,
                    "Cada campo debe ocupar el ancho útil del formulario");
        }
    }

    private static JLabel buscarLabel(Component root, String text) {
        if (root instanceof JLabel label && text.equals(label.getText())) return label;
        if (root instanceof Container container) {
            for (Component c : container.getComponents()) {
                JLabel found = buscarLabel(c, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JLabel buscarLogo(Component root) {
        if (root instanceof JLabel label && label.getIcon() != null
                && label.getIcon().getIconWidth() > 0) return label;
        if (root instanceof Container container) {
            for (Component c : container.getComponents()) {
                JLabel found = buscarLogo(c);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JTextField campo(JDialog dialog, String name) throws Exception {
        Field f = RecuperarContrasenaDialog.class.getDeclaredField(name);
        f.setAccessible(true);
        return (JTextField) f.get(dialog);
    }

    private static JPanel panel(JDialog dialog, String name) throws Exception {
        Field f = RecuperarContrasenaDialog.class.getDeclaredField(name);
        f.setAccessible(true);
        return (JPanel) f.get(dialog);
    }

    private static JLabel label(JDialog dialog, String name) throws Exception {
        Field f = RecuperarContrasenaDialog.class.getDeclaredField(name);
        f.setAccessible(true);
        return (JLabel) f.get(dialog);
    }

    private static boolean contiene(Component c, Class<?> type) {
        if (type.isInstance(c)) return true;
        if (c instanceof Container container)
            for (Component child : container.getComponents())
                if (contiene(child, type)) return true;
        return false;
    }
}
