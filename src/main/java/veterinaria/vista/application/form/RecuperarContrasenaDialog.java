package veterinaria.vista.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import net.miginfocom.swing.MigLayout;
import veterinaria.servicio.RecuperacionClaveService;
import veterinaria.util.EmailUtil;
import veterinaria.util.Validaciones;

public class RecuperarContrasenaDialog extends JDialog {

    private static final int ANCHO_MINIMO = 640;
    private static final int ALTO_MINIMO = 640;

    private final RecuperacionClaveService recuperacionService = new RecuperacionClaveService();
    private final Validaciones validaciones = new Validaciones();

    // Componentes Paso 1
    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtDni = new JTextField();

    // Componentes Paso 2
    private final JTextField txtCodigo = new JTextField();
    private final JPasswordField txtNuevaContrasena = new JPasswordField();
    private final JPasswordField txtRepetirContrasena = new JPasswordField();

    

    private boolean codigoEnviado = false;
    private JButton btnAccion;
    private JPanel panelFormulario;
    private JLabel lblAyuda;

    public RecuperarContrasenaDialog(Window owner, String usuarioInicial) {
        super(owner instanceof Frame ? (Frame) owner : null, "Recuperar Contraseña", ModalityType.APPLICATION_MODAL);
        initComponents(usuarioInicial);
    }

