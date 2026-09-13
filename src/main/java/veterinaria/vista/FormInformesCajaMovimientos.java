package veterinaria.vista;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import veterinaria.controlador.CajaMovimientoControlador;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaMovimiento.TipoMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.ReciboDAO;
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

    private final DateTimeFormatter dtfFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<CajaMovimiento> movimientosActuales = new ArrayList<>();

    public FormInformesCajaMovimientos() {
        initComponents();
        PermisoUI.aplicar(this);

        // textos más claros
        btnImprimir.setText("Imprimir movimiento");
        btnImprimirLista.setText("Imprimir todos");
        btnBuscarMovimientos.setText("Buscar");

        inicializarComboTipos();
        inicializarTabla();
        initListeners();

        // defaults: hoy
        Date hoy = new Date();
        jdcFechaDesdeFiltro.setDate(hoy);
        jdcFechaHastaFiltro.setDate(hoy);

        // totales
        resetTotales();
    }

    private void initListeners() {
        btnBuscarMovimientos.addActionListener(e -> buscarMovimientos());

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

    private void inicializarComboTipos() {
        jcbTipoMovimiento.removeAllItems();
        jcbTipoMovimiento.addItem("TODOS");
        for (TipoMovimiento t : TipoMovimiento.values()) {
            jcbTipoMovimiento.addItem(t.name());
        }
        jcbTipoMovimiento.setSelectedIndex(0);
    }

    private void inicializarTabla() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Fecha", "Tipo", "Monto", "Usuario", "Método", "Recibo", "Descripción"}
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        tableCajaMovimientos.setModel(model);
        tableCajaMovimientos.getTableHeader().setReorderingAllowed(false);
        limpiarTabla();
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
            txtVentas.setText(formatMoney(BigDecimal.ZERO));
        }
        if (txtCompras != null) {
            txtCompras.setText(formatMoney(BigDecimal.ZERO));
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

            movimientosActuales = cajaMovimientoControlador.obtenerMovimientosFiltrados(tipo, desde, hasta);
            cargarEnTabla(movimientosActuales);

            if (movimientosActuales == null || movimientosActuales.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron movimientos con los filtros seleccionados.", "Resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showException("Error al buscar movimientos", ex);
        }
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
            return TipoMovimiento.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void cargarEnTabla(List<CajaMovimiento> movimientos) {
        limpiarTabla();
        DefaultTableModel model = (DefaultTableModel) tableCajaMovimientos.getModel();
        if (movimientos == null) {
            return;
        }

        BigDecimal totalCreditos = BigDecimal.ZERO;
        BigDecimal totalDebitos = BigDecimal.ZERO;

        for (CajaMovimiento m : movimientos) {
            String fecha = "-";
            fecha = formatFecha(m.getFecha());
            String tipo = m.getTipoMovimiento() != null ? m.getTipoMovimiento().name() : "-";
            String monto = formatMoney(m.getMonto());

            Usuario u = m.getUsuario();
            String usuario = nombreUsuario(u);

            MetodoPago mp = m.getMetodoPago();
            String metodo = mp != null ? safe(mp.getNombre()) : "N/A";

            Recibo r = m.getRecibo();
            String recibo = (r != null && r.getIdRecibo() != null) ? ("#" + r.getIdRecibo()) : "N/A";

            String desc = safe(m.getDescripcion());

            model.addRow(new Object[]{m.getIdMovimiento(), fecha, tipo, monto, usuario, metodo, recibo, desc});

            // totales (según TipoMovimiento)
            TipoMovimiento tm = m.getTipoMovimiento();
            BigDecimal mv = m.getMonto() != null ? m.getMonto() : BigDecimal.ZERO;
            if (tm != null) {
                String t = tm.name();
                if (t.contains("CREDITO")) {
                    totalCreditos = totalCreditos.add(mv);
                }
                if (t.contains("DEBITO")) {
                    totalDebitos = totalDebitos.add(mv);
                }
            }
        }

        setTotales(totalCreditos, totalDebitos);
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
            txtVentas.setText(formatMoney(totalCreditos != null ? totalCreditos : BigDecimal.ZERO));
        }
        if (txtCompras != null) {
            txtCompras.setText(formatMoney(totalDebitos != null ? totalDebitos : BigDecimal.ZERO));
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
        int row = tableCajaMovimientos.getSelectedRow();
        if (row < 0) {
            return null;
        }

        Object idObj = tableCajaMovimientos.getValueAt(row, 0);
        if (idObj == null) {
            return null;
        }

        Long id = null;
        try {
            id = (idObj instanceof Long) ? (Long) idObj : Long.valueOf(idObj.toString());
        } catch (Exception e) {
            return null;
        }

        if (movimientosActuales != null) {
            for (CajaMovimiento m : movimientosActuales) {
                if (m != null && m.getIdMovimiento() != null && m.getIdMovimiento().equals(id)) {
                    return m;
                }
            }
        }
        return null;
    }

    private void imprimirMovimientoSeleccionado() {
        try {
            CajaMovimiento mov = getMovimientoSeleccionado();
            if (mov == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un movimiento en la tabla.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("movimiento", mov);

            reporteService.generar(ReporteTipo.CAJA_MOVIMIENTO, req);
        } catch (Exception ex) {
            showException("Error al imprimir", ex);
        }
    }

    private void imprimirListaMovimientos() {
        try {
            if (tableCajaMovimientos == null || tableCajaMovimientos.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay movimientos para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String filtros = buildFiltroTexto();

            ReporteRequest req = new ReporteRequest()
                    .put("tabla", tableCajaMovimientos)
                    .put("filtros", filtros);

            reporteService.generar(ReporteTipo.CAJA_MOVIMIENTOS_LISTADO, req);
        } catch (Exception ex) {
            showException("Error al imprimir", ex);
        }
    }

    private String buildFiltroTexto() {
        String tipo = (jcbTipoMovimiento.getSelectedItem() != null ? jcbTipoMovimiento.getSelectedItem().toString() : "TODOS");
        String desde = formatFecha(jdcFechaDesdeFiltro.getDate());
        String hasta = formatFecha(jdcFechaHastaFiltro.getDate());
        return "Filtros: Tipo=" + tipo + " | Desde=" + desde + " | Hasta=" + hasta;
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
