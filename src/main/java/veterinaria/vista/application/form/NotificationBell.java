package veterinaria.vista.application.form;

import java.awt.*;
import java.awt.geom.*;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
import veterinaria.servicio.ResumenInicioService;

/** Notificaciones persistentes con icono vectorial, sin depender de emojis del sistema. */
public class NotificationBell extends JPanel {
    private static final Color VERDE = new Color(132, 191, 78);
    private static final Color ROJO = new Color(231, 76, 85);
    private final JButton boton = new JButton() {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = (getWidth() - 25) / 2 - 5;
                int y = (getHeight() - 27) / 2;
                g.setColor(VERDE);
                Path2D bell = new Path2D.Double();
                bell.moveTo(x + 3, y + 20);
                bell.curveTo(x + 7, y + 16, x + 6, y + 12, x + 7, y + 9);
                bell.curveTo(x + 8, y + 2, x + 20, y + 2, x + 21, y + 9);
                bell.curveTo(x + 22, y + 12, x + 21, y + 16, x + 25, y + 20);
                bell.closePath();
                g.fill(bell);
                g.fillRoundRect(x + 1, y + 20, 26, 4, 4, 4);
                g.fillOval(x + 11, y + 24, 7, 5);
                if (cantidad > 0) {
                    String texto = cantidad > 99 ? "99+" : Long.toString(cantidad);
                    g.setFont(getFont().deriveFont(Font.BOLD, 11f));
                    FontMetrics fm = g.getFontMetrics();
                    int ancho = Math.max(19, fm.stringWidth(texto) + 10);
                    int bx = x + 24 - ancho / 3;
                    int by = y - 2;
                    g.setColor(ROJO);
                    g.fillRoundRect(bx, by, ancho, 19, 19, 19);
                    g.setColor(Color.WHITE);
                    g.drawString(texto, bx + (ancho - fm.stringWidth(texto)) / 2,
                            by + (19 - fm.getHeight()) / 2 + fm.getAscent());
                }
            } finally {
                g.dispose();
            }
        }
    };
    private final JPopupMenu popup = new JPopupMenu();
    private final Timer actualizador;
    private boolean cargando;
    private long cantidad;
    private ResumenInicioService.Indicadores ultimos;
    private long solicitudesPendientes;
    private boolean esAdmin() {
        veterinaria.util.SesionUsuario sesion = veterinaria.vista.application.Application.getSesionUsuario();
        return sesion != null && sesion.getRol() != null
                && "Administrador".equalsIgnoreCase(sesion.getRol().getNombreRol());
    }


    public NotificationBell() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(56, 38));
        setMinimumSize(new Dimension(56, 38));
        boton.setPreferredSize(new Dimension(56, 38));
        boton.setToolTipText("Notificaciones");
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(0, 0, 0, 0));
        boton.setContentAreaFilled(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(e -> {
            construirPopup();
            popup.show(boton, (boton.getWidth() - popup.getPreferredSize().width) / 2,
                    boton.getHeight() + 8);
            refrescar();
        });
        add(boton, BorderLayout.CENTER);
        popup.setBorder(new LineBorder(new Color(90, 110, 122), 1, true));
        actualizador = new Timer(60000, e -> refrescar());
        actualizador.start();
        refrescar();
    }

    private void refrescar() {
        if (cargando) return;
        cargando = true;
        new SwingWorker<ResumenInicioService.Indicadores, Void>() {
            @Override
            protected ResumenInicioService.Indicadores doInBackground() {
                return new ResumenInicioService().obtener(LocalDate.now());
            }
            @Override
            protected void done() {
                cargando = false;
                try {
                    ultimos = get();
                    cantidad = ultimos.productosStockCritico
                            + (ultimos.pacientesEnEspera > 0 ? 1 : 0);
                    // La cuenta de solicitudes se consulta al abrir la campanita.
                    if (esAdmin()) {
                        try {
                            solicitudesPendientes = new veterinaria.servicio.SolicitudReactivacionService()
                                    .contarPendientes();
                            cantidad += solicitudesPendientes;
                        } catch (RuntimeException ex) {
                            Logger.getLogger(NotificationBell.class.getName()).log(
                                    Level.WARNING, "No se pudieron cargar solicitudes", ex);
                        }
                    }
                    boton.repaint();
                    if (popup.isVisible()) construirPopup();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Logger.getLogger(NotificationBell.class.getName())
                            .log(Level.WARNING, "No se pudieron consultar las alertas", ex.getCause());
                    boton.setToolTipText("No se pudieron actualizar las notificaciones");
                }
            }
        }.execute();
    }

    private void construirPopup() {
        popup.removeAll();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        Color fondo = UIManager.getColor("Panel.background");
        if (fondo == null) fondo = new Color(48, 62, 75);
        panel.setBackground(fondo);
        panel.setBorder(new EmptyBorder(12, 15, 12, 15));
        JLabel cabecera = new JLabel("Notificaciones");
        cabecera.setFont(cabecera.getFont().deriveFont(Font.BOLD, 16f));
        cabecera.setForeground(UIManager.getColor("Label.foreground"));
        cabecera.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cabecera);
        if (ultimos == null) {
            agregarFila(panel, "Cargando notificaciones...", null);
        } else {
            if (ultimos.productosStockCritico > 0)
                agregarFila(panel, "Stock crítico", ultimos.productosStockCritico + " producto(s) con stock mínimo o inferior");
            if (ultimos.pacientesEnEspera > 0)
                agregarFila(panel, "Pacientes en espera", ultimos.pacientesEnEspera + " pendiente(s) de atención");
            if (esAdmin() && solicitudesPendientes > 0) {
                JButton solicitudes = new JButton("Solicitudes de reactivación (" + solicitudesPendientes + ")");
                solicitudes.setAlignmentX(Component.LEFT_ALIGNMENT);
                solicitudes.addActionListener(e -> {
                    popup.setVisible(false);
                    abrirSolicitudes();
                });
                panel.add(Box.createVerticalStrut(12));
                panel.add(solicitudes);
            }
            if (cantidad == 0)
                agregarFila(panel, "Todo al día", "No hay alertas pendientes");
        }
        popup.add(panel);
        if (popup.isVisible()) popup.pack();
        popup.revalidate();
        popup.repaint();
    }

    private void abrirSolicitudes() {
        if (!esAdmin()) return;
        java.awt.Component centro = SwingUtilities.getWindowAncestor(this);
        try {
            veterinaria.servicio.SolicitudReactivacionService servicio =
                    new veterinaria.servicio.SolicitudReactivacionService();
            java.util.List<Object[]> pendientes = servicio.listarPendientes();
            if (pendientes.isEmpty()) {
                JOptionPane.showMessageDialog(centro, "No hay solicitudes pendientes.");
                refrescar();
                return;
            }
            String[] opciones = new String[pendientes.size()];
            for (int i = 0; i < pendientes.size(); i++) {
                Object[] s = pendientes.get(i);
                opciones[i] = "Solicitud N.º " + s[0] + " · " + s[1] + " · "
                        + ("RECTIFICACION".equals(s[2]) ? "Corregir baja" : "Reactivar")
                        + " · " + ("URGENTE".equals(s[3]) ? "Urgente" : "Normal")
                        + " · Solicitó: " + s[8];
            }
            Object seleccionado = JOptionPane.showInputDialog(centro,
                    "Seleccione una solicitud para revisar:",
                    "Solicitudes pendientes", JOptionPane.PLAIN_MESSAGE,
                    null, opciones, opciones[0]);
            if (seleccionado == null) return;
            int indice = java.util.Arrays.asList(opciones).indexOf(seleccionado.toString());
            if (indice < 0) return;
            Object[] item = pendientes.get(indice);
            String fechaSolicitud = String.valueOf(item[5]);
            if (item[5] instanceof java.sql.Timestamp) {
                fechaSolicitud = ((java.sql.Timestamp) item[5]).toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            javax.swing.JTextArea descripcion = new javax.swing.JTextArea(
                    "Paciente: " + item[1] + "\nSolicitado por: " + item[8]
                    + "\nFecha: " + fechaSolicitud
                    + "\nPedido: " + ("RECTIFICACION".equals(item[2])
                            ? "Corregir una baja registrada por error" : "Reactivar al paciente")
                    + "\nPrioridad: " + ("URGENTE".equals(item[3]) ? "Urgente" : "Normal")
                    + "\nMotivo de baja registrado: " + (item[7] == null ? "-" : item[7])
                    + "\nExplicación del solicitante: " + item[4], 8, 38);
            descripcion.setEditable(false);
            descripcion.setLineWrap(true);
            descripcion.setWrapStyleWord(true);
            Object[] acciones = {"Aprobar", "Rechazar", "Volver"};
            int accion = JOptionPane.showOptionDialog(centro,
                    new JScrollPane(descripcion), "Revisar solicitud N.º " + item[0],
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, acciones, acciones[2]);
            if (accion < 0 || accion == 2) return;
            String justificacion = JOptionPane.showInputDialog(centro,
                    "Explique por qué " + (accion == 0 ? "aprueba" : "rechaza")
                            + " la solicitud de " + item[1] + ":",
                    "Motivo de la decisión", JOptionPane.QUESTION_MESSAGE);
            if (justificacion == null) return;
            veterinaria.util.SesionUsuario sesion =
                    veterinaria.vista.application.Application.getSesionUsuario();
            servicio.resolver(((Number)item[0]).intValue(),
                    sesion.getUsuario().getIdUsuario(), accion == 0, justificacion);
            JOptionPane.showMessageDialog(centro,
                    accion == 0
                        ? "Solicitud aprobada. La mascota " + item[1]
                            + " fue reactivada y el motivo de la decisión quedó registrado."
                        : "Solicitud rechazada. La mascota " + item[1]
                            + " continúa inactiva y el motivo de la decisión quedó registrado.",
                    "Solicitud resuelta", JOptionPane.INFORMATION_MESSAGE);
            refrescar();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(centro, "No se pudo resolver la solicitud:\n" + ex.getMessage(),
                    "Solicitud pendiente", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarFila(JPanel panel, String titulo, String detalle) {
        panel.add(Box.createVerticalStrut(12));
        JLabel nombre = new JLabel(titulo);
        nombre.setForeground(VERDE);
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 13f));
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nombre);
        if (detalle != null) {
            JLabel descripcion = new JLabel(detalle);
            descripcion.setForeground(UIManager.getColor("Label.foreground"));
            descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(Box.createVerticalStrut(3));
            panel.add(descripcion);
        }
    }

    @Override
    public void removeNotify() {
        actualizador.stop();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (actualizador != null) actualizador.start();
    }
}
