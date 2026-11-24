package veterinaria.vista.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Graphics;
import java.awt.font.TextLayout;
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;
import java.util.Locale;
import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import veterinaria.servicio.AuditoriaService;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Usuario;
import veterinaria.util.SesionUsuario;
import veterinaria.vista.FormAccesoRestringido;
import veterinaria.vista.FormAuditoria;
import veterinaria.vista.application.Application;
import veterinaria.vista.menu.Menu;
import veterinaria.vista.menu.MenuAction;
import veterinaria.vista.FormCajaRegistradora;
import veterinaria.vista.FormCliente;
import veterinaria.vista.ClassConfig;
import veterinaria.vista.FormCuentaCorrienteMovimientos;
import veterinaria.vista.FormGestionCuentasCorrientes;
import veterinaria.vista.FormHistoriaClinica;
import veterinaria.vista.FormHistorialTurnos;
import veterinaria.vista.FormHospitalizaciones;
import veterinaria.vista.FormInformesCajaMovimientos;
import veterinaria.vista.FormLaboratorio;
import veterinaria.vista.FormMascota;
import veterinaria.vista.FormOrdenesCompra;
import veterinaria.vista.FormProcedimientos;
import veterinaria.vista.FormProducto;
import veterinaria.vista.FormProveedores;
import veterinaria.vista.FormRecibirPedido;
import veterinaria.vista.FormStockProducto;
import veterinaria.vista.FormTurnosPeluqueria;
import veterinaria.vista.PanelTiposCitaPeluqueria;
import veterinaria.vista.FormUsuario;
import veterinaria.vista.FormConsulta;
import veterinaria.vista.FormBienvenida;
import veterinaria.util.ui.TableLayoutSupport;
import veterinaria.vista.FormBackUp;

public class MainForm extends JLayeredPane {

    private final AuditoriaService auditoriaService = new AuditoriaService();
    private SesionUsuario sesion = Application.getSesionUsuario();
    Usuario usuario = Application.getSesionUsuario().getUsuario();
    Rol rolUsuario = Application.getSesionUsuario().getRol();

    public MainForm() {
        init();
        // Inicialización del panel que contendrá los formularios
        // El contenedor principal se inicializa en init().
    }

    private void init() {
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new MainFormLayout());

