package veterinaria.vista;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.MascotaControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Rol;
import veterinaria.entidad.util.ClienteItem;
import veterinaria.util.ManejoTablas;
import veterinaria.util.SesionUsuario;
import veterinaria.util.Validaciones;
import veterinaria.vista.application.Application;
import veterinaria.util.PermisoUI;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;

public class FormMascota extends javax.swing.JPanel {

    private final ClienteControlador operandoCliente = new ClienteControlador();
    private final MascotaControlador operandoMascota = new MascotaControlador();
    private final Validaciones validar = new Validaciones();
    private ManejoTablas operarTablas = new ManejoTablas();
    private Mascota mascota = new Mascota();
    private SesionUsuario sesion = Application.getSesionUsuario();
    private Rol rol = (sesion != null) ? sesion.getRol() : null;

    // Valores originales (para confirmar edición y mostrar cambios)
    private String originalNombre;
    private String originalSexo;
    private String originalRaza;
    private String originalEspecie;
    private String originalCastrado;
    private LocalDate originalFechaNacimiento;
    private String originalTamaño;
    private String originalPeso;
    private Integer originalIdCliente;

    // Flags / cache para no duplicar listeners ni recargar datos innecesariamente
    private boolean combosInicializados = false;
    private boolean listenersInicializados = false;
    private final Map<Integer, Cliente> cacheClientesPorId = new HashMap<>();

