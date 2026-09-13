package veterinaria.vista;

import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.LaboratorioControlador;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.MascotaControlador;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Laboratorio;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.servicio.ConfiguracionService;
import veterinaria.servicio.AgendaSlotService;
import veterinaria.servicio.LaboratorioService;
import veterinaria.util.Constantes;
import veterinaria.util.ManejoTablas;
import veterinaria.util.UtilidadesTabla;
import veterinaria.util.Validaciones;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.ClienteMascotaSelector;
import veterinaria.util.ui.HistoriaEventoAbrible;
import veterinaria.vista.application.Application;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.vista.componentes.InformeLaboratorioDialog;
import veterinaria.vista.componentes.WhatsAppMensajeDialog;

public class FormLaboratorio extends javax.swing.JPanel implements HistoriaEventoAbrible {

    private static final List<Integer> COLUMNAS_BUSQUEDA_TABLA = Arrays.asList(1, 2, 3);
    private ClienteMascotaSelector clienteMascotaSelector;
    private final ClienteControlador clienteControlador = new ClienteControlador();
    private final Validaciones validar = new Validaciones();
    private Laboratorio laboratorio = new Laboratorio();
    private final LaboratorioControlador laboratorioControlador = new LaboratorioControlador();
    private final MascotaControlador mascotaControlador = new MascotaControlador();
    private final ConfiguracionService configService = new ConfiguracionService();
    private final AgendaSlotService slotService = new AgendaSlotService();
    private final LaboratorioService laboratorioService = new LaboratorioService();
    private Mascota mascotaSeleccionada;

    private LocalDate fechaExtraccionOriginal;
    private boolean actualizandoFormulario = false;

    private Integer laboratorioInformeId = null;

    private final java.util.List<AgendaSlot> slotsComboExtraccion = new java.util.ArrayList<>();
    private java.util.List<Usuario> listaVeterinarios = new java.util.ArrayList<>();
    private Boolean editandoPedido = false;

    private boolean requiereConfirmacionReservaTurno(Laboratorio lab) {
        if (lab == null) {
            return true;
        }
        if (lab.getIdLaboratorio() == null) {
            return true;
        }
        String estado = lab.getEstado();
        return (estado == null) || Constantes.ESTADO_LABORATORIO_PENDIENTE.equals(estado);
    }

    private boolean puedeReservarOReprogramarTurno(Laboratorio lab) {
        if (lab == null) {
            return true;
        }
        if (lab.getIdLaboratorio() == null) {
            return true;
        }
        String estado = lab.getEstado();
        return (estado == null) || Constantes.ESTADO_LABORATORIO_PENDIENTE.equals(estado);
    }

    public FormLaboratorio() {
        initComponents();
        PermisoUI.aplicar(this);

        // UX/UI: la botonera del header NO se deshabilita; se oculta según el estado
        // para evitar "ruido" visual.
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
        inicializarFiltroEstado();
        mostrarSoloPedidosLaboratorio();
        cargarCombosBoxs();
        cargarComboVeterinarios();
        llenarComboBoxMotivo();
        configurarSelectorFechaYTurnoLaboratorio();
        cargarLaboratorioEnTabla();
        initListeners();
        initTableSelectionListener();
        UtilidadesTabla.ajustarAnchoColumnas(tablePedidosLaboratorio);
        ManejoTablas manejoTablas = new ManejoTablas();
        manejoTablas.asegurarSeleccionUnica(tablePedidosLaboratorio);
        java.util.Map<Integer, String> tips = new java.util.HashMap<>();
        tips.put(0, "Seleccionar un pedido (se permite sólo uno).");
        tips.put(1, "Número/ID del pedido de extracción.");
        tips.put(2, "Paciente (mascota).");
        tips.put(3, "Cliente / dueño.");
        tips.put(4, "Fecha y hora programadas para la extracción.");
        tips.put(5, "Motivo de la extracción.");
        tips.put(6, "Tipo de análisis solicitado.");
        tips.put(7, "Diagnóstico o nota clínica asociada.");
        tips.put(8, "Fecha de envío a laboratorio.");
        tips.put(9, "Fecha de recepción del laboratorio.");
        tips.put(10, "Estado actual del pedido.");
        ManejoTablas.aplicarTooltipsPorColumna(tablePedidosLaboratorio, tips);
        ManejoTablas.aplicarFiltroYResaltado(tablePedidosLaboratorio, txtBusqueda, COLUMNAS_BUSQUEDA_TABLA);
    }

    /**
     * Selecciona un pedido de laboratorio puntual (modo lectura) desde Historia
     * Clínica.
     */
    @Override
    public void abrirDetallePorId(Integer refId) {
        if (refId == null) {
            return;
        }

        Integer viewRow = buscarFilaPorId(refId);
        if (viewRow == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el pedido de laboratorio #" + refId + " en la lista.",
                    "No encontrado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            tablePedidosLaboratorio.setRowSelectionInterval(viewRow, viewRow);
            tablePedidosLaboratorio.scrollRectToVisible(tablePedidosLaboratorio.getCellRect(viewRow, 0, true));
            actualizarBotoneraSegunSeleccion();
        } catch (Exception ignore) {
        }
    }

