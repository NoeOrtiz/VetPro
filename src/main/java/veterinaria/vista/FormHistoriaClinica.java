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
import veterinaria.reportes.pdf.ReporteEjecutor;

public class FormHistoriaClinica extends javax.swing.JPanel {

    // Mapeo simple para el combo (index -> idMascota)
    private final List<Map.Entry<Integer, String>> listaMascotas = new ArrayList<>();
    private Mascota mascotaSeleccionada;
    private veterinaria.util.PaginacionTablas paginacionHistoria;
    private final javax.swing.JTextField buscarRegistros = new javax.swing.JTextField(22);
    private final javax.swing.JButton btnCancelarHistoria = new javax.swing.JButton("Cancelar");
    private boolean marcandoFila;
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();
    private final MascotaControlador mascotaControlador = new MascotaControlador();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // 🚀 REEMPLAZÁ TU LÍNEA POR ESTA:
    private final ReporteService reporteService = ReporteService.getInstance();

    public FormHistoriaClinica() {
        initComponents();
        PermisoUI.aplicar(this);
        inicializarTabla();
        configurarCabeceraHistoria();
        configurarListaHistoria();
        configurarEstructuraHistoria();
        btnVerRegistros.setText("Ver detalle");
        btnVerRegistros.setToolTipText("Consultar el evento sin modificar el registro original.");
        btnCancelarHistoria.setText("Limpiar");
        cargarComboMascotas();
        initListeners();
        jcbPaciente.setSelectedIndex(0);
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

            // Al seleccionar un paciente, su historia se consulta automáticamente.
            cargarHistoria();

        });

        btnVerHistoria.addActionListener(e -> {

            cargarHistoria();

        });

        // Habilita el detalle de solo lectura al seleccionar una fila
        tableHistoriaClinica.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {

                actualizarEstadoBotones();

            }

        });
        tableHistoriaClinica.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int fila = tableHistoriaClinica.rowAtPoint(e.getPoint());
                // El checkbox tiene su propio listener: no seleccionar dos veces
                // ni recalcular el listado cuando se hace clic sobre la casilla.
                int columna = tableHistoriaClinica.columnAtPoint(e.getPoint());
                if (fila >= 0 && columna >= 0
                        && tableHistoriaClinica.convertColumnIndexToModel(columna) != 0) {
                    seleccionarFilaUnica(tableHistoriaClinica.convertRowIndexToModel(fila));
                }
            }
        });
        tableHistoriaClinica.getModel().addTableModelListener(e -> {
            if (!marcandoFila && e.getType() == javax.swing.event.TableModelEvent.UPDATE
                    && e.getColumn() == 0 && e.getFirstRow() >= 0
                    && Boolean.TRUE.equals(tableHistoriaClinica.getModel().getValueAt(e.getFirstRow(), 0))) {
                seleccionarFilaUnica(e.getFirstRow());
            }
        });
        btnCancelarHistoria.addActionListener(e -> limpiarPantallaHistoria());
    }

    private void seleccionarFilaUnica(int filaModelo) {
        if (marcandoFila) return;
        marcandoFila = true;
        try {
            javax.swing.table.TableModel modelo = tableHistoriaClinica.getModel();
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (!Boolean.valueOf(i == filaModelo).equals(modelo.getValueAt(i, 0)))
                    modelo.setValueAt(i == filaModelo, i, 0);
            }
            int vista = tableHistoriaClinica.convertRowIndexToView(filaModelo);
            if (vista >= 0) tableHistoriaClinica.setRowSelectionInterval(vista, vista);
        } finally {
            marcandoFila = false;
        }
        actualizarEstadoBotones();
    }

    private void limpiarPantallaHistoria() {
        if (paginacionHistoria != null) paginacionHistoria.limpiarBusqueda();
        buscarRegistros.setText("");
        jcbPaciente.setSelectedIndex(0);
        limpiarDetalleMascota();
        limpiarTabla();
        tableHistoriaClinica.clearSelection();
        actualizarEstadoBotones();
    }

    /** Panel de historial con búsqueda, paginación y navegación consistente con Consultas. */
    private javax.swing.JPanel campoFicha(javax.swing.JLabel etiqueta, java.awt.Component control) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.add(etiqueta, java.awt.BorderLayout.NORTH);
        panel.add(control, java.awt.BorderLayout.CENTER);
        panel.setMinimumSize(new java.awt.Dimension(70, 55));
        return panel;
    }

    /** Encabezado compacto, independiente del diseño generado por NetBeans. */
    private void configurarCabeceraHistoria() {
        jPanel1.removeAll();
        jPanel1.setLayout(new java.awt.BorderLayout(0, 13));
        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 16, 16));
        javax.swing.JPanel titulo = new javax.swing.JPanel(new java.awt.BorderLayout());
        titulo.setOpaque(false);
        titulo.add(lbConsumos, java.awt.BorderLayout.WEST);
        javax.swing.JPanel acciones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        btnImprimirHistoria.setText("Imprimir historia clínica");
        acciones.add(btnImprimirHistoria);
        titulo.add(acciones, java.awt.BorderLayout.EAST);
        jPanel1.add(titulo, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel campos = new javax.swing.JPanel();
        campos.setOpaque(false);
        campos.setLayout(new javax.swing.BoxLayout(campos, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JPanel principales = new javax.swing.JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        principales.setOpaque(false);
        principales.add(campoFicha(lbPaciente, jcbPaciente));
        principales.add(campoFicha(lbDueño, txtDueño));
        principales.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 58));
        campos.add(principales);
        campos.add(javax.swing.Box.createVerticalStrut(9));

        javax.swing.JPanel caracteristicas = new javax.swing.JPanel(new java.awt.GridLayout(1, 5, 12, 0));
        caracteristicas.setOpaque(false);
        caracteristicas.add(campoFicha(lbEspecie, txtEspecie));
        caracteristicas.add(campoFicha(lbRaza, txtRaza));
        caracteristicas.add(campoFicha(lbRaza1, txtSexo));
        caracteristicas.add(campoFicha(lbEdad, txtEdad));
        caracteristicas.add(campoFicha(lbPeso, txtPeso));
        caracteristicas.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 58));
        campos.add(caracteristicas);
        jPanel1.add(campos, java.awt.BorderLayout.CENTER);
        jPanel1.add(jSeparator1, java.awt.BorderLayout.SOUTH);
    }

    private void configurarEstructuraHistoria() {
        // El GroupLayout de la versión anterior producía un ancho mínimo excesivo
        // y una barra vertical que escondía la navegación y el título.
        setLayout(new java.awt.BorderLayout(0, 6));
        add(jPanel1, java.awt.BorderLayout.NORTH);
        add(jPanel2, java.awt.BorderLayout.CENTER);
        add(jPanel3, java.awt.BorderLayout.SOUTH);
        setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel1.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel2.setMinimumSize(new java.awt.Dimension(0, 0));
        scroll1.setMinimumSize(new java.awt.Dimension(0, 0));
        tableHistoriaClinica.setMinimumSize(new java.awt.Dimension(0, 0));
        tableHistoriaClinica.setPreferredScrollableViewportSize(new java.awt.Dimension(780, 300));
        revalidate();
        repaint();
    }

    private void configurarListaHistoria() {
        jPanel2.removeAll();
        jPanel2.setLayout(new java.awt.BorderLayout(0, 8));
        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 0, 16));
        javax.swing.JPanel cabecera = new javax.swing.JPanel(new java.awt.BorderLayout(12, 0));
        cabecera.setOpaque(false);
        cabecera.add(lbListaServicios, java.awt.BorderLayout.WEST);
        javax.swing.JPanel botones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnVerRegistros);
        botones.add(btnImprimirListaHistorias);
        cabecera.add(botones, java.awt.BorderLayout.EAST);
        jPanel2.add(cabecera, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel contenido = new javax.swing.JPanel(new java.awt.BorderLayout(0, 6));
        contenido.setOpaque(false);
        javax.swing.JPanel buscador = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 1));
        buscador.setOpaque(false);
        javax.swing.JLabel etiquetaBuscar = new javax.swing.JLabel("BUSCAR");
        buscador.add(etiquetaBuscar);
        javax.swing.JLabel lupa = new javax.swing.JLabel();
        lupa.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png")));
        lupa.setToolTipText("Buscar registros");
        buscador.add(lupa);
        buscarRegistros.setColumns(22);
        buscarRegistros.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Buscar registros...");
        buscador.add(buscarRegistros);
        contenido.add(buscador, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel paginada = new javax.swing.JPanel(new java.awt.BorderLayout());
        paginada.add(scroll1, java.awt.BorderLayout.CENTER);
        paginacionHistoria = new veterinaria.util.PaginacionTablas(
                tableHistoriaClinica, buscarRegistros, 15,
                new int[]{2, 3, 4, 5}, new int[]{2, 3, 4, 5});
        paginacionHistoria.activarBusquedaPorPrefijo();
        // No permitir que el resaltador de búsqueda reemplace el degradado de selección.
        paginacionHistoria.preservarRenderizadoresPersonalizados();
        // Misma X de limpieza integrada al buscador que en Consultas.
        buscarRegistros.setLayout(new java.awt.BorderLayout());
        buscarRegistros.add(paginacionHistoria.getBotonLimpiar(), java.awt.BorderLayout.EAST);
        paginada.add(paginacionHistoria.getControles(), java.awt.BorderLayout.SOUTH);
        contenido.add(paginada, java.awt.BorderLayout.CENTER);
        jPanel2.add(contenido, java.awt.BorderLayout.CENTER);
        javax.swing.SwingUtilities.invokeLater(() -> paginacionHistoria.activarTamanoAdaptable());

        jPanel3.removeAll();
        jPanel3.setLayout(new java.awt.BorderLayout());
        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 16, 8, 16));
        javax.swing.JPanel contador = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
        contador.setOpaque(false);
        contador.add(lblCantidadReg);
        contador.add(lblCantidad);
        jPanel3.add(contador, java.awt.BorderLayout.WEST);
        javax.swing.JPanel navegacion = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));
        navegacion.setOpaque(false);
        btnCancelarHistoria.setToolTipText("Limpiar paciente y registros; volver al inicio.");
        navegacion.add(btnCancelarHistoria);
        jPanel3.add(navegacion, java.awt.BorderLayout.EAST);
        jPanel2.revalidate();
    }

    private void inicializarTabla() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Seleccionar", "ID", "Fecha", "Tipo", "Profesional", "Resumen", "Observaciones"}
        ) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : Object.class;
            }
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 0;
            }
        };
        tableHistoriaClinica.setModel(model);
        tableHistoriaClinica.getTableHeader().setReorderingAllowed(false);
        tableHistoriaClinica.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableHistoriaClinica.setFillsViewportHeight(true);
        tableHistoriaClinica.setRowHeight(34);
        ((veterinaria.vista.table.AutoTable) tableHistoriaClinica)
                .setAjustarAnchoAutomaticamente(false);
        ((veterinaria.vista.table.AutoTable) tableHistoriaClinica)
                .setIgnorarActualizacionesCheckbox(true);
        // Proporciones del modelo antes de ocultar el ID. Las columnas cortas
        // conservan un ancho máximo; Resumen y Observaciones absorben el resto.
        int[] widths = {92, 45, 155, 145, 175, 360, 500};
        for (int i = 0; i < widths.length; i++) {
            tableHistoriaClinica.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        // Una celda larga se distribuye en varios renglones en lugar de cortarse.
        // Conservar el mismo degradado continuo que el módulo Consultas.
        tableHistoriaClinica.getColumnModel().getColumn(0).setCellRenderer(
                new veterinaria.vista.table.TableGradientCheckBoxRenderer());
        for (int columna : new int[]{2, 3, 4, 5, 6}) {
            tableHistoriaClinica.getColumnModel().getColumn(columna).setCellRenderer(
                    new veterinaria.vista.table.TableGradientCell() {
                        @Override public java.awt.Component getTableCellRendererComponent(
                                javax.swing.JTable tabla, Object valor, boolean seleccionado,
                                boolean foco, int fila, int columnaVista) {
                            super.getTableCellRendererComponent(
                                    tabla, valor, seleccionado, foco, fila, columnaVista);
                            String texto = valor == null ? "" : String.valueOf(valor);
                            int disponible = Math.max(72,
                                    tabla.getColumnModel().getColumn(columnaVista).getWidth() - 24);
                            String html = texto.replace("&", "&amp;").replace("<", "&lt;")
                                    .replace(">", "&gt;").replace("\n", "<br>");
                            // Resaltar solo el prefijo de los campos buscados.
                            String consulta = buscarRegistros.getText().trim();
                            int columnaModelo = tabla.convertColumnIndexToModel(columnaVista);
                            if (!consulta.isEmpty() && columnaModelo >= 2 && columnaModelo <= 5
                                    && veterinaria.util.ManejoTablas.coincideBusqueda(
                                            texto, consulta, true) && texto.length() >= consulta.length()) {
                                String prefijo = texto.substring(0, consulta.length());
                                String resto = texto.substring(consulta.length());
                                html = "<span style='color:#FFA500;font-weight:bold'>"
                                        + escaparHtml(prefijo) + "</span>"
                                        + escaparHtml(resto).replace("\n", "<br>");
                            }
                            setText("<html><div style='width:" + disponible
                                    + "px; margin:0px'>" + html + "</div></html>");
                            setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 9, 6, 9));
                            setToolTipText(texto.isBlank() ? null : "<html><div style='width:420px'>"
                                    + html + "</div></html>");
                            return this;
                        }
                    });
        }
        tableHistoriaClinica.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                javax.swing.SwingUtilities.invokeLater(() ->
                        ((veterinaria.vista.table.AutoTable) tableHistoriaClinica)
                                .reajustarColumnasYFilasSeguro());
            }
        });
        tableHistoriaClinica.setToolTipText("Seleccione un registro y presione Ver detalle para leerlo completo.");
        ocultarColumnaId();
        configurarAnchosHistoria();
        limpiarTabla();
    }

    private void configurarAnchosHistoria() {
        javax.swing.table.TableColumnModel columnas = tableHistoriaClinica.getColumnModel();
        // Orden visible: Seleccionar, Fecha, Tipo, Profesional, Resumen, Observaciones.
        int[] fijos = {100, 168, 155, 185};
        for (int i = 0; i < fijos.length; i++) {
            javax.swing.table.TableColumn columna = columnas.getColumn(i);
            columna.setMinWidth(i == 0 ? 92 : (i == 1 ? 150 : 118));
            columna.setMaxWidth(fijos[i]);
            columna.setPreferredWidth(fijos[i]);
        }
        columnas.getColumn(4).setMinWidth(180);
        columnas.getColumn(4).setPreferredWidth(330);
        columnas.getColumn(5).setMinWidth(240);
        columnas.getColumn(5).setPreferredWidth(490);
        tableHistoriaClinica.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private String importeHistoria(String tipo, String valor) {
        if (tipo == null || !tipo.toUpperCase(java.util.Locale.ROOT).contains("PELUQUER")
                || valor == null || !valor.trim().matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            return valor;
        }
        try {
            java.text.NumberFormat formato =
                    java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "AR"));
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);
            return "$ " + formato.format(new java.math.BigDecimal(valor.trim()));
        } catch (Exception ex) {
            return valor;
        }
    }

    private String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
            String label = veterinaria.util.ui.MascotaComboSupport.label(m);
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

        eventos = veterinaria.servicio.HistoriaClinicaPreferencias.filtrar(eventos);

        if (eventos == null || eventos.isEmpty()) {

            actualizarCantidadRegistros(0);

            JOptionPane.showMessageDialog(
                    this,
                    "No hay eventos clínicos visibles según la configuración actual.",
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
                false,
                ev.getIdEvento(),
                fechaTxt,
                nullToEmpty(ev.getTipo()),
                nullToEmpty(ev.getProfesional()),
                nullToEmpty(ev.getResumen()),
                importeHistoria(ev.getTipo(), nullToEmpty(ev.getObservaciones()))
            });

        }

        ocultarColumnaId();
        // Alturas definitivas antes de que el usuario seleccione una fila.
        ((veterinaria.vista.table.AutoTable) tableHistoriaClinica)
                .reajustarColumnasYFilasSeguro();
        actualizarEstadoBotones();

    }

    private void limpiarTabla() {

        DefaultTableModel model = (DefaultTableModel) tableHistoriaClinica.getModel();

        model.setRowCount(0);

        actualizarCantidadRegistros(0);
        actualizarEstadoBotones();

    }

    private void ocultarColumnaId() {

        // Ocultar el ID una sola vez: al recargar se quitaba por error la Fecha.
        javax.swing.table.TableColumn columnaId = null;
        for (java.util.Enumeration<javax.swing.table.TableColumn> cols =
                tableHistoriaClinica.getColumnModel().getColumns(); cols.hasMoreElements();) {
            javax.swing.table.TableColumn col = cols.nextElement();
            if (col.getModelIndex() == 1) {
                columnaId = col;
                break;
            }
        }
        if (columnaId != null) {
            tableHistoriaClinica.removeColumn(columnaId);
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
        Object idObj = tableHistoriaClinica.getModel().getValueAt(modelRow, 1);

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

        javax.swing.JPanel ficha = new javax.swing.JPanel();
        ficha.setLayout(new javax.swing.BoxLayout(ficha, javax.swing.BoxLayout.Y_AXIS));
        ficha.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));
        javax.swing.JLabel encabezado = new javax.swing.JLabel("DATOS DEL REGISTRO");
        encabezado.setForeground(new java.awt.Color(66, 190, 115));
        encabezado.setFont(encabezado.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        encabezado.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(encabezado);
        ficha.add(javax.swing.Box.createVerticalStrut(14));
        javax.swing.JPanel resumen = new javax.swing.JPanel(new java.awt.GridLayout(0, 2, 18, 14));
        resumen.setOpaque(false);
        String[][] campos = {
            {"Fecha", ev.getFecha() == null ? "-" : dtf.format(ev.getFecha())},
            {"Tipo", textoDetalle(ev.getTipo())},
            {"Profesional", textoDetalle(ev.getProfesional())},
            {"Paciente", mascotaSeleccionada == null ? "-" : textoDetalle(mascotaSeleccionada.getNombre())}
        };
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
        ficha.add(javax.swing.Box.createVerticalStrut(18));
        javax.swing.JLabel seccion = new javax.swing.JLabel("INFORMACIÓN CLÍNICA");
        seccion.setForeground(new java.awt.Color(66, 190, 115));
        seccion.setFont(seccion.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        seccion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        ficha.add(seccion);
        ficha.add(javax.swing.Box.createVerticalStrut(12));
        ficha.add(campoDetalle("Resumen", textoDetalle(ev.getResumen())));
        ficha.add(javax.swing.Box.createVerticalStrut(14));
        ficha.add(campoDetalle("Observaciones", textoDetalle(ev.getObservaciones())));
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(ficha);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new java.awt.Dimension(640, 460));
        JOptionPane.showMessageDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                scroll, "Información del registro · Solo lectura", JOptionPane.PLAIN_MESSAGE);

    }

    private String textoDetalle(String texto) {
        return texto == null || texto.isBlank() ? "No registrado" : texto.trim();
    }

    private javax.swing.JPanel campoDetalle(String etiqueta, String valor) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        javax.swing.JLabel label = new javax.swing.JLabel(etiqueta);
        label.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
        panel.add(label, java.awt.BorderLayout.NORTH);
        javax.swing.JTextArea texto = new javax.swing.JTextArea(valor, Math.max(2,
                Math.min(12, (valor.length() + 66) / 67)), 42);
        texto.setEditable(false);
        texto.setOpaque(false);
        texto.setFocusable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        panel.add(texto, java.awt.BorderLayout.CENTER);
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 300));
        return panel;
    }

    private void actualizarEstadoBotones() {

        boolean hayMascota = mascotaSeleccionada != null;

        boolean hayRegistros = tableHistoriaClinica.getRowCount() > 0;

        boolean haySeleccion = tableHistoriaClinica.getSelectedRow() >= 0;

        btnVerHistoria.setEnabled(hayMascota);

        // La historia completa se imprime por paciente, sin seleccionar una fila.
        btnImprimirHistoria.setEnabled(hayMascota);

        btnImprimirListaHistorias.setEnabled(hayRegistros);

        btnVerRegistros.setEnabled(haySeleccion);

        lblCantidad.setText(String.valueOf(tableHistoriaClinica.getModel().getRowCount()));

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

            ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.HISTORIA_CLINICA_LISTADO, req);
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

            List<HistoriaEvento> eventos = veterinaria.servicio.HistoriaClinicaPreferencias.filtrar(
                    historiaEventoControlador.listarPorMascota(mascotaSeleccionada.getIdMascota()));
            if (eventos == null || eventos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "La mascota no tiene eventos para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("mascota", mascotaSeleccionada)
                    .put("eventos", eventos);

            ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.HISTORIA_CLINICA, req);
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