    public FormMascota() {
        initComponents();
        PermisoUI.aplicar(this);
        cargarDatosComboBoxes();
        List<Integer> columnasObjetivo = Arrays.asList(2, 3, 4, 5, 10);
        operarTablas.aplicarFiltroYResaltado(tableMascotas, txtBusqueda, columnasObjetivo);
        cargarDatosTablaMascota();
        panelDatos.setVisible(false);

        // -------------------- Impresión PDF (patrón similar a FormHospitalizaciones / FormUsuario) --------------------
        btnImprimir.setVisible(false); // solo visible si hay un registro seleccionado
        btnImprimir.setEnabled(true);
        btnImprimir.addActionListener(e -> imprimirRegistroSeleccionado());

        btnImprimirLista.setVisible(true);
        btnImprimirLista.setEnabled(true);
        btnImprimirLista.addActionListener(e -> imprimirListaMascotas());

        // Visibilidad del botón Imprimir según selección (checkbox col 0)
        tableMascotas.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() == 0 || e.getColumn() == TableModelEvent.ALL_COLUMNS)) {
                SwingUtilities.invokeLater(this::actualizarBotonImprimir);
            }
        });

        // Click en tabla: NO marcar automáticamente el checkbox.
        // Solo se permitirá editar/eliminar/imprimir cuando el checkbox esté seleccionado.
        // Si se hace click en el checkbox, se asegura selección única.
        tableMascotas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                manejarClickTablaMascotas(evt);
            }
        });

        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }

    /**
     * Maneja clicks en la tabla. - Click en columna 0 (checkbox): permite
     * toggle y asegura selección única. - Click en cualquier otra columna: solo
     * selecciona visualmente la fila, NO marca checkbox.
     */
    private void manejarClickTablaMascotas(java.awt.event.MouseEvent evt) {
        if (evt == null) {
            return;
        }

        int viewRow = tableMascotas.rowAtPoint(evt.getPoint());
        int viewCol = tableMascotas.columnAtPoint(evt.getPoint());

        if (viewRow < 0) {
            return;
        }

        // Selección visual
        try {
            tableMascotas.setRowSelectionInterval(viewRow, viewRow);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
        }

        // Si clickea el checkbox, aseguramos selección única (desmarcar resto)
        if (viewCol == 0) {
            SwingUtilities.invokeLater(() -> {
                try {
                    for (int i = 0; i < tableMascotas.getRowCount(); i++) {
                        if (i != viewRow && Boolean.TRUE.equals(tableMascotas.getValueAt(i, 0))) {
                            tableMascotas.setValueAt(false, i, 0);
                        }
                    }
                } catch (Exception ex) {
                    veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
                }
                actualizarBotonImprimir();
            });
            return;
        }

        // Click fuera del checkbox: NO tocar checkbox.
        SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }

    private void cargarDatosComboBoxes() {
        // 1) Inicializar combos fijos una sola vez (NetBeans-safe)
        if (!combosInicializados) {
            jcbEspecie.removeAllItems();
            jcbSexo.removeAllItems();
            jcbTamaño.removeAllItems();

            jcbEspecie.addItem("Seleccionar");
            jcbEspecie.addItem("Perro");
            jcbEspecie.addItem("Gato");
            jcbEspecie.addItem("Ave");

            jcbSexo.addItem("Seleccionar");
            jcbSexo.addItem("Macho");
            jcbSexo.addItem("Hembra");

            jcbTamaño.addItem("Seleccionar");
            jcbTamaño.addItem("Pequeño");
            jcbTamaño.addItem("Mediano");
            jcbTamaño.addItem("Grande");

            combosInicializados = true;
        }

        // 2) Cargar clientes al combo (con cache) sin repetir llamadas innecesarias
        cargarClientesEnCombo();

        // 3) Listeners: agregar una sola vez para evitar duplicación
        if (!listenersInicializados) {
            jcbDueño.addActionListener(e -> onDueñoSeleccionado(mascota));
            operarTablas.asegurarSeleccionUnica(tableMascotas);
            listenersInicializados = true;
        }
    }

    /**
     * Carga clientes en el combo Dueño. - Mantiene cache por id para evitar
     * consultas repetidas (onDueñoSeleccionado/persistir). - No agrega
     * listeners (se hace una sola vez).
     */
    private void cargarClientesEnCombo() {
        try {
            // Si ya hay items, no recargamos (optimización)
            if (jcbDueño.getItemCount() > 0) {
                Object first = jcbDueño.getItemAt(0);
                if (first instanceof ClienteItem && ((ClienteItem) first).getIdCliente() == 0) {
                    return;
                }
            }

            cacheClientesPorId.clear();
            List<Cliente> listaClientes = operandoCliente.buscarClientesActivos();

            jcbDueño.removeAllItems();
            jcbDueño.addItem(new ClienteItem(0, "Seleccionar"));

            for (Cliente cliente : listaClientes) {
                if (cliente == null) {
                    continue;
                }
                Integer id = cliente.getIdCliente();
                if (id == null) {
                    continue;
                }
                cacheClientesPorId.put(id, cliente);

                String apellido = (cliente.getPersona() != null && cliente.getPersona().getApellido() != null)
                        ? cliente.getPersona().getApellido() : "";
                String nombre = (cliente.getPersona() != null && cliente.getPersona().getNombre() != null)
                        ? cliente.getPersona().getNombre() : "";
                String nombreApellido = (apellido + " " + nombre).trim();
                if (nombreApellido.isEmpty()) {
                    nombreApellido = "Cliente " + id;
                }
                jcbDueño.addItem(new ClienteItem(id, nombreApellido));
            }
        } catch (Exception ex) {
            Logger.getLogger(FormMascota.class.getName()).log(Level.SEVERE, null, ex);
            jcbDueño.removeAllItems();
            jcbDueño.addItem(new ClienteItem(0, "Seleccionar"));
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar la lista de dueños (clientes).\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnGroup = new javax.swing.ButtonGroup();
        panelHadear = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        panelBotones = new javax.swing.JPanel();
        lbUsuarioBusqueda = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        lbBuscar = new javax.swing.JLabel();
        btnImprimir = new javax.swing.JButton();
        panelDatos = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lbNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lbSexo = new javax.swing.JLabel();
        lbRaza = new javax.swing.JLabel();
        txtRaza = new javax.swing.JTextField();
        lbFechaNacimiento = new javax.swing.JLabel();
        lbEspecie = new javax.swing.JLabel();
        jcbEspecie = new javax.swing.JComboBox<>();
        lbDueño = new javax.swing.JLabel();
        jcbDueño = new javax.swing.JComboBox<>();
        jcbSexo = new javax.swing.JComboBox<>();
        rbtnSi = new javax.swing.JRadioButton();
        rbtnNo = new javax.swing.JRadioButton();
        txtCastrado = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        jDateFechaNacimiento = new com.toedter.calendar.JDateChooser();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        lbTamaño = new javax.swing.JLabel();
        txtPeso = new javax.swing.JTextField();
        lbPeso = new javax.swing.JLabel();
        jcbTamaño = new javax.swing.JComboBox<>();
        jpListaPacientes = new javax.swing.JPanel();
        lbListaDePacientes = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scroll = new javax.swing.JScrollPane();
        tableMascotas = new veterinaria.vista.table.AutoTable();
        btnImprimirLista = new javax.swing.JButton();

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Pacientes");

        javax.swing.GroupLayout panelHadearLayout = new javax.swing.GroupLayout(panelHadear);
        panelHadear.setLayout(panelHadearLayout);
        panelHadearLayout.setHorizontalGroup(
            panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(panelHadearLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelHadearLayout.setVerticalGroup(
            panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbUsuarioBusqueda.setText("BUSCAR PACIENTE");

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 292, Short.MAX_VALUE)
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
                        .addComponent(btnNuevo)
                        .addComponent(btnEditar)
                        .addComponent(btnEliminar))
                    .addComponent(lbBuscar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Datos del Paciente");

        lbNombre.setText("Nombre: *");

        txtNombre.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtNombreKeyTyped(evt);
            }
        });

        lbSexo.setText("Sexo:*");

        lbRaza.setText("Raza:*");

        lbFechaNacimiento.setText("Fecha de Nacimiento:*");

        lbEspecie.setText("Especie: *");

        lbDueño.setText("Dueño:*");

        btnGroup.add(rbtnSi);
        rbtnSi.setText("Si");

        btnGroup.add(rbtnNo);
        rbtnNo.setText("No");

        txtCastrado.setText("¿Está Castrado?");

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

        lbTamaño.setText("Tamaño: *");

        txtPeso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPesoActionPerformed(evt);
            }
        });

        lbPeso.setText("Peso(KG): *");

        javax.swing.GroupLayout panelDatosLayout = new javax.swing.GroupLayout(panelDatos);
        panelDatos.setLayout(panelDatosLayout);
        panelDatosLayout.setHorizontalGroup(
            panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDatosLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(panelDatosLayout.createSequentialGroup()
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lbNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jcbSexo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelDatosLayout.createSequentialGroup()
                                    .addComponent(lbSexo)
                                    .addGap(0, 0, Short.MAX_VALUE)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lbRaza)
                                .addComponent(txtRaza, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lbFechaNacimiento)
                                .addComponent(jDateFechaNacimiento, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lbEspecie)
                                .addComponent(jcbEspecie, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(26, 26, 26))
                        .addGroup(panelDatosLayout.createSequentialGroup()
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panelDatosLayout.createSequentialGroup()
                                    .addComponent(lbDueño)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelDatosLayout.createSequentialGroup()
                                    .addComponent(jcbDueño, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(35, 35, 35)))
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtCastrado)
                                .addGroup(panelDatosLayout.createSequentialGroup()
                                    .addComponent(rbtnSi)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(rbtnNo)))
                            .addGap(29, 29, 29)
                            .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panelDatosLayout.createSequentialGroup()
                                    .addComponent(lbTamaño, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(79, 79, 79)
                                    .addComponent(lbPeso, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelDatosLayout.createSequentialGroup()
                                    .addComponent(jcbTamaño, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtPeso, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGap(172, 172, 172))))
                .addContainerGap(52, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelDatosLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelDatosLayout.setVerticalGroup(
            panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDatosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelDatosLayout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(panelDatosLayout.createSequentialGroup()
                                .addComponent(lbEspecie)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbEspecie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(panelDatosLayout.createSequentialGroup()
                                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lbRaza)
                                        .addComponent(lbFechaNacimiento))
                                    .addComponent(lbSexo))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jcbSexo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtRaza, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jDateFechaNacimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(panelDatosLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbNombre)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelDatosLayout.createSequentialGroup()
                        .addComponent(lbDueño)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbDueño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelDatosLayout.createSequentialGroup()
                        .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelDatosLayout.createSequentialGroup()
                                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbTamaño)
                                    .addComponent(lbPeso))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jcbTamaño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtPeso, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelDatosLayout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(rbtnSi)
                                    .addComponent(rbtnNo)))
                            .addComponent(txtCastrado))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnGuardar)
                            .addComponent(btnCancelar)
                            .addComponent(btnLimpiar))))
                .addContainerGap(9, Short.MAX_VALUE))
        );

        jcbSexo.getAccessibleContext().setAccessibleName("");

        lbListaDePacientes.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDePacientes.setText("Lista de Pacientes");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableMascotas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "ID", "Nombre", "Sexo", "Raza", "Edad", "Especie", "Dueño", "Castrado", "Fecha de Nacimiento", "Tamaño", "Peso(KG)"
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
        tableMascotas.setMinimumSize(new java.awt.Dimension(848, 220));
        tableMascotas.setPreferredSize(new java.awt.Dimension(848, 220));
        tableMascotas.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableMascotas);

        btnImprimirLista.setText("Imprimir Lista");

        javax.swing.GroupLayout jpListaPacientesLayout = new javax.swing.GroupLayout(jpListaPacientes);
        jpListaPacientes.setLayout(jpListaPacientesLayout);
        jpListaPacientesLayout.setHorizontalGroup(
            jpListaPacientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaPacientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaPacientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scroll)
                    .addGroup(jpListaPacientesLayout.createSequentialGroup()
                        .addGroup(jpListaPacientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpListaPacientesLayout.createSequentialGroup()
                                .addComponent(lbListaDePacientes)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 645, Short.MAX_VALUE)
                                .addComponent(btnImprimirLista)))
                        .addContainerGap())))
        );
        jpListaPacientesLayout.setVerticalGroup(
            jpListaPacientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaPacientesLayout.createSequentialGroup()
                .addGroup(jpListaPacientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListaDePacientes)
                    .addComponent(btnImprimirLista))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(panelHadear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panelBotones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(panelDatos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 36, Short.MAX_VALUE))))
                    .addComponent(jpListaPacientes, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addComponent(panelDatos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaPacientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        Application.actualizarEstadoUsuario("CreandoMascota");
        mascota = new Mascota();
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        jpListaPacientes.setVisible(false);
        panelDatos.setVisible(true);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        String perm = "FormMascota.EDITAR";
        if (sesion != null && sesion.puede(perm)) {
            Application.actualizarEstadoUsuario("EditandoMascota");
            if (editarMascotaSeleccionada()) {
                btnNuevo.setEnabled(false);
                btnEditar.setEnabled(false);
                btnEliminar.setEnabled(false);
                panelDatos.setVisible(true);
                jpListaPacientes.setVisible(false);
            } else {
                // Si no hay selección válida, permanecemos en modo lista.
                Application.actualizarEstadoUsuario("InicioFormularioMascota");
            }
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para editar mascotas.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        Integer selectedRow = operarTablas.comprobarElementoSeleccionado(tableMascotas);

        String perm = "FormMascota.ELIMINAR";
        if (sesion == null || !sesion.puede(perm)) {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para eliminar mascotas.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedRow != -1) {
            Application.actualizarEstadoUsuario("EliminandoMascota");

            if (confirmarEliminacionMascota(selectedRow)) {
                eliminarMascotaSeleccionada(selectedRow);
                limpiarForm();
                panelDatos.setVisible(false);
                jpListaPacientes.setVisible(true);
                cargarDatosTablaMascota();
            } else {
                Application.actualizarEstadoUsuario("Eliminación cancelada");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Por favor seleccione una mascota a ELIMINAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        imprimirRegistroSeleccionado();
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void txtNombreKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtNombreKeyTyped
        validar.validarSoloLetras(evt);
    }//GEN-LAST:event_txtNombreKeyTyped

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (validarDatosMascota()) {
            // Confirmación SI/NO con resumen (patrón FormUsuario/FormCliente)
            if ("CreandoMascota".equals(Application.consultarEstadoUsuario())) {
                if (!confirmarCreacionMascota()) {
                    return;
                }
            }
            if ("EditandoMascota".equals(Application.consultarEstadoUsuario())) {
                if (!confirmarEdicionMascota()) {
                    return;
                }
            }

            persistirMascota();
            limpiarForm();
            panelDatos.setVisible(false);
            jpListaPacientes.setVisible(true);
            btnNuevo.setEnabled(true);
            btnEditar.setEnabled(true);
            btnEliminar.setEnabled(true);
            cargarDatosTablaMascota();
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarForm();
        panelDatos.setVisible(false);
        jpListaPacientes.setVisible(true);
        btnNuevo.setEnabled(true);
        btnEditar.setEnabled(true);
        btnEliminar.setEnabled(true);
        Application.actualizarEstadoUsuario("InicioFormularioMascota");
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarForm();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void txtPesoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPesoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPesoActionPerformed

    private void cargarDatosTablaMascota() {
        List<Mascota> mascotas = operandoMascota.buscarTodasLasMascotas();
        DefaultTableModel modelo = (DefaultTableModel) tableMascotas.getModel();
        modelo.setRowCount(0);

        for (Mascota mascota : mascotas) {
            Cliente cliente = (mascota != null) ? mascota.getCliente() : null;
            String edad = calcularEdad(mascota.getFechaNacimiento());
            String nombreCliente = "(Sin dueño)";
            try {
                if (cliente != null && cliente.getPersona() != null) {
                    String nombre = (cliente.getPersona().getNombre() != null) ? cliente.getPersona().getNombre() : "";
                    String apellido = (cliente.getPersona().getApellido() != null) ? cliente.getPersona().getApellido() : "";
                    String tmp = (nombre + " " + apellido).trim();
                    if (!tmp.isEmpty()) {
                        nombreCliente = tmp;
                    } else if (cliente.getIdCliente() != null) {
                        nombreCliente = "Cliente " + cliente.getIdCliente();
                    }
                } else if (cliente != null && cliente.getIdCliente() != null) {
                    nombreCliente = "Cliente " + cliente.getIdCliente();
                }
            } catch (Exception ex) {
                veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
            }
            Object[] fila = new Object[]{
                false,
                mascota.getIdMascota(),
                mascota.getNombre(),
                mascota.getSexo(),
                mascota.getRaza(),
                edad,
                mascota.getEspecie(),
                nombreCliente,
                mascota.getCastrado(),
                mascota.getFechaNacimiento(),
                mascota.getTamano(),
                mascota.getPeso()
            };
            modelo.addRow(fila);
        }
        tableMascotas.getColumnModel().getColumn(1).setMinWidth(0);
        tableMascotas.getColumnModel().getColumn(1).setMaxWidth(0);
        tableMascotas.getColumnModel().getColumn(1).setPreferredWidth(0);

    }

    // -------------------- Impresión PDF --------------------
    private void actualizarBotonImprimir() {
        boolean haySeleccion = (operarTablas.comprobarElementoSeleccionado(tableMascotas) != -1);
        btnImprimir.setVisible(haySeleccion);
    }

    // seleccionarFilaDesdeClick fue reemplazado por manejarClickTablaMascotas.
    private Mascota obtenerMascotaSeleccionadaDesdeTabla() {
        int row = operarTablas.comprobarElementoSeleccionado(tableMascotas);
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una mascota en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            Integer idMascota = (Integer) tableMascotas.getValueAt(row, 1);
            Mascota m = operandoMascota.buscarMascotaPorId(idMascota);
            if (m == null) {
                JOptionPane.showMessageDialog(this, "No se encontró la mascota seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return m;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer la mascota seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void imprimirRegistroSeleccionado() {
        Mascota m = obtenerMascotaSeleccionadaDesdeTabla();
        if (m == null) {
            return;
        }

        // 1. Armamos los parámetros para el reporte de la mascota
        ReporteRequest req = new ReporteRequest()
                .put("mascota", m)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 MANDAMOS AL EJECUTOR: Corre con el Singleton en segundo plano y muestra la barra animada
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.MASCOTA_REGISTRO, req);
    }

    private void imprimirListaMascotas() {
        // 1. Armamos los parámetros para el listado de mascotas
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tableMascotas)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 AL EJECUTOR: Limpio, seguro y en segundo plano
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.MASCOTA_LISTADO, req);
    }

    private void persistirMascota() {
        mascota.setNombre(txtNombre.getText());
        mascota.setSexo(jcbSexo.getSelectedItem().toString());
        mascota.setRaza(txtRaza.getText());
        mascota.setEspecie(jcbEspecie.getSelectedItem().toString());
        String castrado = rbtnSi.isSelected() ? "Sí" : "No";
        mascota.setCastrado(castrado);
        mascota.setFechaNacimiento(jDateFechaNacimiento.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        mascota.setTamano(jcbTamaño.getSelectedItem().toString());
        mascota.setPeso(normalizarPeso(txtPeso.getText()));

        // Dueño: setear explícitamente desde el combo (no depender del ActionListener)
        try {
            ClienteItem item = (ClienteItem) jcbDueño.getSelectedItem();
            if (item != null && item.getIdCliente() != null && item.getIdCliente() != 0) {
                Cliente c = cacheClientesPorId.get(item.getIdCliente());
                if (c == null) {
                    c = operandoCliente.buscarPorId(item.getIdCliente());
                    if (c != null) {
                        cacheClientesPorId.put(item.getIdCliente(), c);
                    }
                }
                mascota.setCliente(c);
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
        }

        if ("CreandoMascota".equals(Application.consultarEstadoUsuario())) {
            try {
                operandoMascota.crearMascota(mascota);
                JOptionPane.showMessageDialog(this, "Mascota guardada exitosamente.");
            } catch (Exception ex) {
                Logger.getLogger(FormMascota.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,
                        "No se pudo guardar la mascota.\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        if ("EditandoMascota".equals(Application.consultarEstadoUsuario())) {
            try {
                operandoMascota.actualizarMascota(mascota);
                JOptionPane.showMessageDialog(this, "Mascota actualizada exitosamente.");
            } catch (Exception ex) {
                Logger.getLogger(FormMascota.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,
                        "No se pudo actualizar la mascota.\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onDueñoSeleccionado(Mascota mascota) {
        ClienteItem dueñoSeleccionado = (ClienteItem) jcbDueño.getSelectedItem();  // Obtenemos ClienteItem seleccionado

        if (dueñoSeleccionado != null && dueñoSeleccionado.getIdCliente() != 0) {  // Validamos que no sea la opción "Seleccionar"
            Cliente cliente = cacheClientesPorId.get(dueñoSeleccionado.getIdCliente());
            if (cliente == null) {
                cliente = operandoCliente.buscarPorId(dueñoSeleccionado.getIdCliente());
                if (cliente != null) {
                    cacheClientesPorId.put(dueñoSeleccionado.getIdCliente(), cliente);
                }
            }
            mascota.setCliente(cliente);
        }
    }

    private void seleccionarClienteCbDueño(Integer idCliente) {
        // Recorremos los items en cbDueño para encontrar el idCliente correcto
        for (int i = 0; i < jcbDueño.getItemCount(); i++) {
            ClienteItem item = (ClienteItem) jcbDueño.getItemAt(i);
            if (item.getIdCliente().equals(idCliente)) {
                jcbDueño.setSelectedIndex(i);  // Seleccionamos el índice correspondiente
                break;
            }
        }
    }

    private boolean editarMascotaSeleccionada() {
        int selectedRow = operarTablas.comprobarElementoSeleccionado(tableMascotas);
        boolean bRetorno = false;
        Cliente cliente = new Cliente();

        if (selectedRow != -1) {
            var idMascotaSelec = (Integer) tableMascotas.getValueAt(selectedRow, 1);
            mascota = operandoMascota.buscarMascotaPorId(idMascotaSelec);
            if (mascota == null) {
                JOptionPane.showMessageDialog(this, "No se encontró la mascota seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            if (mascota.getCliente() != null && mascota.getCliente().getIdCliente() != null) {
                cliente = cacheClientesPorId.get(mascota.getCliente().getIdCliente());
                if (cliente == null) {
                    cliente = operandoCliente.buscarPorId(mascota.getCliente().getIdCliente());
                    if (cliente != null && cliente.getIdCliente() != null) {
                        cacheClientesPorId.put(cliente.getIdCliente(), cliente);
                    }
                }
            }

            // Guardar originales para el resumen de confirmación al editar
            originalNombre = mascota.getNombre();
            originalSexo = mascota.getSexo();
            originalRaza = mascota.getRaza();
            originalEspecie = mascota.getEspecie();
            originalPeso = mascota.getPeso();
            originalTamaño = mascota.getTamano();
            originalCastrado = mascota.getCastrado();
            originalFechaNacimiento = mascota.getFechaNacimiento();
            originalIdCliente = (mascota.getCliente() != null) ? mascota.getCliente().getIdCliente() : null;

            txtNombre.setText(mascota.getNombre());
            jcbSexo.setSelectedItem(mascota.getSexo());
            txtRaza.setText(mascota.getRaza());
            jcbEspecie.setSelectedItem(mascota.getEspecie());
            txtPeso.setText(mascota.getPeso());
            jcbTamaño.setSelectedItem(mascota.getTamano());
            if (cliente != null && cliente.getIdCliente() != null) {
                seleccionarClienteCbDueño(cliente.getIdCliente());
            } else {
                jcbDueño.setSelectedIndex(0);
            }
            if ("Sí".equals(mascota.getCastrado())) {
                rbtnSi.setSelected(true);
            } else {
                rbtnNo.setSelected(true);
            }
            jDateFechaNacimiento.setDate(java.sql.Date.valueOf(mascota.getFechaNacimiento()));

            bRetorno = true;
        } else {
            JOptionPane.showMessageDialog(null, "Debe seleccionar una mascota (checkbox) para poder editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;

    }

    // -------------------- Confirmaciones SI/NO (patrón FormUsuario/FormCliente) --------------------
    private boolean confirmarCreacionMascota() {
        String resumenHtml = buildResumenMascotaHtml(false);

        Object[] opciones = {"SI", "NO"};
        int opcion = JOptionPane.showOptionDialog(
                this,
                resumenHtml,
                "Confirmar creación de mascota",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        return opcion == 0; // 0 == "SI"
    }

    private boolean confirmarEdicionMascota() {
        String nuevoNombre = txtNombre.getText().trim();
        String nuevoSexo = (jcbSexo.getSelectedItem() != null) ? jcbSexo.getSelectedItem().toString() : "";
        String nuevoRaza = txtRaza.getText().trim();
        String nuevoEspecie = (jcbEspecie.getSelectedItem() != null) ? jcbEspecie.getSelectedItem().toString() : "";
        String nuevoTamaño = (jcbTamaño.getSelectedItem() != null) ? jcbTamaño.getSelectedItem().toString() : "";
        String nuevoPeso = txtPeso.getText().trim();
        String nuevoCastrado = rbtnSi.isSelected() ? "Sí" : "No";
        LocalDate nuevaFecha = null;
        try {
            if (jDateFechaNacimiento.getDate() != null) {
                nuevaFecha = jDateFechaNacimiento.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
        }

        Integer nuevoIdCliente = null;
        String nuevoDueñoNombre = "";
        try {
            ClienteItem item = (ClienteItem) jcbDueño.getSelectedItem();
            if (item != null) {
                nuevoIdCliente = item.getIdCliente();
                nuevoDueñoNombre = item.toString();
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
        }

        StringBuilder cambios = new StringBuilder();
        cambios.append("<html><body style='width:420px'>");
        cambios.append("<p>Se modificarán los siguientes datos:</p>");

        int cambiosCount = 0;
        cambiosCount += appendCambio(cambios, "Nombre", originalNombre, nuevoNombre);
        cambiosCount += appendCambio(cambios, "Sexo", originalSexo, nuevoSexo);
        cambiosCount += appendCambio(cambios, "Raza", originalRaza, nuevoRaza);
        cambiosCount += appendCambio(cambios, "Especie", originalEspecie, nuevoEspecie);
        cambiosCount += appendCambio(cambios, "Tamaño", originalTamaño, nuevoTamaño);
        cambiosCount += appendCambio(cambios, "Peso", originalPeso, nuevoPeso);
        cambiosCount += appendCambio(cambios, "Castrado", originalCastrado, nuevoCastrado);
        cambiosCount += appendCambio(cambios, "Fecha de nacimiento", formatFecha(originalFechaNacimiento), formatFecha(nuevaFecha));

        // Dueño
        String dueñoOriginal = "";
        if (originalIdCliente != null) {
            try {
                Cliente c = operandoCliente.buscarPorId(originalIdCliente);
                if (c != null && c.getPersona() != null) {
                    dueñoOriginal = c.getPersona().getApellido() + " " + c.getPersona().getNombre();
                }
            } catch (Exception ex) {
                veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
            }
        }
        String dueñoNuevo = (nuevoDueñoNombre == null) ? "" : nuevoDueñoNombre;
        cambiosCount += appendCambio(cambios, "Dueño", dueñoOriginal, dueñoNuevo);

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
                "Confirmar edición de mascota",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        return opcion == 0;
    }

    private String buildResumenMascotaHtml(boolean incluirId) {
        String nombre = txtNombre.getText().trim();
        String sexo = (jcbSexo.getSelectedItem() != null) ? jcbSexo.getSelectedItem().toString() : "";
        String raza = txtRaza.getText().trim();
        String especie = (jcbEspecie.getSelectedItem() != null) ? jcbEspecie.getSelectedItem().toString() : "";
        String tamaño = (jcbTamaño.getSelectedItem() != null) ? jcbTamaño.getSelectedItem().toString() : "";
        String peso = txtPeso.getText().trim();
        String castrado = rbtnSi.isSelected() ? "Sí" : "No";
        LocalDate fecha = null;
        try {
            if (jDateFechaNacimiento.getDate() != null) {
                fecha = jDateFechaNacimiento.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
        }

        String dueño = "";
        try {
            Object sel = jcbDueño.getSelectedItem();
            if (sel != null) {
                dueño = sel.toString();
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormMascota.class, "Excepción no fatal en UI", ex);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:360px'>");
        sb.append("<p>Confirme los datos de la mascota:</p>");
        if (incluirId && mascota != null && mascota.getIdMascota() != null) {
            sb.append("<b>ID:</b> ").append(escapeHtml(String.valueOf(mascota.getIdMascota()))).append("<br>");
        }
        sb.append("<b>Nombre:</b> ").append(escapeHtml(nombre)).append("<br>")
                .append("<b>Especie:</b> ").append(escapeHtml(especie)).append("<br>")
                .append("<b>Sexo:</b> ").append(escapeHtml(sexo)).append("<br>")
                .append("<b>Raza:</b> ").append(escapeHtml(raza)).append("<br>")
                .append("<b>Fecha de nacimiento:</b> ").append(escapeHtml(formatFecha(fecha))).append("<br>")
                .append("<b>Castrado:</b> ").append(escapeHtml(castrado)).append("<br>")
                .append("<b>Tamaño:</b> ").append(escapeHtml(tamaño)).append("<br>")
                .append("<b>Peso:</b> ").append(escapeHtml(peso)).append("<br>")
                .append("<b>Dueño:</b> ").append(escapeHtml(dueño))
                .append("</body></html>");
        return sb.toString();
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

    private String formatFecha(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        try {
            return String.format("%02d/%02d/%04d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
        } catch (Exception ex) {
            return fecha.toString();
        }
    }

    private void eliminarMascotaSeleccionada(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Por favor seleccione una mascota a ELIMINAR", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            var idMascota = (Integer) tableMascotas.getValueAt(selectedRow, 1);
            mascota = operandoMascota.buscarMascotaPorId(idMascota);
            if (mascota == null) {
                JOptionPane.showMessageDialog(this, "No se encontró la mascota seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (operandoMascota.eliminarMascota(mascota)) {
                JOptionPane.showMessageDialog(null, "Mascota desactivada con éxito", "Información", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "No se pudo eliminar la mascota.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            Logger.getLogger(FormMascota.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar la mascota.\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String calcularEdad(LocalDate fechaNacimiento) {
        LocalDate fechaActual = LocalDate.now();
        if (fechaNacimiento != null && fechaNacimiento.isBefore(fechaActual)) {
            Period periodo = Period.between(fechaNacimiento, fechaActual);
            int años = periodo.getYears();
            int meses = periodo.getMonths();
            String textMes = meses == 1 ? "mes" : "meses";
            String textAño = años == 1 ? "año" : "años";

            if (años == 0) {
                return meses + " " + textMes;
            }

            return años + " " + textAño + (meses > 0 ? " y " + meses + " " + textMes : "");
        } else {
            return "Fecha inválida";
        }
    }

    private void limpiarForm() {
        txtNombre.setText("");
        txtRaza.setText("");
        txtPeso.setText("");
        jDateFechaNacimiento.setDate(null);

        if (jcbSexo.getItemCount() > 0) {
            jcbSexo.setSelectedIndex(0);
        }
        if (jcbEspecie.getItemCount() > 0) {
            jcbEspecie.setSelectedIndex(0);
        }
        if (jcbDueño.getItemCount() > 0) {
            jcbDueño.setSelectedIndex(0);
        }
        if (jcbTamaño.getItemCount() > 0) {
            jcbTamaño.setSelectedIndex(0);
        }

        btnGroup.clearSelection();
    }

    // -------------------- Uniformidad confirmaciones --------------------
    private boolean confirmarEliminacionMascota(int selectedRow) {
        try {
            Object id = tableMascotas.getValueAt(selectedRow, 1);
            Object nombre = tableMascotas.getValueAt(selectedRow, 2);
            Object especie = tableMascotas.getValueAt(selectedRow, 6);
            Object dueño = tableMascotas.getValueAt(selectedRow, 7);

            StringBuilder sb = new StringBuilder();
            sb.append("<html><body style='width:360px'>");
            sb.append("<p>¿Confirma la eliminación del siguiente registro?</p>");
            sb.append("<b>ID:</b> ").append(escapeHtml(String.valueOf(id))).append("<br>");
            sb.append("<b>Nombre:</b> ").append(escapeHtml(String.valueOf(nombre))).append("<br>");
            sb.append("<b>Especie:</b> ").append(escapeHtml(String.valueOf(especie))).append("<br>");
            sb.append("<b>Dueño:</b> ").append(escapeHtml(String.valueOf(dueño))).append("<br>");
            sb.append("<br><b>Esta acción no se puede deshacer.</b>");
            sb.append("</body></html>");

            Object[] opciones = {"SI", "NO"};
            int opcion = JOptionPane.showOptionDialog(
                    this,
                    sb.toString(),
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    opciones,
                    opciones[1]
            );
            return opcion == 0;
        } catch (Exception ex) {
            Object[] opciones = {"SI", "NO"};
            int opcion = JOptionPane.showOptionDialog(
                    this,
                    "¿Estás seguro de que deseas eliminar esta mascota?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    opciones,
                    opciones[1]
            );
            return opcion == 0;
        }
    }

    private boolean validarDatosMascota() {

        if (txtNombre.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre de la mascota no puede estar vacío.");
            return false;
        }
        if (jcbSexo.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el sexo de la mascota.");
            return false;
        }
        if (txtRaza.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "La raza de la mascota no puede estar vacía.");
            return false;
        }
        if (jDateFechaNacimiento.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar la fecha de nacimiento de la mascota.");
            return false;
        }
        LocalDate fechaNacimiento = jDateFechaNacimiento.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "La fecha de nacimiento no puede ser futura.");
            return false;
        }
        if (jcbEspecie.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar la especie de la mascota.");
            return false;
        }
        if (jcbDueño.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el dueño de la mascota.");
            return false;
        }
        if (!rbtnSi.isSelected() && !rbtnNo.isSelected()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar si la mascota fue castrada o no.");
            return false;
        }
        if (jcbTamaño.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el tamaño de la mascota.");
            return false;
        }
        if (txtPeso.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Debe ingresar el peso de la mascota.");
            return false;
        }
        try {
            double peso = Double.parseDouble(normalizarPeso(txtPeso.getText()));
            if (peso <= 0) {
                JOptionPane.showMessageDialog(this, "El peso de la mascota debe ser mayor que cero.");
                return false;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un peso válido usando solo números. Ejemplo: 12.5");
            return false;
        }
        return true;
    }

    private String normalizarPeso(String pesoTexto) {
        return pesoTexto != null ? pesoTexto.trim().replace(",", ".") : "";
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.ButtonGroup btnGroup;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private com.toedter.calendar.JDateChooser jDateFechaNacimiento;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JComboBox<ClienteItem> jcbDueño;
    private javax.swing.JComboBox<String> jcbEspecie;
    private javax.swing.JComboBox<String> jcbSexo;
    private javax.swing.JComboBox<String> jcbTamaño;
    private javax.swing.JPanel jpListaPacientes;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbDueño;
    private javax.swing.JLabel lbEspecie;
    private javax.swing.JLabel lbFechaNacimiento;
    private javax.swing.JLabel lbListaDePacientes;
    private javax.swing.JLabel lbNombre;
    private javax.swing.JLabel lbPeso;
    private javax.swing.JLabel lbRaza;
    private javax.swing.JLabel lbSexo;
    private javax.swing.JLabel lbTamaño;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelDatos;
    private javax.swing.JPanel panelHadear;
    private javax.swing.JRadioButton rbtnNo;
    private javax.swing.JRadioButton rbtnSi;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tableMascotas;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JLabel txtCastrado;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtPeso;
    private javax.swing.JTextField txtRaza;
    // End of variables declaration//GEN-END:variables

}
