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

public class FormUsuario extends javax.swing.JPanel {

    private boolean ajustandoSeleccionUsuario;
    private final javax.swing.JButton btnCancelarSeleccion = new javax.swing.JButton("Cancelar selección");
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
        actualizarForm();
        instalarPaginacionCompartida();
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

        // Botones de impresión (patrón similar a FormHospitalizaciones)
        btnImprimir.setVisible(
                false); // solo visible si hay un registro seleccionado
        btnImprimir.setEnabled(
                true);
        btnImprimir.addActionListener(e
                -> imprimirRegistroSeleccionado());

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
                .addContainerGap())
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

    private void instalarFiltroEstado() {
        // Mismo patrón visual de Clientes: las etiquetas arriba y los campos
        // de búsqueda y estado alineados horizontalmente.
        filtroEstado.addActionListener(e -> cargarUsuariosEnTabla());

        jpHeader.removeAll();
        jpHeader.setLayout(new java.awt.BorderLayout(0, 12));

        javax.swing.JPanel titulo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 7));
        titulo.setOpaque(false);
        titulo.add(lblGestionUsuarios, java.awt.BorderLayout.NORTH);
        titulo.add(jSeparator2, java.awt.BorderLayout.SOUTH);
        jpHeader.add(titulo, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel centro = new javax.swing.JPanel(new java.awt.BorderLayout(15, 0));
        centro.setOpaque(false);

        javax.swing.JPanel filtros = new javax.swing.JPanel(new java.awt.GridBagLayout());
        filtros.setOpaque(false);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.fill = java.awt.GridBagConstraints.NONE;
        c.insets = new java.awt.Insets(0, 0, 6, 8);

        c.gridx = 0; c.gridy = 0;
        filtros.add(lbUsuarioBusqueda, c);
        lbUsuarioBusqueda.setText("BUSCAR USUARIO");
        c.gridx = 1;
        filtros.add(new javax.swing.JLabel("Estado:"), c);

        javax.swing.JPanel buscador = new javax.swing.JPanel(new java.awt.BorderLayout(7, 0));
        buscador.setOpaque(false);
        buscador.add(lbBuscar, java.awt.BorderLayout.WEST);
        buscador.add(txtBusqueda, java.awt.BorderLayout.CENTER);
        buscador.setPreferredSize(new java.awt.Dimension(285, 30));
        c.gridx = 0; c.gridy = 1; c.insets = new java.awt.Insets(0, 0, 0, 8);
        filtros.add(buscador, c);
        filtroEstado.setPreferredSize(new java.awt.Dimension(140, 28));
        c.gridx = 1;
        filtros.add(filtroEstado, c);
        centro.add(filtros, java.awt.BorderLayout.WEST);

        javax.swing.JPanel acciones = new javax.swing.JPanel();
        acciones.setOpaque(false);
        acciones.setLayout(new javax.swing.BoxLayout(acciones, javax.swing.BoxLayout.Y_AXIS));
        btnImprimir.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        acciones.add(btnImprimir);
        acciones.add(javax.swing.Box.createVerticalStrut(8));
        javax.swing.JPanel botones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        for (javax.swing.JButton boton : new javax.swing.JButton[]{btnNuevo, btnEditar, btnEliminar}) {
            boton.setPreferredSize(new java.awt.Dimension(90, 28));
            botones.add(boton);
        }
        botones.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        btnCancelarSeleccion.setEnabled(false);
        btnCancelarSeleccion.setToolTipText("Desmarcar el usuario seleccionado");
        btnCancelarSeleccion.addActionListener(e -> cancelarSeleccionUsuario());
        botones.add(btnCancelarSeleccion);
        acciones.add(botones);
        centro.add(acciones, java.awt.BorderLayout.EAST);
        jpHeader.add(centro, java.awt.BorderLayout.CENTER);
        jpHeader.add(jSeparator1, java.awt.BorderLayout.SOUTH);
        jpHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        jpHeader.revalidate();
        jpHeader.repaint();
    }

    private void instalarPaginacionCompartida() {
        // El contenedor se instala fuera del código generado por NetBeans.
        javax.swing.JPanel contenedor = new javax.swing.JPanel(new java.awt.BorderLayout());
        ((javax.swing.GroupLayout) jpListaUsuarios.getLayout()).replace(scroll, contenedor);
        contenedor.add(scroll, java.awt.BorderLayout.CENTER);
        paginacionCompartida = veterinaria.util.GestorTablas.paginar(
                table, txtBusqueda,
                new veterinaria.util.GestorTablas.Configuracion(
                        new int[]{1, 2, 3, 6, 7}, new int[]{1, 2, 3, 6, 7}, 15));
        paginacionCompartida.activarBusquedaPorPrefijo();
        contenedor.add(paginacionCompartida.getControles(), java.awt.BorderLayout.SOUTH);
        txtBusqueda.setLayout(new java.awt.BorderLayout());
        txtBusqueda.add(paginacionCompartida.getBotonLimpiar(), java.awt.BorderLayout.EAST);
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

        // La búsqueda y la selección se administran desde GestorTablas.
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
        jpCamposFormularioUsuario.setVisible(false);
        jpListaUsuarios.setVisible(true);
        actualizarBotonImprimir();
        revalidate();
        repaint();
    }

    private void actualizarBotonImprimir() {
        actualizarAccionesSeleccionUsuario();
    }

    private void actualizarAccionesSeleccionUsuario() {
        boolean haySeleccion = veterinaria.util.GestorTablas.haySeleccion(table);
        btnEditar.setVisible(haySeleccion);
        btnEliminar.setVisible(haySeleccion);
        btnCancelarSeleccion.setVisible(haySeleccion);
        btnImprimir.setVisible(haySeleccion);
        btnEditar.setEnabled(haySeleccion);
        btnEliminar.setEnabled(haySeleccion);
        btnCancelarSeleccion.setEnabled(haySeleccion);
        actualizarTextoEstadoSeleccionado();
    }

    private Usuario obtenerUsuarioSeleccionadoDesdeTabla() {
        int row = operarTablas.comprobarElementoSeleccionado(table);
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            String usuarioSelec = String.valueOf(table.getValueAt(row, 7));
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
        try {
            // Fallback de datos planos desde la JTable (por si falla la carga de Persona)
            int rowSel = operarTablas.comprobarElementoSeleccionado(table);
            String fNombre = (rowSel != -1) ? String.valueOf(table.getValueAt(rowSel, 1)) : "";
            String fApellido = (rowSel != -1) ? String.valueOf(table.getValueAt(rowSel, 2)) : "";
            String fDni = (rowSel != -1) ? String.valueOf(table.getValueAt(rowSel, 3)) : "";
            String fTelefono = (rowSel != -1) ? String.valueOf(table.getValueAt(rowSel, 4)) : "";
            String fDireccion = (rowSel != -1) ? String.valueOf(table.getValueAt(rowSel, 5)) : "";

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
            var usuarioSelec = (String) table.getValueAt(selectedRow, 7);
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

            jpCamposFormularioUsuario.setVisible(true);
            jpListaUsuarios.setVisible(false);
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
            var usuarioSelec = (String) table.getValueAt(selectedRow, 7);
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
        limpiarCamposFormulario();
        jpCamposFormularioUsuario.setVisible(true);
        jpListaUsuarios.setVisible(false);
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
        btnEliminar.setText("Inactivo".equalsIgnoreCase(String.valueOf(table.getValueAt(fila, 9)))
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

    private void instalarPanelFotografia() {
        // El panel de datos se mantiene compacto y los botones quedan fuera del área de foto.
        jpCamposFormularioUsuario.removeAll();
        jpCamposFormularioUsuario.setLayout(new java.awt.BorderLayout(14, 10));

        javax.swing.JPanel izquierda = new javax.swing.JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new javax.swing.BoxLayout(izquierda, javax.swing.BoxLayout.Y_AXIS));
        for (javax.swing.JPanel seccion : new javax.swing.JPanel[]{jpDatosPersonales, jpAcceso, jpDatosCuenta}) {
            seccion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            izquierda.add(seccion);
            izquierda.add(javax.swing.Box.createVerticalStrut(6));
        }

        // Los botones pertenecen al pie del formulario, no a la sección de credenciales.
        // El GroupLayout generado aún reservaba espacio para los botones extraídos.
        // Se reemplaza el layout de esta sección por uno compacto y estable.
        jpDatosCuenta.removeAll();
        jpDatosCuenta.setLayout(new java.awt.BorderLayout(0, 6));
        jpDatosCuenta.add(lbDatosDeCuenta, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel camposCuenta = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        camposCuenta.setOpaque(false);
        for (javax.swing.JComponent[] par : new javax.swing.JComponent[][]{
                {lbNombreUsuario, txtUsuario}, {lbEmail, txtEmail}, {lbDireccion1, txtContraseña}}) {
            javax.swing.JPanel campo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
            campo.setOpaque(false);
            campo.add(par[0], java.awt.BorderLayout.NORTH);
            campo.add(par[1], java.awt.BorderLayout.CENTER);
            campo.setPreferredSize(new java.awt.Dimension(140, 52));
            camposCuenta.add(campo);
        }
        camposCuenta.add(btnVerContrasena);
        jpDatosCuenta.add(camposCuenta, java.awt.BorderLayout.CENTER);
        jpDatosCuenta.setPreferredSize(new java.awt.Dimension(550, 92));
        jpDatosCuenta.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 92));
        javax.swing.JPanel acciones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 6));
        acciones.setOpaque(false);
        acciones.add(btnGuardar);
        acciones.add(btnCancelar);
        acciones.add(btnLimpiar);

        javax.swing.JPanel foto = new javax.swing.JPanel();
        foto.setOpaque(false);
        foto.setPreferredSize(new java.awt.Dimension(175, 250));
        foto.setLayout(new javax.swing.BoxLayout(foto, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JLabel titulo = new javax.swing.JLabel("Foto de perfil");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        titulo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        foto.add(titulo);
        foto.add(javax.swing.Box.createVerticalStrut(8));
        visorFoto = new javax.swing.JLabel();
        visorFoto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        visorFoto.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        visorFoto.setPreferredSize(new java.awt.Dimension(115, 115));
        visorFoto.setMinimumSize(new java.awt.Dimension(115, 115));
        visorFoto.setMaximumSize(new java.awt.Dimension(115, 115));
        foto.add(visorFoto);
        foto.add(javax.swing.Box.createVerticalStrut(10));
        javax.swing.JButton elegir = new javax.swing.JButton("Seleccionar imagen");
        elegir.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        elegir.addActionListener(e -> seleccionarFotografia());
        foto.add(elegir);
        foto.add(javax.swing.Box.createVerticalStrut(6));
        javax.swing.JLabel ayuda = new javax.swing.JLabel("JPG o PNG (opcional)");
        ayuda.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        foto.add(ayuda);

        javax.swing.JPanel cuerpo = new javax.swing.JPanel(new java.awt.BorderLayout(14, 0));
        cuerpo.setOpaque(false);
        cuerpo.add(izquierda, java.awt.BorderLayout.CENTER);
        cuerpo.add(foto, java.awt.BorderLayout.EAST);
        jpCamposFormularioUsuario.add(cuerpo, java.awt.BorderLayout.CENTER);
        acciones.setPreferredSize(new java.awt.Dimension(0, 44));
        jpCamposFormularioUsuario.add(acciones, java.awt.BorderLayout.SOUTH);
        mostrarFoto(null);
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
