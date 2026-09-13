package veterinaria.vista.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
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
        panelBody = new JPanel();
        panelBody.setLayout(new BorderLayout());
        add(panelBody, BorderLayout.CENTER);
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
                + "arc:999;"
                + "focusWidth:0;"
                + "borderWidth:0");
        menuButton.addActionListener((ActionEvent e) -> {
            setMenuFull(!menu.isMenuFull());
        });
        initMenuEvent();
        setLayer(menuButton, JLayeredPane.POPUP_LAYER);
        add(menuButton);
        add(menu);
        add(panelBody);
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

        // ¡LA SOLUCIÓN AQUÍ! 
        // Envolvemos el formulario en un JScrollPane antes de agregarlo al panelBody
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
                panelBody.setBounds(bodyx, bodyy, bodyWidth, bodyHeight);
            }
        }
    }
}
