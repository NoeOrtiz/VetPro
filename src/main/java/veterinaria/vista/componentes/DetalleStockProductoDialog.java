package veterinaria.vista.componentes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import veterinaria.entidad.Producto;
import veterinaria.entidad.StockMovimiento;
import veterinaria.persistencia.ProductoDAO;
import veterinaria.servicio.StockMovimientoService;

/**
 * Dialog de detalle de stock:
 * - Encabezado con datos del producto.
 * - Tabla con movimientos (últimos N) para trazabilidad.
 */
public class DetalleStockProductoDialog extends JDialog {

    private static final DecimalFormat DF_MONEY = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DF_FECHA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final JTable table;
    private final DefaultTableModel model;

    public DetalleStockProductoDialog(Window owner, Integer idProducto) {
        super(owner, "Detalle de Stock", ModalityType.APPLICATION_MODAL);

        Producto p = null;
        try {
            p = new ProductoDAO().buscarPorId(idProducto);
        } catch (Exception ignore) {
        }

        String titulo = (p != null)
                ? ("Producto: " + n(p.getNombre()) + "  |  Código: " + n(p.getCodigo()))
                : ("Producto ID: " + idProducto);

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.LEFT);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        String rubro = (p != null) ? n(p.getRubro()) : "";
        String precio = (p != null && p.getPrecioCosto() != null) ? DF_MONEY.format(p.getPrecioCosto()) : "";
        int stock = (p != null && p.getStock() != null) ? p.getStock() : 0;

        JLabel lblSub = new JLabel("Rubro: " + rubro + "   |   Precio costo: " + precio + "   |   Stock actual: " + stock);
        lblSub.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel north = new JPanel(new BorderLayout());
        north.add(lblTitulo, BorderLayout.NORTH);
        north.add(lblSub, BorderLayout.SOUTH);

        model = new DefaultTableModel(new Object[]{
            "Fecha", "Tipo", "Cantidad", "Stock Antes", "Stock Después", "Usuario", "Obs.", "Recibo", "Recepción"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(24);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        south.add(btnCerrar);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(980, 520));
        pack();
        setLocationRelativeTo(owner);

        cargarMovimientos(idProducto);
    }

    private void cargarMovimientos(Integer idProducto) {
        model.setRowCount(0);
        try {
            List<StockMovimiento> movimientos = new StockMovimientoService().listarPorProducto(idProducto, 200);
            for (StockMovimiento sm : movimientos) {
                String usuario = "";
                try {
                    if (sm.getUsuario() != null && sm.getUsuario().getPersona() != null) {
                        usuario = (n(sm.getUsuario().getPersona().getApellido()) + " " + n(sm.getUsuario().getPersona().getNombre())).trim();
                    }
                } catch (Exception ignore) {
                }

                Object idRecibo = null;
                try {
                    if (sm.getRecibo() != null) idRecibo = sm.getRecibo().getIdRecibo();
                } catch (Exception ignore) {
                }

                Object idRecepcion = null;
                try {
                    if (sm.getCompraRecepcion() != null) idRecepcion = sm.getCompraRecepcion().getIdCompraRecepcion();
                } catch (Exception ignore) {
                }

                model.addRow(new Object[]{
                    sm.getFecha() != null ? DF_FECHA.format(sm.getFecha()) : "",
                    sm.getTipo() != null ? sm.getTipo().name() : "",
                    sm.getCantidad() != null ? sm.getCantidad() : 0,
                    sm.getStockAntes() != null ? sm.getStockAntes() : 0,
                    sm.getStockDespues() != null ? sm.getStockDespues() : 0,
                    usuario,
                    n(sm.getObservacion()),
                    idRecibo != null ? idRecibo : "",
                    idRecepcion != null ? idRecepcion : ""
                });
            }
        } catch (Exception ex) {
            model.addRow(new Object[]{"", "", "", "", "", "", "Error al cargar movimientos: " + ex.getMessage(), "", ""});
        }
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    public static void mostrar(Window owner, Integer idProducto) {
        DetalleStockProductoDialog d = new DetalleStockProductoDialog(owner, idProducto);
        d.setVisible(true);
    }
}
