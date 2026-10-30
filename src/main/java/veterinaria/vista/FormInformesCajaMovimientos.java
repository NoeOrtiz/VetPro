package veterinaria.vista;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;

import veterinaria.controlador.CajaMovimientoControlador;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaMovimiento.TipoMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.persistencia.JPAUtil;
import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Calendar;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.ReciboDAO;
import veterinaria.persistencia.CuentaCorrienteDAO;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.servicio.CajaSesionService;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.util.AppLog;
import veterinaria.util.PermisoUI;

public class FormInformesCajaMovimientos extends javax.swing.JPanel {

    private final CajaMovimientoControlador cajaMovimientoControlador = new CajaMovimientoControlador();
    // 🚀 REEMPLAZÁ TU LÍNEA POR ESTA:
    private final ReporteService reporteService = ReporteService.getInstance();
    private final ReciboDAO reciboDAO = new ReciboDAO();
    private final CuentaCorrienteDAO cuentaCorrienteDAO = new CuentaCorrienteDAO();
    private final CajaSesionService cajaSesionService = new CajaSesionService();

    private final DateTimeFormatter dtfFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<CajaMovimiento> movimientosActuales = new ArrayList<>();
    // Reutiliza el mismo paginador de Pacientes, sin duplicar controles.
    private veterinaria.util.PaginacionTablas paginacionMovimientos;
    private final javax.swing.JTextField busquedaPaginador = new javax.swing.JTextField();
    private boolean ajustandoSeleccionMovimientos;


    public FormInformesCajaMovimientos() {
        initComponents();
        PermisoUI.aplicar(this);

        // textos más claros
        btnImprimir.setText("Imprimir movimiento");
        btnImprimirLista.setText("Imprimir todos");
        btnBuscarMovimientos.setText("Buscar");
        lbInformeDeCreditosyDebitos.setText("Informe de Movimientos de Caja");

        // Distribución adaptable: evita que los botones se recorten y elimina
        // el desplazamiento horizontal cuando la ventana es angosta.
        javax.swing.SwingUtilities.invokeLater(() -> {
            tableCajaMovimientos.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            tableCajaMovimientos.setFillsViewportHeight(true);
            if (tableCajaMovimientos.getParent() instanceof javax.swing.JViewport) {
                javax.swing.JScrollPane panel = (javax.swing.JScrollPane) tableCajaMovimientos.getParent().getParent();
                panel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            }
        });

        organizarPantalla();
        inicializarComboTipos();
        inicializarTabla();
        paginacionMovimientos = new veterinaria.util.PaginacionTablas(
                tableCajaMovimientos, busquedaPaginador, 15, 2, 3, 4, 5, 6, 7, 8);
        jpListaOperaciones.add(paginacionMovimientos.getControles(), java.awt.BorderLayout.SOUTH);
        paginacionMovimientos.activarTamanoAdaptable();
        initListeners();

        // defaults: hoy
        Date hoy = new Date();
        jdcFechaDesdeFiltro.setDate(hoy);
        jdcFechaHastaFiltro.setDate(hoy);

        // totales
        resetTotales();

        // Dos consultas dentro de Informes de Caja, sin crear un nuevo menú
        // ni alterar los controles existentes de Movimientos.
        javax.swing.JPanel panelMovimientos = new javax.swing.JPanel(
                new java.awt.BorderLayout(0, 4));
        panelMovimientos.add(jpHeader, java.awt.BorderLayout.NORTH);
        panelMovimientos.add(jpListaOperaciones, java.awt.BorderLayout.CENTER);
        panelMovimientos.add(jpBotonesTotalesInferior, java.awt.BorderLayout.SOUTH);
        javax.swing.JTabbedPane secciones = new javax.swing.JTabbedPane();
        secciones.addTab("Movimientos de Caja", panelMovimientos);
        secciones.addTab("Recibos de Venta", new PanelRecibosVenta());
        removeAll();
        setLayout(new java.awt.BorderLayout());
        add(secciones, java.awt.BorderLayout.CENTER);
    }

