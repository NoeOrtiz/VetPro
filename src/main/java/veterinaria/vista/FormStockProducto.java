package veterinaria.vista;

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import veterinaria.controlador.ProveedorControlador;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.StockProductoInfo;
import veterinaria.entidad.util.ProveedorItem;
import veterinaria.servicio.StockProductoService;
import veterinaria.util.PermisoUI;
import veterinaria.vista.application.Application;

public class FormStockProducto extends javax.swing.JPanel {

    private final StockProductoService stockService = new StockProductoService();
    private final ProveedorControlador proveedorControlador = new ProveedorControlador();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private TableRowSorter<DefaultTableModel> sorter;

    public FormStockProducto() {
        initComponents();
        PermisoUI.aplicar(this);
        inicializarTabla();

        initListeners();
        configurarCamposSoloLectura();
        cargarCombos();
        cargarTabla();

    }

    private void inicializarTabla() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Código", "Producto", "Categoría", "Proveedor", "Precio Compra", "Precio Venta", "Stock Actual", "Stock Mínimo", "Estado", "Valor en Stock (Costo)"}
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        tableStockProducto.setModel(model);
        tableStockProducto.getTableHeader().setReorderingAllowed(false);
        tableStockProducto.setRowHeight(26);

        // Ocultar ID
        if (tableStockProducto.getColumnModel().getColumnCount() > 0) {
            tableStockProducto.getColumnModel().getColumn(0).setMinWidth(0);
            tableStockProducto.getColumnModel().getColumn(0).setMaxWidth(0);
            tableStockProducto.getColumnModel().getColumn(0).setPreferredWidth(0);
        }

        sorter = new TableRowSorter<>(model);
        tableStockProducto.setRowSorter(sorter);

