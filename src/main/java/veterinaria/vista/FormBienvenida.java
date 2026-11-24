package veterinaria.vista;

import java.awt.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.servicio.ResumenInicioService;
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
    private final JLabel consultasHoy = new JLabel("—");
    private final JLabel enEspera = new JLabel("—");
    private final JLabel turnosHoy = new JLabel("—");
    private boolean cargandoIndicadores;

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
        // El perfil vive en el header global; el Home solo muestra su logo centrado.
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.setAlignmentX(Component.CENTER_ALIGNMENT);
        encabezado.add(logo, BorderLayout.CENTER);
        encabezado.setPreferredSize(new Dimension(0, 145));
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

        // Aspecto renovado sin alterar los accesos originales ni sus permisos.
        JPanel resumen = panelTarjeta(new BorderLayout(12, 12));
        JPanel cabeceraResumen = new JPanel(new BorderLayout(16, 0));
        cabeceraResumen.setOpaque(false);
        cabeceraResumen.add(titulo("Resumen del día", 19), BorderLayout.WEST);
        JPanel fechaPanel = new JPanel();
        fechaPanel.setOpaque(false);
        fechaPanel.setLayout(new BoxLayout(fechaPanel, BoxLayout.Y_AXIS));
        fecha.putClientProperty("vetpro.home.accent", Boolean.TRUE);
        fecha.setForeground(accent);
        hora.setForeground(muted);
        fechaPanel.add(fecha);
        fechaPanel.add(Box.createVerticalStrut(5));
        fechaPanel.add(hora);
        cabeceraResumen.add(fechaPanel, BorderLayout.EAST);
        resumen.add(cabeceraResumen, BorderLayout.NORTH);
        JPanel metricas = new JPanel(new GridLayout(1, 3, 12, 0));
        metricas.setOpaque(false);
        metricas.add(metrica("Consultas de hoy", consultasHoy, false));
        metricas.add(metrica("En espera", enEspera, true));
        metricas.add(metrica("Turnos de peluquería", turnosHoy, false));
        resumen.add(metricas, BorderLayout.CENTER);
        resumen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
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

        contenido.add(Box.createVerticalStrut(12));
        JLabel pie = new JLabel("VetPRO  ·  Sistema de Gestión Veterinaria", SwingConstants.CENTER);
        pie.setForeground(muted);
        pie.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(pie);

        add(contenido, BorderLayout.CENTER);
        actualizarFecha();
        reloj = new Timer(60000, e -> {
            actualizarFecha();
            cargarIndicadores();
        });
        reloj.start();
        aplicarPaleta(this);
        cargarIndicadores();
    }

    private void cargarIndicadores() {
        if (cargandoIndicadores) return;
        cargandoIndicadores = true;
        LocalDate hoy = LocalDate.now();
        new SwingWorker<ResumenInicioService.Indicadores, Void>() {
            @Override
            protected ResumenInicioService.Indicadores doInBackground() {
                return new ResumenInicioService().obtener(hoy);
            }

            @Override
            protected void done() {
                cargandoIndicadores = false;
                try {
                    ResumenInicioService.Indicadores datos = get();
                    consultasHoy.setText(Long.toString(datos.consultasHoy));
                    enEspera.setText(Long.toString(datos.pacientesEnEspera));
                    turnosHoy.setText(Long.toString(datos.turnosPeluqueriaHoy));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    mostrarIndicadoresNoDisponibles("No se pudieron cargar los indicadores.");
                } catch (ExecutionException ex) {
                    Logger.getLogger(FormBienvenida.class.getName()).log(Level.WARNING,
                            "Error al consultar indicadores del inicio", ex.getCause());
                    mostrarIndicadoresNoDisponibles("Indicadores no disponibles. Verificá la conexión a la base de datos.");
                }
            }
        }.execute();
    }

    private void actualizarPaleta() {
        Color fondo = UIManager.getColor("Panel.background");
        if (fondo == null) fondo = new Color(48, 62, 75);
        boolean oscuro = (fondo.getRed() * 299 + fondo.getGreen() * 587
                + fondo.getBlue() * 114) / 1000 < 128;
        card = oscuro ? new Color(38, 49, 61) : new Color(246, 248, 250);
        muted = oscuro ? new Color(185, 195, 205) : new Color(83, 96, 110);
        Color superficie = UIManager.getColor("App.surface.background");
        Color secundario = UIManager.getColor("App.muted.foreground");
        if (superficie != null) card = superficie;
        if (secundario != null) muted = secundario;
    }

    private void aplicarPaleta(Component componente) {
        if (componente instanceof JComponent jc) {
            if (Boolean.TRUE.equals(jc.getClientProperty("vetpro.home.card"))) {
                jc.setBackground(card);
                jc.setForeground(UIManager.getColor("Label.foreground"));
                if (jc instanceof JButton) {
                    jc.putClientProperty("FlatLaf.styleClass", "hibridoAcceso");
                    jc.putClientProperty("FlatLaf.style",
                            com.formdev.flatlaf.FlatLaf.isLafDark()
                                    ? "hoverBackground: #3A4F53" : "hoverBackground: #E9F2DC");
                } else {
                    // El panel pinta sus propias esquinas redondeadas.
                    jc.setBorder(new EmptyBorder(18, 20, 18, 20));
                }
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
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(1, 1, Math.max(0, getWidth() - 2),
                        Math.max(0, getHeight() - 2), 16, 16);
                Color borde = UIManager.getColor("App.borderColor");
                if (borde != null) {
                    g2.setColor(borde);
                    g2.drawRoundRect(1, 1, Math.max(0, getWidth() - 3),
                            Math.max(0, getHeight() - 3), 16, 16);
                }
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        p.setOpaque(false);
        p.putClientProperty("vetpro.home.card", Boolean.TRUE);
        p.setBackground(card);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));
        return p;
    }

    private JPanel metrica(String etiqueta, JLabel valor, boolean destacada) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 6, 8, 6));
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 27f));
        valor.putClientProperty(destacada ? "vetpro.home.accent" : "vetpro.home.title", Boolean.TRUE);
        valor.setForeground(destacada ? accent : UIManager.getColor("Label.foreground"));
        JLabel texto = new JLabel(etiqueta);
        texto.setForeground(muted);
        panel.add(valor, BorderLayout.CENTER);
        panel.add(texto, BorderLayout.SOUTH);
        return panel;
    }

    private void mostrarIndicadoresNoDisponibles(String motivo) {
        for (JLabel valor : new JLabel[]{consultasHoy, enEspera, turnosHoy}) {
            valor.setText("—");
            valor.setToolTipText(motivo);
        }
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
        b.putClientProperty("FlatLaf.styleClass", "hibridoAcceso");
        b.setRolloverEnabled(true);
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
        JLabel nombre = titulo(titulo, 19);
        textos.add(nombre);
        textos.add(Box.createVerticalStrut(10));
        JLabel sub = new JLabel(descripcion);
        sub.setForeground(muted);
        textos.add(sub);
        b.add(textos, BorderLayout.CENTER);
        Border bordeNormal = new CompoundBorder(
                new LineBorder(new Color(80, 99, 113)), new EmptyBorder(19, 19, 19, 19));
        Border bordeHover = new CompoundBorder(
                new LineBorder(accent, 2), new EmptyBorder(18, 18, 18, 18));
        b.setBorder(bordeNormal);
        b.getModel().addChangeListener(e -> {
            boolean hover = b.isEnabled() && b.getModel().isRollover();
            b.setBorder(hover ? bordeHover : bordeNormal);
            nombre.setForeground(hover ? accent : UIManager.getColor("Label.foreground"));
        });
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
        if (reloj != null) cargarIndicadores();
    }
}
