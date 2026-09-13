package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import veterinaria.entidad.MetodoPago;
import veterinaria.persistencia.MetodoPagoDAO;

/**
 * Panel CRUD para Métodos de Pago.
 */
public class PanelMetodosPago extends JPanel {

    private final MetodoPagoDAO dao = new MetodoPagoDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Nombre", "Descripción"}
    ) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int col) {
            if (col == 0) return Integer.class;
            return String.class;
        }
    };

    private final JTable table = new JTable(model);

    private final JButton btnNuevo = new JButton("Agregar");
    private final JButton btnEditar = new JButton("Modificar");
    private final JButton btnEliminar = new JButton("Eliminar");
    private final JButton btnRefrescar = new JButton("Refrescar");

    public PanelMetodosPago() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Configuración - Métodos de Pago");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        JPanel top = new JPanel(new BorderLayout());
        top.add(titulo, BorderLayout.WEST);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnEliminar);
        acciones.add(btnRefrescar);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);

        wireEvents();
        refrescarTabla();
    }

    private void wireEvents() {
        btnRefrescar.addActionListener(e -> refrescarTabla());
        btnNuevo.addActionListener(e -> crearMetodoPago());
        btnEditar.addActionListener(e -> editarMetodoPagoSeleccionado());
        btnEliminar.addActionListener(e -> eliminarMetodoPagoSeleccionado());
    }

    public final void refrescarTabla() {
        model.setRowCount(0);
        List<MetodoPago> lista = dao.buscarTodos();
        for (MetodoPago m : lista) {
            model.addRow(new Object[]{m.getIdMetodoPago(), m.getNombre(), m.getDescripcion()});
        }

        // Ocultar ID visualmente (se queda en el modelo)
        try {
            javax.swing.table.TableColumn colId = table.getColumnModel().getColumn(0);
            colId.setMinWidth(0);
            colId.setMaxWidth(0);
            colId.setPreferredWidth(0);
        } catch (Exception ignore) {}
    }

    private void crearMetodoPago() {
        JTextField txtNombre = new JTextField();
        JTextField txtDesc = new JTextField();

        Object[] form = {
            "Nombre:", txtNombre,
            "Descripción:", txtDesc
        };

        int op = JOptionPane.showConfirmDialog(this, form, "Agregar método de pago",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (op != JOptionPane.OK_OPTION) return;

        String nombre = txtNombre.getText().trim();
        String desc = txtDesc.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        MetodoPago existe = dao.buscarPorNombre(nombre);
        if (existe != null) {
            JOptionPane.showMessageDialog(this, "Ya existe un método de pago con ese nombre.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!confirmarCreacionMetodoPago(nombre, desc)) return;

        MetodoPago nuevo = new MetodoPago(null, nombre, desc);
        boolean ok = dao.crear(nuevo);

        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el método de pago.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        refrescarTabla();
    }

    private void editarMetodoPagoSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un método de pago.", "Atención",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer id = (Integer) model.getValueAt(row, 0);
        MetodoPago m = dao.buscarPorId(id);
        if (m == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el registro seleccionado.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            refrescarTabla();
            return;
        }

        JTextField txtNombre = new JTextField(m.getNombre());
        JTextField txtDesc = new JTextField(m.getDescripcion() == null ? "" : m.getDescripcion());

        Object[] form = {
            "Nombre:", txtNombre,
            "Descripción:", txtDesc
        };

        int op = JOptionPane.showConfirmDialog(this, form, "Modificar método de pago",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (op != JOptionPane.OK_OPTION) return;

        String nombre = txtNombre.getText().trim();
        String desc = txtDesc.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!confirmarEdicionMetodoPago(m.getNombre(), nombre, m.getDescripcion(), desc)) return;

        m.setNombre(nombre);
        m.setDescripcion(desc);

        boolean ok = dao.actualizar(m);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el método de pago.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        refrescarTabla();
    }

    private boolean confirmarCreacionMetodoPago(String nombre, String descripcion) {
        String msg = "<html><body style='width:320px'>"
                + "<p>Se creará el siguiente método de pago:</p>"
                + "<b>Nombre:</b> " + escapeHtml(nombre) + "<br>"
                + "<b>Descripción:</b> " + escapeHtml(descripcion)
                + "<br><br><b>¿Desea confirmar?</b></body></html>";

        int op = JOptionPane.showConfirmDialog(this, msg,
                "Confirmar creación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return op == JOptionPane.YES_OPTION;
    }

    private boolean confirmarEdicionMetodoPago(String nombreOriginal, String nombreNuevo,
                                               String descripcionOriginal, String descripcionNueva) {
        StringBuilder msg = new StringBuilder("<html><body style='width:360px'>");
        msg.append("<p>Se modificarán los siguientes datos:</p>");
        int cambios = 0;
        cambios += appendCambio(msg, "Nombre", nombreOriginal, nombreNuevo);
        cambios += appendCambio(msg, "Descripción", descripcionOriginal, descripcionNueva);

        if (cambios == 0) {
            JOptionPane.showMessageDialog(this, "No se detectaron cambios para guardar.",
                    "Sin cambios", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        msg.append("<br><b>¿Desea confirmar los cambios?</b></body></html>");
        int op = JOptionPane.showConfirmDialog(this, msg.toString(),
                "Confirmar edición",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return op == JOptionPane.YES_OPTION;
    }

    private int appendCambio(StringBuilder sb, String campo, String valorOriginal, String valorNuevo) {
        String original = safe(valorOriginal);
        String nuevo = safe(valorNuevo);
        if (original.equals(nuevo)) {
            return 0;
        }
        sb.append("• <b>").append(escapeHtml(campo)).append(":</b> ")
          .append(escapeHtml(original))
          .append(" → ")
          .append(escapeHtml(nuevo))
          .append("<br>");
        return 1;
    }

    private String safe(String valor) {
        if (valor == null || valor.trim().isEmpty()) return "(vacío)";
        return valor.trim();
    }

    private String escapeHtml(String valor) {
        return safe(valor)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void eliminarMetodoPagoSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un método de pago.", "Atención",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer id = (Integer) model.getValueAt(row, 0);

        int op = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar el método de pago seleccionado?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (op != JOptionPane.YES_OPTION) return;

        boolean ok = dao.eliminarPorId(id);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar (puede estar en uso).", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        refrescarTabla();
    }
}
