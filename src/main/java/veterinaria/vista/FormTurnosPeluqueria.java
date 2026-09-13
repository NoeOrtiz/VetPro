
package veterinaria.vista;

import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.MascotaControlador;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.PeluqueriaControlador;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.Peluqueria;
import veterinaria.entidad.EstadoPeluqueriaEnum;
import veterinaria.util.ui.HistoriaEventoAbrible;
import veterinaria.entidad.TipoCitaPeluqueria;
import veterinaria.util.ManejoTablas;
import veterinaria.vista.application.Application;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.ClienteMascotaSelector;
import veterinaria.servicio.ConfiguracionService;
import veterinaria.servicio.TipoCitaPeluqueriaService;
import veterinaria.util.Constantes.ResultadoEliminarPeluqueria;
import veterinaria.servicio.AgendaSlotService;
import veterinaria.util.PeluqueriaEstados;
import veterinaria.vista.componentes.WhatsAppMensajeDialog;

public class FormTurnosPeluqueria extends javax.swing.JPanel implements HistoriaEventoAbrible {

    private final MascotaControlador operarMascota = new MascotaControlador();
    private final ClienteControlador clienteControlador = new ClienteControlador();
    private ClienteMascotaSelector clienteMascotaSelector;
    private final PeluqueriaControlador operarTurno = new PeluqueriaControlador();
    private final ConfiguracionService configService = new ConfiguracionService();
    private final ManejoTablas operarTabla = new ManejoTablas();
    private Peluqueria turno = new Peluqueria();
    private Mascota mascota = null;
    private Mascota mascotaSeleccionada = null;
    private List<Usuario> listaVeterinarios = new ArrayList<>();
    private final List<AgendaSlot> slotsComboTurno = new ArrayList<>();
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AgendaSlotService slotService = new AgendaSlotService();
    private javax.swing.Timer relojTimer;
    private boolean actualizandoSeleccionTabla = false;

