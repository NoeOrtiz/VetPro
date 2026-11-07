package veterinaria.vista;

import java.awt.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.*;
import veterinaria.vista.application.Application;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.Rol;

/**
 * Inicio de VetPRO. Diseño programático para no alterar el menú lateral ni
 * los formularios generados por NetBeans.
 */
public class FormBienvenida extends JPanel {
    private final Color accent = new Color(132, 191, 78);
    private Color card;
    private Color muted;
    private final JLabel fecha = new JLabel();
    private final JLabel hora = new JLabel();
    private final Timer reloj;

    public FormBienvenida() {
        actualizarPaleta();
        setLayout(new BorderLayout());
        setOpaque(false);
        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(new EmptyBorder(8, 30, 8, 30));
        JLabel logo = new JLabel();
        java.net.URL url = getClass().getResource("/veterinaria/icon/png/bienvenida.png");
        if (url != null) {
            ImageIcon original = new ImageIcon(url);
            if (original.getIconWidth() > 0) {
                Image imagen = original.getImage().getScaledInstance(-1, 145, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(imagen));
            }
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        // El perfil comparte la franja superior con el logo, sin desplazarlo hacia abajo.
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.setAlignmentX(Component.CENTER_ALIGNMENT);
        encabezado.add(logo, BorderLayout.CENTER);
        encabezado.add(crearPerfilSuperior(), BorderLayout.EAST);
        encabezado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        contenido.add(encabezado);
        contenido.add(Box.createVerticalStrut(10));
        JLabel saludo = titulo("¡Bienvenida, " + nombreSesion() + "!", 29);
        saludo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(saludo);
        JLabel subtitulo = new JLabel("¿Qué vamos a hacer hoy?");
        subtitulo.setForeground(muted);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(Box.createVerticalStrut(5));
        contenido.add(subtitulo);
        contenido.add(Box.createVerticalStrut(8));

        JPanel resumen = panelTarjeta(new BorderLayout(20, 0));
        JPanel resumenIzq = new JPanel();
        resumenIzq.setOpaque(false);
        resumenIzq.setLayout(new BoxLayout(resumenIzq, BoxLayout.Y_AXIS));
        resumenIzq.add(titulo("Resumen del día", 19));
        resumenIzq.add(Box.createVerticalStrut(8));
        JLabel aclaracion = new JLabel("Consultas, turnos y alertas: indicadores pendientes de conectar");
        aclaracion.setForeground(muted);
        resumenIzq.add(aclaracion);
        resumen.add(resumenIzq, BorderLayout.CENTER);
        JPanel fechaPanel = new JPanel();
        fechaPanel.setOpaque(false);
        fechaPanel.setLayout(new BoxLayout(fechaPanel, BoxLayout.Y_AXIS));
        fecha.putClientProperty("vetpro.home.accent", Boolean.TRUE);
        fecha.setForeground(accent);
        hora.setForeground(muted);
        fechaPanel.add(fecha);
        fechaPanel.add(Box.createVerticalStrut(6));
        fechaPanel.add(hora);
        resumen.add(fechaPanel, BorderLayout.EAST);
        resumen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        contenido.add(resumen);
        contenido.add(Box.createVerticalStrut(10));

        JPanel accesos = new JPanel(new GridLayout(2, 2, 16, 16));
        accesos.setOpaque(false);
        accesos.add(acceso("Nueva Consulta", "Registrar una nueva atención", "veterinaria/menu/icon/3.svg", () -> abrir("FormConsulta", new FormConsulta())));
        accesos.add(acceso("Agenda / Calendario", "Turnos y programación", "veterinaria/menu/icon/10.svg", () -> abrir("FormTurnosPeluqueria", new FormTurnosPeluqueria())));
        accesos.add(acceso("Pacientes (Fichas)", "Gestionar mascotas", "veterinaria/menu/icon/3.svg", () -> abrir("FormMascota", new FormMascota())));
        accesos.add(acceso("Caja", "Ventas y cobros", "veterinaria/menu/icon/5.svg", () -> abrir("FormCajaRegistradora", new FormCajaRegistradora())));
        accesos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 245));
        contenido.add(accesos);
        contenido.add(Box.createVerticalStrut(18));

