package veterinaria.vista;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import veterinaria.controlador.MascotaControlador;
import veterinaria.controlador.VisitaControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.Visita;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.util.Constantes;
import veterinaria.util.ManejoTablas;
import veterinaria.util.UtilidadesTabla;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.MascotaComboSupport;
import veterinaria.util.ui.HistoriaEventoAbrible;
import veterinaria.vista.application.Application;

public class FormConsulta extends javax.swing.JPanel implements HistoriaEventoAbrible {

    private List<Map.Entry<Integer, String>> listaVisitaMascota = new ArrayList<>();
    private List<Map.Entry<Integer, Cliente>> listaClienteMascota = new ArrayList<>();
    private final MascotaControlador mascotaControlador = new MascotaControlador();
    private Mascota mascotaSeleccionada;
    private Visita visita = new Visita();
    private final VisitaControlador visitaControlador = new VisitaControlador();
    private List<Usuario> listaVeterinarios = new ArrayList<>();
    private boolean listenersTablaInicializados = false;
    private boolean cargandoTabla = false;

    /**
     * Indica si este formulario fue abierto desde Historia Clínica
     * (btnVerHistoria). En ese caso, algunas acciones como "Limpiar" deben
     * quedar deshabilitadas para evitar perder el contexto del evento.
     */
    private boolean origenHistoriaClinica = false;

    // Coincidencias exclusivamente en los campos de búsqueda clínicos.
    private static final int[] COLUMNAS_BUSQUEDA = {2, 3, 4, 9};
    private veterinaria.util.PaginacionTablas paginacionConsultas;
    private javax.swing.JButton btnCambiarEstado;
    private final javax.swing.JComboBox<String> prioridadConsulta =
            new javax.swing.JComboBox<>(new String[]{"CONTROL", "URGENCIA", "EMERGENCIA"});
    private javax.swing.JButton btnLlamarSiguiente;
    private javax.swing.JButton btnVerInfo;
    private javax.swing.JButton btnQuitarSeleccion;
    private javax.swing.JPanel herramientasConsultas;
    private javax.swing.JButton btnFinalizarDesdeFicha;
    private javax.swing.JButton btnGuardarEnObservacion;
    private javax.swing.JButton btnRetomarAtencion;
    private final javax.swing.JLabel resumenCola = new javax.swing.JLabel("En espera: 0  |  Atendiendo: 0");
    private javax.swing.JButton btnActualizarConsultas;
    private javax.swing.JLabel contadorTratamiento;
    private final javax.swing.JTextField txtEspecieConsulta = new javax.swing.JTextField();
    private final javax.swing.JTextField txtRazaConsulta = new javax.swing.JTextField();
    private final javax.swing.JTextField txtPesoConsulta = new javax.swing.JTextField();
    private final javax.swing.JTextField txtOtroMotivo = new javax.swing.JTextField();
    private final javax.swing.JTextField txtOtraPatologia = new javax.swing.JTextField();
    private javax.swing.JPanel panelEvolucionConsulta;
    private final javax.swing.JComboBox<String> comboConsultaOrigen =
            new javax.swing.JComboBox<>();
    private final java.util.List<Integer> idsConsultasOrigen = new java.util.ArrayList<>();
    private final com.toedter.calendar.JDateChooser fechaProximoControl =
            new com.toedter.calendar.JDateChooser();
    private javax.swing.JPanel panelOrigenConsulta;
    private javax.swing.JPanel panelFechaControl;
    private static final String OPCION_OTROS = "Otro/s";
    private static final int MAX_TEXTO_CATALOGO = 200;
    private static final int MAX_CARACTERES_EVOLUCION = 10000;
    private javax.swing.Timer actualizacionCola;
    private boolean comprobandoCambios;
    private String ultimaFirmaConsultas = "";

    public FormConsulta() {
        initComponents();
        configurarPresentacionConsultas();
        PermisoUI.aplicar(this);
        configurarAccionesPorRol();

        // Estado inicial de acciones
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);

        // Estado por defecto (cuando no viene desde Historia Clínica)
        aplicarEstadoOrigenHistoriaClinica();

        // Botón imprimir (registro) se muestra solo si hay una fila seleccionada
        btnImprimirVisita.setVisible(false);

        configurarComboEstado();
        jpDatos.setVisible(false);
        configurarSpinnerHora(null);
        jdcFecha.setDate(null);
        llenarComboBoxPatologias();
        llenarComboBoxMotivos();
        cargarCombosBoxs();
        cargarComboVeterinarios();
        cargarVisitasEnTabla();
        iniciarActualizacionCola();

        initListeners();

        // Los anchos se mantienen proporcionados: no se calculan por textos extensos.

        // Tooltips por columna (descripción)
        ManejoTablas mt = new ManejoTablas();
        mt.aplicarTooltipsPorColumna(tableVisitas, Map.ofEntries(
                Map.entry(0, "Seleccionar una visita para editar o eliminar"),
                Map.entry(1, "ID de la visita"),
                Map.entry(2, "Paciente (Mascota)"),
                Map.entry(3, "Cliente / Dueño"),
                Map.entry(4, "Motivo de la consulta"),
                Map.entry(5, "Patología / Diagnóstico"),
                Map.entry(6, "Tratamiento indicado"),
                Map.entry(7, "Fecha de la visita"),
                Map.entry(8, "Estado (pendiente / finalizada / etc.)"),
                Map.entry(9, "Veterinario que atendió"),
                Map.entry(10, "Prioridad de atención")
        ));

        // Tooltips con texto completo en columnas de texto largo
        ManejoTablas.tooltipValorEnColumnas(tableVisitas, Arrays.asList(3, 4, 5, 6, 7));

