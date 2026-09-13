package veterinaria.vista;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SpinnerDateModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import veterinaria.controlador.HospitalizacionControlador;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.MascotaControlador;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.util.ManejoTablas;
import veterinaria.util.SesionUsuario;
import veterinaria.util.UtilidadesTabla;
import veterinaria.vista.application.Application;
import veterinaria.util.AppLog;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.HistoriaEventoAbrible;
import veterinaria.util.ui.ClienteMascotaSelector;
import veterinaria.util.Constantes;
import veterinaria.servicio.AgendaSlotService;
import veterinaria.servicio.ConfiguracionService;
import veterinaria.servicio.HospitalizacionService;
import java.util.Calendar;
import java.time.LocalDateTime;

import javax.swing.SwingUtilities;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.vista.componentes.RegistroHospitalizacionDialog;
import veterinaria.util.enums.EstadoHospitalizacion;

public class FormHospitalizaciones extends javax.swing.JPanel implements HistoriaEventoAbrible {

    private final ClienteControlador clienteControlador = new ClienteControlador();
    private ClienteMascotaSelector clienteMascotaSelector;
    private final MascotaControlador mascotaControlador = new MascotaControlador();
    private Mascota mascotaSeleccionada;
    private Hospitalizacion hospitalizacion = new Hospitalizacion();
    private final HospitalizacionControlador hospitalizacionControlador = new HospitalizacionControlador();
    private final HospitalizacionService hospitalizacionService = new HospitalizacionService();
    private final AgendaSlotService agendaSlotService = new AgendaSlotService();
    private final ConfiguracionService configService = new ConfiguracionService();
    private List<Usuario> listaVeterinarios = new ArrayList<>();
    private boolean actualizandoFormulario = false;
    private boolean modoEdicion = false;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private boolean modoUrgencia = false;
    private TableRowSorter<TableModel> sorterHospitalizaciones;
    private final Map<Integer, TableCellRenderer> originalColumnRenderers = new HashMap<>();
    private SesionUsuario sesion = Application.getSesionUsuario();

    public FormHospitalizaciones() {
        initComponents();
        try {
            tableHospitalizaciones.setPreferredSize(null);
            tableHospitalizaciones.setMinimumSize(null);
            tableHospitalizaciones.setMaximumSize(null);
            tableHospitalizaciones.setFillsViewportHeight(true);
        } catch (Exception ignore) {
        }
        PermisoUI.aplicar(this);
        initRenderers();
        initListeners();
        configurarSpinnerFechaHoraAlta();

        configurarRestriccionFechaAlta();

        jpAgendarHospitalizacion.setVisible(false);
        jpRegistrarIngresos.setVisible(false);
        jpAltaPaciente.setVisible(false);
        jpButtonPanel.setVisible(false);

        // Botonera del header: en lugar de deshabilitar, ocultamos según el estado
        btnAgendarHospitalizacion.setVisible(true);
        btnAgendarHospitalizacion.setEnabled(true);

        btnEditarHospitalizacion.setVisible(true);
        btnCancelarHospitalizacion.setVisible(false);

        btnRegistrarIngresoHospitalizacion.setVisible(false);
        btnAltaPacienteHospitalizacion.setVisible(false);

        // Acciones sobre registro seleccionado (ver / imprimir)
        btnVerRegistro.setVisible(false);
        btnImprimirRegistro.setVisible(false);
        btnVerRegistro.addActionListener(e -> btnVerRegistroAction());

        initFiltroEstado();
        cargarCombosBoxs();
        configurarSpinnerHora(null);
        llenarComboBoxMotivos();
        cargarComboVeterinarios();
        initListenersAgenda();
        cargarHospitalizacionesEnTabla();
        sorterHospitalizaciones = new TableRowSorter<>(tableHospitalizaciones.getModel());
        tableHospitalizaciones.setRowSorter(sorterHospitalizaciones);
        aplicarFiltros();
        cacheOriginalRenderersIfNeeded();
        UtilidadesTabla.ajustarAnchoColumnas(tableHospitalizaciones);
        ManejoTablas.tooltipValorEnColumnas(tableHospitalizaciones, Arrays.asList(3, 4, 5, 6, 7));
    }

    /**
     * Abre una hospitalización puntual desde Historia Clínica (modo lectura).
     */
    @Override
    public void abrirDetallePorId(Integer refId) {
        if (refId == null) {
            return;
        }

        // Asegurar listado visible y actualizado
        mostrarSoloListado();

        Integer viewRow = buscarFilaPorId(refId);
        if (viewRow == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró la hospitalización #" + refId + " en la lista.",
                    "No encontrado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            tableHospitalizaciones.setRowSelectionInterval(viewRow, viewRow);
            tableHospitalizaciones.scrollRectToVisible(tableHospitalizaciones.getCellRect(viewRow, 0, true));
        } catch (Exception ignore) {
        }

        if (editarHospitalizacionSeleccionada()) {
            // Mostrar panel con datos cargados
            jpListaHospitalizaciones.setVisible(false);
            jpAgendarHospitalizacion.setVisible(true);
            jpRegistrarIngresos.setVisible(false);
            jpAltaPaciente.setVisible(false);
            jpButtonPanel.setVisible(true);

            bloquearLecturaDetalle();
        }
    }

