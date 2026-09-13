package veterinaria.vista;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.CuentaCorrienteControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.persistencia.CuentaCorrienteMovimientoDAO;
import veterinaria.util.ManejoTablas;
import veterinaria.util.SesionUsuario;
import veterinaria.vista.application.Application;
import veterinaria.util.PermisoUI;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.Date;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;

public class FormCuentaCorrienteMovimientos extends javax.swing.JPanel {

    private final CuentaCorrienteControlador operarCuentaCorriente = new CuentaCorrienteControlador();
    private final ClienteControlador operarClientes = new ClienteControlador();
    private final CuentaCorrienteMovimientoDAO operarCCMovimientos = new CuentaCorrienteMovimientoDAO();
    private final ManejoTablas operarTabla = new ManejoTablas();
    private SesionUsuario sesion = Application.getSesionUsuario();

    // --- Reportes (PDF por código) ---
    // 🚀 REEMPLAZÁ TU LÍNEA POR ESTA:
    private final ReporteService reporteService = ReporteService.getInstance();

    // --- Control de alertas de límite (evita spam) ---
    private Integer ultimaCuentaAlertada = null;
    private boolean ultimoEstadoExcedeLimite = false;

    public FormCuentaCorrienteMovimientos() {
        initComponents();
        configurarRendererMonto();
        PermisoUI.aplicar(this);
        configurarCampoSaldo();
        configurarCampoLimite();
        ocultarColumnaSaldoResultante();
        jpMovimientos.setVisible(true);
        jpCuentaCorriente.setVisible(true);
        btnFiltrar.setEnabled(false);
        // Evitar congelamientos: carga inicial y recalculado masivo fuera del EDT.
        cargarInicialAsync();
        inicializarEventos();
    }

    private void setBusy(boolean busy) {
        try {
            setCursor(busy ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
                    : java.awt.Cursor.getDefaultCursor());
        } catch (Exception ignored) {
        }
    }

    /**
     * Carga inicial (recalcular saldos + cargar tabla de CC) fuera del hilo UI.
     */
    private void cargarInicialAsync() {
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                // OJO: este método puede ser pesado si hay muchas cuentas.
                // Al menos lo sacamos del EDT para que no congele la UI.
                operarCuentaCorriente.actualizarTodosLosSaldosCC();
                return null;
            }

