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
    private final JLabel estado = new JLabel("Cargando...");
    private final JLabel nombre = new JLabel("Cargando mis datos...");
    private final JLabel usuario = new JLabel("");
    private final JLabel rol = new JLabel("");
    private final JLabel dni = new JLabel("—");
    private final JLabel telefono = new JLabel("—");
    private final JLabel direccion = new JLabel("—");
    private final JLabel correo = new JLabel("—");
    private final JLabel error = new JLabel(" ");

    public MiPerfilDialog(Window owner, SesionUsuario sesion) {
        super(owner, "Mi perfil", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construir();
        if (sesion == null || sesion.getUsuario() == null
                || sesion.getUsuario().getIdUsuario() == null) {
            mostrarError("No hay una sesión activa.");
        } else {
            cargarPerfil(sesion.getUsuario().getIdUsuario());
        }
    }

    private void construir() {
        JPanel base = new JPanel(new BorderLayout(0, 14));
        base.setBorder(BorderFactory.createEmptyBorder(18, 22, 15, 22));

        JLabel encabezado = new JLabel("Mi perfil");
        encabezado.setFont(encabezado.getFont().deriveFont(Font.BOLD, 19f));
        base.add(encabezado, BorderLayout.NORTH);

        JPanel tarjeta = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    Color c = UIManager.getColor("App.surface.background");
                    g2.setColor(c == null ? new Color(37, 49, 61) : c);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(17, 17, 17, 17));

        JPanel cabecera = new JPanel(new BorderLayout(0, 6));
        cabecera.setOpaque(false);
        estado.setForeground(new Color(130, 214, 167));
        estado.setFont(estado.getFont().deriveFont(Font.BOLD, 12f));
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 19f));
        JPanel identificacion = new JPanel(new GridLayout(3, 1, 0, 3));
        identificacion.setOpaque(false);
        identificacion.add(estado);
        identificacion.add(usuario);
        identificacion.add(nombre);
        cabecera.add(identificacion, BorderLayout.CENTER);
        cabecera.add(rol, BorderLayout.SOUTH);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        JPanel datos = new JPanel(new BorderLayout(0, 12));
        datos.setOpaque(false);
        JLabel titulo = new JLabel("Datos personales");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));
        datos.add(titulo, BorderLayout.NORTH);
        JPanel campos = new JPanel(new GridLayout(2, 2, 24, 22));
        campos.setOpaque(false);
        campos.add(campo("DNI", dni));
        campos.add(campo("Teléfono", telefono));
        campos.add(campo("Dirección", direccion));
        campos.add(campo("Correo electrónico", correo));
        datos.add(campos, BorderLayout.CENTER);
        tarjeta.add(datos, BorderLayout.CENTER);
        base.add(tarjeta, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        error.setForeground(new Color(222, 115, 115));
        abajo.add(error, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        JButton cerrar = new JButton("Cerrar");
        veterinaria.util.ui.EstiloHibrido.botonesSecundarios(cerrar);
        cerrar.addActionListener(e -> dispose());
        acciones.add(cerrar);
        abajo.add(acciones, BorderLayout.EAST);
        base.add(abajo, BorderLayout.SOUTH);

        add(base);
        setMinimumSize(new Dimension(575, 410));
        setSize(590, 420);
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private static JPanel campo(String titulo, JLabel valor) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel etiqueta = new JLabel(titulo);
        Color muted = UIManager.getColor("App.muted.foreground");
        if (muted != null) etiqueta.setForeground(muted);
        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);
        return panel;
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
                    estado.setText(perfil.activo() ? "●  Activo" : "●  Inactivo");
                    estado.setForeground(perfil.activo()
                            ? new Color(130, 214, 167) : new Color(235, 141, 141));
                    usuario.setText("Usuario " + perfil.usuario());
                    String completo = (perfil.nombre() + " " + perfil.apellido()).trim();
                    nombre.setText(completo.isBlank() ? perfil.usuario() : completo);
                    rol.setText(perfil.rol());
                    dni.setText(mostrar(perfil.dni()));
                    telefono.setText(mostrar(perfil.telefono()));
                    direccion.setText(mostrar(perfil.direccion()));
                    correo.setText(mostrar(perfil.correo()));
                    error.setText(" ");
                } catch (Exception ex) {
                    mostrarError("No se pudieron cargar tus datos. Revisá la conexión.");
                }
            }
        }.execute();
    }

    private void mostrarError(String mensaje) {
        estado.setText("No disponible");
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
