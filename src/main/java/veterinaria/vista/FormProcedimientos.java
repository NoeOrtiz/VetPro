
package veterinaria.vista;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import veterinaria.controlador.MascotaControlador;
import veterinaria.controlador.ProcedimientoControlador;
import veterinaria.controlador.HospitalizacionControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Procedimiento;
import veterinaria.entidad.Rol;
import veterinaria.util.ManejoTablas;
import veterinaria.util.SesionUsuario;
import veterinaria.util.UtilidadesTabla;
import veterinaria.util.Validaciones;
import veterinaria.vista.application.Application;
import veterinaria.util.AppLog;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.HistoriaEventoAbrible;
import veterinaria.util.enums.EstadoProcedimiento;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.entidad.Usuario;
import javax.swing.DefaultComboBoxModel;

public class FormProcedimientos extends javax.swing.JPanel implements HistoriaEventoAbrible {

    private List<Map.Entry<Integer, String>> listaVisitaMascota = new ArrayList<>();
    private List<Map.Entry<Integer, Cliente>> listaClienteMascota = new ArrayList<>();
    private final Validaciones validar = new Validaciones();
    private final MascotaControlador mascotaControlador = new MascotaControlador();
    private Mascota mascotaSeleccionada;
    private Procedimiento procedimiento = new Procedimiento();
    private final ProcedimientoControlador procedimientoControlador = new ProcedimientoControlador();
    private final HospitalizacionControlador hospitalizacionControlador = new HospitalizacionControlador();
    private Hospitalizacion hospitalizacionActivaSeleccionada;
    private ManejoTablas operarTablas = new ManejoTablas();
    private SesionUsuario sesion = Application.getSesionUsuario();
    private Rol rol = sesion.getRol();

    // DAO para combos de usuarios (veterinarios)
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private static final String PROCEDIMIENTO_CASTRACION = "Castración";
    private static final String PROCEDIMIENTO_VACUNACION = "Vacunación";
    private static final String LABEL_CIRUJANO = "Cirujano:";
    private static final String LABEL_VETERINARIO = "Veterinario:";
    private static final String ITEM_SELECCIONAR_CIRUJANO = "Seleccionar Cirujano";
    private static final String ITEM_CIRUJANA_FIJA = "Dra. Carolina Boede";
    private static final String ITEM_SELECCIONAR_VETERINARIO = "Seleccionar Veterinario";

    /**
     * Indica si el formulario está en modo edición (vs. alta).
     * En edición se restringen campos editables.
     */
    private boolean modoEdicion = false;

    public FormProcedimientos() {
        initComponents();
        PermisoUI.aplicar(this);
        initListeners();
        jpRegistrarProcedimientos.setVisible(false);
        jpBotones.setVisible(false);
        cargarCombosBoxs();
        cargarComboUsuarioAtiendeSegunProcedimiento();
        cargarEstadosProcedimiento();
        configurarSpinnerHora(null);
        cargarProcedimientosEnTabla();
        ocultarCampoVeterinarioSiExiste();

        // Observaciones solo visible cuando el estado sea CANCELADO
        jcbObservaciones.setEnabled(false);
        jcbObservaciones.setVisible(false);
        lbObservacion.setVisible(false);

        // Ajusta el ancho de las columnas basado en el texto de los encabezados
        UtilidadesTabla.ajustarAnchoColumnas(tableProcedimientos);

        /*List<Integer> indicesColumnasConTooltip = Arrays.asList(3, 4, 5, 6, 7, 8, 9); // Agrega más índices si es necesario
        operarTablas.mouseTooltipText(tableProcedimientos, indicesColumnasConTooltip);*/
    }

    /**
     * Abre un procedimiento puntual desde Historia Clínica (modo lectura).
     */
    @Override
    public void abrirDetallePorId(Integer refId) {
        if (refId == null) return;

        // Asegurar listado visible
        jpListaProcedimientos.setVisible(true);
        jpRegistrarProcedimientos.setVisible(false);

        Integer viewRow = buscarFilaPorId(refId);
        if (viewRow == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el procedimiento #" + refId + " en la lista.",
                    "No encontrado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int modelRow = tableProcedimientos.convertRowIndexToModel(viewRow);
            tableProcedimientos.getModel().setValueAt(true, modelRow, 0);
            tableProcedimientos.setRowSelectionInterval(viewRow, viewRow);
            tableProcedimientos.scrollRectToVisible(tableProcedimientos.getCellRect(viewRow, 0, true));
        } catch (Exception ignore) {
        }

        if (editarProcedimientoSeleccionado()) {
            jpRegistrarProcedimientos.setVisible(true);
            jpBotones.setVisible(true);
            jpListaProcedimientos.setVisible(false);

            // Bloquear acciones
            btnNuevo.setEnabled(false);
            btnGuardar.setEnabled(false);
            btnEditar.setEnabled(false);
            bloquearLecturaFormulario();
        }
    }

