package veterinaria.vista;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import veterinaria.entidad.PeluqueriaHistorial;
import veterinaria.persistencia.PeluqueriaHistorialDAO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.PeluqueriaControlador;
import veterinaria.entidad.EstadoPeluqueriaEnum;
import veterinaria.entidad.Peluqueria;
import java.time.LocalDate;
import java.time.ZoneId;
import veterinaria.persistencia.MascotaDAO;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.util.MascotaItem;
import veterinaria.entidad.util.VeterinarioItem;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import javax.swing.JOptionPane;

public class FormHistorialTurnos extends javax.swing.JPanel {

    private final PeluqueriaControlador operarTurno = new PeluqueriaControlador();
    private final PeluqueriaHistorialDAO turnoHistorialDAO = new PeluqueriaHistorialDAO();

    private final MascotaDAO mascotaDAO = new MascotaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    // 🚀 REEMPLAZÁ TU LÍNEA POR ESTA:
    private final ReporteService reporteService = ReporteService.getInstance();
    private List<Peluqueria> ultimoListado = new ArrayList<>();
    private boolean actualizandoSeleccionTabla = false;

    private static final int IDX_COL_ESTADO = 8;
    private static final int IDX_COL_MOTIVO = 9;

    private javax.swing.table.TableColumn colMotivoCache = null;

    private enum ModoFiltro {
        POR_ESTADO,
        POR_FECHA,
        POR_PACIENTE,
        POR_VETERINARIO
    }

    private ModoFiltro modoFiltro = ModoFiltro.POR_ESTADO;

