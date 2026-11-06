package veterinaria.vista;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.*;
import veterinaria.vista.application.Application;

/**
 * Inicio de VetPRO. Diseño programático para no alterar el menú lateral ni
 * los formularios generados por NetBeans.
 */
public class FormBienvenida extends JPanel {
    private final Color accent = new Color(132, 191, 78);
    private final Color card = new Color(38, 49, 61);
    private final Color muted = new Color(185, 195, 205);
    private final JLabel fecha = new JLabel();
    private final JLabel hora = new JLabel();
    private final Timer reloj;

    public FormBienvenida() {
        setLayout(new BorderLayout());
        setOpaque(false);
        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel logo = new JLabel();
        java.net.URL url = getClass().getResource("/veterinaria/icon/png/bienvenida.png");
        if (url != null) {
            ImageIcon original = new ImageIcon(url);
            if (original.getIconWidth() > 0) {
                Image imagen = original.getImage().getScaledInstance(-1, 125, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(imagen));
            }
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        contenido.add(logo);
        contenido.add(Box.createVerticalStrut(8));
        JLabel saludo = titulo("Bienvenidos", 30);
        saludo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(saludo);
        contenido.add(Box.createVerticalStrut(24));

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
        fecha.setForeground(accent);
        hora.setForeground(muted);
        fechaPanel.add(fecha);
        fechaPanel.add(Box.createVerticalStrut(6));
        fechaPanel.add(hora);
        resumen.add(fechaPanel, BorderLayout.EAST);
        resumen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        contenido.add(resumen);
        contenido.add(Box.createVerticalStrut(18));

        JPanel accesos = new JPanel(new GridLayout(2, 2, 16, 16));
        accesos.setOpaque(false);
        accesos.add(acceso("Nueva Consulta", "Registrar una nueva atención", "✚", () -> abrir("FormConsulta", new FormConsulta())));
        accesos.add(acceso("Agenda / Calendario", "Turnos y programación", "▦", () -> abrir("FormTurnosPeluqueria", new FormTurnosPeluqueria())));
        accesos.add(acceso("Pacientes (Fichas)", "Gestionar mascotas", "♧", () -> abrir("FormMascota", new FormMascota())));
        accesos.add(acceso("Inventario / Caja", "Productos y existencias", "▤", () -> abrir("FormProducto", new FormProducto())));
        accesos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        contenido.add(accesos);
        contenido.add(Box.createVerticalStrut(18));

        JPanel inferiores = new JPanel(new GridLayout(1, 2, 16, 0));
        inferiores.setOpaque(false);
        inferiores.add(info("Actividad reciente", "Próximamente: movimientos registrados por auditoría."));
        inferiores.add(info("Alertas", "Próximamente: vencimientos, turnos y stock crítico."));
        inferiores.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        contenido.add(inferiores);
        contenido.add(Box.createVerticalStrut(16));
        JLabel pie = new JLabel("VetPRO  ·  Sistema de Gestión Veterinaria", SwingConstants.CENTER);
        pie.setForeground(muted);
        pie.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(pie);

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
        actualizarFecha();
        reloj = new Timer(60000, e -> actualizarFecha());
        reloj.start();
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
        label.setForeground(UIManager.getColor("Label.foreground"));
        return label;
    }

    private JPanel panelTarjeta(LayoutManager layout) {
        JPanel p = new JPanel(layout);
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
        b.setBackground(card);
        b.setOpaque(true);
        b.setBorder(new CompoundBorder(new LineBorder(new Color(69, 82, 96), 1, true),
                new EmptyBorder(20, 20, 20, 20)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        JLabel icono = new JLabel(simbolo, SwingConstants.CENTER);
        icono.setFont(icono.getFont().deriveFont(Font.BOLD, 38f));
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