    /** Sustituye las restricciones fijas del diseñador por una composición adaptable. */
    /** Layout flexible sin perder las fechas completas ni los separadores. */
    private void organizarPantalla() {
        jdcFechaDesdeFiltro.setPreferredSize(new java.awt.Dimension(152, 28));
        jdcFechaHastaFiltro.setPreferredSize(new java.awt.Dimension(152, 28));
        jdcFechaDesdeFiltro.setMinimumSize(new java.awt.Dimension(152, 28));
        jdcFechaHastaFiltro.setMinimumSize(new java.awt.Dimension(152, 28));
        jcbTipoMovimiento.setPreferredSize(new java.awt.Dimension(150, 28));

        jpHeader.removeAll();
        jpHeader.setLayout(new java.awt.BorderLayout(4, 6));
        jpHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 4, 12));
        javax.swing.JPanel cabecera = new javax.swing.JPanel(new java.awt.BorderLayout(0, 7));
        cabecera.add(lbInformeDeCreditosyDebitos, java.awt.BorderLayout.NORTH);
        cabecera.add(jSeparator1, java.awt.BorderLayout.SOUTH);
        jpHeader.add(cabecera, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel filtros = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 4));
        filtros.add(lbTipoMovimiento);
        filtros.add(jcbTipoMovimiento);
        filtros.add(lbFechaDesde);
        filtros.add(jdcFechaDesdeFiltro);
        filtros.add(lbFechaHasta);
        filtros.add(jdcFechaHastaFiltro);
        filtros.add(btnBuscarMovimientos);
        jpHeader.add(filtros, java.awt.BorderLayout.CENTER);
        javax.swing.JPanel acciones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));
        acciones.add(btnVer);
        acciones.add(btnImprimir);
        jpHeader.add(acciones, java.awt.BorderLayout.SOUTH);

        jpListaOperaciones.removeAll();
        jpListaOperaciones.setLayout(new java.awt.BorderLayout(6, 6));
        jpListaOperaciones.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 8, 4, 8));
        javax.swing.JPanel titulo = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
        titulo.add(jSeparator2, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel encabezadoLista = new javax.swing.JPanel(new java.awt.BorderLayout());
        encabezadoLista.add(lbListaDeOperaciones, java.awt.BorderLayout.WEST);
        encabezadoLista.add(btnImprimirLista, java.awt.BorderLayout.EAST);
        titulo.add(encabezadoLista, java.awt.BorderLayout.CENTER);
        titulo.add(jSeparator3, java.awt.BorderLayout.SOUTH);
        jpListaOperaciones.add(titulo, java.awt.BorderLayout.NORTH);
        jpListaOperaciones.add(scroll1, java.awt.BorderLayout.CENTER);
        scroll1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableCajaMovimientos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        setLayout(new java.awt.BorderLayout(0, 4));
        removeAll();
        add(jpHeader, java.awt.BorderLayout.NORTH);
        add(jpListaOperaciones, java.awt.BorderLayout.CENTER);
        add(jpBotonesTotalesInferior, java.awt.BorderLayout.SOUTH);
    }

    private void initListeners() {
        btnBuscarMovimientos.addActionListener(e -> buscarMovimientos());
        tableCajaMovimientos.getModel().addTableModelListener(e -> {
            if (ajustandoSeleccionMovimientos
                    || e.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || e.getColumn() != 1 || e.getFirstRow() < 0) return;
            int fila = e.getFirstRow();
            javax.swing.table.TableModel modelo = tableCajaMovimientos.getModel();
            if (fila >= modelo.getRowCount()) return;
            if (Boolean.TRUE.equals(modelo.getValueAt(fila, 1))) {
                seleccionarMovimientoUnico(fila);
            } else {
                int vista = tableCajaMovimientos.getSelectedRow();
                if (vista >= 0 && tableCajaMovimientos.convertRowIndexToModel(vista) == fila) {
                    tableCajaMovimientos.clearSelection();
                }
            }
        });
        tableCajaMovimientos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || ajustandoSeleccionMovimientos) return;
            int vista = tableCajaMovimientos.getSelectedRow();
            if (vista >= 0) seleccionarMovimientoUnico(tableCajaMovimientos.convertRowIndexToModel(vista));
        });

        // Botón "Ver" para detalle
        btnVer.addActionListener(e -> verDetalleMovimientoSeleccionado());

        // Doble click para ver detalle
        tableCajaMovimientos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verDetalleMovimientoSeleccionado();
                }
            }
        });
    }

    private void seleccionarMovimientoUnico(int filaModelo) {
        javax.swing.table.TableModel modelo = tableCajaMovimientos.getModel();
        if (ajustandoSeleccionMovimientos || filaModelo < 0 || filaModelo >= modelo.getRowCount()) return;
        ajustandoSeleccionMovimientos = true;
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                boolean marcar = i == filaModelo;
                if (!Boolean.valueOf(marcar).equals(modelo.getValueAt(i, 1))) {
                    modelo.setValueAt(marcar, i, 1);
                }
            }
            int vista = tableCajaMovimientos.convertRowIndexToView(filaModelo);
            if (vista >= 0 && vista < tableCajaMovimientos.getRowCount()) {
                tableCajaMovimientos.getSelectionModel().setSelectionInterval(vista, vista);
            }
        } finally {
            ajustandoSeleccionMovimientos = false;
            tableCajaMovimientos.repaint();
        }
    }

    private static String formatoTipo(TipoMovimiento tipo) {
        if (tipo == null) return "-";
        switch (tipo) {
            case APERTURA: return "Apertura";
            case CIERRE: return "Cierre";
            case CREDITO: return "Crédito";
            case DEBITO: return "Débito";
            default: return tipo.name();
        }
    }

    private void inicializarComboTipos() {
        jcbTipoMovimiento.removeAllItems();
        jcbTipoMovimiento.addItem("Todos");
        for (TipoMovimiento t : TipoMovimiento.values()) {
            jcbTipoMovimiento.addItem(formatoTipo(t));
        }
        jcbTipoMovimiento.setSelectedIndex(0);
    }

    private void inicializarTabla() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Seleccionar", "Fecha", "Tipo", "Monto", "Usuario", "Método", "Recibo", "Descripción"}
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Boolean.class : Object.class;
            }
        };
        tableCajaMovimientos.setModel(model);
        tableCajaMovimientos.getTableHeader().setReorderingAllowed(false);
        // El informe debe adaptarse al ancho disponible. La descripción ocupa
        // el espacio restante y se envuelve en varias líneas cuando es extensa.
        tableCajaMovimientos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // El ID sigue en el modelo para selección interna, pero no se muestra al usuario.
        tableCajaMovimientos.removeColumn(tableCajaMovimientos.getColumnModel().getColumn(0));
        tableCajaMovimientos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        // La selección visual sigue el checkbox, no un rango antiguo de JTable.
        // Usamos el mismo degradado horizontal que Recibos de Venta.
        veterinaria.vista.table.TableGradientCell rendererSeleccion =
                new veterinaria.vista.table.TableGradientCell();
        for (int columna = 1; columna < tableCajaMovimientos.getColumnCount() - 1; columna++) {
            tableCajaMovimientos.getColumnModel().getColumn(columna)
                    .setCellRenderer(rendererSeleccion);
        }
        configurarColumnasInforme();
        limpiarTabla();
    }

    private void configurarColumnasInforme() {
        // Priorizar la lectura completa de Tipo y dejar el espacio flexible
        // para Descripción. Método no necesita 190 px en todos los casos.
        int[] anchos = {88, 105, 150, 110, 120, 150, 110};
        for (int i = 0; i < anchos.length && i < tableCajaMovimientos.getColumnCount() - 1; i++) {
            tableCajaMovimientos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        int descripcion = tableCajaMovimientos.getColumnCount() - 1;
        tableCajaMovimientos.getColumnModel().getColumn(descripcion)
                .setCellRenderer(new DescripcionMultilineaRenderer());
        tableCajaMovimientos.getColumnModel().getColumn(descripcion).setPreferredWidth(420);
    }

    private static final class DescripcionMultilineaRenderer extends JTextArea implements TableCellRenderer {

        private boolean resaltada;
        private int desplazamientoX;
        private int anchoTabla;
        private java.awt.Color inicio;
        private java.awt.Color fin;

        private DescripcionMultilineaRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setFont(table.getFont());
            // Mismo criterio que TableGradientCell: manda la casilla, no el
            // resaltado temporal de JTable.
            resaltada = Boolean.TRUE.equals(table.getValueAt(row, 1));
            java.awt.Color fondoTema = javax.swing.UIManager.getColor("Panel.background");
            boolean oscuro = fondoTema != null
                    && (fondoTema.getRed() * 299 + fondoTema.getGreen() * 587
                    + fondoTema.getBlue() * 114) / 1000 < 128;
            inicio = oscuro ? new java.awt.Color(169, 65, 44)
                    : new java.awt.Color(255, 210, 157);
            fin = oscuro ? new java.awt.Color(44, 105, 177)
                    : new java.awt.Color(174, 207, 245);
            setForeground(resaltada
                    ? (oscuro ? java.awt.Color.WHITE : new java.awt.Color(35, 39, 45))
                    : table.getForeground());
            setBackground(table.getBackground());
            java.awt.Rectangle celda = table.getCellRect(row, column, true);
            desplazamientoX = -celda.x;
            anchoTabla = Math.max(1, table.getWidth());

            int width = table.getColumnModel().getColumn(column).getWidth();
            setSize(Math.max(width, 1), Short.MAX_VALUE);
            int requiredHeight = Math.max(table.getRowHeight(), getPreferredSize().height);
            if (table.getRowHeight(row) != requiredHeight) {
                table.setRowHeight(row, requiredHeight);
            }
            return this;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            if (resaltada) {
                g2.setPaint(new java.awt.GradientPaint(desplazamientoX, 0, inicio,
                        desplazamientoX + anchoTabla, 0, fin));
            } else {
                g2.setColor(getBackground());
            }
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void limpiarTabla() {
        DefaultTableModel model = (DefaultTableModel) tableCajaMovimientos.getModel();
        model.setRowCount(0);
        resetTotales();
    }

    /**
     * Resetea los totales mostrados en pantalla. En este formulario los totales
     * se visualizan en los campos txtVentas (Créditos) y txtCompras (Débitos).
     */
    private void resetTotales() {
        if (txtVentas != null) {
            txtVentas.setText("$ " + formatMoney(BigDecimal.ZERO));
        }
        if (txtCompras != null) {
            txtCompras.setText("$ " + formatMoney(BigDecimal.ZERO));
        }
    }

    private void buscarMovimientos() {
        try {
            TipoMovimiento tipo = getTipoSeleccionado();
            Date desde = jdcFechaDesdeFiltro.getDate();
            Date hasta = jdcFechaHastaFiltro.getDate();

            if (desde != null && hasta != null && desde.after(hasta)) {
                JOptionPane.showMessageDialog(this, "La fecha 'Desde' no puede ser mayor que 'Hasta'.", "Filtro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            movimientosActuales = new ArrayList<>(cajaMovimientoControlador.obtenerMovimientosFiltrados(tipo, desde, hasta));
            agregarEventosSesion(movimientosActuales, tipo, desde, hasta);
            agregarVentasCuentaCorrienteInformativas(movimientosActuales, tipo, desde, hasta);
            movimientosActuales.sort((a, b) -> {
                Date fa = a.getFechaHora() != null ? a.getFechaHora() : a.getFecha();
                Date fb = b.getFechaHora() != null ? b.getFechaHora() : b.getFecha();
                if (fa == null && fb == null) return 0;
                if (fa == null) return 1;
                if (fb == null) return -1;
                return fb.compareTo(fa);
            });
            busquedaPaginador.setText("");
            cargarEnTabla(movimientosActuales);

            if (movimientosActuales.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron movimientos con los filtros seleccionados.", "Resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showException("Error al buscar movimientos", ex);
        }
    }

    /**
     * Operaciones informativas: ventas financiadas sin entrada de dinero.
     * Se consultan del libro de cuenta corriente, nunca se persisten en Caja.
     */
    private void agregarVentasCuentaCorrienteInformativas(List<CajaMovimiento> destino,
            TipoMovimiento filtro, Date desde, Date hasta) {
        // La categoría informativa solo corresponde a la vista Todos.
        if (filtro != null) {
            return;
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT m FROM CuentaCorrienteMovimiento m "
                    + "WHERE m.tipoMovimiento = :tipo AND m.recibo IS NOT NULL";
            if (desde != null) jpql += " AND m.fechaMovimiento >= :desde";
            if (hasta != null) jpql += " AND m.fechaMovimiento <= :hasta";
            javax.persistence.TypedQuery<CuentaCorrienteMovimiento> q =
                    em.createQuery(jpql, CuentaCorrienteMovimiento.class);
            q.setParameter("tipo", CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
            if (desde != null) {
                q.setParameter("desde", desde.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
            if (hasta != null) {
                q.setParameter("hasta", hasta.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
            for (CuentaCorrienteMovimiento m : q.getResultList()) {
                if (m.getIdMovimiento() == null || m.getFechaMovimiento() == null) continue;
                Date fecha = Date.from(m.getFechaMovimiento().atStartOfDay(ZoneId.systemDefault()).toInstant());
                CajaMovimiento informativo = new CajaMovimiento(
                        -1000000000L - m.getIdMovimiento(), m.getMonto(),
                        TipoMovimiento.CREDITO, fecha, null,
                        "INFORMATIVO - Venta a Cuenta Corriente (sin ingreso de dinero)",
                        false);
                informativo.setFechaHora(fecha);
                informativo.setRecibo(m.getRecibo());
                // Resolver Usuario y Persona antes de cerrar EntityManager:
                // de lo contrario el informe dispara LazyInitializationException.
                Usuario usuarioVenta = m.getRecibo().getUsuario();
                if (usuarioVenta != null) {
                    org.hibernate.Hibernate.initialize(usuarioVenta);
                    org.hibernate.Hibernate.initialize(usuarioVenta.getPersona());
                }
                informativo.setUsuario(usuarioVenta);
                informativo.setAfectaEfectivo(false);
                destino.add(informativo);
            }
        } finally {
            em.close();
        }
    }

    private void agregarEventosSesion(List<CajaMovimiento> destino, TipoMovimiento filtro, Date desde, Date hasta) {
        if (filtro != null && filtro != TipoMovimiento.APERTURA && filtro != TipoMovimiento.CIERRE) return;
        for (CajaSesion s : cajaSesionService.listarSesiones(desde, hasta)) {
            long base = s.getIdCajaSesion() != null ? s.getIdCajaSesion() : 0L;
            if ((filtro == null || filtro == TipoMovimiento.APERTURA) && enRango(s.getFechaApertura(), desde, hasta)) {
                CajaMovimiento m = new CajaMovimiento(-(base * 2L + 1L), s.getMontoInicial(), TipoMovimiento.APERTURA,
                        s.getFechaApertura(), s.getUsuarioApertura(), "Apertura de caja", false);
                m.setFechaHora(s.getFechaApertura());
                destino.add(m);
            }
            if ((filtro == null || filtro == TipoMovimiento.CIERRE) && s.getFechaCierre() != null && enRango(s.getFechaCierre(), desde, hasta)) {
                String desc = "Cierre de caja - Esperado: $ " + formatMoney(s.getEfectivoEsperado())
                        + " - Contado: $ " + formatMoney(s.getEfectivoContado())
                        + " - Diferencia: $ " + formatMoney(s.getDiferencia());
                if (s.getMotivoDiferencia() != null && !s.getMotivoDiferencia().trim().isEmpty()) {
                    desc += " - Motivo: " + s.getMotivoDiferencia().trim();
                }
                CajaMovimiento m = new CajaMovimiento(-(base * 2L + 2L), s.getEfectivoContado(), TipoMovimiento.CIERRE,
                        s.getFechaCierre(), s.getUsuarioCierre(), desc, false);
                m.setFechaHora(s.getFechaCierre());
                destino.add(m);
            }
        }
    }

    private boolean enRango(Date fecha, Date desde, Date hasta) {
        if (fecha == null) return false;
        java.util.Calendar ini = java.util.Calendar.getInstance();
        java.util.Calendar fin = java.util.Calendar.getInstance();
        if (desde != null) {
            ini.setTime(desde); ini.set(java.util.Calendar.HOUR_OF_DAY, 0); ini.set(java.util.Calendar.MINUTE, 0); ini.set(java.util.Calendar.SECOND, 0); ini.set(java.util.Calendar.MILLISECOND, 0);
            if (fecha.before(ini.getTime())) return false;
        }
        if (hasta != null) {
            fin.setTime(hasta); fin.set(java.util.Calendar.HOUR_OF_DAY, 23); fin.set(java.util.Calendar.MINUTE, 59); fin.set(java.util.Calendar.SECOND, 59); fin.set(java.util.Calendar.MILLISECOND, 999);
            if (fecha.after(fin.getTime())) return false;
        }
        return true;
    }

    private TipoMovimiento getTipoSeleccionado() {
        Object sel = jcbTipoMovimiento.getSelectedItem();
        if (sel == null) {
            return null;
        }
        String s = sel.toString();
        if (s.equalsIgnoreCase("TODOS")) {
            return null;
        }
        try {
            return TipoMovimiento.valueOf(s.toUpperCase(java.util.Locale.ROOT).replace("É", "E"));
        } catch (Exception e) {
            return null;
        }
    }

    private void cargarEnTabla(List<CajaMovimiento> movimientos) {
        tableCajaMovimientos.clearSelection();
        limpiarTabla();
        DefaultTableModel model = (DefaultTableModel) tableCajaMovimientos.getModel();
        if (movimientos == null) {
            return;
        }

        BigDecimal totalCreditos = BigDecimal.ZERO;
        BigDecimal totalDebitos = BigDecimal.ZERO;
        // Los totales corresponden a TODO el período filtrado, no solo a la página.
        for (CajaMovimiento m : movimientos) {
            boolean informativo = m.getIdMovimiento() != null && m.getIdMovimiento() <= -1000000000L;
            if (informativo || m.getTipoMovimiento() == null || m.getMonto() == null) continue;
            if (m.getTipoMovimiento() == TipoMovimiento.CREDITO) totalCreditos = totalCreditos.add(m.getMonto());
            if (m.getTipoMovimiento() == TipoMovimiento.DEBITO) totalDebitos = totalDebitos.add(m.getMonto());
        }
        for (CajaMovimiento m : movimientos) {
            String fecha = "-";
            fecha = formatFecha(m.getFecha());
            boolean informativoCC = m.getIdMovimiento() != null && m.getIdMovimiento() <= -1000000000L;
            String tipo = informativoCC ? "Cuenta Corriente (informativo)"
                    : (m.getTipoMovimiento() != null ? formatoTipo(m.getTipoMovimiento()) : "-");
            String monto = "$ " + formatMoney(m.getMonto());

            Usuario u = m.getUsuario();
            String usuario = nombreUsuario(u);

            MetodoPago mp = m.getMetodoPago();
            String metodo = informativoCC ? "Cuenta Corriente"
                    : (mp != null ? safe(mp.getNombre()) : "N/A");

            Recibo r = m.getRecibo();
            String recibo = (r != null && r.getIdRecibo() != null) ? ("Recibo N.º " + r.getIdRecibo()) : "-";

            String desc = informativoCC
                    ? "Venta a Cuenta Corriente - " + recibo
                    : normalizarDescripcion(safe(m.getDescripcion()), r);
            if (r != null && r.getIdRecibo() != null) {
                try {
                    Recibo detalle = reciboDAO.obtenerReciboConDetalles(r.getIdRecibo());
                    if (detalle != null && detalle.getCliente() != null && detalle.getCliente().getPersona() != null) {
                        Persona persona = detalle.getCliente().getPersona();
                        String nombreCliente = (safe(persona.getNombre()) + " " + safe(persona.getApellido())).trim();
                        if (!nombreCliente.isEmpty() && !desc.contains(nombreCliente)) {
                            desc += " - Cliente: " + nombreCliente;
                        }
                    }
                } catch (Exception ex) {
                    AppLog.warn(FormInformesCajaMovimientos.class, "No se pudo recuperar el cliente del recibo para el informe de caja", ex);
                }
            }

            desc = agregarClienteCuentaCorriente(desc);

            model.addRow(new Object[]{m.getIdMovimiento(), Boolean.FALSE, fecha, tipo, monto, usuario, metodo, recibo, desc});

        }

        setTotales(totalCreditos, totalDebitos);
        if (paginacionMovimientos != null) paginacionMovimientos.actualizar();
        // El renderer multilínea ajusta la altura según el ancho real disponible.
        javax.swing.SwingUtilities.invokeLater(() -> {
            configurarColumnasInforme();
            tableCajaMovimientos.revalidate();
            tableCajaMovimientos.repaint();
        });
    }

    /**
     * Setea totales calculados a partir de la lista actual.
     *
     * @param totalCreditos suma de movimientos tipo *CREDITO
     *
     * @param totalDebitos suma de movimientos tipo *DEBITO*
     */
    private void setTotales(BigDecimal totalCreditos, BigDecimal totalDebitos) {
        if (txtVentas != null) {
            txtVentas.setText("$ " + formatMoney(totalCreditos != null ? totalCreditos : BigDecimal.ZERO));
        }
        if (txtCompras != null) {
            txtCompras.setText("$ " + formatMoney(totalDebitos != null ? totalDebitos : BigDecimal.ZERO));
        }
    }

    private void verDetalleMovimientoSeleccionado() {
        CajaMovimiento mov = getMovimientoSeleccionado();
        if (mov == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un movimiento en la tabla.", "Detalle", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String detalle = buildDetalleMovimiento(mov);
            JOptionPane.showMessageDialog(this, detalle, "Detalle de operación", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showException("Error al mostrar el detalle", ex);
        }
    }

    /**
     * Arma un texto de detalle enriquecido. - Si el movimiento referencia un
     * Recibo, trae el recibo con sus productos y métodos de pago. - Si es
     * apertura/cierre, enfatiza usuario + monto + descripción.
     */
    private String buildDetalleMovimiento(CajaMovimiento mov) {
        if (mov.getIdMovimiento() != null && mov.getIdMovimiento() <= -1000000000L) {
            return "Venta a Cuenta Corriente (informativa)\\n"
                    + "Fecha: " + formatFecha(mov.getFecha()) + "\\n"
                    + "Importe financiado: $ " + formatMoney(mov.getMonto()) + "\\n"
                    + "Recibo: " + (mov.getRecibo() != null ? mov.getRecibo().getIdRecibo() : "-") + "\\n"
                    + "No representa un ingreso de dinero en Caja.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Movimiento ID: ").append(mov.getIdMovimiento()).append("\n");
        sb.append("Fecha: ").append(formatFecha(mov.getFecha())).append("\n");
        sb.append("Tipo de movimiento: ").append(mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : "-").append("\n");
        sb.append("Monto: $ ").append(formatMoney(mov.getMonto())).append("\n");
        sb.append("Usuario (movimiento): ").append(nombreUsuario(mov.getUsuario())).append("\n");
        sb.append("Método (movimiento): ").append(mov.getMetodoPago() != null ? safe(mov.getMetodoPago().getNombre()) : "N/A").append("\n");
        sb.append("Descripción: ").append(safe(mov.getDescripcion())).append("\n");

        // Si hay recibo asociado, mostrar detalle de la operación (venta/devolución)
        if (mov.getRecibo() != null && mov.getRecibo().getIdRecibo() != null) {
            Long idRecibo = mov.getRecibo().getIdRecibo();
            Recibo reciboDet = reciboDAO.obtenerReciboConDetalles(idRecibo);
            sb.append("\n");
            sb.append("==============================\n");
            sb.append("Detalle de Recibo #").append(idRecibo).append("\n");

            if (reciboDet == null) {
                sb.append("No fue posible cargar el detalle completo del recibo.\n");
                return sb.toString();
            }

            sb.append("Operación: ").append(safe(reciboDet.getTipo())).append("\n");
            sb.append("Fecha recibo: ").append(formatFecha(reciboDet.getFecha())).append("\n");

            // Cliente
            sb.append("Cliente: ").append(nombreCliente(reciboDet)).append("\n");

            // Usuario que registró el recibo
            sb.append("Usuario (recibo): ").append(nombreUsuario(reciboDet.getUsuario())).append("\n");

            // Totales
            sb.append("Subtotal: $ ").append(formatMoney(reciboDet.getSubtotalRecibo())).append("\n");
            sb.append("Descuento: $ ").append(formatMoney(reciboDet.getTotalDescuento())).append("\n");
            sb.append("IVA: $ ").append(formatMoney(reciboDet.getTotalIva())).append("\n");
            sb.append("TOTAL: $ ").append(formatMoney(reciboDet.getTotalRecibo())).append("\n");

            // Productos
            sb.append("\nProductos:\n");
            List<ReciboProductos> items = reciboDet.getProductos();
            if (items == null || items.isEmpty()) {
                sb.append("  (sin ítems)\n");
            } else {
                for (ReciboProductos rp : items) {
                    if (rp == null) {
                        continue;
                    }
                    String prodNombre = (rp.getProducto() != null ? safe(rp.getProducto().getNombre()) : "(producto)");
                    String prodCodigo = (rp.getProducto() != null ? safe(rp.getProducto().getCodigo()) : "");
                    if (!prodCodigo.isEmpty()) {
                        prodCodigo = " [" + prodCodigo + "]";
                    }
                    sb.append("  - ")
                            .append(rp.getCantidad() != null ? rp.getCantidad() : 0)
                            .append(" x ")
                            .append(prodNombre)
                            .append(prodCodigo)
                            .append(" | Unit: $").append(formatMoney(rp.getTotalUnitario()))
                            .append(" | Total: $").append(formatMoney(rp.getTotalCantidad()))
                            .append("\n");
                }
            }

            // Métodos de pago
            sb.append("\nMétodos de pago:\n");
            List<ReciboMetodoPago> mps = reciboDet.getMetodosPago();
            if (mps == null || mps.isEmpty()) {
                sb.append("  (sin métodos)\n");
            } else {
                for (ReciboMetodoPago mp : mps) {
                    if (mp == null) {
                        continue;
                    }
                    String mpNombre = (mp.getMetodoPago() != null ? safe(mp.getMetodoPago().getNombre()) : "(método)");
                    sb.append("  - ").append(mpNombre)
                            .append(" | $").append(formatMoney(mp.getMonto()));
                    if (mp.getReferencia() != null && !mp.getReferencia().trim().isEmpty()) {
                        sb.append(" | Ref: ").append(mp.getReferencia().trim());
                    }
                    sb.append("\n");
                }
            }
        } else {
            // Apertura/Cierre sin recibo: mostrar lo importante
            if (mov.getTipoMovimiento() == TipoMovimiento.APERTURA || mov.getTipoMovimiento() == TipoMovimiento.CIERRE) {
                sb.append("\nNota: Movimiento interno de ")
                        .append(mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : "-")
                        .append(".\n");
            }
        }

        return sb.toString();
    }

    private static String nombreCliente(Recibo r) {
        if (r == null || r.getCliente() == null) {
            return "-";
        }
        String razon = safe(r.getCliente().getRazonSocial());
        if (!razon.trim().isEmpty()) {
            String cuit = safe(r.getCliente().getCuit());
            if (!cuit.trim().isEmpty()) {
                return razon.trim() + " (CUIT " + cuit.trim() + ")";
            }
            return razon.trim();
        }
        Persona p = r.getCliente().getPersona();
        if (p == null) {
            return "-";
        }
        String nombre = (p.getNombre() != null ? p.getNombre() : "") + (p.getApellido() != null ? " " + p.getApellido() : "");
        return nombre.trim().isEmpty() ? "-" : nombre.trim();
    }

    private String formatFecha(Date d) {
        if (d == null) {
            return "-";
        }
        try {
            // java.sql.Date no soporta toInstant()
            if (d instanceof java.sql.Date) {
                java.sql.Date sd = (java.sql.Date) d;
                return sd.toLocalDate().format(dtfFecha);
            }
            if (d instanceof java.sql.Timestamp) {
                java.sql.Timestamp ts = (java.sql.Timestamp) d;
                return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dtfFecha);
            }
            return java.time.Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().format(dtfFecha);
        } catch (Exception e) {
            return String.valueOf(d);
        }
    }

    private void showException(String titulo, Exception ex) {
        AppLog.error(FormInformesCajaMovimientos.class, "Error en informe de caja", ex);

        String msg = ex.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = ex.toString();
        }

        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root != null && root != ex) {
            String r = root.getMessage();
            if (r == null || r.trim().isEmpty()) {
                r = root.toString();
            }
            msg = msg + "\n\nCausa: " + r;
        }

        JOptionPane.showMessageDialog(this, msg, titulo, JOptionPane.ERROR_MESSAGE);
    }

    private CajaMovimiento getMovimientoSeleccionado() {
        int filaModelo = -1;
        javax.swing.table.TableModel model = tableCajaMovimientos.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 1))) {
                filaModelo = i;
                break;
            }
        }
        if (filaModelo < 0) {
            int filaVista = tableCajaMovimientos.getSelectedRow();
            if (filaVista >= 0) filaModelo = tableCajaMovimientos.convertRowIndexToModel(filaVista);
        }
        if (filaModelo < 0) return null;
        Object idObj = model.getValueAt(filaModelo, 0);
        if (idObj == null) return null;
        try {
            Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.valueOf(idObj.toString());
            for (CajaMovimiento movimiento : movimientosActuales) {
                if (movimiento != null && id.equals(movimiento.getIdMovimiento())) return movimiento;
            }
        } catch (NumberFormatException ex) {
            AppLog.warn(FormInformesCajaMovimientos.class, "Identificador de movimiento inválido", ex);
        }
        return null;
    }

    private void imprimirMovimientoSeleccionado() {
        CajaMovimiento mov = getMovimientoSeleccionado();
        if (mov == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un movimiento en la tabla.",
                    "Imprimir", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (mov.getIdMovimiento() != null && mov.getIdMovimiento() <= -1000000000L) {
            JOptionPane.showMessageDialog(this, "Esta venta es informativa. Consulte el recibo o los movimientos de Cuenta Corriente para imprimir su comprobante.", "Cuenta Corriente", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ReporteRequest req = new ReporteRequest().put("movimiento", mov)
                .put("FORMATO_REPORTE", veterinaria.reportes.pdf.ReporteSelectorUtil.Formato.PDF);
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.CAJA_MOVIMIENTO, req);
    }

    private void imprimirListaMovimientos() {
        if (tableCajaMovimientos.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay movimientos para imprimir.",
                    "Imprimir", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        String[] etiquetas = {"Todos los movimientos", "Solo créditos", "Solo débitos", "Aperturas", "Cierres"};
        String[] filtros = {"TODOS", "CREDITO", "DEBITO", "APERTURA", "CIERRE"};
        for (int i = 0; i < etiquetas.length; i++) {
            final String filtro = filtros[i];
            javax.swing.JMenuItem item = new javax.swing.JMenuItem(etiquetas[i]);
            item.addActionListener(e -> solicitarFormatoEImprimirMovimientos(filtro));
            menu.add(item);
        }
        menu.show(btnImprimirLista, 0, btnImprimirLista.getHeight());
    }

    private void solicitarFormatoEImprimirMovimientos(String filtro) {
        veterinaria.reportes.pdf.ReporteSelectorUtil.Formato formato =
                veterinaria.reportes.pdf.ReporteSelectorUtil.solicitarFormato(this);
        if (formato == null) return;
        javax.swing.table.DefaultTableModel copia = new javax.swing.table.DefaultTableModel(
                new String[]{"ID", "Fecha", "Tipo", "Monto", "Usuario", "Método", "Recibo", "Descripción"}, 0);
        javax.swing.table.TableModel original = tableCajaMovimientos.getModel();
        for (int fila = 0; fila < original.getRowCount(); fila++) {
            String tipo = String.valueOf(original.getValueAt(fila, 3));
            String normalizado = java.text.Normalizer.normalize(tipo, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toUpperCase(java.util.Locale.ROOT);
            if (!"TODOS".equals(filtro) && !filtro.equals(normalizado)) continue;
            copia.addRow(new Object[]{
                original.getValueAt(fila, 0), original.getValueAt(fila, 2),
                original.getValueAt(fila, 3), original.getValueAt(fila, 4),
                original.getValueAt(fila, 5), original.getValueAt(fila, 6),
                original.getValueAt(fila, 7), original.getValueAt(fila, 8)
            });
        }
        if (copia.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay movimientos para el tipo seleccionado.",
                    "Imprimir", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ReporteRequest req = new ReporteRequest()
                .put("tabla", new JTable(copia))
                .put("filtros", buildFiltroTexto() + " | Reporte=" + filtro)
                .put("FORMATO_REPORTE", formato);
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(
                this, ReporteTipo.CAJA_MOVIMIENTOS_LISTADO, req);
    }

    private String buildFiltroTexto() {
        String tipo = (jcbTipoMovimiento.getSelectedItem() != null ? jcbTipoMovimiento.getSelectedItem().toString() : "TODOS");
        String desde = formatFecha(jdcFechaDesdeFiltro.getDate());
        String hasta = formatFecha(jdcFechaHastaFiltro.getDate());
        return "Filtros: Tipo=" + tipo + " | Desde=" + desde + " | Hasta=" + hasta;
    }

    /**
     * Enriquece cobros de cuenta corriente que no tienen recibo asociado.
     * El número indicado en "Cobro cuenta corriente N.º X" identifica la cuenta.
     * Ante una cuenta inexistente se conserva la descripción original.
     */
    private String agregarClienteCuentaCorriente(String descripcion) {
        if (descripcion == null || !descripcion.toLowerCase(java.util.Locale.ROOT)
                .contains("cobro cuenta corriente")) {
            return descripcion;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "(?i)cobro\\s+cuenta\\s+corriente\\s+(?:n\\s*[.º°o]*\\s*)?(\\d+)")
                .matcher(descripcion);
        if (!matcher.find()) return descripcion;
        try {
            CuentaCorriente cuenta = cuentaCorrienteDAO.buscarPorId(Integer.valueOf(matcher.group(1)));
            if (cuenta == null || cuenta.getCliente() == null || cuenta.getCliente().getPersona() == null) {
                return descripcion;
            }
            Persona persona = cuenta.getCliente().getPersona();
            String nombre = (safe(persona.getNombre()) + " " + safe(persona.getApellido())).trim();
            return nombre.isEmpty() || descripcion.contains(nombre)
                    ? descripcion : descripcion + " - Cliente: " + nombre;
        } catch (Exception ex) {
            AppLog.warn(FormInformesCajaMovimientos.class,
                    "No se pudo identificar el cliente del cobro de cuenta corriente", ex);
            return descripcion;
        }
    }

    private static String normalizarDescripcion(String descripcion, Recibo recibo) {
        if (recibo != null && recibo.getIdRecibo() != null && descripcion != null
                && descripcion.toLowerCase(java.util.Locale.ROOT).contains("venta")) {
            String cliente = "Consumidor Final";
            try {
                // El recibo de la tabla puede estar desacoplado de Hibernate.
                // No acceder a asociaciones LAZY sin sesión activa.
                if (recibo.getCliente() != null
                        && org.hibernate.Hibernate.isInitialized(recibo.getCliente())
                        && org.hibernate.Hibernate.isInitialized(recibo.getCliente().getPersona())
                        && recibo.getCliente().getPersona() != null) {
                    Persona p = recibo.getCliente().getPersona();
                    String nombre = (safe(p.getNombre()) + " " + safe(p.getApellido())).trim();
                    if (!nombre.isEmpty()) cliente = nombre;
                }
            } catch (Exception ex) {
                AppLog.warn(FormInformesCajaMovimientos.class, "No se pudo resolver cliente de venta", ex);
            }
            return "Venta a " + cliente + " - Recibo N.º " + recibo.getIdRecibo();
        }
        return descripcion != null ? descripcion : "";
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String nombreUsuario(Usuario u) {
        if (u == null) {
            return "-";
        }
        Persona p = u.getPersona();
        if (p == null) {
            return u.getNombreUsuario() != null ? u.getNombreUsuario() : "-";
        }
        String nombre = (p.getNombre() != null ? p.getNombre() : "")
                + (p.getApellido() != null ? " " + p.getApellido() : "");
        nombre = nombre.trim();
        if (nombre.isEmpty()) {
            return u.getNombreUsuario() != null ? u.getNombreUsuario() : "-";
        }
        return nombre;
    }

    private static String formatMoney(BigDecimal v) {
        try {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "AR"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(v != null ? v : BigDecimal.ZERO);
        } catch (Exception e) {
            return String.valueOf(v != null ? v : BigDecimal.ZERO);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbInformeDeCreditosyDebitos = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        btnImprimir = new javax.swing.JButton();
        btnVer = new javax.swing.JButton();
        lbTipoMovimiento = new javax.swing.JLabel();
        jcbTipoMovimiento = new javax.swing.JComboBox();
        lbFechaDesde = new javax.swing.JLabel();
        jdcFechaDesdeFiltro = new com.toedter.calendar.JDateChooser();
        lbFechaHasta = new javax.swing.JLabel();
        jdcFechaHastaFiltro = new com.toedter.calendar.JDateChooser();
        btnBuscarMovimientos = new javax.swing.JButton();
        jpListaOperaciones = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        lbListaDeOperaciones = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scroll1 = new javax.swing.JScrollPane();
        tableCajaMovimientos = new veterinaria.vista.table.AutoTable();
        btnImprimirLista = new javax.swing.JButton();
        jpBotonesTotalesInferior = new javax.swing.JPanel();
        lbTotalVentas = new javax.swing.JLabel();
        txtVentas = new javax.swing.JTextField();
        lbTotalCompras = new javax.swing.JLabel();
        txtCompras = new javax.swing.JTextField();

        lbInformeDeCreditosyDebitos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbInformeDeCreditosyDebitos.setText("Informes de Creditos y Debitos");

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        btnVer.setText("Ver ");

        lbTipoMovimiento.setText("Tipo Movimiento:");

        lbFechaDesde.setText("Desde:");

        lbFechaHasta.setText("Hasta:");

        btnBuscarMovimientos.setText("Buscar Movimientos");

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(lbInformeDeCreditosyDebitos)
                .addContainerGap(880, Short.MAX_VALUE))
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jcbTipoMovimiento, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbTipoMovimiento))
                        .addGap(12, 12, 12)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jdcFechaDesdeFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbFechaDesde))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbFechaHasta)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnVer)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnImprimir))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(jdcFechaHastaFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnBuscarMovimientos)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addComponent(jSeparator1))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbInformeDeCreditosyDebitos)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(lbFechaHasta)
                                    .addComponent(btnImprimir)
                                    .addComponent(btnVer))
                                .addGap(2, 2, 2)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jdcFechaHastaFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jdcFechaDesdeFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnBuscarMovimientos)))
                        .addGap(0, 27, Short.MAX_VALUE))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbTipoMovimiento)
                            .addComponent(lbFechaDesde))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbTipoMovimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        lbListaDeOperaciones.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDeOperaciones.setText("Lista de operaciones");

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableCajaMovimientos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Fecha", "Tipo", "Monto", "Usuario", "Método", "Recibo", "Descripción"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableCajaMovimientos.setMinimumSize(new java.awt.Dimension(848, 220));
        tableCajaMovimientos.setPreferredSize(new java.awt.Dimension(848, 220));
        tableCajaMovimientos.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableCajaMovimientos);

        btnImprimirLista.setText("Imprimir Lista");
        btnImprimirLista.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirListaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpListaOperacionesLayout = new javax.swing.GroupLayout(jpListaOperaciones);
        jpListaOperaciones.setLayout(jpListaOperacionesLayout);
        jpListaOperacionesLayout.setHorizontalGroup(
            jpListaOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 1110, Short.MAX_VALUE)
            .addGroup(jpListaOperacionesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpListaOperacionesLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lbListaDeOperaciones)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimirLista))
                    .addComponent(jSeparator3)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jpListaOperacionesLayout.setVerticalGroup(
            jpListaOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaOperacionesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpListaOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListaDeOperaciones)
                    .addComponent(btnImprimirLista))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE))
        );

        lbTotalVentas.setText("Total Ventas:");

        lbTotalCompras.setText("Total Compras:");

        javax.swing.GroupLayout jpBotonesTotalesInferiorLayout = new javax.swing.GroupLayout(jpBotonesTotalesInferior);
        jpBotonesTotalesInferior.setLayout(jpBotonesTotalesInferiorLayout);
        jpBotonesTotalesInferiorLayout.setHorizontalGroup(
            jpBotonesTotalesInferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpBotonesTotalesInferiorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbTotalVentas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtVentas, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbTotalCompras)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCompras, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jpBotonesTotalesInferiorLayout.setVerticalGroup(
            jpBotonesTotalesInferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpBotonesTotalesInferiorLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpBotonesTotalesInferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCompras, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbTotalCompras)
                    .addComponent(txtVentas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbTotalVentas))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpListaOperaciones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpBotonesTotalesInferior, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaOperaciones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpBotonesTotalesInferior, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        imprimirMovimientoSeleccionado();
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void btnImprimirListaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirListaActionPerformed
        imprimirListaMovimientos();
    }//GEN-LAST:event_btnImprimirListaActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscarMovimientos;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnVer;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JComboBox jcbTipoMovimiento;
    private com.toedter.calendar.JDateChooser jdcFechaDesdeFiltro;
    private com.toedter.calendar.JDateChooser jdcFechaHastaFiltro;
    private javax.swing.JPanel jpBotonesTotalesInferior;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaOperaciones;
    private javax.swing.JLabel lbFechaDesde;
    private javax.swing.JLabel lbFechaHasta;
    private javax.swing.JLabel lbInformeDeCreditosyDebitos;
    private javax.swing.JLabel lbListaDeOperaciones;
    private javax.swing.JLabel lbTipoMovimiento;
    private javax.swing.JLabel lbTotalCompras;
    private javax.swing.JLabel lbTotalVentas;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JTable tableCajaMovimientos;
    private javax.swing.JTextField txtCompras;
    private javax.swing.JTextField txtVentas;
    // End of variables declaration//GEN-END:variables
}