    private void initComponents(String usuarioInicial) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panelPrincipal = new JPanel(new BorderLayout(0, 16));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 28, 18, 28));

        // Cabecera más compacta para liberar espacio vertical abajo
        JPanel panelCabecera = new JPanel(new MigLayout("insets 0, alignx center, wrap 1, gapy 2", "[center]", "[]0[]2[]"));

        JLabel lblLogo = new JLabel();
        try {
            ImageIcon iconoOriginal = new ImageIcon(getClass().getResource("/veterinaria/icon/png/login.png"));
            Image imagenEscalada = iconoOriginal.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(imagenEscalada));
        } catch (Exception e) {
            // Ignorar si falla la carga
        }

        JLabel lblTitulo = new JLabel("Restablecer acceso");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");

        lblAyuda = new JLabel("<html><div style='text-align: center;'>Complete los datos registrados para recibir un código de verificación en su correo.</div></html>");
        lblAyuda.putClientProperty(FlatClientProperties.STYLE, "foreground:lighten(@foreground,40%); font:$regular.font");

        panelCabecera.add(lblLogo);
        panelCabecera.add(lblTitulo, "gapy 2"); // Espacio mínimo entre el logo y el título
        panelCabecera.add(lblAyuda, "gapy 2");  // Espacio mínimo entre el título y el texto explicativo

        panelFormulario = new JPanel(new MigLayout(
                "fillx, insets 0, hidemode 3, gapx 14, gapy 10",
                "[120!][grow,fill]",
                ""
        ));

        configurarCampo(txtUsuario, "Usuario");
        configurarCampo(txtEmail, "E-mail registrado");
        configurarCampo(txtDni, "DNI");

        if (usuarioInicial != null) {
            txtUsuario.setText(usuarioInicial.trim());
        }

        cargarPaso1();

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(0, 20, 14, 20));

        JButton btnCancelar = new JButton("Cancelar");
        btnAccion = new JButton("Enviar Código");

        String estiloBotones = "borderWidth:1; focusWidth:0; innerFocusWidth:0";
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, estiloBotones);
        btnAccion.putClientProperty(FlatClientProperties.STYLE, estiloBotones);

        btnCancelar.addActionListener(e -> dispose());
        btnAccion.addActionListener(e -> {
            if (!codigoEnviado) {
                enviarCodigoRecuperacion();
            } else {
                procesarRecuperacionFinal();
            }
        });

        panelBotones.add(btnCancelar);
        panelBotones.add(btnAccion);

        panelPrincipal.add(panelCabecera, BorderLayout.NORTH);
        panelPrincipal.add(panelFormulario, BorderLayout.CENTER);

        add(panelPrincipal, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(ANCHO_MINIMO, ALTO_MINIMO));
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
        SwingUtilities.invokeLater(() -> txtUsuario.requestFocusInWindow());
    }

    private void cargarPaso1() {
        panelFormulario.removeAll();

        panelFormulario.add(new JLabel("Usuario"));
        panelFormulario.add(txtUsuario, "wrap, h 34!");

        panelFormulario.add(new JLabel("E-mail"));
        panelFormulario.add(txtEmail, "wrap, h 34!");

        panelFormulario.add(new JLabel("DNI"));
        panelFormulario.add(txtDni, "wrap, h 34!");

        panelFormulario.revalidate();
        panelFormulario.repaint();
    }

    private void cargarPaso2() {
        panelFormulario.removeAll();

        configurarCampo(txtCodigo, "Código de 6 dígitos");
        configurarCampo(txtNuevaContrasena, "Nueva contraseña");
        configurarCampo(txtRepetirContrasena, "Repetir nueva contraseña");

        txtNuevaContrasena.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");
        txtRepetirContrasena.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; showCapsLock:true");

        // Escuchador en tiempo real para actualizar el Tooltip y forzar su redibujado
        txtNuevaContrasena.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void actualizarTooltip() {
                String pass = new String(txtNuevaContrasena.getPassword());

                boolean largo = pass.length() >= 8;
                boolean mayus = pass.matches(".*[A-Z].*");
                boolean num = pass.matches(".*[0-9].*");
                boolean especial = pass.matches(".*[@$!%*?&._\\-].*");

                String htmlTooltip = "<html><b>Requisitos de seguridad:</b><br>"
                        + (largo ? "<font color='#4CAF50'>✓ Mínimo 8 caracteres</font>" : "<font color='#808080'>• Mínimo 8 caracteres</font>") + "<br>"
                        + (mayus ? "<font color='#4CAF50'>✓ Al menos una letra mayúscula</font>" : "<font color='#808080'>• Al menos una letra mayúscula</font>") + "<br>"
                        + (num ? "<font color='#4CAF50'>✓ Al menos un número</font>" : "<font color='#808080'>• Al menos un número</font>") + "<br>"
                        + (especial ? "<font color='#4CAF50'>✓ Carácter especial (@$!%*?&._-)</font>" : "<font color='#808080'>• Carácter especial (@$!%*?&._-)</font>")
                        + "</html>";

                txtNuevaContrasena.setToolTipText(htmlTooltip);

                // Truco de Swing: Si el tooltip ya está abierto en pantalla, lo actualizamos al instante
                javax.swing.ToolTipManager sharedInstance = javax.swing.ToolTipManager.sharedInstance();
                if (txtNuevaContrasena.getMousePosition() != null) {
                    sharedInstance.setEnabled(false);
                    sharedInstance.setEnabled(true);
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                actualizarTooltip();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                actualizarTooltip();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                actualizarTooltip();
            }
        });

        // Tooltip inicial por defecto
        txtNuevaContrasena.setToolTipText("<html><b>Requisitos de seguridad:</b><br>• Mínimo 8 caracteres<br>• Al menos una letra mayúscula<br>• Al menos un número<br>• Carácter especial (@$!%*?&._-)</html>");

        // Agregamos los campos al formulario de manera limpia
        panelFormulario.add(new JLabel("Código recibido"));
        panelFormulario.add(txtCodigo, "wrap, h 34!");

        panelFormulario.add(new JLabel("Nueva clave"));
        panelFormulario.add(txtNuevaContrasena, "wrap, h 34!");

        panelFormulario.add(new JLabel("Repetir clave"));
        panelFormulario.add(txtRepetirContrasena, "wrap, h 34!");

        panelFormulario.revalidate();
        panelFormulario.repaint();
        pack();
        setLocationRelativeTo(getOwner());
        SwingUtilities.invokeLater(() -> txtCodigo.requestFocusInWindow());
    }

    private void configurarCampo(JTextField campo, String placeholder) {
        campo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        campo.setPreferredSize(new Dimension(380, 34));
    }

    private void enviarCodigoRecuperacion() {
        String usuario = txtUsuario.getText().trim();
        String email = txtEmail.getText().trim();
        String dni = txtDni.getText().trim();

        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el usuario.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtUsuario.requestFocusInWindow();
            return;
        }
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el e-mail registrado.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocusInWindow();
            return;
        }
        if (!validaciones.validarCorreoValido(email)) {
            JOptionPane.showMessageDialog(this, "El e-mail ingresado no tiene un formato válido.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocusInWindow();
            return;
        }
        if (dni.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el DNI registrado.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtDni.requestFocusInWindow();
            return;
        }

        Window ventanaAnfitriona = SwingUtilities.getWindowAncestor(this);

        JDialog dialogueCarga = new JDialog(ventanaAnfitriona, "Procesando", Dialog.ModalityType.APPLICATION_MODAL);
        dialogueCarga.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogueCarga.setSize(340, 115);
        dialogueCarga.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "border: 16,16,16,16;"
                + "background: $Panel.background"
        );

        Icon iconoCarga = UIManager.getIcon("OptionPane.informationIcon");
        JLabel lblIcono = new JLabel(iconoCarga);

        JLabel lblMensaje = new JLabel("Enviando código, por favor espere...", JLabel.LEFT);
        lblMensaje.setFont(lblMensaje.getFont().deriveFont(Font.BOLD, 12f));

        JPanel panelInfo = new JPanel(new BorderLayout(10, 10));
        panelInfo.setOpaque(false);
        panelInfo.add(lblIcono, BorderLayout.WEST);
        panelInfo.add(lblMensaje, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        panel.add(panelInfo, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        dialogueCarga.add(panel);

        SwingWorker<String, Void> trabajador = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return recuperacionService.generarCodigoRecuperacion(usuario, email, dni);
            }

            @Override
            protected void done() {
                dialogueCarga.dispose();

                try {
                    String codigoPlano = get();

                    if (codigoPlano != null) {
                        try {
                            EmailUtil.enviarCodigo(email, codigoPlano);

                            codigoEnviado = true;
                            lblAyuda.setText("<html><div style='text-align: center;'>Ingrese el código de 6 dígitos enviado a su correo y su nueva contraseña.</div></html>");
                            btnAccion.setText("Actualizar contraseña");
                            cargarPaso2();

                            JOptionPane.showMessageDialog(RecuperarContrasenaDialog.this,
                                    "¡Código enviado con éxito! Revise su bandeja de entrada.",
                                    "Correo enviado", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            new veterinaria.servicio.AuditoriaService().registrarSinSesion(
                                    "PASSWORD_RESET_FAIL", "Usuario", null, "RecuperarContrasenaDialog",
                                    "Error al enviar correo SMTP para usuario: " + usuario,
                                    veterinaria.servicio.AuditoriaService.RESULT_ERROR, null, null, ex.getMessage()
                            );
                            JOptionPane.showMessageDialog(RecuperarContrasenaDialog.this,
                                    "Error al enviar el correo: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(RecuperarContrasenaDialog.this,
                                "No se encontró un usuario activo que coincida con los datos ingresados.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (java.util.concurrent.ExecutionException ee) {
                    System.err.println("=== [ERROR] Falló la generación del código de recuperación ===");
                    ee.getCause().printStackTrace();
                    JOptionPane.showMessageDialog(RecuperarContrasenaDialog.this,
                            "Error interno:\n" + ee.getCause().getMessage(),
                            "Error de Ejecución", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    System.err.println("=== [ERROR] Error general en recuperación ===");
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(RecuperarContrasenaDialog.this,
                            "Error al procesar la solicitud: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        trabajador.execute();
        dialogueCarga.setVisible(true);
    }

    private void procesarRecuperacionFinal() {
        String usuario = txtUsuario.getText().trim();
        String email = txtEmail.getText().trim();
        String dni = txtDni.getText().trim();
        String codigo = txtCodigo.getText().trim();
        String nuevaContrasena = new String(txtNuevaContrasena.getPassword());
        String repetirContrasena = new String(txtRepetirContrasena.getPassword());

        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el código de verificación.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtCodigo.requestFocusInWindow();
            return;
        }
        if (nuevaContrasena.isBlank()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar una nueva contraseña.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtNuevaContrasena.requestFocusInWindow();
            return;
        }

        if (!validaciones.validarComplejidadContrasena(nuevaContrasena)) {
            JOptionPane.showMessageDialog(this,
                    "<html>La contraseña no cumple con los requisitos de seguridad:<br>"
                    + "• Mínimo 8 caracteres.<br>"
                    + "• Al menos una letra mayúscula.<br>"
                    + "• Al menos un número.<br>"
                    + "• Al menos un carácter especial (@$!%*?&._-).</html>",
                    "Validación de Seguridad",
                    JOptionPane.WARNING_MESSAGE);
            txtNuevaContrasena.requestFocusInWindow();
            return;
        }

        if (!nuevaContrasena.equals(repetirContrasena)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtRepetirContrasena.requestFocusInWindow();
            return;
        }

        boolean actualizado = recuperacionService.restablecerConCodigo(usuario, email, dni, codigo, nuevaContrasena);

        if (actualizado) {
            JOptionPane.showMessageDialog(this,
                    "La contraseña fue actualizada correctamente. Ya puede ingresar al sistema.",
                    "Recuperación exitosa",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "El código ingresado es incorrecto o ha expirado.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            txtCodigo.requestFocusInWindow();
        }
    }
}
