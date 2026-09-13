package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import veterinaria.entidad.TipoCitaPeluqueria;
import veterinaria.servicio.TipoCitaPeluqueriaService;

public class PanelTiposCitaPeluqueria extends JPanel {

    private final TipoCitaPeluqueriaService service = new TipoCitaPeluqueriaService();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Orden", "Descripción", "Precio", "Activo"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Integer.class;
            if (columnIndex == 1) return Integer.class;
            if (columnIndex == 3) return String.class;
            if (columnIndex == 4) return Boolean.class;
            return String.class;
        }
    };

    private final JTable table = new JTable(model);

    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnDesactivar = new JButton("Desactivar");
    private final JButton btnActivar = new JButton("Activar");
    private final JButton btnSubir = new JButton("Subir");
    private final JButton btnBajar = new JButton("Bajar");
    private final JButton btnOrden = new JButton("Orden...");
    private final JButton btnRefrescar = new JButton("Refrescar");

    public PanelTiposCitaPeluqueria() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        top.add(btnNuevo);
        top.add(btnEditar);
        top.add(btnDesactivar);
        top.add(btnActivar);
        top.add(btnSubir);
        top.add(btnBajar);
        top.add(btnOrden);
        top.add(btnRefrescar);

        add(top, BorderLayout.SOUTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> onNuevo());
        btnEditar.addActionListener(e -> onEditar());
        btnDesactivar.addActionListener(e -> onSetActivo(false));
        btnActivar.addActionListener(e -> onSetActivo(true));
        btnSubir.addActionListener(e -> onSubir());
        btnBajar.addActionListener(e -> onBajar());
        btnOrden.addActionListener(e -> onOrdenManual());
        btnRefrescar.addActionListener(e -> cargarTabla());

        cargarTabla();
    }

    private void cargarTabla() {
        model.setRowCount(0);
        List<TipoCitaPeluqueria> lista = service.listarTodos();
        for (TipoCitaPeluqueria t : lista) {
            model.addRow(new Object[]{
                t.getId(),
                t.getOrden(),
                t.getDescripcion(),
                formatPrecio(t.getPrecio()),
                t.isActivo()
            });
        }
    }

    private void onNuevo() {
        String desc = JOptionPane.showInputDialog(this, "Descripción del tipo de cita:", "Nuevo", JOptionPane.QUESTION_MESSAGE);
        if (desc == null) return;
        desc = desc.trim();
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción no puede estar vacía.");
            return;
        }

        BigDecimal precio = pedirPrecio(null);
        if (precio == null) return;

        if (!confirmarCreacionTipoCita(desc, precio)) return;

        try {
            service.guardarNuevo(desc, precio);
            cargarTabla();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar. Detalle: " + ex.getMessage());
        }
    }

    private void onEditar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para editar.");
            return;
        }

        Integer id = (Integer) model.getValueAt(row, 0);
        Integer ordenActual = (Integer) model.getValueAt(row, 1);
        String descActual = String.valueOf(model.getValueAt(row, 2));
        String precioActual = String.valueOf(model.getValueAt(row, 3));
        Boolean activo = (Boolean) model.getValueAt(row, 4);

        String desc = JOptionPane.showInputDialog(this, "Descripción:", descActual);
        if (desc == null) return;
        desc = desc.trim();
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción no puede estar vacía.");
            return;
        }

        BigDecimal precio = pedirPrecio(precioActual);
        if (precio == null) return;

        if (!confirmarEdicionTipoCita(descActual, desc, precioActual, formatPrecio(precio))) return;

        try {
            service.actualizar(id, desc, precio, activo != null ? activo : true);
            cargarTabla();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar. Detalle: " + ex.getMessage());
        }
    }

    private boolean confirmarCreacionTipoCita(String descripcion, BigDecimal precio) {
        String msg = "<html><body style='width:320px'>"
                + "<p>Se creará el siguiente tipo de cita:</p>"
                + "<b>Descripción:</b> " + escapeHtml(descripcion) + "<br>"
                + "<b>Precio:</b> " + escapeHtml(formatPrecio(precio))
                + "<br><br><b>¿Desea confirmar?</b></body></html>";

        int ok = JOptionPane.showConfirmDialog(this, msg,
                "Confirmar creación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return ok == JOptionPane.YES_OPTION;
    }

    private boolean confirmarEdicionTipoCita(String descripcionOriginal, String descripcionNueva,
                                             String precioOriginal, String precioNuevo) {
        StringBuilder msg = new StringBuilder("<html><body style='width:360px'>");
        msg.append("<p>Se modificarán los siguientes datos:</p>");
        int cambios = 0;
        cambios += appendCambio(msg, "Descripción", descripcionOriginal, descripcionNueva);
        cambios += appendCambio(msg, "Precio", precioOriginal, precioNuevo);

        if (cambios == 0) {
            JOptionPane.showMessageDialog(this, "No se detectaron cambios para guardar.",
                    "Sin cambios", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        msg.append("<br><b>¿Desea confirmar los cambios?</b></body></html>");
        int ok = JOptionPane.showConfirmDialog(this, msg.toString(),
                "Confirmar edición",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return ok == JOptionPane.YES_OPTION;
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

    private void onSetActivo(boolean activo) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro.");
            return;
        }
        Integer id = (Integer) model.getValueAt(row, 0);
        String desc = String.valueOf(model.getValueAt(row, 1));
        int ok = JOptionPane.showConfirmDialog(this,
                (activo ? "¿Activar" : "¿Desactivar") + " '" + desc + "'?",
                "Confirmación",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            service.setActivo(id, activo);
            cargarTabla();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cambiar estado. Detalle: " + ex.getMessage());
        }
    }

    private BigDecimal pedirPrecio(String valorInicial) {
        String input = JOptionPane.showInputDialog(this, "Precio (ej: 3500 o 3500.00):", valorInicial == null ? "" : valorInicial);
        if (input == null) return null;
        input = input.trim().replace(",", ".");
        if (input.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(input);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Precio inválido.");
            return null;
        }
    }

    private String formatPrecio(BigDecimal precio) {
        if (precio == null) return "0.00";
        // Dos decimales, sin depender de Locale
        return precio.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }


    private void onSubir() {
        int row = table.getSelectedRow();
        if (row <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro (que no sea el primero) para subir.");
            return;
        }
        Integer idActual = (Integer) model.getValueAt(row, 0);
        Integer idArriba = (Integer) model.getValueAt(row - 1, 0);
        try {
            if (service.swapOrden(idActual, idArriba)) {
                cargarTabla();
                table.setRowSelectionInterval(row - 1, row - 1);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo reordenar. Detalle: " + ex.getMessage());
        }
    }

    private void onBajar() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= model.getRowCount() - 1) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro (que no sea el último) para bajar.");
            return;
        }
        Integer idActual = (Integer) model.getValueAt(row, 0);
        Integer idAbajo = (Integer) model.getValueAt(row + 1, 0);
        try {
            if (service.swapOrden(idActual, idAbajo)) {
                cargarTabla();
                table.setRowSelectionInterval(row + 1, row + 1);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo reordenar. Detalle: " + ex.getMessage());
        }
    }

    private void onOrdenManual() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para asignar orden.");
            return;
        }
        Integer id = (Integer) model.getValueAt(row, 0);
        Integer ordenActual = (Integer) model.getValueAt(row, 1);

        String input = JOptionPane.showInputDialog(this, "Orden (número entero):", ordenActual);
        if (input == null) return;
        input = input.trim();
        if (input.isEmpty()) return;

        int orden;
        try {
            orden = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Orden inválido. Ingrese un número entero.");
            return;
        }
        if (orden < 0) {
            JOptionPane.showMessageDialog(this, "El orden no puede ser negativo.");
            return;
        }

        try {
            if (service.setOrden(id, orden)) {
                cargarTabla();
                // re-seleccionar por id
                for (int i = 0; i < model.getRowCount(); i++) {
                    Integer rid = (Integer) model.getValueAt(i, 0);
                    if (rid != null && rid.equals(id)) {
                        table.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el orden. Detalle: " + ex.getMessage());
        }
    }
}
