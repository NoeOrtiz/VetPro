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
    private final javax.swing.JTextField txtCodigo = new javax.swing.JTextField();
    private final javax.swing.JButton btnEnviarCodigo = new javax.swing.JButton("Enviar código");
    private boolean codigoSolicitado = false;

    public CambiarContrasenaDialog(java.awt.Window parent, Usuario usuario) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.usuario = usuario;
        initComponents();
        init();
    }

    private void init() {
        setTitle("Cambio obligatorio de contraseña");
        getContentPane().removeAll();
        setLayout(new MigLayout("fillx, wrap 1, insets 24, gapy 9", "[grow,fill]"));
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.JLabel info = new javax.swing.JLabel(
                "<html>Para continuar, verifique el correo asociado a su cuenta "
                + "y establezca una contraseña personal.</html>");
        add(info, "w 340!");
        add(new javax.swing.JLabel("Código de verificación (6 dígitos):"));
        txtCodigo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Código recibido por correo");
        add(txtCodigo, "h 30!");
        btnEnviarCodigo.addActionListener(e -> enviarCodigo());
        add(btnEnviarCodigo, "align right");
        add(new javax.swing.JLabel("Nueva contraseña:"));
        txtNuevaPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        txtConfirmarPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        add(txtNuevaPass, "h 30!");
        add(new javax.swing.JLabel("Confirmar contraseña:"));
        add(txtConfirmarPass, "h 30!");
        btnGuardar.setText("Verificar y actualizar");
        add(btnGuardar, "h 32!");
        javax.swing.JLabel advertencia = new javax.swing.JLabel(
                "<html><small>El código vence a los 10 minutos. "
                + "Sin verificarlo no podrá ingresar al sistema.</small></html>");
        add(advertencia);
        txtConfirmarPass.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) guardarNuevaContrasena();
            }
        });
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void enviarCodigo() {
        if (usuario == null || usuario.getIdUsuario() == null
                || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Este usuario no tiene un correo registrado. Contacte al administrador.",
                    "Verificación no disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }
        btnEnviarCodigo.setEnabled(false);
        btnEnviarCodigo.setText("Enviando...");
        new javax.swing.SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                String codigo = autenticacionService.generarCodigoCambioObligatorio(
                        usuario.getIdUsuario());
                if (codigo == null) return false;
                veterinaria.util.EmailUtil.enviarCodigo(usuario.getEmail(), codigo);
                return true;
            }
            @Override protected void done() {
                btnEnviarCodigo.setEnabled(true);
                btnEnviarCodigo.setText("Reenviar código");
                try {
                    codigoSolicitado = get();
                    if (codigoSolicitado) {
                        JOptionPane.showMessageDialog(CambiarContrasenaDialog.this,
                                "Se envió un código a su correo registrado. "
                                + "Tiene una validez de 10 minutos.",
                                "Código enviado", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(CambiarContrasenaDialog.this,
                                "No fue posible generar el código. Verifique su cuenta.",
                                "Verificación", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    codigoSolicitado = false;
                    JOptionPane.showMessageDialog(CambiarContrasenaDialog.this,
                            "No fue posible enviar el código: " + ex.getMessage(),
                            "Error de correo", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void guardarNuevaContrasena() {
        if (!codigoSolicitado || !txtCodigo.getText().trim().matches("\\d{6}")) {
            JOptionPane.showMessageDialog(this,
                    "Primero solicite e ingrese el código de 6 dígitos enviado a su correo.",
                    "Código obligatorio", JOptionPane.WARNING_MESSAGE);
            return;
        }
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

        // La nueva clave debe ser distinta de la clave temporal entregada
        // por el administrador, de lo contrario el cambio no cumple su objetivo.
        if (veterinaria.util.PasswordSecurityUtil.matches(nuevaPass, usuario.getContrasena())) {
            JOptionPane.showMessageDialog(this,
                    "La nueva contraseña debe ser diferente de la contraseña temporal.",
                    "Cambio obligatorio", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean exito = autenticacionService.confirmarCambioObligatorio(
                usuario.getIdUsuario(), txtCodigo.getText().trim(), nuevaPass);

        if (exito) {
            JOptionPane.showMessageDialog(this, "Contraseña actualizada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            contraseñaCambiada = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "El código es incorrecto, expiró o ya fue utilizado. Compruebe el código e inténtelo nuevamente.", "Error", JOptionPane.ERROR_MESSAGE);
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