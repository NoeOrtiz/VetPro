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
    private final javax.swing.JButton seleccionarPaciente = new javax.swing.JButton("Seleccionar paciente...");
    private final javax.swing.JButton btnAnamnesis = new javax.swing.JButton("Anamnesis");
    private final javax.swing.JButton btnHistorialAnamnesis =
            new javax.swing.JButton("Versiones");
    private final veterinaria.persistencia.HistoriaAnamnesisDAO anamnesisDAO =
            new veterinaria.persistencia.HistoriaAnamnesisDAO();
    private final java.util.List<Mascota> pacientesBusqueda = new java.util.ArrayList<>();
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
        seleccionarPaciente.addActionListener(e -> abrirSelectorPacientes());
        btnAnamnesis.addActionListener(e -> abrirAnamnesis());
        btnHistorialAnamnesis.addActionListener(e -> verVersionesAnamnesis());
        initListeners();
        actualizarCantidadRegistros(0);
        actualizarEstadoBotones();

    }

    private void initListeners() {

        jcbPaciente.addActionListener(e -> {

            int idx = jcbPaciente.getSelectedIndex();

            if (idx <= 0) {
                seleccionarPaciente.setText("Seleccionar paciente...");
                limpiarDetalleMascota();
                limpiarTabla();
                return;
            }

            Integer idMascota = listaMascotas.get(idx - 1).getKey();

            mascotaSeleccionada = mascotaControlador.buscarMascotaPorId(idMascota);
            seleccionarPaciente.setText(listaMascotas.get(idx - 1).getValue());

            mostrarDetalleMascota();

            // Solo muestra los datos. La historia se consulta con el botón.
            limpiarTabla();

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
                if (fila >= 0) seleccionarFilaUnica(tableHistoriaClinica.convertRowIndexToModel(fila));
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
        acciones.add(btnAnamnesis);
        acciones.add(btnHistorialAnamnesis);
        acciones.add(btnVerHistoria);
        acciones.add(btnImprimirHistoria);
        titulo.add(acciones, java.awt.BorderLayout.EAST);
        jPanel1.add(titulo, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel campos = new javax.swing.JPanel();
        campos.setOpaque(false);
        campos.setLayout(new javax.swing.BoxLayout(campos, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JPanel principales = new javax.swing.JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        principales.setOpaque(false);
        principales.add(campoFicha(lbPaciente, seleccionarPaciente));
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
        buscador.add(new javax.swing.JLabel("BUSCAR POR INICIO"));
        buscarRegistros.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Buscar desde el inicio de fecha, tipo, profesional o resumen");
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
        int[] widths = {92, 50, 135, 125, 185, 250, 295};
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
        tableHistoriaClinica.getColumnModel().addColumnModelListener(
                new javax.swing.event.TableColumnModelListener() {
                    @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                        javax.swing.SwingUtilities.invokeLater(() ->
                                ((veterinaria.vista.table.AutoTable) tableHistoriaClinica)
                                        .reajustarColumnasYFilasSeguro());
                    }
                    @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
                    @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
                    @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
                    @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
                });
        tableHistoriaClinica.setToolTipText("Seleccione un registro y presione Ver detalle para leerlo completo.");
        ocultarColumnaId();
        limpiarTabla();
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

        pacientesBusqueda.clear();
        for (Mascota m : mascotas) {
            pacientesBusqueda.add(m);
            String label = veterinaria.util.ui.MascotaComboSupport.label(m);
            listaMascotas.add(new AbstractMap.SimpleEntry<>(m.getIdMascota(), label));
            jcbPaciente.addItem(label);
        }
    }

    /** Buscador de pacientes por inicio de nombre o de propietario.
     * Selección única, sin modificar ningún dato del paciente. */
    private void abrirSelectorPacientes() {
        javax.swing.JTextField buscar = new javax.swing.JTextField(30);
        buscar.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Buscar por inicio del nombre del paciente o propietario");
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel(
                new Object[]{"ID", "Paciente", "Propietario", "Especie"}, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        for (Mascota mascota : pacientesBusqueda) {
            if (mascota != null) modelo.addRow(new Object[]{
                mascota.getIdMascota(), nullToEmpty(mascota.getNombre()),
                nombrePropietario(mascota), nullToEmpty(mascota.getEspecie())
            });
        }
        javax.swing.JTable tabla = new javax.swing.JTable(modelo);
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(30);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(270);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(130);

        veterinaria.util.PaginacionTablas paginas =
                new veterinaria.util.PaginacionTablas(tabla, buscar, 12,
                        new int[]{1, 2}, new int[]{1, 2});
        paginas.activarBusquedaPorPrefijo();

        javax.swing.JPanel contenido = new javax.swing.JPanel(new java.awt.BorderLayout(0, 10));
        contenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contenido.add(buscar, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel tablaPaginada = new javax.swing.JPanel(new java.awt.BorderLayout(0, 4));
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tabla);
        scroll.setPreferredSize(new java.awt.Dimension(780, 330));
        tablaPaginada.add(scroll, java.awt.BorderLayout.CENTER);
        tablaPaginada.add(paginas.getControles(), java.awt.BorderLayout.SOUTH);
        contenido.add(tablaPaginada, java.awt.BorderLayout.CENTER);

        javax.swing.JOptionPane opciones = new javax.swing.JOptionPane(contenido,
                javax.swing.JOptionPane.PLAIN_MESSAGE, javax.swing.JOptionPane.OK_CANCEL_OPTION);
        javax.swing.JDialog dialogo = opciones.createDialog(this, "Seleccionar paciente");
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    opciones.setValue(javax.swing.JOptionPane.OK_OPTION);
                    dialogo.dispose();
                }
            }
        });
        dialogo.setVisible(true);
        if (!Integer.valueOf(javax.swing.JOptionPane.OK_OPTION).equals(opciones.getValue())) return;
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un paciente de la lista.");
            return;
        }
        Object valorId = modelo.getValueAt(tabla.convertRowIndexToModel(filaVista), 0);
        if (!(valorId instanceof Integer)) return;
        Integer id = (Integer) valorId;
        for (int i = 0; i < listaMascotas.size(); i++) {
            if (id.equals(listaMascotas.get(i).getKey())) {
                jcbPaciente.setSelectedIndex(i + 1);
                cargarHistoria();
                return;
            }
        }
    }

    private String nombrePropietario(Mascota m) {
        if (m == null || m.getCliente() == null) return "";
        veterinaria.entidad.Persona persona = m.getCliente().getPersona();
        if (persona == null) return nullToEmpty(m.getCliente().getRazonSocial());
        return (nullToEmpty(persona.getNombre()) + " "
                + nullToEmpty(persona.getApellido())).trim();
    }

    private String normalizarInicio(String valor) {
        return java.text.Normalizer.normalize(valor == null ? "" : valor.trim(),
                java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    /** Versiones previas disponibles para consulta, sin editar datos históricos. */
    private void verVersionesAnamnesis() {
        if (mascotaSeleccionada == null) return;
        try {
            java.util.List<veterinaria.entidad.HistoriaAnamnesis> versiones =
                    anamnesisDAO.listarVersiones(mascotaSeleccionada.getIdMascota());
            if (versiones.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay versiones de anamnesis registradas.");
                return;
            }
            String[] opciones = new String[versiones.size()];
            for (int i = 0; i < versiones.size(); i++) {
                veterinaria.entidad.HistoriaAnamnesis v = versiones.get(i);
                opciones[i] = v.getFechaRegistro().format(dtf) + " · "
                        + (v.getRegistradoPor() == null ? "Sin usuario" : v.getRegistradoPor())
                        + " · Versión #" + v.getIdAnamnesis();
            }
            Object elegida = JOptionPane.showInputDialog(this,
                    "Seleccioná una versión histórica para consultar:",
                    "Versiones de anamnesis · Solo lectura",
                    JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
            if (elegida == null) return;
            int indice = java.util.Arrays.asList(opciones).indexOf(elegida.toString());
            if (indice < 0) return;
            String[] valores = valoresAnamnesis(versiones.get(indice));
            StringBuilder texto = new StringBuilder();
            for (int i = 0; i < ETIQUETAS_ANAMNESIS.length; i++) {
                texto.append(ETIQUETAS_ANAMNESIS[i]).append(":\n")
                        .append(valores[i] == null || valores[i].isBlank()
                                ? "Sin registrar" : valores[i])
                        .append("\n\n");
            }
            javax.swing.JTextArea contenido = new javax.swing.JTextArea(texto.toString());
            contenido.setEditable(false);
            contenido.setLineWrap(true);
            contenido.setWrapStyleWord(true);
            contenido.setCaretPosition(0);
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(contenido);
            scroll.setPreferredSize(new java.awt.Dimension(660, 430));
            JOptionPane.showMessageDialog(this, scroll, elegida.toString(),
                    JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al recuperar versiones: "
                    + ex.getMessage(), "Anamnesis", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final String[] ETIQUETAS_ANAMNESIS = {
        "Dieta y alimentación", "Enfermedades previas", "Cirugías previas",
        "Esquema de vacunación", "Última desparasitación y producto",
        "Tratamientos recientes", "Esterilización (Sí / No / Sin datos)",
        "Número de partos", "Viajes recientes", "Convivencia con otros animales",
        "Comportamiento", "Identificaciones y señales particulares"
    };

    private String[] valoresAnamnesis(veterinaria.entidad.HistoriaAnamnesis a) {
        if (a == null) return new String[ETIQUETAS_ANAMNESIS.length];
        return new String[]{a.getDieta(), a.getEnfermedadesPrevias(),
            a.getCirugiasPrevias(), a.getEsquemaVacunal(), a.getDesparasitacion(),
            a.getTratamientosRecientes(), a.getEsterilizacion(), a.getNumeroPartos(),
            a.getViajesRecientes(), a.getConviveConAnimales(),
            a.getComportamiento(), a.getSenalesParticulares()};
    }

    private void abrirAnamnesis() {
        if (mascotaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un paciente.");
            return;
        }
        try {
            veterinaria.entidad.HistoriaAnamnesis anterior =
                    anamnesisDAO.ultima(mascotaSeleccionada.getIdMascota());
            String[] guardados = valoresAnamnesis(anterior);
            javax.swing.JPanel campos = new javax.swing.JPanel();
            campos.setLayout(new javax.swing.BoxLayout(campos, javax.swing.BoxLayout.Y_AXIS));
            campos.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
            javax.swing.JTextArea[] entradas = new javax.swing.JTextArea[ETIQUETAS_ANAMNESIS.length];
            javax.swing.JLabel encabezado = new javax.swing.JLabel(
                    "ANAMNESIS · " + mascotaSeleccionada.getNombre()
                    + (anterior == null ? " · Sin antecedentes registrados"
                            : " · Última actualización: " + anterior.getFechaRegistro().format(dtf)));
            encabezado.setFont(encabezado.getFont().deriveFont(java.awt.Font.BOLD));
            encabezado.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            campos.add(encabezado);
            campos.add(javax.swing.Box.createVerticalStrut(12));
            for (int i = 0; i < entradas.length; i++) {
                javax.swing.JPanel campo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
                campo.setOpaque(false);
                campo.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 85));
                javax.swing.JLabel etiqueta = new javax.swing.JLabel(ETIQUETAS_ANAMNESIS[i]);
                javax.swing.JTextArea entrada = new javax.swing.JTextArea(
                        guardados[i] == null ? "" : guardados[i], 2, 48);
                entrada.setLineWrap(true);
                entrada.setWrapStyleWord(true);
                entradas[i] = entrada;
                campo.add(etiqueta, java.awt.BorderLayout.NORTH);
                campo.add(new javax.swing.JScrollPane(entrada), java.awt.BorderLayout.CENTER);
                campos.add(campo);
                campos.add(javax.swing.Box.createVerticalStrut(10));
            }
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(campos);
            scroll.setPreferredSize(new java.awt.Dimension(720, 485));
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            int respuesta = JOptionPane.showConfirmDialog(this, scroll,
                    "Ficha de anamnesis · Guardar nueva versión",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (respuesta != JOptionPane.OK_OPTION) return;
            boolean cambios = false;
            for (int i = 0; i < entradas.length; i++) {
                if (!entradas[i].getText().trim().equals(
                        guardados[i] == null ? "" : guardados[i].trim())) {
                    cambios = true;
                    break;
                }
            }
            if (!cambios) {
                JOptionPane.showMessageDialog(this, "No hay cambios para guardar.");
                return;
            }
            veterinaria.entidad.HistoriaAnamnesis nueva =
                    new veterinaria.entidad.HistoriaAnamnesis();
            nueva.setMascota(mascotaSeleccionada);
            nueva.setRegistradoPor(
                    veterinaria.vista.application.Application.getNombreApellidoUsuarioLogeado());
            String[] d = new String[entradas.length];
            for (int i = 0; i < d.length; i++) d[i] = entradas[i].getText().trim();
            nueva.setDieta(d[0]);
            nueva.setEnfermedadesPrevias(d[1]);
            nueva.setCirugiasPrevias(d[2]);
            nueva.setEsquemaVacunal(d[3]);
            nueva.setDesparasitacion(d[4]);
            nueva.setTratamientosRecientes(d[5]);
            nueva.setEsterilizacion(d[6]);
            nueva.setNumeroPartos(d[7]);
            nueva.setViajesRecientes(d[8]);
            nueva.setConviveConAnimales(d[9]);
            nueva.setComportamiento(d[10]);
            nueva.setSenalesParticulares(d[11]);
            anamnesisDAO.guardarVersion(nueva);
            JOptionPane.showMessageDialog(this,
                    "Anamnesis guardada. Se conservó la versión anterior.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo consultar o guardar la anamnesis: " + ex.getMessage(),
                    "Error de anamnesis", JOptionPane.ERROR_MESSAGE);
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

        if (eventos == null || eventos.isEmpty()) {

            actualizarCantidadRegistros(0);

            JOptionPane.showMessageDialog(
                    this,
                    "La mascota seleccionada aún no posee registros en su historia clínica.",
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
        btnAnamnesis.setEnabled(hayMascota);
        btnHistorialAnamnesis.setEnabled(hayMascota);

        btnImprimirHistoria.setEnabled(hayRegistros);

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

            List<HistoriaEvento> eventos = historiaEventoControlador.listarPorMascota(mascotaSeleccionada.getIdMascota());
            if (eventos == null || eventos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "La mascota no tiene eventos para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("mascota", mascotaSeleccionada)
                    .put("eventos", eventos)
                    .put("anamnesis", anamnesisDAO.ultima(mascotaSeleccionada.getIdMascota()));

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