    private Integer buscarFilaPorId(Integer idProcedimiento) {
        try {
            for (int viewRow = 0; viewRow < tableProcedimientos.getRowCount(); viewRow++) {
                int modelRow = tableProcedimientos.convertRowIndexToModel(viewRow);
                Object val = tableProcedimientos.getModel().getValueAt(modelRow, 1);
                if (val != null && idProcedimiento.toString().equals(val.toString())) {
                    return viewRow;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private void bloquearLecturaFormulario() {
        try {
            txtDueño.setEnabled(false);
            jcbPaciente.setEnabled(false);
            jdcFechaDeIngreso.setEnabled(false);
            spinnerHora.setEnabled(false);
            jcbUsuarioAtiende.setEnabled(false);
            jcbProcedimiento.setEnabled(false);
            jcbObservaciones.setEditable(false);
            jcbObservaciones.setEnabled(false);
        } catch (Exception ignore) {
        }
    }

    private void cargarEstadosProcedimiento() {
        try {
            jcbEstado.removeAllItems();
            for (EstadoProcedimiento e : EstadoProcedimiento.values()) {
                jcbEstado.addItem(e.toString());
            }
            jcbEstado.setSelectedItem(EstadoProcedimiento.SELECCIONAR_ESTADO.toString());
        } catch (Exception ignore) {
        }
    }

    private EstadoProcedimiento getEstadoSeleccionado() {
        Object sel = jcbEstado.getSelectedItem();
        if (sel == null) {
            return null;
        }
        return EstadoProcedimiento.fromEtiqueta(sel.toString());
    }

    private void setEstadoSeleccionadoFromString(String estadoBD) {
        try {
            EstadoProcedimiento e = EstadoProcedimiento.fromEtiqueta(estadoBD);
            if (e == null) {
                jcbEstado.setSelectedItem(EstadoProcedimiento.SELECCIONAR_ESTADO.toString());
                return;
            }
            jcbEstado.setSelectedItem(e.toString());
        } catch (Exception ignore) {
        }
    }

    /**
     * Aplica la regla de edición: al EDITAR solo se permite editar
     * Veterinario, Anestesiólogo y Estado.
     */
    private void aplicarModoFormulario() {
        try {
            if (!modoEdicion) {
                // Alta: todo habilitado
                txtDueño.setEnabled(false); // dueño siempre readonly
                jcbPaciente.setEnabled(true);
                jdcFechaDeIngreso.setEnabled(true);
                spinnerHora.setEnabled(true);
                jcbProcedimiento.setEnabled(true);
                jcbUsuarioAtiende.setEnabled(true);
                jcbEstado.setEnabled(true);
                actualizarVisibilidadCamposPorProcedimiento((String) jcbProcedimiento.getSelectedItem());
                return;
            }

            // Edición: restringir
            txtDueño.setEnabled(false);
            jcbPaciente.setEnabled(false);
            jdcFechaDeIngreso.setEnabled(false);
            spinnerHora.setEnabled(false);
            jcbProcedimiento.setEnabled(false);

            jcbUsuarioAtiende.setEnabled(true);
            jcbEstado.setEnabled(true);
            actualizarVisibilidadCamposPorProcedimiento((String) jcbProcedimiento.getSelectedItem());
        } catch (Exception ignore) {
        }
    }

    private void cargarCombosBoxs() {
        // Limpia listas para evitar duplicados si se vuelve a invocar
        listaVisitaMascota.clear();
        listaClienteMascota.clear();

        List<Mascota> mascotas = mascotaControlador.buscarTodasLasMascotas();

        // Combo principal (formulario)
        jcbPaciente.removeAllItems();
        jcbPaciente.addItem("Seleccionar Mascota");

        // Combo filtro (header)
        jcbPacienteFiltro.removeAllItems();
        jcbPacienteFiltro.addItem("Seleccionar Paciente");

        for (Mascota mascota : mascotas) {
            Integer idMascota = mascota.getIdMascota();
            String nombreMascota = mascota.getNombre();
            Cliente cargaCliente = mascota.getCliente();
            listaVisitaMascota.add(new AbstractMap.SimpleEntry<>(idMascota, nombreMascota));
            listaClienteMascota.add(new AbstractMap.SimpleEntry<>(idMascota, cargaCliente));
        }

        for (Map.Entry<Integer, String> entry : listaVisitaMascota) {
            jcbPaciente.addItem(entry.getValue());
            jcbPacienteFiltro.addItem(entry.getValue());
        }
    }

    /**
     * Carga dinámicamente el combo/lbl de usuario que atiende según el procedimiento.
     *
     * - Castración: muestra Cirujano y solo permite seleccionar a Dra. Carolina Boede.
     * - Vacunación: muestra Veterinario y carga los veterinarios del sistema.
     * - Sin selección: deja el combo vacío con etiqueta genérica.
     */
    private void cargarComboUsuarioAtiendeSegunProcedimiento() {
        actualizarUsuarioAtiendePorProcedimiento((String) jcbProcedimiento.getSelectedItem(), null);
        actualizarVisibilidadCamposPorProcedimiento((String) jcbProcedimiento.getSelectedItem());
    }

    private boolean esProcedimientoCastracion(String procedimientoSeleccionado) {
        return procedimientoSeleccionado != null
                && PROCEDIMIENTO_CASTRACION.equalsIgnoreCase(procedimientoSeleccionado.trim());
    }

    private void actualizarVisibilidadCamposPorProcedimiento(String procedimientoSeleccionado) {
        boolean mostrarCampos = esProcedimientoCastracion(procedimientoSeleccionado);

        lbMotivo.setVisible(mostrarCampos);
        jcbMotivo.setVisible(mostrarCampos);
        lbAnestesiologo.setVisible(mostrarCampos);
        jcbAnestesiologo.setVisible(mostrarCampos);

        jcbMotivo.setEnabled(mostrarCampos && !modoEdicion);
        jcbAnestesiologo.setEnabled(mostrarCampos);

        if (!mostrarCampos) {
            if (jcbMotivo.getItemCount() > 0) {
                jcbMotivo.setSelectedIndex(0);
            }
            if (jcbAnestesiologo.getItemCount() > 0) {
                jcbAnestesiologo.setSelectedIndex(0);
            }
        }

        revalidate();
        repaint();
    }

    private void actualizarUsuarioAtiendePorProcedimiento(String procedimientoSeleccionado, String valorSeleccionado) {
        try {
            String procedimientoNormalizado = procedimientoSeleccionado != null ? procedimientoSeleccionado.trim() : "";
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();

            if (PROCEDIMIENTO_CASTRACION.equalsIgnoreCase(procedimientoNormalizado)) {
                lbUsuarioAtiende.setText(LABEL_CIRUJANO);
                model.addElement(ITEM_SELECCIONAR_CIRUJANO);
                model.addElement(ITEM_CIRUJANA_FIJA);
            } else if (PROCEDIMIENTO_VACUNACION.equalsIgnoreCase(procedimientoNormalizado)) {
                lbUsuarioAtiende.setText(LABEL_VETERINARIO);
                model.addElement(ITEM_SELECCIONAR_VETERINARIO);

                List<Usuario> vets = usuarioDAO.obtenerUsuariosVeterinarios();
                if (vets != null) {
                    for (Usuario u : vets) {
                        String nombre = nombreVisibleUsuario(u);
                        if (nombre != null && !nombre.trim().isEmpty() && !existeEnModelo(model, nombre)) {
                            model.addElement(nombre);
                        }
                    }
                }
            } else {
                lbUsuarioAtiende.setText("Usuario atiende:");
                model.addElement("Seleccionar Usuario");
            }

            jcbUsuarioAtiende.setModel(model);

            if (valorSeleccionado != null && !valorSeleccionado.trim().isEmpty() && existeEnModelo(model, valorSeleccionado)) {
                jcbUsuarioAtiende.setSelectedItem(valorSeleccionado);
            } else if (model.getSize() > 0) {
                jcbUsuarioAtiende.setSelectedIndex(0);
            }

            // Compatibilidad defensiva con versiones intermedias del formulario
            ocultarCampoVeterinarioSiExiste();
        } catch (Exception ignore) {
            // si falla, dejamos el modelo actual del .form
        }
    }

    private boolean existeEnModelo(DefaultComboBoxModel<String> model, String valor) {
        if (model == null || valor == null) {
            return false;
        }
        String buscado = valor.trim();
        for (int i = 0; i < model.getSize(); i++) {
            Object item = model.getElementAt(i);
            if (item != null && buscado.equalsIgnoreCase(item.toString().trim())) {
                return true;
            }
        }
        return false;
    }

    private String nombreVisibleUsuario(Usuario u) {
        if (u == null) {
            return null;
        }
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
            nombre = (u.getNombreUsuario() != null) ? u.getNombreUsuario() : (u.getIdUsuario() != null ? ("Veterinario #" + u.getIdUsuario()) : "Veterinario");
        }
        return nombre;
    }

    private void ocultarCampoVeterinarioSiExiste() {
        try {
            if (lbUsuarioAtiende != null) {
                lbUsuarioAtiende.setVisible(false);
                lbUsuarioAtiende.setEnabled(false);
            }
            if (lbUsuarioAtiende != null) {
                lbUsuarioAtiende.setVisible(false);
                lbUsuarioAtiende.setEnabled(false);
            }
        } catch (Exception ignore) {
        }
    }

    private void enPacienteSeleccionado() {
        String pacienteSelec = (String) jcbPaciente.getSelectedItem();

        if (pacienteSelec == null || "Seleccionar Mascota".equals(pacienteSelec)) {
            mascotaSeleccionada = null;
            hospitalizacionActivaSeleccionada = null;
            txtDueño.setText("");
            lbRegistarIngresos.setText("Registrar Procedimientos:");
            jcbPaciente.setToolTipText(null);
            return;
        }

        for (Map.Entry<Integer, String> entry : listaVisitaMascota) {
            if (entry.getValue().equals(pacienteSelec)) {
                mascotaSeleccionada = mascotaControlador.buscarMascotaPorId(entry.getKey());
                String cliente = mascotaSeleccionada.getCliente().getPersona().getNombre() + " "
                        + mascotaSeleccionada.getCliente().getPersona().getApellido();
                txtDueño.setText(cliente);

                // Detectar hospitalización activa (si existe) para la mascota seleccionada
                hospitalizacionActivaSeleccionada = hospitalizacionControlador.buscarHospitalizacionActivaPorMascota(mascotaSeleccionada.getIdMascota());

                // Indicador simple (clínica chica): actualizar textos/tooltip sin tocar layout
                if (hospitalizacionActivaSeleccionada != null) {
                    lbRegistarIngresos.setText("Registrar Procedimientos: (Hospitalizado)");
                    jcbPaciente.setToolTipText("Hospitalizado: se vinculará automáticamente a la internación activa.");
                } else {
                    lbRegistarIngresos.setText("Registrar Procedimientos: (Ambulatorio)");
                    jcbPaciente.setToolTipText("Ambulatorio: se guardará sin hospitalización.");
                }
                break;
            }
        }
    }

    private void limpiarCamposFormulario() {
        jcbPaciente.setSelectedIndex(0);
        txtDueño.setText("");
        hospitalizacionActivaSeleccionada = null;
        jcbProcedimiento.setSelectedIndex(0);
        jcbMotivo.setSelectedIndex(0);
        actualizarUsuarioAtiendePorProcedimiento((String) jcbProcedimiento.getSelectedItem(), null);
        jcbAnestesiologo.setSelectedIndex(0);
        jcbEstado.setSelectedIndex(0);
        jcbObservaciones.setSelectedIndex(0);
        configurarSpinnerHora(null);
        syncObservacionesPorEstado();

    }

    private void initListeners() {       
        if (listenersInicializados) {
            return;
        }
        listenersInicializados = true;

        // Filtro de búsqueda por columnas (Paciente, Cliente, Motivo, Cirujano, etc.)
        List<Integer> columnasObjetivo = Arrays.asList(1, 2, 3, 6, 7);
        operarTablas.aplicarFiltroYResaltado(tableProcedimientos, txtBusqueda, columnasObjetivo);

        // Listener para habilitar/deshabilitar jcbObservaciones según el estado
        jcbEstado.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                syncObservacionesPorEstado();
            }
        });

        // Selección de mascota para registrar procedimiento
        jcbPaciente.addActionListener(e -> enPacienteSeleccionado());

        // Ajuste dinámico del profesional que atiende y de los campos visibles según el procedimiento
        jcbProcedimiento.addActionListener(e -> {
            String procedimientoSeleccionado = (String) jcbProcedimiento.getSelectedItem();
            actualizarUsuarioAtiendePorProcedimiento(procedimientoSeleccionado, null);
            actualizarVisibilidadCamposPorProcedimiento(procedimientoSeleccionado);
        });

        // Filtro rápido de tabla por paciente (combo superior)
        jcbPacienteFiltro.addActionListener(e -> enFiltroPacienteSeleccionado());

        // Mantener un único checkbox seleccionado en la tabla
        actualizarTabla();

        // Click en fila => marca checkbox automáticamente
        instalarSeleccionPorClickEnFila();
    }


/**
 * Sincroniza la habilitación de "Observaciones" según el estado actual.
 * - Solo se habilita/visualiza si el estado es "Cancelado".
 * - Si no corresponde, limpia selección y evita valores residuales.
 */
private void syncObservacionesPorEstado() {
    try {
        EstadoProcedimiento estado = getEstadoSeleccionado();
        boolean esCancelado = (estado == EstadoProcedimiento.CANCELADO);

        // Requerimiento: mostrar SOLO si el estado es Cancelado
        jcbObservaciones.setVisible(esCancelado);
        lbObservacion.setVisible(esCancelado);

        // Habilitar SOLO si corresponde
        jcbObservaciones.setEnabled(esCancelado);

        if (!esCancelado) {
            if (jcbObservaciones.getItemCount() > 0) {
                jcbObservaciones.setSelectedIndex(0);
            }
        }
    } catch (Exception ignore) {
    }
}

