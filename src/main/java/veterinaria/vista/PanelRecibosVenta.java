package veterinaria.vista;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import veterinaria.persistencia.ReciboDAO;
import veterinaria.servicio.ComprobantePdfService;
import veterinaria.util.PaginacionTablas;

/** Consulta y visualización de comprobantes históricos, sin registrar operaciones nuevas. */
public class PanelRecibosVenta extends JPanel {

    private static final DecimalFormat MONEDA = new DecimalFormat(
            "#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));
    private final ReciboDAO reciboDAO = new ReciboDAO();
    private final ComprobantePdfService pdfService = new ComprobantePdfService();
    private final JTextField buscar = new JTextField(25);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Recibo", "Fecha", "Cliente", "Importe total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final PaginacionTablas paginacion;

    public PanelRecibosVenta() {
        super(new BorderLayout(8, 8));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 10, 14));

        JPanel encabezado = new JPanel(new BorderLayout(0, 8));
        JLabel titulo = new JLabel("Consulta de recibos de venta");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        encabezado.add(titulo, BorderLayout.NORTH);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filtros.add(new JLabel("Buscar por número o cliente:"));
        buscar.setToolTipText("Ej.: RC-000008, 8 o apellido del cliente");
        filtros.add(buscar);
        JButton btnBuscar = new JButton("Buscar",
                new FlatSVGIcon("veterinaria/icon/svg/buscar.svg", 0.7f));
        filtros.add(btnBuscar);
        JButton btnVer = new JButton("Ver recibo");
        btnVer.setToolTipText("Abrir el comprobante PDF seleccionado");
        filtros.add(btnVer);
        encabezado.add(filtros, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.getTableHeader().setReorderingAllowed(false);

        JPanel listado = new JPanel(new BorderLayout(0, 8));
        JScrollPane desplazamientoTabla = new JScrollPane(tabla);
        desplazamientoTabla.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        listado.add(desplazamientoTabla, BorderLayout.CENTER);
        paginacion = new PaginacionTablas(tabla, new JTextField(), 15, 0, 1, 2, 3);
        listado.add(paginacion.getControles(), BorderLayout.SOUTH);
        add(listado, BorderLayout.CENTER);

        btnBuscar.addActionListener(e -> cargar());
        buscar.addActionListener(e -> cargar());
        btnVer.addActionListener(e -> abrirRecibo());
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    abrirRecibo();
                }
            }
        });
        javax.swing.SwingUtilities.invokeLater(this::cargar);
    }

    private void cargar() {
        try {
            List<Object[]> filas = reciboDAO.buscarRecibos(buscar.getText());
            modelo.setRowCount(0);
            SimpleDateFormat fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (Object[] r : filas) {
                String nombre = ((r[2] == null ? "" : r[2].toString()) + " "
                        + (r[3] == null ? "" : r[3].toString())).trim();
                String numero = String.format("RC-%06d", ((Number) r[0]).longValue());
                String total = "$ " + MONEDA.format((BigDecimal) r[4]);
                modelo.addRow(new Object[]{
                    numero, r[1] == null ? "-" : fecha.format(r[1]),
                    nombre.isEmpty() ? "Consumidor final" : nombre, total
                });
            }
            paginacion.actualizar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron consultar los recibos: " + ex.getMessage(),
                    "Recibos de venta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirRecibo() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un recibo de la tabla.");
            return;
        }
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        String numero = modelo.getValueAt(filaModelo, 0).toString();
        long id = Long.parseLong(numero.substring(3));
        try {
            File archivo = pdfService.generarReciboVentaPdf(id);
            pdfService.abrirArchivo(archivo);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir el comprobante: " + ex.getMessage(),
                    "Recibos de venta", JOptionPane.ERROR_MESSAGE);
        }
    }
}
