package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
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

/** Comprueba la distribución real basada en la UI del compañero, sin MySQL. */
class UsuariosDosFilasAlineadasTest {

    private static void distribuir(Container contenedor) {
        contenedor.doLayout();
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof Container sub) distribuir(sub);
        }
    }

    @Test
    void cincoAccionesConservanTamanosYAlineacionAlSeleccionar() throws Exception {
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
                assertEquals(3, barra.getComponentCount(),
                        "Dos filas independientes y una separación vertical");
                JPanel superior = (JPanel) barra.getComponent(0);
                JPanel inferior = (JPanel) barra.getComponent(2);
                assertEquals(5, inferior.getComponentCount(),
                        "Solo las cinco acciones originales, sin un segundo botón Limpiar");
                assertFalse(java.util.Arrays.asList(inferior.getComponents()).contains(cancelar),
                        "Cancelar selección no debe crear un control visual duplicado");

                for (int ancho : new int[]{950, 1100, 1250}) {
                    barra.setSize(ancho, 80);
                    distribuir(barra);
                    assertEquals(150, nuevo.getWidth());
                    assertEquals(150, listado.getWidth());
                    assertEquals(34, nuevo.getHeight());
                    for (int i = 0; i < acciones.length; i++) {
                        JButton boton = acciones[i];
                        assertEquals(34, boton.getHeight());
                        assertEquals(boton.getPreferredSize().width, boton.getWidth());
                        if (i > 0) {
                            assertEquals(8, boton.getX()
                                    - (acciones[i - 1].getX() + acciones[i - 1].getWidth()));
                        }
                    }
                    // Ambos botones terminan junto al borde derecho de su fila.
                    int derechaNuevo = nuevo.getX() + nuevo.getWidth();
                    int derechaListado = listado.getX() + listado.getWidth();
                    assertTrue(Math.abs(derechaNuevo - derechaListado) <= 8);
                    assertTrue(inferior.getY() > superior.getY());
                }
                int xVer = ver.getX();
                int xImprimir = listado.getX();
                nuevo.setEnabled(false);
                ver.setEnabled(true);
                cancelar.setEnabled(true);
                distribuir(barra);
                assertEquals(xVer, ver.getX());
                assertEquals(xImprimir, listado.getX());
            } catch (Throwable e) {
                fallo.set(e);
            }
        });
        if (fallo.get() != null) throw new AssertionError(fallo.get());
    }
}