        menu = new Menu();
        if (usuario != null) {
            menu.setProfileName(usuario.getNombreUsuario());
            menu.setProfileRol(rolUsuario.getNombreRol());
        }
        panelBody = new JPanel(new BorderLayout());
        initMenuArrowIcon();
        menuButton = new JButton();
        menuButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Menu.button.background;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");
        menuButton.addActionListener((ActionEvent e) -> {
            setMenuFull(!menu.isMenuFull());
        });
        initMenuEvent();
        setLayer(menuButton, JLayeredPane.POPUP_LAYER);
        add(menuButton);
        add(menu);
        notificationBell = new NotificationBell();
        // Cápsula de perfil del compañero con los datos, la foto y la campana
        // de esta versión. El contenido del Home y los permisos no se alteran.
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 14, 6, 30));
        header.setPreferredSize(new Dimension(0, UIScale.scale(76)));
        JPanel controles = new TarjetaPerfil();
        controles.add(notificationBell);
        controles.add(javax.swing.Box.createHorizontalStrut(3));
        JPanel separador = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Color borde = javax.swing.UIManager.getColor("App.borderColor");
                g.setColor(borde == null ? new java.awt.Color(70, 85, 98) : borde);
                g.fillRect(0, 9, 1, Math.max(0, getHeight() - 18));
            }
        };
        separador.setOpaque(false);
        separador.setPreferredSize(new Dimension(1, 40));
        separador.setMaximumSize(new Dimension(1, 40));
        controles.add(separador);
        controles.add(javax.swing.Box.createHorizontalStrut(12));
        avatarPerfil = new JLabel();
        avatarPerfil.setToolTipText("Opciones de cuenta");
        refrescarFotoPerfil();
        controles.add(avatarPerfil);
        controles.add(javax.swing.Box.createHorizontalStrut(9));
        JPanel datos = new JPanel();
        datos.setOpaque(false);
        datos.setLayout(new javax.swing.BoxLayout(datos, javax.swing.BoxLayout.Y_AXIS));
        JLabel cuenta = new JLabel(usuario == null ? "Usuario" : usuario.getNombreUsuario());
        cuenta.setFont(cuenta.getFont().deriveFont(java.awt.Font.BOLD));
        JLabel rol = new JLabel(rolUsuario == null ? "" : rolUsuario.getNombreRol());
        datos.add(javax.swing.Box.createVerticalGlue());
        datos.add(cuenta);
        datos.add(rol);
        datos.add(javax.swing.Box.createVerticalGlue());
        controles.add(datos);
        controles.add(javax.swing.Box.createHorizontalStrut(11));
        javax.swing.JComponent flecha = new FlechaPerfil();
        flecha.setToolTipText("Opciones de cuenta");
        controles.add(flecha);
        instalarMenuPerfil(controles, avatarPerfil, datos, cuenta, rol, flecha);
        header.add(controles, BorderLayout.EAST);
        contenedorPrincipal = new JPanel(new BorderLayout());
        contenedorPrincipal.add(header, BorderLayout.NORTH);
        contenedorPrincipal.add(panelBody, BorderLayout.CENTER);
        add(contenedorPrincipal);
    }

    @Override
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        initMenuArrowIcon();
    }

    private void initMenuArrowIcon() {
        if (menuButton == null) {
            menuButton = new JButton();
        }
        String icon = (getComponentOrientation().isLeftToRight()) ? "menu_left.svg" : "menu_right.svg";
        menuButton.setIcon(new FlatSVGIcon("veterinaria/icon/svg/" + icon, 0.8f));
    }

    private void usuarioMenu(int subIndex, MenuAction action) {
        if (subIndex == 1) {
            showFormConPermiso(new FormUsuario());
        } else {
            action.cancel();
        }
    }

    private void clienteMenu(int subIndex, MenuAction action) {
        if (subIndex == 1) {
            showFormConPermiso(new FormCliente());
        } else {
            action.cancel();
        }
    }

    private void mascotaMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormMascota());
                break;
            case 2:
                showFormConPermiso(new FormConsulta());
                break;
            case 3:
                showFormConPermiso(new FormHistoriaClinica());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void cuentaCorrienteMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormGestionCuentasCorrientes());
                break;
            case 2:
                showFormConPermiso(new FormCuentaCorrienteMovimientos());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void cajaMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormCajaRegistradora());
                break;
            case 2:
                showFormConPermiso(new FormInformesCajaMovimientos());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void productoMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormProducto());
                break;
            case 2:
                showFormConPermiso(new FormStockProducto());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void proveedoresMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormProveedores());
                break;
            case 2:
                showFormConPermiso(new FormOrdenesCompra());
                break;
            case 3:
                showFormConPermiso(new FormRecibirPedido());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void laboratorioMenu(int subIndex, MenuAction action) {
        if (subIndex == 1) {
            showFormConPermiso(new FormLaboratorio());
        } else {
            action.cancel();
        }
    }

    private void hospitalizacionMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormHospitalizaciones());
                break;
            case 2:
                showFormConPermiso(new FormProcedimientos());
                break;

            default:
                action.cancel();
                break;
        }
    }

    private void peluqueriaMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new FormTurnosPeluqueria());
                break;
            case 2:
                showFormConPermiso(new FormHistorialTurnos());
                break;
            case 3:
                showFormConPermiso(new PanelTiposCitaPeluqueria());
                break;

            default:
                action.cancel();
                break;
        }
    }

    private void auditoriaMenu(int subIndex, MenuAction action) {
        // Auditoría NO tiene subitems (ver Menu.menuItems: {"AUDITORÍA"}).
        // Cuando el item no tiene submenús, MenuItem dispara subIndex=0.
        // Para mantener compatibilidad con versiones previas, aceptamos también subIndex=1.
        switch (subIndex) {
            case 0:
            case 1:
                showFormConPermiso(new FormAuditoria());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void configuracionMenu(int subIndex, MenuAction action) {
        switch (subIndex) {
            case 1:
                showFormConPermiso(new ClassConfig());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void backupMenu(int subIndex, MenuAction action) {
        // Al igual que auditoría, si no tiene subitems, maneja el subIndex 0 o 1.
        switch (subIndex) {
            case 0:
            case 1:
                showFormConPermiso(new FormBackUp());
                break;
            default:
                action.cancel();
                break;
        }
    }

    private void initMenuEvent() {
        // IMPORTANTE:
        // No llamar a Application.showForm() durante el constructor de MainForm.
        // Application.showForm() tiene una logica de auto-inicializacion cuando mainForm == null,
        // y si se invoca desde aqui (durante new MainForm()) puede provocar recursion infinita:
        // initializeMainForm -> new MainForm -> initMenuEvent -> showForm -> initializeMainForm...
        // La pantalla de bienvenida ya se muestra desde Application.login() una vez que el mainForm
        // quedo correctamente asignado en la instancia de Application.

        // Añadir eventos a los menús y submenús
        menu.addMenuEvent((int index, int subIndex, MenuAction action) -> {
            switch (index) {
                case 0: //Home
                    Application.showForm(new FormBienvenida());
                    break;
                case 1: // Usuario
                    usuarioMenu(subIndex, action);
                    break;
                case 2: // Cliente
                    clienteMenu(subIndex, action);
                    break;
                case 3: // Mascota
                    mascotaMenu(subIndex, action);
                    break;
                case 4: // Cuenta Corriente
                    cuentaCorrienteMenu(subIndex, action);
                    break;
                case 5: // Caja
                    cajaMenu(subIndex, action);
                    break;
                case 6: // Productos
                    productoMenu(subIndex, action);
                    break;
                case 7: // Proveedores
                    proveedoresMenu(subIndex, action);
                    break;
                case 8: // Laboratorio
                    laboratorioMenu(subIndex, action);
                    break;
                case 9: // Hospitalización
                    hospitalizacionMenu(subIndex, action);
                    break;
                case 10: // Peluquería
                    peluqueriaMenu(subIndex, action);
                    break;
              case 11: // Backup
                    backupMenu(subIndex, action); 
                    break;
                case 12: // Auditoria
                    auditoriaMenu(subIndex, action);
                    break;
                case 13: // Configuración
                    configuracionMenu(subIndex, action);
                    break;
                case 14: // Exit
                    Application.logout();
                    break;
                default:
                    action.cancel();
                    break;
            }
        });
    }

    private void setMenuFull(boolean full) {
        String icon;
        if (getComponentOrientation().isLeftToRight()) {
            icon = (full) ? "menu_left.svg" : "menu_right.svg";
        } else {
            icon = (full) ? "menu_right.svg" : "menu_left.svg";
        }
        menuButton.setIcon(new FlatSVGIcon("veterinaria/icon/svg/" + icon, 0.8f));
        menu.setMenuFull(full);
        revalidate();
    }

    public void hideMenu() {
        menu.hideMenuItem();
    }

    public void showForm(Component component) {
        panelBody.removeAll(); // Limpiar el contenido anterior
        TableLayoutSupport.prepare(component);

        // Informes de Caja gestiona sus propias pestañas, tabla y paginación.
        // Evitar un scroll externo que desplaza los controles fuera de pantalla.
        if (component instanceof FormInformesCajaMovimientos
                || component instanceof FormBienvenida
                || component instanceof FormAuditoria
                || component instanceof FormCajaRegistradora) {
            panelBody.add(component, BorderLayout.CENTER);
            panelBody.revalidate();
            panelBody.repaint();
            return;
        }

        // Los demás formularios conservan su desplazamiento habitual.
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(component);
        scrollPane.setBorder(null); // Le quitamos el borde para que no rompa tu estética FlatLaf
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Scroll más suave al usar la ruedita
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        panelBody.add(scrollPane, BorderLayout.CENTER); // Añadimos el scroll en el centro
        panelBody.repaint(); // Redibujar el panel
        panelBody.revalidate(); // Validar el nuevo diseño
    }

    private void showFormConPermiso(java.awt.Component form) {
        String permiso = form.getClass().getSimpleName();   // ejemplo: "FormCliente"

        // Chequeo unificado con regla de compatibilidad:
        // - Si el permiso NO está definido para el rol, no se restringe.
        // - Si está definido, exige acceso=true.
        if (sesion != null && sesion.getUsuario() != null && sesion.puede(permiso)) {
            Application.showForm(form);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "No tienes permisos suficientes para acceder a esta sección del sistema.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );

            // Auditoría: intento de acceso sin permisos
            try {
                auditoriaService.registrar(
                        "ACCESS_DENIED",
                        "Formulario",
                        null,
                        "UI",
                        "Acceso denegado a " + permiso,
                        AuditoriaService.RESULT_ERROR,
                        null,
                        null,
                        null
                );
            } catch (Exception ex) {
                System.err.println("[AUDITORIA][UI] No se pudo registrar acceso denegado: " + ex.getMessage());
            }

            Application.showForm(new FormAccesoRestringido());
        }
    }

    public void setSelectedMenu(int index, int subIndex) {
        menu.setSelectedMenu(index, subIndex);
    }

    public Menu getMenu() {
        return menu;
    }

    private Menu menu;
    private JPanel panelBody;
    private JButton menuButton;
    private NotificationBell notificationBell;
    private JLabel avatarPerfil;
    private JPanel contenedorPrincipal;

    /** Recarga la foto del usuario autenticado después de editar su perfil. */
    public void refrescarFotoPerfil() {
        if (avatarPerfil == null) return;
        javax.swing.Icon icono = null;
        SesionUsuario sesion = Application.getSesionUsuario();
        Usuario actual = sesion == null ? null : sesion.getUsuario();
        try {
            if (actual != null) {
                Usuario persistido = new veterinaria.persistencia.UsuarioDAO()
                        .obtenerPorId(actual.getIdUsuario());
                String ruta = persistido == null ? null : persistido.getRutaImagenPerfil();
                if (ruta != null && !ruta.isBlank()) {
                    java.io.File archivo = new java.io.File(ruta);
                    if (archivo.isFile()) {
                        ImageIcon foto = new ImageIcon(archivo.getAbsolutePath());
                        if (foto.getIconWidth() > 0 && foto.getIconHeight() > 0) {
                            icono = veterinaria.util.ui.ImageUtils.toCircularIcon(foto, 40);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[PERFIL] No se pudo cargar la foto: " + ex.getMessage());
        }
        if (icono == null) {
            icono = crearAvatarIniciales(actual == null ? null : actual.getNombreUsuario());
        }
        avatarPerfil.setIcon(icono);
        avatarPerfil.revalidate();
        avatarPerfil.repaint();
    }

    private Icon crearAvatarIniciales(String nombreUsuario) {
        String cuenta = nombreUsuario == null ? "" : nombreUsuario.strip().toUpperCase(Locale.ROOT);
        int longitud = Math.min(2, cuenta.codePointCount(0, cuenta.length()));
        String iniciales = longitud == 0 ? "U" : cuenta.substring(0, cuenta.offsetByCodePoints(0, longitud));
        return new AvatarIniciales(iniciales);
    }

    private record AvatarIniciales(String texto) implements Icon {
        private static final int DIAMETRO = 40;

        @Override public int getIconWidth() { return DIAMETRO; }

        @Override public int getIconHeight() { return DIAMETRO; }

        @Override
        public void paintIcon(Component componente, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(new Color(255, 160, 0));
                g.fill(new Ellipse2D.Float(0, 0, DIAMETRO, DIAMETRO));

                Font fuente = componente.getFont().deriveFont(Font.BOLD, 16f);
                TextLayout letras = new TextLayout(texto, fuente, g.getFontRenderContext());
                var limites = letras.getBounds();
                g.setColor(new Color(32, 41, 50));
                letras.draw(g, (float) (DIAMETRO / 2f - limites.getCenterX()),
                        (float) (DIAMETRO / 2f - limites.getCenterY()));
            } finally {
                g.dispose();
            }
        }
    }

    /** Cápsula del perfil que se pinta una sola vez alrededor de los controles existentes. */
    private static final class TarjetaPerfil extends JPanel {
        TarjetaPerfil() {
            setOpaque(false);
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
            setBorder(new EmptyBorder(3, 6, 3, 14));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.Color superficie = javax.swing.UIManager.getColor("App.surface.background");
                java.awt.Color borde = javax.swing.UIManager.getColor("App.borderColor");
                g2.setColor(superficie == null ? new java.awt.Color(35, 47, 59) : superficie);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g2.setColor(borde == null ? new java.awt.Color(58, 75, 89) : borde);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Flecha discreta que identifica el menú de cuenta, sin caracteres de fuente. */
    private static final class FlechaPerfil extends javax.swing.JComponent {
        FlechaPerfil() {
            setPreferredSize(new Dimension(14, 30));
            setMinimumSize(new Dimension(14, 30));
            setMaximumSize(new Dimension(14, 30));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.Color color = javax.swing.UIManager.getColor("App.muted.foreground");
                g2.setColor(color == null ? new java.awt.Color(180, 193, 203) : color);
                g2.setStroke(new java.awt.BasicStroke(1.6f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int x = getWidth() / 2;
                int y = getHeight() / 2;
                g2.drawLine(x - 4, y - 2, x, y + 2);
                g2.drawLine(x, y + 2, x + 4, y - 2);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * El menú de cuenta utiliza acciones existentes y respeta los permisos.
     * La campanita conserva su propio popup de notificaciones.
     */
    private void instalarMenuPerfil(JPanel tarjeta, Component... activadores) {
        java.awt.event.MouseAdapter desplegar = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                javax.swing.JPopupMenu opciones = new javax.swing.JPopupMenu();
                SesionUsuario actual = Application.getSesionUsuario();
                if (actual != null && actual.getUsuario() != null) {
                    // La consulta del perfil no equivale a administrar otros usuarios.
                    javax.swing.JMenuItem perfil = new javax.swing.JMenuItem("Mi perfil");
                    perfil.addActionListener(a -> {
                        java.awt.Window ventana = javax.swing.SwingUtilities.getWindowAncestor(MainForm.this);
                        MiPerfilDialog dialogo = new MiPerfilDialog(ventana, Application.getSesionUsuario());
                        dialogo.setLocationRelativeTo(MainForm.this);
                        dialogo.setVisible(true);
                    });
                    opciones.add(perfil);
                }
                javax.swing.JMenuItem clave = new javax.swing.JMenuItem("Cambiar contraseña");
                clave.addActionListener(a -> {
                    java.awt.Window ventana = javax.swing.SwingUtilities.getWindowAncestor(MainForm.this);
                    RecuperarContrasenaDialog dialogo = new RecuperarContrasenaDialog(
                            ventana, usuario == null ? "" : usuario.getNombreUsuario(), true);
                    dialogo.setLocationRelativeTo(MainForm.this);
                    dialogo.setVisible(true);
                });
                opciones.add(clave);
                opciones.addSeparator();
                javax.swing.JMenuItem salir = new javax.swing.JMenuItem("Cerrar sesión");
                salir.addActionListener(a -> Application.logout());
                opciones.add(salir);
                opciones.show(tarjeta,
                        Math.max(0, tarjeta.getWidth() - opciones.getPreferredSize().width),
                        tarjeta.getHeight() + 6);
            }
        };
        tarjeta.addMouseListener(desplegar);
        for (Component componente : activadores) {
            componente.addMouseListener(desplegar);
            componente.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }
    }

    private class MainFormLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(5, 5);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(0, 0);
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                boolean ltr = parent.getComponentOrientation().isLeftToRight();
                Insets insets = UIScale.scale(parent.getInsets());
                int x = insets.left;
                int y = insets.top;
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int menuWidth = UIScale.scale(menu.isMenuFull() ? menu.getMenuMaxWidth() : menu.getMenuMinWidth());
                int menuX = ltr ? x : x + width - menuWidth;
                menu.setBounds(menuX, y, menuWidth, height);
                int menuButtonWidth = menuButton.getPreferredSize().width;
                int menuButtonHeight = menuButton.getPreferredSize().height;
                int menubX;
                if (ltr) {
                    menubX = (int) (x + menuWidth - (menuButtonWidth * (menu.isMenuFull() ? 0.5f : 0.3f)));
                } else {
                    menubX = (int) (menuX - (menuButtonWidth * (menu.isMenuFull() ? 0.5f : 0.7f)));
                }
                menuButton.setBounds(menubX, UIScale.scale(30), menuButtonWidth, menuButtonHeight);
                int gap = UIScale.scale(5);
                int bodyWidth = width - menuWidth - gap;
                int bodyHeight = height;
                int bodyx = ltr ? (x + menuWidth + gap) : x;
                int bodyy = y;
                contenedorPrincipal.setBounds(bodyx, bodyy, bodyWidth, bodyHeight);
            }
        }
    }
}