        // Click en fila => marcar automáticamente el checkbox (como en otros formularios)
        tableVisitas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                seleccionarFilaDesdeClick(evt);
            }
        });
    }

    /**
     * Llamado por el router de Historia Clínica para ajustar el estado del
     * formulario.
     */
    public void setOrigenHistoriaClinica(boolean desdeHistoriaClinica) {
        this.origenHistoriaClinica = desdeHistoriaClinica;
        aplicarEstadoOrigenHistoriaClinica();
    }

    private void aplicarEstadoOrigenHistoriaClinica() {
        try {
            if (btnLimpiar != null) {
                btnLimpiar.setEnabled(!origenHistoriaClinica);
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
    }

    /**
     * Abre una visita puntual (modo lectura) desde Historia Clínica.
     */
    @Override
    public void abrirDetallePorId(Integer refId) {

        if (refId == null) {
            return;
        }

        // Buscar directamente la visita en la base de datos
        Visita visita = visitaControlador.buscarVisitaPorId(refId);

        if (visita == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró la visita.",
                    "No encontrado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Cargar la visita en el formulario
        if (!editarVisita(visita)) {
            return;
        }

        jpListaVisitas.setVisible(false);
        jpDatos.setVisible(true);

        // Modo solo lectura
        btnNuevo.setEnabled(false);
        btnGuardar.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);

        bloquearFormularioLectura();
    }

    private Integer buscarFilaPorIdEnTabla(Integer idVisita) {

        if (idVisita == null) {
            return null;
        }

        try {

            for (int viewRow = 0; viewRow < tableVisitas.getRowCount(); viewRow++) {

                int modelRow = tableVisitas.convertRowIndexToModel(viewRow);

                Object val = tableVisitas.getModel().getValueAt(modelRow, 1); // ID Visita

                System.out.println("ID en tabla: " + val + " | Buscando: " + idVisita);

                if (val != null && idVisita.toString().equals(val.toString())) {
                    return viewRow;
                }

            }

        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        return null;
    }

    private void bloquearFormularioLectura() {
        try {
            jcbPaciente.setEnabled(false);
            jdcFecha.setEnabled(false);
            spinnerHora.setEnabled(false);
            jcbMotivo.setEnabled(false);
            jcbPatologia.setEnabled(false);
            txtTratamiento.setEditable(false);
            jcbEstado.setEnabled(false);
            jcbVeterinario.setEnabled(false);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
    }

    private void cargarCombosBoxs() {
        listaVisitaMascota.clear();
        listaClienteMascota.clear();
        List<Mascota> mascotas = mascotaControlador.buscarTodasLasMascotas();
        jcbPaciente.removeAllItems();
        jcbPaciente.addItem("Seleccionar Mascota");

        for (Mascota mascota : mascotas) {
            Integer idMascota = mascota.getIdMascota();
            Cliente cargaCliente = mascota.getCliente();

            String display = MascotaComboSupport.buildMascotaDisplay(mascota);
            // Avisar antes del registro: un paciente inactivo requiere
            // URGENCIA o EMERGENCIA. No se modifica su estado ni la búsqueda por ID.
            if (!mascota.isActivo()) display += "  (Inactivo)";
            listaVisitaMascota.add(new AbstractMap.SimpleEntry<>(idMascota, display));
            listaClienteMascota.add(new AbstractMap.SimpleEntry<>(idMascota, cargaCliente));
        }

        for (Map.Entry<Integer, String> entry : listaVisitaMascota) {
            jcbPaciente.addItem(entry.getValue());
        }

        jcbPaciente.addActionListener(e -> enPacienteSeleccionado());
    }

    private void cargarComboVeterinarios() {
        try {
            listaVeterinarios = new UsuarioDAO().obtenerUsuariosVeterinarios();
            javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
            model.addElement("Asignación automática");
            for (Usuario u : listaVeterinarios) {
                model.addElement(nombreCompletoUsuario(u));
            }
            jcbVeterinario.setModel(model);

        } catch (Exception e) {
            jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Asignación automática"}));
            listaVeterinarios = new ArrayList<>();
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
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
        return (u.getNombreUsuario() != null) ? u.getNombreUsuario() : "";
    }

    private void seleccionarVeterinarioEnCombo(String nombreGuardado) {
        if (nombreGuardado == null || nombreGuardado.trim().isEmpty()) {
            jcbVeterinario.setSelectedIndex(0);
            return;
        }
        for (int i = 1; i < jcbVeterinario.getItemCount(); i++) {
            String item = jcbVeterinario.getItemAt(i);
            if (nombreGuardado.equalsIgnoreCase(item)) {
                jcbVeterinario.setSelectedIndex(i);
                return;
            }
        }
        jcbVeterinario.setSelectedIndex(0);
    }

    private void enPacienteSeleccionado() {
        if (visita == null || visita.getIdVisita() == null)
            txtPesoConsulta.setText("");
        String pacienteSelec = (String) jcbPaciente.getSelectedItem();

        if (pacienteSelec == null || "Seleccionar Mascota".equals(pacienteSelec)) {
            mascotaSeleccionada = null;
            txtDueño.setText("");
            txtEspecieConsulta.setText("");
            txtRazaConsulta.setText("");
            cargarConsultasDeSeguimiento();
            return;
        }

        for (Map.Entry<Integer, String> entry : listaVisitaMascota) {
            if (entry != null && pacienteSelec.equals(entry.getValue())) {
                mascotaSeleccionada = mascotaControlador.buscarMascotaPorId(entry.getKey());
                if (mascotaSeleccionada != null
                        && mascotaSeleccionada.getCliente() != null
                        && mascotaSeleccionada.getCliente().getPersona() != null) {
                    String cliente = (mascotaSeleccionada.getCliente().getPersona().getNombre() != null
                            ? mascotaSeleccionada.getCliente().getPersona().getNombre()
                            : "")
                            + " "
                            + (mascotaSeleccionada.getCliente().getPersona().getApellido() != null
                            ? mascotaSeleccionada.getCliente().getPersona().getApellido()
                            : "");
                    txtDueño.setText(cliente.trim());
                } else {
                    txtDueño.setText("");
                }
                txtEspecieConsulta.setText(mascotaSeleccionada == null ? ""
                        : textoSeguro(mascotaSeleccionada.getEspecie()));
                txtRazaConsulta.setText(mascotaSeleccionada == null ? ""
                        : textoSeguro(mascotaSeleccionada.getRaza()));
                cargarConsultasDeSeguimiento();
                return;
            }
        }

        // Si no se encuentra, limpiar.
        mascotaSeleccionada = null;
        txtDueño.setText("");
        txtEspecieConsulta.setText("");
        txtRazaConsulta.setText("");
        cargarConsultasDeSeguimiento();
    }

    private void seleccionarMascotaEnCombo(Integer idMascota) {
        if (idMascota == null) {
            jcbPaciente.setSelectedIndex(0);
            return;
        }
        // index 0 = placeholder
        for (int i = 0; i < listaVisitaMascota.size(); i++) {
            Map.Entry<Integer, String> entry = listaVisitaMascota.get(i);
            if (entry != null && idMascota.equals(entry.getKey())) {
                jcbPaciente.setSelectedIndex(i + 1);
                return;
            }
        }
        // Si no se encuentra, dejar placeholder.
        jcbPaciente.setSelectedIndex(0);
    }

    private void cargarConsultasDeSeguimiento() {
        Integer seleccionAnterior = null;
        int indiceAnterior = comboConsultaOrigen.getSelectedIndex();
        if (indiceAnterior >= 0 && indiceAnterior < idsConsultasOrigen.size())
            seleccionAnterior = idsConsultasOrigen.get(indiceAnterior);

        comboConsultaOrigen.removeAllItems();
        idsConsultasOrigen.clear();
        comboConsultaOrigen.addItem("Sin vinculación");
        idsConsultasOrigen.add(null);
        if (mascotaSeleccionada == null || mascotaSeleccionada.getIdMascota() == null)
            return;
        try {
            java.time.format.DateTimeFormatter formato =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.util.List<Visita> anteriores = visitaControlador.obtenerTodasLasVisitas();
            anteriores.stream()
                    .filter(v -> v.getIdVisita() != null && v.getMascota() != null
                            && mascotaSeleccionada.getIdMascota().equals(v.getMascota().getIdMascota())
                            && "FINALIZADO".equalsIgnoreCase(v.getEstado()))
                    .sorted(java.util.Comparator.comparing(Visita::getIdVisita).reversed())
                    .forEach(v -> {
                        String fecha = v.getFecha() == null ? "Sin fecha" : formato.format(v.getFecha());
                        comboConsultaOrigen.addItem("N.º " + v.getIdVisita() + " - " + fecha
                                + " - " + textoSeguro(v.getMotivoVisita()));
                        idsConsultasOrigen.add(v.getIdVisita());
                    });
            if (seleccionAnterior != null) {
                int i = idsConsultasOrigen.indexOf(seleccionAnterior);
                if (i >= 0) comboConsultaOrigen.setSelectedIndex(i);
            }
        } catch (RuntimeException ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class,
                    "No se pudieron consultar antecedentes de seguimiento", ex);
        }
    }

    private Integer obtenerConsultaOrigenSeleccionada() {
        int index = comboConsultaOrigen.getSelectedIndex();
        return index >= 0 && index < idsConsultasOrigen.size()
                ? idsConsultasOrigen.get(index) : null;
    }

    private LocalDate fechaRecomendadaIngresada() {
        java.util.Date fecha = fechaProximoControl.getDate();
        if (fecha == null) {
            String escrito = fechaProximoControl.getDateEditor().getUiComponent() instanceof javax.swing.text.JTextComponent
                    ? ((javax.swing.text.JTextComponent) fechaProximoControl.getDateEditor()
                            .getUiComponent()).getText().trim() : "";
            if (!escrito.isEmpty())
                throw new IllegalArgumentException("Ingrese una fecha válida mediante el calendario.");
            return null;
        }
        LocalDate control = fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (control.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("El próximo control no puede tener fecha pasada.");
        return control;
    }

    private void llenarComboBoxPatologias() {
        // jcbPatologia.removeAllItems();
        jcbPatologia.addItem("Control");
        // Enfermedades Infecciosas
        jcbPatologia.addItem("Parvovirus");
        jcbPatologia.addItem("Moquillo canino");
        jcbPatologia.addItem("Leptospirosis");

        // Enfermedades Metabólicas
        jcbPatologia.addItem("Diabetes Mellitus");
        jcbPatologia.addItem("Síndrome de Cushing");
        jcbPatologia.addItem("Hipotiroidismo");

        // Enfermedades del Aparato Locomotor
        jcbPatologia.addItem("Displasia de cadera");
        jcbPatologia.addItem("Osteoartritis");
        jcbPatologia.addItem("Luxaciones y fracturas");

        // Enfermedades del Sistema Cardiovascular
        jcbPatologia.addItem("Cardiomiopatía dilatada");
        jcbPatologia.addItem("Enfermedad valvular degenerativa");
        jcbPatologia.addItem("Insuficiencia cardíaca congestiva");

        // Enfermedades del Sistema Respiratorio
        jcbPatologia.addItem("Asma felina");
        jcbPatologia.addItem("Tos de las perreras");
        jcbPatologia.addItem("Neumonía");

        // Enfermedades del Sistema Digestivo
        jcbPatologia.addItem("Gastritis");
        jcbPatologia.addItem("Pancreatitis");
        jcbPatologia.addItem("Colitis");

        // Enfermedades del Sistema Nervioso
        jcbPatologia.addItem("Epilepsia");
        jcbPatologia.addItem("Meningitis");
        jcbPatologia.addItem("Encefalitis");

        // Enfermedades del Sistema Urinario
        jcbPatologia.addItem("Insuficiencia renal crónica");
        jcbPatologia.addItem("Cálculos urinarios");
        jcbPatologia.addItem("Cistitis");

        // Enfermedades del Sistema Reproductivo
        jcbPatologia.addItem("Piometra");
        jcbPatologia.addItem("Mastitis");
        jcbPatologia.addItem("Distocia");

        // Enfermedades Oncológicas
        jcbPatologia.addItem("Tumores mamarios");
        jcbPatologia.addItem("Linfoma");
        jcbPatologia.addItem("Hemangiosarcoma");

        // Enfermedades Dermatológicas
        jcbPatologia.addItem("Dermatitis atópica");
        jcbPatologia.addItem("Alergias alimentarias");
        jcbPatologia.addItem("Demodicosis");

        // Enfermedades Nutricionales
        jcbPatologia.addItem("Deficiencia de calcio");
        jcbPatologia.addItem("Obesidad");
        jcbPatologia.addItem("Hipovitaminosis A");

        // Enfermedades Oculares
        jcbPatologia.addItem("Cataratas");
        jcbPatologia.addItem("Conjuntivitis");
        jcbPatologia.addItem("Glaucoma");
        jcbPatologia.addItem(OPCION_OTROS);
        ordenarCatalogoAlfabeticamente(jcbPatologia);
    }

    /** Orden A-Z en español, manteniendo el placeholder primero y Otro/s al final. */
    private void ordenarCatalogoAlfabeticamente(javax.swing.JComboBox<String> combo) {
        if (combo.getItemCount() <= 2) return;
        String placeholder = combo.getItemAt(0);
        java.util.List<String> valores = new java.util.ArrayList<>();
        for (int i = 1; i < combo.getItemCount(); i++) {
            String valor = combo.getItemAt(i);
            if (valor != null && !OPCION_OTROS.equals(valor))
                valores.add(valor);
        }
        java.text.Collator comparador = java.text.Collator.getInstance(
                new java.util.Locale("es", "AR"));
        comparador.setStrength(java.text.Collator.PRIMARY);
        valores.sort(comparador);
        combo.removeAllItems();
        combo.addItem(placeholder);
        for (String valor : valores) combo.addItem(valor);
        combo.addItem(OPCION_OTROS);
        combo.setSelectedIndex(0);
    }

    private void llenarComboBoxMotivos() {
        jcbMotivo.addItem("Control de Rutina");
        //jcbMotivo.removeAllItems();
        // Motivos relacionados con síntomas o problemas de salud
        jcbMotivo.addItem("Vómitos y diarrea");
        jcbMotivo.addItem("Cojera o dolor al caminar");
        jcbMotivo.addItem("Pérdida de apetito");

        // Motivos relacionados con procedimientos de rutina
        jcbMotivo.addItem("Vacunación");
        jcbMotivo.addItem("Desparasitación");
        jcbMotivo.addItem("Chequeo general");

        // Motivos relacionados con emergencias
        jcbMotivo.addItem("Accidente o trauma");
        jcbMotivo.addItem("Dificultad para respirar");
        jcbMotivo.addItem("Heridas o sangrado");

        // Motivos relacionados con tratamientos anteriores
        jcbMotivo.addItem("Control post-cirugía");
        jcbMotivo.addItem("Evaluación de tratamiento en curso");
        jcbMotivo.addItem("Monitoreo de condición crónica");
        jcbMotivo.addItem(OPCION_OTROS);
        ordenarCatalogoAlfabeticamente(jcbMotivo);
    }

    private void configurarSpinnerHora(LocalTime value) {
        SpinnerDateModel model = new SpinnerDateModel();
        spinnerHora.setModel(model);

        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spinnerHora, "HH:mm");
        spinnerHora.setEditor(timeEditor);

        if (value != null) {
            LocalDate hoy = LocalDate.now();
            Date fechaConHora = Date.from(value.atDate(hoy).atZone(ZoneId.systemDefault()).toInstant());
            spinnerHora.setValue(fechaConHora);
        } else {
            spinnerHora.setValue(new Date());
        }
    }

    private void limpiarCamposFormulario() {
        jcbPaciente.setSelectedIndex(0);
        txtDueño.setText("");
        jcbPatologia.setSelectedIndex(0);
        jcbMotivo.setSelectedIndex(0);
        jcbVeterinario.setSelectedIndex(0);
        prioridadConsulta.setSelectedItem("CONTROL");
        configurarSpinnerHora(null);
        jdcFecha.setDate(new Date());
        txtTratamiento.setText("");
        txtPesoConsulta.setText("");
        txtEspecieConsulta.setText("");
        txtRazaConsulta.setText("");
        txtOtroMotivo.setText("");
        txtOtraPatologia.setText("");
        fechaProximoControl.setDate(null);
        comboConsultaOrigen.removeAllItems();
        idsConsultasOrigen.clear();
        comboConsultaOrigen.addItem("Sin vinculación");
        idsConsultasOrigen.add(null);
        actualizarCamposOtros();
        // Una visita nueva ingresa a la cola en EN_ESPERA.
        try {
            jcbEstado.setSelectedItem(Constantes.EstadoVisita.EN_ESPERA);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
            jcbEstado.setSelectedItem(null);
        }
    }

    /** Reutiliza los controles reales y el tema FlatLaf sin tocar initComponents. */
    private void configurarPresentacionConsultas() {
        // Compartir el mismo patrón aprobado de paginación y búsqueda.
        javax.swing.JPanel tablaPaginada = new javax.swing.JPanel(new java.awt.BorderLayout());
        ((javax.swing.GroupLayout) jpListaVisitas.getLayout()).replace(scroll1, tablaPaginada);
        tablaPaginada.add(scroll1, java.awt.BorderLayout.CENTER);
        paginacionConsultas = new veterinaria.util.PaginacionTablas(
                tableVisitas, txtBusqueda, 15, COLUMNAS_BUSQUEDA, COLUMNAS_BUSQUEDA);
        paginacionConsultas.activarBusquedaPorPrefijo();
        paginacionConsultas.establecerFiltroAdicional(this::coincideEstadoModelo);
        tablaPaginada.add(paginacionConsultas.getControles(), java.awt.BorderLayout.SOUTH);
        txtBusqueda.setLayout(new java.awt.BorderLayout());
        txtBusqueda.add(paginacionConsultas.getBotonLimpiar(), java.awt.BorderLayout.EAST);
        javax.swing.SwingUtilities.invokeLater(() -> paginacionConsultas.activarTamanoAdaptable());

        tableVisitas.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        if (tableVisitas instanceof veterinaria.vista.table.AutoTable)
            ((veterinaria.vista.table.AutoTable) tableVisitas).setAjustarAnchoAutomaticamente(false);
        tableVisitas.setRowHeight(27);
        tableVisitas.getColumnModel().getColumn(0).setCellRenderer(
                new veterinaria.vista.table.TableGradientCheckBoxRenderer());
        // Dar prioridad a Cliente y Motivo: Paciente suele ser un nombre corto.
        // Los índices ocultos (5, 6, 7) se conservan para búsqueda e informes.
        int[] anchos = {84, 52, 122, 225, 205, 95, 185, 115, 135, 148, 112};
        for (int i = 0; i < anchos.length; i++)
            tableVisitas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        tableVisitas.getColumnModel().getColumn(0).setMinWidth(80);
        tableVisitas.getColumnModel().getColumn(1).setMinWidth(45);
        tableVisitas.getColumnModel().getColumn(2).setMinWidth(95);
        tableVisitas.getColumnModel().getColumn(2).setMaxWidth(160);
        tableVisitas.getColumnModel().getColumn(3).setMinWidth(175);
        tableVisitas.getColumnModel().getColumn(4).setMinWidth(150);
        tableVisitas.getColumnModel().getColumn(8).setMinWidth(115);
        tableVisitas.getColumnModel().getColumn(9).setMinWidth(115);
        // Presentación amigable sin alterar los códigos de estado en el modelo/BD.
        tableVisitas.getColumnModel().getColumn(8).setCellRenderer(
                new veterinaria.vista.table.TableGradientCell() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(
                            javax.swing.JTable tabla, Object valor, boolean seleccionado,
                            boolean foco, int fila, int columna) {
                        super.getTableCellRendererComponent(tabla, valor, seleccionado,
                                foco, fila, columna);
                        setText(estadoParaMostrar(valor == null ? "" : valor.toString()));
                        setToolTipText(getText());
                        return this;
                    }
                });
        tableVisitas.getColumnModel().getColumn(1).setHeaderValue("N.º");
        // Los datos clínicos y la fecha se consultan desde Ver info.
        // Conservar los índices originales para no alterar búsqueda ni selección.
        for (int indice : new int[]{5, 6, 7}) {
            javax.swing.table.TableColumn columna = tableVisitas.getColumnModel().getColumn(indice);
            columna.setMinWidth(0);
            columna.setPreferredWidth(0);
            columna.setMaxWidth(0);
        }
        scroll1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll1.setMinimumSize(new java.awt.Dimension(0, 0));
        jpListaVisitas.setMinimumSize(new java.awt.Dimension(0, 0));
        setMinimumSize(new java.awt.Dimension(0, 0));

        btnCambiarEstado = new javax.swing.JButton("Finalizar atención");
        btnRetomarAtencion = new javax.swing.JButton("Retomar");
        btnRetomarAtencion.setToolTipText("Volver a atender una consulta que estaba en observación.");
        btnRetomarAtencion.setEnabled(false);
        btnRetomarAtencion.addActionListener(e -> retomarAtencionSeleccionada());
        btnVerInfo = new javax.swing.JButton("Ver info");
        btnVerInfo.addActionListener(e -> mostrarInformacionConsulta());
        btnVerInfo.setEnabled(false);
        btnQuitarSeleccion = new javax.swing.JButton("×");
        btnQuitarSeleccion.setPreferredSize(new java.awt.Dimension(32, 28));
        btnQuitarSeleccion.setToolTipText("Desmarca la consulta sin modificar su estado.");
        btnQuitarSeleccion.setEnabled(false);
        btnQuitarSeleccion.addActionListener(e -> {
            if (paginacionConsultas != null) paginacionConsultas.limpiarSeleccion();
            tableVisitas.clearSelection();
            actualizarBotonImprimir();
        });
        btnLlamarSiguiente = new javax.swing.JButton("Llamar");
        btnLlamarSiguiente.setToolTipText("Asigna automáticamente el ticket de mayor prioridad disponible.");
        btnLlamarSiguiente.addActionListener(e -> llamarSiguientePaciente());
        btnLlamarSiguiente.setEnabled(usuarioClinico());
        btnCambiarEstado.addActionListener(e -> avanzarEstadoSeleccionado());
        btnCambiarEstado.setEnabled(false);

        // Barra compacta: mismos componentes y estética de la pantalla original.
        jpHeader.removeAll();
        jpHeader.setLayout(new java.awt.BorderLayout(0, 10));
        javax.swing.JPanel titulo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 6));
        titulo.setOpaque(false);
        titulo.add(lbVisitas, java.awt.BorderLayout.NORTH);
        titulo.add(resumenCola, java.awt.BorderLayout.EAST);
        titulo.add(jSeparator1, java.awt.BorderLayout.SOUTH);
        jpHeader.add(titulo, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel herramientas = new javax.swing.JPanel(new java.awt.BorderLayout(12, 0));
        herramientasConsultas = herramientas;
        herramientas.setOpaque(false);
        javax.swing.JPanel filtros = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);
        javax.swing.JPanel buscar = new javax.swing.JPanel(new java.awt.BorderLayout(6, 4));
        buscar.setOpaque(false);
        buscar.add(lbUsuarioBusqueda, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel cajaBuscar = new javax.swing.JPanel(new java.awt.BorderLayout(6, 0));
        cajaBuscar.setOpaque(false);
        cajaBuscar.add(lbBuscar, java.awt.BorderLayout.WEST);
        txtBusqueda.setPreferredSize(new java.awt.Dimension(230, 28));
        cajaBuscar.add(txtBusqueda, java.awt.BorderLayout.CENTER);
        buscar.add(cajaBuscar, java.awt.BorderLayout.CENTER);
        filtros.add(buscar);
        javax.swing.JPanel estado = new javax.swing.JPanel(new java.awt.BorderLayout(0, 4));
        estado.setOpaque(false);
        estado.add(lbEstadoFiltro, java.awt.BorderLayout.NORTH);
        jcbEstadoFiltro.setPreferredSize(new java.awt.Dimension(140, 28));
        resumenCola.setToolTipText("Pacientes en espera de atención y actualmente atendidos");
        // Botón legible incluso cuando la carga del recurso SVG falla.
        btnActualizarConsultas = new javax.swing.JButton("Actualizar");
        btnActualizarConsultas.setPreferredSize(new java.awt.Dimension(88, 28));
        btnActualizarConsultas.setFocusable(false);
        btnActualizarConsultas.setToolTipText("Actualizar consultas");
        btnActualizarConsultas.getAccessibleContext().setAccessibleName("Actualizar consultas");
        btnActualizarConsultas.addActionListener(e -> comprobarActualizaciones(true));
        // Estado queda limpio; actualizar y limpiar selección corresponden a la lista.
        estado.add(jcbEstadoFiltro, java.awt.BorderLayout.CENTER);
        filtros.add(estado);
        herramientas.add(filtros, java.awt.BorderLayout.WEST);

        // Cabecera y tabla comparten un único contenedor: NO reemplazar
        // componentes del GroupLayout generado (provoca huecos verticales).
        jpListaVisitas.removeAll();
        jpListaVisitas.setLayout(new java.awt.BorderLayout(0, 6));
        javax.swing.JPanel cabeceraLista = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
        cabeceraLista.setOpaque(false);
        javax.swing.JPanel accionesLista = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        accionesLista.setOpaque(false);
        accionesLista.add(lbListaDeVisitas);
        accionesLista.add(btnActualizarConsultas);
        accionesLista.add(btnQuitarSeleccion);
        cabeceraLista.add(accionesLista, java.awt.BorderLayout.WEST);
        cabeceraLista.add(btnImprimirListaVisitas, java.awt.BorderLayout.EAST);
        cabeceraLista.add(jSeparator6, java.awt.BorderLayout.SOUTH);
        cabeceraLista.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 0, 10));
        jpListaVisitas.add(cabeceraLista, java.awt.BorderLayout.NORTH);
        // Mantener el contenedor paginado: incluye la tabla y los controles inferiores.
        // Agregar directamente scroll1 lo desprendía de tablaPaginada y ocultaba la paginación.
        tablaPaginada.setOpaque(false);
        jpListaVisitas.add(tablaPaginada, java.awt.BorderLayout.CENTER);
        jpListaVisitas.revalidate();
        jpListaVisitas.repaint();

        // Dos filas por función: evita controles encimados en ventanas reducidas.
        javax.swing.JPanel acciones = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        acciones.setOpaque(false);
        javax.swing.JPanel accionesRecepcion = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        accionesRecepcion.setOpaque(false);
        accionesRecepcion.add(btnNuevo);
        accionesRecepcion.add(btnVerInfo);
        accionesRecepcion.add(btnEliminar);
        accionesRecepcion.add(btnImprimirVisita);
        javax.swing.JPanel accionesClinicas = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        accionesClinicas.setOpaque(false);
        accionesClinicas.add(btnLlamarSiguiente);
        accionesClinicas.add(btnRetomarAtencion);
        accionesClinicas.add(btnEditar);
        // Finalización disponible desde la ficha clínica; no duplicar la acción en la barra.
        acciones.add(accionesRecepcion);
        acciones.add(accionesClinicas);
        herramientas.add(acciones, java.awt.BorderLayout.EAST);
        jpHeader.add(herramientas, java.awt.BorderLayout.CENTER);
        jpHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        jpHeader.revalidate();
        jpHeader.repaint();

        configurarFormularioAmplio();

    }

    private javax.swing.JPanel campoConsulta(javax.swing.JLabel etiqueta,
            java.awt.Component control) {
        javax.swing.JPanel campo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 7));
        campo.setOpaque(false);
        campo.add(etiqueta, java.awt.BorderLayout.NORTH);
        campo.add(control, java.awt.BorderLayout.CENTER);
        campo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return campo;
    }

    /** Mantiene los controles arriba sin estirarlos al aparecer el detalle Otro/s. */
    private javax.swing.JPanel campoConsultaAlineado(javax.swing.JLabel etiqueta,
            java.awt.Component control) {
        javax.swing.JPanel alineado = new javax.swing.JPanel(new java.awt.BorderLayout());
        alineado.setOpaque(false);
        alineado.add(campoConsulta(etiqueta, control), java.awt.BorderLayout.NORTH);
        return alineado;
    }

    /** Distribución funcional, sin alterar colores ni componentes FlatLaf existentes. */
    private void configurarFormularioAmplio() {
        jpDatos.removeAll();
        jpDatos.setLayout(new java.awt.BorderLayout(0, 12));
        jpDatos.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 28, 14, 28));

        javax.swing.JPanel cabecera = new javax.swing.JPanel(new java.awt.BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(lbDatos, java.awt.BorderLayout.WEST);
        javax.swing.JPanel prioridad = new javax.swing.JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.RIGHT, 10, 0));
        prioridad.setOpaque(false);
        prioridad.add(new javax.swing.JLabel("Prioridad de atención:"));
        prioridadConsulta.setPreferredSize(new java.awt.Dimension(170, 32));
        prioridad.add(prioridadConsulta);
        cabecera.add(prioridad, java.awt.BorderLayout.EAST);
        jpDatos.add(cabecera, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel centro = new javax.swing.JPanel();
        centro.setLayout(new javax.swing.BoxLayout(centro, javax.swing.BoxLayout.Y_AXIS));
        centro.setOpaque(false);

        javax.swing.JPanel filaUno = new javax.swing.JPanel(new java.awt.GridLayout(1, 3, 18, 0));
        filaUno.setOpaque(false);
        filaUno.add(campoConsultaAlineado(lbPaciente, jcbPaciente));
        filaUno.add(campoConsultaAlineado(lbDueño, txtDueño));
        txtOtroMotivo.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Especifique el motivo");
        txtOtroMotivo.setToolTipText("Especifique el motivo cuando selecciona Otro/s.");
        javax.swing.JPanel motivoConOtro = comboConOpcionOtro(jcbMotivo, txtOtroMotivo);
        filaUno.add(campoConsultaAlineado(lbMotivo, motivoConOtro));
        filaUno.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 97));
        filaUno.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        centro.add(filaUno);
        centro.add(javax.swing.Box.createVerticalStrut(10));

        javax.swing.JPanel filaDos = new javax.swing.JPanel(new java.awt.GridLayout(1, 3, 18, 0));
        filaDos.setOpaque(false);
        filaDos.add(campoConsultaAlineado(lbVeterinarioAsignado, jcbVeterinario));
        filaDos.add(campoConsultaAlineado(lbEstado, jcbEstado));
        txtOtraPatologia.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Especifique el diagnóstico");
        txtOtraPatologia.setToolTipText("Especifique el diagnóstico cuando selecciona Otro/s.");
        filaDos.add(campoConsultaAlineado(lbPatologia, comboConOpcionOtro(jcbPatologia, txtOtraPatologia)));
        filaDos.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 97));
        filaDos.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        centro.add(filaDos);
        centro.add(javax.swing.Box.createVerticalStrut(8));
        // Datos de paciente: la especie y raza se leen desde Mascota; el peso
        // corresponde exclusivamente a esta atención.
        javax.swing.JPanel filaPaciente = new javax.swing.JPanel(new java.awt.GridLayout(1, 3, 18, 0));
        filaPaciente.setOpaque(false);
        txtEspecieConsulta.setEditable(false);
        txtRazaConsulta.setEditable(false);
        txtEspecieConsulta.setFocusable(false);
        txtRazaConsulta.setFocusable(false);
        txtPesoConsulta.setToolTipText("Peso medido en esta consulta, en kg (ej.: 5,200).");
        filaPaciente.add(campoConsulta(new javax.swing.JLabel("Especie"), txtEspecieConsulta));
        filaPaciente.add(campoConsulta(new javax.swing.JLabel("Raza"), txtRazaConsulta));
        filaPaciente.add(campoConsulta(new javax.swing.JLabel("Peso actual (kg) *"), txtPesoConsulta));
        filaPaciente.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 68));
        filaPaciente.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        centro.add(filaPaciente);
        centro.add(javax.swing.Box.createVerticalStrut(8));

        // Una nueva atención puede vincularse a otra consulta finalizada.
        // La fecha de control solo la recomienda el veterinario, no crea turnos.
        // Misma grilla de tres columnas y mismos márgenes que las filas anteriores.
        // Los contenedores conservan su posición aunque su campo esté oculto por rol.
        javax.swing.JPanel filaSeguimiento = new javax.swing.JPanel(
                new java.awt.GridLayout(1, 3, 18, 0));
        filaSeguimiento.setOpaque(false);
        comboConsultaOrigen.setPreferredSize(new java.awt.Dimension(200, 32));
        comboConsultaOrigen.setToolTipText(
                "Opcional. Seleccione una consulta finalizada de este mismo paciente.");
        panelOrigenConsulta = campoConsulta(
                new javax.swing.JLabel("Seguimiento de consulta (opcional)"), comboConsultaOrigen);
        javax.swing.JPanel columnaOrigen = new javax.swing.JPanel(new java.awt.BorderLayout());
        columnaOrigen.setOpaque(false);
        columnaOrigen.add(panelOrigenConsulta, java.awt.BorderLayout.NORTH);
        // Ambas alternativas usan la misma columna izquierda según el rol.
        // Así el calendario no aparece aislado en el centro del formulario.
        fechaProximoControl.setDateFormatString("dd/MM/yyyy");
        fechaProximoControl.setPreferredSize(new java.awt.Dimension(200, 32));
        fechaProximoControl.setToolTipText("Seleccione el próximo control desde el calendario; no agenda turno.");
        panelFechaControl = campoConsulta(
                new javax.swing.JLabel("Próximo control recomendado"), fechaProximoControl);
        javax.swing.JPanel columnaFechaControl = new javax.swing.JPanel(new java.awt.BorderLayout());
        columnaFechaControl.setOpaque(false);
        columnaFechaControl.add(panelFechaControl, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel columnaSeguimiento = new javax.swing.JPanel(new java.awt.BorderLayout());
        columnaSeguimiento.setOpaque(false);
        columnaSeguimiento.add(columnaOrigen, java.awt.BorderLayout.NORTH);
        columnaSeguimiento.add(columnaFechaControl, java.awt.BorderLayout.CENTER);
        filaSeguimiento.add(columnaSeguimiento);
        for (int i = 0; i < 2; i++) {
            javax.swing.JPanel espacio = new javax.swing.JPanel();
            espacio.setOpaque(false);
            filaSeguimiento.add(espacio);
        }
        filaSeguimiento.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 68));
        filaSeguimiento.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        centro.add(filaSeguimiento);
        centro.add(javax.swing.Box.createVerticalStrut(8));

        configurarLimiteEvolucion();
        javax.swing.JPanel textoEvolucion = new javax.swing.JPanel(new java.awt.BorderLayout(0, 3));
        textoEvolucion.setOpaque(false);
        textoEvolucion.add(jScrollPane1, java.awt.BorderLayout.CENTER);
        textoEvolucion.add(contadorTratamiento, java.awt.BorderLayout.SOUTH);
        javax.swing.JPanel evolucion = campoConsulta(lbTratamiento, textoEvolucion);
        panelEvolucionConsulta = evolucion;
        evolucion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(450, 105));
        evolucion.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 155));
        centro.add(evolucion);
        jpDatos.add(centro, java.awt.BorderLayout.CENTER);
        jcbMotivo.addActionListener(e -> actualizarCamposOtros());
        jcbPatologia.addActionListener(e -> actualizarCamposOtros());
        txtOtroMotivo.setVisible(false);
        txtOtraPatologia.setVisible(false);
        // Al registrarse una consulta aún no corresponde completar evolución.
        jScrollPane1.setFocusable(false);

        javax.swing.JPanel botones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 0));
        botones.setOpaque(false);
        btnGuardar.setPreferredSize(new java.awt.Dimension(153, 30));
        btnGuardarEnObservacion = new javax.swing.JButton("Dejar en observación");
        btnGuardarEnObservacion.setToolTipText("Guarda el avance, mantiene abierta la consulta y permite atender otro paciente.");
        btnGuardarEnObservacion.setPreferredSize(new java.awt.Dimension(155, 30));
        btnGuardarEnObservacion.addActionListener(e -> guardarAtencion(false, true));
        btnFinalizarDesdeFicha = new javax.swing.JButton("Guardar y finalizar");
        btnFinalizarDesdeFicha.setPreferredSize(new java.awt.Dimension(155, 30));
        btnFinalizarDesdeFicha.setToolTipText("Guarda diagnóstico y evolución, y cierra definitivamente la consulta.");
        btnFinalizarDesdeFicha.addActionListener(e -> guardarAtencion(true));
        btnCancelar.setPreferredSize(new java.awt.Dimension(88, 30));
        btnLimpiar.setPreferredSize(new java.awt.Dimension(83, 30));
        botones.add(btnGuardar);
        botones.add(btnGuardarEnObservacion);
        botones.add(btnFinalizarDesdeFicha);
        botones.add(btnCancelar);
        botones.add(btnLimpiar);
        jpDatos.add(botones, java.awt.BorderLayout.SOUTH);
        jpDatos.revalidate();
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private javax.swing.JPanel comboConOpcionOtro(javax.swing.JComboBox<String> combo,
            javax.swing.JTextField detalle) {
        // Dos espacios de igual altura, siempre reservados. Solo cambia la
        // visibilidad del JTextField, nunca el tamaño de los otros componentes.
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 5));
        panel.setOpaque(false);
        javax.swing.JPanel espacioDetalle = new javax.swing.JPanel(new java.awt.BorderLayout());
        espacioDetalle.setOpaque(false);
        espacioDetalle.add(detalle, java.awt.BorderLayout.CENTER);
        panel.add(combo);
        panel.add(espacioDetalle);
        panel.setPreferredSize(new java.awt.Dimension(200, 65));
        detalle.setVisible(false);
        return panel;
    }

    private void actualizarCamposOtros() {
        boolean otroMotivo = OPCION_OTROS.equals(jcbMotivo.getSelectedItem());
        boolean otraPatologia = OPCION_OTROS.equals(jcbPatologia.getSelectedItem())
                && jcbPatologia.isEnabled();
        txtOtroMotivo.setVisible(otroMotivo);
        txtOtraPatologia.setVisible(otraPatologia);
        txtOtroMotivo.setEnabled(otroMotivo && jcbMotivo.isEnabled());
        txtOtraPatologia.setEnabled(otraPatologia);
        if (txtOtroMotivo.getParent() != null) {
            txtOtroMotivo.getParent().revalidate();
            txtOtroMotivo.getParent().repaint();
        }
        if (txtOtraPatologia.getParent() != null) {
            txtOtraPatologia.getParent().revalidate();
            txtOtraPatologia.getParent().repaint();
        }
    }

    private String valorCampoPersonalizado(javax.swing.JComboBox<String> combo,
            javax.swing.JTextField especificacion) {
        if (OPCION_OTROS.equals(combo.getSelectedItem()))
            return especificacion.getText().trim();
        Object item = combo.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private void restaurarCampoPersonalizado(javax.swing.JComboBox<String> combo,
            javax.swing.JTextField especificacion, String valor) {
        if (valor == null || valor.isBlank()) {
            combo.setSelectedIndex(0);
            especificacion.setText("");
            actualizarCamposOtros();
            return;
        }
        for (int i = 1; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (valor.equals(item) && !OPCION_OTROS.equals(item)) {
                combo.setSelectedIndex(i);
                especificacion.setText("");
                actualizarCamposOtros();
                return;
            }
        }
        combo.setSelectedItem(OPCION_OTROS);
        especificacion.setText(valor);
        actualizarCamposOtros();
    }

    private java.math.BigDecimal obtenerPesoActual() {
        String valor = txtPesoConsulta.getText().trim().replace(',', '.');
        if (valor.isEmpty()) return null;
        try {
            java.math.BigDecimal peso = new java.math.BigDecimal(valor);
            if (peso.signum() <= 0 || peso.compareTo(new java.math.BigDecimal("99999.999")) > 0
                    || peso.scale() > 3)
                throw new NumberFormatException();
            return peso;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ingrese un peso válido en kg, mayor a cero, con hasta 3 decimales.");
        }
    }

    private void configurarLimiteEvolucion() {
        contadorTratamiento = new javax.swing.JLabel();
        contadorTratamiento.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        contadorTratamiento.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
        txtTratamiento.setLineWrap(true);
        txtTratamiento.setWrapStyleWord(true);
        javax.swing.text.AbstractDocument doc =
                (javax.swing.text.AbstractDocument) txtTratamiento.getDocument();
        doc.setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto,
                    javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                if (texto == null) return;
                if (fb.getDocument().getLength() + texto.length() <= MAX_CARACTERES_EVOLUCION)
                    super.insertString(fb, offset, texto, attrs);
                else java.awt.Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto,
                    javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                String nuevo = texto == null ? "" : texto;
                if (fb.getDocument().getLength() - length + nuevo.length() <= MAX_CARACTERES_EVOLUCION)
                    super.replace(fb, offset, length, nuevo, attrs);
                else java.awt.Toolkit.getDefaultToolkit().beep();
            }
        });
        doc.addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarContadorEvolucion(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarContadorEvolucion(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarContadorEvolucion(); }
        });
        actualizarContadorEvolucion();
    }

    private void actualizarContadorEvolucion() {
        int cantidad = txtTratamiento.getDocument().getLength();
        contadorTratamiento.setText(cantidad + " / " + MAX_CARACTERES_EVOLUCION + " caracteres");
        contadorTratamiento.setToolTipText("Máximo de caracteres de evolución clínica.");
    }

    private boolean coincideEstadoModelo(int fila) {
        if (jcbEstadoFiltro == null || jcbEstadoFiltro.getSelectedItem() == null)
            return true;
        String estado = normalizarEstadoBD(
                String.valueOf(tableVisitas.getModel().getValueAt(fila, 8)));
        String filtro = jcbEstadoFiltro.getSelectedItem().toString();
        if ("Todas".equals(filtro)) return true;
        if ("Finalizadas".equals(filtro)) return "FINALIZADO".equals(estado);
        return "EN_ESPERA".equals(estado) || "ATENDIENDO".equals(estado)
                || "OBSERVACION".equals(estado);
    }

    private void llamarSiguientePaciente() {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        if (!usuarioClinico() || sesion == null || sesion.getUsuario() == null
                || !sesion.puede("FormConsulta.EDITAR")) {
            JOptionPane.showMessageDialog(this,
                    "La atención debe ser iniciada por un veterinario disponible.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Visita siguiente = visitaControlador.tomarSiguiente(sesion.getUsuario().getIdUsuario());
            if (siguiente == null) {
                JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                        "No hay pacientes pendientes de atención.",
                        "Cola de espera", JOptionPane.INFORMATION_MESSAGE);
                cargarVisitasEnTabla();
                return;
            }
            cargarVisitasEnTabla();
            abrirFichaClinica(siguiente);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                    "No se pudo llamar al siguiente paciente.\n" + ex.getMessage(),
                    "Cola de espera", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void abrirFichaClinica(Visita seleccionada) {
        if (seleccionada == null || !editarVisita(seleccionada)) return;
        configurarModoEdicionRestringida();
        lbDatos.setText("Atención veterinaria · Diagnóstico y evolución");
        btnGuardar.setText("Guardar avance");
        btnFinalizarDesdeFicha.setVisible(true);
        btnGuardarEnObservacion.setVisible(true);
        mostrarSoloFormulario();
        btnGuardar.setEnabled(true);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    private void retomarAtencionSeleccionada() {
        if (!usuarioClinico() || Application.getSesionUsuario() == null
                || !Application.getSesionUsuario().puede("FormConsulta.EDITAR")) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para retomar consultas.");
            return;
        }
        Visita seleccionada = obtenerVisitaSeleccionadaDesdeTabla();
        if (seleccionada == null) return;
        if (!"OBSERVACION".equals(normalizarEstadoBD(seleccionada.getEstado()))) {
            JOptionPane.showMessageDialog(this, "Solo se puede retomar una consulta en observación.");
            return;
        }
        try {
            Visita retomada = visitaControlador.avanzarEstado(
                    seleccionada.getIdVisita(), "OBSERVACION", "ATENDIENDO");
            cargarVisitasEnTabla();
            abrirFichaClinica(retomada);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo retomar la consulta.\n" + ex.getMessage(),
                    "Retomar atención", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void avanzarEstadoSeleccionado() {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        if (sesion == null || !sesion.puede("FormConsulta.EDITAR") || !usuarioClinico()) {
            JOptionPane.showMessageDialog(this,
                    "Solo el veterinario o administrador puede iniciar o finalizar la atención.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Visita seleccionada = obtenerVisitaSeleccionadaDesdeTabla();
        if (seleccionada == null) return;
        String actual = normalizarEstadoBD(seleccionada.getEstado());
        if ("EN_ESPERA".equals(actual)) {
            JOptionPane.showMessageDialog(this, "Para comenzar una atención utilice Llamar.\n"
                    + "El sistema seleccionará automáticamente el caso de mayor prioridad.");
            return;
        }
        if ("FINALIZADO".equals(actual) || "CANCELADO".equals(actual)) {
            JOptionPane.showMessageDialog(this, "Esta consulta ya está cerrada.",
                    "Cambio de estado no permitido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String siguiente = "FINALIZADO";
        boolean iniciar = "ATENDIENDO".equals(siguiente);
        String mensaje = iniciar
                ? "¿Comenzar la atención de " + seleccionada.getMascota().getNombre()
                   + "?\nSe abrirá su ficha para que el veterinario registre diagnóstico y tratamiento."
                : "¿Confirma que finalizó la atención de " + seleccionada.getMascota().getNombre()
                   + "?\nLa consulta quedará cerrada y no podrá editarse.";
        int respuesta = JOptionPane.showConfirmDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                mensaje, iniciar ? "Iniciar atención" : "Finalizar consulta",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) return;
        try {
            visitaControlador.avanzarEstado(seleccionada.getIdVisita(), actual, siguiente);
            cargarVisitasEnTabla();
            JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                    "Consulta finalizada correctamente. Se conservará en la historia clínica.",
                    "Atención finalizada", JOptionPane.INFORMATION_MESSAGE);
            // Solo el veterinario que estaba atendiendo puede tomar automáticamente
            // el siguiente ticket. Un administrador que cierra consultas de terceros
            // no debe asignarse accidentalmente el siguiente paciente.
            if (sesion != null && sesion.getRol() != null
                    && "Veterinario".equalsIgnoreCase(sesion.getRol().getNombreRol())
                    && sesion.getNombreApellido() != null
                    && seleccionada.getUsuarioAtiende() != null
                    && sesion.getNombreApellido().trim().equalsIgnoreCase(
                            seleccionada.getUsuarioAtiende().trim())) {
                String profesionalActual = sesion.getNombreApellido().trim();
                java.util.List<Visita> pendientes = visitaControlador.obtenerTodasLasVisitas();
                boolean sigueAtendiendo = pendientes.stream().anyMatch(v ->
                        "ATENDIENDO".equals(normalizarEstadoBD(v.getEstado()))
                        && profesionalActual.equalsIgnoreCase(
                                v.getUsuarioAtiende() == null ? "" : v.getUsuarioAtiende().trim()));
                boolean haySiguiente = pendientes.stream().anyMatch(v ->
                        "EN_ESPERA".equals(normalizarEstadoBD(v.getEstado())));
                if (!sigueAtendiendo && haySiguiente) llamarSiguientePaciente();
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                    "No se pudo actualizar el estado.\n" + ex.getMessage(),
                    "Estado sin cambios", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void configurarComboEstado() {
        // Combo de estado (edición)
        DefaultComboBoxModel<Constantes.EstadoVisita> modelEstado = new DefaultComboBoxModel<>();
        modelEstado.addElement(null);
        modelEstado.addElement(Constantes.EstadoVisita.EN_ESPERA);
        modelEstado.addElement(Constantes.EstadoVisita.ATENDIENDO);
        modelEstado.addElement(Constantes.EstadoVisita.OBSERVACION);
        modelEstado.addElement(Constantes.EstadoVisita.FINALIZADO);
        jcbEstado.setModel(modelEstado);

        // Combo de filtro de estado
        DefaultComboBoxModel<String> modelFiltro = new DefaultComboBoxModel<>(
                new String[]{"En curso", "Finalizadas", "Todas"});
        jcbEstadoFiltro.setModel(modelFiltro);
        jcbEstadoFiltro.setSelectedItem("En curso");

        // Renderer: null => "Todos" en filtro, "Seleccionar..." en edición
        jcbEstado.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Seleccionar...");
                } else {
                    setText(estadoParaMostrar(((Constantes.EstadoVisita) value).name()));
                }
                return this;
            }
        });

        jcbEstadoFiltro.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "En curso" : value.toString());
                return this;
            }
        });

        // Aplicar filtro combinado cuando cambia el estado
        jcbEstadoFiltro.addActionListener(e -> { if (paginacionConsultas != null) paginacionConsultas.establecerFiltroAdicional(this::coincideEstadoModelo); });
    }

    private String firmaConsultas(java.util.List<Visita> visitas) {
        StringBuilder firma = new StringBuilder();
        for (Visita v : visitas) {
            firma.append(v.getIdVisita()).append('|')
                    .append(v.getEstado()).append('|')
                    .append(v.getUsuarioAtiende()).append('|')
                    .append(v.getPrioridadAtencion()).append('|')
                    .append(v.getPatologia()).append('|')
                    .append(v.getTratamiento()).append(';');
        }
        return firma.toString();
    }

    private void actualizarResumenCola(java.util.List<Visita> visitas) {
        long espera = visitas.stream().filter(v ->
                "EN_ESPERA".equals(normalizarEstadoBD(v.getEstado()))).count();
        long observacion = visitas.stream().filter(v ->
                "OBSERVACION".equals(normalizarEstadoBD(v.getEstado()))).count();
        long atendiendo = visitas.stream().filter(v ->
                "ATENDIENDO".equals(normalizarEstadoBD(v.getEstado()))).count();
        resumenCola.setText("En espera: " + espera + "  |  Atendiendo: " + atendiendo
                + "  |  Observación: " + observacion);
    }

    private void iniciarActualizacionCola() {
        // Consultar en segundo plano evita bloquear la interfaz mientras MySQL responde.
        actualizacionCola = new javax.swing.Timer(8000, e -> comprobarActualizaciones(false));
        actualizacionCola.setInitialDelay(8000);
        actualizacionCola.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (actualizacionCola != null) actualizacionCola.start();
    }

    @Override
    public void removeNotify() {
        if (actualizacionCola != null) actualizacionCola.stop();
        super.removeNotify();
    }

    private void comprobarActualizaciones(boolean manual) {
        if (comprobandoCambios) return;
        comprobandoCambios = true;
        if (btnActualizarConsultas != null) btnActualizarConsultas.setEnabled(false);
        new javax.swing.SwingWorker<java.util.List<Visita>, Void>() {
            @Override
            protected java.util.List<Visita> doInBackground() {
                return visitaControlador.obtenerTodasLasVisitas();
            }

            @Override
            protected void done() {
                try {
                    java.util.List<Visita> nuevas = get();
                    actualizarResumenCola(nuevas);
                    String firma = firmaConsultas(nuevas);
                    if (!firma.equals(ultimaFirmaConsultas)) {
                        // No interrumpir una edición ni desmarcar una fila seleccionada.
                        if (jpListaVisitas.isVisible()
                                && (manual || !veterinaria.util.GestorTablas.haySeleccion(tableVisitas))) {
                            if (manual && paginacionConsultas != null)
                                paginacionConsultas.limpiarSeleccion();
                            cargarVisitasEnTabla();
                        }
                    }
                } catch (Exception ex) {
                    if (manual) javax.swing.JOptionPane.showMessageDialog(FormConsulta.this,
                            "No se pudieron actualizar las consultas: " + ex.getMessage(),
                            "Actualizar", javax.swing.JOptionPane.WARNING_MESSAGE);
                } finally {
                    comprobandoCambios = false;
                    if (btnActualizarConsultas != null) btnActualizarConsultas.setEnabled(true);
                }
            }
        }.execute();
    }

    private void cargarVisitasEnTabla() {

        cargandoTabla = true;

        try {

            DefaultTableModel model = (DefaultTableModel) tableVisitas.getModel();
            model.setRowCount(0);

            List<Visita> visitas = visitaControlador.obtenerTodasLasVisitas();
            ultimaFirmaConsultas = firmaConsultas(visitas);
            actualizarResumenCola(visitas);
            visitas.sort(java.util.Comparator
                    .comparingInt((Visita v) -> {
                        String estado = normalizarEstadoBD(v.getEstado());
                        return "EN_ESPERA".equals(estado) ? 0
                                : "ATENDIENDO".equals(estado) ? 1
                                : "OBSERVACION".equals(estado) ? 2 : 3;
                    })
                    .thenComparingInt(v -> {
                        if (!"EN_ESPERA".equals(normalizarEstadoBD(v.getEstado()))) return 3;
                        String prioridad = v.getPrioridadAtencion();
                        return "EMERGENCIA".equals(prioridad) ? 0
                                : "URGENCIA".equals(prioridad) ? 1 : 2;
                    })
                    .thenComparing(Visita::getFecha,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                    .thenComparing(Visita::getHora,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                    .thenComparing(Visita::getIdVisita));

            for (Visita visita : visitas) {

                String fecha = (visita.getFecha() != null)
                        ? visita.getFecha().toString()
                        : "Fecha no disponible";

                String hora = (visita.getHora() != null)
                        ? visita.getHora().toString()
                        : "Hora no disponible";

                String fechaHora = fecha + " a las " + hora;

                String nombreMascota = (visita.getMascota() != null)
                        ? visita.getMascota().getNombre()
                        : "Sin mascota";

                String cliente = "Sin cliente";

                // Mostrar el responsable guardado en la visita: no el dueño actual de la mascota.
                if (visita.getCliente() != null && visita.getCliente().getPersona() != null) {
                    cliente = visita.getCliente().getPersona().getNombre()
                            + " " + visita.getCliente().getPersona().getApellido();
                }

                Object[] fila = {
                    false,
                    visita.getIdVisita(),
                    nombreMascota,
                    cliente,
                    visita.getMotivoVisita(),
                    visita.getPatologia(),
                    visita.getTratamiento(),
                    fechaHora,
                    visita.getEstado(),
                    visita.getUsuarioAtiende() == null || visita.getUsuarioAtiende().isBlank()
                            ? "Sin asignar" : visita.getUsuarioAtiende(),
                    visita.getPrioridadAtencion() == null ? "CONTROL" : visita.getPrioridadAtencion()
                };

                model.addRow(fila);
            }

            if (!listenersTablaInicializados) {
                listenersTablaInicializados = true;
                new ManejoTablas().asegurarSeleccionUnica(tableVisitas);
            }

            if (paginacionConsultas != null) paginacionConsultas.actualizar();

            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);

        } finally {

            cargandoTabla = false;

        }

        actualizarBotonImprimir();
    }

    /**
     * Al regresar desde un formulario, recalcular la visibilidad de las
     * acciones DESPUÉS de mostrar el listado. Habilitar Nuevo no alcanza
     * si antes había sido ocultado con setVisible(false).
     */
    private void restablecerAccionesDeListado() {
        if (paginacionConsultas != null) paginacionConsultas.limpiarSeleccion();
        tableVisitas.clearSelection();
        actualizarAccionesPorSeleccion();
        if (herramientasConsultas != null) {
            herramientasConsultas.revalidate();
            herramientasConsultas.repaint();
        }
    }

    // -------------------- Acciones contextuales de la lista --------------------
    private void actualizarBotonImprimir() {
        if (cargandoTabla) return;
        actualizarAccionesPorSeleccion();
    }

    private void actualizarNuevoSegunSeleccion(boolean seleccion) {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        boolean autorizado = sesion != null && sesion.puede("FormConsulta.NUEVO");
        btnNuevo.setVisible(autorizado && !seleccion && !jpDatos.isVisible());
        btnNuevo.setEnabled(autorizado && !seleccion && !jpDatos.isVisible());
    }

    /** Solo se muestran operaciones válidas para el registro y rol actuales. */
    private void actualizarAccionesPorSeleccion() {
        int fila = obtenerFilaSeleccionadaPorCheck();
        boolean seleccion = fila >= 0 && fila < tableVisitas.getRowCount();
        String estado = seleccion
                ? normalizarEstadoBD(String.valueOf(tableVisitas.getValueAt(fila, 8))) : "";
        boolean clinico = usuarioClinico() && !jpDatos.isVisible();
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        boolean puedeCancelar = sesion != null && sesion.puede("FormConsulta.CANCELAR");
        boolean puedeImprimir = sesion != null && sesion.puede("FormConsulta.IMPRIMIR");
        boolean cerrada = "FINALIZADO".equals(estado) || "CANCELADO".equals(estado);

        actualizarNuevoSegunSeleccion(seleccion);
        btnVerInfo.setVisible(seleccion && !jpDatos.isVisible());
        btnVerInfo.setEnabled(seleccion && !jpDatos.isVisible());
        btnQuitarSeleccion.setVisible(seleccion && !jpDatos.isVisible());
        btnQuitarSeleccion.setEnabled(seleccion && !jpDatos.isVisible());
        btnImprimirVisita.setVisible(seleccion && puedeImprimir && !jpDatos.isVisible());
        btnImprimirVisita.setEnabled(seleccion && puedeImprimir && !jpDatos.isVisible());

        btnEliminar.setVisible(seleccion && puedeCancelar && !cerrada && !jpDatos.isVisible());
        btnEliminar.setEnabled(seleccion && puedeCancelar && !cerrada && !jpDatos.isVisible());
        btnEditar.setVisible(seleccion && clinico && "ATENDIENDO".equals(estado));
        btnEditar.setEnabled(seleccion && clinico && "ATENDIENDO".equals(estado));
        btnRetomarAtencion.setVisible(seleccion && clinico && "OBSERVACION".equals(estado));
        btnRetomarAtencion.setEnabled(seleccion && clinico && "OBSERVACION".equals(estado));

        // La finalización está integrada a Guardar y finalizar dentro de la ficha.
        btnCambiarEstado.setVisible(false);
        btnCambiarEstado.setEnabled(false);
        if (herramientasConsultas != null) {
            herramientasConsultas.revalidate();
            herramientasConsultas.repaint();
        }
    }

    private void configurarAccionesPorRol() {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        if (sesion == null) return;
        btnLlamarSiguiente.setVisible(usuarioClinico() && sesion.puede("FormConsulta.EDITAR"));
        btnEliminar.setText("Cancelar consulta");
        btnEliminar.setToolTipText("Cancelar la consulta seleccionada sin borrar su registro.");
        btnImprimirListaVisitas.setEnabled(sesion.puede("FormConsulta.IMPRIMIR"));
        actualizarAccionesPorSeleccion();
    }

    private void mostrarInformacionConsulta() {
        Visita v = obtenerVisitaSeleccionadaDesdeTabla();
        if (v == null) return;
        String responsable = v.getCliente() == null || v.getCliente().getPersona() == null
                ? "-" : v.getCliente().getPersona().getNombre() + " "
                    + v.getCliente().getPersona().getApellido();
        String fecha = v.getFecha() == null ? "-" :
                v.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = v.getHora() == null ? "-" :
                v.getHora().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        javax.swing.JPanel ficha = new javax.swing.JPanel();
        ficha.setLayout(new javax.swing.BoxLayout(ficha, javax.swing.BoxLayout.Y_AXIS));
        ficha.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));
        javax.swing.JLabel titulo = new javax.swing.JLabel("DATOS DE LA CONSULTA");
        titulo.setForeground(new java.awt.Color(66, 190, 115));
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        titulo.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(titulo);
        ficha.add(javax.swing.Box.createVerticalStrut(14));

        String[][] campos = {
            {"Ticket", "N.º " + v.getIdVisita()},
            {"Estado", textoInfo(v.getEstado())},
            {"Paciente", v.getMascota() == null ? "-" : textoInfo(v.getMascota().getNombre())},
            {"Especie", textoInfo(v.getEspecieConsulta() != null
                    ? v.getEspecieConsulta()
                    : (v.getMascota() == null ? null : v.getMascota().getEspecie()))},
            {"Raza", textoInfo(v.getRazaConsulta() != null
                    ? v.getRazaConsulta()
                    : (v.getMascota() == null ? null : v.getMascota().getRaza()))},
            {"Peso de esta consulta", v.getPesoConsulta() == null ? "No registrado"
                    : v.getPesoConsulta().toPlainString().replace('.', ',') + " kg"},
            {"Prioridad", textoInfo(v.getPrioridadAtencion())},
            {"Responsable", textoInfo(responsable)},
            {"Fecha y hora", fecha + "  " + hora},
            {"Motivo", textoInfo(v.getMotivoVisita())},
            {"Seguimiento de", v.getIdConsultaOrigen() == null ? "Sin vinculación"
                    : "Consulta N.º " + v.getIdConsultaOrigen()},
            {"Próximo control sugerido", v.getProximoControlRecomendado() == null ? "No indicado"
                    : v.getProximoControlRecomendado().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))},
            {"Veterinario", textoInfo(v.getUsuarioAtiende())}
        };
        javax.swing.JPanel resumen = new javax.swing.JPanel(new java.awt.GridLayout(0, 2, 18, 12));
        resumen.setOpaque(false);
        for (String[] campo : campos) {
            javax.swing.JPanel celda = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 3));
            celda.setOpaque(false);
            javax.swing.JLabel etiqueta = new javax.swing.JLabel(campo[0]);
            etiqueta.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
            javax.swing.JLabel valor = new javax.swing.JLabel(campo[1]);
            valor.setFont(valor.getFont().deriveFont(java.awt.Font.BOLD));
            celda.add(etiqueta);
            celda.add(valor);
            resumen.add(celda);
        }
        resumen.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(resumen);
        ficha.add(javax.swing.Box.createVerticalStrut(16));

        javax.swing.JLabel seccion = new javax.swing.JLabel("INFORMACIÓN CLÍNICA");
        seccion.setForeground(new java.awt.Color(66, 190, 115));
        seccion.setFont(seccion.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        seccion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(seccion);
        ficha.add(javax.swing.Box.createVerticalStrut(10));
        ficha.add(campoInfoConsulta("Diagnóstico", textoInfo(v.getPatologia())));
        ficha.add(javax.swing.Box.createVerticalStrut(10));
        ficha.add(campoInfoConsulta("Evolución y tratamiento", textoInfo(v.getTratamiento())));
        // El tamaño fijo anterior recortaba diagnóstico y evolución.
        // Mantener el cuadro compacto y permitir recorrer la información completa.
        javax.swing.JScrollPane desplazamientoFicha = new javax.swing.JScrollPane(ficha);
        desplazamientoFicha.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        desplazamientoFicha.setHorizontalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazamientoFicha.setVerticalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        desplazamientoFicha.getVerticalScrollBar().setUnitIncrement(16);
        desplazamientoFicha.setPreferredSize(new java.awt.Dimension(640, 460));
        JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                desplazamientoFicha, "Información de la consulta · Solo lectura",
                JOptionPane.PLAIN_MESSAGE);
    }

    private String textoInfo(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor.trim();
    }

    private javax.swing.JPanel campoInfoConsulta(String etiqueta, String valor) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        javax.swing.JLabel titulo = new javax.swing.JLabel(etiqueta);
        titulo.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
        panel.add(titulo, java.awt.BorderLayout.NORTH);
        // Ajustar la altura al contenido clínico, sin cortar el texto largo.
        int renglones = Math.max(2, Math.min(12,
                (valor.length() + 66) / 67 + (int) valor.chars().filter(ch -> ch == '\n').count()));
        javax.swing.JTextArea detalle = new javax.swing.JTextArea(valor, renglones, 44);
        detalle.setEditable(false);
        detalle.setOpaque(false);
        detalle.setFocusable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD));
        panel.add(detalle, java.awt.BorderLayout.CENTER);
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 300));
        return panel;
    }

    private boolean usuarioClinico() {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        String rol = sesion == null || sesion.getRol() == null ? "" : sesion.getRol().getNombreRol();
        return "Veterinario".equalsIgnoreCase(rol) || "Administrador".equalsIgnoreCase(rol);
    }

    private void mostrarSoloFormulario() {
        jpHeader.setVisible(true);
        if (herramientasConsultas != null) herramientasConsultas.setVisible(false);
        jpListaVisitas.setVisible(false);
        jpDatos.setVisible(true);
    }

    /** Solo indica carga de consultas asignadas, no disponibilidad presencial ni turnos. */
    private void configurarModoNuevo() {
        try {
            // Se permite cargar todos los campos
            jcbPaciente.setEnabled(true);
            jdcFecha.setEnabled(true);
            spinnerHora.setEnabled(true);
            jcbMotivo.setEnabled(true);
            jcbPatologia.setEnabled(false); // Diagnóstico exclusivo de atención clínica.
            jcbVeterinario.setEnabled(false); // Asignación dinámica al iniciar la atención.
            jcbVeterinario.setToolTipText("Se asignará automáticamente cuando un veterinario esté disponible.");
            lbVeterinarioAsignado.setText("Veterinario");
            jcbEstado.setEnabled(false); // Siempre inicia EN ESPERA; se cambia desde la lista.
            jcbEstado.setSelectedItem(Constantes.EstadoVisita.EN_ESPERA);
            prioridadConsulta.setEnabled(true);
            txtTratamiento.setEditable(false);
            txtTratamiento.setEnabled(false);
            txtTratamiento.setFocusable(false);
            if (panelEvolucionConsulta != null) panelEvolucionConsulta.setVisible(false);
            txtPesoConsulta.setEditable(true);
            if (panelOrigenConsulta != null) panelOrigenConsulta.setVisible(true);
            comboConsultaOrigen.setEnabled(true);
            if (panelFechaControl != null) panelFechaControl.setVisible(false);
            actualizarCamposOtros();
            btnLimpiar.setVisible(true);
            txtTratamiento.setText("");
            lbTratamiento.setText("Tratamiento (completa el veterinario)");
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
    }

    private void configurarModoEdicionRestringida() {
        try {
            // En edición solo se permite actualizar el tratamiento.
            jcbPaciente.setEnabled(false);
            jdcFecha.setEnabled(false);
            spinnerHora.setEnabled(false);
            jcbMotivo.setEnabled(false);
            jcbPatologia.setEnabled(usuarioClinico());
            jcbVeterinario.setEnabled(false);
            lbVeterinarioAsignado.setText("Veterinario");

            jcbEstado.setEnabled(false); // El estado se cambia solo desde Cambiar estado.
            prioridadConsulta.setEnabled(false);
            txtTratamiento.setEditable(usuarioClinico());
            txtTratamiento.setEnabled(usuarioClinico());
            txtTratamiento.setFocusable(usuarioClinico());
            txtPesoConsulta.setEditable(usuarioClinico());
            // Mostrar el vínculo como dato de solo lectura si la consulta es un seguimiento.
            if (panelOrigenConsulta != null)
                panelOrigenConsulta.setVisible(visita != null && visita.getIdConsultaOrigen() != null);
            comboConsultaOrigen.setEnabled(false);
            if (panelFechaControl != null) panelFechaControl.setVisible(usuarioClinico());
            if (panelEvolucionConsulta != null) panelEvolucionConsulta.setVisible(true);
            actualizarCamposOtros();
            // Evitar borrar accidentalmente una evolución larga: las acciones clínicas guardan o cancelan.
            btnLimpiar.setVisible(false);
            lbTratamiento.setText("Evolución y tratamiento:");
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
    }

    private boolean confirmarGuardarVisita(boolean esNueva) {
        String titulo = esNueva ? "Confirmar nueva visita" : "Confirmar edición de visita";
        String accion = esNueva ? "crear" : "editar";

        String mascotaTxt = "";
        String clienteTxt = "";
        try {
            Object selMascota = jcbPaciente.getSelectedItem();
            mascotaTxt = selMascota != null ? selMascota.toString() : "";
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
        try {
            clienteTxt = txtDueño.getText() != null ? txtDueño.getText() : "";
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        String fechaTxt = "";
        String horaTxt = "";
        try {
            Date d = jdcFecha.getDate();
            if (d != null) {
                LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                fechaTxt = ld.toString();
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
        try {
            Date h = (Date) spinnerHora.getValue();
            if (h != null) {
                LocalTime lt = h.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
                horaTxt = String.format("%02d:%02d", lt.getHour(), lt.getMinute());
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        String motivo = safeText(jcbMotivo.getSelectedItem());
        String patologia = safeText(jcbPatologia.getSelectedItem());
        String tratamiento = txtTratamiento.getText() != null ? txtTratamiento.getText() : "";
        String estado = safeText(jcbEstado.getSelectedItem());
        String veterinario = esNueva ? "Pendiente de asignación" : safeText(jcbVeterinario.getSelectedItem());

        String resumen = esNueva
                ? "¿Registrar el ingreso del paciente y generar su ticket?\n\n"
                    + "Paciente: " + mascotaTxt + "\n"
                    + "Cliente: " + clienteTxt + "\n"
                    + "Fecha: " + fechaTxt + "  Hora: " + horaTxt + "\n"
                    + "Motivo de consulta: " + motivo + "\n"
                    + "Veterinario: se asignará al comenzar la atención.\n"
                    + "Prioridad: " + prioridadConsulta.getSelectedItem() + "\n\n"
                    + "El diagnóstico y el tratamiento se completan durante la atención."
                : "¿Guardar los datos registrados por el veterinario?\n\n"
                    + "Paciente: " + mascotaTxt + "\n"
                    + "Diagnóstico: " + patologia + "\n"
                    + "Evolución y tratamiento: " + tratamiento + "\n"
                    + "Estado: " + estado + "\n";

        Object[] options = {"Sí", "No"};
        int r = JOptionPane.showOptionDialog(
                this,
                resumen,
                titulo,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return r == JOptionPane.YES_OPTION;
    }

    private String safeText(Object o) {
        return o != null ? o.toString() : "";
    }

    private boolean isVisitaFinalizada(Visita v) {
        if (v == null) {
            return false;
        }
        return Constantes.EstadoVisita.FINALIZADO.name().equalsIgnoreCase(normalizarEstadoBD(v.getEstado()));
    }

    private Visita obtenerVisitaSeleccionadaDesdeTabla() {
        int row = obtenerFilaSeleccionadaPorCheck();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una visita en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            Integer idVisita = (Integer) tableVisitas.getValueAt(row, 1);
            Visita v = visitaControlador.buscarVisitaPorId(idVisita);
            if (v == null) {
                JOptionPane.showMessageDialog(this, "No se encontró la visita seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return v;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer la visita seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void imprimirVisitaSeleccionada() {
        Visita v = obtenerVisitaSeleccionadaDesdeTabla();
        if (v == null) {
            return;
        }

        ReporteRequest req = new ReporteRequest()
                .put("visita", v)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 🚀 MANDAMOS AL EJECUTOR: Borra el error rojo y mete el cartel modal
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.VISITA_REGISTRO, req);
    }

    private void imprimirListaVisitas() {
        javax.swing.JComboBox<String> filtro = new javax.swing.JComboBox<>(
                new String[]{"En curso", "Finalizadas", "Todas"});
        filtro.setSelectedItem(jcbEstadoFiltro.getSelectedItem());
        javax.swing.JComboBox<String> formato = new javax.swing.JComboBox<>(
                new String[]{"PDF", "Excel (XLS)"});
        javax.swing.JPanel opciones = new javax.swing.JPanel(new java.awt.GridLayout(2, 2, 10, 10));
        opciones.add(new javax.swing.JLabel("Consultas a incluir:"));
        opciones.add(filtro);
        opciones.add(new javax.swing.JLabel("Formato:"));
        opciones.add(formato);
        int decision = JOptionPane.showConfirmDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this), opciones,
                "Configurar informe de consultas", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (decision != JOptionPane.OK_OPTION) return;
        String filtroElegido = String.valueOf(filtro.getSelectedItem());
        javax.swing.table.TableModel modelo = tableVisitas.getModel();
        java.util.List<String[]> filas = new java.util.ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String estado = normalizarEstadoBD(String.valueOf(modelo.getValueAt(i, 8)));
            boolean incluir = "Todas".equals(filtroElegido)
                    || ("Finalizadas".equals(filtroElegido) && "FINALIZADO".equals(estado))
                    || ("En curso".equals(filtroElegido)
                        && ("EN_ESPERA".equals(estado) || "ATENDIENDO".equals(estado)
                            || "OBSERVACION".equals(estado)));
            if (!incluir) continue;
            String[] fila = new String[8];
            for (int j = 0; j < fila.length; j++) {
                int[] columnas = {1, 2, 3, 4, 8, 9, 10, 7};
                Object valor = modelo.getValueAt(i, columnas[j]);
                fila[j] = valor == null ? "" : String.valueOf(valor);
            }
            filas.add(fila);
        }
        if (filas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay consultas para el filtro seleccionado.",
                    "Informe de consultas", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ReporteRequest req = new ReporteRequest()
                .put("filas", filas)
                .put("filtro", filtroElegido)
                .put("FORMATO_REPORTE", formato.getSelectedIndex() == 0
                    ? veterinaria.reportes.pdf.ReporteSelectorUtil.Formato.PDF
                    : veterinaria.reportes.pdf.ReporteSelectorUtil.Formato.XLS)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this,
                ReporteTipo.VISITA_LISTADO, req);
    }

    /** Etiqueta visible; internamente se conserva EN_ESPERA, etc. */
    private String estadoParaMostrar(String estado) {
        switch (normalizarEstadoBD(estado)) {
            case "EN_ESPERA": return "En espera";
            case "ATENDIENDO": return "Atendiendo";
            case "OBSERVACION": return "En observación";
            case "FINALIZADO": return "Finalizado";
            case "CANCELADO": return "Cancelado";
            default: return estado == null ? "" : estado.replace('_', ' ');
        }
    }

    private String normalizarEstadoBD(String estadoBD) {

        if (estadoBD == null || estadoBD.trim().isEmpty()) {
            return "SIN_ESTADO";
        }

        estadoBD = estadoBD.trim().toUpperCase();

        // Compatibilidad con versiones anteriores
        if (estadoBD.equals("ATENDIENDO") || estadoBD.equals("ATENDIENDO")) {
            return Constantes.EstadoVisita.ATENDIENDO.name();
        }

        if (estadoBD.equals("CANCELADO")) return "CANCELADO";
        if (estadoBD.equals("FINALIZADO") || estadoBD.equals("FINALIZADA")) {
            return Constantes.EstadoVisita.FINALIZADO.name();
        }

        // Si ya viene correcto
        try {
            Constantes.EstadoVisita.valueOf(estadoBD);
            return estadoBD;
        } catch (IllegalArgumentException e) {
            return Constantes.EstadoVisita.ATENDIENDO.name();
        }
    }

    private Integer obtenerFilaSeleccionadaPorCheck() {
        // Prioridad: checkbox en columna 0
        Integer filaCheck = new ManejoTablas().comprobarElementoSeleccionado(tableVisitas);
        if (filaCheck != null && filaCheck >= 0) {
            return filaCheck;
        }
        // Fallback: selección normal
        int filaSel = tableVisitas.getSelectedRow();
        return (filaSel >= 0) ? filaSel : -1;
    }

    private boolean editarVisitaSeleccionada() {

        int filaSeleccionada = obtenerFilaSeleccionadaPorCheck();

        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(
                    null,
                    "¡Debe seleccionar una visita a editar!",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        Integer idVisita = (Integer) tableVisitas.getValueAt(filaSeleccionada, 1);

        Visita visita = visitaControlador.buscarVisitaPorId(idVisita);

        return editarVisita(visita);
    }

    private boolean editarVisita(Visita visitaCargar) {

        if (visitaCargar == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró la visita.",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        this.visita = visitaCargar;
        mascotaSeleccionada = visita.getMascota();

        seleccionarMascotaEnCombo(
                mascotaSeleccionada != null ? mascotaSeleccionada.getIdMascota() : null);

        String nombreApellido = "";

        try {
            if (visita.getCliente() != null
                    && visita.getCliente().getPersona() != null) {

                String n = visita.getCliente().getPersona().getNombre();
                String a = visita.getCliente().getPersona().getApellido();

                nombreApellido = ((n != null ? n : "") + " " + (a != null ? a : "")).trim();
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal", ex);
        }

        txtDueño.setText(nombreApellido);

        Date fecha = Date.from(
                visita.getFecha()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());

        jdcFecha.setDate(fecha);

        configurarSpinnerHora(visita.getHora());

        restaurarCampoPersonalizado(jcbMotivo, txtOtroMotivo, visita.getMotivoVisita());
        restaurarCampoPersonalizado(jcbPatologia, txtOtraPatologia, visita.getPatologia());
        txtPesoConsulta.setText(visita.getPesoConsulta() == null ? ""
                : visita.getPesoConsulta().toPlainString().replace('.', ','));
        txtEspecieConsulta.setText(visita.getEspecieConsulta() != null
                ? visita.getEspecieConsulta() : textoSeguro(mascotaSeleccionada == null ? null : mascotaSeleccionada.getEspecie()));
        txtRazaConsulta.setText(visita.getRazaConsulta() != null
                ? visita.getRazaConsulta() : textoSeguro(mascotaSeleccionada == null ? null : mascotaSeleccionada.getRaza()));
        txtTratamiento.setText(visita.getTratamiento());
        if (visita.getIdConsultaOrigen() != null) {
            int indiceOrigen = idsConsultasOrigen.indexOf(visita.getIdConsultaOrigen());
            if (indiceOrigen >= 0) comboConsultaOrigen.setSelectedIndex(indiceOrigen);
        }
        fechaProximoControl.setDate(visita.getProximoControlRecomendado() == null ? null
                : Date.from(visita.getProximoControlRecomendado()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()));

        jcbEstado.setSelectedItem(
                Constantes.EstadoVisita.valueOf(
                        normalizarEstadoBD(visita.getEstado())));

        seleccionarVeterinarioEnCombo(visita.getUsuarioAtiende());
        prioridadConsulta.setSelectedItem(visita.getPrioridadAtencion() == null
                ? "CONTROL" : visita.getPrioridadAtencion());

        return true;
    }

    private boolean eliminarVisitaSeleccionada() {
        Visita seleccionada = obtenerVisitaSeleccionadaDesdeTabla();
        if (seleccionada == null) return false;
        String estado = normalizarEstadoBD(seleccionada.getEstado());
        if ("FINALIZADO".equals(estado) || "CANCELADO".equals(estado)) {
            JOptionPane.showMessageDialog(this,
                    "Esta consulta ya está cerrada y no puede cancelarse.",
                    "Cancelación no permitida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        int confirmacion = JOptionPane.showConfirmDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "¿Cancelar la consulta N.º " + seleccionada.getIdVisita()
                   + " de " + seleccionada.getMascota().getNombre() + "?\n"
                   + "El registro se conservará como CANCELADO.",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) return false;
        if (!visitaControlador.cancelarVisita(seleccionada)) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo cancelar. Compruebe que la consulta siga abierta.",
                    "Cancelación no realizada", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        JOptionPane.showMessageDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "La consulta N.º " + seleccionada.getIdVisita()
                   + " se canceló. Su registro permanecerá disponible.",
                "Consulta cancelada", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    private boolean validarFormulario() {
        if (mascotaSeleccionada == null || jcbPaciente.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una mascota.");
            return false;
        }
        // La asignación de veterinario es automática al llamar al paciente.
        if (jcbEstado.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un estado.");
            return false;
        }
        if (jcbMotivo.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un motivo de visita.");
            return false;
        }
        String motivoReal = valorCampoPersonalizado(jcbMotivo, txtOtroMotivo);
        if (motivoReal.isBlank() || motivoReal.length() > MAX_TEXTO_CATALOGO) {
            JOptionPane.showMessageDialog(this,
                    "Especifique el motivo de consulta (hasta " + MAX_TEXTO_CATALOGO + " caracteres).",
                    "Motivo obligatorio", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        boolean registroNuevo = visita == null || visita.getIdVisita() == null;
        try {
            // La medición es obligatoria al ingreso; en consultas históricas sin
            // peso guardado no se bloquea la finalización ni se inventa un valor.
            if (registroNuevo && obtenerPesoActual() == null) {
                JOptionPane.showMessageDialog(this,
                        "Debe registrar el peso actual del paciente en kg.",
                        "Peso obligatorio", JOptionPane.WARNING_MESSAGE);
                txtPesoConsulta.requestFocusInWindow();
                return false;
            }
            if (!registroNuevo && !txtPesoConsulta.getText().trim().isEmpty())
                obtenerPesoActual();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Peso inválido", JOptionPane.WARNING_MESSAGE);
            txtPesoConsulta.requestFocusInWindow();
            return false;
        }
        boolean esNueva = visita == null || visita.getIdVisita() == null;
        if (!esNueva && usuarioClinico()) {
            String diagnostico = valorCampoPersonalizado(jcbPatologia, txtOtraPatologia);
            if (jcbPatologia.getSelectedIndex() <= 0 || diagnostico.isBlank()
                    || diagnostico.length() > MAX_TEXTO_CATALOGO) {
                JOptionPane.showMessageDialog(this,
                        "Seleccione o especifique el diagnóstico (hasta "
                        + MAX_TEXTO_CATALOGO + " caracteres).",
                        "Diagnóstico obligatorio", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        if (!esNueva && usuarioClinico()) {
            try {
                fechaRecomendadaIngresada();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Fecha de control inválida", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        String tratamiento = txtTratamiento.getText() != null ? txtTratamiento.getText().trim() : "";
        if (!esNueva && usuarioClinico() && tratamiento.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El veterinario debe registrar el tratamiento o la evolución.");
            return false;
        }
        return true;
    }

    private void initListeners() {
        // La búsqueda y el estado usan una sola paginación y un solo resaltado.

        // Actualiza botón imprimir cuando cambia selección o checkbox
        try {
            tableVisitas.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (e != null && !e.getValueIsAdjusting()) {
                        actualizarBotonImprimir();
                    }
                }
            });
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        try {
            tableVisitas.getModel().addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    // Si cambia el checkbox (col 0) o cualquier carga de tabla, refrescar
                    actualizarBotonImprimir();
                }
            });
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
    }

    /**
     * Al hacer click en una fila (fuera del checkbox), marca automáticamente el
     * checkbox de esa fila y desmarca el resto.
     */
    private void seleccionarFilaDesdeClick(java.awt.event.MouseEvent evt) {
        if (evt == null) {
            return;
        }

        int viewRow = tableVisitas.rowAtPoint(evt.getPoint());
        int viewCol = tableVisitas.columnAtPoint(evt.getPoint());

        if (viewRow < 0) {
            return;
        }

        // Selección visual de la fila
        try {
            tableVisitas.setRowSelectionInterval(viewRow, viewRow);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        // Si clickea el checkbox, permitimos el toggle natural
        if (viewCol == 0) {
            // El editor del checkbox termina después del evento de mouse.
            javax.swing.SwingUtilities.invokeLater(this::actualizarBotonImprimir);
            return;
        }

        try {
            // Desmarcar otros checks (selección única)
            for (int i = 0; i < tableVisitas.getRowCount(); i++) {
                if (i != viewRow && Boolean.TRUE.equals(tableVisitas.getValueAt(i, 0))) {
                    tableVisitas.setValueAt(false, i, 0);
                }
            }
            tableVisitas.setValueAt(true, viewRow, 0);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        javax.swing.SwingUtilities.invokeLater(this::actualizarBotonImprimir);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbVisitas = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbUsuarioBusqueda = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jcbEstadoFiltro = new javax.swing.JComboBox();
        lbEstadoFiltro = new javax.swing.JLabel();
        btnImprimirVisita = new javax.swing.JButton();
        jpDatos = new javax.swing.JPanel();
        lbDatos = new javax.swing.JLabel();
        lbPaciente = new javax.swing.JLabel();
        jcbPaciente = new javax.swing.JComboBox<>();
        lbDueño = new javax.swing.JLabel();
        txtDueño = new javax.swing.JTextField();
        lbFecha = new javax.swing.JLabel();
        lbMotivo = new javax.swing.JLabel();
        jcbMotivo = new javax.swing.JComboBox<>();
        jSeparator2 = new javax.swing.JSeparator();
        lbPatologia = new javax.swing.JLabel();
        jcbPatologia = new javax.swing.JComboBox<>();
        jSeparator3 = new javax.swing.JSeparator();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        spinnerHora = new javax.swing.JSpinner();
        lbHora = new javax.swing.JLabel();
        jdcFecha = new com.toedter.calendar.JDateChooser();
        lbTratamiento = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtTratamiento = new javax.swing.JTextArea();
        lbEstado = new javax.swing.JLabel();
        jcbEstado = new javax.swing.JComboBox();
        jcbVeterinario = new javax.swing.JComboBox<>();
        lbVeterinarioAsignado = new javax.swing.JLabel();
        jpListaVisitas = new javax.swing.JPanel();
        jSeparator5 = new javax.swing.JSeparator();
        lbListaDeVisitas = new javax.swing.JLabel();
        jSeparator6 = new javax.swing.JSeparator();
        scroll1 = new javax.swing.JScrollPane();
        tableVisitas = new veterinaria.vista.table.AutoTable();
        btnImprimirListaVisitas = new javax.swing.JButton();

        lbVisitas.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbVisitas.setText("Consultas");

        lbUsuarioBusqueda.setText("BUSCAR");

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

        btnEliminar.setText("Cancelar consulta");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        lbEstadoFiltro.setText("Estado:");

        btnImprimirVisita.setText("Imprimir");
        btnImprimirVisita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirVisitaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(lbUsuarioBusqueda)
                                .addGap(268, 268, 268))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jcbEstadoFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbEstadoFiltro))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btnImprimirVisita, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addComponent(lbVisitas)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbVisitas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(0, 25, Short.MAX_VALUE)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbEstadoFiltro)
                                .addGap(47, 47, 47))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(jcbEstadoFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(btnImprimirVisita)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnEliminar)
                                    .addComponent(btnEditar)
                                    .addComponent(btnNuevo))
                                .addContainerGap())
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbUsuarioBusqueda)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbBuscar))
                                .addGap(16, 16, 16))))))
        );

        lbDatos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbDatos.setText("Datos del Paciente");

        lbPaciente.setText("Paciente: *");

        jcbPaciente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Mascota" }));

        lbDueño.setText("Dueño:");

        txtDueño.setEditable(false);

        lbFecha.setText("Fecha: *");

        lbMotivo.setText("Motivo de Consulta:");

        jcbMotivo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Motivo" }));

        lbPatologia.setText("Patología:");

        jcbPatologia.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Patología" }));

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

        spinnerHora.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.HOUR));

        lbHora.setText("Hora: *");

        lbTratamiento.setText("Tratamiento:");

        txtTratamiento.setColumns(20);
        txtTratamiento.setRows(5);
        jScrollPane1.setViewportView(txtTratamiento);

        lbEstado.setText("Estado:");

        jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione veterinario" }));

        lbVeterinarioAsignado.setText("Veterinario: *");

        javax.swing.GroupLayout jpDatosLayout = new javax.swing.GroupLayout(jpDatos);
        jpDatos.setLayout(jpDatosLayout);
        jpDatosLayout.setHorizontalGroup(
            jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpDatosLayout.createSequentialGroup()
                                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbDatos)
                                    .addGroup(jpDatosLayout.createSequentialGroup()
                                        .addGap(8, 8, 8)
                                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbVeterinarioAsignado)
                                            .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbFecha)
                                            .addComponent(jdcFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbHora)
                                            .addComponent(spinnerHora, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(lbTratamiento)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 437, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jpDatosLayout.createSequentialGroup()
                                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbPaciente)
                                    .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbDueño)
                                    .addComponent(txtDueño, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbMotivo)
                                    .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbPatologia)
                                    .addComponent(jcbPatologia, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbEstado)
                                    .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpDatosLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jpDatosLayout.setVerticalGroup(
            jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDatosLayout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbDatos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpDatosLayout.createSequentialGroup()
                                .addComponent(lbDueño)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtDueño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jpDatosLayout.createSequentialGroup()
                                .addComponent(lbPaciente)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbMotivo)
                            .addComponent(lbEstado))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addComponent(lbPatologia)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbPatologia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addComponent(lbHora)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addComponent(lbVeterinarioAsignado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addComponent(lbFecha)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdcFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpDatosLayout.createSequentialGroup()
                        .addComponent(lbTratamiento)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpDatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar)
                    .addComponent(btnLimpiar))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbListaDeVisitas.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDeVisitas.setText("Lista de Visitas");

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableVisitas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Nº de Visita", "Paciente", "Cliente", "Motivo", "Patología", "Tratamiento", "Fecha", "Estado", "Veterinario", "Prioridad"
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
        tableVisitas.setMinimumSize(new java.awt.Dimension(0, 0));
        tableVisitas.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableVisitas);

        btnImprimirListaVisitas.setText("Imprimir Lista");
        btnImprimirListaVisitas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirListaVisitasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpListaVisitasLayout = new javax.swing.GroupLayout(jpListaVisitas);
        jpListaVisitas.setLayout(jpListaVisitasLayout);
        jpListaVisitasLayout.setHorizontalGroup(
            jpListaVisitasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scroll1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jpListaVisitasLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaVisitasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator6)
                    .addComponent(jSeparator5)
                    .addGroup(jpListaVisitasLayout.createSequentialGroup()
                        .addComponent(lbListaDeVisitas)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimirListaVisitas)))
                .addContainerGap())
        );
        jpListaVisitasLayout.setVerticalGroup(
            jpListaVisitasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaVisitasLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpListaVisitasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListaDeVisitas)
                    .addComponent(btnImprimirListaVisitas))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpListaVisitas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpDatos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaVisitas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        if (Application.getSesionUsuario() == null
                || !Application.getSesionUsuario().puede("FormConsulta.NUEVO")) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para registrar consultas.");
            return;
        }
        visita = new Visita();
        limpiarCamposFormulario();
        configurarModoNuevo();
        lbDatos.setText("Registro de llegada · Ticket de consulta");
        btnGuardar.setText("Registrar atención");
        btnFinalizarDesdeFicha.setVisible(false);
        btnGuardarEnObservacion.setVisible(false);
        prioridadConsulta.setSelectedItem("CONTROL");
        mostrarSoloFormulario();
        jpListaVisitas.setVisible(false);
        jpDatos.setVisible(true);
        btnGuardar.setEnabled(true);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        if (!usuarioClinico()) {
            JOptionPane.showMessageDialog(this, "El tratamiento lo completa el veterinario.");
            return;
        }
        if (editarVisitaSeleccionada()) {
            if (!"ATENDIENDO".equals(normalizarEstadoBD(visita.getEstado()))) {
                JOptionPane.showMessageDialog(this, "Primero debe comenzar la atención desde Cambiar estado.");
                return;
            }
            // Regla: si está FINALIZADO no se permite editar
            if (isVisitaFinalizada(visita)) {
                JOptionPane.showMessageDialog(this,
                        "No se permite editar una visita en estado FINALIZADO.",
                        "Edición no permitida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            configurarModoEdicionRestringida();
            lbDatos.setText("Atención veterinaria · Diagnóstico y evolución");
            btnGuardar.setText("Guardar avance");
        btnFinalizarDesdeFicha.setVisible(true);
        btnGuardarEnObservacion.setVisible(true);
            mostrarSoloFormulario();
            jpDatos.setVisible(true);
            jpListaVisitas.setVisible(false);
            btnGuardar.setEnabled(true);
            btnNuevo.setEnabled(false);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        if (Application.getSesionUsuario() == null
                || !Application.getSesionUsuario().puede("FormConsulta.CANCELAR")) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para cancelar consultas.");
            return;
        }
        if (eliminarVisitaSeleccionada()) {
            limpiarCamposFormulario();
            jpDatos.setVisible(false);
            cargarVisitasEnTabla();
            jpListaVisitas.setVisible(true);
            restablecerAccionesDeListado();
        }

    }//GEN-LAST:event_btnEliminarActionPerformed

    /** No cambia el estado del paciente: vincula la atención a una solicitud urgente. */
    private boolean prepararAtencionProvisional() {
        if (mascotaSeleccionada.isActivo()) {
            visita.setAtencionProvisional(false);
            visita.setIdSolicitudReactivacion(null);
            return true;
        }
        // Recepción puede emitir el ticket urgente, pero no realizar actos clínicos
        // ni reactivar al paciente. la solicitud se crea o vincula al guardar.
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        if (sesion == null || !sesion.puede("FormConsulta.NUEVO")) {
            JOptionPane.showMessageDialog(this,
                    "No tiene permiso para registrar la llegada del paciente.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        String prioridad = String.valueOf(prioridadConsulta.getSelectedItem());
        if (!"EMERGENCIA".equals(prioridad) && !"URGENCIA".equals(prioridad)) {
            JOptionPane.showMessageDialog(this,
                    "Para registrar una mascota inactiva, seleccione URGENCIA o EMERGENCIA."
                    + "\nLa solicitud urgente se crea automáticamente al registrar el ticket.",
                    "Prioridad de atención", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        int confirmacion = JOptionPane.showConfirmDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "La mascota " + mascotaSeleccionada.getNombre() + " figura como inactiva.\n\n"
                + "Se registrará un ticket URGENTE PROVISORIO en espera de atención veterinaria.\n"
                + "VetPRO generará automáticamente una solicitud urgente de revisión o\n"
                + "vinculará la que ya esté pendiente.\n"
                + "La mascota NO será reactivada ni se modificará su motivo de baja.\n\n"
                + "¿Confirma el registro del ingreso?",
                "Ingreso urgente de paciente inactivo", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) return false;
        visita.setAtencionProvisional(true);
        // DAO crea/vincula la solicitud y el ticket en la MISMA transacción.
        visita.setIdSolicitudReactivacion(null);
        return true;
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarAtencion(false);
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void guardarAtencion(boolean finalizar) {
        guardarAtencion(finalizar, false);
    }

    private void guardarAtencion(boolean finalizar, boolean observacion) {
        if (!validarFormulario()) {
            return;
        }
        if (visita != null && visita.getIdVisita() != null && !usuarioClinico()) {
            JOptionPane.showMessageDialog(this,
                    "El diagnóstico y el tratamiento deben ser registrados por el veterinario.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (mascotaSeleccionada != null) {
            if (visita.getIdVisita() == null && !prepararAtencionProvisional()) return;
            String userGestion = Application.getNombreApellidoUsuarioLogeado();
            if (userGestion == null || userGestion.trim().isEmpty()) {
                userGestion = "Sistema";
            }
            boolean registroInicial = visita.getIdVisita() == null;
            if (registroInicial) {
                // La recepción guarda el titular, motivo y usuario de origen una única vez.
                // Editar tratamiento nunca puede reescribir el dueño histórico.
                visita.setUsuarioGestion(userGestion);
                visita.setMascota(mascotaSeleccionada);
                visita.setCliente(mascotaSeleccionada.getCliente());
                visita.setMotivoVisita(valorCampoPersonalizado(jcbMotivo, txtOtroMotivo));
                visita.setPesoConsulta(obtenerPesoActual());
                visita.setIdConsultaOrigen(obtenerConsultaOrigenSeleccionada());
                visita.setPatologia("");
                visita.setTratamiento("");
            } else {
                visita.setPatologia(valorCampoPersonalizado(jcbPatologia, txtOtraPatologia));
                visita.setProximoControlRecomendado(fechaRecomendadaIngresada());
                // Conservar el valor cargado por recepción, sin sustituirlo por NULL.
                // Consultas históricas sin medición pueden finalizar sin inventar un peso.
                if (!txtPesoConsulta.getText().trim().isEmpty())
                    visita.setPesoConsulta(obtenerPesoActual());
                visita.setTratamiento(txtTratamiento.getText() != null ? txtTratamiento.getText().trim() : "");
            }
            // La recepción registra fecha y hora reales del sistema al guardar el ticket.
            // Al actualizar una consulta clínica se conservan sus datos temporales históricos.
            if (registroInicial) {
                visita.setFecha(LocalDate.now());
                visita.setHora(LocalTime.now().withSecond(0).withNano(0));
            }
            // EstadoVisita ahora es un enum (no String)
            Constantes.EstadoVisita estadoSel = (Constantes.EstadoVisita) jcbEstado.getSelectedItem();
            visita.setEstado(estadoSel != null ? estadoSel.name() : Constantes.EstadoVisita.EN_ESPERA.name());
            if (registroInicial) {
                visita.setUsuarioAtiende("");
                visita.setPrioridadAtencion(String.valueOf(prioridadConsulta.getSelectedItem()));
            }

            try {
                boolean esNueva = (visita.getIdVisita() == null);
                if (finalizar && esNueva) {
                    JOptionPane.showMessageDialog(this,
                            "Primero debe registrarse el ingreso del paciente.");
                    return;
                }
                if (finalizar || observacion) {
                    String mensaje = finalizar
                            ? "¿Guardar los datos clínicos y FINALIZAR la consulta de "
                                    + mascotaSeleccionada.getNombre() + "?\n"
                                    + "Una vez finalizada no podrá editarse."
                            : "¿Guardar el avance y dejar en OBSERVACIÓN a "
                                    + mascotaSeleccionada.getNombre() + "?\n"
                                    + "La consulta seguirá abierta y podrá atender otro paciente.";
                    if (JOptionPane.showConfirmDialog(this, mensaje,
                            finalizar ? "Confirmar finalización" : "Confirmar observación",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
                } else if (!confirmarGuardarVisita(esNueva)) {
                    return;
                }
                if (esNueva) {
                    visitaControlador.crearVisita(visita);
                    JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                            visita.isAtencionProvisional()
                                ? "Consulta provisoria guardada correctamente.\n"
                                  + "La mascota permanece inactiva y la solicitud de reactivación "
                                  + "sigue pendiente de revisión.\n"
                                  + "La consulta quedó vinculada al paciente y se incorporará a su historia clínica al finalizarla."
                                : "Consulta registrada correctamente.\n"
                                    + "Número de ticket: " + visita.getIdVisita() + ".\n"
                                    + "Paciente en espera de atención veterinaria.",
                            "Consulta guardada", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    if (observacion) {
                        visitaControlador.guardarEnObservacion(visita);
                        JOptionPane.showMessageDialog(this,
                                "Avance guardado. El paciente permanece en OBSERVACIÓN.\n"
                                + "Puede retomar su consulta más adelante o llamar a otro paciente.",
                                "Paciente en observación", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        visitaControlador.actualizarVisita(visita);
                    }
                    if (finalizar) {
                        visitaControlador.avanzarEstado(visita.getIdVisita(),
                                "ATENDIENDO", "FINALIZADO");
                        JOptionPane.showMessageDialog(this,
                                "Atención finalizada. La consulta quedó cerrada y se incorporará a la historia clínica.",
                                "Consulta finalizada", JOptionPane.INFORMATION_MESSAGE);
                    } else if (!observacion) {
                        JOptionPane.showMessageDialog(this,
                                "Avance clínico guardado. La consulta continúa ATENDIENDO.\n"
                                + "Puede retomarla más tarde o utilizar Guardar y finalizar.",
                                "Avance guardado", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } catch (Exception e) {
                veterinaria.util.AppLog.error(FormConsulta.class,
                        "No se pudo guardar la consulta clínica", e);
                Throwable causa = e;
                while (causa.getCause() != null && causa.getCause() != causa) {
                    causa = causa.getCause();
                }
                String detalle = causa.getMessage() == null || causa.getMessage().isBlank()
                        ? e.getMessage() : causa.getMessage();
                JOptionPane.showMessageDialog(this,
                        "No se pudo guardar la consulta.\nDetalle: " + detalle,
                        "Error de guardado", JOptionPane.ERROR_MESSAGE);
                return;
            }

            cargarVisitasEnTabla();
            limpiarCamposFormulario();
            jpDatos.setVisible(false);
            jpHeader.setVisible(true);
            if (herramientasConsultas != null) herramientasConsultas.setVisible(true);
            jpListaVisitas.setVisible(true);
            restablecerAccionesDeListado();
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una mascota.");
        }
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarCamposFormulario();
        jpDatos.setVisible(false);
        jpHeader.setVisible(true);
        if (herramientasConsultas != null) herramientasConsultas.setVisible(true);
        txtBusqueda.setEnabled(true);
        jpListaVisitas.setVisible(true);
        restablecerAccionesDeListado();
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnImprimirVisitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirVisitaActionPerformed
        imprimirVisitaSeleccionada();
    }//GEN-LAST:event_btnImprimirVisitaActionPerformed

    private void btnImprimirListaVisitasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirListaVisitasActionPerformed
        imprimirListaVisitas();
    }//GEN-LAST:event_btnImprimirListaVisitasActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnImprimirListaVisitas;
    private javax.swing.JButton btnImprimirVisita;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JComboBox jcbEstado;
    private javax.swing.JComboBox jcbEstadoFiltro;
    private javax.swing.JComboBox<String> jcbMotivo;
    private javax.swing.JComboBox<String> jcbPaciente;
    private javax.swing.JComboBox<String> jcbPatologia;
    private javax.swing.JComboBox<String> jcbVeterinario;
    private com.toedter.calendar.JDateChooser jdcFecha;
    private javax.swing.JPanel jpDatos;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaVisitas;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbDatos;
    private javax.swing.JLabel lbDueño;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbEstadoFiltro;
    private javax.swing.JLabel lbFecha;
    private javax.swing.JLabel lbHora;
    private javax.swing.JLabel lbListaDeVisitas;
    private javax.swing.JLabel lbMotivo;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbPatologia;
    private javax.swing.JLabel lbTratamiento;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JLabel lbVeterinarioAsignado;
    private javax.swing.JLabel lbVisitas;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JSpinner spinnerHora;
    private javax.swing.JTable tableVisitas;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextField txtDueño;
    private javax.swing.JTextArea txtTratamiento;
    // End of variables declaration//GEN-END:variables
}
