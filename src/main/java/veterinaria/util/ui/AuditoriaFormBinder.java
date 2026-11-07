package veterinaria.util.ui;

import com.toedter.calendar.JDateChooser;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import veterinaria.controlador.UsuarioControlador;
import veterinaria.entidad.Auditoria;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.util.UsuarioItem;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.servicio.AuditoriaService;
import veterinaria.vista.application.Application;
import veterinaria.vista.componentes.AuditoriaDetalleDialog;

public class AuditoriaFormBinder {

    private final AuditoriaService auditoriaService = new AuditoriaService();
    private final UsuarioControlador usuarioControlador = new UsuarioControlador();

    private final Component parent;
    private final JTable tableAuditoria;
    private final JComboBox<UsuarioItem> jcbUsuario;
    private final JComboBox<String> jcbAccion;
    private final JComboBox<String> jcbEntidad;
    private final JDateChooser jDateDesde;
    private final JDateChooser jDateHasta;
    private final JButton btnBuscar;
    private final JButton btnLimpiar;
    private final JButton btnVer;
    private final JButton btnImprimir;
    private final JButton btnImprimirLista;

    private final List<Auditoria> cache = new ArrayList<>();
    private final javax.swing.JTextField buscador;
    private veterinaria.util.PaginacionTablas paginacion;

    public AuditoriaFormBinder(Component parent,
            JTable tableAuditoria,
            JComboBox<UsuarioItem> jcbUsuario,
            JComboBox<String> jcbAccion,
            JComboBox<String> jcbEntidad,
            JDateChooser jDateDesde,
            JDateChooser jDateHasta,
            JButton btnBuscar,
            JButton btnLimpiar,
            JButton btnVer,
            JButton btnImprimir,
            JButton btnImprimirLista,
            javax.swing.JTextField buscador) {
        this.parent = parent;
        this.tableAuditoria = tableAuditoria;
        this.jcbUsuario = jcbUsuario;
        this.jcbAccion = jcbAccion;
        this.jcbEntidad = jcbEntidad;
        this.jDateDesde = jDateDesde;
        this.jDateHasta = jDateHasta;
        this.btnBuscar = btnBuscar;
        this.btnLimpiar = btnLimpiar;
        this.btnVer = btnVer;
        this.btnImprimir = btnImprimir;
        this.btnImprimirLista = btnImprimirLista;
        this.buscador = buscador;
    }

    public void init() {
        configurarTabla();
        paginacion = veterinaria.util.GestorTablas.paginar(tableAuditoria, buscador,
                new veterinaria.util.GestorTablas.Configuracion(
                        new int[]{1, 2, 3, 4, 5, 6, 7}, new int[]{1, 2, 3, 4, 5, 6, 7}, 10));
        tableAuditoria.getColumnModel().getColumn(0).setCellRenderer(
                new veterinaria.vista.table.TableGradientCheckBoxRenderer());
        veterinaria.util.GestorTablas.instalarSeleccion(tableAuditoria, this::actualizarBotones);
        cargarCombos();
        configurarEventos();

        Date hoy = new Date();
        jDateHasta.setDate(hoy);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(hoy);
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
        jDateDesde.setDate(cal.getTime());

        btnVer.setEnabled(false);
        btnImprimir.setEnabled(false);

        buscar();
    }

    public javax.swing.JPanel getControlesPaginacion() { return paginacion.getControles(); }

