package veterinaria.vista;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
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

/** Consulta de solo lectura y reimpresión de ventas históricas. */
public class PanelRecibosVenta extends JPanel {

    private final ReciboDAO reciboDAO = new ReciboDAO();
    private final ComprobantePdfService pdfService = new ComprobantePdfService();
    private final JTextField buscar = new JTextField(24);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"N.º recibo", "Fecha", "Cliente", "Total", "Tipo"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public PanelRecibosVenta() {
        super(new BorderLayout(8, 8));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel encabezado = new JPanel(new BorderLayout(6, 6));
        encabezado.add(new JLabel("RECIBOS DE VENTA - Consulta y reimpresión"), BorderLayout.NORTH);
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtros.add(new JLabel("N.º de recibo o cliente:"));
        filtros.add(buscar);
        JButton btnBuscar = new JButton("Buscar");
        filtros.add(btnBuscar);
        encabezado.add(filtros, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(30);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        JButton btnVer = new JButton("Ver recibo",
                new FlatSVGIcon("veterinaria/icon/svg/ver_recibo.svg", 0.8f));
        JButton btnImprimir = new JButton("Imprimir",
                new FlatSVGIcon("veterinaria/icon/svg/imprimir_recibo.svg", 0.8f));
        acciones.add(btnVer);
        acciones.add(btnImprimir);
        add(acciones, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> cargar());
        buscar.addActionListener(e -> cargar());
        btnVer.addActionListener(e -> procesar(false));
        btnImprimir.addActionListener(e -> procesar(true));
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
                modelo.addRow(new Object[]{
                    r[0], r[1] == null ? "-" : fecha.format(r[1]),
                    nombre.isEmpty() ? "Consumidor Final" : nombre,
                    r[4], r[5]
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron consultar los recibos: "
                    + ex.getMessage(), "Recibos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void procesar(boolean imprimir) {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un recibo de la tabla.");
            return;
        }
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        long id = ((Number) modelo.getValueAt(filaModelo, 0)).longValue();
        try {
            File archivo = pdfService.generarReciboVentaPdf(id);
            if (imprimir) pdfService.imprimirArchivo(archivo);
            else pdfService.abrirArchivo(archivo);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el comprobante: "
                    + ex.getMessage(), "Recibos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
