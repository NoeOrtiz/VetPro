package veterinaria.vista;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.ClienteControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.persistencia.ClienteDAO;
import veterinaria.persistencia.PersonaDAO;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.ReporteSelectorUtil;
import veterinaria.util.ManejoTablas;
import veterinaria.util.ModeloTablaGenerico;
import veterinaria.util.SesionUsuario;
import veterinaria.util.Validaciones;
import veterinaria.vista.application.Application;
import veterinaria.util.PermisoUI;
import veterinaria.util.enums.EstadoCliente;
import veterinaria.vista.table.TableGradientCell;

public class FormCliente extends javax.swing.JPanel {

    private SesionUsuario sesion = Application.getSesionUsuario();
    private ClienteControlador clienteControlador = new ClienteControlador();
    private boolean listenerSeleccionUnicaInstalado = false;
    private final javax.swing.JButton btnCancelarSeleccion = new javax.swing.JButton("Cancelar");
    private final javax.swing.JButton btnVerInfo = new javax.swing.JButton("Ver Info");
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final PersonaDAO personaDAO = new PersonaDAO();
    private final Validaciones validar = new Validaciones();
    private final ManejoTablas operarTablas = new ManejoTablas();
    private ModeloTablaGenerico<Cliente> modeloClientes;
    private Persona persona = new Persona();
    private Cliente cliente = new Cliente();

    // Lista oculta para guardar los IDs en el mismo orden que la JTable
    private java.util.List<Integer> listaIdsClientes = new java.util.ArrayList<>();

    // Índices de columnas de JTable
    private static final int COL_SELECCION = 0;
    private static final int COL_ID_CLIENTE = 1;
    private static final int COL_NOMBRE = 2;
    private static final int COL_APELLIDO = 3;
    private static final int COL_DNI = 4;
    private static final int COL_DIRECCION = 5;
    private static final int COL_EMAIL = 6;
    private static final int COL_TELEFONO = 7;
    private static final int COL_RAZON_SOCIAL = 8;
    private static final int COL_CUIT = 9;
    private static final int COL_ESTADO = 10;
    private static final int COL_FECHA_ALTA = 11;

    // Valores originales para confirmar edición
    private String originalNombre = "";
    private String originalApellido = "";
    private String originalDni = "";
    private String originalDireccion = "";
    private String originalTelefono = "";
    private String originalEmail = "";
    private String originalRazonSocial = "";
    private String originalCuit = "";
    private String originalEstado = "";

    public FormCliente() {
        initComponents();
        PermisoUI.aplicar(this);
        btnImprimir.setVisible(false);
        initListeners();
        initCombos();
        configurarAccionesClientes();
        SwingUtilities.invokeLater(() -> {
            cargarClientesEnTabla();
            instalarPaginacionCompartida();
        });
        SwingUtilities.invokeLater(() -> {
            jcbEstadoCliente.setSelectedItem(EstadoCliente.ACTIVO.getDescripcion()); // Arranca en Activo por defecto
            cargarClientesEnTabla();
        });
        ocultarColumnaIdCliente();
        // List<Integer> indicesColumnasConTooltip = Arrays.asList(COL_DNI, COL_DIRECCION, COL_EMAIL, COL_TELEFONO, COL_RAZON_SOCIAL);
        //operarTablas.mouseTooltipText(tableClientes, indicesColumnasConTooltip);
        jpDatosPersonales.setVisible(false);
    }

    @SuppressWarnings("unchecked")

    private veterinaria.util.PaginacionTablas paginacionCompartida;

    private void instalarPaginacionCompartida() {
        // El contenedor se instala fuera del código generado por NetBeans.
        javax.swing.JPanel contenedor = new javax.swing.JPanel(new java.awt.BorderLayout());
        ((javax.swing.GroupLayout) panelLista.getLayout()).replace(scroll, contenedor);
        contenedor.add(scroll, java.awt.BorderLayout.CENTER);
        paginacionCompartida = veterinaria.util.GestorTablas.paginar(
                tableClientes, txtBusqueda,
                new veterinaria.util.GestorTablas.Configuracion(
                        new int[]{2, 3, 4, 6, 8, 9}, new int[]{2, 3, 4, 6, 8, 9}, 15));
        paginacionCompartida.activarBusquedaPorPrefijo();
        contenedor.add(paginacionCompartida.getControles(), java.awt.BorderLayout.SOUTH);
        txtBusqueda.setLayout(new java.awt.BorderLayout());
        txtBusqueda.add(paginacionCompartida.getBotonLimpiar(), java.awt.BorderLayout.EAST);
        tableClientes.getColumnModel().getColumn(0).setCellRenderer(
                new veterinaria.vista.table.TableGradientCheckBoxRenderer());
        paginacionCompartida.actualizar();
    }