    private void filtrarTabla(String query) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableProcedimientos.getModel());
        tableProcedimientos.setRowSorter(sorter);

        if (query.trim().length() == 0) {
            sorter.setRowFilter(null);
            tableProcedimientos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());  // Restablecer renderizador
        } else {

            RowFilter<Object, Object> filtro = RowFilter.regexFilter("(?i)" + query, 2, 3, 7, 8);
            sorter.setRowFilter(filtro);

            tableProcedimientos.setDefaultRenderer(Object.class, new ResaltarCoincidenciasRenderer(query, new int[]{2, 3, 7, 8}));

            if (sorter.getViewRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay resultados para la búsqueda", "Búsqueda", JOptionPane.INFORMATION_MESSAGE);
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
    private boolean tableCheckboxListenerAdded = false;
    private boolean listenersInicializados = false;
    private boolean tableRowClickListenerAdded = false;


    private void actualizarTabla() {
        if (tableCheckboxListenerAdded) {
            return;
        }
        tableCheckboxListenerAdded = true;

        tableProcedimientos.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0 && !isUpdating) {
                isUpdating = true;
                int rowCount = tableProcedimientos.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    if (i != e.getFirstRow() && Boolean.TRUE.equals(tableProcedimientos.getValueAt(i, 0))) {
                        tableProcedimientos.setValueAt(false, i, 0);
                    }
                }
                isUpdating = false;
            }
        });
    }

    /**
     * Devuelve la fila (VIEW) marcada por checkbox (columna 0). Si no hay, devuelve -1.
     */
    private int obtenerFilaMarcadaView() {
        for (int viewRow = 0; viewRow < tableProcedimientos.getRowCount(); viewRow++) {
            Object v = tableProcedimientos.getValueAt(viewRow, 0);
            if (Boolean.TRUE.equals(v)) {
                return viewRow;
            }
        }
        return -1;
    }

    /**
     * Marca el checkbox (col 0) de la fila indicada (VIEW) y desmarca el resto.
     */
    private void marcarFilaUnica(int viewRow) {
        if (viewRow < 0 || viewRow >= tableProcedimientos.getRowCount()) {
            return;
        }
        if (isUpdating) {
            return;
        }
        isUpdating = true;
        try {
            for (int i = 0; i < tableProcedimientos.getRowCount(); i++) {
                tableProcedimientos.setValueAt(i == viewRow, i, 0);
            }
            tableProcedimientos.setRowSelectionInterval(viewRow, viewRow);
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Al hacer click en cualquier parte de una fila, marca automáticamente el checkbox (col 0).
     */
    private void instalarSeleccionPorClickEnFila() {
        if (tableRowClickListenerAdded) {
            return;
        }
        tableRowClickListenerAdded = true;

        tableProcedimientos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = tableProcedimientos.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }
                marcarFilaUnica(viewRow);
            }
        });
    }

    private void cargarProcedimientosEnTabla() {
        DefaultTableModel model = (DefaultTableModel) tableProcedimientos.getModel();
        model.setRowCount(0);
        List<Procedimiento> procedimientos = procedimientoControlador.obtenerTodosLosProcedimientos();

        for (Procedimiento procedimiento : procedimientos) {
            String fechaIngreso = (procedimiento.getFechaIngreso() != null) ? procedimiento.getFechaIngreso().toString() : "Fecha de ingreso no disponible";
            String hora = (procedimiento.getHora() != null) ? procedimiento.getHora().toString() : "Hora no disponible";
            String fechaHora = fechaIngreso + " a las " + hora;
            //String fechaAlta = (hospitalizacion.getFechaAlta() != null) ? hospitalizacion.getFechaAlta().toString() : "Aún hospitalizado";
            //String fechaRango = fechaIngreso + " - " + fechaAlta;
            String nombreMascota = (procedimiento.getMascota() != null) ? procedimiento.getMascota().getNombre() : "Sin mascota";

            String cliente = "Sin cliente";
            if (procedimiento.getCliente() != null && procedimiento.getCliente().getPersona() != null) {
                cliente = procedimiento.getCliente().getPersona().getNombre() + " " + procedimiento.getCliente().getPersona().getApellido();
            }

            Object[] fila = {
                false,
                procedimiento.getIdProcedimiento(),
                nombreMascota,
                cliente,
                fechaHora,
                //hospitalizacion.getFechaIngreso(),
                procedimiento.getProcedimiento(),
                procedimiento.getMotivo(),
                procedimiento.getUsuarioAtiende(),
                procedimiento.getAnestesiologo(),
                procedimiento.getEstado(),
                procedimiento.getObservaciones()
            //fechaRango,
            };
            model.addRow(fila);
        }
        actualizarTabla();
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

    private boolean editarProcedimientoSeleccionado() {
        boolean bRetorno = false;
        int filaSeleccionada = obtenerFilaMarcadaView();
        if (filaSeleccionada == -1) {
            filaSeleccionada = tableProcedimientos.getSelectedRow();
        }

        if (filaSeleccionada >= 0) {
            Integer idProcedimiento = (Integer) tableProcedimientos.getValueAt(filaSeleccionada, 1);
            procedimiento = procedimientoControlador.buscarProcedimientoPorId(idProcedimiento);

            if (procedimiento != null) {
                // Asegura coherencia visual: marcar la fila editada
                marcarFilaUnica(filaSeleccionada);

                mascotaSeleccionada = procedimiento.getMascota();
                jcbPaciente.setSelectedItem(mascotaSeleccionada.getNombre());
                String nombreApellido = mascotaSeleccionada.getCliente().getPersona().getNombre() + " " + mascotaSeleccionada.getCliente().getPersona().getApellido();
                txtDueño.setText(nombreApellido);

                // Cargar fecha desde BD (si existe)
                if (procedimiento.getFechaIngreso() != null) {
                    Date fechaIngreso = Date.from(procedimiento.getFechaIngreso().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    jdcFechaDeIngreso.setDate(fechaIngreso);
                } else {
                    jdcFechaDeIngreso.setDate(null);
                }

                //Date fechaAlta = Date.from(hospitalizacion.getFechaAlta().atStartOfDay(ZoneId.systemDefault()).toInstant());
                //jdcFechaDeIngreso.setDate(fechaAlta);
                // Hora de visita
                configurarSpinnerHora(procedimiento.getHora());
                jcbProcedimiento.setSelectedItem(procedimiento.getProcedimiento());
                actualizarUsuarioAtiendePorProcedimiento(procedimiento.getProcedimiento(), procedimiento.getUsuarioAtiende());
                jcbMotivo.setSelectedItem(procedimiento.getMotivo());
                jcbAnestesiologo.setSelectedItem(procedimiento.getAnestesiologo());
                setEstadoSeleccionadoFromString(procedimiento.getEstado());
                jcbObservaciones.setSelectedItem(procedimiento.getObservaciones());

                // Asegurar coherencia de UI (habilitar/limpiar observaciones según estado)
                syncObservacionesPorEstado();

                // Aplicar restricciones según modo
                aplicarModoFormulario();

                // Recalcula hospitalización activa para mantener el comportamiento (hospitalizado/ambulatorio)
                if (mascotaSeleccionada != null) {
                    hospitalizacionActivaSeleccionada = hospitalizacionControlador.buscarHospitalizacionActivaPorMascota(mascotaSeleccionada.getIdMascota());
                    if (hospitalizacionActivaSeleccionada != null) {
                        lbRegistarIngresos.setText("Registrar Procedimientos: (Hospitalizado)");
                        jcbPaciente.setToolTipText("Hospitalizado: se vinculará automáticamente a la internación activa.");
                    } else {
                        lbRegistarIngresos.setText("Registrar Procedimientos: (Ambulatorio)");
                        jcbPaciente.setToolTipText("Ambulatorio: se guardará sin hospitalización.");
                    }
                }

                bRetorno = true;
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la Procedimiento seleccionada.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "¡Debe seleccionar una Procedimiento a editar!", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return bRetorno;
    }
    private void enFiltroPacienteSeleccionado() {
        try {
            if (jcbPacienteFiltro == null) {
                return;
            }

            Object sel = jcbPacienteFiltro.getSelectedItem();
            if (sel == null) {
                txtBusqueda.setText("");
                return;
            }

            String valor = sel.toString().trim();

            // Opción "Seleccionar..."
            if (valor.isEmpty() || valor.toLowerCase().contains("seleccionar")) {
                txtBusqueda.setText("");
                return;
            }

            // Reusa el mecanismo de filtro que ya tengas ligado a txtBusqueda
            txtBusqueda.setText(valor);

            // Si tu filtro NO se dispara por el txtBusqueda automáticamente,
            // descomentá esto y reemplazá por tu método real:
            // filtrarTablaPorTexto(valor);
        } catch (Exception ex) {
            AppLog.warning(FormProcedimientos.class, "Error en enFiltroPacienteSeleccionado(): " + ex.getMessage(), ex);
        }
    }

    private boolean eliminarProcedimientoSeleccionado() {
        int filaSeleccionada = obtenerFilaMarcadaView();
        if (filaSeleccionada == -1) {
            filaSeleccionada = tableProcedimientos.getSelectedRow();
        }

        if (filaSeleccionada != -1) {
            Integer idProcedimiento = (Integer) tableProcedimientos.getValueAt(filaSeleccionada, 1);
            procedimiento = procedimientoControlador.buscarProcedimientoPorId(idProcedimiento);

            if (procedimiento != null) {
                // Confirmar eliminación
                int respuesta = JOptionPane.showConfirmDialog(
                        this,
                        "¿Estás seguro de que deseas eliminar este registro?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (respuesta == JOptionPane.YES_OPTION) {
                    // Intentar eliminar el procedimiento
                    if (procedimientoControlador.eliminarProcedimiento(procedimiento)) {
                        JOptionPane.showMessageDialog(this, "Procedimiento eliminado con éxito.", "Información", JOptionPane.INFORMATION_MESSAGE);
                        return true;
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo eliminar el Procedimiento. Solo se pueden eliminar Procedimiento en estado 'Dado de Alta'.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    // Deseleccionar la fila si se elige NO
                    tableProcedimientos.setValueAt(false, filaSeleccionada, 0);
                    tableProcedimientos.clearSelection();
                }
            } else {
                JOptionPane.showMessageDialog(this, "El Procedimiento seleccionado no se encontró.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un Procedimiento para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbInternaciones = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbBusqueda = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        jcbPacienteFiltro = new javax.swing.JComboBox<>();
        lbPacienteFiltro = new javax.swing.JLabel();
        jpRegistrarProcedimientos = new javax.swing.JPanel();
        lbRegistarIngresos = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jcbPaciente = new javax.swing.JComboBox<>();
        lbPaciente = new javax.swing.JLabel();
        lbFechaIngreso = new javax.swing.JLabel();
        jdcFechaDeIngreso = new com.toedter.calendar.JDateChooser();
        lbProcedimiento = new javax.swing.JLabel();
        lbMotivo = new javax.swing.JLabel();
        lbUsuarioAtiende = new javax.swing.JLabel();
        lbAnestesiologo = new javax.swing.JLabel();
        lbDueño = new javax.swing.JLabel();
        txtDueño = new javax.swing.JTextField();
        lbHora = new javax.swing.JLabel();
        spinnerHora = new javax.swing.JSpinner();
        jcbMotivo = new javax.swing.JComboBox<>();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jcbEstado = new javax.swing.JComboBox<>();
        lbObservacion = new javax.swing.JLabel();
        jcbObservaciones = new javax.swing.JComboBox<>();
        jcbAnestesiologo = new javax.swing.JComboBox<>();
        jcbUsuarioAtiende = new javax.swing.JComboBox<>();
        jcbProcedimiento = new javax.swing.JComboBox<>();
        jpListaProcedimientos = new javax.swing.JPanel();
        lbListaDePacientes = new javax.swing.JLabel();
        scroll = new javax.swing.JScrollPane();
        tableProcedimientos = new veterinaria.vista.table.AutoTable();
        jSeparator6 = new javax.swing.JSeparator();
        jSeparator7 = new javax.swing.JSeparator();
        jpBotones = new javax.swing.JPanel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();

        lbInternaciones.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbInternaciones.setText("Procedimientos (Ambulatorios y Hospitalizados)");

        lbBusqueda.setText("BUSCAR PACIENTE");

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

        jcbPacienteFiltro.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Paciente" }));

        lbPacienteFiltro.setText("Paciente:");

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(lbInternaciones)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addComponent(lbBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lbBusqueda))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lbPacienteFiltro)
                            .addComponent(jcbPacienteFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbInternaciones)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addComponent(lbBusqueda)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbBuscar)))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addComponent(lbPacienteFiltro)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbPacienteFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnNuevo)
                            .addComponent(btnEditar))))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        lbRegistarIngresos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbRegistarIngresos.setText("Registrar Procedimientos:");

        lbPaciente.setText("Paciente:");

        lbFechaIngreso.setText("Fecha:");

        lbProcedimiento.setText("Procedimiento:");

        lbMotivo.setText("Motivo:");

        lbUsuarioAtiende.setText("Atiende:");

        lbAnestesiologo.setText("Anestesiólogo:");

        lbDueño.setText("Dueño:");

        txtDueño.setEditable(false);

        lbHora.setText("Hora: *");

        spinnerHora.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.HOUR));

        jcbMotivo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Motivo", "Control de población", "Prevención ", "Comportamiento", "Recomendación veterinaria", "Razones de salud" }));

        jLabel3.setText("Estado:");

        jcbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Estado", "En Preparación", "En Procedimiento Quirúrgico", "En Recuperación Postquirúrgica", "Dado de Alta", "Procedimiento Cancelado" }));

        lbObservacion.setText("Observación:");

        jcbObservaciones.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Observación", "Condiciones de Salud Inesperadas", "Falta de Autorización", "Problemas con la Anestesia", "Problemas Loggísticos", "Cancelación por parte del Propietario", "No Cumplimiento de las Instrucciones Prequirúrgicas" }));

        jcbAnestesiologo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Anestesista", "Dr. Alejandro Marín", "Dra. Valentina Reyes", "Dr. Sebastián Torres", "Dra. Gabriela López", "Dr. Nicolás Paredes" }));

        jcbUsuarioAtiende.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Cirujano", "Dra. Carolina Boede" }));

        jcbProcedimiento.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Procedimiento", "Castración", "Vacunación" }));

        javax.swing.GroupLayout jpRegistrarProcedimientosLayout = new javax.swing.GroupLayout(jpRegistrarProcedimientos);
        jpRegistrarProcedimientos.setLayout(jpRegistrarProcedimientosLayout);
        jpRegistrarProcedimientosLayout.setHorizontalGroup(
            jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarProcedimientosLayout.createSequentialGroup()
                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jpRegistrarProcedimientosLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(198, 198, 198)
                                .addComponent(lbObservacion))
                            .addComponent(lbRegistarIngresos)
                            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbObservaciones, javax.swing.GroupLayout.PREFERRED_SIZE, 488, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jcbMotivo, 0, 230, Short.MAX_VALUE)
                                    .addComponent(lbPaciente, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jcbPaciente, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lbMotivo, javax.swing.GroupLayout.Alignment.LEADING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                        .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtDueño, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lbDueño)
                                            .addComponent(lbUsuarioAtiende))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jdcFechaDeIngreso, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(lbFechaIngreso))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(spinnerHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(lbHora))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(lbProcedimiento)
                                                    .addComponent(jcbProcedimiento, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addComponent(lbAnestesiologo)))
                                    .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                        .addComponent(jcbUsuarioAtiende, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jcbAnestesiologo, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jpRegistrarProcedimientosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator2))
                    .addComponent(jSeparator4, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        jpRegistrarProcedimientosLayout.setVerticalGroup(
            jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addComponent(lbRegistarIngresos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jpRegistrarProcedimientosLayout.createSequentialGroup()
                        .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbPaciente)
                            .addComponent(lbDueño))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtDueño)))
                    .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(lbProcedimiento)
                                    .addComponent(lbHora))
                                .addGap(28, 28, 28))
                            .addGroup(jpRegistrarProcedimientosLayout.createSequentialGroup()
                                .addComponent(lbFechaIngreso)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(spinnerHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jcbProcedimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jdcFechaDeIngreso, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbAnestesiologo)
                        .addComponent(lbUsuarioAtiende))
                    .addComponent(lbMotivo, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcbMotivo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcbUsuarioAtiende, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcbAnestesiologo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbObservacion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcbObservaciones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35))
        );

        lbListaDePacientes.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDePacientes.setText("Lista de Procedimientos");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableProcedimientos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "N° de Procedimiento", "Paciente", "Cliente", "Fecha", "Procedimiento", "Motivo", "Cirujano", "Anestesiólogo", "Estado"
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
        tableProcedimientos.setMinimumSize(new java.awt.Dimension(848, 220));
        tableProcedimientos.setPreferredSize(new java.awt.Dimension(848, 220));
        tableProcedimientos.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableProcedimientos);

        javax.swing.GroupLayout jpListaProcedimientosLayout = new javax.swing.GroupLayout(jpListaProcedimientos);
        jpListaProcedimientos.setLayout(jpListaProcedimientosLayout);
        jpListaProcedimientosLayout.setHorizontalGroup(
            jpListaProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpListaProcedimientosLayout.createSequentialGroup()
                .addGroup(jpListaProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(scroll)
                    .addGroup(jpListaProcedimientosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpListaProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator7)
                            .addGroup(jpListaProcedimientosLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(lbListaDePacientes)
                                .addGap(0, 855, Short.MAX_VALUE))
                            .addComponent(jSeparator6))))
                .addContainerGap())
        );
        jpListaProcedimientosLayout.setVerticalGroup(
            jpListaProcedimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaProcedimientosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbListaDePacientes)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
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

        javax.swing.GroupLayout jpBotonesLayout = new javax.swing.GroupLayout(jpBotones);
        jpBotones.setLayout(jpBotonesLayout);
        jpBotonesLayout.setHorizontalGroup(
            jpBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpBotonesLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jpBotonesLayout.setVerticalGroup(
            jpBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpBotonesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpBotonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLimpiar)
                    .addComponent(btnCancelar)
                    .addComponent(btnGuardar))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpListaProcedimientos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpRegistrarProcedimientos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpBotones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpRegistrarProcedimientos, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpBotones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaProcedimientos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        modoEdicion = false;
        limpiarCamposFormulario();
        aplicarModoFormulario();
        jpHeader.setVisible(false);
        jpRegistrarProcedimientos.setVisible(true);
        jpBotones.setVisible(true);
        jpListaProcedimientos.setVisible(false);

        // Defaults para evitar NPE y agilizar carga (clínica chica)
        jdcFechaDeIngreso.setDate(new Date());
        spinnerHora.setValue(new Date());

        jpListaProcedimientos.setEnabled(false);
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        String perm = "FormProcedimientos.EDITAR";
        if (sesion != null && sesion.puede(perm)) {
            // 1) Debe haber una fila seleccionada (checkbox o selección normal)
            int filaSeleccionada = obtenerFilaMarcadaView();
            if (filaSeleccionada == -1) {
                filaSeleccionada = tableProcedimientos.getSelectedRow();
            }

            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(
                        this,
                        "Por favor seleccione un procedimiento a EDITAR.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // 1.1) No permitir editar si el estado está finalizado
            try {
                Object estadoTabla = tableProcedimientos.getValueAt(filaSeleccionada, 9);
                EstadoProcedimiento estado = EstadoProcedimiento.fromEtiqueta(estadoTabla != null ? estadoTabla.toString() : null);
                if (estado == EstadoProcedimiento.DADO_DE_ALTA || estado == EstadoProcedimiento.CANCELADO) {
                    JOptionPane.showMessageDialog(
                            this,
                            "No se puede editar un procedimiento en estado '" + (estado != null ? estado.getEtiqueta() : "") + "'.",
                            "Edición no permitida",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            } catch (Exception ignore) {
            }

            modoEdicion = true;

            // 2) Cargar datos del procedimiento seleccionado en el formulario
            if (!editarProcedimientoSeleccionado()) {
                return; // El método ya informa si hubo algún problema
            }

            Application.actualizarEstadoUsuario("EditandoProcedimiento");
            jpRegistrarProcedimientos.setVisible(true);
            jpListaProcedimientos.setVisible(false);
            jpBotones.setVisible(true);
            jpHeader.setVisible(false);

        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para editar procedimientos.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    
    private boolean validarFormulario() {
        if (mascotaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una mascota.");
            return false;
        }
        if (jdcFechaDeIngreso.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar la fecha.");
            return false;
        }
        if (jcbProcedimiento.getSelectedItem() == null || ((String) jcbProcedimiento.getSelectedItem()).startsWith("Seleccionar")) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un procedimiento.");
            return false;
        }
        if (esProcedimientoCastracion((String) jcbProcedimiento.getSelectedItem())
                && (jcbMotivo.getSelectedItem() == null || ((String) jcbMotivo.getSelectedItem()).startsWith("Seleccionar"))) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un motivo.");
            return false;
        }
        if (jcbUsuarioAtiende.getSelectedItem() == null || ((String) jcbUsuarioAtiende.getSelectedItem()).startsWith("Seleccionar")) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el usuario que atiende.");
            return false;
        }
        if (esProcedimientoCastracion((String) jcbProcedimiento.getSelectedItem())
                && (jcbAnestesiologo.getSelectedItem() == null || ((String) jcbAnestesiologo.getSelectedItem()).startsWith("Seleccionar"))) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un anestesiólogo.");
            return false;
        }
        EstadoProcedimiento estado = getEstadoSeleccionado();
        if (estado == null || estado == EstadoProcedimiento.SELECCIONAR_ESTADO) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un estado.");
            return false;
        }
        // Si está cancelado, observación es obligatoria (básico)
        if (estado == EstadoProcedimiento.CANCELADO) {
            if (!jcbObservaciones.isEnabled() || jcbObservaciones.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una observación para el procedimiento cancelado.");
                return false;
            }
        }
        return true;
    }

