package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import veterinaria.controlador.CompraOrdenControlador;
import veterinaria.controlador.CompraRecepcionControlador;
import veterinaria.entidad.CompraOrden;
import veterinaria.entidad.CompraOrdenEstado;
import veterinaria.entidad.CompraOrdenItem;
import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.CompraRecepcionEstado;
import veterinaria.entidad.CompraRecepcionItem;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Usuario;
import veterinaria.util.PermisoUI;
import veterinaria.util.SesionUsuario;
import veterinaria.vista.application.Application;
import veterinaria.vista.componentes.AnularRecepcionDialog;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteTipo;

public class FormRecibirPedido extends javax.swing.JPanel {

    // --- Controladores ---
    private final CompraOrdenControlador ordenCtrl = new CompraOrdenControlador();
    private final CompraRecepcionControlador recepCtrl = new CompraRecepcionControlador();

    // --- Modelos ---
    private DefaultTableModel modelOrdenes;
    private DefaultTableModel modelPendientes;
    private DefaultTableModel modelRecepciones;
    private DefaultTableModel modelRecepcionDetalle;

    // --- Selección / cache ---
    private Integer idOrdenSeleccionada = null;
    private CompraOrden ordenSeleccionada = null;

    private Long idRecepcionSeleccionada = null;
    private final Map<Long, CompraRecepcion> recepcionesById = new HashMap<>();

    public FormRecibirPedido() {
        initComponents();
        jspOrdenesConfirmadas.setBorder(new TitledBorder("Órdenes CONFIRMADAS"));
        jspPendientePorProducto.setBorder(new TitledBorder("Pendiente por Producto"));
        jspHistorialDeRecepciones.setBorder(new TitledBorder("Historial de Recepciones (Remitos)"));
        jspDetalleRecepcion.setBorder(new TitledBorder("Detalle de Recepción"));

        // Ajustar título (en el diseñador quedó como "Visitas")
        lbVisitas.setText("Recibir Pedidos (Ingreso por OC)");

        PermisoUI.aplicar(this);

        configurarTablas();
        initListeners();
        cargarOrdenesConfirmadas();
    }

