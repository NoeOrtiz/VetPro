package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.JOptionPane;

import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.MetodoPagoControlador;
import veterinaria.controlador.RubroControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Rubro;
import veterinaria.servicio.ConfiguracionService;

/**
 * Configuraciones de Ventas (persistidas en tabla configuracion).
 */
public class PanelConfigVentas extends JPanel {

    private final ConfiguracionService configService = new ConfiguracionService();
    private final RubroControlador rubroControlador = new RubroControlador();
    private final ClienteControlador clienteControlador = new ClienteControlador();
    private final MetodoPagoControlador metodoPagoControlador = new MetodoPagoControlador();

    private final JSpinner spGananciaGlobal = new JSpinner(new SpinnerNumberModel(40, 0, 999, 1));
    private final JSpinner spDecimales = new JSpinner(new SpinnerNumberModel(2, 0, 4, 1));
    private final JComboBox<RedondeoItem> cbRedondeo = new JComboBox<>(new RedondeoItem[]{
        new RedondeoItem("HALF_UP", "Mitad hacia arriba"),
        new RedondeoItem("HALF_EVEN", "Mitad par"),
        new RedondeoItem("UP", "Siempre hacia arriba"),
        new RedondeoItem("DOWN", "Siempre hacia abajo")
    });

    private final JCheckBox chkStockNegativo = new JCheckBox("Permitir vender sin stock (stock negativo)");
    private final JCheckBox chkAbrirPdf = new JCheckBox("Abrir comprobante PDF al generar");
    private final JCheckBox chkImprimirAuto = new JCheckBox("Imprimir automáticamente el comprobante de venta");

    private final JComboBox<ClienteItem> cbClienteDefault = new JComboBox<>();
    private final JComboBox<MetodoPagoItem> cbMetodoPagoDefault = new JComboBox<>();

    private final RubroTableModel rubroModel = new RubroTableModel();
    private final JTable tblRubros = new JTable(rubroModel);

    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnRecargar = new JButton("Recargar");

    public PanelConfigVentas() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Configuraciones de Ventas");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        g.gridx = 0; g.gridy = row; g.weightx = 0;
        center.add(new JLabel("% Ganancia global (fallback)"), g);
        g.gridx = 1; g.weightx = 1;
        center.add(spGananciaGlobal, g);

        row++;
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        center.add(new JLabel("Decimales en montos"), g);
        g.gridx = 1; g.weightx = 1;
        center.add(spDecimales, g);

        row++;
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        center.add(new JLabel("Modo de redondeo"), g);
        g.gridx = 1; g.weightx = 1;
        center.add(cbRedondeo, g);

        row++;
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weightx = 1;
        center.add(chkStockNegativo, g);

        row++;
        g.gridy = row;
        center.add(chkAbrirPdf, g);

        row++;
        g.gridy = row;
        center.add(chkImprimirAuto, g);

        row++;
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        center.add(new JLabel("Cliente por defecto"), g);
        g.gridx = 1; g.weightx = 1;
        center.add(cbClienteDefault, g);

        row++;
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        center.add(new JLabel("Método de pago por defecto"), g);
        g.gridx = 1; g.weightx = 1;
        center.add(cbMetodoPagoDefault, g);

        row++;
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weightx = 1;
        JLabel lbRubros = new JLabel("Ganancia por Rubro (opcional, si está vacío usa el global):");
        lbRubros.setFont(lbRubros.getFont().deriveFont(java.awt.Font.BOLD));
        center.add(lbRubros, g);

        row++;
        g.gridy = row;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        JScrollPane sp = new JScrollPane(tblRubros);
        sp.setPreferredSize(new Dimension(800, 260));
        center.add(sp, g);

        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.add(btnRecargar);
        south.add(btnGuardar);
        add(south, BorderLayout.SOUTH);

        btnRecargar.addActionListener(e -> cargar());
        btnGuardar.addActionListener(e -> guardar());