    public FormHistorialTurnos() {
        initComponents();

        initColumnaMotivoCache();
        // Cargar combos
        configurarComboEstado();
        configurarComboPaciente();
        configurarComboVeterinario();

        // Wire UI
        wireTablaSeleccion();
        wireFiltrosUI();
        wireAcciones();

        // Modo por defecto
        setModoFiltro(ModoFiltro.POR_ESTADO);
        cargarHistorialPorDefecto();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelHadear = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jckbPorFecha = new javax.swing.JCheckBox();
        jckbPorPaciente = new javax.swing.JCheckBox();
        jckbPorVeterinario = new javax.swing.JCheckBox();
        jckbPorEstado = new javax.swing.JCheckBox();
        btnBuscar = new javax.swing.JButton();
        btnImprimir = new javax.swing.JButton();
        jpPorFecha = new javax.swing.JPanel();
        jdcFechaHasta = new com.toedter.calendar.JDateChooser();
        jdcFechaDesde = new com.toedter.calendar.JDateChooser();
        lbDesde = new javax.swing.JLabel();
        lbHasta = new javax.swing.JLabel();
        lbFiltrarPorFecha = new javax.swing.JLabel();
        jsFecha = new javax.swing.JSeparator();
        jpPorPaciente = new javax.swing.JPanel();
        jsPaciente = new javax.swing.JSeparator();
        lbFiltrarPorPaciente = new javax.swing.JLabel();
        lbPaciente = new javax.swing.JLabel();
        jcbPaciente = new javax.swing.JComboBox();
        jpDatosFiltrados = new javax.swing.JPanel();
        lbTurnosRegistrados = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        scroll = new javax.swing.JScrollPane();
        tableTurnos = new veterinaria.vista.table.AutoTable();
        jpPorVeterinario = new javax.swing.JPanel();
        lbFiltrarPorVeterinario = new javax.swing.JLabel();
        lbVeterinarioAsignado = new javax.swing.JLabel();
        jcbVeterinario = new javax.swing.JComboBox();
        jsVeterinario = new javax.swing.JSeparator();
        jpPorEstado = new javax.swing.JPanel();
        lbFiltrarPorEstado = new javax.swing.JLabel();
        lbEstado = new javax.swing.JLabel();
        jcbEstado = new javax.swing.JComboBox();
        jsEstado = new javax.swing.JSeparator();

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Historial de turnos de Peluquería");

        jckbPorFecha.setText("Por fecha");

        jckbPorPaciente.setText("Por paciente");

        jckbPorVeterinario.setText("Por veterinario");

        jckbPorEstado.setText("Por estado");

        btnBuscar.setText("Buscar");

        btnImprimir.setText("Imprimir");

        javax.swing.GroupLayout panelHadearLayout = new javax.swing.GroupLayout(panelHadear);
        panelHadear.setLayout(panelHadearLayout);
        panelHadearLayout.setHorizontalGroup(
            panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jckbPorFecha)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jckbPorPaciente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jckbPorVeterinario)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jckbPorEstado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 270, Short.MAX_VALUE)
                .addComponent(btnImprimir, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panelHadearLayout.setVerticalGroup(
            panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelHadearLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jckbPorFecha)
                    .addComponent(jckbPorPaciente)
                    .addComponent(jckbPorVeterinario)
                    .addComponent(jckbPorEstado)
                    .addComponent(btnBuscar)
                    .addComponent(btnImprimir))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        lbDesde.setText("Desde:");

        lbHasta.setText("Hasta:");

        lbFiltrarPorFecha.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbFiltrarPorFecha.setText("Filtrar por fecha:");

        javax.swing.GroupLayout jpPorFechaLayout = new javax.swing.GroupLayout(jpPorFecha);
        jpPorFecha.setLayout(jpPorFechaLayout);
        jpPorFechaLayout.setHorizontalGroup(
            jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorFechaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpPorFechaLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jdcFechaDesde, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbDesde))
                        .addGap(18, 18, 18)
                        .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jdcFechaHasta, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbHasta)))
                    .addComponent(lbFiltrarPorFecha))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpPorFechaLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jsFecha)
                    .addContainerGap()))
        );
        jpPorFechaLayout.setVerticalGroup(
            jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorFechaLayout.createSequentialGroup()
                .addComponent(lbFiltrarPorFecha)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbDesde)
                    .addComponent(lbHasta))
                .addGap(7, 7, 7)
                .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jdcFechaDesde, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jdcFechaHasta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
            .addGroup(jpPorFechaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpPorFechaLayout.createSequentialGroup()
                    .addContainerGap(75, Short.MAX_VALUE)
                    .addComponent(jsFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );

        lbFiltrarPorPaciente.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbFiltrarPorPaciente.setText("Filtrar por paciente:");

        lbPaciente.setText("Paciente: *");

        jcbPaciente.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar una Mascota" }));

        javax.swing.GroupLayout jpPorPacienteLayout = new javax.swing.GroupLayout(jpPorPaciente);
        jpPorPaciente.setLayout(jpPorPacienteLayout);
        jpPorPacienteLayout.setHorizontalGroup(
            jpPorPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorPacienteLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpPorPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jsPaciente)
                    .addGroup(jpPorPacienteLayout.createSequentialGroup()
                        .addComponent(lbFiltrarPorPaciente)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jpPorPacienteLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jpPorPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbPaciente)
                    .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jpPorPacienteLayout.setVerticalGroup(
            jpPorPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorPacienteLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbFiltrarPorPaciente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbPaciente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jsPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        lbTurnosRegistrados.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbTurnosRegistrados.setText("Turnos de Peluquería registrados:");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableTurnos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Nº Turno", "Fecha Turno", "Paciente", "Cliente", "Tipo de Cita", "Veterinario", "Precio", "Estado", "Motivo", "Fecha Evento"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableTurnos.setMinimumSize(new java.awt.Dimension(848, 220));
        tableTurnos.setPreferredSize(new java.awt.Dimension(848, 220));
        tableTurnos.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableTurnos);

        javax.swing.GroupLayout jpDatosFiltradosLayout = new javax.swing.GroupLayout(jpDatosFiltrados);
        jpDatosFiltrados.setLayout(jpDatosFiltradosLayout);
        jpDatosFiltradosLayout.setHorizontalGroup(
            jpDatosFiltradosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosFiltradosLayout.createSequentialGroup()
                .addGroup(jpDatosFiltradosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpDatosFiltradosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpDatosFiltradosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpDatosFiltradosLayout.createSequentialGroup()
                                .addComponent(lbTurnosRegistrados)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addComponent(scroll))
                .addContainerGap())
        );
        jpDatosFiltradosLayout.setVerticalGroup(
            jpDatosFiltradosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosFiltradosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbTurnosRegistrados)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE))
        );

        lbFiltrarPorVeterinario.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbFiltrarPorVeterinario.setText("Filtrar por veterinario:");

        lbVeterinarioAsignado.setText("Veterinario:");

        jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccione veterinario" }));

        javax.swing.GroupLayout jpPorVeterinarioLayout = new javax.swing.GroupLayout(jpPorVeterinario);
        jpPorVeterinario.setLayout(jpPorVeterinarioLayout);
        jpPorVeterinarioLayout.setHorizontalGroup(
            jpPorVeterinarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorVeterinarioLayout.createSequentialGroup()
                .addGroup(jpPorVeterinarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpPorVeterinarioLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lbFiltrarPorVeterinario))
                    .addGroup(jpPorVeterinarioLayout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(jpPorVeterinarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbVeterinarioAsignado, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpPorVeterinarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpPorVeterinarioLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jsVeterinario)
                    .addContainerGap()))
        );
        jpPorVeterinarioLayout.setVerticalGroup(
            jpPorVeterinarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorVeterinarioLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbFiltrarPorVeterinario)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbVeterinarioAsignado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
            .addGroup(jpPorVeterinarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpPorVeterinarioLayout.createSequentialGroup()
                    .addGap(83, 83, 83)
                    .addComponent(jsVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        lbFiltrarPorEstado.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbFiltrarPorEstado.setText("Filtrar por estado:");

        lbEstado.setText("Estado: *");

        jcbEstado.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar un Estado" }));

        javax.swing.GroupLayout jpPorEstadoLayout = new javax.swing.GroupLayout(jpPorEstado);
        jpPorEstado.setLayout(jpPorEstadoLayout);
        jpPorEstadoLayout.setHorizontalGroup(
            jpPorEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorEstadoLayout.createSequentialGroup()
                .addGroup(jpPorEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpPorEstadoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lbFiltrarPorEstado))
                    .addGroup(jpPorEstadoLayout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jpPorEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbEstado)
                            .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpPorEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpPorEstadoLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jsEstado)
                    .addContainerGap()))
        );
        jpPorEstadoLayout.setVerticalGroup(
            jpPorEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPorEstadoLayout.createSequentialGroup()
                .addComponent(lbFiltrarPorEstado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbEstado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 16, Short.MAX_VALUE))
            .addGroup(jpPorEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpPorEstadoLayout.createSequentialGroup()
                    .addContainerGap(79, Short.MAX_VALUE)
                    .addComponent(jsEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpDatosFiltrados, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jpPorEstado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpPorVeterinario, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpPorPaciente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelHadear, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpPorFecha, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelHadear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpPorFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpPorPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpPorVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jpPorEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatosFiltrados, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Aplica restricciones al selector de fecha (JDateChooser): - No permite
     * fechas pasadas. - Valida que solo se puedan usar los días habilitados en
     * configuración. - Cuando la fecha es válida, carga el combo de turnos
     * (mañana/tarde) según configuración.
     */
    private void wireAcciones() {
        // Buscar (según modo)
        btnBuscar.addActionListener(e -> {
            aplicarFiltroActivo();
        });

        // Estado: refrescar cuando cambia si estamos en modo estado
        jcbEstado.addActionListener(e -> {
            if (modoFiltro == ModoFiltro.POR_ESTADO) {
                aplicarFiltroEstado();
            }
        });

        // Fecha: refrescar cuando cambia si estamos en modo fecha
        try {
            jdcFechaDesde.addPropertyChangeListener("date", evt -> {
                if (modoFiltro == ModoFiltro.POR_FECHA) {
                    aplicarFiltroFecha();
                }
            });
            jdcFechaHasta.addPropertyChangeListener("date", evt -> {
                if (modoFiltro == ModoFiltro.POR_FECHA) {
                    aplicarFiltroFecha();
                }
            });
        } catch (Exception ignore) {
        }

        // Imprimir
        btnImprimir.addActionListener(e -> {
            imprimirListadoActual();
        });
    }

    private void configurarComboEstado() {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccionar un Estado");
        model.addElement("Todos");
        for (EstadoPeluqueriaEnum e : EstadoPeluqueriaEnum.values()) {
            model.addElement(e);
        }
        jcbEstado.setModel(model);
        try {
            jcbEstado.setSelectedIndex(0);
        } catch (Exception ignore) {
        }
    }

    private void configurarComboPaciente() {
        try {
            DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccionar una Mascota");
            model.addElement("Todos");

            List<Mascota> mascotas = mascotaDAO.buscarTodos();
            if (mascotas != null) {
                for (Mascota m : mascotas) {
                    if (m == null) {
                        continue;
                    }
                    String nombre = (m.getNombre() != null) ? m.getNombre() : ("Mascota #" + m.getIdMascota());
                    model.addElement(new MascotaItem(m.getIdMascota(), nombre));
                }
            }

            jcbPaciente.setModel(model);
            jcbPaciente.setSelectedIndex(0);
        } catch (Exception e) {
            // si falla, dejamos el modelo del .form
        }
    }

    private void configurarComboVeterinario() {
        try {
            DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccionar un Veterinario");
            model.addElement("Todos");

            List<Usuario> vets = usuarioDAO.obtenerUsuariosVeterinarios();
            if (vets != null) {
                for (Usuario u : vets) {
                    if (u == null) {
                        continue;
                    }
                    Integer id = u.getIdUsuario();
                    String nombre = "";
                    try {
                        if (u.getPersona() != null) {
                            String a = u.getPersona().getApellido();
                            String n = u.getPersona().getNombre();
                            nombre = ((a != null ? a : "") + " " + (n != null ? n : "")).trim();
                        }
                    } catch (Exception ignore) {
                    }
                    if (nombre == null || nombre.trim().isEmpty()) {
                        nombre = (u.getNombreUsuario() != null) ? u.getNombreUsuario() : ("Veterinario #" + id);
                    }
                    model.addElement(new VeterinarioItem(id, nombre));
                }
            }

            jcbVeterinario.setModel(model);
            jcbVeterinario.setSelectedIndex(0);
        } catch (Exception e) {
            // si falla, dejamos el modelo del .form
        }
    }

    private void wireFiltrosUI() {
        // Exclusividad de filtros (se comportan como radio)
        jckbPorEstado.addActionListener(e -> {
            if (jckbPorEstado.isSelected()) {
                setModoFiltro(ModoFiltro.POR_ESTADO);
            } else {
                asegurarModoValido();
            }
        });
        jckbPorFecha.addActionListener(e -> {
            if (jckbPorFecha.isSelected()) {
                setModoFiltro(ModoFiltro.POR_FECHA);
            } else {
                asegurarModoValido();
            }
        });
        jckbPorPaciente.addActionListener(e -> {
            if (jckbPorPaciente.isSelected()) {
                setModoFiltro(ModoFiltro.POR_PACIENTE);
            } else {
                asegurarModoValido();
            }
        });
        jckbPorVeterinario.addActionListener(e -> {
            if (jckbPorVeterinario.isSelected()) {
                setModoFiltro(ModoFiltro.POR_VETERINARIO);
            } else {
                asegurarModoValido();
            }
        });

        // Al cambiar combos/fechas, si el modo coincide, refrescamos
        jcbPaciente.addActionListener(e -> {
            if (modoFiltro == ModoFiltro.POR_PACIENTE) {
                aplicarFiltroActivo();
            }
        });
        jcbVeterinario.addActionListener(e -> {
            if (modoFiltro == ModoFiltro.POR_VETERINARIO) {
                aplicarFiltroActivo();
            }
        });
    }

    private void asegurarModoValido() {
        if (jckbPorEstado.isSelected() || jckbPorFecha.isSelected() || jckbPorPaciente.isSelected() || jckbPorVeterinario.isSelected()) {
            // ya hay alguno seleccionado: el método setModoFiltro se dispara en su listener
            return;
        }
        // Si quedan todos desmarcados, volvemos a Por Estado
        setModoFiltro(ModoFiltro.POR_ESTADO);
    }

    private void setModoFiltro(ModoFiltro modo) {
        this.modoFiltro = (modo != null) ? modo : ModoFiltro.POR_ESTADO;

        // Marcar/desmarcar checkboxes en forma exclusiva
        jckbPorEstado.setSelected(this.modoFiltro == ModoFiltro.POR_ESTADO);
        jckbPorFecha.setSelected(this.modoFiltro == ModoFiltro.POR_FECHA);
        jckbPorPaciente.setSelected(this.modoFiltro == ModoFiltro.POR_PACIENTE);
        jckbPorVeterinario.setSelected(this.modoFiltro == ModoFiltro.POR_VETERINARIO);

        // Mostrar/ocultar paneles
        jpPorEstado.setVisible(this.modoFiltro == ModoFiltro.POR_ESTADO);
        jpPorFecha.setVisible(this.modoFiltro == ModoFiltro.POR_FECHA);
        jpPorPaciente.setVisible(this.modoFiltro == ModoFiltro.POR_PACIENTE);
        jpPorVeterinario.setVisible(this.modoFiltro == ModoFiltro.POR_VETERINARIO);

        try {
            revalidate();
            repaint();
        } catch (Exception ignore) {
        }

        // Cuando cambiamos modo, volvemos a cargar
        aplicarFiltroActivo();
    }

    private void aplicarFiltroActivo() {
        switch (modoFiltro) {
            case POR_FECHA:
                aplicarFiltroFecha();
                break;
            case POR_PACIENTE:
                aplicarFiltroPaciente();
                break;
            case POR_VETERINARIO:
                aplicarFiltroVeterinario();
                break;
            case POR_ESTADO:
            default:
                aplicarFiltroEstado();
                break;
        }
    }

    private void cargarHistorialPorDefecto() {
        List<EstadoPeluqueriaEnum> estados = Arrays.asList(
                EstadoPeluqueriaEnum.COMPLETADO,
                EstadoPeluqueriaEnum.CANCELADO,
                EstadoPeluqueriaEnum.ELIMINADO
        );
        List<Peluqueria> turnos = operarTurno.obtenerTurnosPorEstados(estados);
        cargarTurnosEnTabla(tableTurnos, turnos);
        ultimoListado = (turnos != null) ? turnos : new ArrayList<>();
        try {
            tableTurnos.clearSelection();
        } catch (Exception ignore) {
        }
    }

    private void aplicarFiltroEstado() {
        // Si no se usa filtro por estado, mantenemos el default del historial.
        if (!jckbPorEstado.isSelected()) {
            cargarHistorialPorDefecto();
            return;
        }

        Object selObj = jcbEstado.getSelectedItem();
        if (selObj == null) {
            cargarHistorialPorDefecto();
            return;
        }

        if (selObj instanceof String) {
            String sel = ((String) selObj).trim();
            if (sel.isEmpty() || sel.equalsIgnoreCase("Seleccionar un Estado")) {
                cargarHistorialPorDefecto();
                return;
            }

            // "Todos" debe mostrar TODOS los registros (incluye cancelados/eliminados).
            if (sel.equalsIgnoreCase("Todos")) {
                List<Peluqueria> turnos = operarTurno.obtenerTodosLosTurnosSinFiltro();
                cargarTurnosEnTabla(tableTurnos, turnos);
                ultimoListado = (turnos != null) ? turnos : new ArrayList<>();
                try {
                    tableTurnos.clearSelection();
                } catch (Exception ignore) {
                }
                return;
            }

            // Si llega un string inesperado, degradamos al default.
            cargarHistorialPorDefecto();
            return;
        }

        if (selObj instanceof EstadoPeluqueriaEnum) {
            EstadoPeluqueriaEnum estado = (EstadoPeluqueriaEnum) selObj;
            List<Peluqueria> turnos = operarTurno.obtenerTurnosPorEstados(Arrays.asList(estado));
            cargarTurnosEnTabla(tableTurnos, turnos);
            ultimoListado = (turnos != null) ? turnos : new ArrayList<>();
            try {
                tableTurnos.clearSelection();
            } catch (Exception ignore) {
            }
            return;
        }

        cargarHistorialPorDefecto();
    }

    private void aplicarFiltroFecha() {
        // Base: historial por defecto
        List<Peluqueria> base = obtenerBaseHistorial();

        LocalDate desde = dateChooserToLocalDate(jdcFechaDesde);
        LocalDate hasta = dateChooserToLocalDate(jdcFechaHasta);

        if (desde == null && hasta == null) {
            cargarTurnosEnTabla(tableTurnos, base);
            ultimoListado = (base != null) ? base : new ArrayList<>();
            return;
        }

        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            // swap
            LocalDate tmp = desde;
            desde = hasta;
            hasta = tmp;
        }

        List<Peluqueria> filtrados = new ArrayList<>();
        if (base != null) {
            for (Peluqueria t : base) {
                if (t == null || t.getFecha() == null) {
                    continue;
                }
                LocalDate f = t.getFecha();
                boolean ok = true;
                if (desde != null && f.isBefore(desde)) {
                    ok = false;
                }
                if (hasta != null && f.isAfter(hasta)) {
                    ok = false;
                }
                if (ok) {
                    filtrados.add(t);
                }
            }
        }

        cargarTurnosEnTabla(tableTurnos, filtrados);
        ultimoListado = filtrados;
    }

    private void aplicarFiltroPaciente() {
        List<Peluqueria> base = obtenerBaseHistorial();

        Object selObj = jcbPaciente.getSelectedItem();
        if (selObj == null) {
            cargarTurnosEnTabla(tableTurnos, base);
            ultimoListado = (base != null) ? base : new ArrayList<>();
            return;
        }

        if (selObj instanceof String) {
            String sel = ((String) selObj).trim();
            if (sel.equalsIgnoreCase("Seleccionar una Mascota")) {
                cargarTurnosEnTabla(tableTurnos, base);
                ultimoListado = (base != null) ? base : new ArrayList<>();
                return;
            }
            if (sel.equalsIgnoreCase("Todos")) {
                List<Peluqueria> all = operarTurno.obtenerTodosLosTurnos();
                cargarTurnosEnTabla(tableTurnos, all);
                ultimoListado = (all != null) ? all : new ArrayList<>();
                return;
            }
            cargarTurnosEnTabla(tableTurnos, base);
            ultimoListado = (base != null) ? base : new ArrayList<>();
            return;
        }

        if (selObj instanceof MascotaItem) {
            Integer idMascota = ((MascotaItem) selObj).getIdMascota();
            List<Peluqueria> filtrados = new ArrayList<>();
            if (base != null && idMascota != null) {
                for (Peluqueria t : base) {
                    if (t != null && t.getMascota() != null && idMascota.equals(t.getMascota().getIdMascota())) {
                        filtrados.add(t);
                    }
                }
            }
            cargarTurnosEnTabla(tableTurnos, filtrados);
            ultimoListado = filtrados;
            return;
        }

        cargarTurnosEnTabla(tableTurnos, base);
        ultimoListado = (base != null) ? base : new ArrayList<>();
    }

    private void aplicarFiltroVeterinario() {
        List<Peluqueria> base = obtenerBaseHistorial();

        Object selObj = jcbVeterinario.getSelectedItem();
        if (selObj == null) {
            cargarTurnosEnTabla(tableTurnos, base);
            ultimoListado = (base != null) ? base : new ArrayList<>();
            return;
        }

        if (selObj instanceof String) {
            String sel = ((String) selObj).trim();
            if (sel.equalsIgnoreCase("Seleccionar un Veterinario")) {
                cargarTurnosEnTabla(tableTurnos, base);
                ultimoListado = (base != null) ? base : new ArrayList<>();
                return;
            }
            if (sel.equalsIgnoreCase("Todos")) {
                List<Peluqueria> all = operarTurno.obtenerTodosLosTurnos();
                cargarTurnosEnTabla(tableTurnos, all);
                ultimoListado = (all != null) ? all : new ArrayList<>();
                return;
            }
            cargarTurnosEnTabla(tableTurnos, base);
            ultimoListado = (base != null) ? base : new ArrayList<>();
            return;
        }

        if (selObj instanceof VeterinarioItem) {
            Integer idVet = ((VeterinarioItem) selObj).getIdUsuario();
            List<Peluqueria> filtrados = new ArrayList<>();
            if (base != null && idVet != null) {
                for (Peluqueria t : base) {
                    if (t != null && t.getVeterinario() != null && idVet.equals(t.getVeterinario().getIdUsuario())) {
                        filtrados.add(t);
                    }
                }
            }
            cargarTurnosEnTabla(tableTurnos, filtrados);
            ultimoListado = filtrados;
            return;
        }

        cargarTurnosEnTabla(tableTurnos, base);
        ultimoListado = (base != null) ? base : new ArrayList<>();
    }

    private List<Peluqueria> obtenerBaseHistorial() {
        // Historial por defecto: completado/cancelado/eliminado
        List<EstadoPeluqueriaEnum> estados = Arrays.asList(
                EstadoPeluqueriaEnum.COMPLETADO,
                EstadoPeluqueriaEnum.CANCELADO,
                EstadoPeluqueriaEnum.ELIMINADO
        );
        return operarTurno.obtenerTurnosPorEstados(estados);
    }

    private LocalDate dateChooserToLocalDate(com.toedter.calendar.JDateChooser chooser) {
        try {
            if (chooser == null || chooser.getDate() == null) {
                return null;
            }
            return chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private void imprimirListadoActual() {
        try {
            if (tableTurnos == null || tableTurnos.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay registros para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String filtrosTxt = buildFiltrosTexto();

            ReporteRequest req = new ReporteRequest()
                    .put("tabla", tableTurnos)
                    .put("filtros", filtrosTxt);

            Object[] options = {"Listado (compacto)", "Detallado", "Cancelar"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Seleccioná el formato de impresión:",
                    "Imprimir Historial de Turnos",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }

            ReporteTipo tipo = (choice == 0)
                    ? ReporteTipo.HISTORIAL_TURNOS_LISTADO
                    : ReporteTipo.HISTORIAL_TURNOS;

            reporteService.generar(tipo, req);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al imprimir", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildFiltrosTexto() {
        StringBuilder sb = new StringBuilder();

        switch (modoFiltro) {
            case POR_FECHA: {
                LocalDate d = dateChooserToLocalDate(jdcFechaDesde);
                LocalDate h = dateChooserToLocalDate(jdcFechaHasta);
                sb.append("Modo: Por fecha").append("\n");
                sb.append("Desde: ").append(d != null ? d.toString() : "-").append("\n");
                sb.append("Hasta: ").append(h != null ? h.toString() : "-");
                break;
            }
            case POR_PACIENTE: {
                sb.append("Modo: Por paciente").append("\n");
                Object o = jcbPaciente.getSelectedItem();
                sb.append("Paciente: ").append(o != null ? String.valueOf(o) : "-");
                break;
            }
            case POR_VETERINARIO: {
                sb.append("Modo: Por veterinario").append("\n");
                Object o = jcbVeterinario.getSelectedItem();
                sb.append("Veterinario: ").append(o != null ? String.valueOf(o) : "-");
                break;
            }
            case POR_ESTADO:
            default: {
                sb.append("Modo: Por estado").append("\n");
                Object o = jcbEstado.getSelectedItem();
                sb.append("Estado: ").append(o != null ? String.valueOf(o) : "-");
                break;
            }
        }

        return sb.toString();
    }

    private void initColumnaMotivoCache() {
        try {
            // La columna existe al iniciar; guardamos referencia para poder reinsertarla cuando haga falta.
            javax.swing.table.TableColumnModel cm = tableTurnos.getColumnModel();
            for (int i = 0; i < cm.getColumnCount(); i++) {
                if (cm.getColumn(i).getModelIndex() == IDX_COL_MOTIVO) {
                    colMotivoCache = cm.getColumn(i);
                    break;
                }
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * La columna "Motivo" solo debe mostrarse cuando en el listado existe al
     * menos un turno CANCELADO.
     */
    private void actualizarVisibilidadColumnaMotivo() {
        boolean hayCancelado = false;

        try {
            javax.swing.table.TableModel model = tableTurnos.getModel();
            for (int r = 0; r < model.getRowCount(); r++) {
                Object v = model.getValueAt(r, IDX_COL_ESTADO);
                if (v != null && String.valueOf(v).trim().equalsIgnoreCase("CANCELADO")) {
                    hayCancelado = true;
                    break;
                }
            }
        } catch (Exception ignore) {
        }

        setMotivoVisible(hayCancelado);
    }

    private void setMotivoVisible(boolean visible) {
        try {
            javax.swing.table.TableColumnModel cm = tableTurnos.getColumnModel();

            // Detectar si la columna está actualmente visible (por modelIndex)
            int viewIdxMotivo = -1;
            for (int i = 0; i < cm.getColumnCount(); i++) {
                if (cm.getColumn(i).getModelIndex() == IDX_COL_MOTIVO) {
                    viewIdxMotivo = i;
                    break;
                }
            }
            boolean actualmenteVisible = viewIdxMotivo >= 0;

            if (visible) {
                if (!actualmenteVisible) {
                    if (colMotivoCache == null) {
                        // Intentar recuperar de la configuración inicial, si todavía existe
                        initColumnaMotivoCache();
                    }
                    if (colMotivoCache != null) {
                        cm.addColumn(colMotivoCache);

                        // addColumn agrega al final; reubicarla detrás de "Estado" si existe
                        int from = cm.getColumnCount() - 1;
                        int idxEstadoView = -1;
                        for (int i = 0; i < cm.getColumnCount(); i++) {
                            if (cm.getColumn(i).getModelIndex() == IDX_COL_ESTADO) {
                                idxEstadoView = i;
                                break;
                            }
                        }
                        int target = (idxEstadoView >= 0) ? Math.min(idxEstadoView + 1, cm.getColumnCount() - 1) : from;
                        if (from != target) {
                            cm.moveColumn(from, target);
                        }
                    }
                }
            } else {
                if (actualmenteVisible) {
                    // Guardar referencia (por si se reconstruye o se vuelve a mostrar)
                    if (colMotivoCache == null) {
                        colMotivoCache = cm.getColumn(viewIdxMotivo);
                    }
                    cm.removeColumn(cm.getColumn(viewIdxMotivo));
                }
            }
        } catch (Exception ignore) {
        }
    }

    private void cargarTurnosEnTabla(JTable tablaTurnos, List<Peluqueria> turnos) {
        DefaultTableModel modelo = (DefaultTableModel) tablaTurnos.getModel();
        modelo.setRowCount(0);
        if (turnos == null) {
            // Si no hay datos, por defecto no mostramos "Motivo".
            setMotivoVisible(false);
            return;
        }

        // Para "Motivo" y "Fecha Evento": último evento del historial del turno.
        Map<Integer, PeluqueriaHistorial> ultimoHistorial = null;
        try {
            List<Integer> ids = new ArrayList<>();
            for (Peluqueria t : turnos) {
                if (t != null && t.getIdTurno() != null) {
                    ids.add(t.getIdTurno());
                }
            }
            ultimoHistorial = turnoHistorialDAO.obtenerUltimoHistorialPorTurnos(ids);
        } catch (Exception ignore) {
        }

        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fechaEvtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Peluqueria turno : turnos) {
            String fecha = (turno.getFecha() != null) ? turno.getFecha().toString() : "Fecha no disponible";
            String hora = (turno.getHora() != null) ? turno.getHora().format(horaFmt) : "--:--";
            String fechaHora = fecha + " " + hora;

            String nombreMascota = (turno.getMascota() != null && turno.getMascota().getNombre() != null)
                    ? turno.getMascota().getNombre()
                    : "Sin mascota";

            String cliente = "Sin cliente";
            try {
                if (turno.getMascota() != null
                        && turno.getMascota().getCliente() != null
                        && turno.getMascota().getCliente().getPersona() != null) {
                    String n = turno.getMascota().getCliente().getPersona().getNombre();
                    String a = turno.getMascota().getCliente().getPersona().getApellido();
                    String full = ((a != null ? a : "") + " " + (n != null ? n : "")).trim();
                    if (!full.isEmpty()) {
                        cliente = full;
                    }
                }
            } catch (Exception ignore) {
            }

            String tipoCita = (turno.getTipoDeCita() != null) ? String.valueOf(turno.getTipoDeCita()) : "";

            String veterinario = "";
            try {
                if (turno.getVeterinario() != null) {
                    if (turno.getVeterinario().getPersona() != null) {
                        String a = turno.getVeterinario().getPersona().getApellido();
                        String n = turno.getVeterinario().getPersona().getNombre();
                        veterinario = ((a != null ? a : "") + " " + (n != null ? n : "")).trim();
                    }
                    if (veterinario == null || veterinario.trim().isEmpty()) {
                        veterinario = turno.getVeterinario().getNombreUsuario();
                    }
                }
            } catch (Exception ignore) {
            }

            String precio = (turno.getPresupuesto() != null) ? String.valueOf(turno.getPresupuesto()) : "";

            String estado = (turno.getEstado() != null) ? String.valueOf(turno.getEstado()) : "";

            String motivo = "";
            String fechaEvento = "";
            try {
                if (turno != null && turno.getIdTurno() != null && ultimoHistorial != null) {
                    PeluqueriaHistorial h = ultimoHistorial.get(turno.getIdTurno());
                    if (h != null) {
                        if (h.getMotivo() != null) {
                            motivo = h.getMotivo().trim();
                        }
                        if (h.getFechaEvento() != null) {
                            fechaEvento = h.getFechaEvento().format(fechaEvtFmt);
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            Object[] fila = {
                false,
                turno.getIdTurno(),
                fechaHora,
                nombreMascota,
                cliente,
                tipoCita,
                veterinario,
                precio,
                estado,
                motivo,
                fechaEvento
            };
            modelo.addRow(fila);
        }

        // Mostrar u ocultar la columna \"Motivo\" según si hay turnos CANCELADOS.
        actualizarVisibilidadColumnaMotivo();
    }

    private void wireTablaSeleccion() {
        // En historial, el checkbox "Seleccionar" debe comportarse como radio:
        // solo una fila seleccionable a la vez.
        try {
            tableTurnos.getModel().addTableModelListener(e -> {
                if (actualizandoSeleccionTabla) {
                    return;
                }
                int col = e.getColumn();
                // Columna 0 = "Seleccionar"
                if (col == 0) {
                    int row = e.getFirstRow();
                    marcarSoloFilaSeleccionada(row);
                }
            });
        } catch (Exception ignore) {
        }
    }

    private void marcarSoloFilaSeleccionada(int fila) {
        if (fila < 0) {
            return;
        }
        try {
            DefaultTableModel model = (DefaultTableModel) tableTurnos.getModel();
            Object val = model.getValueAt(fila, 0);
            boolean selected = (val instanceof Boolean) ? (Boolean) val : false;

            actualizandoSeleccionTabla = true;
            // Desmarcar todos
            for (int i = 0; i < model.getRowCount(); i++) {
                if (i != fila) {
                    model.setValueAt(false, i, 0);
                }
            }
            // Mantener seleccionado si el usuario lo marcó.
            model.setValueAt(selected, fila, 0);
        } catch (Exception ignore) {
        } finally {
            actualizandoSeleccionTabla = false;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JComboBox jcbEstado;
    private javax.swing.JComboBox jcbPaciente;
    private javax.swing.JComboBox jcbVeterinario;
    private javax.swing.JCheckBox jckbPorEstado;
    private javax.swing.JCheckBox jckbPorFecha;
    private javax.swing.JCheckBox jckbPorPaciente;
    private javax.swing.JCheckBox jckbPorVeterinario;
    private com.toedter.calendar.JDateChooser jdcFechaDesde;
    private com.toedter.calendar.JDateChooser jdcFechaHasta;
    private javax.swing.JPanel jpDatosFiltrados;
    private javax.swing.JPanel jpPorEstado;
    private javax.swing.JPanel jpPorFecha;
    private javax.swing.JPanel jpPorPaciente;
    private javax.swing.JPanel jpPorVeterinario;
    private javax.swing.JSeparator jsEstado;
    private javax.swing.JSeparator jsFecha;
    private javax.swing.JSeparator jsPaciente;
    private javax.swing.JSeparator jsVeterinario;
    private javax.swing.JLabel lbDesde;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbFiltrarPorEstado;
    private javax.swing.JLabel lbFiltrarPorFecha;
    private javax.swing.JLabel lbFiltrarPorPaciente;
    private javax.swing.JLabel lbFiltrarPorVeterinario;
    private javax.swing.JLabel lbHasta;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbTurnosRegistrados;
    private javax.swing.JLabel lbVeterinarioAsignado;
    private javax.swing.JPanel panelHadear;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tableTurnos;
    // End of variables declaration//GEN-END:variables
}
