package veterinaria.vista.application.form;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.persistence.EntityManager;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.JPAUtil;
import veterinaria.util.SesionUsuario;

/**
 * Ficha de consulta de la cuenta autenticada; no permite administrar
 * ni visualizar usuarios ajenos y no expone contraseñas.
 */
public final class MiPerfilDialog extends JDialog {
    private final EstadoBadge estado = new EstadoBadge();
    private final JLabel nombre = new JLabel("Cargando mis datos...");
    private final JLabel usuario = new JLabel("");
    private final JLabel rol = new JLabel("");
    private final JLabel dni = new JLabel("—");
    private final JLabel telefono = new JLabel("—");
    private final JLabel direccion = new JLabel("—");
    private final JLabel correo = new JLabel("—");
    private final JLabel error = new JLabel(" ");

    public MiPerfilDialog(Window owner, SesionUsuario sesion) {
        super(owner, "Ficha de usuario", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construir();
        if (sesion == null || sesion.getUsuario() == null
                || sesion.getUsuario().getIdUsuario() == null) {
            mostrarError("No hay una sesión activa.");
        } else {
            cargarPerfil(sesion.getUsuario().getIdUsuario());
        }
    }

    /**
     * Diseño compacto de la ficha del compañero, sin JScrollPane.
     * Conserva los mismos datos provenientes de la sesión autenticada.
     */
    private void construir() {
        JPanel base = new JPanel(new BorderLayout(0, 12));
        base.setBorder(BorderFactory.createEmptyBorder(15, 20, 13, 20));

        // Primera fila: etiqueta de estado y nombre de usuario.
        JPanel filaEstado = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaEstado.setOpaque(false);
        filaEstado.add(estado);
        Color muted = UIManager.getColor("App.muted.foreground");
        if (muted != null) usuario.setForeground(muted);
        filaEstado.add(usuario);
        filaEstado.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        filaEstado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));

        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 18f));
        nombre.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        rol.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        JPanel identidad = new JPanel();
        identidad.setOpaque(false);
        identidad.setLayout(new javax.swing.BoxLayout(identidad, javax.swing.BoxLayout.Y_AXIS));
        identidad.add(filaEstado);
        identidad.add(javax.swing.Box.createVerticalStrut(5));
        identidad.add(nombre);
        identidad.add(javax.swing.Box.createVerticalStrut(2));
        identidad.add(rol);
        base.add(identidad, BorderLayout.NORTH);

        // Tarjeta diferenciada: título y datos organizados como en la referencia.
        JPanel tarjeta = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    Color fondo = UIManager.getColor("App.surface.background");
                    g2.setColor(fondo == null ? new Color(37, 49, 61) : fondo);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(13, 16, 14, 16));

        JLabel tituloDatos = new JLabel("Datos personales");
        tituloDatos.setFont(tituloDatos.getFont().deriveFont(Font.BOLD, 14f));
        tarjeta.add(tituloDatos, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(3, 2, 18, 9));
        campos.setOpaque(false);
        campos.add(campo("DNI", dni));
        campos.add(campo("Teléfono", telefono));
        campos.add(campo("Dirección", direccion));
        campos.add(espacio());
        campos.add(campo("Correo electrónico", correo));
        campos.add(espacio());
        tarjeta.add(campos, BorderLayout.CENTER);
        base.add(tarjeta, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(8, 0));
        pie.setOpaque(false);
        error.setForeground(new Color(222, 115, 115));
        pie.add(error, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        JButton cerrar = new JButton("Cerrar");
        veterinaria.util.ui.EstiloHibrido.botonesPrimarios(cerrar);
        cerrar.setPreferredSize(new Dimension(96, 34));
        cerrar.addActionListener(e -> dispose());
        acciones.add(cerrar);
        pie.add(acciones, BorderLayout.EAST);
        base.add(pie, BorderLayout.SOUTH);

        setContentPane(base);
        // pack() respeta fuentes/escalado del sistema y evita barras innecesarias.
        pack();
        setSize(Math.max(640, getWidth()), Math.max(385, getHeight()));
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private static JPanel espacio() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private static JPanel campo(String titulo, JLabel valor) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel etiqueta = new JLabel(titulo);
        Color muted = UIManager.getColor("App.muted.foreground");
        if (muted != null) etiqueta.setForeground(muted);
        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);
        return panel;
    }

    /** Chip real de estado; no usa glifos que aparecen como cuadrados en Windows. */
    private static final class EstadoBadge extends JLabel {
        private boolean activo;
        EstadoBadge() {
            super("Cargando...");
            setOpaque(false);
            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setBorder(BorderFactory.createEmptyBorder(3, 11, 3, 11));
        }

        void setActivo(boolean valor) {
            activo = valor;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(activo ? new Color(29, 76, 59) : new Color(88, 53, 58));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight());
            } finally {
                g2.dispose();
            }
            setForeground(activo ? new Color(146, 233, 185) : new Color(242, 167, 167));
            super.paintComponent(g);
        }
    }

    /**
     * La identidad consultada procede exclusivamente del usuario en sesión,
     * nunca de una fila seleccionada en Gestión de Usuarios.
     * Todas las asociaciones se cargan antes de cerrar EntityManager.
     */
    private void cargarPerfil(int idSesion) {
        new SwingWorker<Ficha, Void>() {
            @Override protected Ficha doInBackground() {
                EntityManager em = JPAUtil.getEntityManager();
                try {
                    Usuario actual = em.createQuery(
                        "SELECT u FROM Usuario u LEFT JOIN FETCH u.persona "
                        + "LEFT JOIN FETCH u.rol WHERE u.idUsuario = :id", Usuario.class)
                        .setParameter("id", idSesion)
                        .getResultStream().findFirst().orElse(null);
                    if (actual == null) return null;
                    Persona p = actual.getPersona();
                    return new Ficha(
                            dato(actual.getNombreUsuario()),
                            p == null ? "" : dato(p.getNombre()),
                            p == null ? "" : dato(p.getApellido()),
                            p == null ? "" : dato(p.getDni()),
                            p == null ? "" : dato(p.getTelefono()),
                            p == null ? "" : dato(p.getDireccion()),
                            dato(actual.getEmail()),
                            actual.getRol() == null ? "" : dato(actual.getRol().getNombreRol()),
                            actual.isActivo());
                } finally {
                    if (em != null) em.close();
                }
            }

            @Override protected void done() {
                try {
                    Ficha perfil = get();
                    if (perfil == null) {
                        mostrarError("No se pudo encontrar tu cuenta.");
                        return;
                    }
                    estado.setText(perfil.activo() ? "Activo" : "Inactivo");
                    estado.setActivo(perfil.activo());
                    usuario.setText("Usuario " + perfil.usuario());
                    String completo = (perfil.nombre() + " " + perfil.apellido()).trim();
                    nombre.setText(completo.isBlank() ? perfil.usuario() : completo);
                    rol.setText(perfil.rol());
                    dni.setText(mostrar(perfil.dni()));
                    telefono.setText(mostrar(perfil.telefono()));
                    direccion.setText(mostrar(perfil.direccion()));
                    correo.setText(mostrar(perfil.correo()));
                    dni.setToolTipText(dni.getText());
                    telefono.setToolTipText(telefono.getText());
                    direccion.setToolTipText(direccion.getText());
                    correo.setToolTipText(correo.getText());
                    error.setText(" ");
                } catch (Exception ex) {
                    mostrarError("No se pudieron cargar tus datos. Revisá la conexión.");
                }
            }
        }.execute();
    }

    private void mostrarError(String mensaje) {
        estado.setText("No disponible");
        estado.setActivo(false);
        nombre.setText("Mi perfil");
        error.setText(mensaje);
    }

    private static String dato(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String mostrar(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private record Ficha(String usuario, String nombre, String apellido,
            String dni, String telefono, String direccion, String correo,
            String rol, boolean activo) {}
}