    private void configurarTabla() {
        tableAuditoria.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableAuditoria.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Seleccionar", "Fecha/Hora", "Usuario", "Acción", "Entidad", "Módulo", "Descripción", "Resultado"}
        ) {
            @Override
            public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : String.class; }
            @Override
            public boolean isCellEditable(int row, int col) { return col == 0; }
        });
        // Distribuir el ancho disponible, sin dejar una franja vacía a la derecha.
        tableAuditoria.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] anchos = {85, 185, 100, 95, 110, 95, 300, 85};
        int[] minimos = {80, 180, 75, 75, 85, 75, 160, 70};
        for (int i = 0; i < anchos.length; i++) {
            javax.swing.table.TableColumn columna = tableAuditoria.getColumnModel().getColumn(i);
            columna.setMinWidth(minimos[i]);
            columna.setPreferredWidth(anchos[i]);
        }
        tableAuditoria.setFillsViewportHeight(true);
        tableAuditoria.getTableHeader().setResizingAllowed(true);
        tableAuditoria.setRowHeight(Math.max(27, tableAuditoria.getRowHeight()));
        tableAuditoria.getColumnModel().getColumn(0).setMaxWidth(90);
    }

    private void cargarCombos() {
        jcbUsuario.removeAllItems();
        jcbUsuario.addItem(new UsuarioItem(null, "Todos"));

        try {
            List<Usuario> usuarios = usuarioControlador.obtenerTodosLosUsuarios();
            if (usuarios != null) {
                for (Usuario u : usuarios) {
                    String nombre = u.getNombreUsuario();
                    jcbUsuario.addItem(new UsuarioItem(u.getIdUsuario(), nombre));
                }
            }
        } catch (Exception e) {
        }

        jcbAccion.removeAllItems();
        jcbAccion.addItem("Todas");
        jcbAccion.addItem("LOGIN");
        jcbAccion.addItem("LOGIN_FAIL");
        jcbAccion.addItem("LOGOUT");
        jcbAccion.addItem("CREATE");
        jcbAccion.addItem("UPDATE");
        jcbAccion.addItem("DELETE");
        jcbAccion.addItem("PRINT");

        jcbEntidad.removeAllItems();
        jcbEntidad.addItem("Todas");
        jcbEntidad.addItem("Producto");
        jcbEntidad.addItem("Proveedor");
        jcbEntidad.addItem("Visita");
        jcbEntidad.addItem("Usuario");
        jcbEntidad.addItem("Caja");
    }

    private void configurarEventos() {
        btnBuscar.addActionListener(e -> buscar());

        if (btnLimpiar != null) {
            btnLimpiar.addActionListener(e -> limpiarFiltros());
        }

        btnVer.addActionListener(e -> verDetalle());

        btnImprimir.addActionListener(e -> imprimirSeleccionado());

        btnImprimirLista.addActionListener(e -> imprimirLista());


    }

    private void buscar() {
        Date desde = jDateDesde.getDate();
        Date hasta = jDateHasta.getDate();

        UsuarioItem uItem = (UsuarioItem) jcbUsuario.getSelectedItem();
        Integer usuarioId = (uItem != null ? uItem.getIdUsuario() : null);

        String accion = (String) jcbAccion.getSelectedItem();
        if ("Todas".equalsIgnoreCase(accion)) {
            accion = null;
        }

        String entidad = (String) jcbEntidad.getSelectedItem();
        if ("Todas".equalsIgnoreCase(entidad)) {
            entidad = null;
        }

        List<Auditoria> lista = auditoriaService.buscar(desde, hasta, usuarioId, accion, entidad);

        cache.clear();
        if (lista != null) {
            cache.addAll(lista);
        }

        DefaultTableModel model = (DefaultTableModel) tableAuditoria.getModel();
        model.setRowCount(0);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Auditoria a : cache) {
            String fecha = "";
            if (a.getFechaHora() != null) {

                fecha = a.getFechaHora().format(fmt);
            }
            String user = (a.getUsuario() != null ? a.getUsuario().getNombreUsuario() : "Usuario No Registrado");

            model.addRow(new Object[]{
                Boolean.FALSE,
                fecha,
                user,
                a.getAccion(),
                a.getEntidad(),
                a.getModulo(),
                a.getDescripcion(),
                a.getResultado()
            });
        }

        paginacion.actualizar();
        actualizarBotones();
    }

    private void limpiarFiltros() {
        Date hoy = new Date();
        jDateHasta.setDate(hoy);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(hoy);
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
        jDateDesde.setDate(cal.getTime());

        if (jcbUsuario.getItemCount() > 0) {
            jcbUsuario.setSelectedIndex(0);
        }
        if (jcbAccion.getItemCount() > 0) {
            jcbAccion.setSelectedIndex(0);
        }
        if (jcbEntidad.getItemCount() > 0) {
            jcbEntidad.setSelectedIndex(0);
        }

        tableAuditoria.clearSelection();
        btnVer.setEnabled(false);
        btnImprimir.setEnabled(false);

        buscar();
    }

    private void marcarUnico(int fila) {
        for (int i = 0; i < tableAuditoria.getModel().getRowCount(); i++) {
            if (i != fila && Boolean.TRUE.equals(tableAuditoria.getModel().getValueAt(i, 0)))
                tableAuditoria.getModel().setValueAt(false, i, 0);
        }
    }

    private void actualizarBotones() {
        boolean seleccionado = getSeleccionado() != null;
        btnVer.setEnabled(seleccionado);
        btnImprimir.setEnabled(seleccionado);
    }

    private Auditoria getSeleccionado() {
        for (int i = 0; i < tableAuditoria.getModel().getRowCount() && i < cache.size(); i++) {
            if (Boolean.TRUE.equals(tableAuditoria.getModel().getValueAt(i, 0))
                    && tableAuditoria.convertRowIndexToView(i) >= 0) return cache.get(i);
        }
        return null;
    }

    private void verDetalle() {
        Auditoria a = getSeleccionado();
        if (a == null) {
            return;
        }
        new AuditoriaDetalleDialog(getFrame(), a).setVisible(true);
    }

    private void imprimirSeleccionado() {
        Auditoria a = getSeleccionado();
        if (a == null) {
            JOptionPane.showMessageDialog(parent, "Seleccione un evento para imprimir.", "Auditoría", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 1. Armamos los parámetros para el reporte
        ReporteRequest req = new ReporteRequest()
                .put("auditoria", a)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 MANDAMOS AL EJECUTOR: Corre en segundo plano y muestra la barra animada
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(parent, ReporteTipo.AUDITORIA_EVENTO, req);

        // 3. Registramos la acción (esto corre rápido en el hilo principal sin congelar nada)
        try {
            new AuditoriaService().registrar("PRINT", "Auditoria", a.getIdAuditoria(), "FormAuditoria", "Impresión de evento de auditoría", AuditoriaService.RESULT_OK, null, null, null);
        } catch (Exception ex) {
            ex.printStackTrace(); // Si falla el log de auditoría, se imprime en consola para no molestar al usuario
        }
    }

    private void imprimirLista() {
        if (cache.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No hay eventos para imprimir.", "Auditoría", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String filtros = construirResumenFiltros();

        // 1. Armamos los parámetros tal cual los tenías
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tableAuditoria)
                .put("emisor", Application.getNombreApellidoUsuarioLogeado())
                .put("filtros", filtros);

        // 2. 🚀 MANDAMOS AL EJECUTOR: Sin "new", con Singleton de fondo y barra de progreso
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(parent, ReporteTipo.AUDITORIA_LISTADO, req);

        // 3. Registramos la acción en el log local
        try {
            new AuditoriaService().registrar("PRINT", "Auditoria", null, "FormAuditoria", "Impresión de listado de auditoría", AuditoriaService.RESULT_OK, null, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JFrame getFrame() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(parent);
        return (w instanceof JFrame) ? (JFrame) w : null;
    }

    private String construirResumenFiltros() {
        List<String> parts = new ArrayList<>();

        if (jDateDesde.getDate() != null) {
            parts.add("Desde=" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(jDateDesde.getDate()));
        }
        if (jDateHasta.getDate() != null) {
            parts.add("Hasta=" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(jDateHasta.getDate()));
        }

        UsuarioItem uItem = (UsuarioItem) jcbUsuario.getSelectedItem();
        if (uItem != null && uItem.getIdUsuario() != null) {
            parts.add("Usuario=" + uItem.getNombre());
        }

        String acc = (String) jcbAccion.getSelectedItem();
        if (acc != null && !"Todas".equalsIgnoreCase(acc)) {
            parts.add("Acción=" + acc);
        }

        String ent = (String) jcbEntidad.getSelectedItem();
        if (ent != null && !"Todas".equalsIgnoreCase(ent)) {
            parts.add("Entidad=" + ent);
        }

        return String.join(" | ", parts);
    }
}
