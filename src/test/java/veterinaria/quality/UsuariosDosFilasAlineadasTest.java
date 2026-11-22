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

/** Comprueba coordenadas reales del layout, sin instanciar el módulo ni consultar MySQL. */
class UsuariosDosFilasAlineadasTest {

    @Test
    void nuevoEImprimirCompartenColumnaEnDosAnchosYConSeleccion() throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                filtros.add(new JLabel("Buscar usuario:"));
                JTextField buscar = new JTextField(32);
                filtros.add(buscar);
                filtros.add(new JLabel("Estado:"));

                JPanel grupoAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                for (String titulo : new String[]{
                        "Ver ficha", "Editar", "Desactivar", "Imprimir ficha"}) {
                    grupoAcciones.add(new JButton(titulo));
                }
                JButton nuevo = new JButton("Nuevo usuario");
                JButton imprimir = new JButton("Imprimir listado");
                Method metodo = FormUsuario.class.getDeclaredMethod(
                        "crearBarraUsuariosAlineada",
                        JPanel.class, JButton.class, JPanel.class, JButton.class);
                metodo.setAccessible(true);
                JPanel barra = (JPanel) metodo.invoke(null,
                        filtros, nuevo, grupoAcciones, imprimir);
                assertEquals(4, barra.getComponentCount());

                for (int ancho : new int[]{950, 1100, 1250}) {
                    barra.setSize(ancho, 90);
                    barra.doLayout();
                    assertEquals(nuevo.getX(), imprimir.getX(),
                            "Deben empezar exactamente en la misma coordenada X");
                    assertEquals(nuevo.getWidth(), imprimir.getWidth(),
                            "Ambos botones deben tener idéntico ancho");
                    assertEquals(nuevo.getX() + nuevo.getWidth(),
                            imprimir.getX() + imprimir.getWidth(),
                            "Los bordes derechos deben coincidir");
                    assertTrue(imprimir.getY() > nuevo.getY(),
                            "Imprimir listado debe estar en la segunda fila");
                    assertEquals(34, nuevo.getHeight());
                    assertEquals(34, imprimir.getHeight());
                }

                nuevo.setEnabled(false); // usuario seleccionado
                imprimir.setEnabled(true);
                barra.doLayout();
                assertEquals(nuevo.getX(), imprimir.getX(),
                        "Los botones no deben moverse al seleccionar un registro");
            } catch (Throwable e) {
                fallo.set(e);
            }
        });
        if (fallo.get() != null) throw new AssertionError(fallo.get());
    }
}
