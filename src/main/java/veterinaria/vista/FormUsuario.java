package veterinaria.vista;

import java.awt.event.ActionEvent;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.UsuarioControlador;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.PersonaDAO;
import veterinaria.persistencia.RolDAO;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.persistencia.UsuarioRolDAO;
import veterinaria.util.ManejoTablas;
import veterinaria.util.Validaciones;
import veterinaria.vista.application.Application;
import veterinaria.util.PermisoUI;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.ReporteSelectorUtil;

public class FormUsuario extends javax.swing.JPanel {

    private boolean ajustandoSeleccionUsuario;
    private Boolean ultimaSeleccionVisualUsuario;
    private String ultimoEstiloAccionUsuario = "";
    private java.awt.CardLayout vistasUsuario;
    private javax.swing.JPanel panelVistasUsuario;
    private javax.swing.JLabel tituloEditorUsuario;
    private final javax.swing.JButton btnCancelarSeleccion = new javax.swing.JButton("Cancelar");
    private final javax.swing.JButton btnVerFicha = new javax.swing.JButton("Ver ficha");
    private final javax.swing.JButton btnLimpiarBusqueda = new javax.swing.JButton("Limpiar");
    private javax.swing.JButton btnVerContrasena;
    private char caracterOcultoContrasena;

    private javax.swing.JLabel visorFoto;
    private java.io.File archivoSeleccionado; // Guardará temporalmente el archivo elegido de la PC
    private final Map<Integer, String> rolesMap = new HashMap<>();
    private final RolDAO rolDAO = new RolDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final UsuarioRolDAO usuarioRolDAO = new UsuarioRolDAO();
    private final PersonaDAO personaDAO = new PersonaDAO();
    private final Validaciones validar = new Validaciones();
    private Persona personaUsuario = new Persona();
    private Usuario usuario = new Usuario();
    private Rol rolUsuario = new Rol();
    private final UsuarioControlador operandoUsuario = new UsuarioControlador();

    // Snapshot para confirmar cambios al editar
    private String originalNombre;
    private String originalApellido;
    private String originalDni;
    private String originalTelefono;
    private String originalDireccion;
    private String originalEmail;
    private String originalNombreUsuario;
    private String originalRolNombre;
    private ManejoTablas operarTablas = new ManejoTablas();
    private boolean rolesCargados = false;
    private final javax.swing.JComboBox<String> filtroEstado = new javax.swing.JComboBox<>(new String[]{"Todos", "Activos", "Inactivos"});

    public FormUsuario() {
        initComponents();
        veterinaria.util.ui.EstiloHibrido.superficies(jpDatosPersonales, jpAcceso, jpDatosCuenta, jpListaUsuarios);
        veterinaria.util.ui.EstiloHibrido.titulos(lbDatosPersonales, lbDatosDeCuenta, lblGestionUsuarios);
        // Centramos cualquier contenido o icono que se cargue
        // Centramos cualquier contenido o icono que se cargue
       // lbFotoPerfil.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        //lbFotoPerfil.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        inicializarTooltips();
        configurarVisibilidadContrasena();
        instalarPanelFotografia();

        PermisoUI.aplicar(
                this);
        initListeners();

        instalarFiltroEstado();
        organizarVistasUsuario();
        actualizarForm();
        instalarPaginacionCompartida();
        aplicarEstiloVisualUsuarios();
        configurarColumnasCompactasUsuarios();
        // En Usuarios solo se permite una selección a la vez, incluso
        // cuando el usuario marca directamente la casilla de otra fila.
        table.getModel().addTableModelListener(e -> {
            if (ajustandoSeleccionUsuario || e.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || e.getColumn() != 0 || e.getFirstRow() < 0) return;
            int fila = e.getFirstRow();
            javax.swing.table.TableModel modelo = table.getModel();
            if (!Boolean.TRUE.equals(modelo.getValueAt(fila, 0))) return;
            ajustandoSeleccionUsuario = true;
            try {
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    if (i != fila && Boolean.TRUE.equals(modelo.getValueAt(i, 0))) {
                        ((javax.swing.table.DefaultTableModel) modelo).setValueAt(false, i, 0);
                    }
                }
            } finally {
                ajustandoSeleccionUsuario = false;
            }
        });

        // Los cinco botones de acciones permanecen visibles; su disponibilidad
        // sigue la selección, permisos y estado del usuario.
        actualizarAccionesSeleccionUsuario();

        btnImprimirLista.setVisible(
                true);
        btnImprimirLista.setEnabled(
                true);
        btnImprimirLista.addActionListener(e
                -> imprimirListaUsuarios());

        // Botón legacy (si existe en el diseño): lo ocultamos para evitar duplicidad
        if (btnImprimir1
                != null) {
            btnImprimir1.setVisible(false);
            btnImprimir1.setEnabled(false);
        }

