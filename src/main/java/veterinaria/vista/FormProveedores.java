package veterinaria.vista;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.ProveedorControlador;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.Rol;
import veterinaria.persistencia.PersonaDAO;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.core.ReporteService;
import veterinaria.util.ManejoTablas;
import veterinaria.util.SesionUsuario;
import veterinaria.vista.application.Application;
import veterinaria.util.Validaciones;
import veterinaria.util.PermisoUI;

public class FormProveedores extends javax.swing.JPanel {

    private final ProveedorControlador operandoProveedor = new ProveedorControlador();
    private final PersonaDAO personaDAO = new PersonaDAO();
    private final Validaciones validar = new Validaciones();
    private boolean cuitListenerConfigurado = false;
    private final ManejoTablas operarTabla = new ManejoTablas();
    private Proveedor proveedor = new Proveedor();
    private Persona personaProveedor = new Persona();
    private SesionUsuario sesion = Application.getSesionUsuario();
    private Rol rol = sesion.getRol();

    public FormProveedores() {
        initComponents();
        PermisoUI.aplicar(this);
        initListeners();

        // Evitar duplicar KeyListeners sobre CUIT (se llamaba en Nuevo/Editar)
        asegurarValidacionCUIT();
        cargarCombosBoxs();
        jpDatosPersonales.setVisible(false);
        jpDatosProveedor.setVisible(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        cargarProveedoresEnTabla();

        // PDF
        btnImprimir.setVisible(false); // solo visible si hay un registro seleccionado
        btnImprimirLista.addActionListener(evt -> imprimirListaProveedores());
        btnBuscar.addActionListener(evt -> ejecutarBusqueda());

        List<Integer> columnasObjetivo = Arrays.asList(1, 2, 3, 7, 8);
        operarTabla.aplicarFiltroYResaltado(tableProveedores, txtBusqueda, columnasObjetivo);
        /*List<Integer> indicesColumnasConTooltip = Arrays.asList(2, 3, 5, 6);
        operarTabla.mouseTooltipText(tableProveedores, indicesColumnasConTooltip);*/
    }

    private void asegurarValidacionCUIT() {
        if (cuitListenerConfigurado) {
            return;
        }
        try {
            validar.configurarValidacionCUIT(txtCUIT, lbValidacionCuit);
            cuitListenerConfigurado = true;
        } catch (Exception ignore) {
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelHadear = new javax.swing.JPanel();
        lbProveedores = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        panelBotones = new javax.swing.JPanel();
        lbUsuarioBusqueda = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnBuscar = new javax.swing.JButton();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        lbBuscar = new javax.swing.JLabel();
        btnImprimir = new javax.swing.JButton();
        jpDatosPersonales = new javax.swing.JPanel();
        txtTelefono = new javax.swing.JTextField();
        lbTelefono = new javax.swing.JLabel();
        txtDNI = new javax.swing.JTextField();
        lbDireccion = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        lbApellido = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lbNombre = new javax.swing.JLabel();
        lbDatosPersonales = new javax.swing.JLabel();
        lbDireccion2 = new javax.swing.JLabel();
        txtDireccion = new javax.swing.JTextField();
        jSeparator7 = new javax.swing.JSeparator();
        jpDatosProveedor = new javax.swing.JPanel();
        lbDatosPersonales1 = new javax.swing.JLabel();
        jcbRubro = new javax.swing.JComboBox<>();
        lbFechaNacimiento = new javax.swing.JLabel();
        txtRazonSocial = new javax.swing.JTextField();
        lbNombre1 = new javax.swing.JLabel();
        lbEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        jcbEstado = new javax.swing.JComboBox<>();
        lbEstado = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JSeparator();
        txtCUIT = new javax.swing.JTextField();
        lbCUIT = new javax.swing.JLabel();
        lbValidacionCuit = new javax.swing.JLabel();
        jpListaProveedores = new javax.swing.JPanel();
        lbListProv = new javax.swing.JLabel();
        scroll = new javax.swing.JScrollPane();
        tableProveedores = new veterinaria.vista.table.AutoTable();
        jSeparator9 = new javax.swing.JSeparator();
        btnImprimirLista = new javax.swing.JButton();

        lbProveedores.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbProveedores.setText("Proveedores");

        javax.swing.GroupLayout panelHadearLayout = new javax.swing.GroupLayout(panelHadear);
        panelHadear.setLayout(panelHadearLayout);
        panelHadearLayout.setHorizontalGroup(
            panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(panelHadearLayout.createSequentialGroup()
                        .addComponent(lbProveedores)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelHadearLayout.setVerticalGroup(
            panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbProveedores)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbUsuarioBusqueda.setText("BUSCAR PROVEEDOR");

        btnBuscar.setText("BUSCAR");

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

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBotonesLayout = new javax.swing.GroupLayout(panelBotones);
        panelBotones.setLayout(panelBotonesLayout);
        panelBotonesLayout.setHorizontalGroup(
            panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonesLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBotonesLayout.createSequentialGroup()
                        .addComponent(jSeparator2)
                        .addContainerGap())
                    .addGroup(panelBotonesLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(lbBuscar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 103, Short.MAX_VALUE)
                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10))
                    .addGroup(panelBotonesLayout.createSequentialGroup()
                        .addComponent(lbUsuarioBusqueda)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimir, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10))))
        );
        panelBotonesLayout.setVerticalGroup(
            panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbUsuarioBusqueda)
                    .addComponent(btnImprimir, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnBuscar)
                        .addComponent(btnNuevo)
                        .addComponent(btnEditar)
                        .addComponent(btnEliminar))
                    .addComponent(lbBuscar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );

        jpDatosPersonales.setMinimumSize(new java.awt.Dimension(600, 76));

        txtTelefono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtTelefonoKeyTyped(evt);
            }
        });

        lbTelefono.setText("Teléfono: *");

        txtDNI.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtDNIKeyTyped(evt);
            }
        });

        lbDireccion.setText("DNI: *");

        lbApellido.setText("Apellido: *");

        txtNombre.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtNombreKeyTyped(evt);
            }
        });

        lbNombre.setText("Nombre: *");

        lbDatosPersonales.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbDatosPersonales.setText("Datos de Contacto:");

        lbDireccion2.setText("Dirección: *");

        javax.swing.GroupLayout jpDatosPersonalesLayout = new javax.swing.GroupLayout(jpDatosPersonales);
        jpDatosPersonales.setLayout(jpDatosPersonalesLayout);
        jpDatosPersonalesLayout.setHorizontalGroup(
            jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosPersonalesLayout.createSequentialGroup()
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator7))
                    .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbDatosPersonales)
                            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbApellido))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbDireccion)
                                    .addComponent(txtDNI, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbDireccion2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbTelefono)
                                    .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpDatosPersonalesLayout.setVerticalGroup(
            jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                .addComponent(lbDatosPersonales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                            .addComponent(lbDireccion)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtDNI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                            .addComponent(lbDireccion2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpDatosPersonalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                            .addComponent(lbApellido)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                            .addComponent(lbNombre)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jpDatosPersonalesLayout.createSequentialGroup()
                            .addComponent(lbTelefono)
                            .addGap(28, 28, 28))))
                .addGap(28, 28, 28)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbDatosPersonales1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbDatosPersonales1.setText("Datos de la Proveedor:");

        lbFechaNacimiento.setText("Rubro: *");

        lbNombre1.setText("Razon Social:");

        lbEmail.setText("Email: *");

        lbEstado.setText("Estado: *");

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

        lbCUIT.setText("CUIT:");

        lbValidacionCuit.setForeground(java.awt.Color.green);

        javax.swing.GroupLayout jpDatosProveedorLayout = new javax.swing.GroupLayout(jpDatosProveedor);
        jpDatosProveedor.setLayout(jpDatosProveedorLayout);
        jpDatosProveedorLayout.setHorizontalGroup(
            jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbDatosPersonales1)
                    .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                        .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbFechaNacimiento)
                            .addComponent(jcbRubro, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtRazonSocial, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbNombre1, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lbValidacionCuit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtCUIT, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                            .addComponent(lbCUIT, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbEstado))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosProveedorLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSeparator8, javax.swing.GroupLayout.DEFAULT_SIZE, 865, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jpDatosProveedorLayout.setVerticalGroup(
            jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                        .addComponent(lbNombre1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRazonSocial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                        .addComponent(lbDatosPersonales1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jcbRubro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                                .addComponent(lbFechaNacimiento)
                                .addGap(28, 28, 28))))
                    .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                        .addComponent(lbCUIT)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCUIT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                        .addComponent(lbEmail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosProveedorLayout.createSequentialGroup()
                        .addComponent(lbEstado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbValidacionCuit, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancelar)
                    .addComponent(btnLimpiar)
                    .addComponent(btnGuardar))
                .addContainerGap())
            .addGroup(jpDatosProveedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosProveedorLayout.createSequentialGroup()
                    .addGap(0, 134, Short.MAX_VALUE)
                    .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        lbListProv.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListProv.setText("Lista de Proveedores");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableProveedores.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Código", "Razon Social", "CUIT", "Contacto", "Teléfono", "Dirección", "E-mail", "Rubro", "Estado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableProveedores.setMinimumSize(new java.awt.Dimension(848, 220));
        tableProveedores.setPreferredSize(new java.awt.Dimension(848, 220));
        tableProveedores.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableProveedores);

        btnImprimirLista.setText("Imprimir Lista");

        javax.swing.GroupLayout jpListaProveedoresLayout = new javax.swing.GroupLayout(jpListaProveedores);
        jpListaProveedores.setLayout(jpListaProveedoresLayout);
        jpListaProveedoresLayout.setHorizontalGroup(
            jpListaProveedoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaProveedoresLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaProveedoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator9)
                    .addComponent(scroll)
                    .addGroup(jpListaProveedoresLayout.createSequentialGroup()
                        .addComponent(lbListProv)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimirLista))))
        );
        jpListaProveedoresLayout.setVerticalGroup(
            jpListaProveedoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpListaProveedoresLayout.createSequentialGroup()
                .addGroup(jpListaProveedoresLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpListaProveedoresLayout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(lbListProv))
                    .addComponent(btnImprimirLista))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(panelHadear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jpDatosProveedor, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpDatosPersonales, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelBotones, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpListaProveedores, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelHadear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelBotones, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatosPersonales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatosProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaProveedores, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void initListeners() {
        // AUTO-SELECCION AL CLICKEAR FILA + visibilidad de botón imprimir
        tableProveedores.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                seleccionarFilaDesdeClick(evt);
            }
        });

        tableProveedores.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                actualizarAccionesSegunSeleccion();
            }
        });

        txtApellido.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Este método se ejecuta cuando el txtField1 gana el foco (opcional)
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Acción a realizar cuando txtField1 pierde el foco (al tabear o cambiar de campo)
                String nombreApellido = txtNombre.getText() + " " + txtApellido.getText();
                txtRazonSocial.setText(nombreApellido);
            }
        });

        txtRazonSocial.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                String nombreApellido = txtNombre.getText() + " " + txtApellido.getText();
                txtRazonSocial.setText(nombreApellido);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Acción a realizar cuando txtField1 pierde el foco (al tabear o cambiar de campo)
            }
        });
    }

    private void ejecutarBusqueda() {
        txtBusqueda.requestFocusInWindow();
        txtBusqueda.selectAll();
        actualizarAccionesSegunSeleccion();
    }

    private void actualizarAccionesSegunSeleccion() {
        boolean haySeleccion = (operarTabla.comprobarElementoSeleccionado(tableProveedores) != -1);
        btnEditar.setEnabled(haySeleccion);
        btnEliminar.setEnabled(haySeleccion);
        btnImprimir.setVisible(haySeleccion);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void normalizarCamposFormulario() {
        txtNombre.setText(safeTrim(txtNombre.getText()));
        txtApellido.setText(safeTrim(txtApellido.getText()));
        txtDNI.setText(safeTrim(txtDNI.getText()));
        txtDireccion.setText(safeTrim(txtDireccion.getText()));
        txtTelefono.setText(safeTrim(txtTelefono.getText()));
        txtEmail.setText(safeTrim(txtEmail.getText()).toLowerCase());
        txtRazonSocial.setText(safeTrim(txtRazonSocial.getText()));
        txtCUIT.setText(safeTrim(txtCUIT.getText()));
    }

    private String construirResumenProveedor(Proveedor proveedorResumen, String titulo) {
        Persona persona = proveedorResumen != null ? proveedorResumen.getPersona() : null;
        String contacto = (persona != null)
                ? safeTrim(persona.getNombre()) + " " + safeTrim(persona.getApellido())
                : safeTrim(txtNombre.getText()) + " " + safeTrim(txtApellido.getText());
        String dni = (persona != null) ? safeTrim(persona.getDni()) : safeTrim(txtDNI.getText());
        String telefono = (persona != null) ? safeTrim(persona.getTelefono()) : safeTrim(txtTelefono.getText());
        String direccion = (persona != null) ? safeTrim(persona.getDireccion()) : safeTrim(txtDireccion.getText());
        String email = proveedorResumen != null ? safeTrim(proveedorResumen.getEmail()) : safeTrim(txtEmail.getText());
        String razonSocial = proveedorResumen != null ? safeTrim(proveedorResumen.getRazonSocial()) : safeTrim(txtRazonSocial.getText());
        String cuit = proveedorResumen != null ? safeTrim(proveedorResumen.getCuit()) : safeTrim(txtCUIT.getText());
        String rubro = proveedorResumen != null ? safeTrim(proveedorResumen.getRubro()) : String.valueOf(jcbRubro.getSelectedItem());
        String estado = proveedorResumen != null ? safeTrim(proveedorResumen.getEstado()) : String.valueOf(jcbEstado.getSelectedItem());

        return titulo
                + "\n\nRazón social: " + razonSocial
                + "\nCUIT: " + cuit
                + "\nContacto: " + contacto.trim()
                + "\nDNI: " + dni
                + "\nTeléfono: " + telefono
                + "\nDirección: " + direccion
                + "\nEmail: " + email
                + "\nRubro: " + rubro
                + "\nEstado: " + estado;
    }

    private boolean confirmarPersistenciaProveedor() {
        String estadoActual = Application.consultarEstadoUsuario();
        String titulo = "CreandoProveedor".equals(estadoActual)
                ? "¿Deseas guardar este proveedor?"
                : "¿Deseas guardar los cambios del proveedor?";
        int respuesta = JOptionPane.showConfirmDialog(
                this,
                construirResumenProveedor(null, titulo),
                "Confirmar datos del proveedor",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return respuesta == JOptionPane.YES_OPTION;
    }

    private boolean existeProveedorConCuit(String cuitNormalizado, Integer idProveedorActual) {
        for (Proveedor proveedorExistente : operandoProveedor.buscarTodosLosProveedores()) {
            if (proveedorExistente == null) {
                continue;
            }
            String cuitExistente = safeTrim(proveedorExistente.getCuit());
            Integer idExistente = proveedorExistente.getIdProveedor();
            if (cuitNormalizado.equalsIgnoreCase(cuitExistente)
                    && (idProveedorActual == null || !idProveedorActual.equals(idExistente))) {
                return true;
            }
        }
        return false;
    }

    private boolean existeProveedorConDniContacto(String dniNormalizado, Integer idPersonaActual) {
        for (Proveedor proveedorExistente : operandoProveedor.buscarTodosLosProveedores()) {
            if (proveedorExistente == null || proveedorExistente.getPersona() == null) {
                continue;
            }
            Persona personaExistente = proveedorExistente.getPersona();
            String dniExistente = safeTrim(personaExistente.getDni());
            Integer idPersonaExistente = personaExistente.getIdPersona();
            if (dniNormalizado.equalsIgnoreCase(dniExistente)
                    && (idPersonaActual == null || !idPersonaActual.equals(idPersonaExistente))) {
                return true;
            }
        }
        return false;
    }

    // -------------------- Impresión PDF --------------------
    private void actualizarBotonImprimir() {
        boolean haySeleccion = (operarTabla.comprobarElementoSeleccionado(tableProveedores) != -1);
        btnImprimir.setVisible(haySeleccion);
    }

    /**
     * Al hacer click en una fila (fuera del checkbox), marca automáticamente el
     * checkbox de esa fila y desmarca el resto.
     */
    private void seleccionarFilaDesdeClick(java.awt.event.MouseEvent evt) {
        if (evt == null) {
            return;
        }

        int viewRow = tableProveedores.rowAtPoint(evt.getPoint());
        int viewCol = tableProveedores.columnAtPoint(evt.getPoint());
        if (viewRow < 0) {
            return;
        }

        // Selección visual
        try {
            tableProveedores.setRowSelectionInterval(viewRow, viewRow);
        } catch (Exception ignore) {
        }

        // Si clickea el checkbox, permitimos toggle natural
        if (viewCol == 0) {
            SwingUtilities.invokeLater(this::actualizarBotonImprimir);
            return;
        }

        try {
            for (int i = 0; i < tableProveedores.getRowCount(); i++) {
                if (i != viewRow && Boolean.TRUE.equals(tableProveedores.getValueAt(i, 0))) {
                    tableProveedores.setValueAt(false, i, 0);
                }
            }
            tableProveedores.setValueAt(true, viewRow, 0);
        } catch (Exception ignore) {
        }

        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }
    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        Application.actualizarEstadoUsuario("CreandoProveedor");
        asegurarValidacionCUIT();
        panelBotones.setVisible(false);
        jpListaProveedores.setVisible(false);
        jpDatosPersonales.setVisible(true);
        jpDatosProveedor.setVisible(true);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        String perm = "FormProveedores.EDITAR";
        if (sesion == null || !sesion.puede(perm)) {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para editar proveedores.",
                    "Acceso restringido",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!editarProveedorSeleccionado()) {
            return;
        }

        Application.actualizarEstadoUsuario("EditandoProveedor");
        asegurarValidacionCUIT();

        panelBotones.setVisible(false);
        jpListaProveedores.setVisible(false);
        jpDatosPersonales.setVisible(true);
        jpDatosProveedor.setVisible(true);

        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnImprimir.setVisible(false);
}//GEN-LAST:event_btnEditarActionPerformed


    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        Integer selectedRow = operarTabla.comprobarElementoSeleccionado(tableProveedores);

        String perm = "FormProveedores.ELIMINAR";
        if (sesion == null || !sesion.puede(perm)) {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para eliminar proveedores.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedRow != -1) {
            Application.actualizarEstadoUsuario("EliminandoProveedor");

            Proveedor proveedorSeleccionado = obtenerProveedorSeleccionadoDesdeTabla();
            if (proveedorSeleccionado == null) {
                return;
            }

            int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    construirResumenProveedor(proveedorSeleccionado, "¿Confirmás la eliminación del siguiente proveedor?"),
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                if (eliminarProveedorSeleccionado()) {
                    limpiarCamposFormulario();
                    jpDatosProveedor.setVisible(false);
                    cargarProveedoresEnTabla();
                }
            } else {
                Application.actualizarEstadoUsuario("Eliminación cancelada");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Por favor seleccione un proveedor a ELIMINAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void txtNombreKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtNombreKeyTyped
        validar.validarSoloLetras(evt);
    }//GEN-LAST:event_txtNombreKeyTyped

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        normalizarCamposFormulario();
        if (validarDatosProveedor() && confirmarPersistenciaProveedor()) {
            if (persistirProveedor()) {
                limpiarCamposFormulario();
                jpDatosPersonales.setVisible(false);
                jpDatosProveedor.setVisible(false);
                panelBotones.setVisible(true);
                jpListaProveedores.setVisible(true);
                btnNuevo.setEnabled(true);
                btnEditar.setEnabled(false);
                btnEliminar.setEnabled(false);
                cargarProveedoresEnTabla();
            }
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarCamposFormulario();
        panelBotones.setVisible(true);
        jpListaProveedores.setVisible(true);
        jpDatosPersonales.setVisible(false);
        jpDatosProveedor.setVisible(false);
        btnNuevo.setEnabled(true);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnImprimir.setVisible(false);
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void txtTelefonoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTelefonoKeyTyped
        validar.validarSoloNumeros(evt);
    }//GEN-LAST:event_txtTelefonoKeyTyped

    private void txtDNIKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDNIKeyTyped
        validar.validarSoloNumeros(evt);
    }//GEN-LAST:event_txtDNIKeyTyped

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        imprimirRegistroSeleccionado();
    }//GEN-LAST:event_btnImprimirActionPerformed

    private Proveedor obtenerProveedorSeleccionadoDesdeTabla() {
        int row = operarTabla.comprobarElementoSeleccionado(tableProveedores);
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar un proveedor de la lista.",
                    "Atención",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Object idObj = tableProveedores.getValueAt(row, 1);
        Integer id = null;
        try {
            id = Integer.valueOf(String.valueOf(idObj));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo obtener el ID del proveedor seleccionado.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Proveedor p = operandoProveedor.buscarProveedorPorId(id);
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el proveedor seleccionado en la base de datos.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return p;
    }

    private void imprimirRegistroSeleccionado() {
        Proveedor p = obtenerProveedorSeleccionadoDesdeTabla();
        if (p == null) {
            return;
        }

        try {
            int rowSel = operarTabla.comprobarElementoSeleccionado(tableProveedores);
            String fId = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 1)) : "";
            String fRazonSocial = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 2)) : "";
            String fCuit = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 3)) : "";
            String fContacto = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 4)) : "";
            String fTelefono = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 5)) : "";
            String fDireccion = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 6)) : "";
            String fEmail = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 7)) : "";
            String fRubro = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 8)) : "";
            String fEstado = (rowSel != -1) ? String.valueOf(tableProveedores.getValueAt(rowSel, 9)) : "";

            // Cargar Persona vinculada (evita proxies LAZY en reportes)
            Persona personaDb = null;
            try {
                if (p.getPersona() != null && p.getPersona().getIdPersona() != null) {
                    personaDb = personaDAO.obtenerPorId(p.getPersona().getIdPersona());
                }
            } catch (Exception ignore) {
                personaDb = null;
            }

            // 1. Cargamos el ReporteRequest con toda la información mapeada
            ReporteRequest req = new ReporteRequest()
                    .put("proveedor", p)
                    .put("persona", personaDb)
                    .put("idProveedor", fId)
                    .put("razonSocial", fRazonSocial)
                    .put("cuit", fCuit)
                    .put("contacto", fContacto)
                    .put("telefono", fTelefono)
                    .put("direccion", fDireccion)
                    .put("email", fEmail)
                    .put("rubro", fRubro)
                    .put("estado", fEstado)
                    .put("emisor", Application.getNombreApellidoUsuarioLogeado());

            // 2. 🚀 AL EJECUTOR: Corre en segundo plano y nos evita errores de constructor
            veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.PROVEEDOR_REGISTRO, req);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo preparar la información para el reporte.\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirListaProveedores() {
        // 1. Armamos los parámetros para el listado de proveedores
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tableProveedores)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 AL EJECUTOR: Limpio, asíncrono y alineado al Singleton
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.PROVEEDOR_LISTADO, req);
    }

    private boolean editarProveedorSeleccionado() {
        boolean bRetorno = false;
        int selectedRow = operarTabla.comprobarElementoSeleccionado(tableProveedores);

        if (selectedRow != -1) {
            var proveedorSelec = (Integer) tableProveedores.getValueAt(selectedRow, 1);
            proveedor = operandoProveedor.buscarProveedorPorId(proveedorSelec);
            if (proveedor == null || proveedor.getPersona() == null || proveedor.getPersona().getIdPersona() == null) {
                JOptionPane.showMessageDialog(this, "No se pudo cargar el proveedor seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            personaProveedor = personaDAO.obtenerPorId(proveedor.getPersona().getIdPersona());
            if (personaProveedor == null) {
                JOptionPane.showMessageDialog(this, "No se pudo cargar el contacto del proveedor seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            txtNombre.setText(personaProveedor.getNombre());
            txtApellido.setText(personaProveedor.getApellido());
            jcbRubro.setSelectedItem(proveedor.getRubro());
            txtDNI.setText(personaProveedor.getDni());
            txtDireccion.setText(personaProveedor.getDireccion());
            txtTelefono.setText(personaProveedor.getTelefono());
            txtEmail.setText(proveedor.getEmail());
            txtRazonSocial.setText(proveedor.getRazonSocial());
            txtCUIT.setText(proveedor.getCuit());
            jcbEstado.setSelectedItem(proveedor.getEstado());
            bRetorno = true;
        } else {
            JOptionPane.showMessageDialog(null, "¡Debe seleccionar un proveedor a EDITAR!", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;
    }

    private boolean eliminarProveedorSeleccionado() {
        int selectedRow = operarTabla.comprobarElementoSeleccionado(tableProveedores);
        if (selectedRow != -1) {
            var idProveedor = (Integer) tableProveedores.getValueAt(selectedRow, 1);
            proveedor = operandoProveedor.buscarProveedorPorId(idProveedor);
            if (proveedor == null) {
                JOptionPane.showMessageDialog(this, "No se pudo recuperar el proveedor a eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (operandoProveedor.eliminarProveedor(proveedor)) {
                JOptionPane.showMessageDialog(null, "Proveedor eliminado con éxito", "Información", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el proveedor seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            JOptionPane.showMessageDialog(null, "Por favor seleccione un proveedor a ELIMINAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private void limpiarCamposFormulario() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtDNI.setText("");
        txtDireccion.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtRazonSocial.setText("");
        txtCUIT.setText("");
        lbValidacionCuit.setText(" ");
        jcbRubro.removeAllItems();
        jcbEstado.removeAllItems();
        txtBusqueda.setText("");
        cargarCombosBoxs();
        proveedor = new Proveedor();
        personaProveedor = new Persona();
        actualizarAccionesSegunSeleccion();
    }

    private void cargarCombosBoxs() {
        jcbRubro.addItem("Seleccionar Rubro");
        jcbRubro.addItem("Medicamentos");
        jcbRubro.addItem("Alimentos");
        jcbRubro.addItem("Productos de Higiene y Limpieza");
        jcbRubro.addItem("Juguetes y Accesorios");
        jcbRubro.addItem("Equipos Médicos");
        jcbRubro.addItem("Productos de Diagnóstico");
        jcbRubro.addItem("Vacunas");
        jcbRubro.addItem("Materiales de Quirófano");
        jcbRubro.addItem("Productos de Peluquería");
        jcbRubro.addItem("Suplementos y Vitaminas");
        jcbRubro.addItem("Instrumental Veterinario");
        jcbRubro.addItem("Materiales de Laboratorio");
        jcbRubro.addItem("Servicios de Marketing y Publicidad");

        jcbEstado.addItem("Seleccionar Estado");
        jcbEstado.addItem("Activo");
        jcbEstado.addItem("Inactivo");
    }

    private boolean validarDatosProveedor() {
        if (txtNombre.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre del proveedor no puede estar vacío.");
            txtNombre.requestFocusInWindow();
            return false;
        }
        if (txtApellido.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El apellido del proveedor no puede estar vacío.");
            txtApellido.requestFocusInWindow();
            return false;
        }
        if (jcbRubro.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el rubro del proveedor.");
            jcbRubro.requestFocusInWindow();
            return false;
        }
        if (txtDNI.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El DNI del contacto no puede estar vacío.");
            txtDNI.requestFocusInWindow();
            return false;
        }
        if (txtDNI.getText().trim().length() < 7) {
            JOptionPane.showMessageDialog(null, "El DNI del contacto es demasiado corto.");
            txtDNI.requestFocusInWindow();
            return false;
        }
        if (txtDireccion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "La dirección del proveedor no puede estar vacía.");
            txtDireccion.requestFocusInWindow();
            return false;
        }
        if (txtTelefono.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El teléfono del proveedor no puede estar vacío.");
            txtTelefono.requestFocusInWindow();
            return false;
        }
        if (txtTelefono.getText().trim().length() < 6) {
            JOptionPane.showMessageDialog(null, "El teléfono ingresado es demasiado corto.");
            txtTelefono.requestFocusInWindow();
            return false;
        }
        if (!validar.validarCorreoValido(txtEmail.getText().trim())) {
            JOptionPane.showMessageDialog(null, "Debes ingresar un correo válido.");
            txtEmail.requestFocusInWindow();
            return false;
        }
        if (txtRazonSocial.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "La Razón Social no puede estar vacía.");
            txtRazonSocial.requestFocusInWindow();
            return false;
        }
        if (txtCUIT.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El CUIT no puede estar vacío.");
            txtCUIT.requestFocusInWindow();
            return false;
        }
        if (!validar.validarCuitCuil(txtCUIT.getText().trim())) {
            JOptionPane.showMessageDialog(null, "El CUIT ingresado no tiene un formato válido.");
            txtCUIT.requestFocusInWindow();
            return false;
        }
        if (jcbEstado.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el estado del proveedor.");
            jcbEstado.requestFocusInWindow();
            return false;
        }

        Integer idProveedorActual = proveedor != null ? proveedor.getIdProveedor() : null;
        Integer idPersonaActual = personaProveedor != null ? personaProveedor.getIdPersona() : null;

        if (existeProveedorConCuit(txtCUIT.getText(), idProveedorActual)) {
            JOptionPane.showMessageDialog(this, "Ya existe otro proveedor registrado con ese CUIT.", "CUIT duplicado", JOptionPane.WARNING_MESSAGE);
            txtCUIT.requestFocusInWindow();
            return false;
        }
        if (existeProveedorConDniContacto(txtDNI.getText(), idPersonaActual)) {
            JOptionPane.showMessageDialog(this, "Ya existe otro proveedor cuyo contacto tiene ese DNI.", "DNI duplicado", JOptionPane.WARNING_MESSAGE);
            txtDNI.requestFocusInWindow();
            return false;
        }
        return true;
    }

    private boolean persistirProveedor() {
        if ("CreandoProveedor".equals(Application.consultarEstadoUsuario())) {
            proveedor = new Proveedor();
            personaProveedor = new Persona();
        }

        personaProveedor.setNombre(txtNombre.getText());
        personaProveedor.setApellido(txtApellido.getText());
        personaProveedor.setDni(txtDNI.getText());
        personaProveedor.setDireccion(txtDireccion.getText());
        personaProveedor.setTelefono(txtTelefono.getText());
        proveedor.setRubro(String.valueOf(jcbRubro.getSelectedItem()));
        proveedor.setRazonSocial(txtRazonSocial.getText());
        proveedor.setCuit(txtCUIT.getText());
        proveedor.setEmail(txtEmail.getText());
        proveedor.setEstado(String.valueOf(jcbEstado.getSelectedItem()));

        try {
            if ("CreandoProveedor".equals(Application.consultarEstadoUsuario())) {
                boolean creado = operandoProveedor.crearProveedor(personaProveedor, proveedor);
                if (creado) {
                    JOptionPane.showMessageDialog(this, "Proveedor guardado exitosamente.");
                    return true;
                }
                JOptionPane.showMessageDialog(this, "No se pudo guardar el proveedor. Revisá los datos e intentá nuevamente.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if ("EditandoProveedor".equals(Application.consultarEstadoUsuario())) {
                boolean actualizado = operandoProveedor.actualizarProveedor(personaProveedor, proveedor);
                if (actualizado) {
                    JOptionPane.showMessageDialog(this, "Proveedor modificado exitosamente.");
                    return true;
                }
                JOptionPane.showMessageDialog(this, "No se pudo actualizar el proveedor. Revisá los datos e intentá nuevamente.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            JOptionPane.showMessageDialog(this, "El formulario no está en un estado válido para guardar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return false;
        } catch (Exception ex) {
            Logger.getLogger(FormProveedores.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Ocurrió un error al guardar el proveedor.\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void cargarProveedoresEnTabla() {
        List<Proveedor> proveedores = operandoProveedor.buscarTodosLosProveedores();
        DefaultTableModel modelo = (DefaultTableModel) tableProveedores.getModel();
        modelo.setRowCount(0);

        for (Proveedor proveedorBucle : proveedores) {
            Persona personaFila = proveedorBucle != null ? proveedorBucle.getPersona() : null;
            String nombre = personaFila != null ? safeTrim(personaFila.getNombre()) : "";
            String apellido = personaFila != null ? safeTrim(personaFila.getApellido()) : "";
            String telefono = personaFila != null ? safeTrim(personaFila.getTelefono()) : "";
            String direccion = personaFila != null ? safeTrim(personaFila.getDireccion()) : "";
            String contacto = (nombre + " " + apellido).trim();

            Object[] fila = new Object[]{
                false,
                proveedorBucle != null ? proveedorBucle.getIdProveedor() : null,
                proveedorBucle != null ? proveedorBucle.getRazonSocial() : "",
                proveedorBucle != null ? proveedorBucle.getCuit() : "",
                contacto,
                telefono,
                direccion,
                proveedorBucle != null ? proveedorBucle.getEmail() : "",
                proveedorBucle != null ? proveedorBucle.getRubro() : "",
                proveedorBucle != null ? proveedorBucle.getEstado() : ""
            };
            modelo.addRow(fila);
        }
        operarTabla.asegurarSeleccionUnica(tableProveedores);
        actualizarAccionesSegunSeleccion();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JComboBox<String> jcbEstado;
    private javax.swing.JComboBox<String> jcbRubro;
    private javax.swing.JPanel jpDatosPersonales;
    private javax.swing.JPanel jpDatosProveedor;
    private javax.swing.JPanel jpListaProveedores;
    private javax.swing.JLabel lbApellido;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbCUIT;
    private javax.swing.JLabel lbDatosPersonales;
    private javax.swing.JLabel lbDatosPersonales1;
    private javax.swing.JLabel lbDireccion;
    private javax.swing.JLabel lbDireccion2;
    private javax.swing.JLabel lbEmail;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbFechaNacimiento;
    private javax.swing.JLabel lbListProv;
    private javax.swing.JLabel lbNombre;
    private javax.swing.JLabel lbNombre1;
    private javax.swing.JLabel lbProveedores;
    private javax.swing.JLabel lbTelefono;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JLabel lbValidacionCuit;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelHadear;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tableProveedores;
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
