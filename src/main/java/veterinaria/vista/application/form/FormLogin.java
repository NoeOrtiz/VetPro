package veterinaria.vista.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import veterinaria.controlador.UsuarioControlador;
import veterinaria.entidad.Usuario;
import veterinaria.vista.application.Application;

public class FormLogin extends javax.swing.JPanel {

    private final UsuarioControlador operarUsuario = new UsuarioControlador();
   

    public FormLogin() {
        initComponents();
        init();
    }

    public void clearFields() {
        txtUser.setText("");
        txtPass.setText("");
    }

    private void verificarLogin() {
        String usuarioStr = txtUser.getText();
        String contrasena = new String(txtPass.getPassword());

        try {
            boolean loginExitoso = operarUsuario.iniciarSesionUsuario(usuarioStr, contrasena);

            if (loginExitoso) {
                // Obtenemos el objeto usuario completo que acaba de iniciar sesión para chequear su estado
                Usuario usuarioLogueado = operarUsuario.buscarPorUsuario(usuarioStr);

                if (usuarioLogueado != null && usuarioLogueado.isPrimerLogin()) {
                    // 1. Informamos al usuario que debe cambiar su contraseña por seguridad
                    JOptionPane.showMessageDialog(this,
                            "Es su primer inicio de sesión. Debe cambiar su contraseña para continuar.",
                            "Cambio Obligatorio",
                            JOptionPane.INFORMATION_MESSAGE);

                    // 2. Abrimos el diálogo de recuperación / cambio obligatorio
                    java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
                    RecuperarContrasenaDialog dialog = new RecuperarContrasenaDialog(parent, usuarioStr);
                    dialog.setLocationRelativeTo(this);
                    dialog.setVisible(true);

                    return; // Frenamos el acceso directo hasta que complete el flujo
                }

                // Si no es su primer login, ingresa normalmente al sistema
                Application.login();
            } else {
                JOptionPane.showMessageDialog(this, "Credenciales Incorrectas", "Error", JOptionPane.ERROR_MESSAGE);

                // Buenas prácticas de seguridad: Limpiar ambos y volver al inicio
                txtUser.setText("");
                txtPass.setText("");
                txtUser.requestFocusInWindow();
            }

        } catch (SecurityException e) {
            // Capturamos el bloqueo temporal o de intentos y mostramos el cartel prolijo
            JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Acceso Denegado",
                    JOptionPane.WARNING_MESSAGE);

            // >>> LIMPIEZA EN CASO DE BLOQUEO O EXCEPCIÓN DE SEGURIDAD <<<
            txtPass.setText("");          // Borra la contraseña
            txtPass.requestFocusInWindow(); // Devuelve el foco
        }
    }

    private void init() {
        setLayout(new MigLayout("al center center"));

        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");

        txtPass.putClientProperty(FlatClientProperties.STYLE, ""
                + "showRevealButton:true;"
                + "showCapsLock:true");
        cmdLogin.putClientProperty(FlatClientProperties.STYLE, ""
                + "borderWidth:0;"
                + "focusWidth:0");
        txtUser.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Usuario");
        txtPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña");

        // --- Configuración de la etiqueta ---
        lbOlvidastConraseña.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbOlvidastConraseña.setToolTipText("Recuperar contraseña");

        // -> CAMBIA ESTA SECCIÓN: Ahora busca tus colores dinámicos de FlatLaf
        java.awt.Color colorBase = lbOlvidastConraseña.getForeground();
        java.awt.Color colorHover = javax.swing.UIManager.getColor("App.accent.default");

        if (colorHover == null) {
            colorHover = javax.swing.UIManager.getColor("Menu.foreground");
        }

        lbOlvidastConraseña.addMouseListener(new veterinaria.util.ui.HoverEfectoLabel(colorBase, colorHover));

        // Tu listener original de clics se queda exactamente igual, no se toca
        lbOlvidastConraseña.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirRecuperacionContrasena();
            }
        });

        SwingUtilities.invokeLater(() -> txtUser.requestFocusInWindow());
    }

    private void abrirRecuperacionContrasena() {
        java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
        RecuperarContrasenaDialog dialog = new RecuperarContrasenaDialog(parent, txtUser.getText());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelLogin1 = new veterinaria.vista.application.form.PanelLogin();
        lbTitle = new javax.swing.JLabel();
        lbUser = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        lbPass = new javax.swing.JLabel();
        txtPass = new javax.swing.JPasswordField();
        cmdLogin = new javax.swing.JButton();
        lbOlvidastConraseña = new javax.swing.JLabel();

        panelLogin1.setAlignmentX(0.0F);
        panelLogin1.setAlignmentY(0.0F);

        lbTitle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/login.png"))); // NOI18N

        lbUser.setText("Usuario");

        txtUser.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtUserKeyPressed(evt);
            }
        });

        lbPass.setText("Contraseña");

        txtPass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPassActionPerformed(evt);
            }
        });

        cmdLogin.setText("Ingresar");
        cmdLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdLoginActionPerformed(evt);
            }
        });

        lbOlvidastConraseña.setText("¿Olvidaste la Contraseña?");

        javax.swing.GroupLayout panelLogin1Layout = new javax.swing.GroupLayout(panelLogin1);
        panelLogin1.setLayout(panelLogin1Layout);
        panelLogin1Layout.setHorizontalGroup(
            panelLogin1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLogin1Layout.createSequentialGroup()
                .addGroup(panelLogin1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLogin1Layout.createSequentialGroup()
                        .addGap(134, 134, 134)
                        .addComponent(lbTitle))
                    .addGroup(panelLogin1Layout.createSequentialGroup()
                        .addGap(119, 119, 119)
                        .addComponent(lbOlvidastConraseña)))
                .addContainerGap(132, Short.MAX_VALUE))
            .addGroup(panelLogin1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelLogin1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addGroup(panelLogin1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lbUser, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbPass, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmdLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        panelLogin1Layout.setVerticalGroup(
            panelLogin1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLogin1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 176, Short.MAX_VALUE)
                .addComponent(lbOlvidastConraseña)
                .addGap(91, 91, 91))
            .addGroup(panelLogin1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelLogin1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(lbUser)
                    .addGap(7, 7, 7)
                    .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)
                    .addComponent(lbPass)
                    .addGap(7, 7, 7)
                    .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(30, 30, 30)
                    .addComponent(cmdLogin)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(175, 175, 175)
                .addComponent(panelLogin1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(175, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(46, Short.MAX_VALUE)
                .addComponent(panelLogin1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cmdLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdLoginActionPerformed
        verificarLogin();
    }//GEN-LAST:event_cmdLoginActionPerformed

    private void txtUserKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtUserKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!txtUser.getText().trim().isEmpty()) {
                txtPass.requestFocus();
            } else {
                JOptionPane.showMessageDialog(this, "Debe ingresar un nombre de usuario.");
            }
        }

    }//GEN-LAST:event_txtUserKeyPressed

    private void txtPassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPassActionPerformed
        verificarLogin();
    }//GEN-LAST:event_txtPassActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdLogin;
    private javax.swing.JLabel lbOlvidastConraseña;
    private javax.swing.JLabel lbPass;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JLabel lbUser;
    private javax.swing.JPanel panelLogin1;
    private javax.swing.JPasswordField txtPass;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
}