    public FormTurnosPeluqueria() {
        initComponents();
        lbVeterinarioAsignado.setText("Veterinario: *");
        inicializarTiposCitaPeluqueria();
        wireTurnoHoraLabel();
        PermisoUI.aplicar(this);
        configurarSelectorFecha();
        wireComboTurnosRefresh();
        inicializarRelojYFechaBuscar();
        cargarCombosBoxs();
        cargarComboVeterinarios();
        wireValidacionesUX();
        actualizarEstadoBotonGuardar();
        wireTablaSeleccion();
        btnBuscar.addActionListener(e -> cargarTurnosSegunRol());

        cargarTurnosSegunRol();
        try {
            tableTurnos.clearSelection();
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        btnAtenderTurno.setVisible(false);
        btnConfirmarTurno.setVisible(false);
        btnCancelarTurno.setVisible(false);

        try {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    tableTurnos.clearSelection();
                } catch (Exception ex) {
                    veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                }
                actualizarEstadoBotonesAccion();
            });
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        actualizarEstadoBotonesAccion();
        jpDatosTurno.setVisible(false);
        btnEditar.setVisible(false);
        btnEliminar.setVisible(false);        
    }

    /**
     * Abre un turno de peluquería puntual desde Historia Clínica (modo lectura).
     */
    @Override
    public void abrirDetallePorId(Integer refId) {
        if (refId == null) return;

        // Asegurar listado visible
        jpListaTurnos.setVisible(true);
        jpDatosTurno.setVisible(false);

        Integer viewRow = buscarFilaPorId(refId);
        if (viewRow == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el turno de peluquería #" + refId + " en la lista.",
                    "No encontrado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int modelRow = tableTurnos.convertRowIndexToModel(viewRow);
            int colSel = getColSeleccionar();
            if (colSel >= 0) {
                tableTurnos.getModel().setValueAt(true, modelRow, colSel);
            }
            tableTurnos.setRowSelectionInterval(viewRow, viewRow);
            tableTurnos.scrollRectToVisible(tableTurnos.getCellRect(viewRow, 0, true));
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        if (editarTurnoSeleccionado()) {
            jpListaTurnos.setVisible(false);
            jpDatosTurno.setVisible(true);

            // Bloquear acciones de edición
            btnGuardar.setEnabled(false);
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
            btnConfirmarTurno.setVisible(false);
            btnCancelarTurno.setVisible(false);
            btnAtenderTurno.setVisible(false);

            bloquearLecturaFormulario();
        }
    }

    private Integer buscarFilaPorId(Integer idTurno) {
        try {
            for (int viewRow = 0; viewRow < tableTurnos.getRowCount(); viewRow++) {
                int modelRow = tableTurnos.convertRowIndexToModel(viewRow);
                Object val = tableTurnos.getModel().getValueAt(modelRow, 1); // ID col 1
                if (val != null && idTurno.toString().equals(val.toString())) {
                    return viewRow;
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
        return null;
    }

    private void bloquearLecturaFormulario() {
        try {
            jcbClienteDueño.setEnabled(false);
            jcbPaciente.setEnabled(false);
            jdcFecha.setEnabled(false);
            jcbVeterinario.setEnabled(false);
            jcbTurno.setEnabled(false);
            jcbTipoDeCita.setEnabled(false);
            txtPresupuesto.setEditable(false);
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    private void inicializarRelojYFechaBuscar() {
        try {
            // Set fecha de hoy en el buscador
            LocalDate hoy = LocalDate.now();
            Date hoyDate = Date.from(hoy.atStartOfDay(ZoneId.systemDefault()).toInstant());
            jdcFechaBuscar.setDate(hoyDate);

            // Inicial y timer
            actualizarLabelFechaHora();
            relojTimer = new javax.swing.Timer(1000, e -> actualizarLabelFechaHora());
            relojTimer.setRepeats(true);
            relojTimer.start();
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    private void actualizarLabelFechaHora() {
        try {
            lbFechaYHora.setText(LocalDateTime.now().format(FECHA_HORA_FMT));
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    /**
     * Carga turnos en la tabla según el rol actual:
     *  - Administrador: muestra TODOS los turnos (visibles) del sistema.
     *  - Veterinario / Asistente (operario): muestra solo los turnos del día seleccionado (por defecto hoy).
     */
    private void cargarTurnosSegunRol() {
        try {
            final List<Peluqueria> turnos;

            if (esAdministrador()) {
                // Admin: lista completa del sistema (visibles)
                turnos = operarTurno.obtenerTodosLosTurnos();
            } else {
                // Veterinario / Asistente: del día (por defecto hoy)
                Date d = jdcFechaBuscar.getDate();
                LocalDate fecha = (d == null)
                        ? LocalDate.now()
                        : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                turnos = operarTurno.obtenerTurnosPorFechaYEstados(
                        fecha,
                        Arrays.asList(PeluqueriaEstados.PENDIENTE, PeluqueriaEstados.CONFIRMADO)
                );
            }

            cargarTurnosEnTabla(tableTurnos, turnos);

            try {
                tableTurnos.clearSelection();
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }
            actualizarEstadoBotonesAccion();
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los turnos del día.");
        }
    }

    // Compatibilidad: en varios flujos del formulario se usa el nombre histórico.
    // Mantenerlo evita tocar lógica dispersa y preserva el comportamiento esperado.
    private void buscarTurnosDelDia() {
        cargarTurnosSegunRol();
    }

    private boolean esAdministrador() {
        try {
            Usuario u = Application.getSesionUsuario().getUsuario();
            if (u == null || u.getRol() == null) return false;
            if (u.getRol().getIdRol() != null && u.getRol().getIdRol() == 1) return true;
            return u.getRol().getNombreRol() != null
                    && "Administrador".equalsIgnoreCase(u.getRol().getNombreRol().trim());
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            return false;
        }
    }

    private void wireValidacionesUX() {
        // Habilita/Deshabilita Guardar según campos obligatorios (sin mensajes)
        jcbPaciente.addActionListener(e -> {
            // Mantener la referencia de mascota seleccionada sincronizada
            // (evita persistir una Mascota transient con id=null)
            enPacienteSeleccionado();
            actualizarEstadoBotonGuardar();
        });
        jcbTipoDeCita.addActionListener(e -> actualizarEstadoBotonGuardar());
        jcbTurno.addActionListener(e -> actualizarEstadoBotonGuardar());

        // Fecha (JDateChooser)
        try {
            jdcFecha.getDateEditor().addPropertyChangeListener("date", evt -> actualizarEstadoBotonGuardar());
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        // Veterinario ya tiene listener, pero también agregamos un ActionListener por compatibilidad
        jcbVeterinario.addActionListener(e -> actualizarEstadoBotonGuardar());
    }

    private void wireTurnoHoraLabel() {
        // Muestra la hora real del turno seleccionado (segun configuracion) en el label.
        jcbTurno.addActionListener(e -> actualizarHoraLabel());
    }

    private void actualizarHoraLabel() {
        AgendaSlot s = obtenerSlotSeleccionado();
        if (s == null || s.getHoraInicio() == null) {
            lbHora.setText("Peluqueria: *");
            actualizarEstadoBotonGuardar();
        } else {
            String hi = s.getHoraInicio().format(HORA_FMT);
            String hf = s.getHoraFin() != null ? s.getHoraFin().format(HORA_FMT) : "--:--";
            lbHora.setText("Peluqueria: *  Hora: " + hi + " - " + hf);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelHadear = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        panelBotones = new javax.swing.JPanel();
        lbFechaYHora = new javax.swing.JLabel();
        btnBuscar = new javax.swing.JButton();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        lbBuscar = new javax.swing.JLabel();
        btnAtenderTurno = new javax.swing.JButton();
        btnConfirmarTurno = new javax.swing.JButton();
        btnCancelarTurno = new javax.swing.JButton();
        jdcFechaBuscar = new com.toedter.calendar.JDateChooser();
        jSeparator5 = new javax.swing.JSeparator();
        jpDatosTurno = new javax.swing.JPanel();
        lbPaciente = new javax.swing.JLabel();
        jcbPaciente = new javax.swing.JComboBox<>();
        lbFecha = new javax.swing.JLabel();
        jdcFecha = new com.toedter.calendar.JDateChooser();
        lbHora = new javax.swing.JLabel();
        lbTipoDeCita = new javax.swing.JLabel();
        jcbTipoDeCita = new javax.swing.JComboBox<>();
        lbVeterinarioAsignado = new javax.swing.JLabel();
        txtPresupuesto = new javax.swing.JTextField();
        lbEstado2 = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jcbTurno = new javax.swing.JComboBox<>();
        jcbVeterinario = new javax.swing.JComboBox<>();
        jcbClienteDueño = new javax.swing.JComboBox();
        lbPaciente1 = new javax.swing.JLabel();
        jpListaTurnos = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        scroll = new javax.swing.JScrollPane();
        tableTurnos = new veterinaria.vista.table.AutoTable();

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Estética y Peluqería");

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

        lbFechaYHora.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbFechaYHora.setText("Fecha y Hora");

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

        btnAtenderTurno.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnAtenderTurno.setText("Atender");
        btnAtenderTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAtenderTurnoActionPerformed(evt);
            }
        });

        btnConfirmarTurno.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnConfirmarTurno.setText("Confirmar");
        btnConfirmarTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarTurnoActionPerformed(evt);
            }
        });

        btnCancelarTurno.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCancelarTurno.setText("Cancelar");
        btnCancelarTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarTurnoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBotonesLayout = new javax.swing.GroupLayout(panelBotones);
        panelBotones.setLayout(panelBotonesLayout);
        panelBotonesLayout.setHorizontalGroup(
            panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonesLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lbBuscar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBotonesLayout.createSequentialGroup()
                        .addComponent(jdcFechaBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAtenderTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnConfirmarTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelarTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelBotonesLayout.createSequentialGroup()
                        .addComponent(lbFechaYHora)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelBotonesLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSeparator5)
                    .addContainerGap()))
        );
        panelBotonesLayout.setVerticalGroup(
            panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonesLayout.createSequentialGroup()
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbFechaYHora)
                    .addGroup(panelBotonesLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnNuevo)
                            .addComponent(btnEditar)
                            .addComponent(btnEliminar))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAtenderTurno)
                            .addComponent(btnConfirmarTurno)
                            .addComponent(btnCancelarTurno))
                        .addComponent(btnBuscar))
                    .addComponent(lbBuscar)
                    .addComponent(jdcFechaBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(15, Short.MAX_VALUE))
            .addGroup(panelBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBotonesLayout.createSequentialGroup()
                    .addContainerGap(66, Short.MAX_VALUE)
                    .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );

        lbPaciente.setText("Paciente: *");

        jcbPaciente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar una Mascota" }));

        lbFecha.setText("Fecha: *");

        lbHora.setText("Turno: *");

        lbTipoDeCita.setText("Tipo de Cita: *");

        lbVeterinarioAsignado.setText("Veterinario:");

        lbEstado2.setText("Precio:");

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

        jcbTurno.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione un turno" }));
        jcbTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbTurnoActionPerformed(evt);
            }
        });

        jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione veterinario" }));

        jcbClienteDueño.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar cliente" }));

        lbPaciente1.setText("Cliente: *");

        javax.swing.GroupLayout jpDatosTurnoLayout = new javax.swing.GroupLayout(jpDatosTurno);
        jpDatosTurno.setLayout(jpDatosTurnoLayout);
        jpDatosTurnoLayout.setHorizontalGroup(
            jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosTurnoLayout.createSequentialGroup()
                        .addGap(0, 446, Short.MAX_VALUE)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator4)
                            .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbPaciente1)
                                    .addComponent(jcbClienteDueño, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jcbTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbHora))
                                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                                        .addGap(181, 181, 181)
                                        .addComponent(lbFecha))
                                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbPaciente)
                                            .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                                                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                    .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(lbTipoDeCita, javax.swing.GroupLayout.Alignment.LEADING))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jdcFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosTurnoLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jcbTipoDeCita, javax.swing.GroupLayout.PREFERRED_SIZE, 306, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbVeterinarioAsignado, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lbEstado2, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtPresupuesto, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jpDatosTurnoLayout.setVerticalGroup(
            jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                        .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbVeterinarioAsignado)
                            .addComponent(lbFecha)
                            .addComponent(lbPaciente))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jdcFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                        .addComponent(lbPaciente1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbClienteDueño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                        .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbTipoDeCita)
                            .addComponent(lbHora))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbTurno, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbTipoDeCita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpDatosTurnoLayout.createSequentialGroup()
                        .addComponent(lbEstado2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPresupuesto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpDatosTurnoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar)
                    .addComponent(btnLimpiar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Turnos cargados");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableTurnos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Nº Turno", "Fecha", "Paciente", "Cliente", "Tipo de Cita", "Veterinario", "Precio", "Estado"
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
        tableTurnos.setMinimumSize(new java.awt.Dimension(848, 220));
        tableTurnos.setPreferredSize(new java.awt.Dimension(848, 220));
        tableTurnos.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableTurnos);

        javax.swing.GroupLayout jpListaTurnosLayout = new javax.swing.GroupLayout(jpListaTurnos);
        jpListaTurnos.setLayout(jpListaTurnosLayout);
        jpListaTurnosLayout.setHorizontalGroup(
            jpListaTurnosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaTurnosLayout.createSequentialGroup()
                .addGroup(jpListaTurnosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpListaTurnosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpListaTurnosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpListaTurnosLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addComponent(scroll))
                .addContainerGap())
        );
        jpListaTurnosLayout.setVerticalGroup(
            jpListaTurnosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaTurnosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpListaTurnos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jpDatosTurno, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelHadear, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelBotones, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelHadear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelBotones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatosTurno, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaTurnos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        Application.actualizarEstadoUsuario("CreandoTurnoPeluqueria");
        jpDatosTurno.setVisible(true);
        jdcFecha.setDate(null);
        limpiarTurnosCombo();
        jpListaTurnos.setEnabled(false);
        btnNuevo.setVisible(false);
        btnEditar.setVisible(false);
        btnEliminar.setVisible(false);
        try {
            jcbPaciente.setEnabled(true);
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        Application.actualizarEstadoUsuario("EditandoTurnoPeluqueria");
        jpDatosTurno.setVisible(true);
        // Regla: el estado se muestra pero NO se puede modificar desde "Editar".
        if (editarTurnoSeleccionado()) {
            try {
                jcbPaciente.setEnabled(false);
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }
            btnNuevo.setVisible(false);
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        Application.actualizarEstadoUsuario("BotonEliminandoTurno");
        boolean retornar = false;
        int respuesta = JOptionPane.showConfirmDialog(
                this, "¿Estás seguro de que deseas cancelar este turno?",
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (respuesta == JOptionPane.YES_OPTION) {
            Application.actualizarEstadoUsuario("Eliminando");
            if (eliminarTurnoSeleccionado()) {
                retornar = true;
            }
            limpiarCamposFormulario();
            buscarTurnosDelDia();
            try {
                tableTurnos.clearSelection();
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }
            actualizarEstadoBotonesAccion();
            jpDatosTurno.setVisible(false);
            buscarTurnosDelDia();
        } else {
            Application.actualizarEstadoUsuario("Eliminación cancelada");
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarCamposFormulario();
        jpDatosTurno.setVisible(false);
        jpListaTurnos.setVisible(true);
        try {
            tableTurnos.clearSelection();
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
        // Regla: sin selección, solo Nuevo habilitado
        actualizarEstadoBotonesAccion();
        // Asegurar que el paciente quede habilitado para una nueva acción
        try {
            jcbPaciente.setEnabled(true);
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        int opcion = javax.swing.JOptionPane.showConfirmDialog(this, "¿Guardar el turno?", "Confirmación", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (opcion != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        if (validarDatosTurno()) {
            if (persistirTurno()) {
                // Al finalizar la carga del turno, preparar mensaje para WhatsApp.
                // (Funcionalidad que se usaba antes y se perdió con el rollback.)
                try {
                    mostrarDialogoWhatsAppFinalizarCarga(turno);
                } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                }
                limpiarCamposFormulario();
                jpDatosTurno.setVisible(false);
                jpListaTurnos.setVisible(true);
                try {
                    tableTurnos.clearSelection();
                } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                }
                actualizarEstadoBotonesAccion();
                buscarTurnosDelDia();
            }
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void jcbTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbTurnoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jcbTurnoActionPerformed

    private void btnAtenderTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAtenderTurnoActionPerformed
        Long idTurno = obtenerIdTurnoSeleccionado();
        if (idTurno == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un turno.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Regla: solo se puede atender si está Confirmado.
        EstadoPeluqueriaEnum estadoActual = obtenerEstadoTurnoSeleccionadoEnTabla();
        if (estadoActual != EstadoPeluqueriaEnum.CONFIRMADO) {
            JOptionPane.showMessageDialog(this, "Solo se pueden atender turnos confirmados.", "No permitido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
                "¿Marcar el turno seleccionado como ATENDIDO?",
                "Confirmación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) {
            return;
        }


        Integer idUsuario = (Application.getSesionUsuario() != null && Application.getSesionUsuario().getUsuario() != null)
                ? Application.getSesionUsuario().getUsuario().getIdUsuario()
                : null;

        boolean ok = operarTurno.completarTurno(idTurno, idUsuario);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Peluqueria marcado como Completado.");
            limpiarCamposFormulario();
            jpDatosTurno.setVisible(false);
            jpListaTurnos.setVisible(true);
            buscarTurnosDelDia();
            try { tableTurnos.clearSelection(); } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex); }
            actualizarEstadoBotonesAccion();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo completar el turno. Verifique que esté Confirmado.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAtenderTurnoActionPerformed

    private void btnConfirmarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarTurnoActionPerformed
        Long idTurno = obtenerIdTurnoSeleccionado();
        if (idTurno == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un turno.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EstadoPeluqueriaEnum estadoActual = obtenerEstadoTurnoSeleccionadoEnTabla();
        if (estadoActual == null || PeluqueriaEstados.esNoEditable(estadoActual) || estadoActual == EstadoPeluqueriaEnum.COMPLETADO) {
            JOptionPane.showMessageDialog(this, "No se puede confirmar un turno en estado: " + estadoActual, "No permitido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
                "¿Confirmar el turno y preparar el mensaje para WhatsApp?",
                "Confirmación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) {
            return;
        }


        boolean ok = operarTurno.confirmarTurno(idTurno);
        if (ok) {
            mostrarDialogoWhatsAppConfirmacion(idTurno);
            buscarTurnosDelDia();
            actualizarEstadoBotonesAccion();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo confirmar el turno.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnConfirmarTurnoActionPerformed

    private void btnCancelarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarTurnoActionPerformed
        Long idTurno = obtenerIdTurnoSeleccionado();
        if (idTurno == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un turno.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EstadoPeluqueriaEnum estadoActual = obtenerEstadoTurnoSeleccionadoEnTabla();
        if (estadoActual == EstadoPeluqueriaEnum.ELIMINADO) {
            JOptionPane.showMessageDialog(this, "No se puede cancelar un turno Eliminado.", "No permitido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this, "¿Cancelar el turno seleccionado?", "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) {
            return;
        }

        String motivo = "";
        try {
            String input = JOptionPane.showInputDialog(this, "Motivo (opcional):");
            if (input != null) {
                motivo = input.trim();
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        Integer idUsuario = (Application.getSesionUsuario() != null && Application.getSesionUsuario().getUsuario() != null)
                ? Application.getSesionUsuario().getUsuario().getIdUsuario()
                : null;

        boolean ok = operarTurno.cancelarTurno(idTurno, motivo, idUsuario);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Peluqueria cancelado.");
            buscarTurnosDelDia();
            actualizarEstadoBotonesAccion();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo cancelar el turno.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnCancelarTurnoActionPerformed

    /**
     * Aplica restricciones al selector de fecha (JDateChooser): - No permite
     * fechas pasadas. - Valida que solo se puedan usar los días habilitados en
     * configuración. - Cuando la fecha es válida, carga el combo de turnos
     * (mañana/tarde) según configuración.
     */
    private void configurarSelectorFecha() {
        // 1) No permitir seleccionar fechas pasadas
        jdcFecha.setMinSelectableDate(new Date());

        // 2) Validar día habilitado + cargar turnos
        jdcFecha.getDateEditor().addPropertyChangeListener("date", evt -> {
            Date d = (Date) evt.getNewValue();
            if (d == null) {
                limpiarTurnosCombo();
                return;
            }
            LocalDate fecha = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            // Bloquear pasado (por si el minSelectableDate no aplicara en algún caso)
            if (fecha.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "No se puede seleccionar un día pasado.");
                jdcFecha.setDate(null);
                limpiarTurnosCombo();
                return;
            }

            // Validar que el día esté habilitado
            if (!esDiaHabilitadoPeluqueria(fecha)) {
                JOptionPane.showMessageDialog(
                        this,
                        "La fecha seleccionada no está habilitada para Peluquería.\n"
                        + "Seleccione otro día (según configuración).",
                        "Día no habilitado",
                        JOptionPane.WARNING_MESSAGE
                );
                jdcFecha.setDate(null);
                limpiarTurnosCombo();
                return;
            }

            // Si es válida, cargar combo de turnos
            cargarTurnosComboParaFecha(fecha);
        });
    }

    /**
     * Refresca el combo de turnos al abrir el desplegable para evitar mostrar horarios
     * que pudieron haber sido reservados por otros módulos (Hospitalización/Laboratorio).
     *
     * NOTA: No toca el archivo .form. Se limita a recargar el modelo del combo en memoria.
     */
    private void wireComboTurnosRefresh() {
        try {
            jcbTurno.addPopupMenuListener(new PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    try {
                        // Requiere fecha + veterinario
                        if (jdcFecha.getDate() == null) {
                            return;
                        }
                        LocalDate fecha = jdcFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        Usuario vet = obtenerVeterinarioSeleccionado();
                        if (fecha == null || vet == null) {
                            return;
                        }

                        // Preservar selección actual (si existe)
                        AgendaSlot sel = obtenerSlotSeleccionado();
                        Long idSel = (sel != null ? sel.getIdSlot() : null);

                        // Si estamos editando un turno existente, incluir el slot actual aunque esté reservado
                        boolean editando = (turno != null && turno.getIdTurno() != null);
                        Long idActual = null;
                        try {
                            idActual = (editando && turno.getSlot() != null) ? turno.getSlot().getIdSlot() : null;
                        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                        }

                        if (editando) {
                            cargarTurnosComboParaEdicion(fecha, vet, idActual);
                            // En edición, el método ya preselecciona el actual; si el usuario había elegido otro,
                            // intentamos restaurarlo si sigue disponible.
                            if (idSel != null && (idActual == null || !idSel.equals(idActual))) {
                                seleccionarSlotEnCombo(idSel);
                            }
                        } else {
                            cargarTurnosComboParaFecha(fecha);
                            if (idSel != null) {
                                seleccionarSlotEnCombo(idSel);
                            }
                        }

                    } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    private void limpiarTurnosCombo() {
        slotsComboTurno.clear();
        jcbTurno.setModel(new DefaultComboBoxModel<>(new String[]{"Seleccione un turno"}));
        lbHora.setText("Peluqueria: *");
        actualizarEstadoBotonGuardar();
    }

    private AgendaSlot obtenerSlotSeleccionado() {
        int idx = jcbTurno.getSelectedIndex();
        if (idx <= 0) {
            return null;
        }
        int mapIndex = idx - 1;
        if (mapIndex < 0 || mapIndex >= slotsComboTurno.size()) {
            return null;
        }
        return slotsComboTurno.get(mapIndex);
    }

    private void seleccionarSlotEnCombo(Long idSlot) {
        if (idSlot == null) {
            return;
        }
        for (int i = 0; i < slotsComboTurno.size(); i++) {
            AgendaSlot s = slotsComboTurno.get(i);
            if (s != null && idSlot.equals(s.getIdSlot())) {
                jcbTurno.setSelectedIndex(i + 1);
                return;
            }
        }
    }

    private boolean esDiaHabilitadoPeluqueria(LocalDate fecha) {
        String diasCsv = configService.getString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_PELUQUERIA,
                ConfiguracionService.DEFAULT_DIAS_HABILITADOS_PELUQUERIA
        );
        int dayValue = fecha.getDayOfWeek().getValue(); // 1=Lunes ... 7=Domingo
        if (diasCsv == null || diasCsv.trim().isEmpty()) {
            return false;
        }
        String[] parts = diasCsv.split(",");
        for (String p : parts) {
            try {
                if (Integer.parseInt(p.trim()) == dayValue) {
                    return true;
                }
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }
        }
        return false;
    }

    /**
     * Carga el combo de turnos con el formato: - Mañana - Peluqueria 1, Mañana -
     * Peluqueria 2, ... - Tarde - Peluqueria 1, Tarde - Peluqueria 2, ...
     *
     * Además, asigna a cada ítem una hora real (LocalTime) según: horario
     * desde/hasta + duración del turno.
     */
    private void cargarTurnosComboParaFecha(LocalDate fecha) {
        limpiarTurnosCombo();
        if (fecha == null) {
            return;
        }

        if (!esDiaHabilitadoPeluqueria(fecha)) {
            // No cargar horarios en dias no habilitados
            return;
        }

        Usuario vet = obtenerVeterinarioSeleccionado();
        if (vet == null) {
            return;
        }

        List<AgendaSlot> libres = slotService.listarLibres(fecha, vet);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione un turno");

        for (AgendaSlot s : libres) {
            if (s == null) {
                continue;
            }
            String hi = s.getHoraInicio() != null ? s.getHoraInicio().format(HORA_FMT) : "--:--";
            String hf = s.getHoraFin() != null ? s.getHoraFin().format(HORA_FMT) : "--:--";
            model.addElement(hi + " - " + hf);
            slotsComboTurno.add(s);
        }

        jcbTurno.setModel(model);
        jcbTurno.setSelectedIndex(0);
        actualizarHoraLabel();
        actualizarEstadoBotonGuardar();
    }

    private void cargarTurnosComboParaEdicion(LocalDate fecha, Usuario veterinario, Long idSlotActual) {
        limpiarTurnosCombo();
        if (fecha == null || veterinario == null) {
            return;
        }
        if (!esDiaHabilitadoPeluqueria(fecha)) {
            return;
        }

        List<AgendaSlot> slots = slotService.listarParaEdicion(fecha, veterinario, idSlotActual);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione un turno");

        int selectIndex = 0;
        for (AgendaSlot s : slots) {
            if (s == null) {
                continue;
            }
            String hi = s.getHoraInicio() != null ? s.getHoraInicio().format(HORA_FMT) : "--:--";
            String hf = s.getHoraFin() != null ? s.getHoraFin().format(HORA_FMT) : "--:--";
            String extra = (s.getEstadoSlot() != null && s.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE
                    && (idSlotActual == null || !idSlotActual.equals(s.getIdSlot()))) ? " (No disponible)" : "";
            if (s.getEstadoSlot() != null && s.getEstadoSlot() != AgendaSlot.EstadoSlot.LIBRE
                    && idSlotActual != null && idSlotActual.equals(s.getIdSlot())) {
                extra = " (Actual)";
            }
            model.addElement(hi + " - " + hf + extra);
            slotsComboTurno.add(s);

            // Preseleccionar el slot actual al editar, igual que en otros formularios.
            if (idSlotActual != null && idSlotActual.equals(s.getIdSlot())) {
                selectIndex = model.getSize() - 1; // índice real del último elemento agregado
            }
        }

        jcbTurno.setModel(model);
        jcbTurno.setSelectedIndex(selectIndex);
        actualizarHoraLabel();
        actualizarEstadoBotonGuardar();
    }

    private void cargarCombosBoxs() {
        // Unificado: mismo patrón que FormHospitalizaciones (Cliente -> Mascota)
        if (clienteMascotaSelector == null) {
            clienteMascotaSelector = new ClienteMascotaSelector(
                    jcbClienteDueño,
                    jcbPaciente,
                    clienteControlador,
                    operarMascota,
                    (m) -> {
                        mascotaSeleccionada = m;
                        // variable legacy (evita transient al persistir)
                        mascota = m;
                        try { actualizarEstadoBotonGuardar(); } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex); }
                    }
            );
            clienteMascotaSelector.init();
        } else {
            clienteMascotaSelector.reset();
        }
    }

    private void cargarComboVeterinarios() {
        // Carga usuarios con rol "Veterinario" desde la base de datos y los muestra en el combo.
        // Importante: la consulta debe traer Persona/Rol en la misma sesión para evitar LazyInitialization.
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione veterinario");

        try {
            listaVeterinarios = new veterinaria.persistencia.UsuarioDAO().obtenerUsuariosVeterinarios();
            for (Usuario u : listaVeterinarios) {
                String label = nombreCompletoUsuario(u);
                if (label != null && !label.isBlank()) {
                    model.addElement(label);
                }
            }
        } catch (Exception ex) {
            // En UI preferimos degradar con un placeholder antes que romper el formulario.
            Logger.getLogger(FormTurnosPeluqueria.class.getName()).log(Level.SEVERE, null, ex);
        }

        jcbVeterinario.setModel(model);
        jcbVeterinario.setSelectedIndex(0);
        preseleccionarVeterinarioLogeado();
        btnGuardar.setEnabled(false);

        jcbVeterinario.addItemListener(new java.awt.event.ItemListener() {
            @Override
            public void itemStateChanged(java.awt.event.ItemEvent e) {
                if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                    btnGuardar.setEnabled(jcbVeterinario.getSelectedIndex() > 0);
                    // Al cambiar veterinario, recargar los slots disponibles para la fecha seleccionada
                    try {
                        LocalDate f = (jdcFecha.getDate() != null)
                                ? jdcFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                : null;
                        cargarTurnosComboParaFecha(f);
                    } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                    }
                }
            }
        });

    }

    private Usuario obtenerVeterinarioSeleccionado() {
        int idx = jcbVeterinario.getSelectedIndex();
        if (idx <= 0) {
            return null;
        }
        int mapIndex = idx - 1;
        if (mapIndex < 0 || mapIndex >= listaVeterinarios.size()) {
            return null;
        }
        return listaVeterinarios.get(mapIndex);
    }

    private String nombreCompletoUsuario(Usuario u) {
        if (u == null) {
            return "";
        }
        try {
            if (u.getPersona() != null) {
                String nombre = (u.getPersona().getNombre() != null) ? u.getPersona().getNombre().trim() : "";
                String apellido = (u.getPersona().getApellido() != null) ? u.getPersona().getApellido().trim() : "";
                String full = (nombre + " " + apellido).trim();
                if (!full.isBlank()) {
                    return full;
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
        // Fallback
        return (u.getNombreUsuario() != null) ? u.getNombreUsuario() : "";
    }

    private void seleccionarVeterinarioEnCombo(String nombreGuardado) {
        if (nombreGuardado == null || nombreGuardado.trim().isEmpty()) {
            jcbVeterinario.setSelectedIndex(0);
            btnGuardar.setEnabled(false);

            jcbVeterinario.addItemListener(new java.awt.event.ItemListener() {
                @Override
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                        btnGuardar.setEnabled(jcbVeterinario.getSelectedIndex() > 0);
                    }
                }
            });

            return;
        }
        String target = nombreGuardado.trim();
        for (int i = 1; i < jcbVeterinario.getItemCount(); i++) {
            String item = jcbVeterinario.getItemAt(i);
            if (item != null && item.trim().equalsIgnoreCase(target)) {
                jcbVeterinario.setSelectedIndex(i);
                return;
            }
        }
        // Si no se encuentra, deja placeholder
        jcbVeterinario.setSelectedIndex(0);
        btnGuardar.setEnabled(false);

        jcbVeterinario.addItemListener(new java.awt.event.ItemListener() {
            @Override
            public void itemStateChanged(java.awt.event.ItemEvent e) {
                if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                    btnGuardar.setEnabled(jcbVeterinario.getSelectedIndex() > 0);
                }
            }
        });

    }

    private void enPacienteSeleccionado() {
        Object pacienteSelec = jcbPaciente.getSelectedItem();
        if (pacienteSelec instanceof Mascota) {
            mascotaSeleccionada = (Mascota) pacienteSelec;
            // variable legacy usada en persistirTurno()/validaciones
            mascota = mascotaSeleccionada;
        } else {
            mascotaSeleccionada = null;
            mascota = null;
        }
    }

    private void cargarTurnosEnTabla(JTable tablaTurnos, List<Peluqueria> turnos) {
        DefaultTableModel modelo = (DefaultTableModel) tablaTurnos.getModel();
        modelo.setRowCount(0);

        if (turnos == null) {
            return;
        }

        for (Peluqueria turno : turnos) {
            String fecha = (turno.getFecha() != null) ? turno.getFecha().toString() : "Fecha no disponible";
            String hora = (turno.getHora() != null) ? turno.getHora().toString() : "Hora no disponible";
            String fechaHora = fecha + " a las " + hora;

            String nombreMascota = (turno.getMascota() != null) ? turno.getMascota().getNombre() : "Sin mascota";

            String cliente = "Sin cliente";
            if (turno.getMascota() != null && turno.getMascota().getCliente() != null && turno.getMascota().getCliente().getPersona() != null) {
                cliente = turno.getMascota().getCliente().getPersona().getNombre() + " " + turno.getMascota().getCliente().getPersona().getApellido();
            }

            Object[] fila = {
                false,
                turno.getIdTurno(),
                fechaHora,
                nombreMascota,
                cliente,
                turno.getTipoDeCita(),
                turno.getUsuarioAtiende(),
                turno.getPresupuesto(),
                turno.getEstado()
            };
            modelo.addRow(fila);
        }
    }

    private void limpiarCamposFormulario() {
        if (clienteMascotaSelector != null) {
            clienteMascotaSelector.reset();
        } else {
            jcbPaciente.removeAllItems();
        }
        jdcFecha.setDate(null);
        limpiarTurnosCombo();
        jcbTipoDeCita.setSelectedIndex(0);
        txtPresupuesto.setText("");
        mascotaSeleccionada = null;
        mascota = null;
        cargarCombosBoxs();
        cargarComboVeterinarios();
        wireValidacionesUX();
        actualizarEstadoBotonGuardar();
        try {
            jcbPaciente.setEnabled(true);
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

    }

    private Long obtenerIdTurnoSeleccionado() {
        try {
            int selectedRow = operarTabla.comprobarElementoSeleccionado(tableTurnos);
            if (selectedRow == -1) {
                // fallback: selección visual
                selectedRow = tableTurnos.getSelectedRow();
            }
            if (selectedRow == -1) {
                return null;
            }
            Object idObj = tableTurnos.getValueAt(selectedRow, 1);
            if (idObj == null) return null;
            return (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.valueOf(String.valueOf(idObj));
        } catch (Exception e) {
            return null;
        }
    }

    private EstadoPeluqueriaEnum obtenerEstadoTurnoSeleccionadoEnTabla() {
        try {
            int selectedRow = operarTabla.comprobarElementoSeleccionado(tableTurnos);
            if (selectedRow == -1) {
                selectedRow = tableTurnos.getSelectedRow();
            }
            if (selectedRow == -1) {
                return null;
            }
            Object estadoObj = tableTurnos.getValueAt(selectedRow, 8);
            if (estadoObj == null) return null;
            if (estadoObj instanceof EstadoPeluqueriaEnum) {
                return (EstadoPeluqueriaEnum) estadoObj;
            }
            return PeluqueriaEstados.parse(String.valueOf(estadoObj));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean editarTurnoSeleccionado() {
        boolean bRetorno = false;
        int selectedRow = operarTabla.comprobarElementoSeleccionado(tableTurnos);

        if (selectedRow != -1) {
            Object idObj = tableTurnos.getValueAt(selectedRow, 1);
            Long turnoSelec = (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.valueOf(String.valueOf(idObj));
            turno = operarTurno.buscarTurnoPorId(turnoSelec);
            if (turno != null && (turno.getEstado() == EstadoPeluqueriaEnum.CANCELADO || turno.getEstado() == EstadoPeluqueriaEnum.ELIMINADO)) {
                JOptionPane.showMessageDialog(this,
                        "Los turnos Cancelados o Eliminados no pueden ser editados.",
                        "No permitido",
                        JOptionPane.WARNING_MESSAGE);
                jpDatosTurno.setVisible(false);
                jpListaTurnos.setVisible(true);
                return false;
            }
            try {
                Mascota m = turno.getMascota();
                if (clienteMascotaSelector != null && m != null && m.getCliente() != null) {
                    clienteMascotaSelector.seleccionarPorIds(m.getCliente().getIdCliente(), m.getIdMascota());
                }
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }

            Date fecha = Date.from(turno.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant());
            jdcFecha.setDate(fecha);
            Usuario vet = turno.getVeterinario();
            if (vet != null) {
                seleccionarVeterinarioEnCombo(nombreCompletoUsuario(vet));
                cargarTurnosComboParaEdicion(turno.getFecha(), vet, turno.getSlot() != null ? turno.getSlot().getIdSlot() : null);
                seleccionarSlotEnCombo(turno.getSlot() != null ? turno.getSlot().getIdSlot() : null);
            } else {
                cargarTurnosComboParaFecha(turno.getFecha());
            }
            seleccionarTipoCitaEnCombo(turno.getTipoDeCita());
            String presupuesto = (String) tableTurnos.getValueAt(selectedRow, 7);
            txtPresupuesto.setText(presupuesto);
            bRetorno = true;
        } else {
            JOptionPane.showMessageDialog(null, "¡Debe seleccionar un turno a editar!", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;
    }

    private boolean eliminarTurnoSeleccionado() {

        int selectedRow = operarTabla.comprobarElementoSeleccionado(tableTurnos);

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Por favor seleccione un turno para eliminar.",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        Object idObj = tableTurnos.getValueAt(selectedRow, 1);
        Long idTurno = (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.valueOf(String.valueOf(idObj));

        String motivo = "";
        try {
            String input = JOptionPane.showInputDialog(this, "Motivo (opcional):");
            if (input != null) {
                motivo = input.trim();
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
        ResultadoEliminarPeluqueria resultado = operarTurno.eliminarTurno(idTurno, motivo, (Application.getSesionUsuario() != null && Application.getSesionUsuario().getUsuario() != null ? Application.getSesionUsuario().getUsuario().getIdUsuario() : null));

        switch (resultado) {
            case OK -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Peluqueria cancelado con éxito. El horario vuelve a quedar disponible.",
                        "Información",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return true;
            }

            case ESTADO_NO_PERMITIDO -> {
                JOptionPane.showMessageDialog(
                        this,
                        "No se puede cancelar/eliminar el turno porque ya se encuentra cerrado o atendido.",
                        "Operación no permitida",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            case NO_EXISTE -> {
                JOptionPane.showMessageDialog(
                        this,
                        "El turno seleccionado ya no existe.",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            default -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Ocurrió un error al eliminar el turno.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }

        return false;
    }

    private boolean camposObligatoriosCompletos() {
        try {
            if (jcbPaciente.getSelectedIndex() == 0) {
                return false;
            }
            if (jcbTipoDeCita.getSelectedIndex() == 0) {
                return false;
            }
            if (jcbVeterinario.getSelectedIndex() == 0) {
                return false;
            }
            if (mascota == null) {
                return false;
            }
            if (jdcFecha.getDate() == null) {
                return false;
            }
            if (jcbTurno.getSelectedIndex() <= 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void actualizarEstadoBotonGuardar() {
        btnGuardar.setEnabled(camposObligatoriosCompletos());
    }

    private void actualizarEstadoBotonesAccion() {
        try {
            if (jpDatosTurno != null && jpDatosTurno.isVisible()) {
                return;
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        boolean haySeleccion = false;
        try {
            haySeleccion = tableTurnos != null && tableTurnos.getSelectedRow() != -1;
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        EstadoPeluqueriaEnum estadoSel = haySeleccion ? obtenerEstadoTurnoSeleccionadoEnTabla() : null;
        boolean noEditable = PeluqueriaEstados.esNoEditable(estadoSel);
        boolean confirmado = estadoSel == EstadoPeluqueriaEnum.CONFIRMADO;
        boolean eliminado = estadoSel == EstadoPeluqueriaEnum.ELIMINADO;
        boolean completado = estadoSel == EstadoPeluqueriaEnum.COMPLETADO;
        btnNuevo.setVisible(false);
        btnEditar.setVisible(false);
        btnEliminar.setVisible(false);
        btnConfirmarTurno.setVisible(false);
        btnCancelarTurno.setVisible(false);
        btnAtenderTurno.setVisible(false);

        if (!haySeleccion) {
            btnNuevo.setVisible(true);
        } else {
            if (!noEditable) {
                btnEditar.setVisible(true);
            }
            btnEliminar.setVisible(true);

            boolean pendiente = estadoSel == EstadoPeluqueriaEnum.PENDIENTE;
            if (!noEditable && !confirmado && !completado && pendiente) {
                btnConfirmarTurno.setVisible(true);
            }

            if (!eliminado && !completado) {
                btnCancelarTurno.setVisible(true);
            }

            if (confirmado) {
                btnAtenderTurno.setVisible(true);
            }
        }

        try {
            panelBotones.revalidate();
            panelBotones.repaint();
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
}

    private void wireTablaSeleccion() {
        try {
            tableTurnos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            tableTurnos.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    actualizarEstadoBotonesAccion();
                }
            });

            tableTurnos.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int fila = tableTurnos.rowAtPoint(e.getPoint());
                    if (fila >= 0) {
                        marcarSoloFilaSeleccionada(fila);
                    }
                }
            });

            javax.swing.table.TableModel model = tableTurnos.getModel();
            model.addTableModelListener(evt -> {
                try {
                    if (actualizandoSeleccionTabla) {
                        return;
                    }
                    int colSel = getColSeleccionar();
                    if (colSel == -1) {
                        return;
                    }

                    if (evt.getColumn() == colSel && evt.getFirstRow() >= 0) {
                        Object v = model.getValueAt(evt.getFirstRow(), colSel);
                        if (Boolean.TRUE.equals(v)) {
                            marcarSoloFilaSeleccionada(evt.getFirstRow());
                        } else {
                            actualizarEstadoBotonesAccion();
                        }
                    }
                } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
                }
            });

        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    private int getColSeleccionar() {
        try {
            for (int i = 0; i < tableTurnos.getColumnCount(); i++) {
                String n = tableTurnos.getColumnName(i);
                if (n != null && n.trim().equalsIgnoreCase("Seleccionar")) {
                    return i;
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
        return -1;
    }

    private void marcarSoloFilaSeleccionada(int fila) {
        try {
            if (actualizandoSeleccionTabla) {
                return;
            }
            int colSel = getColSeleccionar();
            if (colSel == -1) {
                colSel = 0;
            }

            actualizandoSeleccionTabla = true;
            try {
                for (int i = 0; i < tableTurnos.getRowCount(); i++) {
                    if (i != fila) {
                        tableTurnos.setValueAt(Boolean.FALSE, i, colSel);
                    }
                }
                Object actual = tableTurnos.getValueAt(fila, colSel);
                if (!Boolean.TRUE.equals(actual)) {
                    tableTurnos.setValueAt(Boolean.TRUE, fila, colSel);
                }
            } finally {
                actualizandoSeleccionTabla = false;
            }

            try {
                tableTurnos.setRowSelectionInterval(fila, fila);
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }

            actualizarEstadoBotonesAccion();
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    private void preseleccionarVeterinarioLogeado() {
        try {
            if (Application.getSesionUsuario() == null || Application.getSesionUsuario().getRol() == null) {
                return;
            }
            String rol = Application.getSesionUsuario().getRol().getNombreRol();
            if (rol == null) {
                return;
            }
            String r = rol.trim().toLowerCase();
            if (!(r.equals("veterinario") || r.equals("veterinarios"))) {
                return;
            }

            String nombre = Application.getNombreApellidoUsuarioLogeado();
            if (nombre == null || nombre.trim().isEmpty()) {
                return;
            }

            for (int i = 1; i < jcbVeterinario.getItemCount(); i++) {
                String item = jcbVeterinario.getItemAt(i);
                if (item != null && item.trim().equalsIgnoreCase(nombre.trim())) {
                    jcbVeterinario.setSelectedIndex(i);
                    return;
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }
    }

    private boolean validarDatosTurno() {
        if (jcbPaciente.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una mascota!");
            return false;
        }
        if (jcbTipoDeCita.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un tipo de cita!");
            return false;
        }

        if (jcbVeterinario.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un veterinario!");
            return false;
        }
        if (mascotaSeleccionada == null || mascotaSeleccionada.getIdMascota() == null) {
            JOptionPane.showMessageDialog(this, "Para guardar un turno debe estar asociado a una mascota existente!");
            return false;
        }
        if (jdcFecha.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una fecha!");
            return false;
        }
        if (jcbTurno.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un turno (mañana o tarde)!");
            return false;
        }
        return true;
    }

    private boolean persistirTurno() {
        boolean retornar = false;
        if ("CreandoTurnoPeluqueria".equals(Application.consultarEstadoUsuario())) {
            turno = new Peluqueria();
        }
        turno.setUsuarioGestion(Application.getNombreApellidoUsuarioLogeado());
        Usuario vetSel = obtenerVeterinarioSeleccionado();
        if (vetSel != null) {
            turno.setVeterinario(vetSel);
        }

        turno.setMascota(mascotaSeleccionada);
        LocalDate fecha = jdcFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        turno.setFecha(fecha);

        AgendaSlot slotSel = obtenerSlotSeleccionado();
        turno.setSlot(slotSel);
        if (slotSel != null) {
            try {
                turno.setHora(slotSel.getHoraInicio());
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
            }
        }

        Object selTipo = jcbTipoDeCita.getSelectedItem();
        if (selTipo instanceof TipoCitaPeluqueria) {
            TipoCitaPeluqueria tc = (TipoCitaPeluqueria) selTipo;
            turno.setTipoDeCita(tc.getDescripcion());
            turno.setTipoCitaPeluqueria(tc);
            turno.setPrecioCerrado(tc.getPrecio());
        } else if (selTipo != null) {
            turno.setTipoDeCita(selTipo.toString());
        }

        if ("CreandoTurnoPeluqueria".equals(Application.consultarEstadoUsuario())) {
            turno.setEstado(EstadoPeluqueriaEnum.fromLabel(EstadoPeluqueriaEnum.PENDIENTE.getLabel()));
        }
        turno.setPresupuesto(txtPresupuesto.getText());

        if ("CreandoTurnoPeluqueria".equals(Application.consultarEstadoUsuario())) {
            try {                // Estado en alta fijo a PENDIENTE (validación innecesaria).
                if (operarTurno.crearTurnoMascota(turno)) {
                    JOptionPane.showMessageDialog(this, "Peluqueria guardado exitosamente.");
                    retornar = true;
                } 
            } catch (Exception ex) {
                Logger.getLogger(FormTurnosPeluqueria.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if ("EditandoTurnoPeluqueria".equals(Application.consultarEstadoUsuario())) {
            try {
                if (turno != null && (turno.getEstado() == EstadoPeluqueriaEnum.CANCELADO || turno.getEstado() == EstadoPeluqueriaEnum.ELIMINADO)) {
                    JOptionPane.showMessageDialog(this, "Los turnos Cancelados o Eliminados no pueden ser editados.");
                    return false;
                }

                operarTurno.actualizarTurno(turno);
                JOptionPane.showMessageDialog(this, "Peluqueria editado exitosamente.");
                retornar = true;
            } catch (Exception ex) {
                Logger.getLogger(FormTurnosPeluqueria.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return retornar;
    }


    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAtenderTurno;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCancelarTurno;
    private javax.swing.JButton btnConfirmarTurno;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JComboBox jcbClienteDueño;
    private javax.swing.JComboBox<String> jcbPaciente;
    private javax.swing.JComboBox<TipoCitaPeluqueria> jcbTipoDeCita;
    private javax.swing.JComboBox<String> jcbTurno;
    private javax.swing.JComboBox<String> jcbVeterinario;
    private com.toedter.calendar.JDateChooser jdcFecha;
    private com.toedter.calendar.JDateChooser jdcFechaBuscar;
    private javax.swing.JPanel jpDatosTurno;
    private javax.swing.JPanel jpListaTurnos;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbEstado2;
    private javax.swing.JLabel lbFecha;
    private javax.swing.JLabel lbFechaYHora;
    private javax.swing.JLabel lbHora;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbPaciente1;
    private javax.swing.JLabel lbTipoDeCita;
    private javax.swing.JLabel lbVeterinarioAsignado;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelHadear;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tableTurnos;
    private javax.swing.JTextField txtPresupuesto;
    // End of variables declaration//GEN-END:variables

    private void mostrarDialogoWhatsAppConfirmacion(Long idTurno) {
        Peluqueria t = null;
        try {
            t = (idTurno != null) ? operarTurno.buscarTurnoPorId(idTurno) : null;
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        String texto = construirTextoWhatsAppConfirmacion(t);
        Window owner = SwingUtilities.getWindowAncestor(this);
        WhatsAppMensajeDialog.mostrar(owner, "Mensaje para WhatsApp", texto);
    }

    private void mostrarDialogoWhatsAppFinalizarCarga(Peluqueria t) {
        String texto = construirTextoWhatsAppFinalizarCarga(t);
        Window owner = SwingUtilities.getWindowAncestor(this);
        WhatsAppMensajeDialog.mostrar(owner, "Mensaje para WhatsApp", texto);
    }

    private String construirTextoWhatsAppFinalizarCarga(Peluqueria t) {
        LocalDate fecha = null;
        LocalTime hora = null;
        String tipo = "";
        String mascota = "";
        String cliente = "";

        try {
            if (t != null) {
                fecha = t.getFecha();
                hora = t.getHora();
                tipo = (t.getTipoDeCita() != null) ? t.getTipoDeCita().trim() : "";

                if (t.getMascota() != null) {
                    mascota = (t.getMascota().getNombre() != null) ? t.getMascota().getNombre().trim() : "";
                    if (t.getMascota().getCliente() != null && t.getMascota().getCliente().getPersona() != null) {
                        String n = t.getMascota().getCliente().getPersona().getNombre();
                        String a = t.getMascota().getCliente().getPersona().getApellido();
                        String full = ((n != null) ? n : "") + " " + ((a != null) ? a : "");
                        cliente = full.trim();
                    }
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaTxt = (fecha != null) ? fecha.format(fmtFecha) : "__/__/____";
        String horaTxt = (hora != null) ? hora.format(fmtHora) : "__:__";

        StringBuilder sb = new StringBuilder();
        sb.append("Este es un mensaje generado por el Sistema: ");
        sb.append("Su turno");
        if (!tipo.isEmpty()) {
            sb.append(" (" + tipo + ")");
        }
        sb.append(" en Veterinaria \"DoctorCat\" para el día ");
        sb.append(fechaTxt);
        sb.append(" a las ");
        sb.append(horaTxt);

        if (!cliente.isEmpty()) {
            sb.append(" (Cliente: ");
            sb.append(cliente);
            sb.append(")");
        }

        if (!mascota.isEmpty()) {
            sb.append(" (Mascota: ");
            sb.append(mascota);
            sb.append(")");
        }

        sb.append(" fue REGISTRADO con éxito.\n");
        sb.append("Por favor confirmá este turno respondiendo a este mensaje.\n");
        sb.append("¡Gracias! Los esperamos!");

        return sb.toString();
    }

    private String construirTextoWhatsAppConfirmacion(Peluqueria t) {
        LocalDate fecha = null;
        LocalTime hora = null;
        String tipo = "";
        String mascota = "";
        String cliente = "";

        try {
            if (t != null) {
                fecha = t.getFecha();
                hora = t.getHora();
                tipo = (t.getTipoDeCita() != null) ? t.getTipoDeCita().trim() : "";

                if (t.getMascota() != null) {
                    mascota = (t.getMascota().getNombre() != null) ? t.getMascota().getNombre().trim() : "";
                    if (t.getMascota().getCliente() != null && t.getMascota().getCliente().getPersona() != null) {
                        String n = t.getMascota().getCliente().getPersona().getNombre();
                        String a = t.getMascota().getCliente().getPersona().getApellido();
                        String full = ((n != null) ? n : "") + " " + ((a != null) ? a : "");
                        cliente = full.trim();
                    }
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex);
        }

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaTxt = (fecha != null) ? fecha.format(fmtFecha) : "__/__/____";
        String horaTxt = (hora != null) ? hora.format(fmtHora) : "__:__";

        StringBuilder sb = new StringBuilder();
        sb.append("Este es un mensaje Generado por el Sistema: ");
        sb.append("Su turno");
        if (!tipo.isEmpty()) {
            sb.append(" (" + tipo + ")");
        }
        sb.append(" en Veterinaria \"DoctorCat\" para el día ");
        sb.append(fechaTxt);
        sb.append(" a las ");
        sb.append(horaTxt);

        if (!cliente.isEmpty()) {
            sb.append(" (Cliente: ");
            sb.append(cliente);
            sb.append(")");
        }

        if (!mascota.isEmpty()) {
            sb.append(" (Mascota: ");
            sb.append(mascota);
            sb.append(")");
        }

        sb.append(" fue CONFIRMADO con éxito.\n");
        sb.append("Agendá y no te olvides. Los esperamos!");

        return sb.toString();
    }

    // Nota: el diálogo genérico de copiar/cerrar vive en WhatsAppMensajeDialog.

    private void inicializarTiposCitaPeluqueria() {
        try {
            // Defaults = lo que tenía el combo originalmente en el diseñador (initComponents)
            List<String> defaults = new ArrayList<>();
            int size = jcbTipoDeCita.getItemCount();
            for (int i = 0; i < size; i++) {
                Object it = jcbTipoDeCita.getItemAt(i);
                if (it != null) defaults.add(it.toString());
            }

            TipoCitaPeluqueriaService service = new TipoCitaPeluqueriaService();
            service.ensureSeedFromDefaults(defaults);

            cargarComboTipoCitaDesdeDB();
            configurarPresupuestoPorTipoCita();
            try {
                txtPresupuesto.setEditable(false);
            } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex); }

        } catch (Exception ex) {
            // Si falla por alguna razón (p.ej. tabla no existe aún), no rompemos la UI.
            // Queda el combo con sus valores originales.
        }
    }

    
    private void cargarComboTipoCitaDesdeDB() {
        try {
            TipoCitaPeluqueriaService service = new TipoCitaPeluqueriaService();
            List<TipoCitaPeluqueria> tipos = service.listarActivos();

            DefaultComboBoxModel<TipoCitaPeluqueria> model = new DefaultComboBoxModel<>();
            model.addElement(null);
            for (TipoCitaPeluqueria t : tipos) {
                model.addElement(t);
            }
            jcbTipoDeCita.setModel(model);

            // Renderer: "Descripción — $Precio"
            jcbTipoDeCita.setRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    String txt;
                    if (value == null) {
                        txt = "Seleccione un Tipo de Cita";
                    } else if (value instanceof TipoCitaPeluqueria) {
                        TipoCitaPeluqueria t = (TipoCitaPeluqueria) value;
                        BigDecimal p = (t.getPrecio() == null) ? BigDecimal.ZERO : t.getPrecio();
                        txt = t.getDescripcion() + " — $" + p.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
                    } else {
                        txt = value.toString();
                    }
                    return super.getListCellRendererComponent(list, txt, index, isSelected, cellHasFocus);
                }
            });

            try { jcbTipoDeCita.setSelectedIndex(0); } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex); }

        } catch (Exception ex) {
            // Si la BD no está lista todavía, dejamos el modelo original.
        }
    }
    
    private void configurarPresupuestoPorTipoCita() {
        jcbTipoDeCita.addActionListener(e -> {
            try {
                Object sel = jcbTipoDeCita.getSelectedItem();
                if (!(sel instanceof TipoCitaPeluqueria)) {
                    txtPresupuesto.setText("");
                    return;
                }
                TipoCitaPeluqueria t = (TipoCitaPeluqueria) sel;
                BigDecimal p = (t.getPrecio() == null) ? BigDecimal.ZERO : t.getPrecio();
                txtPresupuesto.setText(p.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
            } catch (Exception ex) {
                // No interrumpir la UI
            }
        });
    }

    private void seleccionarTipoCitaEnCombo(String descripcion) {
        if (descripcion == null) {
            try { jcbTipoDeCita.setSelectedIndex(0); } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex); }
            return;
        }
        String target = descripcion.trim();
        if (target.isEmpty()) return;
        try {
            for (int i = 0; i < jcbTipoDeCita.getItemCount(); i++) {
                Object it = jcbTipoDeCita.getItemAt(i);
                if (it instanceof TipoCitaPeluqueria) {
                    TipoCitaPeluqueria t = (TipoCitaPeluqueria) it;
                    if (t.getDescripcion() != null && t.getDescripcion().trim().equalsIgnoreCase(target)) {
                        jcbTipoDeCita.setSelectedIndex(i);
                        return;
                    }
                }
            }
        } catch (Exception ex) { veterinaria.util.AppLog.warn(FormTurnosPeluqueria.class, "Excepción no fatal en UI", ex); }
    }

}