    private Integer buscarFilaPorId(Integer idLaboratorio) {
        try {
            for (int viewRow = 0; viewRow < tablePedidosLaboratorio.getRowCount(); viewRow++) {
                int modelRow = tablePedidosLaboratorio.convertRowIndexToModel(viewRow);
                Object val = tablePedidosLaboratorio.getModel().getValueAt(modelRow, 1); // ID en col 1
                if (val != null && idLaboratorio.toString().equals(val.toString())) {
                    return viewRow;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private void cargarCombosBoxs() {
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
                    if (jdcFechaDeExtraccion.getDate() != null) {
                        LocalDate f = jdcFechaDeExtraccion.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (!editandoPedido) {
                            cargarTurnosComboParaFechaLaboratorio(f);
                        }
                    } else {
                        limpiarTurnosComboLaboratorio();
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

    private void seleccionarVeterinarioEnCombo(String nombreGuardado) {
        if (nombreGuardado == null || nombreGuardado.trim().isEmpty()) {
            jcbVeterinario.setSelectedIndex(0);
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
        jcbVeterinario.setSelectedIndex(0);
    }

    // Nota: la selección de Mascota se gestiona por ClienteMascotaSelector,
    // que actualiza mascotaSeleccionada automáticamente.
    private void llenarComboBoxMotivo() {
        jcbMotivo.removeAllItems();
        jcbMotivo.addItem("Seleccione un motivo");
        jcbMotivo.addItem("Chequeo general de salud");
        jcbMotivo.addItem("Evaluación preoperatoria");
        jcbMotivo.addItem("Diagnóstico de enfermedades infecciosas");
        jcbMotivo.addItem("Control de enfermedades crónicas");
        jcbMotivo.addItem("Detección de alergias");
        jcbMotivo.setSelectedIndex(0);
        txtTipoAnalisis.setText("");

        // Asocia el listener para cambiar tipo de análisis cuando el motivo cambia
        jcbMotivo.addActionListener(e -> actualizarTipoDeAnalisis());
    }

    private void actualizarTipoDeAnalisis() {
        String motivoSeleccionado = (String) jcbMotivo.getSelectedItem();
        if (motivoSeleccionado == null || "Seleccione un motivo".equalsIgnoreCase(motivoSeleccionado.trim())) {
            txtTipoAnalisis.setText("");
            return;
        }
        if (motivoSeleccionado != null) {
            switch (motivoSeleccionado) {
                case "Chequeo general de salud":
                    txtTipoAnalisis.setText("Análisis de sangre completo");
                    break;
                case "Evaluación preoperatoria":
                    txtTipoAnalisis.setText("Perfil preoperatorio");
                    break;
                case "Diagnóstico de enfermedades infecciosas":
                    txtTipoAnalisis.setText("Pruebas de enfermedades infecciosas");
                    break;
                case "Control de enfermedades crónicas":
                    txtTipoAnalisis.setText("Perfil de control crónico");
                    break;
                case "Detección de alergias":
                    txtTipoAnalisis.setText("Pruebas de alergias");
                    break;
                default:
                    txtTipoAnalisis.setText("");
            }
        }
    }

    private void initListeners() {
        // La búsqueda en la tabla se gestiona por ManejoTablas.aplicarFiltroYResaltado(...)

        // Filtro por estado (incluye "Cancelado" pero por defecto "Todos" excluye cancelados)
        if (jcbFiltrarEstado != null) {
            jcbFiltrarEstado.addActionListener(e -> cargarLaboratorioEnTabla());
        }

        // Acciones de informe
        btnVerInforme.addActionListener(e -> btnVerInformeAction());
        // IMPORTANTE:
        // btnImprimir ya tiene un ActionListener generado por NetBeans (btnImprimirActionPerformed).
        // Si agregamos otro listener acá, el botón dispara 2 acciones:
        // 1) JTable.print() (captura/impresión de tabla)
        // 2) Generación del PDF de informe
        // Eso termina abriendo el diálogo de impresión y hasta una "captura" de la tabla.
        // Dejamos un único flujo: generar el PDF del informe.
    }

    /**
     * Inicializa el combo de filtrado por estado. - Valor por defecto: "Todos"
     * (carga todos excepto Cancelado) - Permite seleccionar estados
     * específicos, incluyendo Cancelado
     */
    private void inicializarFiltroEstado() {
        if (jcbFiltrarEstado == null) {
            return;
        }

        jcbFiltrarEstado.removeAllItems();
        jcbFiltrarEstado.addItem("Todos");
        jcbFiltrarEstado.addItem(Constantes.ESTADO_LABORATORIO_PENDIENTE);
        jcbFiltrarEstado.addItem(Constantes.ESTADO_LABORATORIO_PROCESADO);
        jcbFiltrarEstado.addItem(Constantes.ESTADO_LABORATORIO_ENVIADO);
        jcbFiltrarEstado.addItem(Constantes.ESTADO_LABORATORIO_RECIBIDO);
        jcbFiltrarEstado.addItem(Constantes.ESTADO_LABORATORIO_COMPLETADO);
        jcbFiltrarEstado.addItem(Constantes.ESTADO_LABORATORIO_CANCELADO);
        jcbFiltrarEstado.setSelectedItem("Todos");
    }

    private void mostrarSoloRegistrarExtracciones() {
        jpRegistrarExtracciones.setVisible(true);
        jpPedidosLaboratorio.setVisible(false);
        jpaRecibirMuestra.setVisible(false);
    }

    private void mostrarSoloPedidosLaboratorio() {
        jpRegistrarExtracciones.setVisible(false);
        jpPedidosLaboratorio.setVisible(true);
        jpaRecibirMuestra.setVisible(false);
    }

    private void mostrarSoloRecibirMuestra() {
        jpRegistrarExtracciones.setVisible(false);
        jpPedidosLaboratorio.setVisible(false);
        jpaRecibirMuestra.setVisible(true);
    }

    private void resetearBotonesAccion() {
        // Acciones contextuales (se muestran sólo cuando corresponde)
        btnConfirmarExtraccion.setVisible(false);
        btnEnviarLaboratorio.setVisible(false);
        btnRecibirLaboratorio.setVisible(false);

        // Acciones dependientes de selección
        btnVerInforme.setVisible(false);
        btnImprimir.setVisible(false);
    }

    /**
     * UX/UI: los botones del header se ocultan en lugar de deshabilitarse. Esto
     * evita ruido visual y comunica mejor el estado real.
     */
    private void ocultarBotonesHeaderPorDefecto() {
        // Botones dependientes de selección/estado
        btnEditarTurno.setVisible(false);
        btnCancelarTurno.setVisible(false);
        btnConfirmarExtraccion.setVisible(false);
        btnEnviarLaboratorio.setVisible(false);
        btnRecibirLaboratorio.setVisible(false);
        btnVerInforme.setVisible(false);
        btnImprimir.setVisible(false);
    }

    private void initTableSelectionListener() {
        tablePedidosLaboratorio.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            actualizarBotoneraSegunSeleccion();
        });

        tablePedidosLaboratorio.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                actualizarBotoneraSegunSeleccion();
            }
        });

        jcbFiltrarEstado.addActionListener(e -> {
            cargarLaboratorioEnTabla();
            // Mantener búsqueda vigente al recargar la tabla
            ManejoTablas.refrescarFiltro(tablePedidosLaboratorio, txtBusqueda, COLUMNAS_BUSQUEDA_TABLA);
        });
    }

    private void actualizarBotoneraSegunSeleccion() {
        int fila = tablePedidosLaboratorio.getSelectedRow();
        if (fila < 0) {
            ocultarBotonesHeaderPorDefecto();
            resetearBotonesAccion();
            return;
        }

        // Partimos de un estado "limpio" y vamos habilitando (MOSTRANDO) acciones.
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();

        btnEditarTurno.setVisible(true);

        Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getValueAt(fila, 1);
        Laboratorio lab = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);
        if (lab == null) {
            ocultarBotonesHeaderPorDefecto();
            return;
        }

        // Acciones de informe: sólo tienen sentido si hay selección.
        btnVerInforme.setVisible(true);
        btnImprimir.setVisible(true);

        if (!puedeGestionarLaboratorio(lab)) {
            // Sin permiso: ocultamos todas las acciones para evitar confusión.
            ocultarBotonesHeaderPorDefecto();
            JOptionPane.showMessageDialog(this, "Este laboratorio fue cargado por otro veterinario/usuario. No tiene permiso para recibir ni emitir el informe.", "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String estado = lab.getEstado();
        if (estado == null) {
            // Sin estado definido: no mostramos acciones de flujo.
            return;
        }

        // Requisito: mostrar Cancelar únicamente cuando el estado sea PENDIENTE o PROCESADO.
        boolean puedeCancelar = Constantes.ESTADO_LABORATORIO_PENDIENTE.equals(estado)
                || Constantes.ESTADO_LABORATORIO_PROCESADO.equals(estado);
        btnCancelarTurno.setVisible(puedeCancelar);

        if (Constantes.ESTADO_LABORATORIO_PENDIENTE.equals(estado)) {
            btnConfirmarExtraccion.setVisible(true);
        } else if (Constantes.ESTADO_LABORATORIO_PROCESADO.equals(estado)) {
            btnEnviarLaboratorio.setVisible(true);
        } else if (Constantes.ESTADO_LABORATORIO_ENVIADO.equals(estado)) {
            btnRecibirLaboratorio.setText("Recibir de Laboratorio");
            btnRecibirLaboratorio.setVisible(true);
        } else if (Constantes.ESTADO_LABORATORIO_RECIBIDO.equals(estado)) {
            btnRecibirLaboratorio.setText("Generar Informe");
            btnRecibirLaboratorio.setVisible(true);
        }

        // Refrescar layout del header (Swing puede no recalcular layout si sólo cambia visibilidad)
        jpHeader.revalidate();
        jpHeader.repaint();
    }

    private List<Laboratorio> obtenerLaboratoriosVisibles() {
        try {
            if (esAdminActual()) {
                return laboratorioControlador.obtenerTodosLosLaboratorios();
            }
            String usuarioActual = Application.getNombreApellidoUsuarioLogeado();
            if (usuarioActual == null || usuarioActual.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return laboratorioControlador.obtenerLaboratoriosPorUsuarioGestion(usuarioActual);
        } catch (Exception ex) {
            // Ante cualquier falla, devolvemos lista vacía para no romper la UI
            return new ArrayList<>();
        }
    }

    private boolean puedeGestionarLaboratorio(Laboratorio lab) {
        if (lab == null) {
            return false;
        }
        if (esAdminActual()) {
            return true;
        }

        String usuarioActualNombre = Application.getNombreApellidoUsuarioLogeado();

        // 1) Por usuarioGestion (string)
        if (usuarioActualNombre != null && lab.getUsuarioGestion() != null
                && lab.getUsuarioGestion().equalsIgnoreCase(usuarioActualNombre)) {
            return true;
        }

        try {
            Usuario u = (Application.getSesionUsuario() != null) ? Application.getSesionUsuario().getUsuario() : null;
            Integer idUsuarioActual = (u != null) ? u.getIdUsuario() : null;

            if (idUsuarioActual != null && lab.getVeterinario() != null && lab.getVeterinario().getIdUsuario() != null) {
                return idUsuarioActual.equals(lab.getVeterinario().getIdUsuario());
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean esAdminActual() {
        try {
            if (Application.getSesionUsuario() == null || Application.getSesionUsuario().getRol() == null) {
                return false;
            }
            String rol = Application.getSesionUsuario().getRol().getNombreRol();
            return rol != null && rol.toLowerCase().contains("admin");
        } catch (Exception ex) {
            return false;
        }
    }

    private void actualizarTabla() {
        // La selección única ahora se gestiona por ManejoTablas.asegurarSeleccionUnica(...)
        tablePedidosLaboratorio.repaint();
    }

    private void cargarLaboratorioEnTabla() {
        DefaultTableModel model = (DefaultTableModel) tablePedidosLaboratorio.getModel();
        model.setRowCount(0);
        List<Laboratorio> laboratorios = obtenerLaboratoriosVisibles();
        String estadoFiltro = (jcbFiltrarEstado != null && jcbFiltrarEstado.getSelectedItem() != null)
                ? String.valueOf(jcbFiltrarEstado.getSelectedItem())
                : "Todos";

        for (Laboratorio laboratorio : laboratorios) {

            String estadoActual = (laboratorio.getEstado() != null) ? laboratorio.getEstado() : "";
            if ("Todos".equalsIgnoreCase(estadoFiltro)) {
                if (Constantes.ESTADO_LABORATORIO_CANCELADO.equalsIgnoreCase(estadoActual)) {
                    continue;
                }
            } else {
                if (!estadoFiltro.equalsIgnoreCase(estadoActual)) {
                    continue;
                }
            }

            String fechaExtraccion = "";
            if (laboratorio.getFechaExtraccion() != null) {
                fechaExtraccion = laboratorio.getFechaExtraccion().toString();
                if (laboratorio.getHoraExtraccion() != null) {
                    fechaExtraccion = fechaExtraccion + " " + laboratorio.getHoraExtraccion().toString();
                }
            } else {
                fechaExtraccion = "Fecha de ingreso no disponible";
            }
            String nombreMascota = (laboratorio.getMascota() != null) ? laboratorio.getMascota().getNombre() : "Sin mascota";
            String cliente = "Sin cliente";

            if (laboratorio.getCliente() != null && laboratorio.getCliente().getPersona() != null) {
                cliente = laboratorio.getCliente().getPersona().getNombre() + " " + laboratorio.getCliente().getPersona().getApellido();
            }

            Object[] fila = {
                false,
                laboratorio.getIdLaboratorio(),
                nombreMascota,
                cliente,
                fechaExtraccion,
                laboratorio.getMotivoExtraccion(),
                laboratorio.getTipoAnalisis(), // Correcto
                laboratorio.getDiagnostico(), // Correcto
                laboratorio.getFechaEnvio(),
                laboratorio.getFechaRecepcion(),
                laboratorio.getEstado()
            };
            model.addRow(fila);
        }
        // Si hay texto en búsqueda, mantenerlo aplicado luego de recargar el modelo
        ManejoTablas.refrescarFiltro(tablePedidosLaboratorio, txtBusqueda, COLUMNAS_BUSQUEDA_TABLA);
    }

    private boolean editarLaboratorioSeleccionado() {
        boolean bRetorno = false;
        int filaSeleccionada = tablePedidosLaboratorio.getSelectedRow();

        if (filaSeleccionada >= 0) {
            Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getValueAt(filaSeleccionada, 1);
            laboratorio = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);

            if (laboratorio != null) {
                fechaExtraccionOriginal = laboratorio.getFechaExtraccion();
                mascotaSeleccionada = laboratorio.getMascota();
                // Preselección cliente/mascota en combos dependientes
                try {
                    Integer idCliente = (laboratorio.getCliente() != null) ? laboratorio.getCliente().getIdCliente() : null;
                    Integer idMascota = (laboratorio.getMascota() != null) ? laboratorio.getMascota().getIdMascota() : null;
                    if (clienteMascotaSelector != null) {
                        clienteMascotaSelector.seleccionarPorIds(idCliente, idMascota);
                    }
                } catch (Exception ignore) {
                }

                Date fechaExtraccion = Date.from(laboratorio.getFechaExtraccion().atStartOfDay(ZoneId.systemDefault()).toInstant());
                actualizandoFormulario = true;
                jdcFechaDeExtraccion.setDate(fechaExtraccion);
                actualizandoFormulario = false;

                // Veterinario + combo de turnos en edición
                try {
                    if (laboratorio.getVeterinario() != null) {
                        String nombreVet = nombreCompletoUsuario(laboratorio.getVeterinario());
                        seleccionarVeterinarioEnCombo(nombreVet);
                    } else {
                        seleccionarVeterinarioEnCombo(laboratorio.getUsuarioAtiende());
                    }
                    LocalDate f = laboratorio.getFechaExtraccion();
                    Usuario vetSel = getVeterinarioSeleccionado();
                    Long idSlotActual = (laboratorio.getSlot() != null) ? laboratorio.getSlot().getIdSlot() : null;
                    cargarTurnosComboParaEdicionLaboratorio(f, vetSel, idSlotActual);
                } catch (Exception ignore) {
                }
                jcbMotivo.setSelectedItem(laboratorio.getMotivoExtraccion());
                taDiagnostico.setText(laboratorio.getDiagnostico());
                txtTipoAnalisis.setText(laboratorio.getTipoAnalisis());

                bRetorno = true;
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "¡Debe seleccionar un registro a editar!", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;
    }

    private boolean cancelarExtraccionSeleccionada() {
        int filaSeleccionada = tablePedidosLaboratorio.getSelectedRow();

        if (filaSeleccionada != -1) {
            Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getValueAt(filaSeleccionada, 1);
            laboratorio = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);

            if (laboratorio != null) {
                int respuesta = JOptionPane.showConfirmDialog(
                        this,
                        "¿Estás seguro de que deseas cancelar esta extracción?",
                        "Confirmar cancelación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (respuesta == JOptionPane.YES_OPTION) {
                    boolean ok = laboratorioService.cancelarExtraccion(laboratorio.getIdLaboratorio());
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Extracción cancelada con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);
                        return true;
                    }
                    JOptionPane.showMessageDialog(this, "No se pudo cancelar la extracción (puede haber sido modificada).", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    return false;
                } else {
                    tablePedidosLaboratorio.setValueAt(false, filaSeleccionada, 0);
                    tablePedidosLaboratorio.clearSelection();
                }
            } else {
                JOptionPane.showMessageDialog(this, "La extracción seleccionada no se encontró.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una extracción para cancelar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return false;
    }

    private void limpiarCamposFormulario() {
        jcbClienteDueño.setEnabled(true);
        jcbPaciente.setEnabled(true);
        try {
            if (clienteMascotaSelector != null) {
                clienteMascotaSelector.reset();
            } else {
                // fallback
                jcbClienteDueño.setSelectedIndex(0);
                jcbPaciente.setSelectedIndex(0);
            }
        } catch (Exception ignore) {
            jcbClienteDueño.setSelectedIndex(0);
            jcbPaciente.setSelectedIndex(0);
        }
        mascotaSeleccionada = null;
        jcbMotivo.setSelectedIndex(0);
        taDiagnostico.setText("");
        txtTipoAnalisis.setText("");

        limpiarTurnosComboLaboratorio();

        fechaExtraccionOriginal = null;
        actualizandoFormulario = true;
        jdcFechaDeExtraccion.setDate(new Date());
        actualizandoFormulario = false;

        try {
            LocalDate f = jdcFechaDeExtraccion.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (esDiaHabilitadoLaboratorio(f) && getVeterinarioSeleccionado() != null) {
                cargarTurnosComboParaFechaLaboratorio(f);
            }
        } catch (Exception ignore) {
        }

    }

    private void configurarSelectorFechaYTurnoLaboratorio() {
        try {
            LocalDate fechaActual = LocalDate.now();
            Date fechaMinima = java.sql.Date.valueOf(fechaActual);
            jdcFechaDeExtraccion.setMinSelectableDate(fechaMinima);

            jdcFechaDeExtraccion.getDateEditor().addPropertyChangeListener("date", evt -> {
                if (actualizandoFormulario) {
                    return;
                }
                Date d = (Date) evt.getNewValue();
                if (d == null) {
                    limpiarTurnosComboLaboratorio();
                    return;
                }

                LocalDate fecha = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                // Reaseguro: no pasado
                if (fecha.isBefore(LocalDate.now())) {
                    JOptionPane.showMessageDialog(this, "No se puede seleccionar un día pasado.");
                    actualizandoFormulario = true;
                    jdcFechaDeExtraccion.setDate(null);
                    actualizandoFormulario = false;
                    limpiarTurnosComboLaboratorio();
                    return;
                }

                // Día habilitado para Laboratorio
                if (!esDiaHabilitadoLaboratorio(fecha)) {
                    JOptionPane.showMessageDialog(this, "El día seleccionado no está habilitado para Laboratorio.\nConfigurá los días en Configuración.", "Día no habilitado", JOptionPane.WARNING_MESSAGE);
                    actualizandoFormulario = true;
                    jdcFechaDeExtraccion.setDate(null);
                    actualizandoFormulario = false;
                    limpiarTurnosComboLaboratorio();
                    return;
                }

                // Cargar slots libres (si hay veterinario seleccionado)
                cargarTurnosComboParaFechaLaboratorio(fecha);
            });

            // Si ya hay una fecha cargada al abrir el formulario, precargar combo.
            try {
                if (jdcFechaDeExtraccion.getDate() != null) {
                    LocalDate f = jdcFechaDeExtraccion.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    cargarTurnosComboParaFechaLaboratorio(f);
                }
            } catch (Exception ignore2) {
            }
        } catch (Exception ignore) {
            // Si el DateEditor no está disponible, al menos dejamos el minSelectableDate.
        }
    }

    private void limpiarTurnosComboLaboratorio() {
        try {
            slotsComboExtraccion.clear();
            if (jcbTurnoExtracion != null) {
                jcbTurnoExtracion.setModel(new DefaultComboBoxModel<>(new String[]{"Seleccione un turno"}));
                jcbTurnoExtracion.setSelectedIndex(0);
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * Carga el combo jcbTurnoExtracion con slots libres para la
     * fecha/veterinario. Laboratorio usa la misma lógica de turnos por duración
     * que el resto de servicios.
     */
    private void cargarTurnosComboParaFechaLaboratorio(LocalDate fecha) {
        limpiarTurnosComboLaboratorio();
        if (fecha == null) {
            return;
        }
        Usuario vet = getVeterinarioSeleccionado();
        if (vet == null) {
            // Aún sin veterinario seleccionado: dejar placeholder.
            return;
        }

        List<AgendaSlot> libres = slotService.listarLibres(fecha, vet);
        if (libres == null || libres.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay horarios disponibles para la fecha/veterinario seleccionado.", "Sin turnos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione un turno");
        for (AgendaSlot s : libres) {
            slotsComboExtraccion.add(s);
            model.addElement(s.getHoraInicio() + " - " + s.getHoraFin());
        }

        if (slotsComboExtraccion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay horarios disponible para la fecha seleccionada.\nSeleccione otro día para programar un turno.", "Sin turnos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        jcbTurnoExtracion.setModel(model);
        jcbTurnoExtracion.setSelectedIndex(0);
    }

    /**
     * Variante para edición: incluye el slot actual aunque esté reservado.
     */
    private void cargarTurnosComboParaEdicionLaboratorio(LocalDate fecha, Usuario vet, Long idSlotActual) {
        limpiarTurnosComboLaboratorio();
        if (fecha == null || vet == null) {
            return;
        }

        List<AgendaSlot> libres = slotService.listarLibres(fecha, vet);
        if (libres == null) {
            libres = new ArrayList<>();
        }

        // Incluir slot actual (si existe) aunque no esté libre
        AgendaSlot actual = (laboratorio != null) ? laboratorio.getSlot() : null;
        if (actual != null && idSlotActual != null && idSlotActual.equals(actual.getIdSlot())) {
            boolean yaEsta = false;
            for (AgendaSlot s : libres) {
                if (s.getIdSlot().equals(actual.getIdSlot())) {
                    yaEsta = true;
                    break;
                }
            }
            if (!yaEsta) {
                libres.add(0, actual);
            }
        }

        if (libres.isEmpty()) {
            return;
        }

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione un turno");

        int selectIndex = 0;
        for (AgendaSlot s : libres) {
            boolean esActual = (actual != null && s.getIdSlot().equals(actual.getIdSlot()));
            slotsComboExtraccion.add(s);
            String label = s.getHoraInicio() + " - " + s.getHoraFin();
            if (esActual) {
                label = label + " (Actual)";
                selectIndex = slotsComboExtraccion.size(); // +1 por placeholder
            }
            model.addElement(label);
        }

        jcbTurnoExtracion.setModel(model);
        jcbTurnoExtracion.setSelectedIndex(selectIndex);
    }

    private boolean esDiaHabilitadoLaboratorio(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        String diasCsv = configService.getString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_LABORATORIO,
                ConfiguracionService.DEFAULT_DIAS_HABILITADOS_LABORATORIO
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

    private AgendaSlot getSlotSeleccionadoCombo() {
        int idx = jcbTurnoExtracion.getSelectedIndex();
        if (idx <= 0) {
            return null;
        }
        int mapIndex = idx - 1;
        if (mapIndex < 0 || mapIndex >= slotsComboExtraccion.size()) {
            return null;
        }
        return slotsComboExtraccion.get(mapIndex);
    }

    private boolean validarCamposFormulario() {
        if (mascotaSeleccionada == null || jcbPaciente.getSelectedItem() == null || jcbPaciente.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente.");
            return false;
        }
        if (mascotaSeleccionada.getCliente() == null) {
            JOptionPane.showMessageDialog(this, "La mascota seleccionada no tiene un cliente asociado.");
            return false;
        }
        if (jcbMotivo.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un motivo de extracción.");
            return false;
        }
        if (taDiagnostico.getText() == null || taDiagnostico.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el diagnóstico.");
            return false;
        }

        return true; // Todos los campos están completos
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbLaboratorio = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbBusqueda = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnAgendarExtraccion = new javax.swing.JButton();
        btnEditarTurno = new javax.swing.JButton();
        btnCancelarTurno = new javax.swing.JButton();
        btnImprimir = new javax.swing.JButton();
        btnEnviarLaboratorio = new javax.swing.JButton();
        btnRecibirLaboratorio = new javax.swing.JButton();
        btnConfirmarExtraccion = new javax.swing.JButton();
        jcbFiltrarEstado = new javax.swing.JComboBox<>();
        btnVerInforme = new javax.swing.JButton();
        jpRegistrarExtracciones = new javax.swing.JPanel();
        lbRegistarExtracciones = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jcbPaciente = new javax.swing.JComboBox<>();
        lbPaciente = new javax.swing.JLabel();
        lbFechaIngreso = new javax.swing.JLabel();
        lbMotivo = new javax.swing.JLabel();
        jcbMotivo = new javax.swing.JComboBox<>();
        jdcFechaDeExtraccion = new com.toedter.calendar.JDateChooser();
        btnGuardarTurno = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        txtTipoAnalisis = new javax.swing.JTextField();
        jcbTurnoExtracion = new javax.swing.JComboBox<>();
        lbTurnoExtraccion = new javax.swing.JLabel();
        jcbVeterinario = new javax.swing.JComboBox<>();
        lbVeterinarioAsignado = new javax.swing.JLabel();
        jcbClienteDueño = new javax.swing.JComboBox();
        lbPaciente1 = new javax.swing.JLabel();
        jpaRecibirMuestra = new javax.swing.JPanel();
        lbRegistarExtracciones1 = new javax.swing.JLabel();
        btnGuardarInformeFinal = new javax.swing.JButton();
        btnCancelarInformeFinal = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        taDiagnostico = new javax.swing.JTextArea();
        jpPedidosLaboratorio = new javax.swing.JPanel();
        lbListaDePacientes = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        spPedidosLaboratorio = new javax.swing.JScrollPane();
        tablePedidosLaboratorio = new veterinaria.vista.table.AutoTable();
        jSeparator6 = new javax.swing.JSeparator();

        lbLaboratorio.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbLaboratorio.setText("Gestión de Pedidos Extracción - Laboratorio Clínico de Mascotas");

        lbBusqueda.setText("BUSCAR:");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        btnAgendarExtraccion.setText("Agendar Extracción");
        btnAgendarExtraccion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgendarExtraccionActionPerformed(evt);
            }
        });

        btnEditarTurno.setText("Editar");
        btnEditarTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarTurnoActionPerformed(evt);
            }
        });

        btnCancelarTurno.setText("Cancelar");
        btnCancelarTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarTurnoActionPerformed(evt);
            }
        });

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        btnEnviarLaboratorio.setText("Enviar a Laboratorio");
        btnEnviarLaboratorio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarLaboratorioActionPerformed(evt);
            }
        });

        btnRecibirLaboratorio.setText("Recibir de Laboratorio");
        btnRecibirLaboratorio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRecibirLaboratorioActionPerformed(evt);
            }
        });

        btnConfirmarExtraccion.setText("Confirmar Extracción");
        btnConfirmarExtraccion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarExtraccionActionPerformed(evt);
            }
        });

        jcbFiltrarEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos" }));
        jcbFiltrarEstado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbFiltrarEstadoActionPerformed(evt);
            }
        });

        btnVerInforme.setText("Ver ");

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                        .addGap(0, 570, Short.MAX_VALUE)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(btnConfirmarExtraccion)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEnviarLaboratorio)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRecibirLaboratorio))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(btnAgendarExtraccion)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEditarTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnCancelarTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(btnVerInforme)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnImprimir))))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(19, 19, 19)
                                .addComponent(lbLaboratorio))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbBusqueda)
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addGap(4, 4, 4)
                                        .addComponent(lbBuscar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jcbFiltrarEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbLaboratorio)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbBusqueda)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnImprimir)
                        .addComponent(jcbFiltrarEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnVerInforme))
                    .addComponent(lbBuscar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancelarTurno)
                    .addComponent(btnEditarTurno)
                    .addComponent(btnAgendarExtraccion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEnviarLaboratorio)
                    .addComponent(btnRecibirLaboratorio)
                    .addComponent(btnConfirmarExtraccion))
                .addContainerGap())
        );

        lbRegistarExtracciones.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbRegistarExtracciones.setText("Registrar Extracciones:");

        jcbPaciente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Paciente" }));

        lbPaciente.setText("Paciente: *");

        lbFechaIngreso.setText("Fecha de Extracción: *");

        lbMotivo.setText("Motivo de la Extracción: *");

        jcbMotivo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar" }));

        btnGuardarTurno.setText("Guardar");
        btnGuardarTurno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarTurnoActionPerformed(evt);
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

        jLabel1.setText("Tipo de Análisis: *");

        txtTipoAnalisis.setEditable(false);

        jcbTurnoExtracion.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione un turno" }));
        jcbTurnoExtracion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbTurnoExtracionActionPerformed(evt);
            }
        });

        lbTurnoExtraccion.setText("Turno: *");

        jcbVeterinario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione veterinario" }));

        lbVeterinarioAsignado.setText("Veterinario: *");

        jcbClienteDueño.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar cliente" }));

        lbPaciente1.setText("Cliente: *");

        javax.swing.GroupLayout jpRegistrarExtraccionesLayout = new javax.swing.GroupLayout(jpRegistrarExtracciones);
        jpRegistrarExtracciones.setLayout(jpRegistrarExtraccionesLayout);
        jpRegistrarExtraccionesLayout.setHorizontalGroup(
            jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(lbRegistarExtracciones)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarExtraccionesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnGuardarTurno)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator4)
                    .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jcbClienteDueño, 0, 204, Short.MAX_VALUE)
                                .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                                    .addComponent(lbPaciente1)
                                    .addGap(0, 0, Short.MAX_VALUE))
                                .addComponent(jdcFechaDeExtraccion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(lbFechaIngreso))
                        .addGap(12, 12, 12)
                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbPaciente)
                                    .addComponent(lbVeterinarioAsignado))
                                .addGap(144, 144, 144)
                                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbTurnoExtraccion)
                                    .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lbMotivo))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel1)
                                            .addComponent(txtTipoAnalisis, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jcbVeterinario, 0, 200, Short.MAX_VALUE)
                                    .addComponent(jcbPaciente, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jcbTurnoExtracion, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jpRegistrarExtraccionesLayout.setVerticalGroup(
            jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbRegistarExtracciones)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                        .addComponent(lbPaciente1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbClienteDueño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbPaciente)
                            .addComponent(lbMotivo)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTipoAnalisis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jpRegistrarExtraccionesLayout.createSequentialGroup()
                                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(lbVeterinarioAsignado)
                                    .addComponent(lbTurnoExtraccion))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jcbVeterinario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jcbTurnoExtracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarExtraccionesLayout.createSequentialGroup()
                                .addComponent(lbFechaIngreso)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jdcFechaDeExtraccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(18, 18, 18)
                .addGroup(jpRegistrarExtraccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardarTurno)
                    .addComponent(btnCancelar)
                    .addComponent(btnLimpiar))
                .addContainerGap())
        );

        lbRegistarExtracciones1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbRegistarExtracciones1.setText("Recibir Muestra / Generar Informe:");

        btnGuardarInformeFinal.setText("Guardar Informe");
        btnGuardarInformeFinal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarInformeFinalActionPerformed(evt);
            }
        });

        btnCancelarInformeFinal.setText("Cancelar");
        btnCancelarInformeFinal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarInformeFinalActionPerformed(evt);
            }
        });

        taDiagnostico.setColumns(20);
        taDiagnostico.setRows(5);
        jScrollPane1.setViewportView(taDiagnostico);

        javax.swing.GroupLayout jpaRecibirMuestraLayout = new javax.swing.GroupLayout(jpaRecibirMuestra);
        jpaRecibirMuestra.setLayout(jpaRecibirMuestraLayout);
        jpaRecibirMuestraLayout.setHorizontalGroup(
            jpaRecibirMuestraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpaRecibirMuestraLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpaRecibirMuestraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpaRecibirMuestraLayout.createSequentialGroup()
                        .addComponent(lbRegistarExtracciones1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpaRecibirMuestraLayout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnGuardarInformeFinal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelarInformeFinal, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jpaRecibirMuestraLayout.setVerticalGroup(
            jpaRecibirMuestraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpaRecibirMuestraLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpaRecibirMuestraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancelarInformeFinal)
                    .addComponent(btnGuardarInformeFinal))
                .addContainerGap())
            .addGroup(jpaRecibirMuestraLayout.createSequentialGroup()
                .addComponent(lbRegistarExtracciones1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE))
        );

        lbListaDePacientes.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDePacientes.setText("Lista de Procedimientos");

        spPedidosLaboratorio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tablePedidosLaboratorio.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Nº Extración", "Paciente", "Cliente", "Fecha/Hora Extracción", "Motivo de Extracción", "Tipo de Análisis", "Diagnóstico", "Fecha Envio a Lab.", "Fecha Recepción de Lab.", "Estado"
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
        tablePedidosLaboratorio.setMinimumSize(new java.awt.Dimension(848, 220));
        tablePedidosLaboratorio.setPreferredSize(new java.awt.Dimension(848, 220));
        tablePedidosLaboratorio.getTableHeader().setReorderingAllowed(false);
        spPedidosLaboratorio.setViewportView(tablePedidosLaboratorio);

        javax.swing.GroupLayout jpPedidosLaboratorioLayout = new javax.swing.GroupLayout(jpPedidosLaboratorio);
        jpPedidosLaboratorio.setLayout(jpPedidosLaboratorioLayout);
        jpPedidosLaboratorioLayout.setHorizontalGroup(
            jpPedidosLaboratorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spPedidosLaboratorio)
            .addGroup(jpPedidosLaboratorioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpPedidosLaboratorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpPedidosLaboratorioLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lbListaDePacientes)
                        .addContainerGap(838, Short.MAX_VALUE))
                    .addGroup(jpPedidosLaboratorioLayout.createSequentialGroup()
                        .addGroup(jpPedidosLaboratorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator6, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator3))
                        .addGap(6, 6, 6))))
        );
        jpPedidosLaboratorioLayout.setVerticalGroup(
            jpPedidosLaboratorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPedidosLaboratorioLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(lbListaDePacientes)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spPedidosLaboratorio, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpPedidosLaboratorio, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpRegistrarExtracciones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpaRecibirMuestra, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addComponent(jpRegistrarExtracciones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpaRecibirMuestra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpPedidosLaboratorio, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgendarExtraccionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgendarExtraccionActionPerformed
        laboratorio = new Laboratorio();
        // Al crear una nueva extracción, dejar solamente visible el panel de registro.
        mostrarSoloRegistrarExtracciones();
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
        mascotaSeleccionada = null;
        fechaExtraccionOriginal = null;
        limpiarCamposFormulario();
    }//GEN-LAST:event_btnAgendarExtraccionActionPerformed

    private void btnCancelarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarTurnoActionPerformed
        if (cancelarExtraccionSeleccionada()) {
            limpiarCamposFormulario();
            // Mantener vista de pedidos y refrescar (por defecto, "Todos" no muestra cancelados)
            mostrarSoloPedidosLaboratorio();
            ocultarBotonesHeaderPorDefecto();
            resetearBotonesAccion();
            cargarLaboratorioEnTabla();
        }
    }//GEN-LAST:event_btnCancelarTurnoActionPerformed

    private void btnGuardarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarTurnoActionPerformed
        if (mascotaSeleccionada != null) {
            Usuario vet = getVeterinarioSeleccionado();
            if (vet == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un veterinario.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (jdcFechaDeExtraccion.getDate() == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una fecha de extracción.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            AgendaSlot slotSel = getSlotSeleccionadoCombo();
            if (slotSel == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un horario disponible para la fecha/veterinario.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Diseño: la posibilidad de reservar/reprogramar se deriva del ESTADO.
            // Evitamos flags y estados inconsistentes.
            if (!puedeReservarOReprogramarTurno(laboratorio)) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se puede reprogramar el turno porque el pedido ya no está en estado 'Pendiente'.\n"
                        + "Estado actual: " + (laboratorio != null && laboratorio.getEstado() != null ? laboratorio.getEstado() : "(sin estado)"),
                        "Acción no permitida",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (requiereConfirmacionReservaTurno(laboratorio)) {
                LocalDate fechaExtraccionConfirm = jdcFechaDeExtraccion.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");
                String nombreVetConfirm = vet.getPersona() != null
                        ? vet.getPersona().getNombre() + " " + vet.getPersona().getApellido()
                        : "(Sin datos)";
                LocalTime hi = slotSel.getHoraInicio();
                LocalTime hf = slotSel.getHoraFin();
                String mascotaResumen = resumenMascotaSeleccionada();
                String clienteResumen = resumenClienteSeleccionado();

                String resumen = "Fecha: " + (fechaExtraccionConfirm != null ? fechaExtraccionConfirm.format(fmtFecha) : "--/--/----")
                        + "\nHora: " + (hi != null ? hi.format(fmtHora) : "--:--") + (hf != null ? " - " + hf.format(fmtHora) : "")
                        + "\nVeterinario: " + nombreVetConfirm
                        + "\nMascota: " + mascotaResumen
                        + "\nCliente: " + clienteResumen;

                String[] opciones = {"Sí", "No"};
                int opcion = JOptionPane.showOptionDialog(
                        this,
                        "¿Confirmar la reserva del turno para la extracción?\n\n" + resumen,
                        "Confirmación de Laboratorio",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        opciones,
                        opciones[0]
                );
                if (opcion != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            laboratorio.setUsuarioGestion(Application.getNombreApellidoUsuarioLogeado());
            laboratorio.setMascota(mascotaSeleccionada);
            laboratorio.setCliente(mascotaSeleccionada.getCliente());
            laboratorio.setMotivoExtraccion((String) jcbMotivo.getSelectedItem());
            laboratorio.setTipoAnalisis(txtTipoAnalisis.getText());
            laboratorio.setDiagnostico(taDiagnostico.getText());

            try {
                LocalDate fechaExtraccion = jdcFechaDeExtraccion.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                validar.validarFechaNoAnteriorALaActual(fechaExtraccion);
                if (!esDiaHabilitadoLaboratorio(fechaExtraccion)) {
                    JOptionPane.showMessageDialog(this, "El día seleccionado no está habilitado para Laboratorio.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean esAlta = (laboratorio.getIdLaboratorio() == null);

                boolean ok = false;
                if (esAlta) {
                    laboratorio.setEstado(Constantes.ESTADO_LABORATORIO_PENDIENTE);
                    ok = laboratorioService.reservarExtraccion(laboratorio, slotSel.getIdSlot());
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Laboratorio registrado con éxito.");
                    }
                } else {
                    ok = laboratorioService.actualizarExtraccionReasignandoSlot(laboratorio, slotSel.getIdSlot());
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Laboratorio actualizado con éxito.");
                    }
                }

                if (!ok) {
                    JOptionPane.showMessageDialog(this, "No se pudo reservar el horario seleccionado (puede haberse ocupado).\nActualizá la grilla y elegí otro.", "Horario sin disponibilidad", JOptionPane.WARNING_MESSAGE);
                    cargarTurnosComboParaFechaLaboratorio(fechaExtraccion);
                    return;
                }

                // Mensaje WhatsApp predefinido (al finalizar la carga / reserva del turno de extracción)
                try {
                    mostrarDialogoWhatsAppFinalizarCargaLaboratorio(esAlta, fechaExtraccion, slotSel, vet, mascotaSeleccionada);
                } catch (Exception ignore) {
                }

                cargarLaboratorioEnTabla();
                mostrarSoloPedidosLaboratorio();

                // Vuelta a lista: restaurar header base y ocultar acciones contextuales.
                btnAgendarExtraccion.setVisible(true);
                btnAgendarExtraccion.setEnabled(true);
                ocultarBotonesHeaderPorDefecto();
                resetearBotonesAccion();
                editandoPedido = false;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el turno de laboratorio: " + e.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una mascota.");
        }
    }//GEN-LAST:event_btnGuardarTurnoActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarCamposFormulario();
        // Desmarcar el checkbox en la fila seleccionada
        int filaSeleccionada = tablePedidosLaboratorio.getSelectedRow(); // Obtén la fila seleccionada
        if (filaSeleccionada >= 0) {
            // Asumiendo que la columna 0 tiene el checkbox
            tablePedidosLaboratorio.setValueAt(false, filaSeleccionada, 0); // Desmarcar el checkbox
        }
        mostrarSoloPedidosLaboratorio();
        btnAgendarExtraccion.setVisible(true);
        btnAgendarExtraccion.setEnabled(true);
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {
        // Este botón debe imprimir el INFORME en PDF (no la tabla).
        // La impresión directa del JTable abre un diálogo de impresora/PDF y genera una "captura" de la tabla,
        // lo cual NO es el comportamiento deseado en Laboratorio.
        btnImprimirInformeAction();
    }

    /**
     * Devuelve un texto amigable de la mascota seleccionada (igual a lo que se
     * ve en el combo).
     */
    private String resumenMascotaSeleccionada() {
        try {
            Object it = jcbPaciente.getSelectedItem();
            if (!(it instanceof Mascota)) {
                return "--";
            }
            Mascota m = (Mascota) it;
            String nombre = safe(m.getNombre());
            if (nombre.isBlank()) {
                nombre = (m.getIdMascota() != null) ? ("Mascota #" + m.getIdMascota()) : "Mascota";
            }
            String especie = safe(m.getEspecie());
            String raza = safe(m.getRaza());
            if (especie.isBlank() && raza.isBlank()) {
                return nombre;
            }
            if (!especie.isBlank() && !raza.isBlank()) {
                return nombre + " (" + especie + "/" + raza + ")";
            }
            return nombre + " (" + (!especie.isBlank() ? especie : raza) + ")";
        } catch (Exception e) {
            return "--";
        }
    }

    /**
     * Devuelve un texto amigable del cliente seleccionado (igual a lo que se ve
     * en el combo).
     */
    private String resumenClienteSeleccionado() {
        try {
            Object it = jcbClienteDueño.getSelectedItem();
            if (!(it instanceof Cliente)) {
                return "--";
            }
            Cliente c = (Cliente) it;
            if (c.getPersona() != null) {
                String n = c.getPersona().getNombre() != null ? c.getPersona().getNombre().trim() : "";
                String a = c.getPersona().getApellido() != null ? c.getPersona().getApellido().trim() : "";
                String full = (n + " " + a).trim();
                if (!full.isBlank()) {
                    return full;
                }
            }
            if (c.getRazonSocial() != null && !c.getRazonSocial().isBlank()) {
                return c.getRazonSocial().trim();
            }
            return (c.getIdCliente() != null) ? ("Cliente #" + c.getIdCliente()) : "Cliente";
        } catch (Exception e) {
            return "--";
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private void btnEnviarLaboratorioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarLaboratorioActionPerformed
        int viewRow = tablePedidosLaboratorio.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tablePedidosLaboratorio.convertRowIndexToModel(viewRow);
        Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getModel().getValueAt(modelRow, 1);

        Laboratorio lab = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);
        if (lab == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Constantes.ESTADO_LABORATORIO_PROCESADO.equals(lab.getEstado())) {
            JOptionPane.showMessageDialog(this, "La acción solo aplica a registros en estado 'Procesado'.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "¿Confirmar envío a laboratorio?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }

        lab.setFechaEnvio(LocalDate.now());
        lab.setEstado(Constantes.ESTADO_LABORATORIO_ENVIADO);
        if (!laboratorioControlador.actualizarLaboratorio(lab)) {
            JOptionPane.showMessageDialog(this, "No se pudo registrar el envío.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Envío registrado.", "Enviado", JOptionPane.INFORMATION_MESSAGE);

        cargarLaboratorioEnTabla();
        tablePedidosLaboratorio.clearSelection();
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
    }//GEN-LAST:event_btnEnviarLaboratorioActionPerformed

    private void btnRecibirLaboratorioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRecibirLaboratorioActionPerformed
        int viewRow = tablePedidosLaboratorio.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tablePedidosLaboratorio.convertRowIndexToModel(viewRow);
        Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getModel().getValueAt(modelRow, 1);

        Laboratorio lab = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);
        if (lab == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!puedeGestionarLaboratorio(lab)) {
            JOptionPane.showMessageDialog(this,
                    "Este laboratorio fue cargado por otro veterinario/usuario. No tiene permiso para recibir ni emitir el informe.",
                    "Acceso denegado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String estado = lab.getEstado();

        if (Constantes.ESTADO_LABORATORIO_ENVIADO.equals(estado)) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "¿Confirmar recepción desde laboratorio? Esto permitirá generar el informe.",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }

            lab.setEstado(Constantes.ESTADO_LABORATORIO_RECIBIDO);
            if (lab.getFechaRecepcion() == null) {
                lab.setFechaRecepcion(LocalDate.now());
            }
            if (!laboratorioControlador.actualizarLaboratorio(lab)) {
                JOptionPane.showMessageDialog(this, "No se pudo registrar la recepción.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            estado = lab.getEstado();
        }

        if (!Constantes.ESTADO_LABORATORIO_RECIBIDO.equals(estado)) {
            JOptionPane.showMessageDialog(this,
                    "La acción solo aplica a registros en estado 'Enviado' o 'Recibido'.",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (lab.getFechaRecepcion() == null) {
            lab.setFechaRecepcion(LocalDate.now());
            if (!laboratorioControlador.actualizarLaboratorio(lab)) {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la fecha de recepción.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        laboratorioInformeId = lab.getIdLaboratorio();
        taDiagnostico.setText(lab.getDiagnostico() != null ? lab.getDiagnostico() : "");
        taDiagnostico.requestFocusInWindow();
        mostrarSoloRecibirMuestra();
        cargarLaboratorioEnTabla();
    }//GEN-LAST:event_btnRecibirLaboratorioActionPerformed

    private void btnGuardarInformeFinalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarInformeFinalActionPerformed
        Integer idLaboratorio = laboratorioInformeId;
        if (idLaboratorio == null) {
            int viewRow = tablePedidosLaboratorio.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla (estado 'Recibido').", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow = tablePedidosLaboratorio.convertRowIndexToModel(viewRow);
            idLaboratorio = (Integer) tablePedidosLaboratorio.getModel().getValueAt(modelRow, 1);
        }

        Laboratorio lab = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);
        if (lab == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Constantes.ESTADO_LABORATORIO_RECIBIDO.equals(lab.getEstado())) {
            JOptionPane.showMessageDialog(this, "Para guardar el informe el registro debe estar en estado 'Recibido'.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String informe = taDiagnostico.getText() != null ? taDiagnostico.getText().trim() : "";
        if (informe.isEmpty()) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "El informe está vacío. ¿Desea completar igualmente el procedimiento?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            boolean ok = laboratorioService.completarExtraccion(
                    idLaboratorio,
                    informe,
                    Application.getNombreApellidoUsuarioLogeado()
            );
            if (!ok) {
                JOptionPane.showMessageDialog(this, "No se pudo completar el procedimiento.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al completar el procedimiento: " + ex.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Informe guardado. Se ha liberado el turno.", "Completado", JOptionPane.INFORMATION_MESSAGE);

        taDiagnostico.setText("");
        laboratorioInformeId = null;
        mostrarSoloPedidosLaboratorio();
        cargarLaboratorioEnTabla();
        tablePedidosLaboratorio.clearSelection();
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
    }//GEN-LAST:event_btnGuardarInformeFinalActionPerformed

    private void btnCancelarInformeFinalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarInformeFinalActionPerformed
        // Vuelve sin modificar estado ni texto persistido
        taDiagnostico.setText("");
        laboratorioInformeId = null;
        mostrarSoloPedidosLaboratorio();
        cargarLaboratorioEnTabla();
        tablePedidosLaboratorio.clearSelection();
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
    }//GEN-LAST:event_btnCancelarInformeFinalActionPerformed

    private void btnConfirmarExtraccionActionPerformed(java.awt.event.ActionEvent evt) {
        int opcion = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "¿Confirmar la extracción de la muestra para laboratorio?",
                "Confirmación",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
        );

        if (opcion != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
//GEN-FIRST:event_btnConfirmarExtraccionActionPerformed
        int fila = tablePedidosLaboratorio.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getValueAt(fila, 1);
        Laboratorio lab = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);
        if (lab == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        lab.setFechaExtraccion(LocalDate.now());
        lab.setEstado(Constantes.ESTADO_LABORATORIO_PROCESADO);

        if (!laboratorioControlador.actualizarLaboratorio(lab)) {
            JOptionPane.showMessageDialog(this, "No se pudo confirmar la extracción.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Extracción confirmada. Fecha: " + LocalDate.now() + " Turno:" + lab.getSlot().getHoraInicio().toString() + "hs.", "Procesado", JOptionPane.INFORMATION_MESSAGE);

        cargarLaboratorioEnTabla();
        tablePedidosLaboratorio.clearSelection();
        ocultarBotonesHeaderPorDefecto();
        resetearBotonesAccion();
    }//GEN-LAST:event_btnConfirmarExtraccionActionPerformed

    private void btnEditarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarTurnoActionPerformed
        editandoPedido = true;
        if (editarLaboratorioSeleccionado()) {
            jpRegistrarExtracciones.setVisible(true);
            jpPedidosLaboratorio.setVisible(false);
            // En modo edición, ocultamos la botonera para evitar acciones cruzadas.
            btnAgendarExtraccion.setVisible(false);
            ocultarBotonesHeaderPorDefecto();
            jcbClienteDueño.setEnabled(false);
            jcbPaciente.setEnabled(false);
        }
    }//GEN-LAST:event_btnEditarTurnoActionPerformed

    private void jcbTurnoExtracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbTurnoExtracionActionPerformed
        try {
            int idx = jcbTurnoExtracion.getSelectedIndex();
            if (idx <= 0) {
                lbTurnoExtraccion.setText("Laboratorio: *");
                return;
            }
            String label = (String) jcbTurnoExtracion.getSelectedItem();
            if (label == null || label.isBlank()) {
                lbTurnoExtraccion.setText("Laboratorio: *");
                return;
            }
            lbTurnoExtraccion.setText("Laboratorio: *  " + label);
        } catch (Exception ignore) {
        }
    }//GEN-LAST:event_jcbTurnoExtracionActionPerformed

    private void jcbFiltrarEstadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbFiltrarEstadoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jcbFiltrarEstadoActionPerformed

    // -------------------- Informe: ver / imprimir --------------------
    private Laboratorio obtenerLaboratorioSeleccionadoDesdeTabla() {
        int fila = tablePedidosLaboratorio.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            int modelRow = tablePedidosLaboratorio.convertRowIndexToModel(fila);
            Integer idLaboratorio = (Integer) tablePedidosLaboratorio.getModel().getValueAt(modelRow, 1);
            Laboratorio lab = laboratorioControlador.obtenerLaboratorioPorId(idLaboratorio);
            if (lab == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return lab;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el registro seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void btnVerInformeAction() {
        Laboratorio lab = obtenerLaboratorioSeleccionadoDesdeTabla();
        if (lab == null) {
            return;
        }
        InformeLaboratorioDialog.mostrar(SwingUtilities.getWindowAncestor(this), lab);
    }

    private void btnImprimirInformeAction() {
        Laboratorio lab = obtenerLaboratorioSeleccionadoDesdeTabla();
        if (lab == null) {
            return;
        }

        // 1. Armamos los parámetros para el informe de laboratorio
        ReporteRequest req = new ReporteRequest()
                .put("laboratorio", lab)
                .put("usuario", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 AL EJECUTOR: Sin "new", usa el Singleton y maneja el hilo en segundo plano
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.LABORATORIO_INFORME, req);
    }

    // -------------------- WhatsApp: mensaje predefinido al finalizar carga --------------------
    private void mostrarDialogoWhatsAppFinalizarCargaLaboratorio(boolean esAlta, LocalDate fechaExtraccion, AgendaSlot slotSel, Usuario vet, Mascota mascota) {
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaTxt = (fechaExtraccion != null) ? fechaExtraccion.format(fmtFecha) : "__/__/____";
        String hiTxt = "__:__";
        String hfTxt = "";
        try {
            LocalTime hi = (slotSel != null) ? slotSel.getHoraInicio() : null;
            LocalTime hf = (slotSel != null) ? slotSel.getHoraFin() : null;
            if (hi != null) {
                hiTxt = hi.format(fmtHora);
            }
            if (hf != null) {
                hfTxt = " - " + hf.format(fmtHora);
            }
        } catch (Exception ignore) {
        }

        String nombreVet = "";
        try {
            if (vet != null && vet.getPersona() != null) {
                String n = vet.getPersona().getNombre();
                String a = vet.getPersona().getApellido();
                nombreVet = ((n != null ? n : "") + " " + (a != null ? a : "")).trim();
            }
        } catch (Exception ignore) {
        }

        String mascotaTxt = "";
        String clienteTxt = "";
        try {
            if (mascota != null) {
                mascotaTxt = (mascota.getNombre() != null) ? mascota.getNombre().trim() : "";
                if (mascota.getCliente() != null && mascota.getCliente().getPersona() != null) {
                    String n = mascota.getCliente().getPersona().getNombre();
                    String a = mascota.getCliente().getPersona().getApellido();
                    clienteTxt = ((n != null ? n : "") + " " + (a != null ? a : "")).trim();
                }
            }
        } catch (Exception ignore) {
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Este es un mensaje generado por el Sistema: ");
        sb.append("Su turno de extracción (Laboratorio) en Veterinaria \"DoctorCat\" para el día ");
        sb.append(fechaTxt);
        sb.append(" a las ");
        sb.append(hiTxt).append(hfTxt);

        if (!nombreVet.isEmpty()) {
            sb.append(" (Veterinario: ").append(nombreVet).append(")");
        }
        if (!clienteTxt.isEmpty()) {
            sb.append(" (Cliente: ").append(clienteTxt).append(")");
        }
        if (!mascotaTxt.isEmpty()) {
            sb.append(" (Mascota: ").append(mascotaTxt).append(")");
        }

        sb.append(esAlta ? " fue REGISTRADO con éxito.\n" : " fue ACTUALIZADO con éxito.\n");
        sb.append("Por favor confirmá este turno respondiendo a este mensaje.\n");
        sb.append("¡Gracias! Los esperamos!");

        Window owner = SwingUtilities.getWindowAncestor(this);
        WhatsAppMensajeDialog.mostrar(owner, "Mensaje para WhatsApp", sb.toString());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgendarExtraccion;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCancelarInformeFinal;
    private javax.swing.JButton btnCancelarTurno;
    private javax.swing.JButton btnConfirmarExtraccion;
    private javax.swing.JButton btnEditarTurno;
    private javax.swing.JButton btnEnviarLaboratorio;
    private javax.swing.JButton btnGuardarInformeFinal;
    private javax.swing.JButton btnGuardarTurno;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnRecibirLaboratorio;
    private javax.swing.JButton btnVerInforme;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JComboBox jcbClienteDueño;
    private javax.swing.JComboBox<String> jcbFiltrarEstado;
    private javax.swing.JComboBox<String> jcbMotivo;
    private javax.swing.JComboBox<String> jcbPaciente;
    private javax.swing.JComboBox<String> jcbTurnoExtracion;
    private javax.swing.JComboBox<String> jcbVeterinario;
    private com.toedter.calendar.JDateChooser jdcFechaDeExtraccion;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpPedidosLaboratorio;
    private javax.swing.JPanel jpRegistrarExtracciones;
    private javax.swing.JPanel jpaRecibirMuestra;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbBusqueda;
    private javax.swing.JLabel lbFechaIngreso;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JLabel lbListaDePacientes;
    private javax.swing.JLabel lbMotivo;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbPaciente1;
    private javax.swing.JLabel lbRegistarExtracciones;
    private javax.swing.JLabel lbRegistarExtracciones1;
    private javax.swing.JLabel lbTurnoExtraccion;
    private javax.swing.JLabel lbVeterinarioAsignado;
    private javax.swing.JScrollPane spPedidosLaboratorio;
    private javax.swing.JTextArea taDiagnostico;
    private javax.swing.JTable tablePedidosLaboratorio;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextField txtTipoAnalisis;
    // End of variables declaration//GEN-END:variables
}
