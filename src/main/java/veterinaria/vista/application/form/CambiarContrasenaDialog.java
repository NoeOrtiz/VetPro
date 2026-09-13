package veterinaria.vista.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import net.miginfocom.swing.MigLayout;
import veterinaria.entidad.Usuario;
import veterinaria.servicio.AutenticacionService;
import veterinaria.util.Validaciones;

public class CambiarContrasenaDialog extends javax.swing.JDialog {

    private final Usuario usuario;
    private final AutenticacionService autenticacionService = new AutenticacionService();
    private final Validaciones validaciones = new Validaciones();
    private boolean contraseñaCambiada = false;

    public CambiarContrasenaDialog(java.awt.Window parent, Usuario usuario) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.usuario = usuario;
        initComponents();
        init();
    }

    private void init() {
        setTitle("Actualización obligatoria de contraseña");
        setLayout(new MigLayout("wrap 1, al center center, fillx", "[250]"));
        setLocationRelativeTo(getOwner());
        setResizable(false);

        // Estilos visuales con FlatLaf para los campos de contraseña
        txtNuevaPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        txtConfirmarPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        txtNuevaPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nueva contraseña");
        txtConfirmarPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Confirmar contraseña");

        // Evento Enter para guardar directo
        txtConfirmarPass.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    guardarNuevaContrasena();
                }
            }
        });
    }

    private void guardarNuevaContrasena() {
        String nuevaPass = new String(txtNuevaPass.getPassword()).trim();
        String confirmarPass = new String(txtConfirmarPass.getPassword()).trim();

        if (nuevaPass.isEmpty() || confirmarPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete ambos campos.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!nuevaPass.equals(confirmarPass)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validar complejidad con tu clase Validaciones
        if (!validaciones.validarComplejidadContrasena(nuevaPass)) {
            JOptionPane.showMessageDialog(this, 
                "La contraseña no cumple con los requisitos de seguridad\n(Debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial).", 
                "Seguridad insuficiente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Llamamos al servicio para actualizar la contraseña, bajar el primer login y actualizar la fecha
        boolean exito = autenticacionService.actualizarContrasenaObligatoria(usuario.getIdUsuario(), nuevaPass);

        if (exito) {
            JOptionPane.showMessageDialog(this, "Contraseña actualizada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            contraseñaCambiada = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar la contraseña en el sistema.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isContraseñaCambiada() {
        return contraseñaCambiada;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        lblMensaje = new javax.swing.JLabel();
        lblNueva = new javax.swing.JLabel();
        txtNuevaPass = new javax.swing.JPasswordField();
        lblConfirmar = new javax.swing.JLabel();
        txtConfirmarPass = new javax.swing.JPasswordField();
        btnGuardar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        lblMensaje.setText("Por seguridad, debe actualizar su contraseña.");
        lblNueva.setText("Nueva Contraseña:");
        lblConfirmar.setText("Confirmar Contraseña:");

        btnGuardar.setText("Guardar y Continuar");
        btnGuardar.addActionListener(evt -> guardarNuevaContrasena());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblMensaje, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblNueva)
                    .addComponent(txtNuevaPass)
                    .addComponent(lblConfirmar)
                    .addComponent(txtConfirmarPass)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblMensaje)
                .addGap(18, 18, 18)
                .addComponent(lblNueva)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNuevaPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(lblConfirmar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtConfirmarPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24)
                .addComponent(btnGuardar)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        pack();
    }

    private javax.swing.JButton btnGuardar;
    private javax.swing.JLabel lblConfirmar;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JLabel lblNueva;
    private javax.swing.JPasswordField txtConfirmarPass;
    private javax.swing.JPasswordField txtNuevaPass;
}