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
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import veterinaria.entidad.Rubro;
import veterinaria.persistencia.RubroDAO;

/**
 * Panel CRUD para Rubros (catálogo) con Stock Mínimo Default.
 *
 * Regla de negocio (sugerido):
 * - Si producto.stock_minimo > 0 se usa ese valor.
 * - Si producto.stock_minimo = 0 se usa rubro.stock_minimo_default.
 */
public class PanelRubros extends JPanel {

    private final RubroDAO dao = new RubroDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Nombre", "Stock mínimo default", "Activo"}
    ) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int col) {
            switch (col) {
                case 0: return Integer.class;
                case 2: return Integer.class;
                case 3: return Boolean.class;
                default: return String.class;
            }
        }
    };

    private final JTable table = new JTable(model);

    private final JButton btnNuevo = new JButton("Agregar");
    private final JButton btnEditar = new JButton("Modificar");
    private final JButton btnActivarDesactivar = new JButton("Activar/Desactivar");
    private final JButton btnSincronizar = new JButton("Sincronizar desde Productos");
    private final JButton btnRefrescar = new JButton("Refrescar");

    public PanelRubros() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Configuración - Rubros");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        JPanel top = new JPanel(new BorderLayout());
        top.add(titulo, BorderLayout.WEST);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnActivarDesactivar);
        acciones.add(btnSincronizar);
        acciones.add(btnRefrescar);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);

        wireEvents();
        refrescarTabla();
    }

    private void wireEvents() {
        btnRefrescar.addActionListener(e -> refrescarTabla());
        btnNuevo.addActionListener(e -> crearRubro());
        btnEditar.addActionListener(e -> editarRubroSeleccionado());
        btnActivarDesactivar.addActionListener(e -> toggleActivoSeleccionado());
        btnSincronizar.addActionListener(e -> sincronizarDesdeProductos());
    }

    public final void refrescarTabla() {
        model.setRowCount(0);
        List<Rubro> lista = dao.listarTodos();
        for (Rubro r : lista) {
            model.addRow(new Object[]{
                r.getIdRubro(),
                r.getNombre(),
                r.getStockMinimoDefault(),
                r.isActivo()
            });
        }

        // Ocultar ID visualmente (se queda en el modelo)
        try {
            javax.swing.table.TableColumn colId = table.getColumnModel().getColumn(0);
            colId.setMinWidth(0);
            colId.setMaxWidth(0);
            colId.setPreferredWidth(0);
        } catch (Exception ignore) {}
    }

    private void crearRubro() {
        JTextField txtNombre = new JTextField();
        JSpinner spMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));

        Object[] form = {
            "Nombre:", txtNombre,
            "Stock mínimo default:", spMin
        };

        int op = JOptionPane.showConfirmDialog(this, form, "Agregar rubro",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        String nombre = txtNombre.getText().trim();
        int stockMin = (Integer) spMin.getValue();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Rubro existe = dao.buscarPorNombre(nombre);
        if (existe != null) {
            JOptionPane.showMessageDialog(this, "Ya existe un rubro con ese nombre.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!confirmarCreacionRubro(nombre, stockMin)) return;

        Rubro r = new Rubro();
        r.setNombre(nombre);
        r.setStockMinimoDefault(stockMin);
        r.setActivo(true);

        boolean ok = dao.crear(r);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el rubro.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        refrescarTabla();
    }

    private void editarRubroSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un rubro.", "Atención",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer id = (Integer) model.getValueAt(row, 0);
        Rubro r = dao.buscarPorId(id);
        if (r == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el rubro seleccionado.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            refrescarTabla();
            return;
        }

        JTextField txtNombre = new JTextField(r.getNombre());
        JSpinner spMin = new JSpinner(new SpinnerNumberModel(r.getStockMinimoDefault(), 0, 999999, 1));

        Object[] form = {
            "Nombre:", txtNombre,
            "Stock mínimo default:", spMin
        };

        int op = JOptionPane.showConfirmDialog(this, form, "Modificar rubro",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        String nombre = txtNombre.getText().trim();
        int stockMin = (Integer) spMin.getValue();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Rubro otro = dao.buscarPorNombre(nombre);
        if (otro != null && !otro.getIdRubro().equals(r.getIdRubro())) {
            JOptionPane.showMessageDialog(this, "Ya existe otro rubro con ese nombre.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!confirmarEdicionRubro(r.getNombre(), nombre, r.getStockMinimoDefault(), stockMin)) return;

        r.setNombre(nombre);
        r.setStockMinimoDefault(stockMin);

        boolean ok = dao.actualizar(r);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el rubro.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        refrescarTabla();
    }

    private boolean confirmarCreacionRubro(String nombre, int stockMinimoDefault) {
        String msg = "<html><body style='width:320px'>"
                + "<p>Se creará el siguiente rubro:</p>"
                + "<b>Nombre:</b> " + escapeHtml(nombre) + "<br>"
                + "<b>Stock mínimo default:</b> " + stockMinimoDefault
                + "<br><br><b>¿Desea confirmar?</b></body></html>";

        int op = JOptionPane.showConfirmDialog(this, msg,
                "Confirmar creación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return op == JOptionPane.YES_OPTION;
    }

    private boolean confirmarEdicionRubro(String nombreOriginal, String nombreNuevo,
                                          int stockMinOriginal, int stockMinNuevo) {
        StringBuilder msg = new StringBuilder("<html><body style='width:360px'>");
        msg.append("<p>Se modificarán los siguientes datos:</p>");
        int cambios = 0;
        cambios += appendCambio(msg, "Nombre", nombreOriginal, nombreNuevo);
        cambios += appendCambio(msg, "Stock mínimo default", String.valueOf(stockMinOriginal), String.valueOf(stockMinNuevo));

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

    private void toggleActivoSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un rubro.", "Atención",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer id = (Integer) model.getValueAt(row, 0);
        Rubro r = dao.buscarPorId(id);
        if (r == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el rubro seleccionado.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            refrescarTabla();
            return;
        }

        boolean nuevoEstado = !r.isActivo();
        int op = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea " + (nuevoEstado ? "ACTIVAR" : "DESACTIVAR") + " el rubro '" + r.getNombre() + "'?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        r.setActivo(nuevoEstado);
        boolean ok = dao.actualizar(r);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado del rubro.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        refrescarTabla();
    }

    private void sincronizarDesdeProductos() {
        int op = JOptionPane.showConfirmDialog(this,
                "Esto creará rubros activos (stock mínimo default=0) a partir de valores existentes en productos.\n\n¿Continuar?",
                "Sincronizar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        int insertados = dao.sincronizarDesdeProductos();
        JOptionPane.showMessageDialog(this,
                "Sincronización finalizada. Rubros insertados: " + insertados,
                "OK",
                JOptionPane.INFORMATION_MESSAGE);
        refrescarTabla();
    }
}