        JPanel inferiores = new JPanel(new GridLayout(1, 2, 16, 0));
        inferiores.setOpaque(false);
        inferiores.add(info("Actividad reciente", "Próximamente: movimientos registrados por auditoría."));
        inferiores.add(info("Alertas", "Próximamente: vencimientos, turnos y stock crítico."));
        inferiores.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        contenido.add(inferiores);
        contenido.add(Box.createVerticalStrut(6));
        JLabel pie = new JLabel("VetPRO  ·  Sistema de Gestión Veterinaria", SwingConstants.CENTER);
        pie.setForeground(muted);
        pie.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(pie);

        add(contenido, BorderLayout.CENTER);
        actualizarFecha();
        reloj = new Timer(60000, e -> actualizarFecha());
        reloj.start();
        aplicarPaleta(this);
    }

    private void actualizarPaleta() {
        Color fondo = UIManager.getColor("Panel.background");
        if (fondo == null) fondo = new Color(48, 62, 75);
        boolean oscuro = (fondo.getRed() * 299 + fondo.getGreen() * 587
                + fondo.getBlue() * 114) / 1000 < 128;
        card = oscuro ? new Color(38, 49, 61) : new Color(246, 248, 250);
        muted = oscuro ? new Color(185, 195, 205) : new Color(83, 96, 110);
    }

    private void aplicarPaleta(Component componente) {
        if (componente instanceof JComponent jc) {
            if (Boolean.TRUE.equals(jc.getClientProperty("vetpro.home.card"))) {
                jc.setBackground(card);
                jc.setForeground(UIManager.getColor("Label.foreground"));
                jc.setBorder(new CompoundBorder(
                        new LineBorder(UIManager.getColor("Component.borderColor") != null
                                ? UIManager.getColor("Component.borderColor")
                                : new Color(110, 125, 138), 1, true),
                        new EmptyBorder(18, 20, 18, 20)));
            }
            if (componente instanceof JLabel etiqueta) {
                if (Boolean.TRUE.equals(jc.getClientProperty("vetpro.home.title"))) {
                    etiqueta.setForeground(UIManager.getColor("Label.foreground"));
                } else if (!Boolean.TRUE.equals(jc.getClientProperty("vetpro.home.accent"))
                        && etiqueta.getIcon() == null) {
                    etiqueta.setForeground(muted);
                }
            }
        }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) aplicarPaleta(hijo);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (fecha != null && hora != null && getComponentCount() > 0) {
            actualizarPaleta();
            aplicarPaleta(this);
            fecha.setForeground(accent);
        }
    }

    private String nombreSesion() {
        Usuario usuario = Application.getSesionUsuario() == null
                ? null : Application.getSesionUsuario().getUsuario();
        if (usuario == null) return "usuario";
        String nombre = usuario.getNombreUsuario();
        try {
            if (usuario.getPersona() != null
                    && usuario.getPersona().getNombre() != null
                    && !usuario.getPersona().getNombre().isBlank()) {
                nombre = usuario.getPersona().getNombre();
            }
        } catch (RuntimeException ex) {
            // Si la relación Persona no está cargada, usar el nombre de usuario.
        }
        if (nombre == null || nombre.isBlank()) return "usuario";
        nombre = nombre.trim();
        return nombre.substring(0, 1).toUpperCase(new Locale("es", "AR"))
                + nombre.substring(1).toLowerCase(new Locale("es", "AR"));
    }

    private JPanel crearPerfilSuperior() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        barra.setOpaque(false);
        Usuario usuario = Application.getSesionUsuario() == null
                ? null : Application.getSesionUsuario().getUsuario();
        Rol rol = Application.getSesionUsuario() == null
                ? null : Application.getSesionUsuario().getRol();
        JLabel avatar = new JLabel();
        java.net.URL recurso = getClass().getResource("/veterinaria/icon/png/default.png");
        if (recurso != null) {
            Image img = new ImageIcon(recurso).getImage()
                    .getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            avatar.setIcon(new ImageIcon(img));
        }
        barra.add(avatar);
        JPanel datos = new JPanel();
        datos.setOpaque(false);
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        String cuenta = usuario == null ? "Usuario" : usuario.getNombreUsuario();
        datos.add(titulo(cuenta == null ? "Usuario" : cuenta, 14));
        JLabel cargo = new JLabel(rol == null ? "" : rol.getNombreRol());
        cargo.setForeground(muted);
        datos.add(cargo);
        barra.add(datos);
        barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return barra;
    }

    private void actualizarFecha() {
        LocalDateTime ahora = LocalDateTime.now();
        Locale argentina = Locale.forLanguageTag("es-AR");
        fecha.setText(ahora.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", argentina)));
        hora.setText(ahora.format(DateTimeFormatter.ofPattern("HH:mm 'hs.'", argentina)));
    }

    private JLabel titulo(String texto, int size) {
        JLabel label = new JLabel(texto);
        label.setFont(label.getFont().deriveFont(Font.BOLD, (float) size));
        label.putClientProperty("vetpro.home.title", Boolean.TRUE);
        label.setForeground(UIManager.getColor("Label.foreground"));
        return label;
    }

    private JPanel panelTarjeta(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.putClientProperty("vetpro.home.card", Boolean.TRUE);
        p.setBackground(card);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(69, 82, 96), 1, true),
                new EmptyBorder(18, 20, 18, 20)));
        return p;
    }

    private JPanel info(String titulo, String detalle) {
        JPanel p = panelTarjeta(new BorderLayout(0, 10));
        p.add(titulo(titulo, 17), BorderLayout.NORTH);
        JLabel mensaje = new JLabel("<html>" + detalle + "</html>");
        mensaje.setForeground(muted);
        p.add(mensaje, BorderLayout.CENTER);
        return p;
    }

    private JButton acceso(String titulo, String descripcion, String simbolo, Runnable accion) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(16, 0));
        b.putClientProperty("vetpro.home.card", Boolean.TRUE);
        b.setBackground(card);
        b.setOpaque(true);
        b.setBorder(new CompoundBorder(new LineBorder(new Color(69, 82, 96), 1, true),
                new EmptyBorder(20, 20, 20, 20)));
        b.setForeground(UIManager.getColor("Label.foreground"));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        FlatSVGIcon svg = new FlatSVGIcon(simbolo, 2.0f);
        FlatSVGIcon.ColorFilter filtro = new FlatSVGIcon.ColorFilter();
        filtro.add(Color.decode("#969696"), accent, accent);
        svg.setColorFilter(filtro);
        JLabel icono = new JLabel(svg, SwingConstants.CENTER);
        icono.setPreferredSize(new Dimension(60, 64));
        icono.putClientProperty("vetpro.home.accent", Boolean.TRUE);
        icono.setForeground(accent);
        b.add(icono, BorderLayout.WEST);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(titulo(titulo, 19));
        textos.add(Box.createVerticalStrut(10));
        JLabel sub = new JLabel(descripcion);
        sub.setForeground(muted);
        textos.add(sub);
        b.add(textos, BorderLayout.CENTER);
        b.addActionListener(e -> accion.run());
        return b;
    }

    private void abrir(String permiso, JPanel destino) {
        if (Application.getSesionUsuario() != null
                && Application.getSesionUsuario().getUsuario() != null
                && Application.getSesionUsuario().puede(permiso)) {
            Application.showForm(destino);
        } else {
            JOptionPane.showMessageDialog(this, "No tenés permiso para acceder a este módulo.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void removeNotify() {
        reloj.stop();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (reloj != null) reloj.start();
    }
}