    private void configurarAccionesClientes() {
        // Conservar el diseño y los componentes existentes, alineando las acciones a la derecha.
        panelBotones.removeAll();
        panelBotones.setLayout(new java.awt.BorderLayout(0, 10));
        javax.swing.JPanel titulo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 7));
        titulo.setOpaque(false);
        titulo.add(jLabel1, java.awt.BorderLayout.NORTH);
        titulo.add(jSeparator1, java.awt.BorderLayout.SOUTH);
        panelBotones.add(titulo, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel contenido = new javax.swing.JPanel(new java.awt.BorderLayout(12, 0));
        contenido.setOpaque(false);
        javax.swing.JPanel filtros = new javax.swing.JPanel(new java.awt.GridBagLayout());
        filtros.setOpaque(false);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.insets = new java.awt.Insets(0, 0, 6, 8);
        c.gridx = 0; c.gridy = 0;
        filtros.add(lbUsuarioBusqueda, c);
        c.gridx = 1;
        filtros.add(lbEstado1, c);
        javax.swing.JPanel busqueda = new javax.swing.JPanel(new java.awt.BorderLayout(7, 0));
        busqueda.setOpaque(false);
        busqueda.add(lbBuscar, java.awt.BorderLayout.WEST);
        busqueda.add(txtBusqueda, java.awt.BorderLayout.CENTER);
        busqueda.setPreferredSize(new java.awt.Dimension(285, 30));
        c.gridx = 0; c.gridy = 1; c.insets = new java.awt.Insets(0, 0, 0, 8);
        filtros.add(busqueda, c);
        jcbEstadoCliente.setPreferredSize(new java.awt.Dimension(140, 28));
        c.gridx = 1;
        filtros.add(jcbEstadoCliente, c);
        contenido.add(filtros, java.awt.BorderLayout.WEST);

        javax.swing.JPanel acciones = new javax.swing.JPanel();
        acciones.setOpaque(false);
        acciones.setLayout(new javax.swing.BoxLayout(acciones, javax.swing.BoxLayout.Y_AXIS));
        btnImprimir.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        acciones.add(btnImprimir);
        acciones.add(javax.swing.Box.createVerticalStrut(8));
        javax.swing.JPanel fila = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        fila.setOpaque(false);
        for (javax.swing.JButton boton : new javax.swing.JButton[]{btnNuevo, btnEditar, btnEliminar}) {
            boton.setPreferredSize(new java.awt.Dimension(90, 28));
            fila.add(boton);
        }
        btnEliminar.setText("Desactivar");
        btnVerInfo.setToolTipText("Consultar la ficha completa y las mascotas del cliente");
        btnVerInfo.addActionListener(e -> mostrarInfoCliente());
        fila.add(btnVerInfo);
        btnCancelarSeleccion.setToolTipText("Desmarcar el cliente seleccionado");
        btnCancelarSeleccion.addActionListener(e -> cancelarSeleccionCliente());
        fila.add(btnCancelarSeleccion);
        fila.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        acciones.add(fila);
        contenido.add(acciones, java.awt.BorderLayout.EAST);
        panelBotones.add(contenido, java.awt.BorderLayout.CENTER);
        panelBotones.add(jSeparator2, java.awt.BorderLayout.SOUTH);
        panelBotones.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        actualizarAccionesCliente();
    }

    private void actualizarAccionesCliente() {
        boolean seleccion = veterinaria.util.GestorTablas.haySeleccion(tableClientes);
        btnEditar.setVisible(seleccion);
        btnEliminar.setVisible(seleccion);
        btnImprimir.setVisible(seleccion);
        btnCancelarSeleccion.setVisible(seleccion);
        btnVerInfo.setVisible(seleccion);
        btnEditar.setEnabled(seleccion);
        btnEliminar.setEnabled(seleccion);
        actualizarTextoAccionCliente();
        btnNuevo.setEnabled(!seleccion);
    }

    private void actualizarTextoAccionCliente() {
        if (!veterinaria.util.GestorTablas.haySeleccion(tableClientes)) {
            btnEliminar.setText("Desactivar");
            return;
        }
        Cliente elegido = obtenerClienteSeleccionadoDesdeTabla();
        if (elegido != null) {
            btnEliminar.setText(elegido.isActivo() ? "Desactivar" : "Reactivar");
        }
    }

    private void cancelarSeleccionCliente() {
        for (int i = 0; i < tableClientes.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableClientes.getValueAt(i, COL_SELECCION))) {
                tableClientes.setValueAt(false, i, COL_SELECCION);
            }
        }
        tableClientes.clearSelection();
        actualizarAccionesCliente();
    }

    private void initListeners() {
        // Escucha cuando cambia el combo de estado superior para recargar la tabla
        jcbEstadoCliente.addActionListener(e -> cargarClientesEnTabla());
        // Columnas para filtro/búsqueda (Nombre, Apellido, DNI, Email, Razón Social, CUIT)
        // La búsqueda y la selección se administran desde GestorTablas.
        veterinaria.util.GestorTablas.instalarSeleccion(tableClientes, () -> {
            boolean seleccion = veterinaria.util.GestorTablas.haySeleccion(tableClientes);
            actualizarAccionesCliente();
        });


        SwingUtilities.invokeLater(this::actualizarBotonImprimir);

        txtApellido.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtRazonSocial.getText().trim().isEmpty()) {
                    String nombreApellido = (txtNombre.getText() + " " + txtApellido.getText()).trim();
                    txtRazonSocial.setText(nombreApellido);
                }
            }
        });
    }

    private void initCombos() {
        // Limpiamos ambos combos por seguridad
        jcbEstadoCliente.removeAllItems();
        jcbEstado.removeAllItems();

        // 1. Combo del filtro superior (Incluye "Todos")
        jcbEstadoCliente.addItem("Seleccionar Estado");
        jcbEstadoCliente.addItem(EstadoCliente.TODOS.getDescripcion());
        jcbEstadoCliente.addItem(EstadoCliente.ACTIVO.getDescripcion());
        jcbEstadoCliente.addItem(EstadoCliente.INACTIVO.getDescripcion());
        jcbEstadoCliente.setSelectedItem(EstadoCliente.ACTIVO.getDescripcion());

        // 2. Combo de edición (Solo Activo e Inactivo)
        jcbEstado.addItem(EstadoCliente.ACTIVO.getDescripcion());
        jcbEstado.addItem(EstadoCliente.INACTIVO.getDescripcion());
        jcbEstado.setVisible(false);
    }

    /**
     * Al hacer click en una fila (fuera del checkbox), marca automáticamente el
     * checkbox de esa fila y desmarca el resto.
     */
    private void seleccionarFilaDesdeClick(java.awt.event.MouseEvent evt) {
        if (evt == null) {
            return;
        }

        int viewRow = tableClientes.rowAtPoint(evt.getPoint());
        int viewCol = tableClientes.columnAtPoint(evt.getPoint());

        if (viewRow < 0) {
            return;
        }

        try {
            tableClientes.setRowSelectionInterval(viewRow, viewRow);
        } catch (Exception ignore) {
        }

        if (viewCol == COL_SELECCION) {
            SwingUtilities.invokeLater(this::actualizarBotonImprimir);
            return;
        }

        try {
            for (int i = 0; i < tableClientes.getRowCount(); i++) {
                if (i != viewRow && Boolean.TRUE.equals(tableClientes.getValueAt(i, 0))) {
                    tableClientes.setValueAt(false, i, 0);
                }
            }
            tableClientes.setValueAt(true, viewRow, 0);
        } catch (Exception ignore) {
        }

        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBotones = new javax.swing.JPanel();
        lbUsuarioBusqueda = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        lbBuscar = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        btnImprimir = new javax.swing.JButton();
        jcbEstadoCliente = new javax.swing.JComboBox<>();
        lbEstado1 = new javax.swing.JLabel();
        jpDatosPersonales = new javax.swing.JPanel();
        lbNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        txtTelefono = new javax.swing.JTextField();
        lbTelefono = new javax.swing.JLabel();
        txtDNI = new javax.swing.JTextField();
        lbDireccion = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        lbApellido = new javax.swing.JLabel();
        lbDatosPersonales = new javax.swing.JLabel();
        lbDireccion2 = new javax.swing.JLabel();
        txtDireccion = new javax.swing.JTextField();
        jSeparator7 = new javax.swing.JSeparator();
        lbEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        btnGuardar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        txtRazonSocial = new javax.swing.JTextField();
        lbRazonSocial = new javax.swing.JLabel();
        txtCUIT = new javax.swing.JTextField();
        lbCUIT = new javax.swing.JLabel();
        lbValidacionCuit = new javax.swing.JLabel();
        jcbEstado = new javax.swing.JComboBox<>();
        lblEstado = new javax.swing.JLabel();
        panelLista = new javax.swing.JPanel();
        lbListCli = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scroll = new javax.swing.JScrollPane();
        tableClientes = new veterinaria.vista.table.AutoTable();
        btnImprimirLista = new javax.swing.JButton();

        lbUsuarioBusqueda.setText("BUSCAR CLIENTE");

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

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Gestión de Clientes");

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        lbEstado1.setText("Estado:");

        javax.swing.GroupLayout panelBotonesLayout = new javax.swing.GroupLayout(panelBotones);
        panelBotones.setLayout(panelBotonesLayout);
        panelBotonesLayout.setHorizontalGroup(
            panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBotonesLayout.createSequentialGroup()
                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator1)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelBotonesLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelBotonesLayout.createSequentialGroup()
                                .addComponent(lbBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBotonesLayout.createSequentialGroup()
                                        .addGap(0, 504, Short.MAX_VALUE)
                                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBotonesLayout.createSequentialGroup()
                                                .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(panelBotonesLayout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnImprimir, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addGroup(panelBotonesLayout.createSequentialGroup()
                                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lbUsuarioBusqueda))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbEstado1)
                                            .addComponent(jcbEstadoCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addContainerGap())
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)))
        );
        panelBotonesLayout.setVerticalGroup(
            panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonesLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbUsuarioBusqueda)
                    .addComponent(lbEstado1))
                .addGap(4, 4, 4)
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBotonesLayout.createSequentialGroup()
                        .addComponent(btnImprimir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnEditar)
                            .addComponent(btnEliminar)
                            .addComponent(btnNuevo))
                        .addGap(7, 7, 7))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBotonesLayout.createSequentialGroup()
                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelBotonesLayout.createSequentialGroup()
                                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jcbEstadoCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(3, 3, 3))
                            .addComponent(lbBuscar, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(57, 57, 57)))
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jpDatosPersonales.setMinimumSize(new java.awt.Dimension(0, 0));
        jpDatosPersonales.setPreferredSize(new java.awt.Dimension(838, 0));

        lbNombre.setText("Nombre: *");

        txtNombre.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtNombreKeyTyped(evt);
            }
        });

        txtTelefono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtTelefonoKeyTyped(evt);
            }
        });

        lbTelefono.setText("Teléfono: ");

        txtDNI.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtDNIKeyTyped(evt);
            }
        });

        lbDireccion.setText("DNI: *");

        txtApellido.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtApellidoKeyTyped(evt);
            }
        });

        lbApellido.setText("Apellido: *");

        lbDatosPersonales.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbDatosPersonales.setText("Datos Personales:");

        lbDireccion2.setText("Dirección: *");

        lbEmail.setText("Email: *");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        lbRazonSocial.setText("Razon Social:");

        lbCUIT.setText("CUIT:");

        lbValidacionCuit.setForeground(java.awt.Color.green);

        lblEstado.setText("Estado:");

        javax.swing.GroupLayout jpDatosPersonalesLayout = new javax.swing.GroupLayout(jpDatosPersonales);
        jpDatosPersonales.setLayout(jpDatosPersonalesLayout);
        jpDatosPersonalesLayout.setHorizontalGroup(
            jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator7)
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbDatosPersonales)
                            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lbApellido))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbDireccion)
                                            .addComponent(txtDNI, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lbDireccion2)))
                                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(lbEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtEmail, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                                            .addComponent(lblEstado)
                                            .addComponent(jcbEstado, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                                                .addGap(372, 372, 372)
                                                .addComponent(lbValidacionCuit, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(lbTelefono)
                                                    .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(lbRazonSocial)
                                                    .addComponent(txtRazonSocial, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(txtCUIT, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(lbCUIT))))))))
                        .addGap(0, 67, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosPersonalesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jpDatosPersonalesLayout.setVerticalGroup(
            jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                .addComponent(lbDatosPersonales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbDireccion2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbApellido)
                        .addGap(28, 28, 28))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbNombre)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbDireccion)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtDNI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtRazonSocial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbEmail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addComponent(lbCUIT)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCUIT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lbRazonSocial)
                            .addComponent(lbTelefono))
                        .addGap(28, 28, 28)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblEstado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbValidacionCuit, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnLimpiar)
                    .addComponent(btnCancelar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        lbListCli.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListCli.setText("Lista de Clientes");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableClientes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "ID", "Nombre", "Apellido", "DNI", "Dirección", "E-mail", "Teléfono", "Razon Social", "CUIT", "Estado", "Fecha de Alta"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableClientes.setMinimumSize(new java.awt.Dimension(0, 0));
        // JTable calcula su altura real a partir de todas las filas del modelo.
        // Un preferredSize fijo de 220 px recortaba los registros al desplazarse.
        tableClientes.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableClientes);
        tableClientes.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tableClientes.setFillsViewportHeight(true);
        scroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        btnImprimirLista.setText("Imprimir Lista");
        btnImprimirLista.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirListaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelListaLayout = new javax.swing.GroupLayout(panelLista);
        panelLista.setLayout(panelListaLayout);
        panelListaLayout.setHorizontalGroup(
            panelListaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 823, Short.MAX_VALUE)
            .addGroup(panelListaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbListCli)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnImprimirLista)
                .addContainerGap())
            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        panelListaLayout.setVerticalGroup(
            panelListaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelListaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelListaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListCli)
                    .addComponent(btnImprimirLista))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 9, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(panelLista, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jpDatosPersonales, javax.swing.GroupLayout.DEFAULT_SIZE, 829, Short.MAX_VALUE)
                    .addComponent(panelBotones, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelBotones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatosPersonales, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelLista, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        Application.actualizarEstadoUsuario("CreandoCliente");

        validar.configurarValidacionCUIT(txtCUIT, lbValidacionCuit);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        jpDatosPersonales.setVisible(true);
        panelLista.setVisible(false);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        // 1. Verificación de permisos por RBAC
        String perm = "FormCliente.EDITAR";
        if (sesion == null || !sesion.puede(perm)) {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para editar clientes.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // 2. Actualizamos estado de usuario y delegamos la lógica a actualizarCliente()
        Application.actualizarEstadoUsuario("EditandoCliente");
        actualizarCliente();
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        String perm = "FormCliente.ELIMINAR";
        if (sesion == null || !sesion.puede(perm)) {
            JOptionPane.showMessageDialog(null, "No tiene permisos suficientes para desactivar clientes.", "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cliente clienteSeleccionado = obtenerClienteSeleccionadoDesdeTabla();
        if (clienteSeleccionado == null) {
            JOptionPane.showMessageDialog(null, "Seleccione un cliente para desactivar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean reactivar = !clienteSeleccionado.isActivo();
        try {
            int confirm = JOptionPane.showConfirmDialog(this,
                    reactivar ? "¿Reactivar este cliente y sus mascotas?" :
                            "¿Desactivar este cliente y sus mascotas?",
                    reactivar ? "Confirmar reactivación" : "Confirmar desactivación",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                ClienteControlador controlador = new ClienteControlador();
                boolean ok = reactivar
                        ? controlador.procesarReactivacionPorId(clienteSeleccionado.getIdCliente())
                        : controlador.procesarBajaPorId(clienteSeleccionado.getIdCliente());
                JOptionPane.showMessageDialog(this,
                        ok ? (reactivar ? "Cliente reactivado." : "Cliente desactivado.")
                           : "No se pudo completar la operación.");
                cargarClientesEnTabla();
                limpiarForm();
                jpDatosPersonales.setVisible(false);
            }
        } catch (Exception ex) {
            String detalle = ex.getMessage() == null ? "" : ex.getMessage();
            Throwable causa = ex.getCause();
            while (causa != null) {
                if (causa.getMessage() != null
                        && causa.getMessage().contains("cuenta corriente tiene saldo")) {
                    detalle = causa.getMessage();
                    break;
                }
                causa = causa.getCause();
            }
            boolean saldoPendiente = detalle.contains("cuenta corriente tiene saldo");
            String mensaje = saldoPendiente
                    ? "No se puede desactivar este cliente porque su cuenta corriente "
                      + "registra un saldo pendiente o a favor.\n"
                      + "Regularice el saldo antes de realizar la desactivación.\n"
                      + "El cliente y sus mascotas permanecen activos."
                    : "No se pudo completar la desactivación del cliente.\n" + detalle;
            JOptionPane.showMessageDialog(this, mensaje,
                    saldoPendiente ? "Desactivación no permitida" : "Error al desactivar",
                    saldoPendiente ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarForm();
        jpDatosPersonales.setVisible(false);
        panelLista.setVisible(true);
        setEstadoBotones(true);
        tableClientes.setVisible(true);

    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarForm();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (validarDatosCliente()) {

            String estadoUsuario = Application.consultarEstadoUsuario();

            if ("CreandoCliente".equals(estadoUsuario) && !confirmarCreacionCliente()) {
                return;
            }
            if ("EditandoCliente".equals(estadoUsuario) && !confirmarEdicionCliente()) {
                return;
            }

            try {
                ClienteControlador operandoCliente = new ClienteControlador();

                boolean exito = operandoCliente.procesarGuardado(
                        estadoUsuario,
                        this.cliente,
                        this.persona,
                        txtNombre.getText(),
                        txtApellido.getText(),
                        txtDNI.getText(),
                        txtDireccion.getText(),
                        txtTelefono.getText(),
                        txtEmail.getText(),
                        txtCUIT.getText(),
                        txtRazonSocial.getText(),
                        "CreandoCliente".equals(estadoUsuario) ? "Activo" : (this.cliente.isActivo() ? "Activo" : "Inactivo")
                );

                if (exito) {
                    JOptionPane.showMessageDialog(this, "Operación realizada con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);

                    limpiarForm();
                    panelLista.setVisible(true);
                    jpDatosPersonales.setVisible(false);
                    cargarClientesEnTabla();

                    setEstadoBotones(true);
                }

                // ⚠️ ELIMINADO: actualizarCliente(); <-- Esto estaba de más y rompía el flujo al guardar.
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo procesar la solicitud:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnGuardarActionPerformed
    private void setEstadoBotones(boolean activo) {
        actualizarAccionesCliente();
        // Si agregás más botones, solo los sumás acá
    }
    private void txtApellidoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtApellidoKeyTyped
        validar.validarSoloLetras(evt);
    }//GEN-LAST:event_txtApellidoKeyTyped

    private void txtTelefonoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTelefonoKeyTyped
        validar.validarSoloNumeros(evt);
    }//GEN-LAST:event_txtTelefonoKeyTyped

    private void txtNombreKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtNombreKeyTyped
        validar.validarSoloLetras(evt);
    }//GEN-LAST:event_txtNombreKeyTyped

    private void txtDNIKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDNIKeyTyped
        validar.validarSoloNumeros(evt);
    }//GEN-LAST:event_txtDNIKeyTyped

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        imprimirRegistroSeleccionado();
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void btnImprimirListaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirListaActionPerformed
        javax.swing.JComboBox<String> alcance = new javax.swing.JComboBox<>(
                new String[]{"Todos los clientes", "Solo activos", "Solo inactivos"});
        javax.swing.JComboBox<String> formato = new javax.swing.JComboBox<>(
                new String[]{"PDF", "Excel (XLS)"});
        javax.swing.JPanel opciones = new javax.swing.JPanel(new java.awt.GridLayout(2, 2, 10, 10));
        opciones.add(new javax.swing.JLabel("Clientes a incluir:"));
        opciones.add(alcance);
        opciones.add(new javax.swing.JLabel("Formato:"));
        opciones.add(formato);
        if (JOptionPane.showConfirmDialog(this, opciones, "Configurar informe de clientes",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String estado = alcance.getSelectedIndex() == 1 ? "ACTIVO"
                : alcance.getSelectedIndex() == 2 ? "INACTIVO" : "TODOS";
        ReporteSelectorUtil.Formato elegido = formato.getSelectedIndex() == 0
                ? ReporteSelectorUtil.Formato.PDF : ReporteSelectorUtil.Formato.XLS;
        imprimirListaClientes(elegido, estado);
    }//GEN-LAST:event_btnImprimirListaActionPerformed

    private String obtenerFiltroEstadoClienteSeleccionado() {
        Object seleccionado = jcbEstadoCliente.getSelectedItem();
        if (seleccionado != null) {
            String textoSeleccionado = seleccionado.toString();

            if (textoSeleccionado.equalsIgnoreCase("Activo") || textoSeleccionado.equalsIgnoreCase("Activos")) {
                return "ACTIVO";
            } else if (textoSeleccionado.equalsIgnoreCase("Inactivo") || textoSeleccionado.equalsIgnoreCase("Inactivos")) {
                return "INACTIVO";
            } else {
                return "TODOS";
            }
        }
        return "ACTIVO";
    }

    private Cliente obtenerClienteSeleccionadoDesdeTabla() {
        int filaSeleccionadaVista = -1;

        // Buscamos qué fila tiene el checkbox tildado (columna 0)
        for (int i = 0; i < tableClientes.getRowCount(); i++) {
            Object val = tableClientes.getValueAt(i, 0);
            if (val instanceof Boolean && (Boolean) val) {
                filaSeleccionadaVista = i;
                break;
            }
        }

        if (filaSeleccionadaVista == -1) {
            return null;
        }

        try {
            // La paginación puede utilizar un modelo de tabla intermediario:
            // su índice de fila no corresponde necesariamente a modeloClientes.
            // Recuperamos el ID desde la columna del MODELO (aunque esté oculta
            // en la vista) y buscamos el objeto por su identificador estable.
            int columnaIdVista = tableClientes.convertColumnIndexToView(COL_ID_CLIENTE);
            Object idValor = columnaIdVista >= 0
                    ? tableClientes.getValueAt(filaSeleccionadaVista, columnaIdVista)
                    : tableClientes.getModel().getValueAt(
                            tableClientes.convertRowIndexToModel(filaSeleccionadaVista), COL_ID_CLIENTE);
            if (idValor == null) {
                return null;
            }
            int id = idValor instanceof Number
                    ? ((Number) idValor).intValue()
                    : Integer.parseInt(idValor.toString());
            return clienteDAO.buscarPorId(id);
        } catch (Exception ex) {
            System.err.println("Error al obtener objeto de la tabla: " + ex.getMessage());
        }

        return null;
    }

    private void cargarClientesEnTabla() {
        String filtroEstado = obtenerFiltroEstadoClienteSeleccionado(); // Ej: "Todos", "Activos", "Eliminados"

        // Llamada directa y unificada al DAO/Controlador
        List<Cliente> clientes = clienteControlador.buscarClientePorEstado(filtroEstado);

        // Formateador para la fecha de alta
        //java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (modeloClientes == null) {
            String[] columnas = {
                "Seleccionar", "ID", "Nombre", "Apellido", "DNI", "Dirección", "Email", "Teléfono", "Razón Social", "CUIT", "Estado", "Fecha Alta"
            };

            modeloClientes = operarTablas.configurarTablaGenerica(tableClientes, columnas, (clienteBucle, columnIndex) -> {
                if (clienteBucle == null) {
                    return null;
                }

                String fechaStr = "";
                if (clienteBucle.getFechaAlta() != null) {
                    fechaStr = clienteBucle.getFechaAlta().format(formatter);
                }

                String nombre = (clienteBucle.getPersona() != null) ? clienteBucle.getPersona().getNombre() : "";
                String apellido = (clienteBucle.getPersona() != null) ? clienteBucle.getPersona().getApellido() : "";
                String dni = (clienteBucle.getPersona() != null) ? clienteBucle.getPersona().getDni() : "";
                String direccion = (clienteBucle.getPersona() != null) ? clienteBucle.getPersona().getDireccion() : "";
                String telefono = (clienteBucle.getPersona() != null) ? clienteBucle.getPersona().getTelefono() : "";

                switch (columnIndex) {
                    case 1:
                        return clienteBucle.getIdCliente();
                    case 2:
                        return nombre;
                    case 3:
                        return apellido;
                    case 4:
                        return dni;
                    case 5:
                        return direccion;
                    case 6:
                        return clienteBucle.getEmail();
                    case 7:
                        return telefono;
                    case 8:
                        return clienteBucle.getRazonSocial();
                    case 9:
                        return clienteBucle.getCuit();
                    case 10:
                        return clienteBucle.isActivo() ? "Activo" : "Inactivo";
                    case 11:
                        return fechaStr;
                    default:
                        return null;
                }
            });

            operarTablas.ocultarColumnaId(tableClientes, 1);
            ocultarColumnasSecundariasCliente();
        }

        // Cargamos los datos limpios al modelo genérico
        if (modeloClientes != null) {
            modeloClientes.cargarDatos(clientes);
        }

        if (btnEditar != null) {
            btnEditar.setEnabled(false);
        }
        if (btnEliminar != null) {
            btnEliminar.setEnabled(false);
        }
        btnCancelarSeleccion.setVisible(false);
        btnVerInfo.setVisible(false);

        instalarDegradadoSeleccionClientes();
        actualizarTabla();
        if (paginacionCompartida != null) paginacionCompartida.actualizar();
    }

    /**
     * Solo pinta las celdas de texto; preserva el checkbox y el modelo existente.
     * No modifica anchos, filas, scroll ni tipografía.
     */
    private void ocultarColumnasSecundariasCliente() {
        // Ocultar solo en la vista: los datos y el ID siguen en el modelo.
        javax.swing.table.TableColumnModel columnas = tableClientes.getColumnModel();
        for (int indiceModelo : new int[]{COL_RAZON_SOCIAL, COL_CUIT, COL_FECHA_ALTA}) {
            for (int i = columnas.getColumnCount() - 1; i >= 0; i--) {
                if (columnas.getColumn(i).getModelIndex() == indiceModelo) {
                    columnas.removeColumn(columnas.getColumn(i));
                    break;
                }
            }
        }
    }

    private void mostrarInfoCliente() {
        Cliente cliente = obtenerClienteSeleccionadoDesdeTabla();
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente.");
            return;
        }
        Persona p = cliente.getPersona();
        java.awt.Color verde = new java.awt.Color(66, 190, 115);
        javax.swing.JPanel ficha = new javax.swing.JPanel();
        ficha.setLayout(new javax.swing.BoxLayout(ficha, javax.swing.BoxLayout.Y_AXIS));
        ficha.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));

        agregarSeccionFicha(ficha, "DATOS PERSONALES", verde);
        javax.swing.JPanel personales = new javax.swing.JPanel(new java.awt.GridLayout(0, 2, 18, 12));
        personales.setOpaque(false);
        agregarDatoFicha(personales, "Nombre y apellido", p == null ? null
                : valorFicha(p.getNombre()) + " " + valorFicha(p.getApellido()));
        agregarDatoFicha(personales, "DNI", p == null ? null : String.valueOf(p.getDni()));
        agregarDatoFicha(personales, "Teléfono", p == null ? null : p.getTelefono());
        agregarDatoFicha(personales, "Dirección", p == null ? null : p.getDireccion());
        agregarDatoFicha(personales, "Email", cliente.getEmail());
        agregarDatoFicha(personales, "Estado", cliente.isActivo() ? "Activo" : "Inactivo");
        personales.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(personales);

        agregarSeccionFicha(ficha, "DATOS FISCALES Y ADMINISTRATIVOS", verde);
        javax.swing.JPanel fiscales = new javax.swing.JPanel(new java.awt.GridLayout(0, 2, 18, 12));
        fiscales.setOpaque(false);
        agregarDatoFicha(fiscales, "Razón social", cliente.getRazonSocial());
        agregarDatoFicha(fiscales, "CUIT", cliente.getCuit());
        agregarDatoFicha(fiscales, "Fecha de alta", cliente.getFechaAlta() == null
                ? null : cliente.getFechaAlta().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        fiscales.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(fiscales);

        agregarSeccionFicha(ficha, "MASCOTAS ASOCIADAS", verde);
        try {
            java.util.List<veterinaria.entidad.Mascota> mascotas =
                    new veterinaria.persistencia.MascotaDAO()
                            .buscarTodasPorCliente(cliente.getIdCliente());
            if (mascotas.isEmpty()) {
                ficha.add(new javax.swing.JLabel("Sin mascotas registradas"));
            }
            for (veterinaria.entidad.Mascota m : mascotas) {
                javax.swing.JPanel tarjeta = new javax.swing.JPanel(new java.awt.BorderLayout(10, 0));
                tarjeta.setBackground(javax.swing.UIManager.getColor("Table.background"));
                tarjeta.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(new java.awt.Color(72, 113, 96)),
                        javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14)));
                javax.swing.JLabel nombre = new javax.swing.JLabel("Mascota: " + valorFicha(m.getNombre()));
                nombre.setFont(nombre.getFont().deriveFont(java.awt.Font.BOLD, 13f));
                nombre.setForeground(verde);
                javax.swing.JLabel detalle = new javax.swing.JLabel(valorFicha(m.getEspecie())
                        + " · " + valorFicha(m.getRaza()) + " · " + valorFicha(m.getSexo()));
                javax.swing.JLabel estado = new javax.swing.JLabel(m.isActivo() ? "Activa" : "Inactiva");
                estado.setForeground(m.isActivo() ? verde : java.awt.Color.GRAY);
                javax.swing.JPanel info = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 5));
                info.setOpaque(false);
                info.add(nombre);
                info.add(detalle);
                tarjeta.add(info, java.awt.BorderLayout.CENTER);
                tarjeta.add(estado, java.awt.BorderLayout.EAST);
                tarjeta.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 72));
                tarjeta.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                ficha.add(tarjeta);
                ficha.add(javax.swing.Box.createVerticalStrut(8));
            }
        } catch (Exception ex) {
            ficha.add(new javax.swing.JLabel("No se pudieron consultar las mascotas asociadas"));
        }
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(ficha);
        scroll.setPreferredSize(new java.awt.Dimension(640, 480));
        scroll.setBorder(null);
        JOptionPane.showMessageDialog(this, scroll, "Información del Cliente · Solo lectura",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void agregarSeccionFicha(javax.swing.JPanel ficha, String titulo, java.awt.Color verde) {
        ficha.add(javax.swing.Box.createVerticalStrut(18));
        javax.swing.JLabel etiqueta = new javax.swing.JLabel(titulo);
        etiqueta.setFont(etiqueta.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        etiqueta.setForeground(verde);
        etiqueta.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        etiqueta.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        ficha.add(etiqueta);
        ficha.add(javax.swing.Box.createVerticalStrut(12));
    }

    private void agregarDatoFicha(javax.swing.JPanel contenedor, String etiqueta, String valor) {
        javax.swing.JPanel campo = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 3));
        campo.setOpaque(false);
        javax.swing.JLabel titulo = new javax.swing.JLabel(etiqueta);
        titulo.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
        javax.swing.JLabel dato = new javax.swing.JLabel(valorFicha(valor));
        dato.setFont(dato.getFont().deriveFont(java.awt.Font.BOLD));
        campo.add(titulo);
        campo.add(dato);
        contenedor.add(campo);
    }

    private String valorFicha(String valor) {
        return valor == null || valor.trim().isEmpty() ? "No registrado" : valor;
    }

    private void instalarDegradadoSeleccionClientes() {
        javax.swing.table.TableColumnModel columns = tableClientes.getColumnModel();
        for (int col = 0; col < columns.getColumnCount(); col++) {
            int modelColumn = columns.getColumn(col).getModelIndex();
            if (modelColumn != COL_SELECCION && modelColumn != COL_ID_CLIENTE) {
                columns.getColumn(col).setCellRenderer(new TableGradientCell());
            }
        }
        columns.getColumn(0).setCellRenderer(
                new veterinaria.vista.table.TableGradientCheckBoxRenderer());
        tableClientes.repaint();
    }

    private void actualizarTabla() {
        if (listenerSeleccionUnicaInstalado) {
            return;
        }

        tableClientes.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0 || e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                // Reemplazado por clase anónima para evitar incompatibilidades
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        verificarSeleccionTabla();
                    }
                });
            }
        });

        listenerSeleccionUnicaInstalado = true;
    }

    private void verificarSeleccionTabla() {
        boolean haySeleccion = false;
        int rowCount = tableClientes.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            Object valor = tableClientes.getValueAt(i, 0);
            if (valor instanceof Boolean && (Boolean) valor) {
                haySeleccion = true;
                break;
            }
        }

        if (btnEditar != null) {
            btnEditar.setVisible(haySeleccion);
            btnEditar.setEnabled(haySeleccion);
        }
        if (btnEliminar != null) {
            btnEliminar.setVisible(haySeleccion);
            btnEliminar.setEnabled(haySeleccion);
        }
        if (btnImprimir != null) {
            btnImprimir.setVisible(haySeleccion);
        }
        btnCancelarSeleccion.setVisible(haySeleccion);
        btnVerInfo.setVisible(haySeleccion);
        actualizarTextoAccionCliente();
        btnNuevo.setEnabled(!haySeleccion);
    }

    private void ocultarColumnaIdCliente() {
        if (tableClientes.getColumnCount() > COL_ID_CLIENTE) {
            tableClientes.getColumnModel().getColumn(COL_ID_CLIENTE).setMinWidth(0);
            tableClientes.getColumnModel().getColumn(COL_ID_CLIENTE).setMaxWidth(0);
            tableClientes.getColumnModel().getColumn(COL_ID_CLIENTE).setPreferredWidth(0);
        }
    }

    // -------------------- Impresión PDF --------------------
    private void actualizarBotonImprimir() {
        boolean haySeleccion = (operarTablas.comprobarElementoSeleccionado(tableClientes) != -1);
        actualizarAccionesCliente();
    }

    private void imprimirRegistroSeleccionado() {
        Cliente clienteSeleccionado = obtenerClienteSeleccionadoDesdeTabla();

        if (clienteSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un cliente de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ReporteSelectorUtil.Formato formato = ReporteSelectorUtil.solicitarFormato(this);
        if (formato == null) {
            return;
        }

        try {
            // 🌟 Obtenemos los datos completos actualizados desde el controlador usando el ID
            ClienteControlador operandoCliente = new ClienteControlador();
            Cliente clienteCompleto = operandoCliente.obtenerClienteParaReporte(clienteSeleccionado.getIdCliente());

            // Armamos el request visual
            ReporteRequest request = new ReporteRequest();
            request.put("cliente", clienteCompleto);
            request.put("persona", clienteCompleto.getPersona());
            request.put("emisor", Application.getNombreApellidoUsuarioLogeado());
            request.put("FORMATO_REPORTE", formato);

            veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.CLIENTE_REGISTRO, request);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirListaClientes(ReporteSelectorUtil.Formato formato, String estadoFiltro) {
        try {
            // 1. Buscamos los datos reales en la base de datos según el filtro
            List<Cliente> listaParaImprimir = clienteControlador.buscarClientePorEstado(estadoFiltro);

            // 2. Validamos si hay datos para evitar imprimir en blanco
            if (listaParaImprimir == null || listaParaImprimir.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay datos para imprimir con el estado seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Creamos una tabla exclusiva para el reporte con la lista consultada.
            // Así la impresión no depende del filtro o contenido visible de la pantalla.
            javax.swing.JTable tablaReporte = crearTablaClientesParaReporte(listaParaImprimir);

            ReporteRequest req = new ReporteRequest()
                    .put("tabla", tablaReporte)
                    .put("emisor", Application.getNombreApellidoUsuarioLogeado())
                    .put("estadoFiltro", estadoFiltro)
                    .put("FORMATO_REPORTE", formato);

            // Ejecutamos el reporte
            veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.CLIENTE_LISTADO, req);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocurrió un error al preparar la impresión: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private javax.swing.JTable crearTablaClientesParaReporte(List<Cliente> clientes) {
        String[] columnas = {
            "Seleccionar", "ID", "Nombre", "Apellido", "DNI", "Dirección",
            "Email", "Teléfono", "Razón Social", "CUIT", "Estado", "Fecha Alta"
        };

        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        java.time.format.DateTimeFormatter formatoFecha
                = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Cliente c : clientes) {
            Persona p = c != null ? c.getPersona() : null;
            String fechaAlta = c != null && c.getFechaAlta() != null
                    ? c.getFechaAlta().format(formatoFecha) : "";

            modelo.addRow(new Object[]{
                false,
                c != null ? c.getIdCliente() : null,
                p != null ? p.getNombre() : "",
                p != null ? p.getApellido() : "",
                p != null ? p.getDni() : "",
                p != null ? p.getDireccion() : "",
                c != null ? c.getEmail() : "",
                p != null ? p.getTelefono() : "",
                c != null ? c.getRazonSocial() : "",
                c != null ? c.getCuit() : "",
                c != null && c.isActivo() ? "ACTIVO" : "INACTIVO",
                fechaAlta
            });
        }

        return new javax.swing.JTable(modelo);
    }

    private boolean validarDatosCliente() {
        if (txtNombre.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre del Cliente no puede estar vacío.");
            return false;
        }
        if (txtApellido.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El apellido del Cliente no puede estar vacío.");
            return false;
        }
        if (txtDNI.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El DNI del Cliente no puede estar vacío.");
            return false;
        }
        if (txtDireccion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "La dirección del cliente no puede estar vacía.");
            return false;
        }
        if (!validar.validarCorreoValido(txtEmail.getText().trim())) {
            JOptionPane.showMessageDialog(null, "Debes ingresar un correo válido.");
            return false;
        }

        // NUEVA VALIDACIÓN: Obliga al usuario a elegir un estado real
       /* if (jcbEstado.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(null, "Por favor, seleccione un estado válido (ACTIVO o INACTIVO) para el cliente.");
            return false;
        }*/

        return true;
    }

    private boolean confirmarCreacionCliente() {
        String estadoSeleccionado = (jcbEstado.getSelectedItem() == null) ? "" : jcbEstado.getSelectedItem().toString();

        String resumenHtml = "<html><body style='width:380px'>"
                + "<p>Confirme la creación del cliente con los siguientes datos:</p>"
                + "<b>Nombre:</b> " + escapeHtml(txtNombre.getText().trim()) + "<br>"
                + "<b>Apellido:</b> " + escapeHtml(txtApellido.getText().trim()) + "<br>"
                + "<b>DNI:</b> " + escapeHtml(txtDNI.getText().trim()) + "<br>"
                + "<b>Teléfono:</b> " + escapeHtml(txtTelefono.getText().trim()) + "<br>"
                + "<b>Dirección:</b> " + escapeHtml(txtDireccion.getText().trim()) + "<br>"
                + "<b>Email:</b> " + escapeHtml(txtEmail.getText().trim()) + "<br>"
                + "<b>Razón Social:</b> " + escapeHtml(txtRazonSocial.getText().trim()) + "<br>"
                + "<b>CUIT:</b> " + escapeHtml(txtCUIT.getText().trim()) + "<br>"
                // Agregamos la línea del estado en el resumen visual
                + "<b>Estado:</b> " + escapeHtml(estadoSeleccionado)
                + "</body></html>";

        Object[] opciones = {"SI", "NO"};
        int opcion = JOptionPane.showOptionDialog(
                this,
                resumenHtml,
                "Confirmar creación de cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        return opcion == 0;
    }

    private boolean confirmarEdicionCliente() {
        String nuevoNombre = txtNombre.getText().trim();
        String nuevoApellido = txtApellido.getText().trim();
        String nuevoDni = txtDNI.getText().trim();
        String nuevoTelefono = txtTelefono.getText().trim();
        String nuevoDireccion = txtDireccion.getText().trim();
        String nuevoEmail = txtEmail.getText().trim();
        String nuevoRazonSocial = txtRazonSocial.getText().trim();
        String nuevoCuit = txtCUIT.getText().trim();
        // capturamos el estado actual del JComboBox
        String nuevoEstado = originalEstado;

        StringBuilder cambios = new StringBuilder();
        cambios.append("<html><body style='width:420px'>");
        cambios.append("<p>Se modificarán los siguientes datos:</p>");

        int cambiosCount = 0;
        cambiosCount += appendCambio(cambios, "Nombre", originalNombre, nuevoNombre);
        cambiosCount += appendCambio(cambios, "Apellido", originalApellido, nuevoApellido);
        cambiosCount += appendCambio(cambios, "DNI", originalDni, nuevoDni);
        cambiosCount += appendCambio(cambios, "Teléfono", originalTelefono, nuevoTelefono);
        cambiosCount += appendCambio(cambios, "Dirección", originalDireccion, nuevoDireccion);
        cambiosCount += appendCambio(cambios, "Email", originalEmail, nuevoEmail);
        cambiosCount += appendCambio(cambios, "Razón Social", originalRazonSocial, nuevoRazonSocial);
        cambiosCount += appendCambio(cambios, "CUIT", originalCuit, nuevoCuit);
        // Sumamos el cambio de estado a la lista visual si es que cambió


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
                "Confirmar edición de cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        return opcion == 0;
    }

    private void capturarOriginalesCliente() {
        originalNombre = (txtNombre.getText() == null) ? "" : txtNombre.getText().trim();
        originalApellido = (txtApellido.getText() == null) ? "" : txtApellido.getText().trim();
        originalDni = (txtDNI.getText() == null) ? "" : txtDNI.getText().trim();
        originalDireccion = (txtDireccion.getText() == null) ? "" : txtDireccion.getText().trim();
        originalTelefono = (txtTelefono.getText() == null) ? "" : txtTelefono.getText().trim();
        originalEmail = (txtEmail.getText() == null) ? "" : txtEmail.getText().trim();
        originalRazonSocial = (txtRazonSocial.getText() == null) ? "" : txtRazonSocial.getText().trim();
        originalCuit = (txtCUIT.getText() == null) ? "" : txtCUIT.getText().trim();
        originalEstado = (jcbEstado.getSelectedItem() == null) ? "" : jcbEstado.getSelectedItem().toString();
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

    private void limpiarForm() {
        // 1. Limpieza de todos los componentes visuales (UI)
        txtNombre.setText("");
        txtApellido.setText("");
        txtDNI.setText("");
        txtDireccion.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtBusqueda.setText("");
        txtRazonSocial.setText("");
        txtCUIT.setText("");

        // Resetea el combo a la instrucción inicial (índice 0)
        jcbEstado.setSelectedIndex(0);

        // 2. Reseteo de las instancias de la vista (las dejamos limpias)
        this.persona = null;
        this.cliente = null;

        // 3. Refresco visual de la JTable
        cargarClientesEnTabla();
    }

    private void actualizarCliente() {
        // Control de selección de la interfaz gráfica (compatible con filtros)
        Cliente seleccionado = obtenerClienteSeleccionadoDesdeTabla();

        if (seleccionado != null) {
            Integer idSel = seleccionado.getIdCliente();

            if (idSel == null) {
                JOptionPane.showMessageDialog(this, "No se pudo determinar el ID del cliente seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                ClienteControlador operandoCliente = new ClienteControlador();

                this.cliente = operandoCliente.obtenerClienteParaEdicion(idSel);
                System.out.println("Cliente ID: " + this.cliente.getIdCliente());
                System.out.println("Estado BD: " + this.cliente.getEstado());
                System.out.println("Activo(): " + this.cliente.isActivo());
                if (this.cliente != null) {
                    this.persona = this.cliente.getPersona();
                }

                if (this.persona != null) {
                    txtNombre.setText(this.persona.getNombre() != null ? this.persona.getNombre() : "");
                    txtApellido.setText(this.persona.getApellido() != null ? this.persona.getApellido() : "");
                    txtDNI.setText(this.persona.getDni() != null ? this.persona.getDni() : "");
                    txtDireccion.setText(this.persona.getDireccion() != null ? this.persona.getDireccion() : "");
                    txtTelefono.setText(this.persona.getTelefono() != null ? this.persona.getTelefono() : "");
                }
                txtEmail.setText(this.cliente.getEmail() != null ? this.cliente.getEmail() : "");
                txtRazonSocial.setText(this.cliente.getRazonSocial() != null ? this.cliente.getRazonSocial() : "");
                txtCUIT.setText(this.cliente.getCuit() != null ? this.cliente.getCuit() : "");

                // CORREGIDO: Llenamos primero el combo de edición y usamos jcbEstado (no jcbEstadoCliente)
                jcbEstado.removeAllItems();
                jcbEstado.addItem(EstadoCliente.ACTIVO.getDescripcion());
                jcbEstado.addItem(EstadoCliente.INACTIVO.getDescripcion());

                if (this.cliente.getEstado() != null) {

                    String estado = this.cliente.getEstado().trim();

                    if ("ACTIVO".equalsIgnoreCase(estado)) {
                        jcbEstado.setSelectedItem(EstadoCliente.ACTIVO.getDescripcion());
                    } else if ("INACTIVO".equalsIgnoreCase(estado)) {
                        jcbEstado.setSelectedItem(EstadoCliente.INACTIVO.getDescripcion());
                    }

                } else {
                    jcbEstado.setSelectedIndex(0);
                }
                jcbEstado.setVisible(false);
                capturarOriginalesCliente();

                // Mostramos el panel de edición y ocultamos la lista
                jpDatosPersonales.setVisible(true);
                if (panelLista != null) {
                    panelLista.setVisible(false);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
            }

        } else {
            JOptionPane.showMessageDialog(null, "Por favor seleccione un cliente a EDITAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JComboBox<String> jcbEstado;
    private javax.swing.JComboBox<String> jcbEstadoCliente;
    private javax.swing.JPanel jpDatosPersonales;
    private javax.swing.JLabel lbApellido;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbCUIT;
    private javax.swing.JLabel lbDatosPersonales;
    private javax.swing.JLabel lbDireccion;
    private javax.swing.JLabel lbDireccion2;
    private javax.swing.JLabel lbEmail;
    private javax.swing.JLabel lbEstado1;
    private javax.swing.JLabel lbListCli;
    private javax.swing.JLabel lbNombre;
    private javax.swing.JLabel lbRazonSocial;
    private javax.swing.JLabel lbTelefono;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JLabel lbValidacionCuit;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelLista;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tableClientes;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextField txtCUIT;
    private javax.swing.JTextField txtDNI;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtRazonSocial;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables

}
