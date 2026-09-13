package veterinaria.vista.application;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import raven.toast.Notifications;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.JPAUtil;
import veterinaria.util.SesionUsuario;
import veterinaria.vista.application.form.FormLogin;
import veterinaria.vista.application.form.MainForm;
import veterinaria.vista.FormBienvenida;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.bootstrap.DataBootstrap;
import veterinaria.bootstrap.AgendaBootstrap;
import veterinaria.bootstrap.PeluqueriaBootstrap;
import veterinaria.config.PersistenceConfig;
import veterinaria.servicio.AuditoriaService;
import veterinaria.vista.application.UiActionAuditor;
import veterinaria.vista.table.TableGradientCell;

public class Application extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(Application.class.getName());
    private final AuditoriaService auditoriaService = new AuditoriaService();

    private static Application app;
    private MainForm mainForm;
    private final FormLogin loginForm;
    private static final SesionUsuario sesionUsuario = SesionUsuario.getInstancia();

    public Application() {
        initComponents();

        runCriticalBootstrapOrFail();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JPAUtil.close();
            }
        });
        //setSize(new Dimension(1366, 768));
        setExtendedState(JFrame.MAXIMIZED_BOTH);  // Maximizar ventana al abrir
        setLocationRelativeTo(null);
        loginForm = new FormLogin();
        setContentPane(loginForm);
        getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        Notifications.getInstance().setJFrame(this);
    }

    private void runCriticalBootstrapOrFail() {
        try {
            DataBootstrap.ensureBaseSecurityData();
            AgendaBootstrap.warmupAgendaSchema();
            PeluqueriaBootstrap.warmupPeluqueriaSchema();
            PeluqueriaBootstrap.ensureTiposBase();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[BOOTSTRAP] Fallo crítico de inicialización.", ex);
            throw new IllegalStateException("Falló la inicialización crítica del sistema. " + ex.getMessage(), ex);
        }
    }

    public static void login() {
        if (app != null) {
            app.initializeMainForm();
            FlatAnimatedLafChange.showSnapshot();
            app.setContentPane(app.mainForm);
            app.mainForm.applyComponentOrientation(app.getComponentOrientation());
            setSelectedMenu(0, 0);
            app.mainForm.hideMenu();
            SwingUtilities.updateComponentTreeUI(app.mainForm);

            // Auditoría centralizada de acciones internas UI (botones / menús)
            // Se instala una sola vez y registra eventos post-login.
            UiActionAuditor.install(app.auditoriaService);

            Application.showForm(new FormBienvenida());  // Mostrar el formulario de bienvenida
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    private void initializeMainForm() {
        mainForm = new MainForm();
    }

    public static void showForm(Component component) {
        if (app == null) {
            System.err.println("[UI] No se puede mostrar formulario: Application no inicializada.");
            return;
        }
        // Auto-recuperación: si por algún flujo el mainForm aún no fue creado,
        // lo inicializamos para evitar nulls y mensajes confusos en consola.
        if (app.mainForm == null) {
            app.initializeMainForm();
            app.setContentPane(app.mainForm);
            SwingUtilities.updateComponentTreeUI(app.mainForm);
        }
        component.applyComponentOrientation(app.getComponentOrientation());

        // Auditoría centralizada de ACCESO a formularios (post-login)
        // Objetivo: registrar todos los formularios efectivamente accedidos por usuarios.
        // Nota: AuditoriaService ya valida que exista sesión activa.
        try {
            String formName = component.getClass().getSimpleName();
            app.auditoriaService.registrar(
                    "ACCESS",
                    "Formulario",
                    null,
                    "UI",
                    "Acceso a " + formName,
                    AuditoriaService.RESULT_OK,
                    null,
                    null,
                    null
            );
        } catch (Exception ex) {
            // No bloquear navegación por auditoría
            System.err.println("[AUDITORIA][UI] No se pudo registrar acceso a formulario: " + ex.getMessage());
        }

        app.mainForm.showForm(component);
        component.revalidate();
        component.repaint();
    }

    public static void logout() {
        FlatAnimatedLafChange.showSnapshot();
        app.loginForm.clearFields();  // Limpiar los campos de inicio de sesión
        app.setContentPane(app.loginForm);
        app.loginForm.applyComponentOrientation(app.getComponentOrientation());
        SwingUtilities.updateComponentTreeUI(app.loginForm);
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        if (app != null) {
            app.auditoriaService.registrar(
                    "LOGOUT",
                    "Usuario",
                    (sesionUsuario.getUsuario() != null ? Long.valueOf(sesionUsuario.getUsuario().getIdUsuario()) : null),
                    "Application",
                    "Cierre de sesión",
                    AuditoriaService.RESULT_OK,
                    null,
                    null,
                    null
            );
        }
        System.out.println(app);
        sesionUsuario.cerrarSesion();
    }

    public static void setSelectedMenu(int index, int subIndex) {
        app.mainForm.setSelectedMenu(index, subIndex);
    }

    private static void configurarTextosDialogosEnEspañol() {
        UIManager.put("OptionPane.yesButtonText", "SI");
        UIManager.put("OptionPane.noButtonText", "NO");
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
    }

    public static void main(String args[]) {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        configurarTextosDialogosEnEspañol();
        FlatMacDarkLaf.setup();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            JPAUtil.close();
        }));

        java.awt.EventQueue.invokeLater(() -> {
            try {
                app = new Application();
                app.setVisible(true);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "No se pudo iniciar la aplicación.", ex);
                JOptionPane.showMessageDialog(
                        null,
                        """
                    No se pudo iniciar la aplicaci\u00f3n.
                    
                    Motivo: """ + ex.getMessage() + "\n"
                        + "Configuración BD: " + PersistenceConfig.describeSafe() + "\n\n"
                        + "Revise la conexión, el esquema y el bootstrap antes de presentar.",
                        "Inicio cancelado",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
    }

    public static SesionUsuario getSesionUsuario() {
        return sesionUsuario;
    }

    public static void setSesionUsuario(Usuario usuario) {
        sesionUsuario.iniciarSesion(usuario);
    }

    public static void actualizarEstadoUsuario(String estado) {
        sesionUsuario.setEstado(estado);
    }

    public static String consultarEstadoUsuario() {
        return sesionUsuario.getEstado();
    }

    public static String getNombreApellidoUsuarioLogeado() {
        return sesionUsuario.getNombreApellido();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 719, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 521, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public MainForm getMainForm() {
        return mainForm;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
