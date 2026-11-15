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
    private javax.swing.JPanel herramientasConsultas;

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
        prioridadConsulta.setSelectedItem("CONTROL");
        configurarSpinnerHora(null);
        jdcFecha.setDate(new Date());
        txtTratamiento.setText("");
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
        int[] anchos = {82, 52, 95, 110, 115, 95, 185, 115, 95, 110, 94};
        for (int i = 0; i < anchos.length; i++)
            tableVisitas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        tableVisitas.getColumnModel().getColumn(0).setMinWidth(80);
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
        btnVerInfo = new javax.swing.JButton("Ver info");
        btnVerInfo.addActionListener(e -> mostrarInformacionConsulta());
        btnVerInfo.setEnabled(false);
        btnLlamarSiguiente = new javax.swing.JButton("Llamar siguiente");
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
        estado.add(jcbEstadoFiltro, java.awt.BorderLayout.CENTER);
        filtros.add(estado);
        herramientas.add(filtros, java.awt.BorderLayout.WEST);

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
        accionesClinicas.add(btnEditar);
        accionesClinicas.add(btnCambiarEstado);
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

    /** Distribución funcional, sin alterar colores ni componentes FlatLaf existentes. */
    private void configurarFormularioAmplio() {
        jpDatos.removeAll();
        jpDatos.setLayout(new java.awt.BorderLayout(0, 22));
        jpDatos.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 28, 22, 28));

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
        filaUno.add(campoConsulta(lbPaciente, jcbPaciente));
        filaUno.add(campoConsulta(lbDueño, txtDueño));
        filaUno.add(campoConsulta(lbMotivo, jcbMotivo));
        filaUno.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 68));
        filaUno.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        centro.add(filaUno);
        centro.add(javax.swing.Box.createVerticalStrut(20));

        javax.swing.JPanel filaDos = new javax.swing.JPanel(new java.awt.GridLayout(1, 3, 18, 0));
        filaDos.setOpaque(false);
        filaDos.add(campoConsulta(lbVeterinarioAsignado, jcbVeterinario));
        filaDos.add(campoConsulta(lbEstado, jcbEstado));
        filaDos.add(campoConsulta(lbPatologia, jcbPatologia));
        filaDos.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 68));
        filaDos.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        centro.add(filaDos);
        centro.add(javax.swing.Box.createVerticalStrut(20));

        javax.swing.JPanel evolucion = campoConsulta(lbTratamiento, jScrollPane1);
        evolucion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(450, 125));
        evolucion.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 155));
        centro.add(evolucion);
        jpDatos.add(centro, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel botones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 0));
        botones.setOpaque(false);
        btnGuardar.setPreferredSize(new java.awt.Dimension(185, 36));
        btnCancelar.setPreferredSize(new java.awt.Dimension(115, 36));
        btnLimpiar.setPreferredSize(new java.awt.Dimension(105, 36));
        botones.add(btnGuardar);
        botones.add(btnCancelar);
        botones.add(btnLimpiar);
        jpDatos.add(botones, java.awt.BorderLayout.SOUTH);
        jpDatos.revalidate();
    }

    private boolean coincideEstadoModelo(int fila) {
        if (jcbEstadoFiltro == null || jcbEstadoFiltro.getSelectedItem() == null)
            return true;
        String estado = normalizarEstadoBD(
                String.valueOf(tableVisitas.getModel().getValueAt(fila, 8)));
        String filtro = jcbEstadoFiltro.getSelectedItem().toString();
        if ("Todas".equals(filtro)) return true;
        if ("Finalizadas".equals(filtro)) return "FINALIZADO".equals(estado);
        return "EN_ESPERA".equals(estado) || "ATENDIENDO".equals(estado);
    }

    private void llamarSiguientePaciente() {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        if (!usuarioClinico() || sesion == null || sesion.getUsuario() == null) {
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
        btnGuardar.setText("Guardar atención");
        mostrarSoloFormulario();
        btnGuardar.setEnabled(true);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
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
            JOptionPane.showMessageDialog(this, "Para comenzar una atención utilice Llamar siguiente.\n"
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
            // El profesional que terminó queda disponible: asignación automática.
            llamarSiguientePaciente();
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
                setText(value == null ? "En curso" : value.toString());
                return this;
            }
        });

        // Aplicar filtro combinado cuando cambia el estado
        jcbEstadoFiltro.addActionListener(e -> { if (paginacionConsultas != null) paginacionConsultas.establecerFiltroAdicional(this::coincideEstadoModelo); });
    }

    private void cargarVisitasEnTabla() {

        cargandoTabla = true;

        try {

            DefaultTableModel model = (DefaultTableModel) tableVisitas.getModel();
            model.setRowCount(0);

            List<Visita> visitas = visitaControlador.obtenerTodasLasVisitas();
            visitas.sort(java.util.Comparator
                    .comparingInt((Visita v) -> {
                        String estado = normalizarEstadoBD(v.getEstado());
                        return "EN_ESPERA".equals(estado) ? 0
                                : "ATENDIENDO".equals(estado) ? 1 : 2;
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

    // -------------------- Impresión PDF --------------------
    private void actualizarBotonImprimir() {

        if (cargandoTabla) {
            return;
        }

        boolean haySeleccion = (obtenerFilaSeleccionadaPorCheck() != -1);

        btnImprimirVisita.setVisible(haySeleccion && !jpDatos.isVisible());
        actualizarAccionesPorSeleccion();
    }

    private void actualizarAccionesPorSeleccion() {

        // Si la tabla está vacía, deshabilitar acciones
        if (tableVisitas.getRowCount() == 0) {
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
            if (btnVerInfo != null) btnVerInfo.setEnabled(false);
            if (btnCambiarEstado != null) btnCambiarEstado.setEnabled(false);
            return;
        }

        int row = obtenerFilaSeleccionadaPorCheck();

        // Validar que la fila exista
        if (row < 0 || row >= tableVisitas.getRowCount()) {
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
            if (btnVerInfo != null) btnVerInfo.setEnabled(false);
            if (btnCambiarEstado != null) btnCambiarEstado.setEnabled(false);
            return;
        }

        boolean esFinalizado = false;

        try {
            Object estadoObj = tableVisitas.getValueAt(row, 8); // Columna Estado
            String estado = estadoObj != null ? estadoObj.toString() : "";

            esFinalizado = "FINALIZADO".equalsIgnoreCase(normalizarEstadoBD(estado))
                    || "CANCELADO".equalsIgnoreCase(normalizarEstadoBD(estado));

        } catch (Exception ex) {
            veterinaria.util.AppLog.warn(FormConsulta.class,
                    "Excepción no fatal en UI", ex);
            return;
        }

        // Reglas de negocio
        if (btnVerInfo != null) btnVerInfo.setEnabled(!jpDatos.isVisible());
        String estadoSeleccionado = normalizarEstadoBD(String.valueOf(tableVisitas.getValueAt(row, 8)));
        btnEditar.setEnabled(usuarioClinico() && "ATENDIENDO".equals(estadoSeleccionado)
                && !jpDatos.isVisible());
        if (btnCambiarEstado != null)
            btnCambiarEstado.setEnabled(usuarioClinico() && "ATENDIENDO".equals(estadoSeleccionado)
                    && !jpDatos.isVisible());
        // Cancelar es una baja lógica, nunca debe habilitarse en FINALIZADO.
        btnEliminar.setEnabled(!esFinalizado && !jpDatos.isVisible()
                && Application.getSesionUsuario() != null
                && Application.getSesionUsuario().puede("FormConsulta.CANCELAR"));
    }

    private void configurarAccionesPorRol() {
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        if (sesion == null) return;
        boolean clinico = usuarioClinico();
        // Los permisos históricos del módulo siguen siendo FormVisita.*.
        btnNuevo.setEnabled(sesion.puede("FormConsulta.NUEVO"));
        btnLlamarSiguiente.setVisible(clinico);
        btnCambiarEstado.setVisible(clinico);
        btnEditar.setVisible(clinico);
        btnEliminar.setVisible(sesion.puede("FormConsulta.CANCELAR"));
        btnEliminar.setText("Cancelar consulta");
        btnImprimirVisita.setEnabled(sesion.puede("FormConsulta.IMPRIMIR"));
        btnImprimirListaVisitas.setEnabled(sesion.puede("FormConsulta.IMPRIMIR"));
    }

    private void mostrarInformacionConsulta() {
        Visita v = obtenerVisitaSeleccionadaDesdeTabla();
        if (v == null) return;
        String fecha = v.getFecha() == null ? "-" : v.getFecha().toString();
        String hora = v.getHora() == null ? "-" : v.getHora().toString();
        String responsable = v.getCliente() == null || v.getCliente().getPersona() == null
                ? "-" : v.getCliente().getPersona().getNombre() + " "
                        + v.getCliente().getPersona().getApellido();
        String texto = "Ticket N.º " + v.getIdVisita()
                + "\nPaciente: " + (v.getMascota() == null ? "-" : v.getMascota().getNombre())
                + "\nResponsable: " + responsable
                + "\nFecha y hora: " + fecha + " " + hora
                + "\nEstado: " + v.getEstado()
                + "\nPrioridad: " + (v.getPrioridadAtencion() == null ? "CONTROL" : v.getPrioridadAtencion())
                + "\nMotivo: " + (v.getMotivoVisita() == null ? "-" : v.getMotivoVisita())
                + "\nVeterinario: " + (v.getUsuarioAtiende() == null || v.getUsuarioAtiende().isBlank()
                        ? "Pendiente de asignación" : v.getUsuarioAtiende())
                + "\nDiagnóstico: " + (v.getPatologia() == null || v.getPatologia().isBlank()
                        ? "Pendiente" : v.getPatologia())
                + "\nEvolución y tratamiento: " + (v.getTratamiento() == null || v.getTratamiento().isBlank()
                        ? "Pendiente" : v.getTratamiento());
        javax.swing.JTextArea area = new javax.swing.JTextArea(texto, 13, 46);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        javax.swing.JScrollPane contenido = new javax.swing.JScrollPane(area);
        contenido.setPreferredSize(new java.awt.Dimension(520, 310));
        JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                contenido, "Información de la consulta · Solo lectura", JOptionPane.PLAIN_MESSAGE);
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
            lbVeterinarioAsignado.setText("Veterinario (asignación automática)");
            jcbEstado.setEnabled(false); // Siempre inicia EN ESPERA; se cambia desde la lista.
            jcbEstado.setSelectedItem(Constantes.EstadoVisita.EN_ESPERA);
            prioridadConsulta.setEnabled(true);
            txtTratamiento.setEditable(false);
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
            lbVeterinarioAsignado.setText("Veterinario asignado");

            jcbEstado.setEnabled(false); // El estado se cambia solo desde Cambiar estado.
            prioridadConsulta.setEnabled(false);
            txtTratamiento.setEditable(usuarioClinico());
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
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tableVisitas)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 🚀 MANDAMOS AL EJECUTOR: Limpio, sin "new" y en segundo plano
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.VISITA_LISTADO, req);
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
        boolean esNueva = visita == null || visita.getIdVisita() == null;
        if (!esNueva && usuarioClinico() && jcbPatologia.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "El veterinario debe completar el diagnóstico.");
            return false;
        }
        String tratamiento = txtTratamiento.getText() != null ? txtTratamiento.getText().trim() : "";
        if (!esNueva && usuarioClinico() && tratamiento.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El veterinario debe registrar el tratamiento o la evolución.");
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
        btnGuardar.setText("Registrar Atención");
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
            btnGuardar.setText("Guardar atención");
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
            btnNuevo.setEnabled(Application.getSesionUsuario() != null
                && Application.getSesionUsuario().puede("FormConsulta.NUEVO"));
            btnEditar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }

    }//GEN-LAST:event_btnEliminarActionPerformed

    /** No cambia el estado del paciente: vincula la atención a una solicitud urgente. */
    private boolean prepararAtencionProvisional() {
        if (mascotaSeleccionada.isActivo()) {
            visita.setAtencionProvisional(false);
            visita.setIdSolicitudReactivacion(null);
            return true;
        }
        veterinaria.util.SesionUsuario sesion = Application.getSesionUsuario();
        String rol = sesion == null || sesion.getRol() == null ? "" :
                sesion.getRol().getNombreRol();
        if (!"Veterinario".equalsIgnoreCase(rol)
                && !"Administrador".equalsIgnoreCase(rol)) {
            JOptionPane.showMessageDialog(this,
                    "El paciente está inactivo. Solo el veterinario o administrador "
                    + "pueden registrar una atención urgente provisoria.",
                    "Atención restringida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        Integer solicitud;
        try {
            solicitud = new veterinaria.servicio.SolicitudReactivacionService()
                    .solicitudUrgentePendiente(mascotaSeleccionada.getIdMascota());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo verificar la solicitud urgente.\n"
                    + "Consulte al administrador o verifique la migración SQL.",
                    "Verificación pendiente", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (solicitud == null) {
            JOptionPane.showMessageDialog(this,
                    "El paciente está inactivo. Para registrar una atención urgente provisoria, "
                    + "primero debe existir una solicitud urgente que aún no haya sido resuelta.",
                    "Solicitud urgente requerida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        int confirmacion = JOptionPane.showConfirmDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "La mascota " + mascotaSeleccionada.getNombre() + " figura como inactiva.\n\n"
                + "Se guardará una CONSULTA PROVISORIA por una atención urgente.\n"
                + "Quedará relacionada con la solicitud de reactivación N.º " + solicitud + ".\n"
                + "La consulta NO reactiva a la mascota ni modifica su motivo de baja.\n\n"
                + "¿Desea guardar esta atención urgente?",
                "Confirmar consulta provisoria", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) return false;
        visita.setAtencionProvisional(true);
        visita.setIdSolicitudReactivacion(solicitud);
        return true;
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
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
            visita.setUsuarioGestion(userGestion);
            visita.setMascota(mascotaSeleccionada);
            visita.setCliente(mascotaSeleccionada.getCliente());
            visita.setMotivoVisita((String) jcbMotivo.getSelectedItem());
            boolean registroInicial = visita.getIdVisita() == null;
            if (registroInicial) {
                visita.setPatologia("");
                visita.setTratamiento("");
            } else {
                visita.setPatologia((String) jcbPatologia.getSelectedItem());
                visita.setTratamiento(txtTratamiento.getText() != null ? txtTratamiento.getText().trim() : "");
            }
            visita.setFecha(jdcFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            visita.setHora(((Date) spinnerHora.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
            // EstadoVisita ahora es un enum (no String)
            Constantes.EstadoVisita estadoSel = (Constantes.EstadoVisita) jcbEstado.getSelectedItem();
            visita.setEstado(estadoSel != null ? estadoSel.name() : Constantes.EstadoVisita.EN_ESPERA.name());
            if (registroInicial) {
                visita.setUsuarioAtiende("");
                visita.setPrioridadAtencion(String.valueOf(prioridadConsulta.getSelectedItem()));
            }

            try {
                boolean esNueva = (visita.getIdVisita() == null);
                if (!confirmarGuardarVisita(esNueva)) {
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
                    visitaControlador.actualizarVisita(visita);
                    JOptionPane.showMessageDialog(this, "Visita actualizada con éxito.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al registrar la visita: " + e.getMessage());
                return;
            }

            cargarVisitasEnTabla();
            limpiarCamposFormulario();
            jpDatos.setVisible(false);
            jpHeader.setVisible(true);
            if (herramientasConsultas != null) herramientasConsultas.setVisible(true);
            btnNuevo.setEnabled(Application.getSesionUsuario() != null
                && Application.getSesionUsuario().puede("FormConsulta.NUEVO"));
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
        jpHeader.setVisible(true);
        if (herramientasConsultas != null) herramientasConsultas.setVisible(true);
        btnNuevo.setEnabled(Application.getSesionUsuario() != null
                && Application.getSesionUsuario().puede("FormConsulta.NUEVO"));
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