            @Override
            protected void done() {
                try {
                    cargarTodasLasCuentasCorrientesEnTablaAsync();
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void cargarTodasLasCuentasCorrientesEnTablaAsync() {
        setBusy(true);
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<CuentaCorriente> cuentasDeClientes = operarCuentaCorriente.buscarTodasLasCuentasCorrientes();
                java.util.ArrayList<Object[]> filas = new java.util.ArrayList<>();
                for (CuentaCorriente cc : cuentasDeClientes) {
                    Cliente cliente = operarClientes.buscarPorId(cc.getCliente().getIdCliente());
                    Object nombreCliente = (cliente != null && cliente.getPersona() != null)
                            ? (cliente.getPersona().getNombre() + " " + cliente.getPersona().getApellido())
                            : "";
                    CuentaCorrienteMovimiento ultimoDebito = operarCCMovimientos.obtenerUltimoDebito(cc.getIdCuentaCorriente());
                    CuentaCorrienteMovimiento ultimoCredito = operarCCMovimientos.obtenerUltimoCredito(cc.getIdCuentaCorriente());
                    Object fechaUltimoDebito = (ultimoDebito != null) ? ultimoDebito.getFechaMovimiento() : null;
                    Object fechaUltimoCredito = (ultimoCredito != null) ? ultimoCredito.getFechaMovimiento() : null;

                    filas.add(new Object[]{
                        false,
                        cc.getIdCuentaCorriente(),
                        nombreCliente,
                        cc.getSaldoActual(),
                        fechaUltimoDebito,
                        fechaUltimoCredito,
                        cc.getEstado(),});
                }
                return filas;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> filas;
                    try {
                        filas = get();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(FormCuentaCorrienteMovimientos.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    DefaultTableModel modelo = (DefaultTableModel) tableCCClientes.getModel();
                    modelo.setRowCount(0);
                    for (Object[] f : filas) {
                        modelo.addRow(f);
                    }
                    operarTabla.asegurarSeleccionUnica(tableCCClientes);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * Configura el campo de saldo total de la cuenta corriente. Se muestra en
     * verde si está a favor (>= 0) o rojo si está en deuda (< 0).
     */
    private void configurarCampoSaldo() {
        txtSaldoCC.setEditable(false);
        txtSaldoCC.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtSaldoCC.setFont(txtSaldoCC.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        txtSaldoCC.setText(formatearMoneda(BigDecimal.ZERO));
        txtSaldoCC.setForeground(new Color(0, 140, 0));
    }

    /**
     * Configura el campo de límite de la cuenta corriente (solo lectura).
     */
    private void configurarCampoLimite() {
        txtLimiteCC.setEditable(false);
        txtLimiteCC.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtLimiteCC.setFont(txtLimiteCC.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        txtLimiteCC.setText(formatearMoneda(BigDecimal.ZERO));
        txtLimiteCC.setForeground(new Color(80, 80, 80));
    }

    /**
     * Oculta la columna "Saldo Resultante" de la tabla de movimientos. Dejamos
     * de mostrar la evolución por fila para evitar inconsistencias; el saldo
     * total se muestra en txtSaldoCC (fuente de verdad: SUM créditos - SUM
     * débitos).
     */
    private void ocultarColumnaSaldoResultante() {
        // La columna "Saldo Resultante" es la última (índice 6) según el DefaultTableModel.
        try {
            int colIndex = 6;
            if (tableMovimientosCC.getColumnModel().getColumnCount() > colIndex) {
                var col = tableMovimientosCC.getColumnModel().getColumn(colIndex);
                col.setMinWidth(0);
                col.setMaxWidth(0);
                col.setPreferredWidth(0);
            }
        } catch (Exception ignored) {
            // Si por algún motivo cambia el modelo/índices, preferimos no romper la vista.
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return nf.format(valor);
    }

    /**
     * Actualiza el campo txtSaldoCC y devuelve el saldo calculado. Fuente de
     * verdad: SUM(CREDITO) - SUM(DEBITO)
     */
    private BigDecimal actualizarSaldoCC(Integer idCuentaCorriente) {
        BigDecimal saldo = operarCCMovimientos.obtenerSaldoActualSumando(idCuentaCorriente);
        if (saldo == null) {
            saldo = BigDecimal.ZERO;
        }
        txtSaldoCC.setText(formatearMoneda(saldo));
        if (saldo.signum() < 0) {
            txtSaldoCC.setForeground(new Color(180, 0, 0)); // deuda -> rojo
        } else {
            txtSaldoCC.setForeground(new Color(0, 140, 0)); // a favor -> verde
        }
        return saldo;
    }

    private BigDecimal obtenerLimiteCredito(Integer idCuentaCorriente) {
        CuentaCorriente cuenta = operarCuentaCorriente.buscarCuentaCorrientePorId(idCuentaCorriente);
        if (cuenta == null || cuenta.getLimiteCredito() == null) {
            return BigDecimal.ZERO;
        }
        return cuenta.getLimiteCredito();
    }

    private void actualizarLimiteCC(Integer idCuentaCorriente) {
        BigDecimal limite = obtenerLimiteCredito(idCuentaCorriente);
        txtLimiteCC.setText(formatearMoneda(limite));
    }

    /**
     * Muestra alerta si la deuda supera el límite. Regla: si saldo < 0 y abs(saldo)
     * > limiteCredito => excede.
     */
    private void verificarYAlertarLimite(Integer idCuentaCorriente, BigDecimal saldoActual) {
        BigDecimal limite = obtenerLimiteCredito(idCuentaCorriente);
        boolean excede = false;
        if (saldoActual != null && saldoActual.signum() < 0) {
            BigDecimal deuda = saldoActual.abs();
            excede = deuda.compareTo(limite) > 0;
        }

        // Mostrar solo si excede y (cambio de cuenta) o (antes no excedía)
        boolean mismaCuenta = (ultimaCuentaAlertada != null && ultimaCuentaAlertada.equals(idCuentaCorriente));
        if (excede && (!mismaCuenta || !ultimoEstadoExcedeLimite)) {
            JOptionPane.showMessageDialog(
                    this,
                    "⚠ La deuda de la cuenta corriente supera el límite de crédito.\n\n"
                    + "Saldo: " + formatearMoneda(saldoActual) + "\n"
                    + "Límite: " + formatearMoneda(limite),
                    "Límite excedido",
                    JOptionPane.WARNING_MESSAGE
            );
        }

        ultimaCuentaAlertada = idCuentaCorriente;
        ultimoEstadoExcedeLimite = excede;
    }

    /**
     * Renderer para pintar la columna "Monto" según el tipo de movimiento. -
     * Débito: rojo y con signo "-" - Crédito: verde, en negrita y con signo "+"
     * Además formatea el valor como moneda AR.
     */
    private class MontoCCRenderer extends DefaultTableCellRenderer {

        private final int columnaTipoMovimiento;

        public MontoCCRenderer(int columnaTipoMovimiento) {
            this.columnaTipoMovimiento = columnaTipoMovimiento;
            setHorizontalAlignment(RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Restaurar estilo base
            setFont(table.getFont());

            String tipo = "";
            try {
                Object v = table.getValueAt(row, columnaTipoMovimiento);
                tipo = (v != null) ? v.toString().trim().toUpperCase() : "";
            } catch (Exception ignored) {
            }

            BigDecimal monto = BigDecimal.ZERO;
            try {
                if (value instanceof BigDecimal) {
                    monto = (BigDecimal) value;
                } else if (value != null) {
                    monto = new BigDecimal(value.toString());
                }
            } catch (Exception ignored) {
                monto = BigDecimal.ZERO;
            }

            boolean esDebito = tipo.contains("DEBIT");
            boolean esCredito = tipo.contains("CREDIT");

            String signo = esCredito ? "+" : (esDebito ? "-" : "");
            String montoTxt = formatearMoneda(monto.abs());
            setText((signo.isEmpty() ? "" : (signo + " ")) + montoTxt);

            if (isSelected) {
                // Mantener colores del Look&Feel cuando está seleccionado
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
                if (esDebito) {
                    setForeground(new Color(180, 0, 0));
                } else if (esCredito) {
                    setForeground(new Color(0, 140, 0));
                    setFont(table.getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(table.getForeground());
                }
            }

            return this;
        }
    }

    /**
     * Aplica renderer a la columna "Monto" para colorear/formatear según
     * Débito/Crédito.
     */
    private void configurarRendererMonto() {
        try {
            int colMonto = tableMovimientosCC.getColumnModel().getColumnIndex("Monto");
            int colTipo = tableMovimientosCC.getColumnModel().getColumnIndex("Tipo de Transacción");
            tableMovimientosCC.getColumnModel().getColumn(colMonto).setCellRenderer(new MontoCCRenderer(colTipo));
        } catch (Exception ignored) {
            // Si cambian los nombres/índices de columnas no rompemos la vista.
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpBuscar = new javax.swing.JPanel();
        lbMovimientoCC = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbUsuarioBusqueda = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusqueda = new javax.swing.JTextField();
        jpCuentaCorriente = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scroll = new javax.swing.JScrollPane();
        tableCCClientes = new javax.swing.JTable();
        btnImprimirCC = new javax.swing.JButton();
        jpMovimientos = new javax.swing.JPanel();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        scroll1 = new javax.swing.JScrollPane();
        tableMovimientosCC = new veterinaria.vista.table.AutoTable();
        lbDesde = new javax.swing.JLabel();
        jdcDesde = new com.toedter.calendar.JDateChooser();
        lbHasta = new javax.swing.JLabel();
        jdcHasta = new com.toedter.calendar.JDateChooser();
        btnFiltrar = new javax.swing.JButton();
        jcbTipoMovimiento = new javax.swing.JComboBox<>();
        lbTipoMovimiento = new javax.swing.JLabel();
        btnImprimirMovimientos = new javax.swing.JButton();
        jpBotonesInferior = new javax.swing.JPanel();
        lbSaldoCC = new javax.swing.JLabel();
        txtSaldoCC = new javax.swing.JTextField();
        lbLimiteCC = new javax.swing.JLabel();
        txtLimiteCC = new javax.swing.JTextField();

        lbMovimientoCC.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbMovimientoCC.setText("Movimiento de Cuenta Corriente");

        lbUsuarioBusqueda.setText("Buscar cuenta corriente");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        javax.swing.GroupLayout jpBuscarLayout = new javax.swing.GroupLayout(jpBuscar);
        jpBuscar.setLayout(jpBuscarLayout);
        jpBuscarLayout.setHorizontalGroup(
            jpBuscarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpBuscarLayout.createSequentialGroup()
                .addGroup(jpBuscarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpBuscarLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1))
                    .addGroup(jpBuscarLayout.createSequentialGroup()
                        .addGroup(jpBuscarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpBuscarLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(lbMovimientoCC))
                            .addGroup(jpBuscarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jpBuscarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbUsuarioBusqueda)
                                    .addGroup(jpBuscarLayout.createSequentialGroup()
                                        .addComponent(lbBuscar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpBuscarLayout.setVerticalGroup(
            jpBuscarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpBuscarLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(lbMovimientoCC)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE)
                .addComponent(lbUsuarioBusqueda)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpBuscarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbBuscar))
                .addContainerGap())
        );

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Lista de Cuentas Corrientes");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableCCClientes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Núm. de Cuenta", "Cliente", "Saldo", "Ultimo Consumo", "Ultimo Pago", "Estado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableCCClientes.setMinimumSize(new java.awt.Dimension(848, 220));
        tableCCClientes.setPreferredSize(new java.awt.Dimension(848, 220));
        tableCCClientes.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(tableCCClientes);

        btnImprimirCC.setText("Imprimir Cuentas");

        javax.swing.GroupLayout jpCuentaCorrienteLayout = new javax.swing.GroupLayout(jpCuentaCorriente);
        jpCuentaCorriente.setLayout(jpCuentaCorrienteLayout);
        jpCuentaCorrienteLayout.setHorizontalGroup(
            jpCuentaCorrienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCuentaCorrienteLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpCuentaCorrienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(jpCuentaCorrienteLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimirCC))
                    .addComponent(jSeparator3)
                    .addComponent(scroll, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jpCuentaCorrienteLayout.setVerticalGroup(
            jpCuentaCorrienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCuentaCorrienteLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpCuentaCorrienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(btnImprimirCC))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE))
        );

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Lista de Movimientos");

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableMovimientosCC.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Movimiento", "Descripción", "Fecha", "Tipo de Transacción", "Monto", "Saldo Resultante"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableMovimientosCC.setMinimumSize(new java.awt.Dimension(848, 220));
        tableMovimientosCC.setPreferredSize(new java.awt.Dimension(848, 220));
        tableMovimientosCC.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableMovimientosCC);

        lbDesde.setText("Desde:");

        lbHasta.setText("Hasta:");

        btnFiltrar.setText("Filtrar");
        btnFiltrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFiltrarActionPerformed(evt);
            }
        });

        jcbTipoMovimiento.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Debito", "Credito" }));

        lbTipoMovimiento.setText("Tipo:");

        btnImprimirMovimientos.setText("Imprimir Movimientos");

        javax.swing.GroupLayout jpMovimientosLayout = new javax.swing.GroupLayout(jpMovimientos);
        jpMovimientos.setLayout(jpMovimientosLayout);
        jpMovimientosLayout.setHorizontalGroup(
            jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpMovimientosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpMovimientosLayout.createSequentialGroup()
                        .addGroup(jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator5, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator4)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jpMovimientosLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpMovimientosLayout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(jpMovimientosLayout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(lbDesde)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jdcDesde, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lbHasta)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jdcHasta, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lbTipoMovimiento)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jcbTipoMovimiento, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnFiltrar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnImprimirMovimientos)))))
                        .addContainerGap())
                    .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 832, Short.MAX_VALUE)))
        );
        jpMovimientosLayout.setVerticalGroup(
            jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpMovimientosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addComponent(jLabel3)
                .addGap(4, 4, 4)
                .addGroup(jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jdcHasta, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbDesde, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jdcDesde, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbHasta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpMovimientosLayout.createSequentialGroup()
                        .addGroup(jpMovimientosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbTipoMovimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbTipoMovimiento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnFiltrar)
                            .addComponent(btnImprimirMovimientos))
                        .addGap(5, 5, 5)))
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addContainerGap())
        );

        lbSaldoCC.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbSaldoCC.setText("SALDO:");

        lbLimiteCC.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbLimiteCC.setText("LIMITE:");

        javax.swing.GroupLayout jpBotonesInferiorLayout = new javax.swing.GroupLayout(jpBotonesInferior);
        jpBotonesInferior.setLayout(jpBotonesInferiorLayout);
        jpBotonesInferiorLayout.setHorizontalGroup(
            jpBotonesInferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpBotonesInferiorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbSaldoCC)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtSaldoCC, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lbLimiteCC)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtLimiteCC, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jpBotonesInferiorLayout.setVerticalGroup(
            jpBotonesInferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpBotonesInferiorLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpBotonesInferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSaldoCC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbSaldoCC)
                    .addComponent(lbLimiteCC)
                    .addComponent(txtLimiteCC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jpBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpCuentaCorriente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpMovimientos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpBotonesInferior, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpCuentaCorriente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpMovimientos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpBotonesInferior, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnFiltrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFiltrarActionPerformed
        // Evitar warnings al intentar seleccionar: validamos solo al ejecutar la acción.
        if (tableCCClientes.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this,
                    "¡Debe seleccionar una Cuenta Corriente!",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            btnFiltrar.setEnabled(false);
            return;
        }
        cargarMovimientosDeClienteAsync();
    }//GEN-LAST:event_btnFiltrarActionPerformed

    private void btnRecalcularSaldoActionPerformed(java.awt.event.ActionEvent evt) {
        recalcularSaldoCuentaSeleccionadaAsync();
    }

    private void inicializarEventos() {

        // En Swing, el MouseListener puede dispararse antes de que la selección se actualice
        // (especialmente en el primer click). Por eso usamos el modelo de selección.
        tableCCClientes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableCCClientes.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int selectedRow = tableCCClientes.getSelectedRow();
            if (selectedRow != -1) {
                cargarMovimientosDeClienteAsync();
                btnFiltrar.setEnabled(true);
            } else {
                btnFiltrar.setEnabled(false);
            }
        });

        btnImprimirMovimientos.addActionListener(evt -> {
            imprimirMovimientosCuentaCorrienteSeleccionada();
        });

        btnImprimirCC.addActionListener(evt -> {
            imprimirListadoCuentasCorrientes();
        });
    }

    private void imprimirListadoCuentasCorrientes() {
        try {
            if (tableCCClientes.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay cuentas corrientes para imprimir.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("tabla", tableCCClientes)
                    .put("usuario", (sesion != null && sesion.getUsuario() != null) ? sesion.getUsuario().getPersona().getNombre() + " " + sesion.getUsuario().getPersona().getApellido() : "")
                    .put("rol", (sesion != null && sesion.getUsuario() != null && sesion.getUsuario().getRol() != null) ? sesion.getUsuario().getRol().getNombreRol() : "");

            reporteService.generar(ReporteTipo.CC_LISTA_CUENTAS, req);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirMovimientosCuentaCorrienteSeleccionada() {
        try {
            int filaSeleccionada = tableCCClientes.getSelectedRow();
            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una Cuenta Corriente para imprimir sus movimientos.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Integer idCuentaCorriente = (Integer) tableCCClientes.getValueAt(filaSeleccionada, 1);
            String nombreCliente = String.valueOf(tableCCClientes.getValueAt(filaSeleccionada, 2));

            if (tableMovimientosCC.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "La cuenta corriente seleccionada no tiene movimientos para imprimir.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date fechaDesde = jdcDesde.getDate();
            Date fechaHasta = jdcHasta.getDate();
            String tipo = (String) jcbTipoMovimiento.getSelectedItem();

            String filtros = "";
            if (fechaDesde != null) {
                filtros += "Desde: " + fechaDesde + "  ";
            }
            if (fechaHasta != null) {
                filtros += "Hasta: " + fechaHasta + "  ";
            }
            if (tipo != null && !tipo.trim().isEmpty()) {
                filtros += "Tipo: " + tipo;
            }

            ReporteRequest req = new ReporteRequest()
                    .put("idCuentaCorriente", idCuentaCorriente)
                    .put("cliente", nombreCliente)
                    .put("cuenta", "CC #" + idCuentaCorriente)
                    .put("filtros", filtros)
                    .put("tabla", tableMovimientosCC)
                    .put("saldo", (txtSaldoCC != null) ? txtSaldoCC.getText() : "")
                    .put("limite", (txtLimiteCC != null) ? txtLimiteCC.getText() : "")
                    .put("usuario", (sesion != null && sesion.getUsuario() != null) ? sesion.getUsuario().getPersona().getNombre() + " " + sesion.getUsuario().getPersona().getApellido() : "")
                    .put("rol", (sesion != null && sesion.getUsuario() != null && sesion.getUsuario().getRol() != null) ? sesion.getUsuario().getRol().getNombreRol() : "");

            reporteService.generar(ReporteTipo.CC_MOVIMIENTOS, req);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalcularSaldoCuentaSeleccionada() {
        int filaSeleccionada = tableCCClientes.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una Cuenta Corriente para recalcular el saldo.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Integer idCuentaCorriente = (Integer) tableCCClientes.getValueAt(filaSeleccionada, 1);
        CuentaCorriente cuenta = operarCuentaCorriente.buscarCuentaCorrientePorId(idCuentaCorriente);
        if (cuenta == null) {
            JOptionPane.showMessageDialog(this, "No se encontró la cuenta corriente seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (operarCuentaCorriente.actualizarSaldoActualCC(cuenta)) {
            JOptionPane.showMessageDialog(this, "Saldo recalculado y actualizado.", "OK", JOptionPane.INFORMATION_MESSAGE);
            cargarTodasLasCuentasCorrientesEnTablaAsync();
            cargarMovimientosDeClienteAsync();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo recalcular el saldo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalcularSaldoCuentaSeleccionadaAsync() {
        int filaSeleccionada = tableCCClientes.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una Cuenta Corriente para recalcular el saldo.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Integer idCuentaCorriente = (Integer) tableCCClientes.getValueAt(filaSeleccionada, 1);
        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                CuentaCorriente cuenta = operarCuentaCorriente.buscarCuentaCorrientePorId(idCuentaCorriente);
                if (cuenta == null) {
                    return false;
                }
                return operarCuentaCorriente.actualizarSaldoActualCC(cuenta);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = false;
                    try {
                        ok = get();
                    } catch (Exception ignored) {
                        ok = false;
                    }
                    if (ok) {
                        JOptionPane.showMessageDialog(FormCuentaCorrienteMovimientos.this, "Saldo recalculado y actualizado.", "OK", JOptionPane.INFORMATION_MESSAGE);
                        cargarTodasLasCuentasCorrientesEnTablaAsync();
                        cargarMovimientosDeClienteAsync();
                    } else {
                        JOptionPane.showMessageDialog(FormCuentaCorrienteMovimientos.this, "No se pudo recalcular el saldo.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void cargarTodasLasCuentasCorrientesEnTabla() {
        DefaultTableModel modelo = (DefaultTableModel) tableCCClientes.getModel();
        modelo.setRowCount(0);

        List<CuentaCorriente> cuentasDeClientes = operarCuentaCorriente.buscarTodasLasCuentasCorrientes();
        Cliente cliente = new Cliente();

        for (CuentaCorriente cuentaCorrienteBucle : cuentasDeClientes) {
            cliente = operarClientes.buscarPorId(cuentaCorrienteBucle.getCliente().getIdCliente());
            Object nombreCliente = cliente.getPersona().getNombre() + " " + cliente.getPersona().getApellido();
            CuentaCorrienteMovimiento ultimoDebito = operarCCMovimientos.obtenerUltimoDebito(cuentaCorrienteBucle.getIdCuentaCorriente());
            CuentaCorrienteMovimiento ultimoCredito = operarCCMovimientos.obtenerUltimoCredito(cuentaCorrienteBucle.getIdCuentaCorriente());
            Object fechaUltimoDebito = (ultimoDebito != null) ? ultimoDebito.getFechaMovimiento() : null;
            Object fechaUltimoCredito = (ultimoCredito != null) ? ultimoCredito.getFechaMovimiento() : null;
            Object[] fila = new Object[]{
                false,
                cuentaCorrienteBucle.getIdCuentaCorriente(),
                nombreCliente,
                cuentaCorrienteBucle.getSaldoActual(),
                fechaUltimoDebito,
                fechaUltimoCredito,
                cuentaCorrienteBucle.getEstado(),};
            modelo.addRow(fila);
        }
        operarTabla.asegurarSeleccionUnica(tableCCClientes);
    }

    private void cargarMovimientosDeCliente() {
        int filaSeleccionada = tableCCClientes.getSelectedRow();
        if (filaSeleccionada != -1) {
            Integer idCuentaCorriente = (Integer) tableCCClientes.getValueAt(filaSeleccionada, 1);

            // Obtener las fechas de jdcDesde y jdcHasta
            java.util.Date fechaDesde = jdcDesde.getDate();
            java.util.Date fechaHasta = jdcHasta.getDate();

            // Convertir las fechas a java.sql.Date si es necesario
            java.sql.Date sqlFechaDesde = (fechaDesde != null) ? new java.sql.Date(fechaDesde.getTime()) : null;
            java.sql.Date sqlFechaHasta = (fechaHasta != null) ? new java.sql.Date(fechaHasta.getTime()) : null;

            // Obtener los movimientos del cliente en el rango de fechas
            String tipo = (String) jcbTipoMovimiento.getSelectedItem();
            List<CuentaCorrienteMovimiento> movimientos = operarCCMovimientos.obtenerMovimientosEnRango(idCuentaCorriente, sqlFechaDesde, sqlFechaHasta, tipo);

            // Actualizar la tabla de movimientos
            DefaultTableModel modeloMovimientos = (DefaultTableModel) tableMovimientosCC.getModel();
            modeloMovimientos.setRowCount(0); // Limpiar las filas existentes

            for (CuentaCorrienteMovimiento movimiento : movimientos) {
                Object[] fila = new Object[]{
                    false,
                    movimiento.getIdMovimiento(),
                    movimiento.getDescripcion(),
                    movimiento.getFechaMovimiento(),
                    movimiento.getTipoMovimiento(),
                    movimiento.getMonto(),
                    "", // saldo por fila oculto; el saldo total se calcula con SUM(CREDITO) - SUM(DEBITO)
                };
                modeloMovimientos.addRow(fila);
            }

            // Actualizar saldo total + límite y alertar si excede
            BigDecimal saldoTotal = actualizarSaldoCC(idCuentaCorriente);
            actualizarLimiteCC(idCuentaCorriente);
            verificarYAlertarLimite(idCuentaCorriente, saldoTotal);
        } else {
            // Sin selección: dejamos el saldo en cero
            txtSaldoCC.setText(formatearMoneda(BigDecimal.ZERO));
            txtSaldoCC.setForeground(new Color(0, 140, 0));
            txtLimiteCC.setText(formatearMoneda(BigDecimal.ZERO));
            txtLimiteCC.setForeground(new Color(80, 80, 80));

            // Reset del estado de alerta
            ultimaCuentaAlertada = null;
            ultimoEstadoExcedeLimite = false;
        }
    }

    /**
     * Carga movimientos + saldo fuera del EDT para evitar congelamientos.
     */
    private void cargarMovimientosDeClienteAsync() {
        int filaSeleccionada = tableCCClientes.getSelectedRow();
        if (filaSeleccionada == -1) {
            // Sin selección: dejamos el saldo en cero
            txtSaldoCC.setText(formatearMoneda(BigDecimal.ZERO));
            txtSaldoCC.setForeground(new Color(0, 140, 0));
            txtLimiteCC.setText(formatearMoneda(BigDecimal.ZERO));
            txtLimiteCC.setForeground(new Color(80, 80, 80));
            ultimaCuentaAlertada = null;
            ultimoEstadoExcedeLimite = false;
            return;
        }

        Integer idCuentaCorriente = (Integer) tableCCClientes.getValueAt(filaSeleccionada, 1);
        java.util.Date fechaDesde = jdcDesde.getDate();
        java.util.Date fechaHasta = jdcHasta.getDate();
        java.sql.Date sqlFechaDesde = (fechaDesde != null) ? new java.sql.Date(fechaDesde.getTime()) : null;
        java.sql.Date sqlFechaHasta = (fechaHasta != null) ? new java.sql.Date(fechaHasta.getTime()) : null;
        String tipo = (String) jcbTipoMovimiento.getSelectedItem();

        setBusy(true);
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                List<CuentaCorrienteMovimiento> movimientos = operarCCMovimientos.obtenerMovimientosEnRango(idCuentaCorriente, sqlFechaDesde, sqlFechaHasta, tipo);
                BigDecimal saldoTotal = operarCCMovimientos.obtenerSaldoActualSumando(idCuentaCorriente);
                BigDecimal limite = obtenerLimiteCredito(idCuentaCorriente);
                return new Object[]{movimientos, saldoTotal, limite};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] res;
                    try {
                        res = get();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(FormCuentaCorrienteMovimientos.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<CuentaCorrienteMovimiento> movimientos = (List<CuentaCorrienteMovimiento>) res[0];
                    BigDecimal saldoTotal = (BigDecimal) res[1];
                    BigDecimal limite = (BigDecimal) res[2];

                    DefaultTableModel modeloMovimientos = (DefaultTableModel) tableMovimientosCC.getModel();
                    modeloMovimientos.setRowCount(0);
                    for (CuentaCorrienteMovimiento movimiento : movimientos) {
                        Object[] fila = new Object[]{
                            false,
                            movimiento.getIdMovimiento(),
                            movimiento.getDescripcion(),
                            movimiento.getFechaMovimiento(),
                            movimiento.getTipoMovimiento(),
                            movimiento.getMonto(),
                            "",};
                        modeloMovimientos.addRow(fila);
                    }

                    // Pintar saldo + límite
                    if (saldoTotal == null) {
                        saldoTotal = BigDecimal.ZERO;
                    }
                    txtSaldoCC.setText(formatearMoneda(saldoTotal));
                    txtSaldoCC.setForeground(saldoTotal.signum() < 0 ? new Color(180, 0, 0) : new Color(0, 140, 0));

                    if (limite == null) {
                        limite = BigDecimal.ZERO;
                    }
                    txtLimiteCC.setText(formatearMoneda(limite));
                    txtLimiteCC.setForeground(new Color(80, 80, 80));

                    verificarYAlertarLimite(idCuentaCorriente, saldoTotal);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFiltrar;
    private javax.swing.JButton btnImprimirCC;
    private javax.swing.JButton btnImprimirMovimientos;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JComboBox<String> jcbTipoMovimiento;
    private com.toedter.calendar.JDateChooser jdcDesde;
    private com.toedter.calendar.JDateChooser jdcHasta;
    private javax.swing.JPanel jpBotonesInferior;
    private javax.swing.JPanel jpBuscar;
    private javax.swing.JPanel jpCuentaCorriente;
    private javax.swing.JPanel jpMovimientos;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbDesde;
    private javax.swing.JLabel lbHasta;
    private javax.swing.JLabel lbLimiteCC;
    private javax.swing.JLabel lbMovimientoCC;
    private javax.swing.JLabel lbSaldoCC;
    private javax.swing.JLabel lbTipoMovimiento;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JTable tableCCClientes;
    private javax.swing.JTable tableMovimientosCC;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextField txtLimiteCC;
    private javax.swing.JTextField txtSaldoCC;
    // End of variables declaration//GEN-END:variables
}