    // =========================
    //  Inicialización UI/Lógica
    // =========================
    private void configurarTablas() {
        // Órdenes confirmadas
        modelOrdenes = new DefaultTableModel(new Object[]{"ID", "Nro", "Fecha", "Proveedor", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableOrdenesConfirmadas.setModel(modelOrdenes);
        tableOrdenesConfirmadas.setRowHeight(24);
        ocultarColumna(tableOrdenesConfirmadas, 0);

        // Pendientes
        modelPendientes = new DefaultTableModel(new Object[]{
            "IDProd", "Categoría", "Producto", "Pedida", "Recibida", "Pendiente", "Recibir ahora"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 6;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3 || columnIndex == 4 || columnIndex == 5 || columnIndex == 6) {
                    return Integer.class;
                }
                return String.class;
            }
        };
        tablePendientePorProducto.setModel(modelPendientes);
        tablePendientePorProducto.setRowHeight(24);
        ocultarColumna(tablePendientePorProducto, 0);

        // Historial
        modelRecepciones = new DefaultTableModel(new Object[]{"ID", "Fecha", "Remito", "Factura", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableHistorialDeRecepciones.setModel(modelRecepciones);
        tableHistorialDeRecepciones.setRowHeight(22);
        ocultarColumna(tableHistorialDeRecepciones, 0);

        // Detalle
        modelRecepcionDetalle = new DefaultTableModel(new Object[]{"Categoría", "Producto", "Cantidad", "Costo Unit."}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Integer.class;
                }
                return String.class;
            }
        };
        tableDetalleRecepcion.setModel(modelRecepcionDetalle);
        tableDetalleRecepcion.setRowHeight(22);

        // Estado inicial
        btnVer.setEnabled(false);
        btnAnular.setEnabled(false);
        btnRegistrarRecepcion.setEnabled(false);
    }

    private void ocultarColumna(JTable table, int colIdx) {
        try {
            TableColumn col = table.getColumnModel().getColumn(colIdx);
            col.setMinWidth(0);
            col.setMaxWidth(0);
            col.setPreferredWidth(0);
        } catch (Exception ex) {
            // ignore
        }
    }

    private void initListeners() {
        btnBuscar.addActionListener(e -> cargarOrdenesConfirmadas());

        tableOrdenesConfirmadas.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                onOrdenSeleccionada();
            }
        });

        tableOrdenesConfirmadas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = tableOrdenesConfirmadas.rowAtPoint(e.getPoint());
                if (r >= 0) {
                    tableOrdenesConfirmadas.setRowSelectionInterval(r, r);
                }
            }
        });

        tableHistorialDeRecepciones.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                onRecepcionSeleccionada();
            }
        });

        tableHistorialDeRecepciones.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = tableHistorialDeRecepciones.rowAtPoint(e.getPoint());
                if (r >= 0) {
                    tableHistorialDeRecepciones.setRowSelectionInterval(r, r);
                }
            }
        });

        modelPendientes.addTableModelListener(e -> validarExcesoEnPendientes());

        btnRegistrarRecepcion.addActionListener(e -> registrarRecepcion());
        btnVer.addActionListener(e -> verRecepcionSeleccionada());
        btnAnular.addActionListener(e -> anularRecepcionSeleccionada());
    }

    // =========================
    //  Lógica (portada del form viejo)
    // =========================
    private void cargarOrdenesConfirmadas() {
        // limpiar
        modelOrdenes.setRowCount(0);
        modelPendientes.setRowCount(0);
        modelRecepciones.setRowCount(0);
        modelRecepcionDetalle.setRowCount(0);
        recepcionesById.clear();
        idRecepcionSeleccionada = null;
        btnVer.setEnabled(false);
        btnAnular.setEnabled(false);
        btnRegistrarRecepcion.setEnabled(false);

        ordenSeleccionada = null;
        idOrdenSeleccionada = null;

        String filtro = (txtBusqueda == null) ? null : txtBusqueda.getText();

        var ordenes = ordenCtrl.buscarTodos(filtro, CompraOrdenEstado.CONFIRMADA.name());
        if (ordenes != null) {
            for (CompraOrden o : ordenes) {
                modelOrdenes.addRow(new Object[]{
                    o.getIdCompraOrden(),
                    o.getNumeroOrden(),
                    o.getFechaPedido(),
                    (o.getProveedor() != null ? o.getProveedor().getRazonSocial() : ""),
                    (o.getEstado() != null ? o.getEstado().name() : "")
                });
            }
        }
    }

    private void onOrdenSeleccionada() {
        int row = tableOrdenesConfirmadas.getSelectedRow();
        if (row < 0) {
            return;
        }

        Integer id = (Integer) modelOrdenes.getValueAt(row, 0);
        if (id == null) {
            return;
        }

        CompraOrden o = ordenCtrl.buscarPorId(id);
        if (o == null) {
            return;
        }

        if (o.getEstado() != CompraOrdenEstado.CONFIRMADA) {
            JOptionPane.showMessageDialog(this, "Solo se puede recepcionar una orden CONFIRMADA.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        idOrdenSeleccionada = id;
        ordenSeleccionada = o;

        cargarPendientesDeOrden(o);
        cargarHistorialRecepciones(id);
        validarExcesoEnPendientes();

        btnRegistrarRecepcion.setEnabled(modelPendientes.getRowCount() > 0 && isPendientesValidos());
    }

    private void cargarPendientesDeOrden(CompraOrden orden) {
        modelPendientes.setRowCount(0);

        Map<Integer, Integer> recibido = recepCtrl.sumarRecibidoPorOrden(orden.getIdCompraOrden());
        if (recibido == null) {
            recibido = new HashMap<>();
        }

        for (CompraOrdenItem oi : orden.getItems()) {
            if (oi == null || oi.getProducto() == null) {
                continue;
            }

            Producto p = oi.getProducto();
            int pedida = (oi.getCantidad() == null) ? 0 : oi.getCantidad();
            int rec = recibido.getOrDefault(p.getIdProducto(), 0);
            int pend = pedida - rec;
            if (pend < 0) {
                pend = 0;
            }

            modelPendientes.addRow(new Object[]{
                p.getIdProducto(),
                (p.getRubro() != null ? p.getRubro() : ""),
                safe(p.getNombre()),
                pedida,
                rec,
                pend,
                0
            });
        }
    }

    private void registrarRecepcion() {
        if (ordenSeleccionada == null || idOrdenSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una orden.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar cantidades "recibir ahora"
        boolean tieneAlgo = false;
        for (int i = 0; i < modelPendientes.getRowCount(); i++) {
            Integer pend = (Integer) modelPendientes.getValueAt(i, 5);
            Integer recAhora = parseInt(modelPendientes.getValueAt(i, 6));
            if (recAhora != null && recAhora > 0) {
                tieneAlgo = true;
                if (pend != null && recAhora > pend) {
                    JOptionPane.showMessageDialog(this,
                            "La cantidad a recibir no puede superar lo pendiente. Si llegó mercadería de más, apartá el excedente para devolver al proveedor. Producto: " + modelPendientes.getValueAt(i, 2),
                            "Validación",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        if (!tieneAlgo) {
            JOptionPane.showMessageDialog(this, "Ingrese al menos una cantidad en 'Recibir ahora'.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // El nuevo diseño no tiene campos visibles para remito/factura, así que los pedimos al registrar.
        String remito = JOptionPane.showInputDialog(this, "N° Remito (opcional):", "Remito", JOptionPane.QUESTION_MESSAGE);
        if (remito == null) {
            return;
        }
        String factura = JOptionPane.showInputDialog(this, "N° Factura (opcional):", "Factura", JOptionPane.QUESTION_MESSAGE);
        if (factura == null) {
            return;
        }

        int r = JOptionPane.showConfirmDialog(
                this,
                "Se registrará la recepción y se actualizará el stock. ¿Confirmar?",
                "Confirmar recepción",
                JOptionPane.YES_NO_OPTION
        );
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        CompraRecepcion rec = new CompraRecepcion();
        rec.setFecha(new Date());
        rec.setOrden(ordenSeleccionada);
        rec.setNumeroRemito(remito);
        rec.setNumeroFactura(factura);

        for (int i = 0; i < modelPendientes.getRowCount(); i++) {
            Integer recAhora = parseInt(modelPendientes.getValueAt(i, 6));
            if (recAhora != null && recAhora > 0) {
                Integer idProd = (Integer) modelPendientes.getValueAt(i, 0);
                CompraRecepcionItem it = new CompraRecepcionItem();
                Producto p = new Producto();
                p.setIdProducto(idProd);
                it.setProducto(p);
                it.setCantidadRecibida(recAhora);
                rec.getItems().add(it);
            }
        }

        Usuario usuario = getUsuarioSesion();

        boolean ok = recepCtrl.crear(rec, usuario);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo registrar la recepción. Revise logs.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Recepción registrada correctamente.", "OK", JOptionPane.INFORMATION_MESSAGE);

        Integer keepId = idOrdenSeleccionada;
        cargarOrdenesConfirmadas();
        if (keepId != null) {
            seleccionarOrdenEnTabla(keepId);
        }
    }

    private void cargarHistorialRecepciones(Integer idCompraOrden) {
        modelRecepciones.setRowCount(0);
        modelRecepcionDetalle.setRowCount(0);
        recepcionesById.clear();

        if (idCompraOrden == null) {
            return;
        }

        var receps = recepCtrl.buscarPorOrden(idCompraOrden);
        if (receps == null) {
            return;
        }

        for (CompraRecepcion r : receps) {
            Long id = r.getIdCompraRecepcion();
            recepcionesById.put(id, r);
            modelRecepciones.addRow(new Object[]{
                id,
                r.getFecha(),
                safe(r.getNumeroRemito()),
                safe(r.getNumeroFactura()),
                (r.getEstado() != null ? r.getEstado().name() : "")
            });
        }

        if (modelRecepciones.getRowCount() > 0) {
            tableHistorialDeRecepciones.setRowSelectionInterval(0, 0);
        }
    }

    private void onRecepcionSeleccionada() {
        int row = tableHistorialDeRecepciones.getSelectedRow();
        if (row < 0) {
            return;
        }

        Object idObj = modelRecepciones.getValueAt(row, 0);
        if (idObj == null) {
            return;
        }

        Long id = (idObj instanceof Long) ? (Long) idObj : Long.valueOf(idObj.toString());
        idRecepcionSeleccionada = id;
        CompraRecepcion r = recepcionesById.get(id);
        if (r == null) {
            r = recepCtrl.buscarPorId(id);
        }
        if (r == null) {
            return;
        }

        btnVer.setEnabled(true);
        boolean puedeAnular = (r.getEstado() == null) || (r.getEstado() == CompraRecepcionEstado.CONFIRMADA);
        btnAnular.setEnabled(puedeAnular);

        modelRecepcionDetalle.setRowCount(0);
        if (r.getItems() == null) {
            return;
        }

        for (CompraRecepcionItem it : r.getItems()) {
            String cat = "";
            String prod = "";
            if (it.getProducto() != null) {
                prod = safe(it.getProducto().getNombre());
                if (it.getProducto().getRubro() != null) {
                    cat = safe(it.getProducto().getRubro());
                }
            }
            modelRecepcionDetalle.addRow(new Object[]{
                cat,
                prod,
                it.getCantidadRecibida(),
                (it.getPrecioCostoUnitario() != null ? it.getPrecioCostoUnitario().toString() : "")
            });
        }
    }

    private void verRecepcionSeleccionada() {
        if (idRecepcionSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una recepción del historial.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CompraRecepcion r = recepCtrl.buscarPorId(idRecepcionSeleccionada);
        if (r == null) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar la recepción.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mostrarDialogoRecepcion(r);
    }

    private void anularRecepcionSeleccionada() {
        if (idRecepcionSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una recepción del historial.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CompraRecepcion r = recepCtrl.buscarPorId(idRecepcionSeleccionada);
        if (r == null) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar la recepción.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (r.getEstado() == CompraRecepcionEstado.ANULADA) {
            JOptionPane.showMessageDialog(this, "La recepción ya está ANULADA.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String info = "Recepción ID: " + r.getIdCompraRecepcion()
                + " | OC: " + (r.getOrden() != null ? safe(r.getOrden().getNumeroOrden()) : "")
                + " | Remito: " + safe(r.getNumeroRemito());
        String usuarioLinea = "Usuario: " + safe(SesionUsuario.getInstancia().getNombreApellido());
        String motivo = AnularRecepcionDialog.pedirMotivo(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                info,
                usuarioLinea
        );
        if (motivo == null) {
            return;
        }
        if (motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe indicar el motivo de anulación.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int c = JOptionPane.showConfirmDialog(
                this,
                "Se ANULARÁ la recepción y se revertirá el stock (movimiento inverso).\n\nMotivo: " + motivo + "\n\n¿Confirmar?",
                "Anular recepción",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (c != JOptionPane.YES_OPTION) {
            return;
        }

        Usuario usuario = getUsuarioSesion();

        boolean ok = recepCtrl.anular(idRecepcionSeleccionada, usuario, motivo);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo anular la recepción. Revise logs.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Recepción anulada correctamente.", "OK", JOptionPane.INFORMATION_MESSAGE);

        Integer keepOrden = idOrdenSeleccionada;
        cargarOrdenesConfirmadas();
        if (keepOrden != null) {
            seleccionarOrdenEnTabla(keepOrden);
        }
    }

    private void mostrarDialogoRecepcion(CompraRecepcion r) {
        JDialog d = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(this), "Recepción de Compra", JDialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout(10, 10));

        String prov = "";
        String oc = "";
        if (r.getOrden() != null) {
            oc = safe(r.getOrden().getNumeroOrden());
            if (r.getOrden().getProveedor() != null) {
                prov = safe(r.getOrden().getProveedor().getRazonSocial());
            }
        }

        String texto = String.format(
                "Recepción ID: %s | Estado: %s | OC: %s | Proveedor: %s | Remito: %s | Factura: %s",
                r.getIdCompraRecepcion(),
                r.getEstado() != null ? r.getEstado().name() : "",
                oc,
                prov,
                safe(r.getNumeroRemito()),
                safe(r.getNumeroFactura())
        );

        JLabel head = new JLabel(texto);
        head.setBorder(new TitledBorder("Comprobante"));
        JPanel pnHead = new JPanel(new BorderLayout(5, 5));
        pnHead.add(head, BorderLayout.NORTH);

        if (r.getEstado() == CompraRecepcionEstado.ANULADA) {
            String mot = safe(r.getMotivoAnulacion());
            String fa = (r.getFechaAnulacion() != null) ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(r.getFechaAnulacion()) : "";
            String anulador = "";
            if (r.getUsuarioAnulacion() != null) {
                if (r.getUsuarioAnulacion().getPersona() != null) {
                    anulador = safe(r.getUsuarioAnulacion().getPersona().getNombre()) + " " + safe(r.getUsuarioAnulacion().getPersona().getApellido());
                    anulador = anulador.trim();
                }
                if (anulador.isBlank()) {
                    anulador = safe(r.getUsuarioAnulacion().getNombreUsuario());
                }
            }

            String extra = (fa.isBlank() ? "" : (" | Fecha: " + fa))
                    + (anulador.isBlank() ? "" : (" | Usuario: " + anulador));
            JLabel lmot = new JLabel("Motivo anulación: " + mot + extra);
            lmot.setBorder(new TitledBorder("Anulación"));
            pnHead.add(lmot, BorderLayout.CENTER);
        }

        d.add(pnHead, BorderLayout.NORTH);

        DefaultTableModel m = new DefaultTableModel(new Object[]{"Categoría", "Producto", "Cantidad", "Costo Unit."}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 2) ? Integer.class : String.class;
            }
        };
        JTable t = new JTable(m);
        t.setRowHeight(22);

        if (r.getItems() != null) {
            for (CompraRecepcionItem it : r.getItems()) {
                String cat = "";
                String prodN = "";
                if (it.getProducto() != null) {
                    Producto p = it.getProducto();
                    prodN = safe(p.getNombre());
                    if (p.getRubro() != null) {
                        cat = safe(p.getRubro());
                    }
                }
                m.addRow(new Object[]{cat, prodN, it.getCantidadRecibida(), (it.getPrecioCostoUnitario() != null ? it.getPrecioCostoUnitario().toString() : "")});
            }
        }

        d.add(new JScrollPane(t), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPdf = new JButton("Generar PDF");
        JButton btnCerrar = new JButton("Cerrar");
        actions.add(btnPdf);
        actions.add(btnCerrar);
        d.add(actions, BorderLayout.SOUTH);

        btnPdf.addActionListener(e -> generarPdfRecepcion(r));
        btnCerrar.addActionListener(e -> d.dispose());

        d.setSize(900, 520);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void generarPdfRecepcion(CompraRecepcion r) {
        // 1. Armamos los parámetros del reporte (manteniendo tu lógica de usuario)
        ReporteRequest req = new ReporteRequest();
        req.put("recepcion", r);

        Usuario u = getUsuarioSesion();
        if (u != null && u.getPersona() != null) {
            req.put("usuario", safe(u.getPersona().getApellido() + " " + u.getPersona().getNombre()));
            req.put("rol", (u.getRol() != null ? safe(u.getRol().toString()) : ""));
        }

        // 2. 🚀 AL EJECUTOR: Limpio, asíncrono y alineado al Singleton
        veterinaria.reportes.pdf.ReporteEjecutor.ejecutarConAviso(this, ReporteTipo.COMPRA_RECEPCION, req);
    }

    private Usuario getUsuarioSesion() {
        try {
            var su = Application.getSesionUsuario();
            return (su != null) ? su.getUsuario() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void validarExcesoEnPendientes() {
        boolean hayExceso = false;
        for (int i = 0; i < modelPendientes.getRowCount(); i++) {
            Integer pend = (Integer) modelPendientes.getValueAt(i, 5);
            Integer recAhora = parseInt(modelPendientes.getValueAt(i, 6));
            if (pend != null && recAhora != null && recAhora > pend) {
                hayExceso = true;
                break;
            }
        }

        if (hayExceso) {
            jspPendientePorProducto.setBorder(new TitledBorder("⚠ Pendiente por Producto (hay excedente)"));
        } else {
            jspPendientePorProducto.setBorder(new TitledBorder("Pendiente por Producto"));
        }

        btnRegistrarRecepcion.setEnabled(modelPendientes.getRowCount() > 0 && !hayExceso);
    }

    private boolean isPendientesValidos() {
        for (int i = 0; i < modelPendientes.getRowCount(); i++) {
            Integer pend = (Integer) modelPendientes.getValueAt(i, 5);
            Integer recAhora = parseInt(modelPendientes.getValueAt(i, 6));
            if (pend != null && recAhora != null && recAhora > pend) {
                return false;
            }
        }
        return true;
    }

    private void seleccionarOrdenEnTabla(Integer idCompraOrden) {
        if (idCompraOrden == null) {
            return;
        }
        for (int i = 0; i < modelOrdenes.getRowCount(); i++) {
            Integer id = (Integer) modelOrdenes.getValueAt(i, 0);
            if (id != null && id.equals(idCompraOrden)) {
                tableOrdenesConfirmadas.setRowSelectionInterval(i, i);
                tableOrdenesConfirmadas.scrollRectToVisible(tableOrdenesConfirmadas.getCellRect(i, 0, true));
                return;
            }
        }
    }

    private Integer parseInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
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
        btnBuscar = new javax.swing.JButton();
        jpOrdenesConfirmadas = new javax.swing.JPanel();
        jspOrdenesConfirmadas = new javax.swing.JScrollPane();
        tableOrdenesConfirmadas = new veterinaria.vista.table.AutoTable();
        jpPendientePorProducto = new javax.swing.JPanel();
        jspPendientePorProducto = new javax.swing.JScrollPane();
        tablePendientePorProducto = new veterinaria.vista.table.AutoTable();
        jpHistorialDeRecepciones = new javax.swing.JPanel();
        jspHistorialDeRecepciones = new javax.swing.JScrollPane();
        tableHistorialDeRecepciones = new veterinaria.vista.table.AutoTable();
        jpDetalleRecepcion = new javax.swing.JPanel();
        jspDetalleRecepcion = new javax.swing.JScrollPane();
        tableDetalleRecepcion = new javax.swing.JTable();
        jpFooter = new javax.swing.JPanel();
        btnVer = new javax.swing.JButton();
        btnRegistrarRecepcion = new javax.swing.JButton();
        btnAnular = new javax.swing.JButton();

        lbVisitas.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbVisitas.setText("Visitas");

        lbUsuarioBusqueda.setText("Filtro");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        btnBuscar.setText("Buscar");

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(lbVisitas)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbUsuarioBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jpHeaderLayout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(lbBuscar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnBuscar)))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbVisitas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbUsuarioBusqueda)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnBuscar))
                    .addComponent(lbBuscar))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jspOrdenesConfirmadas.setToolTipText("");

        tableOrdenesConfirmadas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jspOrdenesConfirmadas.setViewportView(tableOrdenesConfirmadas);
        tableOrdenesConfirmadas.getAccessibleContext().setAccessibleName("");
        tableOrdenesConfirmadas.getAccessibleContext().setAccessibleDescription("");

        javax.swing.GroupLayout jpOrdenesConfirmadasLayout = new javax.swing.GroupLayout(jpOrdenesConfirmadas);
        jpOrdenesConfirmadas.setLayout(jpOrdenesConfirmadasLayout);
        jpOrdenesConfirmadasLayout.setHorizontalGroup(
            jpOrdenesConfirmadasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspOrdenesConfirmadas, javax.swing.GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE)
        );
        jpOrdenesConfirmadasLayout.setVerticalGroup(
            jpOrdenesConfirmadasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspOrdenesConfirmadas)
        );

        tablePendientePorProducto.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jspPendientePorProducto.setViewportView(tablePendientePorProducto);

        javax.swing.GroupLayout jpPendientePorProductoLayout = new javax.swing.GroupLayout(jpPendientePorProducto);
        jpPendientePorProducto.setLayout(jpPendientePorProductoLayout);
        jpPendientePorProductoLayout.setHorizontalGroup(
            jpPendientePorProductoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspPendientePorProducto, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
        );
        jpPendientePorProductoLayout.setVerticalGroup(
            jpPendientePorProductoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspPendientePorProducto, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
        );

        tableHistorialDeRecepciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jspHistorialDeRecepciones.setViewportView(tableHistorialDeRecepciones);

        javax.swing.GroupLayout jpHistorialDeRecepcionesLayout = new javax.swing.GroupLayout(jpHistorialDeRecepciones);
        jpHistorialDeRecepciones.setLayout(jpHistorialDeRecepcionesLayout);
        jpHistorialDeRecepcionesLayout.setHorizontalGroup(
            jpHistorialDeRecepcionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspHistorialDeRecepciones)
        );
        jpHistorialDeRecepcionesLayout.setVerticalGroup(
            jpHistorialDeRecepcionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspHistorialDeRecepciones, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
        );

        tableDetalleRecepcion.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jspDetalleRecepcion.setViewportView(tableDetalleRecepcion);

        javax.swing.GroupLayout jpDetalleRecepcionLayout = new javax.swing.GroupLayout(jpDetalleRecepcion);
        jpDetalleRecepcion.setLayout(jpDetalleRecepcionLayout);
        jpDetalleRecepcionLayout.setHorizontalGroup(
            jpDetalleRecepcionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspDetalleRecepcion)
        );
        jpDetalleRecepcionLayout.setVerticalGroup(
            jpDetalleRecepcionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jspDetalleRecepcion, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
        );

        btnVer.setText("Ver");

        btnRegistrarRecepcion.setText("Registrar Recepción");

        btnAnular.setText("Anular");

        javax.swing.GroupLayout jpFooterLayout = new javax.swing.GroupLayout(jpFooter);
        jpFooter.setLayout(jpFooterLayout);
        jpFooterLayout.setHorizontalGroup(
            jpFooterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpFooterLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAnular)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnVer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnRegistrarRecepcion)
                .addContainerGap())
        );
        jpFooterLayout.setVerticalGroup(
            jpFooterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpFooterLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpFooterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVer)
                    .addComponent(btnRegistrarRecepcion)
                    .addComponent(btnAnular))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jpFooter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jpOrdenesConfirmadas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jpPendientePorProducto, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jpHistorialDeRecepciones, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jpDetalleRecepcion, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jpPendientePorProducto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jpHistorialDeRecepciones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jpDetalleRecepcion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jpOrdenesConfirmadas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpFooter, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAnular;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnRegistrarRecepcion;
    private javax.swing.JButton btnVer;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel jpDetalleRecepcion;
    private javax.swing.JPanel jpFooter;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpHistorialDeRecepciones;
    private javax.swing.JPanel jpOrdenesConfirmadas;
    private javax.swing.JPanel jpPendientePorProducto;
    private javax.swing.JScrollPane jspDetalleRecepcion;
    private javax.swing.JScrollPane jspHistorialDeRecepciones;
    private javax.swing.JScrollPane jspOrdenesConfirmadas;
    private javax.swing.JScrollPane jspPendientePorProducto;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbUsuarioBusqueda;
    private javax.swing.JLabel lbVisitas;
    private javax.swing.JTable tableDetalleRecepcion;
    private javax.swing.JTable tableHistorialDeRecepciones;
    private javax.swing.JTable tableOrdenesConfirmadas;
    private javax.swing.JTable tablePendientePorProducto;
    private javax.swing.JTextField txtBusqueda;
    // End of variables declaration//GEN-END:variables
}