        Application.actualizarEstadoUsuario(
                "InicioFormularioUsuario");
    }


    /** Botón externo al campo, junto a Contraseña. */
    private void configurarVisibilidadContrasena() {
        caracterOcultoContrasena = txtContraseña.getEchoChar();
        // Ícono de ojo dibujado: evita el carácter Unicode que aparece como cuadrado.
        // Botón independiente, sin fondo ni borde, visualmente como el del Login.
        btnVerContrasena.setText("");
        btnVerContrasena.setIcon(crearIconoOjo(false));
        btnVerContrasena.setFocusable(false);
        btnVerContrasena.setContentAreaFilled(false);
        btnVerContrasena.setBorderPainted(false);
        btnVerContrasena.setOpaque(false);
        btnVerContrasena.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btnVerContrasena.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnVerContrasena.setToolTipText("Mostrar contraseña");
        btnVerContrasena.addActionListener(e -> {
            boolean visible = txtContraseña.getEchoChar() != 0;
            txtContraseña.setEchoChar(visible ? (char) 0 : caracterOcultoContrasena);
            btnVerContrasena.setIcon(crearIconoOjo(visible));
            btnVerContrasena.setToolTipText(visible ? "Ocultar contraseña" : "Mostrar contraseña");
        });
    }

    /** Ojo vectorial pequeño, legible en FlatLaf oscuro y claro. */
    private javax.swing.Icon crearIconoOjo(boolean ocultar) {
        return new javax.swing.Icon() {
            @Override public int getIconWidth() { return 18; }
            @Override public int getIconHeight() { return 18; }
            @Override public void paintIcon(java.awt.Component c, java.awt.Graphics graphics,
                    int x, int y) {
                java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    java.awt.Color color = javax.swing.UIManager.getColor("Label.foreground");
                    g.setColor(color != null ? color : java.awt.Color.GRAY);
                    g.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_ROUND,
                            java.awt.BasicStroke.JOIN_ROUND));
                    java.awt.geom.Path2D ojo = new java.awt.geom.Path2D.Float();
                    ojo.moveTo(x + 1, y + 9);
                    ojo.curveTo(x + 5, y + 2, x + 13, y + 2, x + 17, y + 9);
                    ojo.curveTo(x + 13, y + 16, x + 5, y + 16, x + 1, y + 9);
                    g.draw(ojo);
                    g.drawOval(x + 7, y + 7, 4, 4);
                    if (ocultar) {
                        g.drawLine(x + 2, y + 16, x + 16, y + 2);
                    }
                } finally {
                    g.dispose();
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lblGestionUsuarios = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        lbUsuarioBusqueda = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        btnImprimir = new javax.swing.JButton();
        jpCamposFormularioUsuario = new javax.swing.JPanel();
        jpDatosPersonales = new javax.swing.JPanel();
        lbDatosPersonales = new javax.swing.JLabel();
        lbNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lbApellido = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        lbCuilCuit = new javax.swing.JLabel();
        txtDNI = new javax.swing.JTextField();
        lbTelefono = new javax.swing.JLabel();
        txtTelefono = new javax.swing.JTextField();
        lbDireccion = new javax.swing.JLabel();
        txtDireccion = new javax.swing.JTextField();
        jSeparator7 = new javax.swing.JSeparator();
        jpAcceso = new javax.swing.JPanel();
        lbNivelDeAcceso = new javax.swing.JLabel();
        lbRol = new javax.swing.JLabel();
        jcbRol = new javax.swing.JComboBox<>();
        jSeparator3 = new javax.swing.JSeparator();
        jpDatosCuenta = new javax.swing.JPanel();
        lbDatosDeCuenta = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        lbNombreUsuario = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        lbEmail = new javax.swing.JLabel();
        lbDireccion1 = new javax.swing.JLabel();
        txtContraseña = new javax.swing.JPasswordField();
        btnVerContrasena = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JSeparator();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jpListaUsuarios = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        scroll = new javax.swing.JScrollPane();
        table = new veterinaria.vista.table.AutoTable();
        btnImprimir1 = new javax.swing.JButton();
        btnImprimirLista = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JSeparator();

        setMinimumSize(new java.awt.Dimension(850, 600));
        setPreferredSize(new java.awt.Dimension(850, 600));

        lblGestionUsuarios.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblGestionUsuarios.setText("Gestión de Usuarios");

        lbUsuarioBusqueda.setText("BUSCAR USUARIO");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });

        btnEditar.setText("Editar");
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });

        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(lblGestionUsuarios)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbUsuarioBusqueda)
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(lbBuscar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(btnImprimir, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap())))
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                .addComponent(lblGestionUsuarios)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(btnImprimir)
                        .addGap(9, 9, 9)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnNuevo)
                            .addComponent(btnEditar)
                            .addComponent(btnEliminar))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbUsuarioBusqueda)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbBuscar)
                            .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)))
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jpCamposFormularioUsuario.setMinimumSize(new java.awt.Dimension(848, 180));
        jpCamposFormularioUsuario.setPreferredSize(new java.awt.Dimension(848, 340));

        jpDatosPersonales.setMinimumSize(new java.awt.Dimension(600, 76));

        lbDatosPersonales.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbDatosPersonales.setText("Datos Personales:");

        lbNombre.setText("Nombre: *");

        txtNombre.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtNombreKeyTyped(evt);
            }
        });

        lbApellido.setText("Apellido: *");

        txtApellido.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtApellidoKeyTyped(evt);
            }
        });

        lbCuilCuit.setText("DNI: *");

        txtDNI.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtDNIKeyTyped(evt);
            }
        });

        lbTelefono.setText("Teléfono: *");

        txtTelefono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtTelefonoKeyTyped(evt);
            }
        });

        lbDireccion.setText("Dirección: *");

        javax.swing.GroupLayout jpDatosPersonalesLayout = new javax.swing.GroupLayout(jpDatosPersonales);
        jpDatosPersonales.setLayout(jpDatosPersonalesLayout);
        jpDatosPersonalesLayout.setHorizontalGroup(
            jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbDatosPersonales)
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbApellido)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbCuilCuit)
                            .addComponent(txtDNI, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbTelefono)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbDireccion)
                            .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jSeparator7, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jpDatosPersonalesLayout.setVerticalGroup(
            jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbDireccion)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbApellido)
                            .addComponent(lbCuilCuit)
                            .addComponent(lbTelefono))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtDNI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbDatosPersonales)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbNombre)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(28, 28, 28)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jpAcceso.setMinimumSize(new java.awt.Dimension(246, 100));
        jpAcceso.setPreferredSize(new java.awt.Dimension(246, 100));

        lbNivelDeAcceso.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbNivelDeAcceso.setText("Nivel de acceso:");

        lbRol.setText("Rol: *");

        javax.swing.GroupLayout jpAccesoLayout = new javax.swing.GroupLayout(jpAcceso);
        jpAcceso.setLayout(jpAccesoLayout);
        jpAccesoLayout.setHorizontalGroup(
            jpAccesoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAccesoLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jpAccesoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbNivelDeAcceso)
                    .addComponent(jcbRol, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbRol))
                .addContainerGap(724, Short.MAX_VALUE))
            .addGroup(jpAccesoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator3)
                .addContainerGap())
        );
        jpAccesoLayout.setVerticalGroup(
            jpAccesoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAccesoLayout.createSequentialGroup()
                .addComponent(lbNivelDeAcceso)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbRol)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbRol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jpDatosCuenta.setMinimumSize(new java.awt.Dimension(444, 100));
        jpDatosCuenta.setPreferredSize(new java.awt.Dimension(569, 100));

        lbDatosDeCuenta.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbDatosDeCuenta.setText("Datos de cuenta:");

        lbNombreUsuario.setText("Usuario:");

        lbEmail.setText("Email: *");

        lbDireccion1.setText("Contraseña: *");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpDatosCuentaLayout = new javax.swing.GroupLayout(jpDatosCuenta);
        jpDatosCuenta.setLayout(jpDatosCuentaLayout);
        jpDatosCuentaLayout.setHorizontalGroup(
            jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosCuentaLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbDatosDeCuenta)
                    .addGroup(jpDatosCuentaLayout.createSequentialGroup()
                        .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbNombreUsuario))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbDireccion1)
                            .addGroup(jpDatosCuentaLayout.createSequentialGroup()
                                .addComponent(txtContraseña, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(btnVerContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpDatosCuentaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosCuentaLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator6))
                .addContainerGap())
        );
        jpDatosCuentaLayout.setVerticalGroup(
            jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosCuentaLayout.createSequentialGroup()
                .addComponent(lbDatosDeCuenta)
                .addGap(7, 7, 7)
                .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosCuentaLayout.createSequentialGroup()
                        .addComponent(lbNombreUsuario)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosCuentaLayout.createSequentialGroup()
                        .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbDireccion1)
                            .addComponent(lbEmail))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtContraseña, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnVerContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosCuentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar)
                    .addComponent(btnLimpiar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jpCamposFormularioUsuarioLayout = new javax.swing.GroupLayout(jpCamposFormularioUsuario);
        jpCamposFormularioUsuario.setLayout(jpCamposFormularioUsuarioLayout);
        jpCamposFormularioUsuarioLayout.setHorizontalGroup(
            jpCamposFormularioUsuarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpAcceso, javax.swing.GroupLayout.DEFAULT_SIZE, 896, Short.MAX_VALUE)
            .addComponent(jpDatosCuenta, javax.swing.GroupLayout.DEFAULT_SIZE, 896, Short.MAX_VALUE)
            .addComponent(jpDatosPersonales, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jpCamposFormularioUsuarioLayout.setVerticalGroup(
            jpCamposFormularioUsuarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCamposFormularioUsuarioLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpDatosPersonales, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpAcceso, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatosCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Lista de Usuarios");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Nombre", "Apellido", "DNI", "Teléfono", "Dirección", "Rol", "Usuario", "E-mail"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return columnIndex >= 0 && columnIndex < types.length
                        ? types[columnIndex] : String.class;
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex >= 0 && columnIndex < canEdit.length
                        && canEdit[columnIndex];
            }
        });
        table.setMinimumSize(new java.awt.Dimension(848, 220));
        // La altura de la JTable debe depender de todas las filas, no de un valor fijo.
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);
        scroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        btnImprimir1.setText("Imprimir");

        btnImprimirLista.setText("Imprimir Lista");

        javax.swing.GroupLayout jpListaUsuariosLayout = new javax.swing.GroupLayout(jpListaUsuarios);
        jpListaUsuarios.setLayout(jpListaUsuariosLayout);
        jpListaUsuariosLayout.setHorizontalGroup(
            jpListaUsuariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 896, Short.MAX_VALUE)
            .addGroup(jpListaUsuariosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnImprimirLista)
                .addGap(16, 16, 16))
            .addComponent(jSeparator8)
            .addGroup(jpListaUsuariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpListaUsuariosLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(btnImprimir1)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        jpListaUsuariosLayout.setVerticalGroup(
            jpListaUsuariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaUsuariosLayout.createSequentialGroup()
                .addGroup(jpListaUsuariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(jpListaUsuariosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnImprimirLista)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
            .addGroup(jpListaUsuariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpListaUsuariosLayout.createSequentialGroup()
                    .addGap(0, 217, Short.MAX_VALUE)
                    .addComponent(btnImprimir1)
                    .addGap(0, 218, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jpCamposFormularioUsuario, javax.swing.GroupLayout.DEFAULT_SIZE, 896, Short.MAX_VALUE)
                    .addComponent(jpListaUsuarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpCamposFormularioUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 390, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaUsuarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jpCamposFormularioUsuario.getAccessibleContext().setAccessibleName("");
        jpCamposFormularioUsuario.getAccessibleContext().setAccessibleDescription("");
    }// </editor-fold>//GEN-END:initComponents

    private veterinaria.util.PaginacionTablas paginacionCompartida;

    /**
     * Barra y acciones como en la referencia, con los componentes y listeners
     * originales de la versión de Noelia. No modifica los métodos del negocio.
     */
    private void instalarFiltroEstado() {
        filtroEstado.setSelectedItem("Activos");
        filtroEstado.addActionListener(e -> cargarUsuariosEnTabla());

        jpHeader.removeAll();
        jpHeader.setOpaque(false);
        jpHeader.setLayout(new java.awt.BorderLayout(0, 10));
        jpHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 2, 7, 2));

        javax.swing.JPanel titulo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 7));
        titulo.setOpaque(false);
        titulo.add(lblGestionUsuarios, java.awt.BorderLayout.NORTH);
        jSeparator2.setVisible(false);
        jpHeader.add(titulo, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel filtros = new javax.swing.JPanel(new java.awt.BorderLayout(12, 0));
        filtros.setOpaque(false);
        filtros.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        javax.swing.JPanel izquierda = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 9, 0));
        izquierda.setOpaque(false);
        lbUsuarioBusqueda.setText("Buscar usuario:");
        izquierda.add(lbUsuarioBusqueda);
        // Se mantiene el ícono original de búsqueda.
        lbBuscar.setVisible(false);
        txtBusqueda.setPreferredSize(new java.awt.Dimension(415, 34));
        izquierda.add(txtBusqueda);
        btnLimpiarBusqueda.setToolTipText("Limpiar la búsqueda y volver a la primera página");
        btnLimpiarBusqueda.addActionListener(e -> {
            if (paginacionCompartida != null) paginacionCompartida.limpiarBusqueda();
            else txtBusqueda.setText("");
        });
        izquierda.add(btnLimpiarBusqueda);
        izquierda.add(new javax.swing.JLabel("Estado:"));
        filtroEstado.setPreferredSize(new java.awt.Dimension(135, 34));
        izquierda.add(filtroEstado);
        filtros.add(izquierda, java.awt.BorderLayout.WEST);

        btnVerFicha.addActionListener(e -> mostrarFichaUsuarioSeleccionado());
        btnVerFicha.setToolTipText("Mostrar los datos del usuario seleccionado");
        btnCancelarSeleccion.setToolTipText("Quitar la selección");
        btnCancelarSeleccion.addActionListener(e -> cancelarSeleccionUsuario());
        btnCancelarSeleccion.setText("");

        // Cinco acciones en cinco columnas reales: misma altura, separación
        // y coordenadas de fila. La última comparte columna con Nuevo usuario.
        javax.swing.JPanel filas = crearBarraUsuariosAlineada(
                filtros, btnNuevo, btnCancelarSeleccion,
                btnVerFicha, btnEditar, btnEliminar, btnImprimir, btnImprimirLista);
        jpHeader.add(filas, java.awt.BorderLayout.CENTER);
        jSeparator1.setVisible(false);
        jpHeader.revalidate();
        jpHeader.repaint();
    }

    /** Barra compacta: filtros arriba y cinco acciones alineadas a la derecha. */
    private static javax.swing.JPanel crearBarraUsuariosAlineada(
            javax.swing.JPanel filtros,
            javax.swing.JButton nuevo,
            javax.swing.JButton cancelarSeleccion,
            javax.swing.JButton verFicha,
            javax.swing.JButton editar,
            javax.swing.JButton desactivar,
            javax.swing.JButton imprimirFicha,
            javax.swing.JButton imprimirListado) {
        javax.swing.JPanel filas = new javax.swing.JPanel();
        filas.setOpaque(false);
        filas.setLayout(new javax.swing.BoxLayout(filas, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JPanel superior = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
        superior.setOpaque(false);
        nuevo.setPreferredSize(new java.awt.Dimension(150, 34));
        superior.add(filtros, java.awt.BorderLayout.CENTER);
        superior.add(nuevo, java.awt.BorderLayout.EAST);
        superior.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        superior.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 36));
        filas.add(superior);
        filas.add(javax.swing.Box.createVerticalStrut(8));

        javax.swing.JPanel inferior = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        inferior.setOpaque(false);
        cancelarSeleccion.setPreferredSize(new java.awt.Dimension(34, 34));
        inferior.add(cancelarSeleccion);
        for (javax.swing.JButton boton : new javax.swing.JButton[]{
                verFicha, editar, desactivar, imprimirFicha, imprimirListado}) {
            int ancho = boton == imprimirListado ? 140
                    : boton == imprimirFicha ? 135
                    : boton == desactivar ? 115
                    : boton == verFicha ? 105 : 90;
            boton.setPreferredSize(new java.awt.Dimension(ancho, 34));
            inferior.add(boton);
        }
        inferior.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        inferior.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 36));
        filas.add(inferior);
        return filas;
    }

    /** Reutiliza los tres paneles generados por NetBeans, pero evita el alto fijo de 390px. */
    private void organizarVistasUsuario() {
        removeAll();
        setLayout(new java.awt.BorderLayout(0, 8));
        panelVistasUsuario = new javax.swing.JPanel();
        panelVistasUsuario.setOpaque(false);
        vistasUsuario = new java.awt.CardLayout();
        panelVistasUsuario.setLayout(vistasUsuario);
        panelVistasUsuario.add(jpListaUsuarios, "LISTA");
        panelVistasUsuario.add(jpCamposFormularioUsuario, "EDITOR");
        add(jpHeader, java.awt.BorderLayout.NORTH);
        add(panelVistasUsuario, java.awt.BorderLayout.CENTER);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }

    private void mostrarVistaUsuario(boolean editor) {
        jpHeader.setVisible(!editor);
        if (vistasUsuario != null) {
            vistasUsuario.show(panelVistasUsuario, editor ? "EDITOR" : "LISTA");
        }
        revalidate();
        repaint();
    }

    /** Iconos SVG y estilos visuales sin cambiar los ActionListener de VetPRO. */
    private void aplicarEstiloVisualUsuarios() {
        veterinaria.util.ui.EstiloHibrido.titulos(lblGestionUsuarios, jLabel2);
        veterinaria.util.ui.EstiloHibrido.botonesPrimarios(btnNuevo, btnGuardar);
        veterinaria.util.ui.EstiloHibrido.botonesSecundarios(
                btnVerFicha, btnEditar, btnImprimir, btnImprimirLista);
        veterinaria.util.ui.EstiloHibrido.botonesPeligro(btnEliminar);
        veterinaria.util.ui.EstiloHibrido.botonesSutiles(
                btnLimpiarBusqueda, btnCancelarSeleccion, btnCancelar, btnLimpiar);

        lblGestionUsuarios.setText("Gestión de usuarios");
        btnNuevo.setText("Nuevo usuario");
        btnVerFicha.setText("Ver ficha");
        btnEditar.setText("Editar");
        btnEliminar.setText("Desactivar");
        btnImprimir.setText("Imprimir ficha");
        btnImprimirLista.setText("Imprimir listado");

        iconoUsuario(btnNuevo, "nuevo_usuario.svg");
        iconoUsuario(btnVerFicha, "ver_recibo.svg");
        iconoUsuario(btnEditar, "editar_usuario.svg");
        iconoUsuario(btnEliminar, "desactivar_usuario.svg");
        iconoUsuario(btnImprimir, "imprimir_recibo.svg");
        iconoUsuario(btnImprimirLista, "imprimir_recibo.svg");
        iconoUsuario(btnLimpiarBusqueda, "limpiar_usuario.svg");
        iconoUsuario(btnCancelarSeleccion, "limpiar_usuario.svg");
        btnCancelarSeleccion.setToolTipText("Quitar selección del usuario");

        txtBusqueda.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Nombre, apellido, DNI, usuario o rol");
        for (javax.swing.JButton boton : new javax.swing.JButton[]{
                btnNuevo, btnVerFicha, btnEditar, btnEliminar, btnImprimir,
                btnImprimirLista, btnLimpiarBusqueda, btnCancelarSeleccion}) {
            boton.setFocusable(false);
            int minWidth = boton == btnNuevo ? 138
                    : boton == btnImprimirLista ? 152
                    : boton == btnImprimir ? 140
                    : boton == btnVerFicha ? 115
                    : boton == btnEliminar ? 122
                    : boton == btnLimpiarBusqueda ? 95
                    : boton == btnCancelarSeleccion ? 35 : 92;
            boton.setPreferredSize(new java.awt.Dimension(
                    Math.max(minWidth, boton.getPreferredSize().width), 34));
        }
        // Conserva columnas, casillas, filtros, paginación y badges.
        veterinaria.util.ui.EstiloHibrido.tablaClientes(table, 9);
        // Hace coincidir el fondo de todas las celdas con el degradado del checkbox.
        table.putClientProperty("vetpro.gradienteCompanero", Boolean.TRUE);
    }

    /**
     * Vista compacta de cinco columnas como la referencia híbrida.
     * El modelo conserva las diez columnas para los informes, filtros y ABM.
     * La casilla de selección sigue en la columna 0 con ancho cero.
     */
    private void configurarColumnasCompactasUsuarios() {
        javax.swing.table.TableColumnModel columnas = table.getColumnModel();
        for (int i = columnas.getColumnCount() - 1; i >= 0; i--) {
            int modelo = columnas.getColumn(i).getModelIndex();
            if (modelo != 1 && modelo != 3
                    && modelo != 6 && modelo != 7 && modelo != 9) {
                columnas.removeColumn(columnas.getColumn(i));
            }
        }
        int[] orden = {1, 7, 3, 6, 9};
        for (int i = 0; i < orden.length; i++) {
            for (int j = i; j < columnas.getColumnCount(); j++) {
                if (columnas.getColumn(j).getModelIndex() == orden[i]) {
                    columnas.moveColumn(j, i);
                    break;
                }
            }
        }
        // No hay columna de casillas visible; el modelo la conserva para las acciones.
        table.setAutoCreateColumnsFromModel(false);
        columnas.getColumn(0).setHeaderValue("Nombre completo");
        columnas.getColumn(1).setHeaderValue("Usuario");
        columnas.getColumn(2).setHeaderValue("DNI");
        columnas.getColumn(3).setHeaderValue("Rol");
        columnas.getColumn(4).setHeaderValue("Estado");
        int[] ancho = {290, 180, 145, 190, 95};
        for (int i = 0; i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setPreferredWidth(ancho[i]);
        }
        javax.swing.table.TableCellRenderer original =
                table.getDefaultRenderer(Object.class);
        columnas.getColumn(0).setCellRenderer((tabla, value, selected, focus, row, column) -> {
            int filaModelo = tabla.convertRowIndexToModel(row);
            Object apellido = tabla.getModel().getValueAt(filaModelo, 2);
            String nombreCompleto = String.valueOf(value == null ? "" : value)
                    + " " + String.valueOf(apellido == null ? "" : apellido);
            return original.getTableCellRendererComponent(
                    tabla, nombreCompleto.trim(), selected, focus, row, column);
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    int fila = table.rowAtPoint(e.getPoint());
                    if (fila >= 0) {
                        int filaModelo = table.convertRowIndexToModel(fila);
                        javax.swing.table.TableModel modelo = table.getModel();
                        for (int i = 0; i < modelo.getRowCount(); i++) {
                            boolean marcar = i == filaModelo;
                            if (!java.lang.Boolean.valueOf(marcar).equals(modelo.getValueAt(i, 0))) {
                                ((javax.swing.table.DefaultTableModel) modelo).setValueAt(marcar, i, 0);
                            }
                        }
                        actualizarAccionesSeleccionUsuario();
                    }
                }
            }
        });
        table.getTableHeader().repaint();
    }

    /** Acceso por índice del modelo; la vista puede ocultar o reordenar columnas. */
    private Object datoUsuario(int filaVista, int columnaModelo) {
        if (filaVista < 0) return null;
        return table.getModel().getValueAt(table.convertRowIndexToModel(filaVista), columnaModelo);
    }

    private void iconoUsuario(javax.swing.JButton boton, String archivo) {
        String carpeta = "veterinaria/icon/svg/";
        // El ojo y la impresora ya existen: no se duplican sus recursos.
        try {
            boton.setIcon(new com.formdev.flatlaf.extras.FlatSVGIcon(carpeta + archivo, 0.70f));
            boton.setIconTextGap(6);
            boton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        } catch (RuntimeException e) {
            boton.setToolTipText("Acción: " + boton.getText());
        }
    }

    /** Vista de solo lectura, sin exponer contraseñas ni modificar el usuario. */
    private void mostrarFichaUsuarioSeleccionado() {
        int fila = operarTablas.comprobarElementoSeleccionado(table);
        if (fila < 0) return;
        String[] campos = {"Nombre", "Apellido", "DNI", "Teléfono",
                "Dirección", "Rol", "Usuario", "E-mail", "Estado"};
        javax.swing.JPanel ficha = new javax.swing.JPanel(
                new java.awt.GridLayout(campos.length, 2, 12, 7));
        ficha.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        for (int i = 0; i < campos.length; i++) {
            javax.swing.JLabel label = new javax.swing.JLabel(campos[i] + ":");
            label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
            ficha.add(label);
            Object valor = datoUsuario(fila, i + 1);
            ficha.add(new javax.swing.JLabel(valor == null ? "—" : String.valueOf(valor)));
        }
        JOptionPane.showMessageDialog(this, ficha, "Ficha de usuario",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void instalarPaginacionCompartida() {
        // El listado queda bajo la barra de acciones: sin encabezados duplicados
        // ni botón de impresión fuera de su fila.
        jpListaUsuarios.removeAll();
        jpListaUsuarios.setLayout(new java.awt.BorderLayout(0, 4));
        jpListaUsuarios.add(scroll, java.awt.BorderLayout.CENTER);

        paginacionCompartida = veterinaria.util.GestorTablas.paginar(
                table, txtBusqueda,
                new veterinaria.util.GestorTablas.Configuracion(
                        new int[]{1, 2, 3, 6, 7}, new int[]{1, 2, 3, 6, 7}, 15));
        paginacionCompartida.activarBusquedaPorPrefijo();
        jpListaUsuarios.add(paginacionCompartida.getControles(), java.awt.BorderLayout.SOUTH);
        // La acción limpiar está en la fila de filtros; no hay una X superpuesta.
        table.getColumnModel().getColumn(0).setCellRenderer(
                new veterinaria.vista.table.TableGradientCheckBoxRenderer());
        paginacionCompartida.actualizar();
    }

    private void initListeners() {
        txtNombre.getDocument().addDocumentListener(new PersonalDataListener());
        txtApellido.getDocument().addDocumentListener(new PersonalDataListener());
        txtDNI.getDocument().addDocumentListener(new PersonalDataListener());
        txtTelefono.getDocument().addDocumentListener(new PersonalDataListener());
        txtDireccion.getDocument().addDocumentListener(new PersonalDataListener());
        // Listener único para rol: actualiza rolUsuario + habilita/deshabilita datos de cuenta
        jcbRol.addActionListener((ActionEvent e) -> {
            onRolSeleccionado();
            boolean rolOk = jcbRol.getSelectedIndex() > 0;
            habilitarDatosDeCuenta(rolOk);
            if (!rolOk) {
                limpiarCamposCuenta();
            }
        });
        jcbRol.setEnabled(false);
        txtUsuario.setEnabled(false);
        habilitarDatosDeCuenta(false);

        // En Usuarios el botón debe responder en el mismo ciclo EDT.
        // Se aplica solo a este módulo, no al resto de tablas.
        table.putClientProperty("vetpro.seleccion.rapida", Boolean.TRUE);
        veterinaria.util.GestorTablas.instalarSeleccion(table, () -> {
            boolean seleccion = veterinaria.util.GestorTablas.haySeleccion(table);
            actualizarAccionesSeleccionUsuario();
        });


        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }

    private void cancelarSeleccionUsuario() {
        javax.swing.table.TableModel modelo = table.getModel();
        ajustandoSeleccionUsuario = true;
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (Boolean.TRUE.equals(modelo.getValueAt(i, 0))) {
                    ((javax.swing.table.DefaultTableModel) modelo).setValueAt(false, i, 0);
                }
            }
        } finally {
            ajustandoSeleccionUsuario = false;
        }
        table.clearSelection();
        actualizarAccionesSeleccionUsuario();
    }

    private void seleccionarFilaDesdeClick(java.awt.event.MouseEvent evt) {
        if (evt == null) {
            return;
        }

        int viewRow = table.rowAtPoint(evt.getPoint());
        int viewCol = table.columnAtPoint(evt.getPoint());

        if (viewRow < 0) {
            return;
        }

        // Asegurar selección visual de la fila clickeada
        try {
            table.setRowSelectionInterval(viewRow, viewRow);
        } catch (Exception ignore) {
        }

        // Si el usuario clickea directamente el checkbox, no forzamos el valor (permitimos toggle)
        if (viewCol == 0) {
            SwingUtilities.invokeLater(this::actualizarBotonImprimir);
            return;
        }

        // Marcar la fila clickeada y desmarcar el resto
        try {
            for (int i = 0; i < table.getRowCount(); i++) {
                if (i != viewRow && Boolean.TRUE.equals(table.getValueAt(i, 0))) {
                    table.setValueAt(false, i, 0);
                }
            }
            table.setValueAt(true, viewRow, 0);
        } catch (Exception ignore) {
        }

        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }

    private void actualizarForm() {
        limpiarCamposFormulario();
        cargarRolesEnComboBox();
        cargarUsuariosEnTabla();
        mostrarVistaUsuario(false);
        actualizarBotonImprimir();
        revalidate();
        repaint();
    }

    private void actualizarBotonImprimir() {
        actualizarAccionesSeleccionUsuario();
    }

    private void actualizarAccionesSeleccionUsuario() {
        boolean haySeleccion = veterinaria.util.GestorTablas.haySeleccion(table);
        // Las acciones conservan su ubicación aun al desmarcar una fila.
        btnVerFicha.setVisible(true);
        btnEditar.setVisible(true);
        btnEliminar.setVisible(true);
        btnImprimir.setVisible(true);
        btnImprimirLista.setVisible(true);
        btnCancelarSeleccion.setVisible(haySeleccion);
        btnVerFicha.setEnabled(haySeleccion);
        btnNuevo.setEnabled(!haySeleccion && permisoUsuario("NUEVO"));
        btnEditar.setEnabled(haySeleccion && permisoUsuario("EDITAR"));
        btnEliminar.setEnabled(haySeleccion && permisoUsuario("ELIMINAR"));
        btnImprimir.setEnabled(haySeleccion && permisoUsuario("IMPRIMIR"));
        btnImprimirLista.setEnabled(permisoUsuario("IMPRIMIR"));
        btnCancelarSeleccion.setEnabled(haySeleccion);
        actualizarTextoEstadoSeleccionado();

        // Evitar revalidaciones de FlatLaf al cambiar de registro: solo
        // ajustar borde o clase si realmente cambiaron los valores.
        if (!java.util.Objects.equals(ultimaSeleccionVisualUsuario, haySeleccion)) {
            java.awt.Color borde = javax.swing.UIManager.getColor("Component.borderColor");
            if (borde == null) borde = new java.awt.Color(64, 82, 95);
            scroll.setBorder(javax.swing.BorderFactory.createLineBorder(
                    haySeleccion ? new java.awt.Color(238, 153, 45) : borde, 1, true));
            ultimaSeleccionVisualUsuario = haySeleccion;
        }
        String estilo = "Reactivar".equals(btnEliminar.getText()) ? "reactivar" : "desactivar";
        if (!estilo.equals(ultimoEstiloAccionUsuario)) {
            if ("reactivar".equals(estilo)) {
                veterinaria.util.ui.EstiloHibrido.botonesSecundarios(btnEliminar);
            } else {
                veterinaria.util.ui.EstiloHibrido.botonesPeligro(btnEliminar);
            }
            ultimoEstiloAccionUsuario = estilo;
        }
        // El modelo actualiza el renderer; evitar un segundo repintado
        // de toda la tabla por cada evento de selección.
    }

    /** Conserva los permisos reales del módulo al habilitar acciones. */
    private boolean permisoUsuario(String accion) {
        veterinaria.util.SesionUsuario sesion =
                veterinaria.util.SesionUsuario.getInstancia();
        return sesion == null || sesion.getUsuario() == null
                || sesion.puede("FormUsuario." + accion);
    }

    private Usuario obtenerUsuarioSeleccionadoDesdeTabla() {
        int row = operarTablas.comprobarElementoSeleccionado(table);
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            String usuarioSelec = String.valueOf(datoUsuario(row, 7));
            Usuario u = usuarioDAO.buscarPorNombreUsuario(usuarioSelec);
            if (u == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el usuario seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return u;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el usuario seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void imprimirRegistroSeleccionado() {
        Usuario u = obtenerUsuarioSeleccionadoDesdeTabla();
        if (u == null) {
            return;
        }
        ReporteSelectorUtil.Formato formato = ReporteSelectorUtil.solicitarFormatoCombo(
                this, "Configurar informe de usuario");
        if (formato == null) {
            return;
        }
        try {
            // Fallback de datos planos desde la JTable (por si falla la carga de Persona)
            int rowSel = operarTablas.comprobarElementoSeleccionado(table);
            String fNombre = (rowSel != -1) ? String.valueOf(datoUsuario(rowSel, 1)) : "";
            String fApellido = (rowSel != -1) ? String.valueOf(datoUsuario(rowSel, 2)) : "";
            String fDni = (rowSel != -1) ? String.valueOf(datoUsuario(rowSel, 3)) : "";
            String fTelefono = (rowSel != -1) ? String.valueOf(datoUsuario(rowSel, 4)) : "";
            String fDireccion = (rowSel != -1) ? String.valueOf(datoUsuario(rowSel, 5)) : "";

            // Cargar Persona vinculada (evita LazyInitializationException / proxy sin Session)
            veterinaria.entidad.Persona persona = null;
            try {
                if (u.getPersona() != null && u.getPersona().getIdPersona() != null) {
                    persona = personaDAO.obtenerPorId(u.getPersona().getIdPersona());
                }
            } catch (Exception ignore) {
                persona = null;
            }

            Rol rol = null;
            try {
                rol = usuarioRolDAO.obtenerRol(u.getIdUsuario());
            } catch (Exception ignore) {
            }
            String rolNombre = (rol != null && rol.getNombreRol() != null) ? rol.getNombreRol() : "";

            // 1. Armamos el request con toda la info ya procesada
            ReporteRequest req = new ReporteRequest()
                    .put("usuario", u)
                    .put("persona", persona)
                    .put("nombre", fNombre)
                    .put("apellido", fApellido)
                    .put("dni", fDni)
                    .put("telefono", fTelefono)
                    .put("direccion", fDireccion)
                    .put("rol", rolNombre)
                    .put("estado", u.isActivo() ? "Activo" : "Inactivo")
                    .put("FORMATO_REPORTE", formato)
                    .put("emisor", Application.getNombreApellidoUsuarioLogeado());

            // 2. 🚀 AL EJECUTOR: Corre asíncronamente con su barra de progreso modal
            veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.USUARIO_REGISTRO, req);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo preparar la información para el reporte.\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirListaUsuarios() {
        javax.swing.JComboBox<String> alcance = new javax.swing.JComboBox<>(
                new String[]{"Todos los usuarios", "Solo activos", "Solo inactivos"});
        javax.swing.JComboBox<String> formato = new javax.swing.JComboBox<>(new String[]{"PDF", "Excel (XLS)"});
        javax.swing.JPanel opciones = new javax.swing.JPanel(new java.awt.GridLayout(2, 2, 10, 10));
        opciones.add(new javax.swing.JLabel("Usuarios a incluir:"));
        opciones.add(alcance);
        opciones.add(new javax.swing.JLabel("Formato:"));
        opciones.add(formato);
        if (JOptionPane.showConfirmDialog(this, opciones, "Configurar informe de usuarios",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String estado = alcance.getSelectedIndex() == 1 ? "ACTIVO" :
                alcance.getSelectedIndex() == 2 ? "INACTIVO" : "TODOS";
        // Modelo independiente: incluye TODOS los registros de la base de datos,
        // no solamente la página actual de la tabla paginada.
        javax.swing.table.DefaultTableModel datos = new javax.swing.table.DefaultTableModel(
                new String[]{"Seleccionar","Nombre","Apellido","DNI","Teléfono",
                    "Dirección","Rol","Usuario","E-mail","Estado"}, 0);
        try {
            for (Usuario u : usuarioDAO.obtenerTodos()) {
                Persona p = personaDAO.obtenerPorId(u.getPersona().getIdPersona());
                Rol rol = usuarioRolDAO.obtenerRol(u.getIdUsuario());
                datos.addRow(new Object[]{false, p.getNombre(), p.getApellido(), p.getDni(),
                    p.getTelefono(), p.getDireccion(), rol == null ? "Sin rol" : rol.getNombreRol(),
                    u.getNombreUsuario(), u.getEmail(), u.isActivo() ? "Activo" : "Inactivo"});
            }
            ReporteRequest req = new ReporteRequest()
                    .put("tabla", new javax.swing.JTable(datos))
                    .put("estadoFiltro", estado)
                    .put("FORMATO_REPORTE", formato.getSelectedIndex() == 0
                            ? veterinaria.reportes.pdf.ReporteSelectorUtil.Formato.PDF
                            : veterinaria.reportes.pdf.ReporteSelectorUtil.Formato.XLS)
                    .put("emisor", Application.getNombreApellidoUsuarioLogeado());
            veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.USUARIO_LISTADO, req);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el listado: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean editarUsuarioSeleccionado() {
        boolean bRetorno = false;
        int selectedRow = operarTablas.comprobarElementoSeleccionado(table);

        if (selectedRow != -1) {
            var usuarioSelec = (String) datoUsuario(selectedRow, 7);
            usuario = usuarioDAO.buscarPorNombreUsuario(usuarioSelec);
            personaUsuario = personaDAO.obtenerPorId(usuario.getPersona().getIdPersona());
            rolUsuario = usuarioRolDAO.obtenerRol(usuario.getIdUsuario());
            seleccionarRolPorId(rolUsuario.getIdRol());

            txtNombre.setText(personaUsuario.getNombre());
            txtApellido.setText(personaUsuario.getApellido());
            txtDNI.setText(personaUsuario.getDni());
            txtTelefono.setText(personaUsuario.getTelefono());
            txtDireccion.setText(personaUsuario.getDireccion());
            txtEmail.setText(usuario.getEmail());
            txtUsuario.setText(usuario.getNombreUsuario());
            mostrarFoto(usuario.getRutaImagenPerfil());
            // Seguridad/UX: no precargar contraseña en edición.
            txtContraseña.setText("");

            // Guardamos snapshot para confirmar cambios al editar.
            originalNombre = personaUsuario.getNombre();
            originalApellido = personaUsuario.getApellido();
            originalDni = personaUsuario.getDni();
            originalTelefono = personaUsuario.getTelefono();
            originalDireccion = personaUsuario.getDireccion();
            originalEmail = usuario.getEmail();
            originalNombreUsuario = usuario.getNombreUsuario();
            originalRolNombre = (rolUsuario != null) ? rolUsuario.getNombreRol() : null;

            mostrarVistaUsuario(true);
            habilitarCamposPersonales(true);
            habilitarDatosDeCuenta(true);
            bRetorno = true;
        } else {
            JOptionPane.showMessageDialog(null, "¡Debe seleccionar un usuario a EDITAR!", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;
    }

    private boolean eliminarUsuarioSeleccionado() {
        boolean retornar = false;
        int selectedRow = operarTablas.comprobarElementoSeleccionado(table);

        if (selectedRow != -1) {
            var usuarioSelec = (String) datoUsuario(selectedRow, 7);
            usuario = usuarioDAO.buscarPorNombreUsuario(usuarioSelec);
            personaUsuario = personaDAO.obtenerPorId(usuario.getPersona().getIdPersona());
            rolUsuario = usuarioRolDAO.obtenerRol(usuario.getIdUsuario());
            if (Application.getSesionUsuario().getUsuario().getIdUsuario() == usuario.getIdUsuario()) {
                JOptionPane.showMessageDialog(this, "No es posible eliminar un usuario cuando está logeado!", "Advertencia", JOptionPane.WARNING_MESSAGE);
            } else {
                if (operandoUsuario.eliminarUsuario(personaUsuario, usuario, rolUsuario)) {
                    JOptionPane.showMessageDialog(this, "Usuario eliminado!", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    retornar = true;
                }
            }
        }
        return retornar;
    }

    private boolean camposPersonalesCompletos() {
        return !txtNombre.getText().isEmpty()
                && !txtApellido.getText().isEmpty()
                && !txtDNI.getText().isEmpty()
                && !txtTelefono.getText().isEmpty()
                && !txtDireccion.getText().isEmpty();
    }

    private void cargarRolesEnComboBox() {
        // Método puro: solo carga datos. NO agrega listeners.
        List<Rol> roles = rolDAO.obtenerRoles();
        rolesMap.clear();
        jcbRol.removeAllItems();
        rolesMap.put(0, "Seleccione rol");
        jcbRol.addItem("Seleccione rol");
        for (Rol rol : roles) {
            jcbRol.addItem(rol.getNombreRol());
            rolesMap.put(rol.getIdRol(), rol.getNombreRol());
        }
        rolesCargados = true;
    }

    private void seleccionarRolPorId(Integer idRol) {
        String nombreRol = rolesMap.get(idRol);
        if (nombreRol != null) {
            for (int i = 0; i < jcbRol.getItemCount(); i++) {
                if (jcbRol.getItemAt(i).equals(nombreRol)) {
                    jcbRol.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void onRolSeleccionado() {
        if (!rolesCargados) {
            return;
        }
        if (jcbRol.getSelectedIndex() <= 0) {
            rolUsuario = new Rol();
            return;
        }

        String rolSeleccionado = (String) jcbRol.getSelectedItem();
        if (rolSeleccionado == null) {
            rolUsuario = new Rol();
            return;
        }

        for (Map.Entry<Integer, String> entry : rolesMap.entrySet()) {
            if (rolSeleccionado.equals(entry.getValue())) {
                rolUsuario = rolDAO.obtenerPorId(entry.getKey());
                return;

            }
        }
    }

    private class PersonalDataListener implements javax.swing.event.DocumentListener {

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            verificarCamposPersonales();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            verificarCamposPersonales();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            verificarCamposPersonales();
        }

        private void verificarCamposPersonales() {
            if (camposPersonalesCompletos()) {
                jcbRol.setEnabled(true);
                generarNombreUsuario();
                habilitarDatosDeCuenta(true);
            } else {
                jcbRol.setEnabled(false);
                habilitarDatosDeCuenta(false);
            }
        }
    }

    private void generarNombreUsuario() {
        if (!txtNombre.getText().isEmpty() && !txtApellido.getText().isEmpty()) {
            String base = (txtNombre.getText().substring(0, 1) + txtApellido.getText())
                    .toLowerCase();

            base = normalizarUsuario(base);

            // Evitar colisiones: si existe, agregar sufijo incremental.
            String candidato = base;
            int sufijo = 2;
            while (existeNombreUsuario(candidato)) {
                candidato = base + sufijo;
                sufijo++;
            }
            txtUsuario.setText(candidato);
        } else {
            txtUsuario.setText("");
        }
    }

    private String normalizarUsuario(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^a-z0-9]", "");
        return normalized;
    }

    private boolean existeNombreUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return false;
        }
        Usuario existente = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);
        if (existente == null) {
            return false;
        }
        // Si estamos editando y el usuario encontrado es el mismo, no es colisión.
        if ("EditandoUsuario".equals(Application.consultarEstadoUsuario())
                && usuario != null
                && usuario.getIdUsuario() != null
                && existente.getIdUsuario() != null
                && existente.getIdUsuario().equals(usuario.getIdUsuario())) {
            return false;
        }
        return true;
    }

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        Application.actualizarEstadoUsuario("CreandoUsuario");
        if (tituloEditorUsuario != null) tituloEditorUsuario.setText("Nuevo usuario");
        btnGuardar.setText("Crear usuario");
        iconoUsuario(btnGuardar, "nuevo_usuario.svg");
        limpiarCamposFormulario();
        mostrarVistaUsuario(true);
        mostrarFoto(null);
        habilitarCamposPersonales(true);
        cargarRolesEnComboBox();
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (validarDatosUsuario()) {
            // Confirmación SI/NO solo al crear un usuario (patrón de seguridad/UX)
            if ("CreandoUsuario".equals(Application.consultarEstadoUsuario())) {
                if (!confirmarCreacionUsuario()) {
                    return;
                }
            }

            // Confirmación SI/NO al editar: muestra solo los cambios.
            if ("EditandoUsuario".equals(Application.consultarEstadoUsuario())) {
                if (!confirmarEdicionUsuario()) {
                    return;
                }
            }
            // ========================================================
            // PROCESAMOS LA FOTO AQUÍ: 
            // Se ejecuta justo antes de persistir para que viaje la ruta 
            // de la imagen en el objeto 'usuario'.
            // La fotografía se guarda después de persistir, cuando ya existe el ID.
            // ========================================================

            if (persistirUsuario()) {
                procesarYGuardarImagenPerfil();
                Application.refrescarFotoPerfil();
                volverAModoLista();
                Application.actualizarEstadoUsuario("InicioFormularioUsuario");
            }
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private boolean confirmarCreacionUsuario() {
        String rolSeleccionado = (jcbRol.getSelectedItem() != null) ? jcbRol.getSelectedItem().toString() : "";

        // No mostramos contraseña en el resumen.
        // Usamos HTML para resaltar etiquetas en negrita y mejorar la legibilidad.
        String resumenHtml = "<html><body style='width:360px'>"
                + "<p>Confirme la creación del usuario con los siguientes datos:</p>"
                + "<b>Nombre:</b> " + txtNombre.getText().trim() + "<br>"
                + "<b>Apellido:</b> " + txtApellido.getText().trim() + "<br>"
                + "<b>CUIT/CUIL:</b> " + txtDNI.getText().trim() + "<br>"
                + "<b>Teléfono:</b> " + txtTelefono.getText().trim() + "<br>"
                + "<b>Dirección:</b> " + txtDireccion.getText().trim() + "<br>"
                + "<b>Rol:</b> " + rolSeleccionado + "<br><br>"
                + "<b>Usuario:</b> " + txtUsuario.getText().trim() + "<br>"
                + "<b>Email:</b> " + txtEmail.getText().trim()
                + "</body></html>";

        // Botones en español.
        Object[] opciones = {"SI", "NO"};
        int opcion = JOptionPane.showOptionDialog(
                this,
                resumenHtml,
                "Confirmar creación de usuario",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        return opcion == 0; // 0 == "SI"
    }

    private boolean confirmarEdicionUsuario() {
        String nuevoNombre = txtNombre.getText().trim();
        String nuevoApellido = txtApellido.getText().trim();
        String nuevoDni = txtDNI.getText().trim();
        String nuevoTelefono = txtTelefono.getText().trim();
        String nuevoDireccion = txtDireccion.getText().trim();
        String nuevoEmail = txtEmail.getText().trim();
        String nuevoNombreUsuario = txtUsuario.getText().trim();
        String nuevoRolNombre = (jcbRol.getSelectedItem() != null) ? jcbRol.getSelectedItem().toString() : "";

        StringBuilder cambios = new StringBuilder();
        cambios.append("<html><body style='width:420px'>");
        cambios.append("<p>Se modificarán los siguientes datos:</p>");

        int cambiosCount = 0;
        cambiosCount += appendCambio(cambios, "Nombre", originalNombre, nuevoNombre);
        cambiosCount += appendCambio(cambios, "Apellido", originalApellido, nuevoApellido);
        cambiosCount += appendCambio(cambios, "CUIT/CUIL", originalDni, nuevoDni);
        cambiosCount += appendCambio(cambios, "Teléfono", originalTelefono, nuevoTelefono);
        cambiosCount += appendCambio(cambios, "Dirección", originalDireccion, nuevoDireccion);
        cambiosCount += appendCambio(cambios, "Rol", originalRolNombre, nuevoRolNombre);
        cambiosCount += appendCambio(cambios, "Usuario", originalNombreUsuario, nuevoNombreUsuario);
        cambiosCount += appendCambio(cambios, "Email", originalEmail, nuevoEmail);

        // Contraseña: no mostramos valores. Solo indicamos si se actualizará.
        if (txtContraseña.getPassword() != null && txtContraseña.getPassword().length > 0) {
            cambios.append("<b>Contraseña:</b> <b>(se actualizará)</b><br>");
            cambiosCount++;
        }

        // ========================================================
        // CONTROL DE IMAGEN DE PERFIL AQUÍ:
        // ========================================================
        if (archivoSeleccionado != null) {
            cambios.append("<b>Imagen de perfil:</b> <b>(se actualizará nueva foto)</b><br>");
            cambiosCount++;
        }
        // ========================================================

        if (cambiosCount == 0) {
            JOptionPane.showMessageDialog(this,
                    "No se detectaron cambios para guardar.",
                    "Sin cambios",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        cambios.append("<br><p><b>¿Desea confirmar los cambios?</b></p>");
        cambios.append("</body></html>");

        Object[] opciones = {"SI", "NO"};
        int opcion = JOptionPane.showOptionDialog(
                this,
                cambios.toString(),
                "Confirmar edición de usuario",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        return opcion == 0;
    }

    private int appendCambio(StringBuilder sb, String etiqueta, String viejo, String nuevo) {
        String v = (viejo == null) ? "" : viejo.trim();
        String n = (nuevo == null) ? "" : nuevo.trim();
        if (!v.equals(n)) {
            sb.append("<b>").append(escapeHtml(etiqueta)).append(":</b> ")
                    .append(escapeHtml(v)).append(" -> ")
                    .append("<b>").append(escapeHtml(n)).append("</b><br>");
            return 1;
        }
        return 0;
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean validarDatosUsuario() {
        if (txtNombre.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre del usuario no puede estar vacío.");
            return false;
        }
        if (txtApellido.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El apellido del usuario no puede estar vacío.");
            return false;
        }
        if (txtDNI.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El CUIT/CUIL del usuario no puede estar vacío.");
            return false;
        }
        if (txtTelefono.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El teléfono del usuario no puede estar vacío.");
            return false;
        }
        if (txtDireccion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "La dirección del usuario no puede estar vacía.");
            return false;
        }
        if (jcbRol.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el nivel de acceso!");
            return false;
        }
        if (txtUsuario.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario no puede estar vacío. Revise nombre, apellido y DNI para regenerarlo.");
            return false;
        }
        if (txtEmail.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El correo electrónico del usuario no puede estar vacío.");
            return false;
        }
        // Validación de formato de correo (patrón de otros ABM: FormCliente/FormProveedores)
        if (!validar.validarCorreoValido(txtEmail.getText().trim())) {
            JOptionPane.showMessageDialog(null, "Debes ingresar un correo válido.");
            return false;
        }
        // En edición permitimos dejar la contraseña vacía para no modificarla.
        if ("CreandoUsuario".equals(Application.consultarEstadoUsuario())) {
            if (txtContraseña.getPassword().length == 0) {
                JOptionPane.showMessageDialog(null, "La contraseña no puede estar vacía.");
                return false;
            }
        }
        return true;
    }

    private boolean persistirUsuario() {
        boolean retornar = false;

        if ("CreandoUsuario".equals(Application.consultarEstadoUsuario())) {
            personaUsuario = new Persona();
            usuario = new Usuario();
        }

        personaUsuario.setNombre(txtNombre.getText().trim());
        personaUsuario.setApellido(txtApellido.getText().trim());
        personaUsuario.setDni(txtDNI.getText().trim());
        personaUsuario.setDireccion(txtDireccion.getText().trim());
        personaUsuario.setTelefono(txtTelefono.getText().trim());
        usuario.setNombreUsuario(txtUsuario.getText().trim());
        usuario.setEmail(txtEmail.getText().trim());

        // En edición, null significa conservar el hash persistido.
        char[] claveIngresada = txtContraseña.getPassword();
        if (claveIngresada.length > 0) {
            usuario.setContrasena(new String(claveIngresada));
        } else if ("EditandoUsuario".equals(Application.consultarEstadoUsuario())) {
            usuario.setContrasena(null);
        }

        // Usamos try-catch para capturar las reglas de negocio / validaciones de seguridad
        try {
            if ("CreandoUsuario".equals(Application.consultarEstadoUsuario())) {
                if (operandoUsuario.registrarUsuario(personaUsuario, usuario, rolUsuario)) {
                    JOptionPane.showMessageDialog(this, "Usuario registrado con éxito!", "Información", JOptionPane.INFORMATION_MESSAGE);
                    retornar = true;
                } else {
                    JOptionPane.showMessageDialog(this, "Error al registrar un usuario.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if ("EditandoUsuario".equals(Application.consultarEstadoUsuario())) {
                if (operandoUsuario.actualizarUsuario(personaUsuario, usuario, rolUsuario)) {
                    JOptionPane.showMessageDialog(this, "Usuario actualizado con éxito!", "Información", JOptionPane.INFORMATION_MESSAGE);
                    retornar = true;
                } else {
                    JOptionPane.showMessageDialog(this, "Error al editar un usuario.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (IllegalArgumentException e) {
            // Aquí capturamos cuando la contraseña NO cumple con los requisitos de seguridad
            JOptionPane.showMessageDialog(this,
                    "<html><b>No se pudo guardar el usuario:</b><br>" + e.getMessage() + "</html>",
                    "Validación de Seguridad",
                    JOptionPane.WARNING_MESSAGE);
            txtContraseña.requestFocusInWindow();
        } catch (Exception e) {
            // Por si ocurre cualquier otro error inesperado en base de datos
            JOptionPane.showMessageDialog(this,
                    "Ocurrió un error inesperado: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        return retornar;
    }

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
        cargarRolesEnComboBox();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        Application.actualizarEstadoUsuario("EditandoUsuario");
        int selectedRow = operarTablas.comprobarElementoSeleccionado(table);

        if (selectedRow != -1) {
            if (editarUsuarioSeleccionado()) {
                if (tituloEditorUsuario != null) tituloEditorUsuario.setText("Editar usuario");
                btnGuardar.setText("Guardar cambios");
                iconoUsuario(btnGuardar, "guardar_usuario.svg");
                btnNuevo.setEnabled(false);
                btnEditar.setEnabled(false);
                btnEliminar.setEnabled(false);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Por favor seleccione un usuario a EDITAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        Usuario elegido = obtenerUsuarioSeleccionadoDesdeTabla();
        if (elegido == null) return;
        Usuario actual = Application.getSesionUsuario().getUsuario();
        if (actual != null && actual.getIdUsuario().equals(elegido.getIdUsuario())) {
            JOptionPane.showMessageDialog(this, "No podés desactivar tu propia sesión.");
            return;
        }
        boolean activar = !elegido.isActivo();
        String accion = activar ? "reactivar" : "desactivar";
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Querés " + accion + " al usuario " + elegido.getNombreUsuario() + "?",
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) return;
        try {
            boolean correcto = activar
                    ? new veterinaria.servicio.UsuarioService().reactivarUsuario(elegido)
                    : new veterinaria.servicio.UsuarioService().desactivarUsuario(elegido);
            if (correcto) {
                cargarUsuariosEnTabla();
                actualizarTextoEstadoSeleccionado();
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cambiar el estado: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void actualizarTextoEstadoSeleccionado() {
        int fila = operarTablas.comprobarElementoSeleccionado(table);
        if (fila < 0) {
            btnEliminar.setText("Desactivar");
            return;
        }
        btnEliminar.setText("Inactivo".equalsIgnoreCase(String.valueOf(datoUsuario(fila, 9)))
                ? "Reactivar" : "Desactivar");
    }

    private void txtNombreKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtNombreKeyTyped
        validar.validarSoloLetras(evt);
    }//GEN-LAST:event_txtNombreKeyTyped

    private void txtApellidoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtApellidoKeyTyped
        validar.validarSoloLetras(evt);
    }//GEN-LAST:event_txtApellidoKeyTyped

    private void txtTelefonoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTelefonoKeyTyped
        validar.validarSoloNumeros(evt);
    }//GEN-LAST:event_txtTelefonoKeyTyped

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        volverAModoLista();
        Application.actualizarEstadoUsuario("InicioFormularioUsuario");
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void txtDNIKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDNIKeyTyped
        validar.validarSoloNumeros(evt);
    }//GEN-LAST:event_txtDNIKeyTyped

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        imprimirRegistroSeleccionado();
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void cargarUsuariosEnTabla() {
        List<Usuario> usuarios = usuarioDAO.obtenerTodos();
        String filtro = (String) filtroEstado.getSelectedItem();

        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        if (modelo.getColumnCount() == 9) modelo.addColumn("Estado");
        modelo.setRowCount(0);

        for (Usuario usuario : usuarios) {
            if ("Activos".equals(filtro) && !usuario.isActivo()) continue;
            if ("Inactivos".equals(filtro) && usuario.isActivo()) continue;
            personaUsuario = personaDAO.obtenerPorId(usuario.getPersona().getIdPersona());
            String nombre = personaUsuario.getNombre();
            String apellido = personaUsuario.getApellido();
            String cuitCuil = personaUsuario.getDni();
            String direccion = personaUsuario.getDireccion();
            String telefono = personaUsuario.getTelefono();
            Rol rol = usuarioRolDAO.obtenerRol(usuario.getIdUsuario());
            String rolNombre = rol.getNombreRol() != null ? rol.getNombreRol() : "Sin rol";
            String emailUsuario = usuario.getEmail();
            String nombreUsuario = usuario.getNombreUsuario();
            Object[] fila = new Object[]{
                false, // Checkbox para selección
                nombre,
                apellido,
                cuitCuil,
                telefono,
                direccion,
                rolNombre,
                nombreUsuario,
                emailUsuario,
                usuario.isActivo() ? "Activo" : "Inactivo"
            };

            modelo.addRow(fila);
        }
        actualizarAccionesSeleccionUsuario();
        if (paginacionCompartida != null) paginacionCompartida.actualizar();
    }

    private void habilitarCamposPersonales(boolean habilitar) {
        txtNombre.setEnabled(habilitar);
        txtApellido.setEnabled(habilitar);
        txtDNI.setEnabled(habilitar);
        txtTelefono.setEnabled(habilitar);
        txtDireccion.setEnabled(habilitar);
    }

    private void habilitarDatosDeCuenta(boolean habilitar) {
        txtContraseña.setEnabled(habilitar);
        txtEmail.setEnabled(habilitar);
    }

    private void limpiarCamposCuenta() {
        txtContraseña.setText("");
        txtEmail.setText("");
    }

    private void volverAModoLista() {
        if (tituloEditorUsuario != null) tituloEditorUsuario.setText("Nuevo usuario");
        if (btnGuardar != null) {
            btnGuardar.setText("Crear usuario");
            iconoUsuario(btnGuardar, "nuevo_usuario.svg");
        }
        actualizarForm();
        btnNuevo.setEnabled(true);
        actualizarAccionesSeleccionUsuario();
    }

    private void limpiarCamposFormulario() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtDNI.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        txtUsuario.setText("");
        txtContraseña.setText("");
        txtEmail.setText("");
        jcbRol.removeAllItems();
        rolesMap.clear();
        rolesCargados = false;
        jcbRol.setEnabled(false);
        habilitarDatosDeCuenta(false);
        txtBusqueda.setText("");
        archivoSeleccionado = null;
        mostrarFoto(null);
    }


    /** Diseño inspirado en el compañero con los campos y eventos originales. */
    private void instalarPanelFotografia() {
        jpCamposFormularioUsuario.removeAll();
        jpCamposFormularioUsuario.setLayout(new java.awt.BorderLayout());
        javax.swing.JPanel pagina = new javax.swing.JPanel();
        pagina.setOpaque(false);
        pagina.setLayout(new javax.swing.BoxLayout(pagina, javax.swing.BoxLayout.Y_AXIS));
        pagina.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 7, 22, 7));

        tituloEditorUsuario = new javax.swing.JLabel("Nuevo usuario");
        tituloEditorUsuario.setFont(tituloEditorUsuario.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        tituloEditorUsuario.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pagina.add(tituloEditorUsuario);
        pagina.add(javax.swing.Box.createVerticalStrut(15));

        jpDatosPersonales.removeAll();
        jpDatosPersonales.setLayout(new java.awt.BorderLayout(16, 10));
        jpDatosPersonales.setOpaque(true);
        jpDatosPersonales.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 15, 16));
        lbDatosPersonales.setText("Datos personales y contacto");
        lbDatosPersonales.setFont(lbDatosPersonales.getFont().deriveFont(java.awt.Font.BOLD));
        jpDatosPersonales.add(lbDatosPersonales, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel personales = new javax.swing.JPanel(new net.miginfocom.swing.MigLayout(
                "insets 0, fillx, wrap 2, gapx 14, gapy 10",
                "[grow,fill][grow,fill]",
                "[52!][52!][52!]"));
        personales.setOpaque(false);
        personales.add(campoUsuario(lbNombre, txtNombre));
        personales.add(campoUsuario(lbApellido, txtApellido));
        personales.add(campoUsuario(lbCuilCuit, txtDNI));
        personales.add(campoUsuario(lbTelefono, txtTelefono));
        personales.add(campoUsuario(lbDireccion, txtDireccion));
        personales.add(campoUsuario(lbEmail, txtEmail));
        jpDatosPersonales.add(personales, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel foto = new javax.swing.JPanel();
        foto.setOpaque(false);
        foto.setLayout(new javax.swing.BoxLayout(foto, javax.swing.BoxLayout.Y_AXIS));
        foto.setPreferredSize(new java.awt.Dimension(132, 0));
        visorFoto = new javax.swing.JLabel();
        visorFoto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        visorFoto.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        visorFoto.setPreferredSize(new java.awt.Dimension(110, 110));
        visorFoto.setMinimumSize(new java.awt.Dimension(110, 110));
        visorFoto.setMaximumSize(new java.awt.Dimension(110, 110));
        foto.add(visorFoto);
        foto.add(javax.swing.Box.createVerticalStrut(10));
        javax.swing.JButton elegirFoto = new javax.swing.JButton("Agregar foto");
        elegirFoto.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        elegirFoto.addActionListener(e -> seleccionarFotografia());
        veterinaria.util.ui.EstiloHibrido.botonesSecundarios(elegirFoto);
        foto.add(elegirFoto);
        foto.add(javax.swing.Box.createVerticalStrut(8));
        javax.swing.JLabel ayudaFoto = new javax.swing.JLabel("JPG o PNG - opcional");
        ayudaFoto.setFont(ayudaFoto.getFont().deriveFont(11f));
        ayudaFoto.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        foto.add(ayudaFoto);
        jpDatosPersonales.add(foto, java.awt.BorderLayout.EAST);
        jpDatosPersonales.setPreferredSize(new java.awt.Dimension(780, 253));
        jpDatosPersonales.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 253));
        jpDatosPersonales.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pagina.add(jpDatosPersonales);
        pagina.add(javax.swing.Box.createVerticalStrut(11));

        jpAcceso.removeAll();
        jpAcceso.setLayout(new java.awt.BorderLayout(0, 10));
        jpAcceso.setOpaque(true);
        jpAcceso.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 15, 16));
        lbNivelDeAcceso.setText("Datos de acceso");
        lbNivelDeAcceso.setFont(lbNivelDeAcceso.getFont().deriveFont(java.awt.Font.BOLD));
        jpAcceso.add(lbNivelDeAcceso, java.awt.BorderLayout.NORTH);
        jpDatosCuenta.removeAll();
        jpDatosCuenta.setLayout(new net.miginfocom.swing.MigLayout(
                "insets 0, fillx, wrap 2, gapx 14, gapy 9",
                "[grow,fill][grow,fill]",
                "[52!][52!]"));
        jpDatosCuenta.setOpaque(false);
        jpDatosCuenta.add(campoUsuario(lbRol, jcbRol));
        jpDatosCuenta.add(campoUsuario(lbNombreUsuario, txtUsuario));
        javax.swing.JPanel clave = new javax.swing.JPanel(new java.awt.BorderLayout(6, 0));
        clave.setOpaque(false);
        clave.add(txtContraseña, java.awt.BorderLayout.CENTER);
        clave.add(btnVerContrasena, java.awt.BorderLayout.EAST);
        jpDatosCuenta.add(campoUsuario(lbDireccion1, clave));
        javax.swing.JLabel info = new javax.swing.JLabel(
                "Usá al menos 8 caracteres, una mayúscula, un número y un símbolo. La contraseña se cambiará al ingresar.");
        java.awt.Color atenuado = javax.swing.UIManager.getColor("App.muted.foreground");
        if (atenuado != null) info.setForeground(atenuado);
        javax.swing.JPanel espacioInfo = new javax.swing.JPanel();
        espacioInfo.setOpaque(false);
        jpDatosCuenta.add(espacioInfo);
        jpAcceso.add(jpDatosCuenta, java.awt.BorderLayout.CENTER);
        jpAcceso.add(info, java.awt.BorderLayout.SOUTH);
        jpAcceso.setPreferredSize(new java.awt.Dimension(780, 208));
        jpAcceso.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 208));
        jpAcceso.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pagina.add(jpAcceso);
        pagina.add(javax.swing.Box.createVerticalStrut(8));

        javax.swing.JPanel acciones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 6));
        acciones.setOpaque(false);
        acciones.add(btnLimpiar);
        acciones.add(btnCancelar);
        acciones.add(btnGuardar);
        // Acciones del editor en el mismo lenguaje visual de la barra de listado.
        iconoUsuario(btnLimpiar, "limpiar_usuario.svg");
        iconoUsuario(btnCancelar, "cancelar_usuario.svg");
        iconoUsuario(btnGuardar, "nuevo_usuario.svg");
        for (javax.swing.JButton boton : new javax.swing.JButton[]{
                btnLimpiar, btnCancelar, btnGuardar}) {
            boton.setPreferredSize(new java.awt.Dimension(
                    boton == btnGuardar ? 150 : 96, 34));
        }
        acciones.setPreferredSize(new java.awt.Dimension(780, 52));
        acciones.setMinimumSize(new java.awt.Dimension(350, 52));
        acciones.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 6, 10));
        mostrarFoto(null);

        javax.swing.JScrollPane scroller = new javax.swing.JScrollPane(pagina);
        scroller.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroller.getVerticalScrollBar().setUnitIncrement(15);
        scroller.setHorizontalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getViewport().setOpaque(false);
        scroller.setOpaque(false);
        jpCamposFormularioUsuario.add(scroller, java.awt.BorderLayout.CENTER);
        jpCamposFormularioUsuario.add(acciones, java.awt.BorderLayout.SOUTH);
    }

    private javax.swing.JPanel campoUsuario(javax.swing.JComponent rotulo,
            javax.swing.JComponent control) {
        javax.swing.JPanel campo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 4));
        campo.setOpaque(false);
        campo.add(rotulo, java.awt.BorderLayout.NORTH);
        // Altura consistente de 32px para todos los campos y el selector de rol.
        control.setPreferredSize(new java.awt.Dimension(
                Math.max(135, control.getPreferredSize().width), 32));
        campo.add(control, java.awt.BorderLayout.CENTER);
        return campo;
    }

    private void seleccionarFotografia() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes JPG y PNG", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.io.File elegida = chooser.getSelectedFile();
        try {
            java.awt.image.BufferedImage imagen = javax.imageio.ImageIO.read(elegida);
            if (imagen == null || imagen.getWidth() < 1) throw new java.io.IOException("Imagen no válida");
            if (elegida.length() > 5_000_000L) throw new java.io.IOException("Máximo 5 MB");
            archivoSeleccionado = elegida;
            mostrarIconoFoto(new javax.swing.ImageIcon(imagen));
        } catch (java.io.IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudo cargar la foto: " + ex.getMessage());
        }
    }

    private void mostrarFoto(String ruta) {
        if (visorFoto == null) return;
        javax.swing.ImageIcon icono = null;
        if (ruta != null && !ruta.isBlank()) {
            java.io.File archivo = new java.io.File(ruta);
            if (archivo.isFile()) icono = new javax.swing.ImageIcon(archivo.getAbsolutePath());
        }
        mostrarIconoFoto(icono);
    }

    private void mostrarIconoFoto(javax.swing.ImageIcon icono) {
        if (visorFoto == null) return;
        if (icono == null) {
            // Avatar neutro como Windows; no utilizar la imagen ilustrada por defecto.
            java.awt.image.BufferedImage avatar = new java.awt.image.BufferedImage(110, 110,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = avatar.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new java.awt.Color(92, 104, 114));
            g.fillOval(0, 0, 109, 109);
            g.setColor(new java.awt.Color(222, 229, 234));
            g.setStroke(new java.awt.BasicStroke(3f));
            g.drawOval(43, 25, 24, 24);
            g.drawArc(24, 53, 62, 44, 0, 180);
            g.dispose();
            visorFoto.setIcon(new javax.swing.ImageIcon(avatar));
        } else {
            visorFoto.setIcon(veterinaria.util.ui.ImageUtils.toCircularIcon(icono, 110));
        }
        visorFoto.setText("");
    }

    private void procesarYGuardarImagenPerfil() {
        if (archivoSeleccionado == null || usuario == null || usuario.getIdUsuario() == null) return;
        try {
            java.nio.file.Path carpeta = java.nio.file.Paths.get("imagen_perfil");
            java.nio.file.Files.createDirectories(carpeta);
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(archivoSeleccionado);
            if (original == null) throw new java.io.IOException("Formato de imagen inválido");
            java.awt.image.BufferedImage reducida = new java.awt.image.BufferedImage(320, 320,
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = reducida.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, 320, 320);
            double escala = Math.min(320.0 / original.getWidth(), 320.0 / original.getHeight());
            int ancho = (int) Math.round(original.getWidth() * escala);
            int alto = (int) Math.round(original.getHeight() * escala);
            g.drawImage(original, (320 - ancho) / 2, (320 - alto) / 2, ancho, alto, null);
            g.dispose();
            java.nio.file.Path destino = carpeta.resolve("user_" + usuario.getIdUsuario() + ".jpg");
            if (!javax.imageio.ImageIO.write(reducida, "jpg", destino.toFile()))
                throw new java.io.IOException("No se pudo generar el JPG");
            String ruta = destino.toString().replace('\\', '/');
            javax.persistence.EntityManager em = veterinaria.persistencia.JPAUtil.getEntityManager();
            try {
                em.getTransaction().begin();
                em.createQuery("UPDATE Usuario u SET u.rutaImagenPerfil = :ruta WHERE u.idUsuario = :id")
                        .setParameter("ruta", ruta)
                        .setParameter("id", usuario.getIdUsuario())
                        .executeUpdate();
                em.getTransaction().commit();
                usuario.setRutaImagenPerfil(ruta);
            } catch (RuntimeException ex) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw ex;
            } finally {
                em.close();
            }
            archivoSeleccionado = null;
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "El usuario se guardó, pero no se pudo guardar su foto: " + ex.getMessage(),
                    "Fotografía", javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimir1;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JComboBox<String> jcbRol;
    private javax.swing.JPanel jpAcceso;
    private javax.swing.JPanel jpCamposFormularioUsuario;
    private javax.swing.JPanel jpDatosCuenta;
    private javax.swing.JPanel jpDatosPersonales;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaUsuarios;
    private javax.swing.JLabel lbApellido;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbCuilCuit;
    private javax.swing.JLabel lbDatosDeCuenta;
    private javax.swing.JLabel lbDatosPersonales;
    private javax.swing.JLabel lbDireccion;
    private javax.swing.JLabel lbDireccion1;
    private javax.swing.JLabel lbEmail;
    private javax.swing.JLabel lbNivelDeAcceso;
    private javax.swing.JLabel lbNombre;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JLabel lbRol;
    private javax.swing.JLabel lbTelefono;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JLabel lblGestionUsuarios;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JPasswordField txtContraseña;
    private javax.swing.JTextField txtDNI;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables

    private void inicializarTooltips() {
        String mensaje = "Busca en las columnas Nombre, Apellido y DNI";
        lbUsuarioBusqueda.setToolTipText(mensaje);
        lbBuscar.setToolTipText(mensaje);
        txtBusqueda.setToolTipText(mensaje);
    }

}