    private Integer buscarFilaPorId(Integer idHospitalizacion) {
        try {
            for (int viewRow = 0; viewRow < tableHospitalizaciones.getRowCount(); viewRow++) {
                int modelRow = tableHospitalizaciones.convertRowIndexToModel(viewRow);
                Object val = tableHospitalizaciones.getModel().getValueAt(modelRow, 1); // ID col 1
                if (val != null && idHospitalizacion.toString().equals(val.toString())) {
                    return viewRow;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private void bloquearLecturaDetalle() {
        try {
            // Ocultar acciones del header (evita edición desde historial)
            deshabilitarHeader();

            // Botonera inferior
            btnGuardar.setEnabled(false);
            btnCancelar.setEnabled(true);

            // Campos
            jcbClienteDueño.setEnabled(false);
            jcbPaciente.setEnabled(false);
            jcbMotivo.setEnabled(false);
            jcbVeterinario.setEnabled(false);
            jdcFechaTurnoHospitalizacion.setEnabled(false);
            jdcFechaDeIngreso.setEnabled(false);
            jcbTurnoExtracion.setEnabled(false);
        } catch (Exception ignore) {
        }
    }

    private void initListenersAgenda() {
        // Cambio de fecha (BUSCAR TURNO) -> sincronizar y refrescar turnos
        jdcFechaTurnoHospitalizacion.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (actualizandoFormulario) {
                    return;
                }
                try {
                    Date d = jdcFechaTurnoHospitalizacion.getDate();
                    actualizandoFormulario = true;
                    jdcFechaDeIngreso.setDate(d);
                    actualizandoFormulario = false;

                    if (d != null && getVeterinarioSeleccionado() != null) {
                        LocalDate f = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        cargarTurnosComboParaFechaHospitalizacion(f);
                    } else {
                        limpiarTurnosComboHospitalizacion();
                    }
                } catch (Exception ignore) {
                    actualizandoFormulario = false;
                }
            }
        });

        // Cambio de fecha (Ingreso) -> sincronizar con BUSCAR TURNO (por si el usuario toca la otra)
        jdcFechaDeIngreso.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (actualizandoFormulario) {
                    return;
                }
                try {
                    Date d = jdcFechaDeIngreso.getDate();
                    actualizandoFormulario = true;
                    jdcFechaTurnoHospitalizacion.setDate(d);
                    actualizandoFormulario = false;

                    if (d != null && getVeterinarioSeleccionado() != null) {
                        LocalDate f = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        cargarTurnosComboParaFechaHospitalizacion(f);
                    } else {
                        limpiarTurnosComboHospitalizacion();
                    }
                } catch (Exception ignore) {
                    actualizandoFormulario = false;
                }
            }
        });

        // Cambio de veterinario -> refrescar turnos con la fecha actual
        jcbVeterinario.addActionListener(e -> {
            if (actualizandoFormulario) {
                return;
            }
            try {
                Date d = (jdcFechaTurnoHospitalizacion.getDate() != null) ? jdcFechaTurnoHospitalizacion.getDate() : jdcFechaDeIngreso.getDate();
                if (d != null && getVeterinarioSeleccionado() != null) {
                    LocalDate f = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    cargarTurnosComboParaFechaHospitalizacion(f);
                } else {
                    limpiarTurnosComboHospitalizacion();
                }
            } catch (Exception ignore) {
            }
        });

        // Cambio de turno -> reflejar hora en spinner (solo visual)
        jcbTurnoExtracion.addActionListener(e -> {
            if (actualizandoFormulario) {
                return;
            }
            try {
                AgendaSlot s = getSlotSeleccionadoCombo();
                if (s != null) {
                    configurarSpinnerHora(s.getHoraInicio());
                }
            } catch (Exception ignore) {
            }
        });
    }

    private void cargarCombosBoxs() {
        // Unificado: selección Cliente -> Mascota (mismo helper para todos los formularios)
        if (clienteMascotaSelector == null) {
            clienteMascotaSelector = new ClienteMascotaSelector(
                    jcbClienteDueño,
                    jcbPaciente,
                    clienteControlador,
                    mascotaControlador,
                    (m) -> mascotaSeleccionada = m
            );
            clienteMascotaSelector.init();
        } else {
            clienteMascotaSelector.reset();
        }
    }

    private String nombreCliente(Cliente c) {
        if (c == null) {
            return "";
        }
        if (c.getPersona() != null) {
            String n = c.getPersona().getNombre() != null ? c.getPersona().getNombre().trim() : "";
            String a = c.getPersona().getApellido() != null ? c.getPersona().getApellido().trim() : "";
            String full = (n + " " + a).trim();
            if (!full.isBlank()) {
                return full;
            }
        }
        if (c.getRazonSocial() != null && !c.getRazonSocial().isBlank()) {
            return c.getRazonSocial();
        }
        return (c.getIdCliente() != null) ? ("Cliente #" + c.getIdCliente()) : "Cliente";
    }

    private void initRenderers() {
        // Dueños: muestra nombre/apellido o razón social
        jcbClienteDueño.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente) {
                    setText(nombreCliente((Cliente) value));
                } else if (value != null) {
                    setText(value.toString());
                } else {
                    setText("");
                }
                return this;
            }
        });

        // Mascotas: Nombre (Especie/Raza)
        jcbPaciente.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Mascota) {
                    setText(nombreMascota((Mascota) value));
                } else if (value != null) {
                    setText(value.toString());
                } else {
                    setText("");
                }
                return this;
            }
        });

        // Turnos: HH:mm - HH:mm (y marca Actual si corresponde)
        jcbTurnoExtracion.setRenderer(new javax.swing.DefaultListCellRenderer() {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AgendaSlot) {
                    AgendaSlot s = (AgendaSlot) value;
                    String label = (s.getHoraInicio() != null ? s.getHoraInicio().format(fmt) : "")
                            + " - " + (s.getHoraFin() != null ? s.getHoraFin().format(fmt) : "");
                    if (hospitalizacion != null && hospitalizacion.getSlot() != null
                            && hospitalizacion.getSlot().getIdSlot() != null
                            && hospitalizacion.getSlot().getIdSlot().equals(s.getIdSlot())) {
                        label += " (Actual)";
                    }
                    setText(label);
                } else if (value != null) {
                    setText(value.toString());
                } else {
                    setText("");
                }
                return this;
            }
        });
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private String clienteSortKey(Cliente c) {
        if (c == null) {
            return "";
        }
        if (c.getPersona() != null) {
            String a = safe(c.getPersona().getApellido());
            String n = safe(c.getPersona().getNombre());
            String key = (a + " " + n).trim();
            if (!key.isBlank()) {
                return key;
            }
        }
        if (c.getRazonSocial() != null && !c.getRazonSocial().isBlank()) {
            return c.getRazonSocial().trim();
        }
        return (c.getIdCliente() != null) ? ("Cliente " + c.getIdCliente()) : "Cliente";
    }

    private String nombreMascota(Mascota m) {
        if (m == null) {
            return "";
        }
        String nombre = safe(m.getNombre());
        if (nombre.isBlank()) {
            nombre = (m.getIdMascota() != null) ? ("Mascota #" + m.getIdMascota()) : "Mascota";
        }
        String especie = safe(m.getEspecie());
        String raza = safe(m.getRaza());
        if (especie.isBlank() && raza.isBlank()) {
            return nombre;
        }
        if (especie.isBlank()) {
            especie = "-";
        }
        if (raza.isBlank()) {
            raza = "-";
        }
        return nombre + " (" + especie + "/" + raza + ")";
    }

    private void limpiarComboMascotas() {
        DefaultComboBoxModel<Object> modelMascotas = new DefaultComboBoxModel<>();
        modelMascotas.addElement("Seleccionar Mascota");
        jcbPaciente.setModel(modelMascotas);
        mascotaSeleccionada = null;
    }

    private Cliente getClienteSeleccionado() {
        Object item = jcbClienteDueño.getSelectedItem();
        return (item instanceof Cliente) ? (Cliente) item : null;
    }

    private Mascota getMascotaSeleccionadaCombo() {
        Object item = jcbPaciente.getSelectedItem();
        return (item instanceof Mascota) ? (Mascota) item : null;
    }

    private void cargarMascotasParaCliente(Cliente c) {
        limpiarComboMascotas();
        if (c == null || c.getIdCliente() == null) {
            return;
        }

        List<Mascota> todas = mascotaControlador.buscarTodasLasMascotas();
        if (todas == null) {
            return;
        }

        List<Mascota> filtradas = new ArrayList<>();
        for (Mascota m : todas) {
            try {
                if (m != null && m.getCliente() != null && c.getIdCliente().equals(m.getCliente().getIdCliente())) {
                    filtradas.add(m);
                }
            } catch (Exception ignore) {
            }
        }

        filtradas.sort((a, b) -> safe(a != null ? a.getNombre() : "").compareToIgnoreCase(safe(b != null ? b.getNombre() : "")));

        DefaultComboBoxModel<Object> modelMascotas = new DefaultComboBoxModel<>();
        modelMascotas.addElement("Seleccionar Mascota");
        for (Mascota m : filtradas) {
            modelMascotas.addElement(m); // ID interno oculto
        }
        jcbPaciente.setModel(modelMascotas);
    }

    private void seleccionarClienteEnComboPorId(Integer idCliente) {
        if (idCliente == null) {
            jcbClienteDueño.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < jcbClienteDueño.getItemCount(); i++) {
            Object it = jcbClienteDueño.getItemAt(i);
            if (it instanceof Cliente) {
                Cliente c = (Cliente) it;
                if (idCliente.equals(c.getIdCliente())) {
                    jcbClienteDueño.setSelectedIndex(i);
                    return;
                }
            }
        }
        jcbClienteDueño.setSelectedIndex(0);
    }

    private void seleccionarMascotaEnComboPorId(Integer idMascota) {
        if (idMascota == null) {
            jcbPaciente.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < jcbPaciente.getItemCount(); i++) {
            Object it = jcbPaciente.getItemAt(i);
            if (it instanceof Mascota) {
                Mascota m = (Mascota) it;
                if (idMascota.equals(m.getIdMascota())) {
                    jcbPaciente.setSelectedIndex(i);
                    mascotaSeleccionada = m;
                    return;
                }
            }
        }
        jcbPaciente.setSelectedIndex(0);
        mascotaSeleccionada = null;
    }

    private void cargarComboVeterinarios() {
        try {
            listaVeterinarios = new UsuarioDAO().obtenerUsuariosVeterinarios();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccione veterinario");
            for (Usuario u : listaVeterinarios) {
                model.addElement(nombreCompletoUsuario(u));
            }
            jcbVeterinario.setModel(model);

            jcbVeterinario.addActionListener(e -> {
                if (actualizandoFormulario) {
                    return;
                }
                try {
                    if (jdcFechaDeIngreso.getDate() != null) {
                        LocalDate f = jdcFechaDeIngreso.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        cargarTurnosComboParaFechaHospitalizacion(f);
                    } else {
                        limpiarTurnosComboHospitalizacion();
                    }
                } catch (Exception ignore) {
                }
            });
        } catch (Exception e) {
            jcbVeterinario.setModel(new DefaultComboBoxModel<>(new String[]{"Seleccione veterinario"}));
        }
    }

    private Usuario getVeterinarioSeleccionado() {
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
        } catch (Exception ignore) {
        }
        return (u.getNombreUsuario() != null) ? u.getNombreUsuario() : "";
    }

    private void seleccionarVeterinarioEnComboPorId(Integer idVet) {
        if (idVet == null) {
            jcbVeterinario.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < listaVeterinarios.size(); i++) {
            Usuario u = listaVeterinarios.get(i);
            if (u != null && u.getIdUsuario() != null && u.getIdUsuario().equals(idVet)) {
                jcbVeterinario.setSelectedIndex(i + 1);
                return;
            }
        }
        jcbVeterinario.setSelectedIndex(0);
    }

    private boolean esDiaHabilitadoHospitalizacion(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        String diasCsv = configService.getString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_HOSPITALIZACION,
                ConfiguracionService.DEFAULT_DIAS_HABILITADOS_HOSPITALIZACION
        );
        int dayValue = fecha.getDayOfWeek().getValue();
        if (diasCsv == null || diasCsv.trim().isEmpty()) {
            return false;
        }
        String[] parts = diasCsv.split(",");
        for (String p : parts) {
            try {
                if (Integer.parseInt(p.trim()) == dayValue) {
                    return true;
                }
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    private void limpiarTurnosComboHospitalizacion() {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione horario");
        jcbTurnoExtracion.setModel(model);
        lbTurnoExtraccion.setText("Turno: *");
    }

    private void cargarTurnosComboParaFechaHospitalizacion(LocalDate fecha) {
        Usuario vet = getVeterinarioSeleccionado();
        if (fecha == null || vet == null) {
            limpiarTurnosComboHospitalizacion();
            return;
        }

        if (!esDiaHabilitadoHospitalizacion(fecha)) {
            limpiarTurnosComboHospitalizacion();
            if (!modoUrgencia) {
                JOptionPane.showMessageDialog(this, "El día seleccionado no está habilitado para Hospitalización.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                AppLog.info(FormHospitalizaciones.class, "Modo Urgencia True");
                return;
            }
        }

        Long idSlotActual = (hospitalizacion != null && hospitalizacion.getSlot() != null) ? hospitalizacion.getSlot().getIdSlot() : null;
        List<AgendaSlot> slots = agendaSlotService.listarParaEdicion(fecha, vet, idSlotActual);

        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione horario");

        int selectIndex = 0;
        int idx = 0;
        for (AgendaSlot s : slots) {
            idx++;
            model.addElement(s); // ID interno oculto (AgendaSlot)
            if (idSlotActual != null && idSlotActual.equals(s.getIdSlot())) {
                selectIndex = idx;
            }
        }

        jcbTurnoExtracion.setModel(model);
        jcbTurnoExtracion.setSelectedIndex(selectIndex);
    }

    private AgendaSlot getSlotSeleccionadoCombo() {
        Object item = jcbTurnoExtracion.getSelectedItem();
        return (item instanceof AgendaSlot) ? (AgendaSlot) item : null;
    }

    private void llenarComboBoxMotivos() {
        jcbMotivo.removeAllItems();
        jcbMotivo.addItem("Seleccione un motivo");
        jcbMotivo.addItem("Programar cirujia - Castración");
        jcbMotivo.addItem("Deshidratación");
        jcbMotivo.addItem("Infecciones");
        jcbMotivo.addItem("Dolor posquirúrgico");
        jcbMotivo.addItem("Recuperación de Cirugia");
        jcbMotivo.addItem("Observación por Intoxicación");
        jcbMotivo.addItem("ACV");
        jcbMotivo.addItem("Accidente o trauma");
        jcbMotivo.addItem("Dificultad para respirar");
        jcbMotivo.addItem("Heridas o sangrado");
        jcbMotivo.setSelectedIndex(0);

    }

    private void limpiarCamposFormulario() {
        jcbClienteDueño.setSelectedIndex(0);
        limpiarComboMascotas();
        mascotaSeleccionada = null;
        jcbMotivo.setSelectedIndex(0);
        configurarSpinnerHora(null);
        jdcFechaDeIngreso.setDate(new Date());
        spFechaHoraAlta.setValue(new Date());
        taDiagnosticoHospitalizacion.setText("");
        taTratamiento.setText("");
        jcbVeterinario.setSelectedIndex(0);
        limpiarTurnosComboHospitalizacion();
    }

    private void initFiltroEstado() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Todos");
        model.addElement(Constantes.ESTADO_HOSPITALIZACION_PENDIENTE);
        model.addElement(Constantes.ESTADO_HOSPITALIZACION_INTERNADO);
        model.addElement(Constantes.ESTADO_HOSPITALIZACION_ALTA);
        model.addElement(Constantes.ESTADO_HOSPITALIZACION_CANCELADO);
        jcbFiltroEstado.setModel(model);
        jcbFiltroEstado.setSelectedItem("Todos");
    }

    private void mostrarSoloListado() {
        jpHeader.setVisible(true);
        jpAgendarHospitalizacion.setVisible(false);
        jpRegistrarIngresos.setVisible(false);
        jpAltaPaciente.setVisible(false);
        jpButtonPanel.setVisible(false);
        jpListaHospitalizaciones.setVisible(true);
        btnAgendarHospitalizacion.setEnabled(true);
        modoEdicion = false;
        actualizarBotonesPorSeleccion();
    }

    private void configurarUIEdicionSimple(boolean esEdicion) {
        // En este formulario sólo se agenda/edita: fecha, veterinario, turno y motivo.
        modoEdicion = esEdicion;
        jpHeader.setVisible(false);
        jpAgendarHospitalizacion.setVisible(true);
        jpButtonPanel.setVisible(true);
        jpRegistrarIngresos.setVisible(false);
        jpAltaPaciente.setVisible(false);

        // Campos no editables en edición
        jcbClienteDueño.setEnabled(!esEdicion);
        jcbPaciente.setEnabled(!esEdicion);

        // Permitidos
        jcbMotivo.setEnabled(true);
        jcbVeterinario.setEnabled(true);
        jdcFechaTurnoHospitalizacion.setEnabled(true);
        jdcFechaDeIngreso.setEnabled(true);
        jcbTurnoExtracion.setEnabled(true);

        // No se usan en este flujo
        taDiagnosticoHospitalizacion.setEnabled(false);
        taTratamiento.setEnabled(false);
        spHoraHospitalizacion.setEnabled(false);
        spFechaHoraAlta.setEnabled(false);
    }

    private void deshabilitarHeader() {
        // En vez de deshabilitar, ocultamos botones del header para limpiar la UI
        btnAgendarHospitalizacion.setVisible(false);
        btnEditarHospitalizacion.setVisible(false);
        btnCancelarHospitalizacion.setVisible(false);
        btnRegistrarIngresoHospitalizacion.setVisible(false);
        btnAltaPacienteHospitalizacion.setVisible(false);
        btnVerRegistro.setVisible(false);
        btnImprimirRegistro.setVisible(false);
        jpHeader.revalidate();
        jpHeader.repaint();
    }

    private boolean entrarModoEdicionPorEstado(String estado) {

        if (Constantes.ESTADO_HOSPITALIZACION_ALTA.equals(estado)
                || Constantes.ESTADO_HOSPITALIZACION_CANCELADO.equals(estado)) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se puede editar una hospitalización en estado \"" + estado + "\".",
                    "Edición no permitida",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        modoEdicion = true;
        deshabilitarHeader();

        // En edición se oculta el listado y se usa el panel de botones
        jpListaHospitalizaciones.setVisible(false);
        jpButtonPanel.setVisible(true);

        jpAgendarHospitalizacion.setVisible(true);
        jpAltaPaciente.setVisible(false);

        if (Constantes.ESTADO_HOSPITALIZACION_INTERNADO.equals(estado)) {
            jpRegistrarIngresos.setVisible(true);
        } else {
            jpRegistrarIngresos.setVisible(false);
        }
        return true;
    }

    private String getEstadoSeleccionado() {
        int viewRow = tableHospitalizaciones.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = tableHospitalizaciones.convertRowIndexToModel(viewRow);
        Object estadoObj = tableHospitalizaciones.getModel().getValueAt(modelRow, 10);
        return (estadoObj != null) ? estadoObj.toString() : null;
    }

    private void ocultarBotonesHeaderContextuales() {
        btnEditarHospitalizacion.setVisible(false);
        btnCancelarHospitalizacion.setVisible(false);
        btnRegistrarIngresoHospitalizacion.setVisible(false);
        btnAltaPacienteHospitalizacion.setVisible(false);
        btnVerRegistro.setVisible(false);
        btnImprimirRegistro.setVisible(false);
    }

    private void refrescarHeader() {
        jpHeader.revalidate();
        jpHeader.repaint();
    }

    private void actualizarBotonesPorSeleccion() {
        if (modoEdicion) {
            btnAgendarHospitalizacion.setVisible(false);
            ocultarBotonesHeaderContextuales();
            refrescarHeader();
            return;
        }

        btnAgendarHospitalizacion.setVisible(true);

        int viewRow = tableHospitalizaciones.getSelectedRow();
        if (viewRow < 0) {
            // Sin selección: ocultar acciones contextuales
            ocultarBotonesHeaderContextuales();
            refrescarHeader();
            return;
        }

        // Siempre se puede ver / imprimir el registro si hay una selección.
        btnVerRegistro.setVisible(true);
        btnImprimirRegistro.setVisible(true);

        int modelRow = tableHospitalizaciones.convertRowIndexToModel(viewRow);
        Object estadoObj = tableHospitalizaciones.getModel().getValueAt(modelRow, 10);
        String estado = (estadoObj != null) ? estadoObj.toString() : "";

        boolean esPendiente = Constantes.ESTADO_HOSPITALIZACION_PENDIENTE.equals(estado);
        boolean esInternado = Constantes.ESTADO_HOSPITALIZACION_INTERNADO.equals(estado);

        // Acciones por estado
        btnCancelarHospitalizacion.setVisible(esPendiente);
        btnRegistrarIngresoHospitalizacion.setVisible(esPendiente);
        btnAltaPacienteHospitalizacion.setVisible(esInternado);
        btnEditarHospitalizacion.setVisible(esPendiente);

        refrescarHeader();
    }

    private void initListeners() {
        txtBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltros();
            }
        });

        // Filtro por estado
        jcbFiltroEstado.addActionListener(e -> aplicarFiltros());

        // Habilitar Editar/Cancelar cuando haya selección
        tableHospitalizaciones.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            actualizarBotonesPorSeleccion();
        });

        // Alta Paciente: impedir seleccionar fechas anteriores a hoy
        spFechaHoraAlta.addChangeListener(e -> {
            if (actualizandoFormulario) {
                return;
            }
            corregirFechaAltaSiEsPasado();
        });
    }

    private void configurarSpinnerFechaHoraAlta() {
        try {
            SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
            spFechaHoraAlta.setModel(model);
            spFechaHoraAlta.setEditor(new JSpinner.DateEditor(spFechaHoraAlta, "dd-MM-yyyy HH:mm"));
            if (spFechaHoraAlta.getValue() == null) {
                spFechaHoraAlta.setValue(new Date());
            }
            spFechaHoraAlta.setEnabled(false);
        } catch (Exception ignore) {
        }
    }

    private void configurarRestriccionFechaAlta() {
        try {
            Date hoy = dateDesdeLocalDate(LocalDate.now()); // 00:00 de hoy
            SpinnerDateModel model = (SpinnerDateModel) spFechaHoraAlta.getModel();
            model.setStart(hoy);
            model.setEnd(null);

            Date actual = (Date) spFechaHoraAlta.getValue();
            if (actual == null || normalizarFecha(actual).before(hoy)) {
                spFechaHoraAlta.setValue(hoy);
            }
        } catch (Exception ignore) {
        }

    }

    private void corregirFechaAltaSiEsPasado() {
        try {
            Date hoy = dateDesdeLocalDate(LocalDate.now());
            SpinnerDateModel model = (SpinnerDateModel) spFechaHoraAlta.getModel();
            model.setStart(hoy);
            model.setEnd(null);

            Date sel = (Date) spFechaHoraAlta.getValue();
            if (sel != null && normalizarFecha(sel).before(hoy)) {
                spFechaHoraAlta.setValue(hoy);
                JOptionPane.showMessageDialog(this,
                        "La fecha/hora de alta no puede ser anterior a la fecha actual.",
                        "Fecha inválida",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ignore) {
        }

    }

    private Date dateDesdeLocalDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date normalizarFecha(Date d) {
        if (d == null) {
            return null;
        }
        LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return dateDesdeLocalDate(ld);
    }

    private void aplicarFiltros() {
        if (sorterHospitalizaciones == null) {
            sorterHospitalizaciones = new TableRowSorter<>(tableHospitalizaciones.getModel());
            tableHospitalizaciones.setRowSorter(sorterHospitalizaciones);
        }

        String query = (txtBusqueda.getText() != null) ? txtBusqueda.getText().trim() : "";
        String estadoSel = (String) jcbFiltroEstado.getSelectedItem();

        List<RowFilter<Object, Object>> filtros = new ArrayList<>();

        // Búsqueda por texto (cols: mascota, cliente, fechaHora, motivo, estado)
        if (!query.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 2, 3, 4, 5, 10));
            aplicarRendererBusqueda(query);
        } else {
            restaurarRendererBusqueda();
        }

        // Estado
        if (estadoSel == null || estadoSel.equals("Todos")) {
            // Todos por defecto: muestra todos MENOS cancelados
            filtros.add(RowFilter.notFilter(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(Constantes.ESTADO_HOSPITALIZACION_CANCELADO) + "$", 10)));
        } else {
            filtros.add(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(estadoSel) + "$", 10));
        }

        sorterHospitalizaciones.setRowFilter(RowFilter.andFilter(filtros));
        actualizarBotonesPorSeleccion();
    }

    private void cacheOriginalRenderersIfNeeded() {
        if (!originalColumnRenderers.isEmpty()) {
            return;
        }
        int[] cols = new int[]{2, 3, 4, 5, 10};
        for (int c : cols) {
            try {
                TableCellRenderer r = tableHospitalizaciones.getColumnModel().getColumn(c).getCellRenderer();
                originalColumnRenderers.put(c, r); // puede ser null
            } catch (Exception ignore) {
            }
        }
    }

    private void aplicarRendererBusqueda(String query) {
        cacheOriginalRenderersIfNeeded();
        int[] cols = new int[]{2, 3, 4, 5, 10};
        TableCellRenderer r = new ResaltarCoincidenciasRenderer(query, cols);
        for (int c : cols) {
            try {
                tableHospitalizaciones.getColumnModel().getColumn(c).setCellRenderer(r);
            } catch (Exception ignore) {
            }
        }
    }

    private void restaurarRendererBusqueda() {
        cacheOriginalRenderersIfNeeded();
        for (Map.Entry<Integer, TableCellRenderer> e : originalColumnRenderers.entrySet()) {
            try {
                tableHospitalizaciones.getColumnModel().getColumn(e.getKey()).setCellRenderer(e.getValue());
            } catch (Exception ignore) {
            }
        }
    }

    private class ResaltarCoincidenciasRenderer extends DefaultTableCellRenderer {

        private final String query;
        private final int[] columnasObjetivo;

        public ResaltarCoincidenciasRenderer(String query, int[] columnasObjetivo) {
            this.query = query;
            this.columnasObjetivo = columnasObjetivo;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Verifica si la columna actual es una de las que queremos resaltar
            boolean columnaCoincide = false;
            for (int col : columnasObjetivo) {
                if (col == column) {
                    columnaCoincide = true;
                    break;
                }
            }
            if (columnaCoincide && value != null && query != null && !query.isEmpty()) {
                String cellValue = value.toString();
                String lowerCellValue = cellValue.toLowerCase();
                String lowerQuery = query.toLowerCase();

                if (lowerCellValue.startsWith(lowerQuery)) {

                    String highlightedText = "<html><span style='color:#FFA500; font-weight:bold;'>" + cellValue.substring(0, query.length()) + "</span>" + cellValue.substring(query.length()) + "</html>";

                    setText(highlightedText);
                } else {

                    setText(cellValue);
                }
            } else {

                setText(value != null ? value.toString() : "");
            }

            return c;
        }
    }
    private boolean isUpdating = false;

    private void actualizarTabla() {
        tableHospitalizaciones.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0 && !isUpdating) {
                isUpdating = true;
                int rowCount = tableHospitalizaciones.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    if (i != e.getFirstRow() && Boolean.TRUE.equals(tableHospitalizaciones.getValueAt(i, 0))) {
                        tableHospitalizaciones.setValueAt(false, i, 0);
                    }
                }
                isUpdating = false;
            }
        });
    }

    private void cargarHospitalizacionesEnTabla() {
        DefaultTableModel model = (DefaultTableModel) tableHospitalizaciones.getModel();
        model.setRowCount(0);
        List<Hospitalizacion> hospitalizaciones = hospitalizacionControlador.obtenerTodasLasHospitalizacion();

        for (Hospitalizacion hospitalizacion : hospitalizaciones) {
            String fechaIngreso = (hospitalizacion.getFechaIngreso() != null) ? hospitalizacion.getFechaIngreso().toString() : "Fecha de ingreso no disponible";
            String hora = (hospitalizacion.getHora() != null) ? hospitalizacion.getHora().toString() : "Hora no disponible";
            String fechaHora = fechaIngreso + " a las " + hora;
            //String fechaAlta = (hospitalizacion.getFechaAlta() != null) ? hospitalizacion.getFechaAlta().toString() : "Aún hospitalizado";
            //String fechaRango = fechaIngreso + " - " + fechaAlta;
            String nombreMascota = (hospitalizacion.getMascota() != null) ? hospitalizacion.getMascota().getNombre() : "Sin mascota";

            String cliente = "Sin cliente";
            if (hospitalizacion.getCliente() != null && hospitalizacion.getCliente().getPersona() != null) {
                cliente = hospitalizacion.getCliente().getPersona().getNombre() + " " + hospitalizacion.getCliente().getPersona().getApellido();
            }
            String vet = "Sin veterinario";
            if (hospitalizacion.getVeterinario() != null && hospitalizacion.getVeterinario().getPersona() != null) {
                vet = hospitalizacion.getVeterinario().getPersona().getNombre() + " " + hospitalizacion.getVeterinario().getPersona().getApellido();
            }
            String fechaAltaStr = renderFechaAltaSegunEstado(hospitalizacion);

            Object[] fila = {
                false,
                hospitalizacion.getIdHospitalizacion(),
                nombreMascota,
                cliente,
                fechaHora,
                //hospitalizacion.getFechaIngreso(),
                hospitalizacion.getMotivo(),
                hospitalizacion.getDiagnostico(),
                hospitalizacion.getTratamiento(),
                vet,
                //fechaRango,
                fechaAltaStr,
                hospitalizacion.getEstado()
            };
            model.addRow(fila);
        }
        actualizarTabla();

        // Re-aplicar filtros (estado/búsqueda) tras recargar datos
        if (sorterHospitalizaciones != null) {
            aplicarFiltros();
        }
    }

    /**
     * Renderiza la columna "Fecha Alta" respetando el estado funcional del
     * flujo: - Pendiente -> "Aún No Hospitalizado" (si no hay fechaAlta) -
     * Internado -> "Aún hospitalizado" (si no hay fechaAlta) - Alta medica->
     * fecha formateada (si faltara, "-") - Cancelado -> "-" (independiente de
     * fechaAlta)
     */
    private String renderFechaAltaSegunEstado(Hospitalizacion h) {
        if (h == null) {
            return "-";
        }

        EstadoHospitalizacion estado = EstadoHospitalizacion.fromEtiqueta(h.getEstado());

        // Cancelado: siempre guion medio
        if (estado == EstadoHospitalizacion.CANCELADO) {
            return "-";
        }

        // Alta médica: debería tener fecha; si no, guion medio
        if (estado == EstadoHospitalizacion.ALTA) {
            return formatFechaAlta(h.getFechaAlta(), "-");
        }

        // Pendiente / Internado (o estado desconocido): depende de fechaAlta
        if (h.getFechaAlta() == null) {
            if (estado == EstadoHospitalizacion.PENDIENTE) {
                return "Aún No Hospitalizado";
            }
            if (estado == EstadoHospitalizacion.INTERNADO) {
                return "Aún hospitalizado";
            }
            // Fallback seguro si el estado viene null o distinto
            return "Aún hospitalizado";
        }

        // Si hay fechaAlta, mostrarla formateada (aunque el estado esté inconsistente)
        return formatFechaAlta(h.getFechaAlta(), h.getFechaAlta().toString());
    }

    private boolean fechaAltaPosteriorOIgualAlIngreso(LocalDateTime fechaHoraAlta) {
        if (fechaHoraAlta == null || hospitalizacion == null || hospitalizacion.getFechaIngreso() == null) {
            return true;
        }
        LocalTime horaIngreso = hospitalizacion.getHora() != null ? hospitalizacion.getHora() : LocalTime.MIN;
        LocalDateTime fechaHoraIngreso = LocalDateTime.of(hospitalizacion.getFechaIngreso(), horaIngreso);
        return !fechaHoraAlta.isBefore(fechaHoraIngreso);
    }

    private String formatFechaAlta(java.time.LocalDateTime fechaAlta, String fallback) {
        if (fechaAlta == null) {
            return fallback;
        }
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            return fechaAlta.format(f);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void configurarSpinnerHora(LocalTime value) {
        SpinnerDateModel model = new SpinnerDateModel();
        spHoraHospitalizacion.setModel(model);

        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spHoraHospitalizacion, "HH:mm");
        spHoraHospitalizacion.setEditor(timeEditor);

        if (value != null) {
            LocalDate hoy = LocalDate.now();
            Date fechaConHora = Date.from(value.atDate(hoy).atZone(ZoneId.systemDefault()).toInstant());
            spHoraHospitalizacion.setValue(fechaConHora);
        } else {
            spHoraHospitalizacion.setValue(new Date());
        }
    }

    private LocalTime getHoraSpinnerHospitalizacion() {
        try {
            Object v = spHoraHospitalizacion.getValue();
            if (v instanceof Date) {
                Date d = (Date) v;
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                        .withSecond(0).withNano(0);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * Ajusta el formulario según modo urgencia. - En urgencia, el turno NO es
     * obligatorio y se omiten restricciones de agenda.
     */
    private void aplicarModoUrgenciaUI(boolean urgencia) {
        try {
            jcbTurnoExtracion.setEnabled(!urgencia);
            // En urgencia, mantenemos visible la fecha pero el turno deja de ser obligatorio.
            lbTurnoExtraccion.setText(urgencia ? "Turno:" : "Turno: *");

            if (urgencia) {
                // Selección por defecto
                if (jcbTurnoExtracion.getItemCount() > 0) {
                    jcbTurnoExtracion.setSelectedIndex(0);
                }
            }
        } catch (Exception ignore) {
        }
    }

    private boolean editarHospitalizacionSeleccionada() {
        boolean bRetorno = false;
        int viewRow = tableHospitalizaciones.getSelectedRow();

        if (viewRow >= 0) {
            int modelRow = tableHospitalizaciones.convertRowIndexToModel(viewRow);
            Integer idHospitalizacion = (Integer) tableHospitalizaciones.getModel().getValueAt(modelRow, 1);
            hospitalizacion = hospitalizacionControlador.buscarHospitalizacionPorId(idHospitalizacion);

            if (hospitalizacion != null) {
                actualizandoFormulario = true;
                mascotaSeleccionada = hospitalizacion.getMascota();

// Asegurar que la mascota tenga su Dueño cargado (HospitalizacionDAO puede no traer m.cliente en algunas versiones)
                try {
                    if (mascotaSeleccionada != null
                            && mascotaSeleccionada.getIdMascota() != null
                            && (mascotaSeleccionada.getCliente() == null
                            || mascotaSeleccionada.getCliente().getIdCliente() == null)) {

                        Mascota recargada = mascotaControlador.buscarMascotaPorId(mascotaSeleccionada.getIdMascota());
                        if (recargada != null) {
                            mascotaSeleccionada = recargada;
                            hospitalizacion.setMascota(recargada);
                        }
                    }
                } catch (Exception ignore) {
                }

// Dueño + Mascota
                Cliente dueño = null;
                try {
                    if (hospitalizacion.getCliente() != null && hospitalizacion.getCliente().getIdCliente() != null) {
                        dueño = hospitalizacion.getCliente();
                    } else if (mascotaSeleccionada != null && mascotaSeleccionada.getCliente() != null) {
                        dueño = mascotaSeleccionada.getCliente();
                    }
                } catch (Exception ignore) {
                }

// Cargar y seleccionar en combos (reutilizando selector, si está inicializado)
                Integer idCliente = (dueño != null) ? dueño.getIdCliente() : null;
                Integer idMascota = (mascotaSeleccionada != null) ? mascotaSeleccionada.getIdMascota() : null;

                if (clienteMascotaSelector != null) {
                    clienteMascotaSelector.seleccionarPorIds(idCliente, idMascota);
                    // Mantener variable local para validaciones/guardado
                    mascotaSeleccionada = clienteMascotaSelector.getMascotaSeleccionada();
                } else {
                    if (dueño != null) {
                        seleccionarClienteEnComboPorId(idCliente);
                        cargarMascotasParaCliente(dueño);
                    } else {
                        jcbClienteDueño.setSelectedIndex(0);
                        limpiarComboMascotas();
                    }
                    if (idMascota != null) {
                        seleccionarMascotaEnComboPorId(idMascota);
                    }
                }
                if (hospitalizacion.getFechaIngreso() != null) {
                    Date fechaIngreso = Date.from(hospitalizacion.getFechaIngreso().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    jdcFechaDeIngreso.setDate(fechaIngreso);
                    jdcFechaTurnoHospitalizacion.setDate(fechaIngreso);
                } else {
                    jdcFechaDeIngreso.setDate(null);
                    jdcFechaTurnoHospitalizacion.setDate(null);
                }

                configurarSpinnerHora(hospitalizacion.getHora());
                jcbMotivo.setSelectedItem(hospitalizacion.getMotivo());
                taDiagnosticoHospitalizacion.setText(hospitalizacion.getDiagnostico() != null ? hospitalizacion.getDiagnostico() : "");
                taTratamiento.setText(hospitalizacion.getTratamiento() != null ? hospitalizacion.getTratamiento() : "");

                // Fecha/Hora de alta (si existe). Si no, dejamos la actual.
                if (hospitalizacion.getFechaAlta() != null) {
                    try {
                        Date dAlta = Date.from(hospitalizacion.getFechaAlta().atZone(ZoneId.systemDefault()).toInstant());
                        spFechaHoraAlta.setValue(dAlta);
                    } catch (Exception ignore) {
                    }
                } else {
                    spFechaHoraAlta.setValue(new Date());
                }

                if (hospitalizacion.getVeterinario() != null) {
                    seleccionarVeterinarioEnComboPorId(hospitalizacion.getVeterinario().getIdUsuario());
                } else {
                    jcbVeterinario.setSelectedIndex(0);
                }
                // refrescar turnos para incluir el actual
                if (hospitalizacion.getFechaIngreso() != null && getVeterinarioSeleccionado() != null) {
                    if (!Constantes.ESTADO_HOSPITALIZACION_INTERNADO
                            .equals(hospitalizacion.getEstado())) {
                        cargarTurnosComboParaFechaHospitalizacion(
                                hospitalizacion.getFechaIngreso()
                        );
                    }
                } else {
                    limpiarTurnosComboHospitalizacion();
                }

                actualizandoFormulario = false;

                bRetorno = true;
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la Hospitalización seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "¡Debe seleccionar una Hospitalización a editar!", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;
    }

    private boolean eliminarHospitalizacionSeleccionada() {
        int viewRow = tableHospitalizaciones.getSelectedRow();

        if (viewRow != -1) {
            int modelRow = tableHospitalizaciones.convertRowIndexToModel(viewRow);
            Integer idHospitalizacion = (Integer) tableHospitalizaciones.getModel().getValueAt(modelRow, 1);
            hospitalizacion = hospitalizacionControlador.buscarHospitalizacionPorId(idHospitalizacion);

            if (hospitalizacion != null) {
                // Confirmar eliminación
                int respuesta = JOptionPane.showConfirmDialog(
                        this,
                        "¿Estás seguro de que deseas eliminar este registro?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (respuesta == JOptionPane.YES_OPTION) {
                    boolean ok = false;
                    try {
                        ok = hospitalizacionService.cancelarIngreso(hospitalizacion.getIdHospitalizacion());
                    } catch (Exception e) {
                        ok = false;
                    }
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Hospitalización cancelada con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);
                        return true;
                    }
                    JOptionPane.showMessageDialog(this, "No se pudo cancelar la Hospitalización.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                } else {                    // Deseleccionar la fila si se elige NO
                    tableHospitalizaciones.clearSelection();
                }
            } else {
                JOptionPane.showMessageDialog(this, "La Hospitalización seleccionada no se encontró.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una Hospitalización para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        lbInternaciones = new javax.swing.JLabel();
        lbBusqueda = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnAgendarHospitalizacion = new javax.swing.JButton();
        btnEditarHospitalizacion = new javax.swing.JButton();
        btnCancelarHospitalizacion = new javax.swing.JButton();
        btnRegistrarIngresoHospitalizacion = new javax.swing.JButton();
        btnAltaPacienteHospitalizacion = new javax.swing.JButton();
        lbEstado = new javax.swing.JLabel();
        jcbFiltroEstado = new javax.swing.JComboBox<>();
        btnVerRegistro = new javax.swing.JButton();
        btnImprimirRegistro = new javax.swing.JButton();
        jpAgendarHospitalizacion = new javax.swing.JPanel();
        jSeparator7 = new javax.swing.JSeparator();
        lbTituloAgendaHospitalizacion = new javax.swing.JLabel();
        lbPaciente = new javax.swing.JLabel();
        jcbPaciente = new javax.swing.JComboBox();
        jcbMotivo = new javax.swing.JComboBox<>();
        lbMotivo = new javax.swing.JLabel();
        jcbVeterinario = new javax.swing.JComboBox<>();
        lbVeterinarioAsignado = new javax.swing.JLabel();
        lbTurnoExtraccion = new javax.swing.JLabel();
        jcbTurnoExtracion = new javax.swing.JComboBox();
        jcbClienteDueño = new javax.swing.JComboBox();
        lbCliente = new javax.swing.JLabel();
        jdcFechaTurnoHospitalizacion = new com.toedter.calendar.JDateChooser();
        lbFechaExtracción = new javax.swing.JLabel();
        jpRegistrarIngresos = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        lbRegistarIngresos = new javax.swing.JLabel();
        lbFechaIngreso = new javax.swing.JLabel();
        jdcFechaDeIngreso = new com.toedter.calendar.JDateChooser();
        lbDiagnostico = new javax.swing.JLabel();
        lbHora = new javax.swing.JLabel();
        spHoraHospitalizacion = new javax.swing.JSpinner();
        jScrollPane2 = new javax.swing.JScrollPane();
        taDiagnosticoHospitalizacion = new javax.swing.JTextArea();
        jpAltaPaciente = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        taTratamiento = new javax.swing.JTextArea();
        lbTratamiento = new javax.swing.JLabel();
        jSeparator8 = new javax.swing.JSeparator();
        lbFechaNacimiento1 = new javax.swing.JLabel();
        spFechaHoraAlta = new javax.swing.JSpinner();
        lbAltaPaciente = new javax.swing.JLabel();
        jpButtonPanel = new javax.swing.JPanel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jpListaHospitalizaciones = new javax.swing.JPanel();
        jSeparator5 = new javax.swing.JSeparator();
        lbListaDeHospitalizaciones = new javax.swing.JLabel();
        jSeparator6 = new javax.swing.JSeparator();
        spTableHospitalizaciones = new javax.swing.JScrollPane();
        tableHospitalizaciones = new veterinaria.vista.table.AutoTable();

        lbInternaciones.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lbInternaciones.setText("Gestión de Hospitalizaciónes");

        lbBusqueda.setText("BUSCAR PACIENTE");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        btnAgendarHospitalizacion.setText("Agendar Hospitalización");
        btnAgendarHospitalizacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgendarHospitalizacionActionPerformed(evt);
            }
        });

        btnEditarHospitalizacion.setText("Editar");
        btnEditarHospitalizacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarHospitalizacionActionPerformed(evt);
            }
        });

        btnCancelarHospitalizacion.setText("Cancelar");
        btnCancelarHospitalizacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarHospitalizacionActionPerformed(evt);
            }
        });

        btnRegistrarIngresoHospitalizacion.setText("Registrar Ingreso");
        btnRegistrarIngresoHospitalizacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarIngresoHospitalizacionActionPerformed(evt);
            }
        });

        btnAltaPacienteHospitalizacion.setText("Alta Paciente");
        btnAltaPacienteHospitalizacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAltaPacienteHospitalizacionActionPerformed(evt);
            }
        });

        lbEstado.setText("Estado:");

        jcbFiltroEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos" }));

        btnVerRegistro.setText("Ver ");
        btnVerRegistro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerRegistroActionPerformed(evt);
            }
        });

        btnImprimirRegistro.setText("Imprimir");
        btnImprimirRegistro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirRegistroActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lbBusqueda))
                        .addGap(12, 12, 12)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbEstado)
                            .addComponent(jcbFiltroEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(18, 18, Short.MAX_VALUE)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addComponent(btnVerRegistro, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnImprimirRegistro, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addComponent(btnAgendarHospitalizacion)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnEditarHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnCancelarHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btnRegistrarIngresoHospitalizacion)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAltaPacienteHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jSeparator1)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(lbInternaciones)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbInternaciones)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVerRegistro)
                    .addComponent(btnImprimirRegistro))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAgendarHospitalizacion)
                            .addComponent(btnEditarHospitalizacion)
                            .addComponent(btnCancelarHospitalizacion))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAltaPacienteHospitalizacion)
                            .addComponent(btnRegistrarIngresoHospitalizacion)))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lbEstado)
                            .addComponent(lbBusqueda))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jcbFiltroEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lbBuscar))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbTituloAgendaHospitalizacion.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lbTituloAgendaHospitalizacion.setText("Agendar Hospitalización");

        lbPaciente.setText("Paciente: *");

        jcbPaciente.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar Paciente" }));

        jcbMotivo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Motivo" }));

        lbMotivo.setText("Motivo: *");

        jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione veterinario" }));

        lbVeterinarioAsignado.setText("Veterinario: *");

        lbTurnoExtraccion.setText("Turno: *");

        jcbTurnoExtracion.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccione un turno" }));
        jcbTurnoExtracion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbTurnoExtracionActionPerformed(evt);
            }
        });

        jcbClienteDueño.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar cliente" }));

        lbCliente.setText("Cliente: *");

        lbFechaExtracción.setText("Fecha: *");

        javax.swing.GroupLayout jpAgendarHospitalizacionLayout = new javax.swing.GroupLayout(jpAgendarHospitalizacion);
        jpAgendarHospitalizacion.setLayout(jpAgendarHospitalizacionLayout);
        jpAgendarHospitalizacionLayout.setHorizontalGroup(
            jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addComponent(lbTituloAgendaHospitalizacion)
                        .addGap(0, 870, Short.MAX_VALUE))
                    .addComponent(jSeparator7))
                .addContainerGap())
            .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbFechaExtracción)
                            .addComponent(jdcFechaTurnoHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbTurnoExtraccion)
                            .addComponent(jcbTurnoExtracion, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jcbClienteDueño, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbCliente))
                        .addGap(18, 18, 18)
                        .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbPaciente)
                            .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbMotivo)
                    .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, 310, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbVeterinarioAsignado)
                    .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jpAgendarHospitalizacionLayout.setVerticalGroup(
            jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbTituloAgendaHospitalizacion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbPaciente)
                            .addComponent(lbCliente))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbClienteDueño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addComponent(lbVeterinarioAsignado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jpAgendarHospitalizacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addComponent(lbTurnoExtraccion)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbTurnoExtracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addComponent(lbMotivo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpAgendarHospitalizacionLayout.createSequentialGroup()
                        .addComponent(lbFechaExtracción)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdcFechaTurnoHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(12, 12, 12))
        );

        lbRegistarIngresos.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lbRegistarIngresos.setText("Registrar Ingreso a Hospitalización:");

        lbFechaIngreso.setText("Fecha de Ingreso:");

        lbDiagnostico.setText("Diagnostico: *");

        lbHora.setText("Hora: *");

        spHoraHospitalizacion.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.HOUR));

        taDiagnosticoHospitalizacion.setColumns(20);
        taDiagnosticoHospitalizacion.setRows(5);
        jScrollPane2.setViewportView(taDiagnosticoHospitalizacion);

        javax.swing.GroupLayout jpRegistrarIngresosLayout = new javax.swing.GroupLayout(jpRegistrarIngresos);
        jpRegistrarIngresos.setLayout(jpRegistrarIngresosLayout);
        jpRegistrarIngresosLayout.setHorizontalGroup(
            jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarIngresosLayout.createSequentialGroup()
                .addGroup(jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(jpRegistrarIngresosLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbDiagnostico)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 550, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jpRegistrarIngresosLayout.createSequentialGroup()
                                .addGroup(jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jdcFechaDeIngreso, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbFechaIngreso))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbHora)
                                    .addComponent(spHoraHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(lbRegistarIngresos))
                        .addGap(0, 499, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(jSeparator4)
        );
        jpRegistrarIngresosLayout.setVerticalGroup(
            jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarIngresosLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbRegistarIngresos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbHora, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lbFechaIngreso, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarIngresosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jdcFechaDeIngreso, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spHoraHospitalizacion, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbDiagnostico)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        taTratamiento.setColumns(20);
        taTratamiento.setRows(5);
        jScrollPane1.setViewportView(taTratamiento);

        lbTratamiento.setText("Tratamiento: *");

        lbFechaNacimiento1.setText("Fecha/Hora de Alta: *");

        lbAltaPaciente.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lbAltaPaciente.setText("Alta de Paciente:");

        javax.swing.GroupLayout jpAltaPacienteLayout = new javax.swing.GroupLayout(jpAltaPaciente);
        jpAltaPaciente.setLayout(jpAltaPacienteLayout);
        jpAltaPacienteLayout.setHorizontalGroup(
            jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAltaPacienteLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpAltaPacienteLayout.createSequentialGroup()
                        .addGroup(jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator8)
                            .addGroup(jpAltaPacienteLayout.createSequentialGroup()
                                .addComponent(lbAltaPaciente)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jpAltaPacienteLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbTratamiento)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 550, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spFechaHoraAlta, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbFechaNacimiento1))
                        .addContainerGap(352, Short.MAX_VALUE))))
        );
        jpAltaPacienteLayout.setVerticalGroup(
            jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpAltaPacienteLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbAltaPaciente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addGroup(jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbTratamiento)
                    .addComponent(lbFechaNacimiento1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpAltaPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                    .addGroup(jpAltaPacienteLayout.createSequentialGroup()
                        .addComponent(spFechaHoraAlta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

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

        javax.swing.GroupLayout jpButtonPanelLayout = new javax.swing.GroupLayout(jpButtonPanel);
        jpButtonPanel.setLayout(jpButtonPanelLayout);
        jpButtonPanelLayout.setHorizontalGroup(
            jpButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpButtonPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jpButtonPanelLayout.setVerticalGroup(
            jpButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpButtonPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar)
                    .addComponent(btnLimpiar))
                .addContainerGap())
        );

        lbListaDeHospitalizaciones.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lbListaDeHospitalizaciones.setText("Lista de Hospitalizaciones");

        spTableHospitalizaciones.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableHospitalizaciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Nº Paciente", "Nombre", "Cliente (Dueño)", "Fecha de Ingreso", "Motivo", "Diagnóstico", "Tratamiento", "Veterinario", "Fecha de Alta", "Estado"
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
        tableHospitalizaciones.setMinimumSize(new java.awt.Dimension(848, 220));
        tableHospitalizaciones.setPreferredSize(new java.awt.Dimension(848, 220));
        tableHospitalizaciones.getTableHeader().setReorderingAllowed(false);
        spTableHospitalizaciones.setViewportView(tableHospitalizaciones);

        javax.swing.GroupLayout jpListaHospitalizacionesLayout = new javax.swing.GroupLayout(jpListaHospitalizaciones);
        jpListaHospitalizaciones.setLayout(jpListaHospitalizacionesLayout);
        jpListaHospitalizacionesLayout.setHorizontalGroup(
            jpListaHospitalizacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaHospitalizacionesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaHospitalizacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spTableHospitalizaciones, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator6)
                    .addComponent(jSeparator5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpListaHospitalizacionesLayout.createSequentialGroup()
                        .addComponent(lbListaDeHospitalizaciones)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpListaHospitalizacionesLayout.setVerticalGroup(
            jpListaHospitalizacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaHospitalizacionesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbListaDeHospitalizaciones)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spTableHospitalizaciones, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpRegistrarIngresos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpListaHospitalizaciones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpAgendarHospitalizacion, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpAltaPaciente, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpButtonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpAgendarHospitalizacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpRegistrarIngresos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpAltaPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaHospitalizaciones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgendarHospitalizacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgendarHospitalizacionActionPerformed
        hospitalizacion = new Hospitalizacion();
        limpiarCamposFormulario();
        jdcFechaDeIngreso.setDate(null);
        jdcFechaTurnoHospitalizacion.setDate(null);
        configurarUIEdicionSimple(false);
        modoUrgencia = false;
        aplicarModoUrgenciaUI(false);

        jpHeader.setVisible(false);
        jpListaHospitalizaciones.setVisible(false);

        spHoraHospitalizacion.setEnabled(true);
        jdcFechaDeIngreso.setEnabled(true);
    }//GEN-LAST:event_btnAgendarHospitalizacionActionPerformed

    private void btnEditarHospitalizacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarHospitalizacionActionPerformed
        String perm = "FormHospitalizaciones.EDITAR";
        if (sesion != null && sesion.puede(perm)) {
            String estadoSel = getEstadoSeleccionado();
            if (estadoSel == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro para editar.");
                return;
            }
            // No permitir edición en ALTA o CANCELADO
            if (Constantes.ESTADO_HOSPITALIZACION_ALTA.equals(estadoSel)
                    || Constantes.ESTADO_HOSPITALIZACION_CANCELADO.equals(estadoSel)) {
                JOptionPane.showMessageDialog(this,
                        "No se puede editar una hospitalización en estado \"" + estadoSel + "\".",
                        "Edición no permitida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (editarHospitalizacionSeleccionada()) {
                configurarUIEdicionSimple(true);
                entrarModoEdicionPorEstado(estadoSel);
            }
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para editar hospitalizaciones.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }//GEN-LAST:event_btnEditarHospitalizacionActionPerformed

    private void btnCancelarHospitalizacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarHospitalizacionActionPerformed
        if (eliminarHospitalizacionSeleccionada()) {
            limpiarCamposFormulario();
            mostrarSoloListado();
            cargarHospitalizacionesEnTabla();
        }
    }//GEN-LAST:event_btnCancelarHospitalizacionActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed

        if (modoEdicion && jpAltaPaciente.isVisible()) {
            if (hospitalizacion == null || hospitalizacion.getIdHospitalizacion() == null) {
                JOptionPane.showMessageDialog(this, "No se encontró la hospitalización seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date dAlta = (Date) spFechaHoraAlta.getValue();
            LocalDateTime fechaHoraAlta = (dAlta != null)
                    ? dAlta.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : null;

            if (fechaHoraAlta == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una fecha/hora de alta.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!fechaAltaPosteriorOIgualAlIngreso(fechaHoraAlta)) {
                JOptionPane.showMessageDialog(this,
                        "La fecha/hora de alta no puede ser anterior al ingreso del paciente.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String tratamiento = taTratamiento.getText() != null ? taTratamiento.getText().trim() : "";
            if (tratamiento.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese el tratamiento indicado para el alta.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Integer idUsuarioGestion = (sesion != null && sesion.getUsuario() != null)
                    ? sesion.getUsuario().getIdUsuario()
                    : null;

            String nombreMascota = (hospitalizacion.getMascota() != null && hospitalizacion.getMascota().getNombre() != null)
                    ? hospitalizacion.getMascota().getNombre()
                    : "-";
            String nombreCliente = (hospitalizacion.getCliente() != null && hospitalizacion.getCliente().getPersona() != null)
                    ? (hospitalizacion.getCliente().getPersona().getNombre() + " " + hospitalizacion.getCliente().getPersona().getApellido())
                    : "-";

            String msg = "Se dará el alta médica a:\n"
                    + "Mascota: " + nombreMascota + "\n"
                    + "Cliente: " + nombreCliente + "\n"
                    + "Fecha/Hora Alta: " + fechaHoraAlta.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n\n"
                    + "¿Desea confirmar?";

            int resp = JOptionPane.showConfirmDialog(this, msg, "Confirmar alta médica", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                boolean ok = hospitalizacionService.registrarAltaMedica(
                        hospitalizacion.getIdHospitalizacion(),
                        fechaHoraAlta,
                        tratamiento,
                        idUsuarioGestion
                );

                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                            "No se pudo dar el alta. Verifique que el registro esté en estado Internado.",
                            "Acción no permitida",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JOptionPane.showMessageDialog(this, "Alta médica registrada con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);
                cargarHospitalizacionesEnTabla();
                mostrarSoloListado();
                actualizarBotonesPorSeleccion();
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al registrar el alta: " + e.getMessage(),
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (modoEdicion && jpRegistrarIngresos.isVisible()) {
            if (hospitalizacion == null || hospitalizacion.getIdHospitalizacion() == null) {
                JOptionPane.showMessageDialog(this, "No se encontró la hospitalización seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String diagnostico = taDiagnosticoHospitalizacion.getText() != null ? taDiagnosticoHospitalizacion.getText().trim() : "";
            if (diagnostico.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese un diagnóstico antes de registrar el ingreso.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Integer idUsuarioGestion = (sesion != null && sesion.getUsuario() != null)
                    ? sesion.getUsuario().getIdUsuario()
                    : null;

            String nombreMascota = (hospitalizacion.getMascota() != null && hospitalizacion.getMascota().getNombre() != null)
                    ? hospitalizacion.getMascota().getNombre()
                    : "-";
            String nombreCliente = hospitalizacion.getCliente() != null ? hospitalizacion.getCliente().getPersona().getNombre() + " " + hospitalizacion.getCliente().getPersona().getApellido() : "-";

            String fechaIng = (hospitalizacion.getFechaIngreso() != null)
                    ? hospitalizacion.getFechaIngreso().format(dtf)
                    : "-";
            String horaIng = (hospitalizacion.getHora() != null) ? hospitalizacion.getHora().toString() : "-";

            String msg
                    = "Se registrará el ingreso de hospitalización para:\n"
                    + "Mascota: " + nombreMascota + "\n"
                    + "Cliente: " + nombreCliente + "\n"
                    + "Fecha/Hora: " + fechaIng + " " + horaIng + "\n\n"
                    + "Acción: actualizar diagnóstico y cambiar estado a Internado.\n\n"
                    + "¿Desea confirmar?";

            int resp = JOptionPane.showConfirmDialog(this, msg, "Confirmar hospitalización", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                boolean ok = hospitalizacionService.registrarIngresoPendiente(
                        hospitalizacion.getIdHospitalizacion(),
                        diagnostico,
                        idUsuarioGestion
                );

                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                            "No se pudo registrar el ingreso. Verifique que el registro esté en estado Pendiente.",
                            "Acción no permitida",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JOptionPane.showMessageDialog(this, "Paciente hospitalizado con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);
                cargarHospitalizacionesEnTabla();
                mostrarSoloListado();
                btnAgendarHospitalizacion.setEnabled(true);
                modoUrgencia = false;
                aplicarModoUrgenciaUI(false);
                actualizarBotonesPorSeleccion();
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al registrar el ingreso: " + e.getMessage(),
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (mascotaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una mascota.");
            return;
        }

        // Validaciones UX: veterinario + fecha + turno
        Usuario vet = getVeterinarioSeleccionado();
        if (vet == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un veterinario.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (jdcFechaDeIngreso.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una fecha de ingreso.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate fechaIngreso = jdcFechaDeIngreso.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        AgendaSlot slotSel = null;
        if (!modoUrgencia) {
            if (!esDiaHabilitadoHospitalizacion(fechaIngreso)) {
                JOptionPane.showMessageDialog(this, "El día seleccionado no está habilitado para Hospitalización.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            slotSel = getSlotSeleccionadoCombo();
            if (slotSel == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un horario disponible para la fecha/veterinario.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (mascotaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (mascotaSeleccionada.getCliente() == null) {
            JOptionPane.showMessageDialog(this, "La mascota seleccionada no tiene un cliente asociado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Guardar ID (FK) del usuario que gestiona
        if (sesion != null && sesion.getUsuario() != null) {
            hospitalizacion.setUsuarioGestion(sesion.getUsuario());
        } else {
            hospitalizacion.setUsuarioGestion(null);
        }

        if (!modoEdicion) {
            hospitalizacion.setMascota(mascotaSeleccionada);
            hospitalizacion.setCliente(mascotaSeleccionada.getCliente());
            hospitalizacion.setDiagnostico(taDiagnosticoHospitalizacion.getText());
            hospitalizacion.setTratamiento(taTratamiento.getText());
            hospitalizacion.setFechaAlta(null);
        }

        if (jcbMotivo.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un motivo.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        hospitalizacion.setMotivo((String) jcbMotivo.getSelectedItem());

        if (modoUrgencia) {
            hospitalizacion.setVeterinario(vet);
            hospitalizacion.setFechaIngreso(fechaIngreso);
            LocalTime horaUrg = getHoraSpinnerHospitalizacion();
            if (horaUrg == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una hora.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            hospitalizacion.setHora(horaUrg);
            if (hospitalizacion.getEstado() == null || hospitalizacion.getEstado().isBlank()) {
                hospitalizacion.setEstado(Constantes.ESTADO_HOSPITALIZACION_INTERNADO);
            }
        }

        if (hospitalizacion.getIdHospitalizacion() == null) {
            String resumen = construirResumenConfirmacionHospitalizacion(mascotaSeleccionada, vet, fechaIngreso, slotSel);
            int r = JOptionPane.showConfirmDialog(this,
                    resumen,
                    "Confirmar hospitalización",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            boolean ok;
            if (hospitalizacion.getIdHospitalizacion() == null) {
                if (modoUrgencia) {
                    ok = hospitalizacionService.registrarIngresoUrgencia(hospitalizacion);
                } else {
                    if (hospitalizacion.getEstado() == null || hospitalizacion.getEstado().isBlank()) {
                        hospitalizacion.setEstado(Constantes.ESTADO_HOSPITALIZACION_PENDIENTE);
                    }
                    ok = hospitalizacionService.reservarIngreso(hospitalizacion, slotSel.getIdSlot());
                }
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Hospitalización registrada con éxito.");
                }
            } else {
                if (slotSel == null) {
                    JOptionPane.showMessageDialog(this, "Seleccione un turno para actualizar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ok = hospitalizacionService.actualizarIngresoReasignandoSlot(hospitalizacion, slotSel.getIdSlot());
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Hospitalización actualizada con éxito.");
                }
            }

            if (!ok) {
                if (!modoUrgencia) {
                    JOptionPane.showMessageDialog(this, "No se pudo reservar el horario seleccionado (puede haberse ocupado).\nActualizá la lista y elegí otro.", "Sin disponibilidad", JOptionPane.WARNING_MESSAGE);
                    cargarTurnosComboParaFechaHospitalizacion(fechaIngreso);
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo registrar la urgencia.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                }
                return;
            }

            cargarHospitalizacionesEnTabla();
            mostrarSoloListado();
            btnAgendarHospitalizacion.setEnabled(true);
            modoUrgencia = false;
            aplicarModoUrgenciaUI(false);
            // Editar/Cancelar dependerán de la selección
            actualizarBotonesPorSeleccion();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar la Hospitalización: " + e.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        hospitalizacion = new Hospitalizacion();
        mostrarSoloListado();

        modoUrgencia = false;
        aplicarModoUrgenciaUI(false);
        actualizarBotonesPorSeleccion();
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void jcbTurnoExtracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbTurnoExtracionActionPerformed
        try {
            int idx = jcbTurnoExtracion.getSelectedIndex();
            if (idx <= 0) {
                lbTurnoExtraccion.setText("Turno: *");
                return;
            }
            String label = (String) jcbTurnoExtracion.getSelectedItem();
            if (label == null || label.isBlank()) {
                lbTurnoExtraccion.setText("Turno: *");
                return;
            }
            lbTurnoExtraccion.setText("Turno: *  " + label);
        } catch (Exception ignore) {
        }
    }//GEN-LAST:event_jcbTurnoExtracionActionPerformed

    private void btnRegistrarIngresoHospitalizacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarIngresoHospitalizacionActionPerformed
        String estadoSel = getEstadoSeleccionado();
        if (estadoSel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para registrar ingreso.");
            return;
        }
        if (!Constantes.ESTADO_HOSPITALIZACION_PENDIENTE.equals(estadoSel)) {
            JOptionPane.showMessageDialog(this, "Solo se puede registrar ingreso si el estado es Pendiente.", "Acción no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (editarHospitalizacionSeleccionada()) {
            modoEdicion = true;
            jpHeader.setVisible(false);
            //deshabilitarHeader();
            jpListaHospitalizaciones.setVisible(false);

            // Mostrar datos de la agenda (Dueño/Mascota/Motivo/Veterinario/Fecha/Turno) precargados
            // pero en modo SOLO LECTURA para el flujo de Registrar Ingreso.
            jpAgendarHospitalizacion.setVisible(true);
            jpRegistrarIngresos.setVisible(true);
            jpAltaPaciente.setVisible(false);
            jpButtonPanel.setVisible(true);

            // Bloquear edición de datos de agenda: en este flujo sólo se registra el ingreso
            // (diagnóstico), manteniendo el resto como referencia.
            try {
                jcbClienteDueño.setEnabled(false);
                jcbPaciente.setEnabled(false);
                jcbMotivo.setEnabled(false);
                jcbVeterinario.setEnabled(false);
                jdcFechaTurnoHospitalizacion.setEnabled(false);
                jdcFechaDeIngreso.setEnabled(false);
                jcbTurnoExtracion.setEnabled(false);
                lbTituloAgendaHospitalizacion.setText("Datos del Paciente");
            } catch (Exception ignore) {
            }

            // En registrar ingreso, habilitamos campos clínicos
            taDiagnosticoHospitalizacion.setEnabled(true);
            // Regla: en "Registrar Ingreso" solo se actualiza Diagnóstico
            taTratamiento.setEnabled(false);
            spHoraHospitalizacion.setEnabled(false);
            jdcFechaDeIngreso.setEnabled(false);
        }
    }//GEN-LAST:event_btnRegistrarIngresoHospitalizacionActionPerformed

    private void btnAltaPacienteHospitalizacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAltaPacienteHospitalizacionActionPerformed
        String estadoSel = getEstadoSeleccionado();
        if (estadoSel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para dar el alta.");
            return;
        }
        if (!Constantes.ESTADO_HOSPITALIZACION_INTERNADO.equals(estadoSel)) {
            JOptionPane.showMessageDialog(this, "Solo se puede dar el alta si el estado es Internado.", "Acción no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (editarHospitalizacionSeleccionada()) {
            modoEdicion = true;
            jpHeader.setVisible(false);
            //deshabilitarHeader();
            jpListaHospitalizaciones.setVisible(false);
            jpAgendarHospitalizacion.setVisible(false);
            jpRegistrarIngresos.setVisible(false);
            jpAltaPaciente.setVisible(true);
            jpButtonPanel.setVisible(true);
            // En alta, habilitar fecha alta (si aplica)
            configurarRestriccionFechaAlta();
            spFechaHoraAlta.setEnabled(true);
            taTratamiento.setEnabled(true);
        }
    }//GEN-LAST:event_btnAltaPacienteHospitalizacionActionPerformed

    private void btnImprimirRegistroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirRegistroActionPerformed
        btnImprimirRegistroAction();
    }//GEN-LAST:event_btnImprimirRegistroActionPerformed

    private void btnVerRegistroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerRegistroActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnVerRegistroActionPerformed

    // -------------------- Registro: ver / imprimir --------------------
    private Hospitalizacion obtenerHospitalizacionSeleccionadaDesdeTabla() {
        int fila = tableHospitalizaciones.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            int modelRow = tableHospitalizaciones.convertRowIndexToModel(fila);
            Integer id = (Integer) tableHospitalizaciones.getModel().getValueAt(modelRow, 1);
            Hospitalizacion h = hospitalizacionControlador.buscarHospitalizacionPorId(id);
            if (h == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return h;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el registro seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void btnVerRegistroAction() {
        Hospitalizacion h = obtenerHospitalizacionSeleccionadaDesdeTabla();
        if (h == null) {
            return;
        }
        RegistroHospitalizacionDialog.mostrar(SwingUtilities.getWindowAncestor(this), h);
    }

    private void btnImprimirRegistroAction() {
        Hospitalizacion h = obtenerHospitalizacionSeleccionadaDesdeTabla();
        if (h == null) {
            return;
        }

        // 1. Armamos los parámetros del reporte (manteniendo tus claves)
        ReporteRequest req = new ReporteRequest()
                .put("hospitalizacion", h)
                .put("usuario", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 AL EJECUTOR: Limpio de "new", usa el Singleton del profesor y corre con cartel de carga
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.HOSPITALIZACION_REGISTRO, req);
    }

    private String construirResumenConfirmacionHospitalizacion(Mascota mascota, Usuario vet, LocalDate fechaIngreso, AgendaSlot slotSel) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<b>Vas a programar una nueva hospitalización</b><br><br>");

        // Mascota + Cliente
        String mascotaTxt = (mascota != null)
                ? (valorSeguro(mascota.getNombre()) + " (" + valorSeguro(mascota.getEspecie()) + (mascota.getRaza() != null && !mascota.getRaza().isBlank() ? " - " + mascota.getRaza() : "") + ")")
                : "-";
        String clienteTxt = "-";
        try {
            if (mascota != null && mascota.getCliente() != null) {
                clienteTxt = valorSeguro(mascota.getCliente().getPersona().getNombre()) + (mascota.getCliente().getPersona().getApellido() != null ? " " + mascota.getCliente().getPersona().getApellido() : "");
                clienteTxt = clienteTxt.trim();
            }
        } catch (Exception ignore) {
        }

        sb.append("<b>Cliente:</b> ").append(escapeHtml(clienteTxt)).append("<br>");
        sb.append("<b>Mascota:</b> ").append(escapeHtml(mascotaTxt)).append("<br>");

        // Motivo
        String motivo = (jcbMotivo.getSelectedIndex() > 0) ? String.valueOf(jcbMotivo.getSelectedItem()) : "-";
        sb.append("<b>Motivo:</b> ").append(escapeHtml(motivo)).append("<br>");

        // Fecha/Hora/Veterinario (slot o urgencia)
        LocalDate f = fechaIngreso;
        LocalTime h = null;
        Usuario v = vet;

        if (!modoUrgencia && slotSel != null) {
            try {
                f = slotSel.getFecha();
                h = slotSel.getHoraInicio();
                v = slotSel.getVeterinario();
            } catch (Exception ignore) {
            }
        } else {
            h = getHoraSpinnerHospitalizacion();
        }

        // Si el usuario seleccionó un veterinario en el combo, ese es el "source of truth" para el resumen
        try {
            Usuario vetSel = getVeterinarioSeleccionado();
            if (vetSel != null) {
                v = vetSel;
            }
        } catch (Exception ignore) {
        }

        sb.append("<b>Ingreso:</b> ").append(escapeHtml(formatearFecha(f))).append(" ").append(escapeHtml(formatearHora(h))).append("<br>");
        sb.append("<b>Veterinario:</b> ").append(escapeHtml(nombreVisibleUsuario(v))).append("<br>");

        // Estado previsto
        String estadoPrev = modoUrgencia ? Constantes.ESTADO_HOSPITALIZACION_INTERNADO : Constantes.ESTADO_HOSPITALIZACION_PENDIENTE;
        sb.append("<b>Estado inicial:</b> ").append(escapeHtml(estadoPrev)).append("<br>");

        // Diagnóstico / Tratamiento (vista previa)
        String diag = taDiagnosticoHospitalizacion.getText();
        String trat = taTratamiento.getText();
        if (diag != null && !diag.isBlank()) {
            sb.append("<br><b>Diagnóstico:</b><br>")
                    .append(escapeHtml(recortar(diag, 220))).append("<br>");
        }
        if (trat != null && !trat.isBlank()) {
            sb.append("<br><b>Tratamiento:</b><br>")
                    .append(escapeHtml(recortar(trat, 220))).append("<br>");
        }

        sb.append("<br>¿Confirmás el registro?</html>");
        return sb.toString();
    }

    private String valorSeguro(String s) {
        return (s == null || s.isBlank()) ? "-" : s.trim();
    }

    private String recortar(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String formatearFecha(LocalDate d) {
        if (d == null) {
            return "-";
        }
        try {
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return String.valueOf(d);
        }
    }

    private String formatearHora(LocalTime t) {
        if (t == null) {
            return "-";
        }
        try {
            return t.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return String.valueOf(t);
        }
    }

    private String nombreVisibleUsuario(Usuario u) {
        if (u == null) {
            return "-";
        }
        try {
            if (u.getPersona() != null) {
                String n = u.getPersona().getNombre() != null ? u.getPersona().getNombre() + " " : " ";
                String m = u.getPersona().getApellido() != null ? u.getPersona().getApellido() : " ";
                n = n.trim() + m.trim();
                if (!n.isBlank()) {
                    return n;
                }
            }
        } catch (Exception ignore) {
        }
        try {
            if (u.getNombreUsuario() != null && !u.getNombreUsuario().isBlank()) {
                return u.getNombreUsuario();
            }
        } catch (Exception ignore) {
        }
        return u.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgendarHospitalizacion;
    private javax.swing.JButton btnAltaPacienteHospitalizacion;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCancelarHospitalizacion;
    private javax.swing.JButton btnEditarHospitalizacion;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnImprimirRegistro;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnRegistrarIngresoHospitalizacion;
    private javax.swing.JButton btnVerRegistro;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JComboBox jcbClienteDueño;
    private javax.swing.JComboBox<String> jcbFiltroEstado;
    private javax.swing.JComboBox<String> jcbMotivo;
    private javax.swing.JComboBox jcbPaciente;
    private javax.swing.JComboBox jcbTurnoExtracion;
    private javax.swing.JComboBox<String> jcbVeterinario;
    private com.toedter.calendar.JDateChooser jdcFechaDeIngreso;
    private com.toedter.calendar.JDateChooser jdcFechaTurnoHospitalizacion;
    private javax.swing.JPanel jpAgendarHospitalizacion;
    private javax.swing.JPanel jpAltaPaciente;
    private javax.swing.JPanel jpButtonPanel;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaHospitalizaciones;
    private javax.swing.JPanel jpRegistrarIngresos;
    private javax.swing.JLabel lbAltaPaciente;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbBusqueda;
    private javax.swing.JLabel lbCliente;
    private javax.swing.JLabel lbDiagnostico;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbFechaExtracción;
    private javax.swing.JLabel lbFechaIngreso;
    private javax.swing.JLabel lbFechaNacimiento1;
    private javax.swing.JLabel lbHora;
    private javax.swing.JLabel lbInternaciones;
    private javax.swing.JLabel lbListaDeHospitalizaciones;
    private javax.swing.JLabel lbMotivo;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbRegistarIngresos;
    private javax.swing.JLabel lbTituloAgendaHospitalizacion;
    private javax.swing.JLabel lbTratamiento;
    private javax.swing.JLabel lbTurnoExtraccion;
    private javax.swing.JLabel lbVeterinarioAsignado;
    private javax.swing.JSpinner spFechaHoraAlta;
    private javax.swing.JSpinner spHoraHospitalizacion;
    private javax.swing.JScrollPane spTableHospitalizaciones;
    private javax.swing.JTextArea taDiagnosticoHospitalizacion;
    private javax.swing.JTextArea taTratamiento;
    private javax.swing.JTable tableHospitalizaciones;
    private javax.swing.JTextField txtBusqueda;
    // End of variables declaration//GEN-END:variables
}
