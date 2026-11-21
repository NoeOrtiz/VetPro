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
                Point posCampo = SwingUtilities.convertPoint(
                        usuario.getParent(), usuario.getLocation(), dialog.getContentPane());
                Point posAyuda = SwingUtilities.convertPoint(
                        ayuda.getParent(), ayuda.getLocation(), dialog.getContentPane());
                Rectangle boundsForm = SwingUtilities.convertRectangle(
                        form.getParent(), form.getBounds(), dialog.getContentPane());
                assertTrue(boundsForm.width >= 530,
                        "El formulario debe ocupar el ancho disponible, no una caja de 525 px");
                assertTrue(usuario.getWidth() >= 420, "Los campos deben ser anchos");
                assertTrue(email.getWidth() >= 420);
                assertTrue(dni.getWidth() >= 420);
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
                assertTrue(codigo.getWidth() >= 420, "El código debe ocupar el ancho útil");
            } catch (Throwable ex) {
                fallo.set(ex);
            } finally {
                if (dialog != null) dialog.dispose();
            }
        });
        if (fallo.get() != null) throw new AssertionError(fallo.get());
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
