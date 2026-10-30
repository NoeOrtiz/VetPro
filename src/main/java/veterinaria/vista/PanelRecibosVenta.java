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
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import veterinaria.persistencia.ReciboDAO;
import veterinaria.servicio.ComprobantePdfService;
import veterinaria.util.PaginacionTablas;

/** Consulta de recibos con el patrón visual y de selección de Pacientes. */
public class PanelRecibosVenta extends JPanel {

    private static final DecimalFormat MONEDA = new DecimalFormat(
            "#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));
    private final ReciboDAO reciboDAO = new ReciboDAO();
    private final ComprobantePdfService pdfService = new ComprobantePdfService();
    private final JTextField buscar = new JTextField(25);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Seleccionar", "Recibo", "Fecha", "Cliente", "Importe total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        @Override public Class<?> getColumnClass(int column) {
            return column == 0 ? Boolean.class : String.class;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final PaginacionTablas paginacion;
    private boolean ajustandoSeleccion;

    public PanelRecibosVenta() {
        super(new BorderLayout(6, 6));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel encabezado = new JPanel(new BorderLayout(8, 4));
        JLabel titulo = new JLabel("Recibos de Venta");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        encabezado.add(titulo, BorderLayout.NORTH);

        JPanel barra = new JPanel(new BorderLayout(10, 0));
        JPanel busqueda = new JPanel(new BorderLayout(0, 2));
        busqueda.add(new JLabel("BUSCAR RECIBO"), BorderLayout.NORTH);
        JPanel campo = new JPanel(new BorderLayout(6, 0));
        JLabel lupa = new JLabel(new FlatSVGIcon("veterinaria/icon/svg/buscar.svg", 1f));
        lupa.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 4));
        campo.add(lupa, BorderLayout.WEST);
        buscar.setToolTipText("Escribí el número de recibo o el nombre del cliente");
        campo.add(buscar, BorderLayout.CENTER);
        busqueda.add(campo, BorderLayout.CENTER);
        barra.add(busqueda, BorderLayout.WEST);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        JButton btnVer = new JButton("Ver recibo");
        btnVer.setPreferredSize(new java.awt.Dimension(105, 29));
        btnVer.setToolTipText("Abrir el PDF del recibo seleccionado");
        acciones.add(btnVer);
        barra.add(acciones, BorderLayout.EAST);
        encabezado.add(barra, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(29);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(85);
        tabla.getColumnModel().getColumn(0).setMaxWidth(110);
        // Mantener el degradado horizontal original de VetPRO.
        // Un renderer compartido permite que la transición sea continua
        // desde la primera hasta la última columna, sin repetir el color.
        veterinaria.vista.table.TableGradientCell degradado =
                new veterinaria.vista.table.TableGradientCell();
        for (int columna = 1; columna < tabla.getColumnCount(); columna++) {
            tabla.getColumnModel().getColumn(columna).setCellRenderer(degradado);
        }

        JPanel listado = new JPanel(new BorderLayout(0, 6));
        listado.add(new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        // La búsqueda y la paginación utilizan el mismo campo: filtro inmediato.
        paginacion = new PaginacionTablas(tabla, buscar, 15, 1, 2, 3, 4);
        paginacion.activarTamanoAdaptable();
        listado.add(paginacion.getControles(), BorderLayout.SOUTH);
        add(listado, BorderLayout.CENTER);

        // La casilla y el resaltado de JTable representan una única selección.
        modelo.addTableModelListener(e -> {
            if (ajustandoSeleccion || e.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || e.getColumn() != 0 || e.getFirstRow() < 0) return;
            int filaModelo = e.getFirstRow();
            if (filaModelo >= modelo.getRowCount()) return;
            if (Boolean.TRUE.equals(modelo.getValueAt(filaModelo, 0))) {
                seleccionarUnico(filaModelo);
            } else if (tabla.getSelectedRow() >= 0
                    && tabla.convertRowIndexToModel(tabla.getSelectedRow()) == filaModelo) {
                tabla.clearSelection();
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || ajustandoSeleccion) return;
            int vista = tabla.getSelectedRow();
            if (vista >= 0) {
                seleccionarUnico(tabla.convertRowIndexToModel(vista));
            }
        });

        // Al modificar el filtro (incluso al vaciarlo), quitar la selección anterior.
        // Se difiere para no alterar el modelo durante la notificación del documento.
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            private void limpiarSeleccion() {
                SwingUtilities.invokeLater(() -> {
                    if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
                    ajustandoSeleccion = true;
                    try {
                        tabla.clearSelection();
                        for (int i = 0; i < modelo.getRowCount(); i++) {
                            if (Boolean.TRUE.equals(modelo.getValueAt(i, 0))) {
                                modelo.setValueAt(Boolean.FALSE, i, 0);
                            }
                        }
                    } finally {
                        ajustandoSeleccion = false;
                        tabla.repaint();
                    }
                });
            }
            @Override public void insertUpdate(DocumentEvent e) { limpiarSeleccion(); }
            @Override public void removeUpdate(DocumentEvent e) { limpiarSeleccion(); }
            @Override public void changedUpdate(DocumentEvent e) { limpiarSeleccion(); }
        });

        btnVer.addActionListener(e -> abrirRecibo());
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    abrirRecibo();
                }
            }
        });
        SwingUtilities.invokeLater(this::cargar);
    }

    private void seleccionarUnico(int filaModelo) {
        if (filaModelo < 0 || filaModelo >= modelo.getRowCount() || ajustandoSeleccion) return;
        ajustandoSeleccion = true;
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                boolean debeMarcarse = i == filaModelo;
                if (!Boolean.valueOf(debeMarcarse).equals(modelo.getValueAt(i, 0))) {
                    modelo.setValueAt(debeMarcarse, i, 0);
                }
            }
            int vista = tabla.convertRowIndexToView(filaModelo);
            if (vista >= 0 && vista < tabla.getRowCount()) {
                tabla.getSelectionModel().setSelectionInterval(vista, vista);
            }
        } finally {
            ajustandoSeleccion = false;
            tabla.repaint();
        }
    }

    private void cargar() {
        try {
            List<Object[]> filas = reciboDAO.buscarRecibos("");
            modelo.setRowCount(0);
            SimpleDateFormat fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (Object[] r : filas) {
                String nombre = ((r[2] == null ? "" : r[2].toString()) + " "
                        + (r[3] == null ? "" : r[3].toString())).trim();
                String numero = String.format("RC-%06d", ((Number) r[0]).longValue());
                String total = "$ " + MONEDA.format((BigDecimal) r[4]);
                modelo.addRow(new Object[]{
                    false, numero, r[1] == null ? "-" : fecha.format(r[1]),
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
        String numero = modelo.getValueAt(filaModelo, 1).toString();
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
