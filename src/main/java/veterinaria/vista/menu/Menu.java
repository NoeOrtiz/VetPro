package veterinaria.vista.menu;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import veterinaria.entidad.Usuario;
import veterinaria.util.SesionUsuario;
import veterinaria.vista.application.Application;
import veterinaria.vista.menu.mode.LightDarkMode;
import veterinaria.vista.menu.mode.ToolBarAccentColor;
import veterinaria.util.ui.ImageUtils;

public class Menu extends JPanel {

    Usuario usuario = Application.getSesionUsuario().getUsuario();
    private final SesionUsuario sesion = Application.getSesionUsuario();

    private final String menuItems[][] = {
        {"HOME"}, // 0
        {"USUARIO", "Gestión de Usuario"}, // 1
        {"CLIENTE", "Gestión de Cliente"}, // 2
        {"MASCOTA", "Gestión de Mascota", "Consultas", "Historia Clínica"}, // 3
        {"CUENTA CORRENTE", "Gestión de Cuenta Corriente", "Movimientos Cuenta Corriente"}, // 4
        {"CAJA", "Caja Ventas y Cobros", "Informes Movimiento Caja"}, // 5
        {"PRODUCTO", "Gestión de Producto", "Stock de Productos"}, // 6
        {"PROVEEDORES", "Gestión de Proveedores", "Cargar Pedido", "Recepcionar Pedidos"}, // 7
        {"LABORATORIO", "Gestión de Pedidos"}, // 8
        {"HOSPITALIZACION", "Hospitalización", "Prodecimientos"}, // 9
        {"TURNOS DE PELUQUERIA", "Gestionar Turnos", "Historial de Turnos", "Tipos de cita (Config.)"}, // 10
        {"BACKUP", "Generar Respaldo"}, // 11
        {"AUDITORÍA"}, // 12
        {"CONFIGURACION", "Configuración General"}, // 13
        {"SALIR"} // 14
    };

    public boolean isMenuFull() {
        return menuFull;
    }