        // Render estado con colores (por nombre de columna para evitar fallos si cambia el orden)
        aplicarRendererEstado();
        limpiarTabla();
    }

    /**
     * Aplica el renderer de "Estado" detectando la columna por nombre. Esto
     * evita depender de índices fijos (que se rompen si se reordenan/ajustan
     * columnas).
     */
    private void aplicarRendererEstado() {
        int estadoViewIndex = -1;
        try {
            // Búsqueda robusta por nombre visible del encabezado
            for (int i = 0; i < tableStockProducto.getColumnModel().getColumnCount(); i++) {
                Object header = tableStockProducto.getColumnModel().getColumn(i).getHeaderValue();
                if (header != null && "Estado".equalsIgnoreCase(header.toString().trim())) {
                    estadoViewIndex = i;
                    break;
                }
            }
        } catch (Exception ex) {
            estadoViewIndex = -1;
        }

        if (estadoViewIndex >= 0) {
            tableStockProducto.getColumnModel().getColumn(estadoViewIndex).setCellRenderer(new EstadoRenderer());
        }
    }

    private void limpiarTabla() {
        ((DefaultTableModel) tableStockProducto.getModel()).setRowCount(0);
    }

    private void abrirDetalleProductoSeleccionado() {
        int viewRow = tableStockProducto.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto en la tabla.");
            return;
        }
        int row = tableStockProducto.convertRowIndexToModel(viewRow);

        DefaultTableModel m = (DefaultTableModel) tableStockProducto.getModel();
        Object codigo = m.getValueAt(row, 1);
        Object producto = m.getValueAt(row, 2);
        Object rubro = m.getValueAt(row, 3);
        Object prov = m.getValueAt(row, 4);
        Object stock = m.getValueAt(row, 7);
        Object minimo = m.getValueAt(row, 8);
        Object estado = m.getValueAt(row, 9);
        Object valor = m.getValueAt(row, 10);

        String msg = "Código: " + codigo
                + "\nProducto: " + producto
                + "\nCategoría: " + rubro
                + "\nProveedor: " + prov
                + "\nStock actual: " + stock
                + "\nStock mínimo (efectivo): " + minimo
                + "\nEstado: " + estado
                + "\nValor en stock (costo): " + valor;

        JOptionPane.showMessageDialog(this, msg, "Detalle de Stock", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initListeners() {
        btnBuscar.addActionListener(e -> cargarTabla());
        btnLimpiar.addActionListener(e -> limpiarFiltros());
        btnVer.addActionListener(e -> abrirDetalleProductoSeleccionado());

        txtBuscar.addActionListener(e -> cargarTabla());
    }

    private void configurarCamposSoloLectura() {
        txtTotalProductosListado.setEditable(false);
        txtTotalProductosListado.setFocusable(false);
        txtTotalUnidadesEnStock.setEditable(false);
        txtTotalUnidadesEnStock.setFocusable(false);
        txtValorTotalInventario.setEditable(false);
        txtValorTotalInventario.setFocusable(false);
        txtCantidadProductosStockMinimo.setEditable(false);
        txtCantidadProductosStockMinimo.setFocusable(false);
        txtCantidadProductosSinStock.setEditable(false);
        txtCantidadProductosSinStock.setFocusable(false);
    }

    private ReporteRequest construirRequestProductoSeleccionado() {
        int viewRow = tableStockProducto.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int row = tableStockProducto.convertRowIndexToModel(viewRow);

        DefaultTableModel m = (DefaultTableModel) tableStockProducto.getModel();

        return new ReporteRequest()
                .put("idProducto", m.getValueAt(row, 0))
                .put("codigo", nvl(m.getValueAt(row, 1)))
                .put("producto", nvl(m.getValueAt(row, 2)))
                .put("categoria", nvl(m.getValueAt(row, 3)))
                .put("proveedor", nvl(m.getValueAt(row, 4)))
                .put("precioCompra", nvl(m.getValueAt(row, 5)))
                .put("precioVenta", nvl(m.getValueAt(row, 6)))
                .put("stockActual", nvl(m.getValueAt(row, 7)))
                .put("stockMinimo", nvl(m.getValueAt(row, 8)))
                .put("estado", nvl(m.getValueAt(row, 9)))
                .put("valorEnStock", nvl(m.getValueAt(row, 10)))
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());
    }

    private String construirResumenFiltros() {
        StringBuilder sb = new StringBuilder();

        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim() : "";
        String categoria = (jcbCategoria.getSelectedItem() != null) ? String.valueOf(jcbCategoria.getSelectedItem()).trim() : "Todos";
        Object provSel = jcbProveedor.getSelectedItem();
        String proveedor = (provSel != null) ? String.valueOf(provSel).trim() : "Todos";
        String estado = (jcbEstadoStock.getSelectedItem() != null) ? String.valueOf(jcbEstadoStock.getSelectedItem()).trim() : "Todos";

        sb.append("Búsqueda: ").append(texto.isEmpty() ? "(sin filtro)" : texto);
        sb.append("\nCategoría: ").append((categoria.isEmpty() || "Todos".equalsIgnoreCase(categoria)) ? "Todas" : categoria);
        sb.append("\nProveedor: ").append((proveedor.isEmpty() || proveedor.startsWith("Todos")) ? "Todos" : proveedor);
        sb.append("\nEstado: ").append((estado.isEmpty() || "Todos".equalsIgnoreCase(estado)) ? "Todos" : estado);

        return sb.toString();
    }

    private void imprimirDetalleProductoSeleccionado() {
        ReporteRequest req = construirRequestProductoSeleccionado();
        if (req == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto para imprimir.", "Impresión", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 🚀 AL EJECUTOR: Corre en segundo plano y muestra la barra animada de carga
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.STOCK_PRODUCTO_REGISTRO, req);
    }

    private void imprimirTablaStock() {
        if (tableStockProducto.getRowCount() <= 0) {
            JOptionPane.showMessageDialog(this, "No hay datos en la tabla para imprimir.", "Impresión", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Armamos los parámetros con toda la info de los totales del stock
        ReporteRequest req = new ReporteRequest()
                .put("tabla", tableStockProducto)
                .put("filtros", construirResumenFiltros())
                .put("totalProductos", txtTotalProductosListado.getText())
                .put("totalUnidades", txtTotalUnidadesEnStock.getText())
                .put("valorInventario", txtValorTotalInventario.getText())
                .put("cantidadStockMinimo", txtCantidadProductosStockMinimo.getText())
                .put("cantidadSinStock", txtCantidadProductosSinStock.getText())
                .put("emisor", Application.getNombreApellidoUsuarioLogeado());

        // 2. 🚀 AL EJECUTOR: Limpio, seguro y asíncrono
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.STOCK_PRODUCTO_LISTADO, req);
    }

    private void cargarCombos() {
        StringBuilder advertencias = new StringBuilder();

        // Categorías (Rubros)
        DefaultComboBoxModel<String> catModel = new DefaultComboBoxModel<>();
        catModel.addElement("Todos");
        try {
            List<String> rubros = stockService.listarRubros();
            if (rubros != null) {
                for (String r : rubros) {
                    if (r != null && !r.trim().isEmpty()) {
                        catModel.addElement(r.trim());
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error cargando rubros en FormStockProducto: " + ex.getMessage());
            advertencias.append("- No se pudieron cargar las categorías.\n");
        }
        jcbCategoria.setModel(catModel);

        // Proveedores
        DefaultComboBoxModel<ProveedorItem> provModel = new DefaultComboBoxModel<>();
        provModel.addElement(new ProveedorItem(null, "Todos"));
        try {
            List<Proveedor> proveedores = proveedorControlador.buscarTodosLosProveedores();
            if (proveedores != null) {
                for (Proveedor p : proveedores) {
                    String nombre = (p.getRazonSocial() != null && !p.getRazonSocial().trim().isEmpty())
                            ? p.getRazonSocial()
                            : (p.getPersona() != null
                            ? (safe(p.getPersona().getNombre()) + " " + safe(p.getPersona().getApellido())).trim()
                            : "Proveedor #" + p.getIdProveedor());
                    provModel.addElement(new ProveedorItem(p.getIdProveedor(), nombre));
                }
            }
        } catch (Exception ex) {
            System.err.println("Error cargando proveedores en FormStockProducto: " + ex.getMessage());
            advertencias.append("- No se pudieron cargar los proveedores.\n");
        }
        jcbProveedor.setModel(provModel);

        if (advertencias.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "El formulario se abrió con filtros parciales.\n\n" + advertencias.toString(),
                    "Advertencia de carga",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cargarTabla() {
        limpiarTabla();

        String q = txtBuscar.getText();
        String rubro = (String) jcbCategoria.getSelectedItem();
        ProveedorItem provItem = (ProveedorItem) jcbProveedor.getSelectedItem();
        Integer idProveedor = (provItem != null) ? provItem.getIdProveedor() : null;

        StockProductoInfo.EstadoStock estado = null;
        String est = (String) jcbEstadoStock.getSelectedItem();
        if (est != null) {
            if ("Sin Stock".equalsIgnoreCase(est) || "Sin stock".equalsIgnoreCase(est)) {
                estado = StockProductoInfo.EstadoStock.SIN_STOCK;
            } else if ("Stock normal".equalsIgnoreCase(est) || "Con stock normal".equalsIgnoreCase(est)
                    || "Con Stock".equalsIgnoreCase(est) || "Con stock".equalsIgnoreCase(est)) {
                estado = StockProductoInfo.EstadoStock.NORMAL;
            } else if ("Bajo mínimo".equalsIgnoreCase(est) || "Bajo minimo".equalsIgnoreCase(est)) {
                estado = StockProductoInfo.EstadoStock.BAJO;
            }
        }

        if (rubro != null && "Todos".equalsIgnoreCase(rubro.trim())) {
            rubro = null;
        }
        if (idProveedor != null && idProveedor <= 0) {
            idProveedor = null;
        }

        List<StockProductoInfo> lista;
        try {
            lista = stockService.buscar(q, rubro, idProveedor, estado);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar stock: " + ex.getMessage(), "Stock", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (lista == null) {
            lista = java.util.Collections.emptyList();
        }

        int totalProductos = 0;
        int totalUnidades = 0;
        int sinStock = 0;
        int bajoMinimo = 0;
        BigDecimal valorInventario = BigDecimal.ZERO;

        DefaultTableModel m = (DefaultTableModel) tableStockProducto.getModel();
        for (StockProductoInfo s : lista) {
            totalProductos++;
            int stockActual = (s.getStockActual() == null) ? 0 : s.getStockActual();
            int stockMinimo = (s.getStockMinimo() == null) ? 0 : s.getStockMinimo();
            totalUnidades += stockActual;
            StockProductoInfo.EstadoStock estCalc = s.getEstado();
            if (estCalc == StockProductoInfo.EstadoStock.SIN_STOCK) {
                sinStock++;
            } else if (estCalc == StockProductoInfo.EstadoStock.BAJO) {
                bajoMinimo++;
            }
            valorInventario = valorInventario.add(s.getValorEnStock());

            m.addRow(new Object[]{
                s.getIdProducto(),
                nvl(s.getCodigo()),
                nvl(s.getProducto()),
                nvl(s.getRubro()),
                nvl(s.getProveedor()),
                money(s.getPrecioCompra()),
                money(s.getPrecioVenta()),
                stockActual,
                stockMinimo,
                estadoAmigable(estCalc),
                money(s.getValorEnStock())
            });
        }

        txtTotalProductosListado.setText(String.valueOf(totalProductos));
        txtTotalUnidadesEnStock.setText(String.valueOf(totalUnidades));
        txtValorTotalInventario.setText(df.format(valorInventario));
        txtCantidadProductosSinStock.setText(String.valueOf(sinStock));
        txtCantidadProductosStockMinimo.setText(String.valueOf(bajoMinimo));
    }

    private void limpiarFiltros() {
        txtBuscar.setText("");
        if (jcbCategoria.getItemCount() > 0) {
            jcbCategoria.setSelectedIndex(0);
        }
        if (jcbProveedor.getItemCount() > 0) {
            jcbProveedor.setSelectedIndex(0);
        }
        if (jcbEstadoStock.getItemCount() > 0) {
            jcbEstadoStock.setSelectedIndex(0);
        }
        cargarTabla();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String nvl(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String money(BigDecimal v) {
        if (v == null) {
            return "0.00";
        }
        return df.format(v);
    }

    private String estadoAmigable(StockProductoInfo.EstadoStock estado) {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case SIN_STOCK:
                return "SIN STOCK";
            case BAJO:
                return "STOCK BAJO";
            case NORMAL:
            default:
                return "STOCK NORMAL";
        }
    }

    /**
     * Render de estado para resaltar SIN_STOCK/BAJO/NORMAL.
     */
    private static class EstadoRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // Asegura que se pinten background/foreground
            if (c instanceof javax.swing.JComponent) {
                ((javax.swing.JComponent) c).setOpaque(true);
            }

            if (isSelected) {
                // Respetar colores del Look&Feel en selección
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
                return c;
            }

            String v = value != null ? value.toString().trim() : "";
            if ("SIN_STOCK".equalsIgnoreCase(v) || "SIN STOCK".equalsIgnoreCase(v)) {
                c.setBackground(new Color(255, 230, 230));
                c.setForeground(new Color(120, 0, 0)); // rojo oscuro para contraste
            } else if ("BAJO".equalsIgnoreCase(v) || "STOCK BAJO".equalsIgnoreCase(v)) {
                c.setBackground(new Color(255, 250, 210));
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(new Color(230, 255, 230));
                c.setForeground(new Color(0, 90, 0)); // verde oscuro para contraste
            }
            return c;
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbInformeDeStockDePRoductos = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbBuscarText = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        lbCategoria = new javax.swing.JLabel();
        jcbCategoria = new javax.swing.JComboBox<>();
        lbProveedor = new javax.swing.JLabel();
        jcbProveedor = new javax.swing.JComboBox();
        lbEstadoStock = new javax.swing.JLabel();
        jcbEstadoStock = new javax.swing.JComboBox<>();
        btnImprimir = new javax.swing.JButton();
        btnVer = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jpListaProductos = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        lbListaDeProductosStock = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scroll1 = new javax.swing.JScrollPane();
        tableStockProducto = new veterinaria.vista.table.AutoTable();
        btnImprimirLista = new javax.swing.JButton();
        jpTotales = new javax.swing.JPanel();
        lbTotalProductosListado = new javax.swing.JLabel();
        txtTotalProductosListado = new javax.swing.JTextField();
        lbTotalUnidadesEnStock = new javax.swing.JLabel();
        txtTotalUnidadesEnStock = new javax.swing.JTextField();
        lbValorTotalInventario = new javax.swing.JLabel();
        txtValorTotalInventario = new javax.swing.JTextField();
        lbCantidadProductosStockMinimo = new javax.swing.JLabel();
        txtCantidadProductosStockMinimo = new javax.swing.JTextField();
        lbCantidadProductosSinStock = new javax.swing.JLabel();
        txtCantidadProductosSinStock = new javax.swing.JTextField();

        lbInformeDeStockDePRoductos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbInformeDeStockDePRoductos.setText("Informe de Stock de Productos");

        lbBuscarText.setText("BUSCAR:");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        lbCategoria.setText("Categoría:");

        jcbCategoria.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Categoría" }));

        lbProveedor.setText("Proveedor:");

        jcbProveedor.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Seleccionar Proveedor" }));

        lbEstadoStock.setText("Estado Stock:");

        jcbEstadoStock.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Stock normal", "Sin Stock", "Bajo mínimo" }));

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        btnVer.setText("Ver ");

        btnBuscar.setText("Buscar");

        btnLimpiar.setText("Limpiar");

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(lbInformeDeStockDePRoductos)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbBuscarText)
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addComponent(lbBuscar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jcbCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbCategoria))
                                .addGap(18, 18, 18)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbProveedor)
                                    .addComponent(jcbProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbEstadoStock)
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addComponent(jcbEstadoStock, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(btnBuscar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnLimpiar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnVer)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnImprimir))))
                            .addComponent(jSeparator1))))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbInformeDeStockDePRoductos)
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addComponent(lbBuscarText)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(3, 3, 3))
                            .addComponent(lbBuscar, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(lbCategoria)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbEstadoStock))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnImprimir)
                                .addComponent(btnVer))
                            .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jcbProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jcbEstadoStock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnBuscar)
                                .addComponent(btnLimpiar)))))
                .addContainerGap())
        );

        lbListaDeProductosStock.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDeProductosStock.setText("Lista de Productos & Stock");

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableStockProducto.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Código", "Producto", "Categoría", "Proveedor", "Precio Compra", "Precio Venta", "Stock Actual", "Stock Mínimo", "Estado", "Valor Potencial Stock"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableStockProducto.setMaximumSize(null);
        tableStockProducto.setMinimumSize(new java.awt.Dimension(0, 0));
        tableStockProducto.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableStockProducto);

        btnImprimirLista.setText("Imprimir Lista");
        btnImprimirLista.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirListaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpListaProductosLayout = new javax.swing.GroupLayout(jpListaProductos);
        jpListaProductos.setLayout(jpListaProductosLayout);
        jpListaProductosLayout.setHorizontalGroup(
            jpListaProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaProductosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpListaProductosLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lbListaDeProductosStock)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimirLista))
                    .addComponent(jSeparator3)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addComponent(scroll1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jpListaProductosLayout.setVerticalGroup(
            jpListaProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaProductosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpListaProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListaDeProductosStock)
                    .addComponent(btnImprimirLista))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE))
        );

        lbTotalProductosListado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbTotalProductosListado.setText("Total Productos Listados:");

        lbTotalUnidadesEnStock.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbTotalUnidadesEnStock.setText("Total Unidades en Stock:");

        lbValorTotalInventario.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbValorTotalInventario.setText("Valor Inventario Total: $");

        lbCantidadProductosStockMinimo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbCantidadProductosStockMinimo.setText("Cantidad de Productos Stock Mínimo:");

        lbCantidadProductosSinStock.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbCantidadProductosSinStock.setText("Cantidad de Productos Sin Stock:");

        javax.swing.GroupLayout jpTotalesLayout = new javax.swing.GroupLayout(jpTotales);
        jpTotales.setLayout(jpTotalesLayout);
        jpTotalesLayout.setHorizontalGroup(
            jpTotalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpTotalesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpTotalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpTotalesLayout.createSequentialGroup()
                        .addComponent(lbTotalProductosListado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTotalProductosListado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lbTotalUnidadesEnStock)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTotalUnidadesEnStock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbValorTotalInventario)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtValorTotalInventario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpTotalesLayout.createSequentialGroup()
                        .addComponent(lbCantidadProductosStockMinimo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCantidadProductosStockMinimo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbCantidadProductosSinStock)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCantidadProductosSinStock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jpTotalesLayout.setVerticalGroup(
            jpTotalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpTotalesLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpTotalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbTotalProductosListado)
                    .addComponent(txtTotalProductosListado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbTotalUnidadesEnStock)
                    .addComponent(txtTotalUnidadesEnStock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbValorTotalInventario)
                    .addComponent(txtValorTotalInventario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpTotalesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbCantidadProductosStockMinimo)
                    .addComponent(txtCantidadProductosStockMinimo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbCantidadProductosSinStock)
                    .addComponent(txtCantidadProductosSinStock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpListaProductos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpTotales, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaProductos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpTotales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        imprimirDetalleProductoSeleccionado();
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void btnImprimirListaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirListaActionPerformed
        imprimirTablaStock();
    }//GEN-LAST:event_btnImprimirListaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnVer;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JComboBox<String> jcbCategoria;
    private javax.swing.JComboBox<String> jcbEstadoStock;
    private javax.swing.JComboBox jcbProveedor;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaProductos;
    private javax.swing.JPanel jpTotales;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbBuscarText;
    private javax.swing.JLabel lbCantidadProductosSinStock;
    private javax.swing.JLabel lbCantidadProductosStockMinimo;
    private javax.swing.JLabel lbCategoria;
    private javax.swing.JLabel lbEstadoStock;
    private javax.swing.JLabel lbInformeDeStockDePRoductos;
    private javax.swing.JLabel lbListaDeProductosStock;
    private javax.swing.JLabel lbProveedor;
    private javax.swing.JLabel lbTotalProductosListado;
    private javax.swing.JLabel lbTotalUnidadesEnStock;
    private javax.swing.JLabel lbValorTotalInventario;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JTable tableStockProducto;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtCantidadProductosSinStock;
    private javax.swing.JTextField txtCantidadProductosStockMinimo;
    private javax.swing.JTextField txtTotalProductosListado;
    private javax.swing.JTextField txtTotalUnidadesEnStock;
    private javax.swing.JTextField txtValorTotalInventario;
    // End of variables declaration//GEN-END:variables
}
