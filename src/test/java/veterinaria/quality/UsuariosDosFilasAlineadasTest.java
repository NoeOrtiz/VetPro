package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.FlowLayout;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import veterinaria.vista.FormUsuario;

/** Prueba posiciones reales de los cinco botones de acción, sin MySQL. */
class UsuariosDosFilasAlineadasTest {

    @Test
    void cincoAccionesYBotonNuevoMantienenAlturaSeparacionYColumnas() throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                filtros.add(new JLabel("Buscar usuario:"));
                filtros.add(new JTextField(32));
                filtros.add(new JLabel("Estado:"));
                JButton nuevo = new JButton("Nuevo usuario");
                JButton cancelar = new JButton();
                JButton ver = new JButton("Ver ficha");
                JButton editar = new JButton("Editar");
                JButton desactivar = new JButton("Desactivar");
                JButton ficha = new JButton("Imprimir ficha");
                JButton listado = new JButton("Imprimir listado");
                JButton[] acciones = {ver, editar, desactivar, ficha, listado};

                Method metodo = FormUsuario.class.getDeclaredMethod(
                        "crearBarraUsuariosAlineada", JPanel.class, JButton.class,
                        JButton.class, JButton.class, JButton.class,
                        JButton.class, JButton.class, JButton.class);
                metodo.setAccessible(true);
                JPanel barra = (JPanel) metodo.invoke(null, filtros, nuevo,
                        cancelar, ver, editar, desactivar, ficha, listado);
                assertEquals(8, barra.getComponentCount(),
                        "La barra contiene filtros, Nuevo, cancelar y cinco acciones");

                for (int ancho : new int[]{950, 1100, 1250}) {
                    barra.setSize(ancho, 94);
                    barra.doLayout();
                    assertEquals(nuevo.getX(), listado.getX(),
                            "Nuevo e Imprimir listado comparten inicio");
                    assertEquals(nuevo.getWidth(), listado.getWidth(),
                            "Nuevo e Imprimir listado comparten ancho");
                    assertEquals(nuevo.getX() + nuevo.getWidth(),
                            listado.getX() + listado.getWidth(),
                            "Sus bordes derechos coinciden");
                    int altura = acciones[0].getHeight();
                    int fila = acciones[0].getY();
                    assertEquals(34, altura);
                    for (int i = 0; i < acciones.length; i++) {
                        JButton btn = acciones[i];
                        assertEquals(altura, btn.getHeight(),
                                "Todos los botones tienen la misma altura");
                        assertEquals(fila, btn.getY(),
                                "Todos los botones comparten línea de base");
                        assertTrue(btn.getWidth() >= btn.getPreferredSize().width);
                        if (i > 0) {
                            assertEquals(8, btn.getX() -
                                    (acciones[i - 1].getX() + acciones[i - 1].getWidth()),
                                    "La separación debe ser constante");
                        }
                    }
                    assertTrue(fila > nuevo.getY());
                }
                // Los botones no se mueven por habilitar/deshabilitar acciones.
                int xVer = ver.getX();
                int xImprimir = listado.getX();
                nuevo.setEnabled(false);
                ver.setEnabled(true);
                cancelar.setVisible(true);
                barra.doLayout();
                assertEquals(xVer, ver.getX());
                assertEquals(xImprimir, listado.getX());
            } catch (Throwable e) {
                fallo.set(e);
            }
        });
        if (fallo.get() != null) throw new AssertionError(fallo.get());
    }
}
