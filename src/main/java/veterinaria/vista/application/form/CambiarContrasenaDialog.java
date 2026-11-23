package veterinaria.vista.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import net.miginfocom.swing.MigLayout;
import veterinaria.entidad.Usuario;
import veterinaria.servicio.AutenticacionService;
import veterinaria.util.Validaciones;
import veterinaria.util.ui.EstiloHibrido;

public class CambiarContrasenaDialog extends javax.swing.JDialog {

    private final Usuario usuario;
    private final AutenticacionService autenticacionService = new AutenticacionService();
    private final Validaciones validaciones = new Validaciones();
    private boolean contraseñaCambiada = false;
    private final JTextField txtCodigo = new JTextField();
    private final JPasswordField txtNuevaPass = new JPasswordField();
    private final JPasswordField txtConfirmarPass = new JPasswordField();
    private final JButton btnEnviarCodigo = new JButton("Enviar código");
    private final JButton btnGuardar = new JButton("Verificar y actualizar");
    private boolean codigoSolicitado = false;

    public CambiarContrasenaDialog(java.awt.Window parent, Usuario usuario) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.usuario = usuario;
        init();
    }

    private void init() {
        setTitle("Cambio obligatorio de contraseña");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setLayout(new MigLayout("fillx, wrap 1, insets 22 24 20 24, gapy 0",
                "[380lp,grow,fill]"));

        JLabel titulo = new JLabel("Creá tu contraseña personal");
        titulo.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");
        add(titulo, "gapbottom 6lp");

        JLabel info = new JLabel("<html>Solicitá el código que enviaremos al correo de tu cuenta.<br>"
                + "Ingresalo para verificar tu identidad y continuar.</html>");
        add(info, "gapbottom 12lp");

        EstiloHibrido.botonesSecundarios(btnEnviarCodigo);
        btnEnviarCodigo.addActionListener(e -> enviarCodigo());
        add(btnEnviarCodigo, "align right, h 36lp!, gapbottom 14lp");

        txtCodigo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Código recibido por correo");
        agregarCampo("Código de verificación (6 dígitos)", txtCodigo);
        txtNuevaPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        txtConfirmarPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        agregarCampo("Nueva contraseña", txtNuevaPass);
        agregarCampo("Confirmar contraseña", txtConfirmarPass);

        JLabel advertencia = new JLabel("<html>El código vence a los 10 minutos.<br>"
                + "Completá la verificación para ingresar al sistema.</html>");
        advertencia.putClientProperty(FlatClientProperties.STYLE_CLASS, "ayuda");
        add(advertencia, "gapbottom 16lp");

        EstiloHibrido.botonesPrimarios(btnGuardar);
        btnGuardar.addActionListener(e -> guardarNuevaContrasena());
        txtConfirmarPass.addActionListener(e -> guardarNuevaContrasena());
        add(btnGuardar, "align right, h 36lp!");

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void agregarCampo(String titulo, JTextField campo) {
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setLabelFor(campo);
        add(etiqueta, "gapbottom 5lp");
        add(campo, "h 36lp!, gapbottom 12lp");
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
}
