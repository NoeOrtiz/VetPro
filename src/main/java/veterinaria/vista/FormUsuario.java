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

    public FormUsuario() {
        initComponents();
        // Centramos cualquier contenido o icono que se cargue
        // Centramos cualquier contenido o icono que se cargue
       // lbFotoPerfil.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        //lbFotoPerfil.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        inicializarTooltips();

        PermisoUI.aplicar(
                this);
        initListeners();

        actualizarForm();

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
                            .addComponent(txtContraseña, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))
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
                            .addComponent(txtContraseña, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
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
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setMinimumSize(new java.awt.Dimension(848, 220));
        table.setPreferredSize(new java.awt.Dimension(848, 220));
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);

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
                .addComponent(jpCamposFormularioUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaUsuarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jpCamposFormularioUsuario.getAccessibleContext().setAccessibleName("");
        jpCamposFormularioUsuario.getAccessibleContext().setAccessibleDescription("");
    }// </editor-fold>//GEN-END:initComponents

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

        List<Integer> columnasObjetivo = Arrays.asList(1, 2, 3, 6, 7);
        operarTablas.aplicarFiltroYResaltado(table, txtBusqueda, columnasObjetivo);

        // Visibilidad del botón Imprimir según selección (checkbox col 0)
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() == 0 || e.getColumn() == TableModelEvent.ALL_COLUMNS)) {
                SwingUtilities.invokeLater(this::actualizarBotonImprimir);
            }
        });

        // Asegurar selección única en checkbox (columna 0) (patrón común)
        operarTablas.asegurarSeleccionUnica(table);

        // Click en fila => marcar automáticamente el checkbox (como en otros formularios)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                seleccionarFilaDesdeClick(evt);
            }
        });

        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
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
        boolean haySeleccion = (operarTablas.comprobarElementoSeleccionado(table) != -1);
        btnImprimir.setVisible(haySeleccion);
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
        // 1. Armamos los parámetros con la JTable del formulario
        ReporteRequest req = new ReporteRequest()
                .put("tabla", table)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 AL EJECUTOR: Limpio, asíncrono y alineado al Singleton
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.USUARIO_LISTADO, req);
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
            procesarYGuardarImagenPerfil();
            // ========================================================

            if (persistirUsuario()) {
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

        // Si está editando y el campo contraseña viene vacío, NO sobreescribimos.
        if (txtContraseña.getPassword() != null && txtContraseña.getPassword().length > 0) {
            usuario.setContrasena(String.valueOf(txtContraseña.getPassword()));
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
        int selectedRow = operarTablas.comprobarElementoSeleccionado(table);

        if (selectedRow != -1) {
            int respuesta = JOptionPane.showConfirmDialog(
                    this, "¿Estás seguro de que deseas eliminar este usuario?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                Application.actualizarEstadoUsuario("EliminandoUsuario");
                eliminarUsuarioSeleccionado();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Por favor seleccione un usuario a ELIMINAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        actualizarForm();
    }//GEN-LAST:event_btnEliminarActionPerformed

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

        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        modelo.setRowCount(0);

        for (Usuario usuario : usuarios) {
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
                emailUsuario
            };

            modelo.addRow(fila);
        }
        actualizarBotonImprimir();
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
        btnEditar.setEnabled(true);
        btnEliminar.setEnabled(true);
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
    }

    private void procesarYGuardarImagenPerfil() {
        if (archivoSeleccionado == null || usuario == null) {
            return;
        }
        try {
            java.io.File destinoCarpeta = new java.io.File("imagen_perfil");
            if (!destinoCarpeta.exists()) {
                destinoCarpeta.mkdirs();
            }

            String extension = archivoSeleccionado.getName().substring(archivoSeleccionado.getName().lastIndexOf("."));
            java.io.File archivoDestino = new java.io.File(destinoCarpeta, "user_" + usuario.getIdUsuario() + extension);

            java.nio.file.Files.copy(
                    archivoSeleccionado.toPath(),
                    archivoDestino.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            String rutaParaBD = "imagen_perfil/" + archivoDestino.getName();
            usuario.setRutaImagenPerfil(rutaParaBD);

            java.awt.Image imgEscaladaMenu = new javax.swing.ImageIcon(archivoDestino.getAbsolutePath())
                    .getImage().getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);

            // ASÍ REEMPLAZALO AHORA (Solución limpia):
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (window instanceof Application) {
                Application appInstance = (Application) window;
                // Si tu MainForm tiene un método público getMenu(), lo usamos:
                if (appInstance.getMainForm() != null && appInstance.getMainForm().getMenu() != null) {
                    appInstance.getMainForm().getMenu().setProfileIcon(new javax.swing.ImageIcon(imgEscaladaMenu));
                }
            }

            archivoSeleccionado = null;
        } catch (Exception ex) {
            System.err.println("Error al procesar la imagen: " + ex.getMessage());
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
