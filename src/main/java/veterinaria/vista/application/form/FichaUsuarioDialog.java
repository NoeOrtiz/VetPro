package veterinaria.vista.application.form;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

public final class FichaUsuarioDialog extends JDialog {
    public record Datos(String nombre, String apellido, String dni, String telefono,
            String direccion, String rol, String usuario, String correo, String estado) {}

    public FichaUsuarioDialog(Window owner, Datos datos) {
        super(owner, "Ficha de usuario", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        JPanel encabezado = new JPanel(new BorderLayout(16, 0));
        encabezado.setOpaque(false);
        String nombreCompleto = (datos.nombre() + " " + datos.apellido()).trim();
        JLabel nombre = texto(nombreCompleto.isBlank() ? datos.usuario() : nombreCompleto);
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 20f));
        encabezado.add(nombre, BorderLayout.CENTER);

        JLabel estado = texto(datos.estado());
        String colorEstado = "Activo".equalsIgnoreCase(datos.estado()) ? "ok" : "error";
        estado.setOpaque(true);
        estado.setBackground(color("App.status." + colorEstado + ".background", contenido.getBackground()));
        estado.setForeground(color("App.status." + colorEstado + ".foreground", estado.getForeground()));
        estado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        JPanel ladoEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        ladoEstado.setOpaque(false);
        ladoEstado.add(estado);
        encabezado.add(ladoEstado, BorderLayout.EAST);
        contenido.add(encabezado, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setBackground(color("App.surface.background", contenido.getBackground()));
        campos.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        agregarCampo(campos, 0, "Usuario", datos.usuario());
        agregarCampo(campos, 1, "Rol", datos.rol());
        agregarCampo(campos, 2, "DNI", datos.dni());
        agregarCampo(campos, 3, "Teléfono", datos.telefono());
        agregarCampo(campos, 4, "Dirección", datos.direccion());
        agregarCampo(campos, 5, "Correo electrónico", datos.correo());
        contenido.add(campos, BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(evento -> dispose());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        acciones.add(cerrar);
        contenido.add(acciones, BorderLayout.SOUTH);
        setContentPane(contenido);
        getRootPane().setDefaultButton(cerrar);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cerrar");
        getRootPane().getActionMap().put("cerrar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent evento) {
                dispose();
            }
        });

        pack();
        setSize(Math.max(460, getWidth()), getHeight());
        setLocationRelativeTo(owner);
    }

    private static void agregarCampo(JPanel panel, int fila, String titulo, String valor) {
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setForeground(color("App.muted.foreground", etiqueta.getForeground()));
        GridBagConstraints posicion = new GridBagConstraints();
        posicion.gridx = 0;
        posicion.gridy = fila;
        posicion.anchor = GridBagConstraints.WEST;
        posicion.insets = new Insets(5, 0, 5, 18);
        panel.add(etiqueta, posicion);

        JLabel dato = texto(valor);
        posicion.gridx = 1;
        posicion.weightx = 1;
        posicion.insets = new Insets(5, 0, 5, 0);
        panel.add(dato, posicion);
    }

    private static JLabel texto(String valor) {
        JLabel etiqueta = new JLabel();
        etiqueta.putClientProperty("html.disable", Boolean.TRUE);
        etiqueta.setText(valor.isBlank() ? "—" : valor);
        return etiqueta;
    }

    private static Color color(String clave, Color alternativo) {
        Color color = UIManager.getColor(clave);
        return color == null ? alternativo : color;
    }
}