        // carga inicial
        SwingUtilities.invokeLater(this::cargar);
    }

    public final void cargar() {
        try {
            // Controles simples
            spGananciaGlobal.setValue(configService.getInt(
                    ConfiguracionService.KEY_VENTAS_PORC_GANANCIA_GLOBAL,
                    ConfiguracionService.DEFAULT_VENTAS_PORC_GANANCIA_GLOBAL
            ));
            spDecimales.setValue(configService.getInt(
                    ConfiguracionService.KEY_VENTAS_DECIMALES,
                    ConfiguracionService.DEFAULT_VENTAS_DECIMALES
            ));
            String rm = configService.getString(
                    ConfiguracionService.KEY_VENTAS_REDONDEO,
                    ConfiguracionService.DEFAULT_VENTAS_REDONDEO
            );
                        // Seleccionar por código (la UI muestra texto en español)
            for (int i = 0; i < cbRedondeo.getItemCount(); i++) {
                RedondeoItem it = cbRedondeo.getItemAt(i);
                if (it != null && it.getCode() != null && it.getCode().equalsIgnoreCase(rm)) {
                    cbRedondeo.setSelectedIndex(i);
                    break;
                }
            }

            chkStockNegativo.setSelected(configService.getBoolean(
                    ConfiguracionService.KEY_VENTAS_PERMITIR_STOCK_NEGATIVO,
                    ConfiguracionService.DEFAULT_VENTAS_PERMITIR_STOCK_NEGATIVO != 0
            ));
            chkAbrirPdf.setSelected(configService.getBoolean(
                    ConfiguracionService.KEY_VENTAS_ABRIR_COMPROBANTE_PDF,
                    ConfiguracionService.DEFAULT_VENTAS_ABRIR_COMPROBANTE_PDF != 0
            ));
            chkImprimirAuto.setSelected(configService.getBoolean(
                    ConfiguracionService.KEY_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO,
                    ConfiguracionService.DEFAULT_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO != 0
            ));

            // Combos defaults
            cargarClientesCombo();
            cargarMetodosPagoCombo();

            // Tabla rubros
            List<Rubro> rubros = rubroControlador.listarTodos();
            rubroModel.setData(rubros);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar Config Ventas: " + ex.getMessage(),
                    "Config Ventas", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarClientesCombo() {
        cbClienteDefault.removeAllItems();
        cbClienteDefault.addItem(new ClienteItem(0, "Sin default"));

        List<Cliente> clientes = clienteControlador.buscarTodosLosClientes();
        for (Cliente c : clientes) {
            String nombre = (c.getPersona() != null)
                    ? (c.getPersona().getNombre() + " " + c.getPersona().getApellido())
                    : (c.getRazonSocial() != null ? c.getRazonSocial() : ("Cliente #" + c.getIdCliente()));
            cbClienteDefault.addItem(new ClienteItem(c.getIdCliente(), nombre));
        }

        int idDefault = configService.getInt(
                ConfiguracionService.KEY_VENTAS_CLIENTE_DEFAULT_ID,
                ConfiguracionService.DEFAULT_VENTAS_CLIENTE_DEFAULT_ID
        );
        seleccionarClienteCombo(idDefault);
    }

    private void seleccionarClienteCombo(int idDefault) {
        if (idDefault <= 0) return;
        for (int i = 0; i < cbClienteDefault.getItemCount(); i++) {
            ClienteItem it = cbClienteDefault.getItemAt(i);
            if (it != null && it.id == idDefault) {
                cbClienteDefault.setSelectedIndex(i);
                break;
            }
        }
    }

    private void cargarMetodosPagoCombo() {
        cbMetodoPagoDefault.removeAllItems();
        cbMetodoPagoDefault.addItem(new MetodoPagoItem(0, "Sin default"));

        List<MetodoPago> metodos = metodoPagoControlador.obtenerTodosLosMetodosPago();
        for (MetodoPago mp : metodos) {
            cbMetodoPagoDefault.addItem(new MetodoPagoItem(mp.getIdMetodoPago(), mp.getNombre()));
        }

        int idDefault = configService.getInt(
                ConfiguracionService.KEY_VENTAS_METODO_PAGO_DEFAULT_ID,
                ConfiguracionService.DEFAULT_VENTAS_METODO_PAGO_DEFAULT_ID
        );
        seleccionarMetodoPagoCombo(idDefault);
    }

    private void seleccionarMetodoPagoCombo(int idDefault) {
        if (idDefault <= 0) return;
        for (int i = 0; i < cbMetodoPagoDefault.getItemCount(); i++) {
            MetodoPagoItem it = cbMetodoPagoDefault.getItemAt(i);
            if (it != null && it.id == idDefault) {
                cbMetodoPagoDefault.setSelectedIndex(i);
                break;
            }
        }
    }

    private void guardar() {
        try {
            int ganGlobal = (Integer) spGananciaGlobal.getValue();
            int dec = (Integer) spDecimales.getValue();
                        RedondeoItem rmItem = (RedondeoItem) cbRedondeo.getSelectedItem();
            String rm = (rmItem != null ? rmItem.getCode() : ConfiguracionService.DEFAULT_VENTAS_REDONDEO);

            // Validaciones básicas
            if (ganGlobal < 0 || ganGlobal > 999) {
                JOptionPane.showMessageDialog(this, "El % global debe estar entre 0 y 999.");
                return;
            }
            try {
                RoundingMode.valueOf(rm);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Modo de redondeo inválido.");
                return;
            }

            configService.setInt(ConfiguracionService.KEY_VENTAS_PORC_GANANCIA_GLOBAL, ganGlobal, "Porcentaje de ganancia global (fallback).");
            configService.setInt(ConfiguracionService.KEY_VENTAS_DECIMALES, dec, "Cantidad de decimales en montos de ventas.");
            configService.setString(ConfiguracionService.KEY_VENTAS_REDONDEO, rm, "Modo de redondeo.");

            configService.setBoolean(ConfiguracionService.KEY_VENTAS_PERMITIR_STOCK_NEGATIVO, chkStockNegativo.isSelected(), "Permitir stock negativo.");
            configService.setBoolean(ConfiguracionService.KEY_VENTAS_ABRIR_COMPROBANTE_PDF, chkAbrirPdf.isSelected(), "Abrir comprobante PDF (venta).");
            configService.setBoolean(ConfiguracionService.KEY_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO, chkImprimirAuto.isSelected(), "Imprimir comprobante de venta.");

            ClienteItem cli = (ClienteItem) cbClienteDefault.getSelectedItem();
            MetodoPagoItem mp = (MetodoPagoItem) cbMetodoPagoDefault.getSelectedItem();
            configService.setInt(ConfiguracionService.KEY_VENTAS_CLIENTE_DEFAULT_ID, cli == null ? 0 : cli.id, "Cliente por defecto (venta).");
            configService.setInt(ConfiguracionService.KEY_VENTAS_METODO_PAGO_DEFAULT_ID, mp == null ? 0 : mp.id, "Método de pago por defecto (venta).");

            // Rubros: guardar porcentaje (nullable) + activo
            List<Rubro> rubros = rubroModel.getData();
            for (Rubro r : rubros) {
                rubroControlador.actualizar(r);
            }

            ConfiguracionService.invalidateCache();
            JOptionPane.showMessageDialog(this, "Configuración de ventas guardada.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar Config Ventas: " + ex.getMessage(),
                    "Config Ventas", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Items ----------------

    private static class ClienteItem {
        final int id;
        final String label;
        ClienteItem(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    private static class MetodoPagoItem {
        final int id;
        final String label;
        MetodoPagoItem(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    // ---------------- Table model Rubro ----------------

    
    private static class RedondeoItem {
        private final String code;
        private final String label;

        RedondeoItem(String code, String label) {
            this.code = code;
            this.label = label;
        }

        String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return label;
        }
    }

private static class RubroTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Nombre", "% Ganancia (opcional)", "Activo"};
        private List<Rubro> data = java.util.Collections.emptyList();

        void setData(List<Rubro> rubros) {
            this.data = (rubros == null) ? java.util.Collections.emptyList() : rubros;
            fireTableDataChanged();
        }

        List<Rubro> getData() { return data; }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }
        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 || columnIndex == 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Rubro r = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return r.getIdRubro();
                case 1: return r.getNombre();
                case 2: return r.getPorcentajeGanancia() == null ? "" : r.getPorcentajeGanancia().toPlainString();
                case 3: return r.isActivo();
                default: return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Rubro r = data.get(rowIndex);
            if (columnIndex == 2) {
                String s = (aValue == null) ? "" : aValue.toString().trim();
                if (s.isEmpty()) {
                    r.setPorcentajeGanancia(null);
                } else {
                    try {
                        BigDecimal bd = new BigDecimal(s);
                        r.setPorcentajeGanancia(bd);
                    } catch (Exception e) {
                        // inválido: no cambia
                    }
                }
            } else if (columnIndex == 3) {
                boolean v = false;
                if (aValue instanceof Boolean) v = (Boolean) aValue;
                r.setActivo(v);
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) return Boolean.class;
            return String.class;
        }
    }
}