private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
    if (!validarFormulario()) {
        return;
    }
    if (mascotaSeleccionada != null) {
        String user = Application.getNombreApellidoUsuarioLogeado();
        procedimiento.setUsuarioGestion(user);
        procedimiento.setMascota(mascotaSeleccionada);
        procedimiento.setCliente(mascotaSeleccionada.getCliente());
        procedimiento.setFechaIngreso(jdcFechaDeIngreso.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        procedimiento.setHora(((Date) spinnerHora.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
        procedimiento.setProcedimiento((String) jcbProcedimiento.getSelectedItem());
        boolean esCastracion = esProcedimientoCastracion((String) jcbProcedimiento.getSelectedItem());
        procedimiento.setMotivo(esCastracion ? (String) jcbMotivo.getSelectedItem() : null);
        procedimiento.setUsuarioAtiende((String) jcbUsuarioAtiende.getSelectedItem());
        procedimiento.setAnestesiologo(esCastracion ? (String) jcbAnestesiologo.getSelectedItem() : null);
        EstadoProcedimiento estado = getEstadoSeleccionado();
        procedimiento.setEstado(estado != null ? estado.getEtiqueta() : null);
        procedimiento.setObservaciones((String) jcbObservaciones.getSelectedItem());

        if (jcbObservaciones.isEnabled()) {
            procedimiento.setObservaciones((String) jcbObservaciones.getSelectedItem());
        } else {
            procedimiento.setObservaciones(null); // No guarda observaciones si no está habilitado
        }

        procedimiento.setHospitalizacion(hospitalizacionActivaSeleccionada);

        StringBuilder resumen = new StringBuilder();
        resumen.append("Resumen del procedimiento\n\n");
        resumen.append("Mascota: ").append(mascotaSeleccionada != null ? mascotaSeleccionada.getNombre() : "(sin seleccionar)").append("\n");

        String clienteNombre = "(sin cliente)";
        if (mascotaSeleccionada != null && mascotaSeleccionada.getCliente() != null && mascotaSeleccionada.getCliente().getPersona() != null) {
            String n = mascotaSeleccionada.getCliente().getPersona().getNombre();
            String a = mascotaSeleccionada.getCliente().getPersona().getApellido();
            clienteNombre = (n != null ? n : "") + " " + (a != null ? a : "");
            clienteNombre = clienteNombre.trim().isEmpty() ? "(sin cliente)" : clienteNombre.trim();
        }
        resumen.append("Cliente: ").append(clienteNombre).append("\n");

        resumen.append("Fecha ingreso: ").append(procedimiento.getFechaIngreso() != null ? procedimiento.getFechaIngreso().toString() : "").append("\n");
        resumen.append("Hora: ").append(procedimiento.getHora() != null ? procedimiento.getHora().toString() : "").append("\n");
        resumen.append("Procedimiento: ").append(procedimiento.getProcedimiento() != null ? procedimiento.getProcedimiento() : "").append("\n");
        resumen.append("Motivo: ").append(procedimiento.getMotivo() != null ? procedimiento.getMotivo() : "").append("\n");
        resumen.append(lbUsuarioAtiende.getText()).append(" ").append(procedimiento.getUsuarioAtiende() != null ? procedimiento.getUsuarioAtiende() : "").append("\n");
        resumen.append("Anestesiólogo: ").append(procedimiento.getAnestesiologo() != null ? procedimiento.getAnestesiologo() : "").append("\n");
        resumen.append("Estado: ").append(procedimiento.getEstado() != null ? procedimiento.getEstado() : "").append("\n");
        resumen.append("Observaciones: ").append(procedimiento.getObservaciones() != null ? procedimiento.getObservaciones() : "(sin observaciones)").append("\n");
        resumen.append("Hospitalización: ").append(procedimiento.getHospitalizacion() != null ? ("ID " + procedimiento.getHospitalizacion().getIdHospitalizacion()) : "Ambulatorio").append("\n\n");
        resumen.append("¿Desea guardar este procedimiento?");

        int opcion = JOptionPane.showConfirmDialog(
                this,
                resumen.toString(),
                "Confirmación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (opcion != JOptionPane.YES_OPTION) {
            return; // Mantiene el formulario abierto
        }

        try {
            if (procedimiento.getIdProcedimiento() == null) {
                procedimientoControlador.crearProcedimiento(procedimiento);
                JOptionPane.showMessageDialog(this, "Procedimiento registrado con éxito.");
            } else {
                procedimientoControlador.actualizarProcedimiento(procedimiento);
                JOptionPane.showMessageDialog(this, "Procedimiento actualizada con éxito.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al registrar la Procedimiento: " + e.getMessage());
        }
        jpBotones.setVisible(false);
        cargarProcedimientosEnTabla();
        modoEdicion = false;
        limpiarCamposFormulario();
        aplicarModoFormulario();
        jpRegistrarProcedimientos.setVisible(false);
        jpHeader.setVisible(true);
    } else {
        JOptionPane.showMessageDialog(this, "Por favor, selecciona una mascota.");
    }
    jpListaProcedimientos.setVisible(true);
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        modoEdicion = false;
        limpiarCamposFormulario();
        aplicarModoFormulario();
        // Desmarcar el checkbox en la fila seleccionada
        int filaSeleccionada = obtenerFilaMarcadaView();
        if (filaSeleccionada == -1) {
            filaSeleccionada = tableProcedimientos.getSelectedRow();
        }
        if (filaSeleccionada >= 0) {
            // Asumiendo que la columna 0 tiene el checkbox
            tableProcedimientos.setValueAt(false, filaSeleccionada, 0); // Desmarcar el checkbox
            tableProcedimientos.clearSelection();
        }
        jpRegistrarProcedimientos.setVisible(false);
        jpListaProcedimientos.setVisible(true);
        btnNuevo.setEnabled(true);
        btnEditar.setEnabled(true);
        jpHeader.setVisible(true);
        jpBotones.setVisible(false);
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarCamposFormulario();
        aplicarModoFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JComboBox<String> jcbAnestesiologo;
    private javax.swing.JComboBox<String> jcbEstado;
    private javax.swing.JComboBox<String> jcbMotivo;
    private javax.swing.JComboBox<String> jcbObservaciones;
    private javax.swing.JComboBox<String> jcbPaciente;
    private javax.swing.JComboBox<String> jcbPacienteFiltro;
    private javax.swing.JComboBox<String> jcbProcedimiento;
    private javax.swing.JComboBox<String> jcbUsuarioAtiende;
    private com.toedter.calendar.JDateChooser jdcFechaDeIngreso;
    private javax.swing.JPanel jpBotones;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaProcedimientos;
    private javax.swing.JPanel jpRegistrarProcedimientos;
    private javax.swing.JLabel lbAnestesiologo;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbBusqueda;
    private javax.swing.JLabel lbDueño;
    private javax.swing.JLabel lbFechaIngreso;
    private javax.swing.JLabel lbHora;
    private javax.swing.JLabel lbInternaciones;
    private javax.swing.JLabel lbListaDePacientes;
    private javax.swing.JLabel lbMotivo;
    private javax.swing.JLabel lbObservacion;
    private javax.swing.JLabel lbPaciente;
    private javax.swing.JLabel lbPacienteFiltro;
    private javax.swing.JLabel lbProcedimiento;
    private javax.swing.JLabel lbRegistarIngresos;
    private javax.swing.JLabel lbUsuarioAtiende;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JSpinner spinnerHora;
    private javax.swing.JTable tableProcedimientos;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextField txtDueño;
    // End of variables declaration//GEN-END:variables
}
