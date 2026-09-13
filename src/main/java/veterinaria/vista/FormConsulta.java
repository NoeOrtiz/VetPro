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

    private static final List<Integer> COLUMNAS_BUSQUEDA_TABLA = Arrays.asList(2, 3, 8, 9);

    public FormConsulta() {
        initComponents();
        PermisoUI.aplicar(this);

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

        initListeners();

        // Ajusta el ancho de las columnas basado en el texto de los encabezados
        UtilidadesTabla.ajustarAnchoColumnas(tableVisitas);

        // Tooltips por columna (descripción)
        ManejoTablas mt = new ManejoTablas();
        mt.aplicarTooltipsPorColumna(tableVisitas, Map.of(
                0, "Seleccionar una visita para editar o eliminar",
                1, "ID de la visita",
                2, "Paciente (Mascota)",
                3, "Cliente / Dueño",
                4, "Motivo de la consulta",
                5, "Patología / Diagnóstico",
                6, "Tratamiento indicado",
                7, "Fecha de la visita",
                8, "Estado (pendiente / finalizada / etc.)",
                9, "Veterinario que atendió"
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
            model.addElement("Seleccione veterinario");
            for (Usuario u : listaVeterinarios) {
                model.addElement(nombreCompletoUsuario(u));
            }
            jcbVeterinario.setModel(model);
        } catch (Exception e) {
            jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Seleccione veterinario"}));
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
        String pacienteSelec = (String) jcbPaciente.getSelectedItem();

        if (pacienteSelec == null || "Seleccionar Mascota".equals(pacienteSelec)) {
            mascotaSeleccionada = null;
            txtDueño.setText("");
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
                return;
            }
        }

        // Si no se encuentra, limpiar.
        mascotaSeleccionada = null;
        txtDueño.setText("");
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
        configurarSpinnerHora(null);
        jdcFecha.setDate(new Date());
        txtTratamiento.setText("");
        // Default: ATENDIENDO (evita estado null y mejora UX)
        try {
            jcbEstado.setSelectedItem(Constantes.EstadoVisita.ATENDIENDO);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
            jcbEstado.setSelectedItem(null);
        }
    }

    private void configurarComboEstado() {
        // Combo de estado (edición)
        DefaultComboBoxModel<Constantes.EstadoVisita> modelEstado = new DefaultComboBoxModel<>();
        modelEstado.addElement(null);
        modelEstado.addElement(Constantes.EstadoVisita.ATENDIENDO);
        modelEstado.addElement(Constantes.EstadoVisita.FINALIZADO);
        jcbEstado.setModel(modelEstado);

        // Combo de filtro de estado
        DefaultComboBoxModel<Constantes.EstadoVisita> modelFiltro = new DefaultComboBoxModel<>();
        modelFiltro.addElement(null); // "Todos"
        modelFiltro.addElement(Constantes.EstadoVisita.ATENDIENDO);
        modelFiltro.addElement(Constantes.EstadoVisita.FINALIZADO);
        jcbEstadoFiltro.setModel(modelFiltro);

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
                    setText(((Constantes.EstadoVisita) value).name());
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
                if (value == null) {
                    setText("Todos");
                } else {
                    setText(((Constantes.EstadoVisita) value).name());
                }
                return this;
            }
        });

        // Aplicar filtro combinado cuando cambia el estado
        jcbEstadoFiltro.addActionListener(e -> ManejoTablas.refrescarFiltroCombinado(tableVisitas, txtBusqueda, COLUMNAS_BUSQUEDA_TABLA, buildFiltroEstado()));
    }

    private void cargarVisitasEnTabla() {

        cargandoTabla = true;

        try {

            DefaultTableModel model = (DefaultTableModel) tableVisitas.getModel();
            model.setRowCount(0);

            List<Visita> visitas = visitaControlador.obtenerVisitasActivas();
            System.out.println("Cantidad de visitas BD: " + visitas.size());

            for (Visita v : visitas) {
                System.out.println(
                        "BD -> ID: " + v.getIdVisita()
                        + " Estado: " + v.getEstado()
                        + " Mascota: " + v.getMascota().getNombre()
                );
            }

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

                if (visita.getMascota() != null
                        && visita.getMascota().getCliente() != null
                        && visita.getMascota().getCliente().getPersona() != null) {

                    cliente = visita.getMascota().getCliente().getPersona().getNombre()
                            + " "
                            + visita.getMascota().getCliente().getPersona().getApellido();
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
                    visita.getUsuarioAtiende()
                };

                model.addRow(fila);
            }

            if (!listenersTablaInicializados) {
                listenersTablaInicializados = true;
                new ManejoTablas().asegurarSeleccionUnica(tableVisitas);
            }

            ManejoTablas.refrescarFiltroCombinado(
                    tableVisitas,
                    txtBusqueda,
                    COLUMNAS_BUSQUEDA_TABLA,
                    buildFiltroEstado()
            );

            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);

        } finally {

            cargandoTabla = false;

        }

        actualizarBotonImprimir();
    }

    // -------------------- Impresión PDF --------------------
    private void actualizarBotonImprimir() {

        if (cargandoTabla) {
            return;
        }

        boolean haySeleccion = (obtenerFilaSeleccionadaPorCheck() != -1);

        btnImprimirVisita.setVisible(haySeleccion);

        actualizarAccionesPorSeleccion();
    }

    private void actualizarAccionesPorSeleccion() {

        // Si la tabla está vacía, deshabilitar acciones
        if (tableVisitas.getRowCount() == 0) {
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
            return;
        }

        int row = obtenerFilaSeleccionadaPorCheck();

        // Validar que la fila exista
        if (row < 0 || row >= tableVisitas.getRowCount()) {
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
            return;
        }

        boolean esFinalizado = false;

        try {
            Object estadoObj = tableVisitas.getValueAt(row, 8); // Columna Estado
            String estado = estadoObj != null ? estadoObj.toString() : "";

            esFinalizado = Constantes.EstadoVisita.FINALIZADO.name()
                    .equalsIgnoreCase(normalizarEstadoBD(estado));

        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class,
                    "Excepción no fatal en UI", ex);
            return;
        }

        // Reglas de negocio
        btnEditar.setEnabled(!esFinalizado);

        // Solo permitir eliminar visitas finalizadas (si mantenés esa regla)
        btnEliminar.setEnabled(esFinalizado);
    }

    private void configurarModoNuevo() {
        try {
            // Se permite cargar todos los campos
            jcbPaciente.setEnabled(true);
            jdcFecha.setEnabled(true);
            spinnerHora.setEnabled(true);
            jcbMotivo.setEnabled(true);
            jcbPatologia.setEnabled(true);
            jcbVeterinario.setEnabled(true);
            jcbEstado.setEnabled(true);
            txtTratamiento.setEditable(true);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }
    }

    private void configurarModoEdicionRestringida() {
        try {
            // En edición solo se permite Estado + Tratamiento
            jcbPaciente.setEnabled(false);
            jdcFecha.setEnabled(false);
            spinnerHora.setEnabled(false);
            jcbMotivo.setEnabled(false);
            jcbPatologia.setEnabled(false);
            jcbVeterinario.setEnabled(false);

            jcbEstado.setEnabled(true);
            txtTratamiento.setEditable(true);
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
        String veterinario = safeText(jcbVeterinario.getSelectedItem());

        String resumen = "Vas a " + accion + " la siguiente visita:\n\n"
                + "Mascota: " + mascotaTxt + "\n"
                + "Cliente: " + clienteTxt + "\n"
                + "Fecha: " + fechaTxt + "  Hora: " + horaTxt + "\n"
                + "Motivo: " + motivo + "\n"
                + "Diagnóstico/Patología: " + patologia + "\n"
                + "Tratamiento: " + tratamiento + "\n"
                + "Estado: " + estado + "\n"
                + "Veterinario: " + veterinario + "\n";

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
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tableVisitas)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 🚀 MANDAMOS AL EJECUTOR: Limpio, sin "new" y en segundo plano
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.VISITA_LISTADO, req);
    }

    private javax.swing.RowFilter<Object, Object> buildFiltroEstado() {

        // Columna "Estado" en la tabla: 8 (0 Seleccionar | 1 Nº | 2 Paciente | 3 Cliente | 4 Motivo | 5 Patología | 6 Tratamiento | 7 Fecha | 8 Estado | 9 Veterinario)
        final int ESTADO_COL = 8;

        Constantes.EstadoVisita estado
                = (Constantes.EstadoVisita) jcbEstadoFiltro.getSelectedItem();

        if (estado == null) {
            return null; // "Todos"
        }

        // Matchea exacto el name() del enum (ATENDIENDO / FINALIZADO)
        String pattern = "^" + java.util.regex.Pattern.quote(estado.name()) + "$";

        return javax.swing.RowFilter.regexFilter(pattern, ESTADO_COL);
    }

    private String normalizarEstadoBD(String estadoBD) {

        if (estadoBD == null || estadoBD.trim().isEmpty()) {
            return Constantes.EstadoVisita.ATENDIENDO.name();
        }

        estadoBD = estadoBD.trim().toUpperCase();

        // Compatibilidad con versiones anteriores
        if (estadoBD.equals("ATENDIENDO") || estadoBD.equals("ATENDIENDO")) {
            return Constantes.EstadoVisita.ATENDIENDO.name();
        }

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
            if (mascotaSeleccionada != null
                    && mascotaSeleccionada.getCliente() != null
                    && mascotaSeleccionada.getCliente().getPersona() != null) {

                String n = mascotaSeleccionada.getCliente().getPersona().getNombre();
                String a = mascotaSeleccionada.getCliente().getPersona().getApellido();

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

        jcbMotivo.setSelectedItem(visita.getMotivoVisita());
        jcbPatologia.setSelectedItem(visita.getPatologia());
        txtTratamiento.setText(visita.getTratamiento());

        jcbEstado.setSelectedItem(
                Constantes.EstadoVisita.valueOf(
                        normalizarEstadoBD(visita.getEstado())));

        seleccionarVeterinarioEnCombo(visita.getUsuarioAtiende());

        return true;
    }

    private boolean eliminarVisitaSeleccionada() {
        int filaSeleccionada = obtenerFilaSeleccionadaPorCheck(); // Preferir checkbox

        if (filaSeleccionada != -1) {
            Integer idVisita = (Integer) tableVisitas.getValueAt(filaSeleccionada, 1); // ID en la columna 1
            Visita visita = visitaControlador.buscarVisitaPorId(idVisita);

            if (visita != null) {
                // Confirmar eliminación
                int respuesta = JOptionPane.showConfirmDialog(
                        this,
                        "¿Estás seguro de que deseas eliminar esta visita?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (respuesta == JOptionPane.YES_OPTION) {
                    // Eliminar visita solo si su estado es "Finalizado"
                    if (visitaControlador.eliminarVisita(visita)) {
                        JOptionPane.showMessageDialog(this, "Visita eliminada con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);
                        return true;
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo eliminar la visita. Solo se pueden eliminar visitas en estado 'Finalizado'.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    // Deseleccionar la fila si se elige NO
                    tableVisitas.setValueAt(false, filaSeleccionada, 0);
                    tableVisitas.clearSelection();
                }
            } else {
                JOptionPane.showMessageDialog(this, "La visita seleccionada no se encontró.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una visita para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return false;
    }

    private boolean validarFormulario() {
        if (mascotaSeleccionada == null || jcbPaciente.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una mascota.");
            return false;
        }
        if (getVeterinarioSeleccionado() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un veterinario.");
            return false;
        }
        if (jdcFecha.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar la fecha.");
            return false;
        }
        if (jcbEstado.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un estado.");
            return false;
        }
        if (jcbMotivo.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un motivo de visita.");
            return false;
        }
        if (jcbPatologia.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una patología o diagnóstico.");
            return false;
        }
        String tratamiento = txtTratamiento.getText() != null ? txtTratamiento.getText().trim() : "";
        if (tratamiento.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el tratamiento u observaciones de la visita.");
            return false;
        }
        try {
            Date hora = (Date) spinnerHora.getValue();
            if (hora == null) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una hora para la visita.");
                return false;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una hora válida para la visita.");
            return false;
        }
        return true;
    }

    private void initListeners() {
        // Filtro/resaltado centralizado (sin popups modales)
        ManejoTablas.aplicarFiltroYResaltadoCombinado(tableVisitas, txtBusqueda, COLUMNAS_BUSQUEDA_TABLA, this::buildFiltroEstado);

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

        // Habilitar acciones sobre selección (solo si estamos en modo lista)
        try {
            if (!jpDatos.isVisible()) {
                btnEditar.setEnabled(true);
                btnEliminar.setEnabled(true);
            }
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        // Selección visual de la fila
        try {
            tableVisitas.setRowSelectionInterval(viewRow, viewRow);
        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class, "Excepción no fatal en UI", ex);
        }

        // Si clickea el checkbox, permitimos el toggle natural
        if (viewCol == 0) {
            actualizarBotonImprimir();
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

        actualizarBotonImprimir();
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

        btnEliminar.setText("Cancelar");
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
                "Seleccionar", "Nº de Visita", "Paciente", "Cliente", "Motivo", "Patología", "Tratamiento", "Fecha", "Estado", "Veterinario"
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
        tableVisitas.setMinimumSize(new java.awt.Dimension(848, 220));
        tableVisitas.setPreferredSize(new java.awt.Dimension(848, 220));
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
        visita = new Visita();
        limpiarCamposFormulario();
        configurarModoNuevo();
        jpListaVisitas.setVisible(false);
        jpDatos.setVisible(true);
        btnGuardar.setEnabled(true);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        if (editarVisitaSeleccionada()) {
            // Regla: si está FINALIZADO no se permite editar
            if (isVisitaFinalizada(visita)) {
                JOptionPane.showMessageDialog(this,
                        "No se permite editar una visita en estado FINALIZADO.",
                        "Edición no permitida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            configurarModoEdicionRestringida();
            jpDatos.setVisible(true);
            btnGuardar.setEnabled(true);
            btnNuevo.setEnabled(false);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        if (eliminarVisitaSeleccionada()) {
            limpiarCamposFormulario();
            jpDatos.setVisible(false);
            cargarVisitasEnTabla();
            jpListaVisitas.setVisible(true);
            btnNuevo.setEnabled(true);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }

    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (!validarFormulario()) {
            return;
        }
        if (mascotaSeleccionada != null) {
            String userGestion = Application.getNombreApellidoUsuarioLogeado();
            if (userGestion == null || userGestion.trim().isEmpty()) {
                userGestion = "Sistema";
            }
            visita.setUsuarioGestion(userGestion);
            visita.setMascota(mascotaSeleccionada);
            visita.setCliente(mascotaSeleccionada.getCliente());
            visita.setMotivoVisita((String) jcbMotivo.getSelectedItem());
            visita.setPatologia((String) jcbPatologia.getSelectedItem());
            visita.setTratamiento(txtTratamiento.getText() != null ? txtTratamiento.getText().trim() : "");
            visita.setFecha(jdcFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            visita.setHora(((Date) spinnerHora.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
            // EstadoVisita ahora es un enum (no String)
            Constantes.EstadoVisita estadoSel = (Constantes.EstadoVisita) jcbEstado.getSelectedItem();
            visita.setEstado(estadoSel != null ? estadoSel.name() : Constantes.EstadoVisita.ATENDIENDO.name());
            visita.setUsuarioAtiende((String) jcbVeterinario.getSelectedItem());

            try {
                boolean esNueva = (visita.getIdVisita() == null);
                if (!confirmarGuardarVisita(esNueva)) {
                    return;
                }
                if (esNueva) {
                    visitaControlador.crearVisita(visita);
                    JOptionPane.showMessageDialog(this, "Visita registrada con éxito.");
                } else {
                    visitaControlador.actualizarVisita(visita);
                    JOptionPane.showMessageDialog(this, "Visita actualizada con éxito.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al registrar la visita: " + e.getMessage());
            }

            cargarVisitasEnTabla();
            limpiarCamposFormulario();
            jpDatos.setVisible(false);
            btnNuevo.setEnabled(true);
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
            jpListaVisitas.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una mascota.");
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarCamposFormulario();
        jpDatos.setVisible(false);
        btnNuevo.setEnabled(true);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        txtBusqueda.setEnabled(true);
        jpListaVisitas.setVisible(true);
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
