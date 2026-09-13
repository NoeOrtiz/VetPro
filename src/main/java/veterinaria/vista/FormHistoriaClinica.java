package veterinaria.vista;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;
import javax.swing.JOptionPane;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.controlador.HistoriaEventoControlador;
import veterinaria.controlador.MascotaControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.HistoriaEvento;
import veterinaria.entidad.Mascota;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.HistoriaEventoUIRouter;

public class FormHistoriaClinica extends javax.swing.JPanel {

    // Mapeo simple para el combo (index -> idMascota)
    private final List<Map.Entry<Integer, String>> listaMascotas = new ArrayList<>();
    private Mascota mascotaSeleccionada;
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();
    private final MascotaControlador mascotaControlador = new MascotaControlador();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // 🚀 REEMPLAZÁ TU LÍNEA POR ESTA:
    private final ReporteService reporteService = ReporteService.getInstance();

    public FormHistoriaClinica() {
        initComponents();
        PermisoUI.aplicar(this);
        inicializarTabla();
        cargarComboMascotas();
        initListeners();
        actualizarCantidadRegistros(0);
        actualizarEstadoBotones();

    }

    private void initListeners() {

        jcbPaciente.addActionListener(e -> {

            int idx = jcbPaciente.getSelectedIndex();

            if (idx <= 0) {
                limpiarDetalleMascota();
                limpiarTabla();
                return;
            }

            Integer idMascota = listaMascotas.get(idx - 1).getKey();

            mascotaSeleccionada = mascotaControlador.buscarMascotaPorId(idMascota);

            mostrarDetalleMascota();

            // Solo muestra los datos. La historia se consulta con el botón.
            limpiarTabla();

        });

        btnVerHistoria.addActionListener(e -> {

            cargarHistoria();

        });

        // Habilita el botón "Abrir Registro" cuando se selecciona una fila
        tableHistoriaClinica.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {

                actualizarEstadoBotones();

            }

        });

    }

    private void inicializarTabla() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Fecha", "Tipo", "Profesional", "Resumen", "Observaciones"}
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        tableHistoriaClinica.setModel(model);
        tableHistoriaClinica.getTableHeader().setReorderingAllowed(false);
        limpiarTabla();
    }

    private void actualizarCantidadRegistros(int cantidad) {

        lblCantidad.setText(String.valueOf(cantidad));

    }

    private void cargarComboMascotas() {
        listaMascotas.clear();
        jcbPaciente.removeAllItems();
        jcbPaciente.addItem("Seleccionar Mascota");

        List<Mascota> mascotas = mascotaControlador.buscarTodasLasMascotas();
        if (mascotas == null) {
            return;
        }

        for (Mascota m : mascotas) {
            String label = (m.getNombre() != null ? m.getNombre() : "Mascota") /*+ " (#" + m.getIdMascota() + ")"*/;
            listaMascotas.add(new AbstractMap.SimpleEntry<>(m.getIdMascota(), label));
            jcbPaciente.addItem(label);
        }
    }

    private void mostrarDetalleMascota() {
        if (mascotaSeleccionada == null) {
            limpiarDetalleMascota();
            return;
        }

        Cliente c = mascotaSeleccionada.getCliente();
        String duenio = "";
        if (c != null && c.getPersona() != null) {
            String a = c.getPersona().getApellido() != null ? c.getPersona().getApellido() : "";
            String n = c.getPersona().getNombre() != null ? c.getPersona().getNombre() : "";
            duenio = (a + " " + n).trim();
        }

        txtDueño.setText(duenio);
        txtEspecie.setText(nullToEmpty(mascotaSeleccionada.getEspecie()));
        txtRaza.setText(nullToEmpty(mascotaSeleccionada.getRaza()));
        txtSexo.setText(nullToEmpty(mascotaSeleccionada.getSexo()));
        txtPeso.setText(nullToEmpty(mascotaSeleccionada.getPeso()));
        txtEdad.setText(calcularEdadTexto(mascotaSeleccionada.getFechaNacimiento()));
    }

    private void limpiarDetalleMascota() {
        mascotaSeleccionada = null;
        txtDueño.setText("");
        txtEspecie.setText("");
        txtRaza.setText("");
        txtSexo.setText("");
        txtEdad.setText("");
        txtPeso.setText("");
    }

    private String calcularEdadTexto(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return "";
        }
        try {
            Period p = Period.between(fechaNacimiento, LocalDate.now());
            if (p.getYears() > 0) {
                return p.getYears() + " años";
            }
            if (p.getMonths() > 0) {
                return p.getMonths() + " meses";
            }
            return p.getDays() + " días";
        } catch (Exception e) {
            return "";
        }
    }

    private void cargarHistoria() {

        limpiarTabla();

        if (mascotaSeleccionada == null || mascotaSeleccionada.getIdMascota() == null) {
            return;
        }

        List<HistoriaEvento> eventos
                = historiaEventoControlador.listarPorMascota(
                        mascotaSeleccionada.getIdMascota());

        if (eventos == null || eventos.isEmpty()) {

            actualizarCantidadRegistros(0);

            JOptionPane.showMessageDialog(
                    this,
                    "La mascota seleccionada aún no posee registros en su historia clínica.",
                    "Historia Clínica",
                    JOptionPane.INFORMATION_MESSAGE);

            return;
        }

        actualizarCantidadRegistros(eventos.size());

        DefaultTableModel model = (DefaultTableModel) tableHistoriaClinica.getModel();

        for (HistoriaEvento ev : eventos) {

            String fechaTxt = "";

            if (ev.getFecha() != null) {
                fechaTxt = dtf.format(ev.getFecha());
            }

            model.addRow(new Object[]{
                ev.getIdEvento(),
                fechaTxt,
                nullToEmpty(ev.getTipo()),
                nullToEmpty(ev.getProfesional()),
                nullToEmpty(ev.getResumen()),
                nullToEmpty(ev.getObservaciones())
            });

        }

        ocultarColumnaId();
        actualizarEstadoBotones();

    }

    private void limpiarTabla() {

        DefaultTableModel model = (DefaultTableModel) tableHistoriaClinica.getModel();

        model.setRowCount(0);

        actualizarCantidadRegistros(0);
        actualizarEstadoBotones();

    }

    private void ocultarColumnaId() {

        if (tableHistoriaClinica.getColumnModel().getColumnCount() > 0) {

            tableHistoriaClinica.removeColumn(
                    tableHistoriaClinica.getColumnModel().getColumn(0));

        }

    }

    private void abrirDetalleEventoSeleccionado() {

        int row = tableHistoriaClinica.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un evento en la tabla.");
            return;
        }

        // Obtener la fila REAL del modelo
        int modelRow = tableHistoriaClinica.convertRowIndexToModel(row);

        // Obtener el ID desde el modelo (aunque la columna esté oculta)
        Object idObj = tableHistoriaClinica.getModel().getValueAt(modelRow, 0);

        if (idObj == null) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo obtener el identificador del evento.");
            return;
        }

        Integer idEvento;

        try {
            idEvento = Integer.valueOf(idObj.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al obtener el identificador del evento.");
            return;
        }

        HistoriaEvento ev = historiaEventoControlador.buscarPorId(idEvento);

        if (ev == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el evento.");
            return;
        }

        HistoriaEventoUIRouter.abrir(ev);

    }

    private void actualizarEstadoBotones() {

        boolean hayMascota = mascotaSeleccionada != null;

        boolean hayRegistros = tableHistoriaClinica.getRowCount() > 0;

        boolean haySeleccion = tableHistoriaClinica.getSelectedRow() >= 0;

        btnVerHistoria.setEnabled(hayMascota);

        btnImprimirHistoria.setEnabled(hayRegistros);

        btnImprimirListaHistorias.setEnabled(hayRegistros);

        btnVerRegistros.setEnabled(haySeleccion);

        lblCantidad.setText(String.valueOf(tableHistoriaClinica.getRowCount()));

    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lbConsumos = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jcbPaciente = new javax.swing.JComboBox<>();
        lbPaciente = new javax.swing.JLabel();
        lbDueño = new javax.swing.JLabel();
        txtDueño = new javax.swing.JTextField();
        lbEspecie = new javax.swing.JLabel();
        txtEspecie = new javax.swing.JTextField();
        txtRaza = new javax.swing.JTextField();
        lbRaza = new javax.swing.JLabel();
        lbRaza1 = new javax.swing.JLabel();
        txtSexo = new javax.swing.JTextField();
        txtEdad = new javax.swing.JTextField();
        lbEdad = new javax.swing.JLabel();
        lbPeso = new javax.swing.JLabel();
        txtPeso = new javax.swing.JTextField();
        btnImprimirHistoria = new javax.swing.JButton();
        btnVerHistoria = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        lbListaServicios = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scroll1 = new javax.swing.JScrollPane();
        tableHistoriaClinica = new veterinaria.vista.table.AutoTable();
        btnImprimirListaHistorias = new javax.swing.JButton();
        btnVerRegistros = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        lblCantidadReg = new javax.swing.JLabel();
        lblCantidad = new javax.swing.JLabel();

        lbConsumos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbConsumos.setText("Historia Clínica de mascotas");

        jcbPaciente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Mascota" }));

        lbPaciente.setText("Paciente: *");

        lbDueño.setText("Dueño:");

        txtDueño.setEditable(false);

        lbEspecie.setText("Especie:");

        txtEspecie.setEditable(false);

        txtRaza.setEditable(false);

        lbRaza.setText("Raza:");

        lbRaza1.setText("Sexo:");

        txtSexo.setEditable(false);

        txtEdad.setEditable(false);

        lbEdad.setText("Edad:");

        lbPeso.setText("Peso (KG):");

        txtPeso.setEditable(false);

        btnImprimirHistoria.setText("Imprimir Historia ");
        btnImprimirHistoria.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirHistoriaActionPerformed(evt);
            }
        });

        btnVerHistoria.setText("Consultar Historia");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(23, 23, 23)
                                .addComponent(lbConsumos))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbPaciente)
                                            .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtDueño, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(lbDueño)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnVerHistoria)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnImprimirHistoria))))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(lbEspecie)
                                            .addComponent(txtEspecie, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbRaza)
                                            .addComponent(txtRaza, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbRaza1)
                                            .addComponent(txtSexo, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbEdad)
                                            .addComponent(txtEdad, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbPeso)
                                            .addComponent(txtPeso, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                        .addGap(13, 13, 13)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbConsumos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnImprimirHistoria)
                                    .addComponent(btnVerHistoria))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtDueño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(lbPaciente)
                                    .addComponent(lbDueño))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(lbEspecie)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtEspecie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(lbRaza)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRaza, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(lbRaza1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSexo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(lbEdad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtEdad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(lbPeso)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPeso, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        lbListaServicios.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaServicios.setText("Registros de la Historia Clínica");

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableHistoriaClinica.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Fecha", "Tipo", "Profesional", "Resumen", "Observaciones"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableHistoriaClinica.setMinimumSize(new java.awt.Dimension(848, 220));
        tableHistoriaClinica.setPreferredSize(new java.awt.Dimension(848, 220));
        tableHistoriaClinica.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableHistoriaClinica);

        btnImprimirListaHistorias.setText("Imprimir Lista");
        btnImprimirListaHistorias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirListaHistoriasActionPerformed(evt);
            }
        });

        btnVerRegistros.setText("Abrir Registro");
        btnVerRegistros.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerRegistrosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lbListaServicios)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnVerRegistros)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnImprimirListaHistorias)
                        .addGap(17, 17, 17))
                    .addComponent(jSeparator3)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 1110, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListaServicios)
                    .addComponent(btnImprimirListaHistorias)
                    .addComponent(btnVerRegistros))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE))
        );

        lblCantidadReg.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblCantidadReg.setText("Cantidad de Registros:");

        lblCantidad.setText("Cantidad");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCantidadReg)
                .addGap(18, 18, 18)
                .addComponent(lblCantidad)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCantidadReg)
                    .addComponent(lblCantidad))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnImprimirHistoriaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirHistoriaActionPerformed
        imprimirHistoriaClinicaDetallada();
    }//GEN-LAST:event_btnImprimirHistoriaActionPerformed

    private void btnImprimirListaHistoriasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirListaHistoriasActionPerformed
        imprimirListaHistoriaClinica();
    }//GEN-LAST:event_btnImprimirListaHistoriasActionPerformed

    private void btnVerRegistrosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerRegistrosActionPerformed
        abrirDetalleEventoSeleccionado();
    }//GEN-LAST:event_btnVerRegistrosActionPerformed

    private void imprimirListaHistoriaClinica() {
        try {
            if (mascotaSeleccionada == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una mascota.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (tableHistoriaClinica == null || tableHistoriaClinica.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay eventos para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("tabla", tableHistoriaClinica)
                    .put("mascota", mascotaSeleccionada);

            reporteService.generar(ReporteTipo.HISTORIA_CLINICA_LISTADO, req);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al imprimir", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirHistoriaClinicaDetallada() {
        try {
            if (mascotaSeleccionada == null || mascotaSeleccionada.getIdMascota() == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una mascota.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            List<HistoriaEvento> eventos = historiaEventoControlador.listarPorMascota(mascotaSeleccionada.getIdMascota());
            if (eventos == null || eventos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "La mascota no tiene eventos para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("mascota", mascotaSeleccionada)
                    .put("eventos", eventos);

            reporteService.generar(ReporteTipo.HISTORIA_CLINICA, req);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al imprimir", JOptionPane.ERROR_MESSAGE);
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnImprimirHistoria;
    private javax.swing.JButton btnImprimirListaHistorias;
    private javax.swing.JButton btnVerHistoria;
    private javax.swing.JButton btnVerRegistros;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JComboBox<String> jcbPaciente;
    private javax.swing.JLabel lbConsumos;
    private javax.swing.JLabel lbDueño;
    private javax.swing.JLabel lbEdad;
    private javax.swing.JLabel lbEspecie;
    private javax.swing.JLabel lbListaServicios;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbPeso;
    private javax.swing.JLabel lbRaza;
    private javax.swing.JLabel lbRaza1;
    private javax.swing.JLabel lblCantidad;
    private javax.swing.JLabel lblCantidadReg;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JTable tableHistoriaClinica;
    private javax.swing.JTextField txtDueño;
    private javax.swing.JTextField txtEdad;
    private javax.swing.JTextField txtEspecie;
    private javax.swing.JTextField txtPeso;
    private javax.swing.JTextField txtRaza;
    private javax.swing.JTextField txtSexo;
    // End of variables declaration//GEN-END:variables
}