    public void setMenuFull(boolean menuFull) {
        this.menuFull = menuFull;
        if (menuFull) {
            header.setText(headerName);
            header.setHorizontalAlignment(getComponentOrientation().isLeftToRight() ? JLabel.LEFT : JLabel.RIGHT);
            lbProfileImg.setHorizontalAlignment(getComponentOrientation().isLeftToRight() ? JLabel.LEFT : JLabel.RIGHT);
            lbProfileName.setText(profileName);
            lbProfileRol.setText(profileRol);
        } else {
            header.setText("");
            header.setHorizontalAlignment(JLabel.CENTER);
            lbProfileImg.setHorizontalAlignment(JLabel.CENTER);
            lbProfileName.setText("");
            lbProfileRol.setText("");
        }
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                ((MenuItem) com).setFull(menuFull);
            }
        }
        lightDarkMode.setMenuFull(menuFull);
        toolBarAccentColor.setMenuFull(menuFull);
    }

    private final List<MenuEvent> events = new ArrayList<>();
    private boolean menuFull = true;
    private final String headerName = "Clinica Veterinaria";
    private String profileName = "";
    private String profileRol = "";

    protected final boolean hideMenuTitleOnMinimum = true;
    protected final int menuTitleLeftInset = 5;
    protected final int menuTitleVgap = 5;
    protected final int menuMaxWidth = 250;
    protected final int menuMinWidth = 60;
    protected final int headerFullHgap = 5;

    public Menu() {
        init();
        cargarImagenPerfil();
    }

    private void init() {
        setLayout(new MenuLayout());
        putClientProperty(FlatClientProperties.STYLE, ""
                + "border:20,2,2,2;"
                + "background:$Menu.background;"
                + "arc:10");
        header = new JLabel(headerName);
        header.setIcon(new ImageIcon(getClass().getResource("/veterinaria/icon/png/logo.png")));
        header.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.header.font;"
                + "foreground:$Menu.foreground");
        lbProfileImg = new JLabel("");
        lbProfileImg.setIcon(new ImageIcon(getClass().getResource("/veterinaria/icon/png/admin.png")));
        lbProfileName = new JLabel(profileName);
        lbProfileName.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.label.font;"
                + "foreground:$Menu.foreground");
        lbProfileRol = new JLabel(profileRol);
        lbProfileRol.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.label.font;"
                + "foreground:$Menu.foreground");

        scroll = new JScrollPane();
        panelMenu = new JPanel(new MenuItemLayout(this));
        panelMenu.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5;"
                + "background:$Menu.background");

        scroll.setViewportView(panelMenu);
        scroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:null");
        JScrollBar vscroll = scroll.getVerticalScrollBar();
        vscroll.setUnitIncrement(10);
        vscroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "width:$Menu.scroll.width;"
                + "trackInsets:$Menu.scroll.trackInsets;"
                + "thumbInsets:$Menu.scroll.thumbInsets;"
                + "background:$Menu.ScrollBar.background;"
                + "thumb:$Menu.ScrollBar.thumb");
        createMenu();
        lightDarkMode = new LightDarkMode();
        toolBarAccentColor = new ToolBarAccentColor(this);
        toolBarAccentColor.setVisible(FlatUIUtils.getUIBoolean("AccentControl.show", false));
        add(header);
        add(lbProfileName);
        add(lbProfileRol);
        add(lbProfileImg);
        add(scroll);
        add(lightDarkMode);
        add(toolBarAccentColor);
    }

    private JLabel header;
    private JLabel lbProfileName;
    private JLabel lbProfileRol;
    private JLabel lbProfileImg;
    private JScrollPane scroll;
    private JPanel panelMenu;
    private LightDarkMode lightDarkMode;
    private ToolBarAccentColor toolBarAccentColor;

    private void createMenu() {
        int index = 0;
        for (int i = 0; i < menuItems.length; i++) {
            String menuName = menuItems[i][0];
            if (menuName.startsWith("~") && menuName.endsWith("~")) {
                panelMenu.add(createTitle(menuName));
            } else {
                MenuItem menuItem = new MenuItem(this, menuItems[i], index, events);
                aplicarPermisosMenu(menuItem, index);
                panelMenu.add(menuItem);
                index++;
            }
        }
    }

    private void aplicarPermisosMenu(MenuItem menuItem, int menuIndex) {
        if (menuIndex == 0 || menuIndex == 14) {
            menuItem.setVisible(true);
            return;
        }

        int subCount = menuItem.getMenus().length - 1;
        boolean anyAllowed = false;

        for (int subIndex = 1; subIndex <= subCount; subIndex++) {
            String perm = mapPermiso(menuIndex, subIndex);
            boolean allowed;

            if (perm == null) {
                allowed = false;
            } else {
                allowed = (sesion != null) && sesion.puede(perm);
            }

            menuItem.setSubVisible(subIndex, allowed);
            anyAllowed = anyAllowed || allowed;
        }

        if (subCount == 0) {
            String perm = mapPermiso(menuIndex, 0);
            anyAllowed = (perm == null) ? true : ((sesion != null) && sesion.puede(perm));
        }

        menuItem.setVisible(anyAllowed);
    }

    private String mapPermiso(int menuIndex, int subIndex) {
        switch (menuIndex) {
            case 1:
                return "FormUsuario";
            case 2:
                return "FormCliente";
            case 3:
                if (subIndex == 1) {
                    return "FormMascota";
                }
                if (subIndex == 2) {
                    return "FormVisita";
                }
                if (subIndex == 3) {
                    return "FormHistoriaClinica";
                }
                return null;
            case 4:
                if (subIndex == 1) {
                    return "FormGestionCuentasCorrientes";
                }
                if (subIndex == 2) {
                    return "FormCuentaCorrienteMovimientos";
                }
                return null;
            case 5:
                if (subIndex == 1) {
                    return "FormCajaRegistradora";
                }
                if (subIndex == 2) {
                    return "FormInformesCajaMovimientos";
                }
                return null;
            case 6:
                if (subIndex == 1) {
                    return "FormProducto";
                }
                if (subIndex == 2) {
                    return "FormStockProducto";
                }
                return null;
            case 7:
                if (subIndex == 1) {
                    return "FormProveedores";
                }
                if (subIndex == 2) {
                    return "FormOrdenesCompra";
                }
                if (subIndex == 3) {
                    return "FormRecibirPedido";
                }
                return null;
            case 8:
                return "FormLaboratorio";
            case 9:
                if (subIndex == 1) {
                    return "FormHospitalizaciones";
                }
                if (subIndex == 2) {
                    return "FormProcedimientos";
                }
                return null;
            case 10:
                if (subIndex == 1) {
                    return "FormTurnosPeluqueria";
                }
                if (subIndex == 2) {
                    return "FormHistorialTurnos";
                }
                if (subIndex == 3) {
                    return "PanelTiposCitaPeluqueria";
                }
                return null;
            case 11:
                return "FormBackUp";     // Posición 11: Backup
            case 12:
                return "FormAuditoria";  // Posición 12: Auditoría
            case 13:
                return "ClassConfig";    // Posición 13: Configuración
            default:
                return null;
        }
    }

    private JLabel createTitle(String title) {
        String menuName = title.substring(1, title.length() - 1);
        JLabel lbTitle = new JLabel(menuName);
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.label.font;"
                + "foreground:$Menu.title.foreground");
        return lbTitle;
    }

    public void setSelectedMenu(int index, int subIndex) {
        runEvent(index, subIndex);
    }

    protected void setSelected(int index, int subIndex) {
        int size = panelMenu.getComponentCount();
        for (int i = 0; i < size; i++) {
            Component com = panelMenu.getComponent(i);
            if (com instanceof MenuItem) {
                MenuItem item = (MenuItem) com;
                if (item.getMenuIndex() == index) {
                    item.setSelectedIndex(subIndex);
                } else {
                    item.setSelectedIndex(-1);
                }
            }
        }
    }

    protected void runEvent(int index, int subIndex) {
        MenuAction menuAction = new MenuAction();
        for (MenuEvent event : events) {
            event.menuSelected(index, subIndex, menuAction);
        }
        if (!menuAction.isCancel()) {
            setSelected(index, subIndex);
        }
    }

    public void addMenuEvent(MenuEvent event) {
        events.add(event);
    }

    public void hideMenuItem() {
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                ((MenuItem) com).hideMenuItem();
            }
        }
        revalidate();
    }

    public boolean isHideMenuTitleOnMinimum() {
        return hideMenuTitleOnMinimum;
    }

    public int getMenuTitleLeftInset() {
        return menuTitleLeftInset;
    }

    public int getMenuTitleVgap() {
        return menuTitleVgap;
    }

    public int getMenuMaxWidth() {
        return menuMaxWidth;
    }

    public int getMenuMinWidth() {
        return menuMinWidth;
    }

    private void cargarImagenPerfil() {
        try {
            ImageIcon iconoDefecto = new ImageIcon(getClass().getResource("/veterinaria/icon/png/default.png"));
            lbProfileImg.setIcon(ImageUtils.toCircularIcon(iconoDefecto, 45));
        } catch (Exception ignore) {
        }

        final String rutaImagen = (usuario != null) ? usuario.getRutaImagenPerfil() : null;
        if (rutaImagen == null || rutaImagen.trim().isEmpty()) {
            return;
        }

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    return ImageUtils.loadCircularFromUrl(rutaImagen.trim(), 45);
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        lbProfileImg.setIcon(icon);
                    }
                } catch (Exception ignore) {
                }
            }
        }.execute();
    }

    private ImageIcon redondearImagen(ImageIcon icono) {
        int tamaño = Math.min(icono.getIconWidth(), icono.getIconHeight());
        BufferedImage imagenRedonda = new BufferedImage(tamaño, tamaño, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = imagenRedonda.createGraphics();
        g.setClip(new Ellipse2D.Double(0, 0, tamaño, tamaño));
        g.drawImage(icono.getImage(), 0, 0, tamaño, tamaño, null);
        g.dispose();

        return new ImageIcon(imagenRedonda);
    }

    public void setProfileName(String profileName) {
        this.profileName = (profileName != null ? profileName.toUpperCase() : "USUARIO");
        lbProfileName.setText(this.profileName);
    }

    public void setProfileRol(String profileRol) {
        this.profileRol = profileRol != null ? profileRol : "RolUsuario";
        lbProfileRol.setText(this.profileRol);
    }

    private class MenuLayout implements LayoutManager {

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
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int gap = UIScale.scale(5);
                int sheaderFullHgap = UIScale.scale(headerFullHgap);
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int iconWidth = width;
                int iconHeight = header.getPreferredSize().height;
                int iconHProfile = lbProfileImg.getPreferredSize().height;
                int hgap = menuFull ? sheaderFullHgap : 0;
                int accentColorHeight = 0;
                if (toolBarAccentColor.isVisible()) {
                    accentColorHeight = toolBarAccentColor.getPreferredSize().height + gap;
                }

                header.setBounds(x + hgap, y, iconWidth - (hgap * 2), iconHeight);
                lbProfileImg.setBounds(x + hgap, y + 45, iconWidth - (hgap * 2), iconHProfile);
                lbProfileRol.setBounds(x + hgap + 55, y + 35, iconWidth - (hgap * 2), iconHProfile);
                lbProfileName.setBounds(x + hgap + 55, y + 55, iconWidth - (hgap * 2), iconHProfile);

                int ldgap = UIScale.scale(10);
                int ldWidth = width - ldgap * 2;
                int ldHeight = lightDarkMode.getPreferredSize().height;
                int ldx = x + ldgap;
                int ldy = y + height - ldHeight - ldgap - accentColorHeight;

                int menux = x;
                int menuy = y + iconHeight + gap + 40;
                int menuWidth = width;
                int menuHeight = height - (iconHeight + gap) - (ldHeight + ldgap * 2) - (accentColorHeight);
                scroll.setBounds(menux, menuy, menuWidth, menuHeight - lightDarkMode.getPreferredSize().height);

                lightDarkMode.setBounds(ldx, ldy, ldWidth, ldHeight);

                if (toolBarAccentColor.isVisible()) {
                    int tbheight = toolBarAccentColor.getPreferredSize().height;
                    int tbwidth = Math.min(toolBarAccentColor.getPreferredSize().width, ldWidth);
                    int tby = y + height - tbheight - ldgap;
                    int tbx = ldx + ((ldWidth - tbwidth) / 2);
                    toolBarAccentColor.setBounds(tbx, tby, tbwidth, tbheight);
                }
            }
        }
    }

    public void setProfileIcon(ImageIcon nuevoIcono) {
        if (lbProfileImg != null && nuevoIcono != null) {
            try {
                ImageIcon iconoCircular = ImageUtils.toCircularIcon(nuevoIcono, 45);
                lbProfileImg.setIcon(iconoCircular);
                lbProfileImg.revalidate();
                lbProfileImg.repaint();
            } catch (Exception ex) {
                lbProfileImg.setIcon(nuevoIcono);
            }
        }
    }
}
