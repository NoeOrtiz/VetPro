package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import veterinaria.entidad.Rol;
import veterinaria.persistencia.PermisoDAO;
import veterinaria.persistencia.RolDAO;
import veterinaria.util.Constantes.TipoPermiso;
import veterinaria.util.ManejoTablas;
import veterinaria.util.PermisoUI;

public class PanelRolesPermisos extends JPanel {

    // UI
    private final JLabel lbTitulo = new JLabel("Configuración - Permisos por Rol");
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cbRol = new JComboBox<>(new String[]{"Seleccione rol"});
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"Seleccione tipo"});

    private final JTable tablePermisos;
    private final DefaultTableModel model;

    private final JTextField txtNuevoNombre = new JTextField();
    private final JTextField txtNuevaDesc = new JTextField();
    private final JComboBox<String> cbNuevoTipo = new JComboBox<>(
            new String[]{TipoPermiso.FORMULARIO.toString(), TipoPermiso.FUNCIONALIDAD.toString()}
    );

    private final JButton btnRefrescar = new JButton("Refrescar");
    private final JButton btnGuardarCambios = new JButton("Guardar cambios");
    private final JButton btnRevocar = new JButton("Revocar (Quitar) seleccionado");
    private final JButton btnCrearYAsignar = new JButton("Crear permiso + Asignar al rol");

    // DAOs
    private final RolDAO rolDAO = new RolDAO();
    private final PermisoDAO permisoDAO = new PermisoDAO();

    // helpers
    private final Map<Integer, String> rolesMap = new LinkedHashMap<>();

    // Evita recargas duplicadas cuando cambiamos combos desde código (UX)
    private boolean internalChange = false;

    public PanelRolesPermisos() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lbTitulo.setFont(lbTitulo.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        // Tabla
        model = new DefaultTableModel(new Object[][]{}, new String[]{"ID", "Nombre", "Descripción", "Tipo", "Acceso"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Sólo el checkbox de acceso es editable
                return column == 4;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class;
                if (columnIndex == 4) return Boolean.class;
                return String.class;
            }
        };
        tablePermisos = new JTable(model);
        tablePermisos.setRowHeight(22);

        // Ocultar ID
        try {
            javax.swing.table.TableColumn colId = tablePermisos.getColumnModel().getColumn(0);
            colId.setMinWidth(0);
            colId.setMaxWidth(0);
            colId.setPreferredWidth(0);
            colId.setResizable(false);
        } catch (Exception ignore) {
        }

        // Armado UI
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        wireEvents();
        cargarCombos();

        // Estado inicial
        actualizarEstadoAcciones();

        // Filtro de tabla (busca por Nombre/Descripción/Tipo)
        ManejoTablas.aplicarFiltroYResaltado(tablePermisos, txtBuscar, Arrays.asList(1, 2, 3));

        // RBAC
        PermisoUI.aplicar(this);
    }

    private void actualizarEstadoAcciones() {
        boolean rolOk = getIdRolSeleccionado() > 0;
        btnRefrescar.setEnabled(rolOk);
        btnGuardarCambios.setEnabled(rolOk);
        btnRevocar.setEnabled(rolOk);
        btnCrearYAsignar.setEnabled(rolOk);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.add(lbTitulo, BorderLayout.WEST);
        return header;
    }

    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(buildTopFilters(), BorderLayout.NORTH);
        panel.add(new JScrollPane(tablePermisos), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTopFilters() {
        JPanel p = new JPanel();
        p.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(4, 4, 4, 4);
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        // Row 0
        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Rol:"), c);

        c.gridx = 1; c.gridy = 0; c.weightx = 0.25;
        p.add(cbRol, c);

        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        p.add(new JLabel("Tipo:"), c);

        c.gridx = 3; c.gridy = 0; c.weightx = 0.20;
        p.add(cbTipo, c);

        c.gridx = 4; c.gridy = 0; c.weightx = 0;
        p.add(btnRefrescar, c);

        // Row 1
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        p.add(new JLabel("Buscar:"), c);

        c.gridx = 1; c.gridy = 1; c.gridwidth = 4; c.weightx = 1;
        txtBuscar.setPreferredSize(new Dimension(240, 26));
        p.add(txtBuscar, c);

        c.gridwidth = 1;
        return p;
    }

    private JPanel buildBottom() {
        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout(10, 10));

        bottom.add(buildActionsBar(), BorderLayout.NORTH);
        bottom.add(buildCreatePermissionPanel(), BorderLayout.CENTER);

        return bottom;
    }

    private JPanel buildActionsBar() {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        p.add(btnRevocar);
        p.add(btnGuardarCambios);
        return p;
    }

    private JPanel buildCreatePermissionPanel() {
        JPanel box = new JPanel(new java.awt.GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder("Crear permiso y asignarlo al rol seleccionado"));
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(4, 4, 4, 4);
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        box.add(new JLabel("Nombre:"), c);

        c.gridx = 1; c.gridy = 0; c.weightx = 0.35;
        box.add(txtNuevoNombre, c);

        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        box.add(new JLabel("Tipo:"), c);

        c.gridx = 3; c.gridy = 0; c.weightx = 0.20;
        box.add(cbNuevoTipo, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        box.add(new JLabel("Descripción:"), c);

        c.gridx = 1; c.gridy = 1; c.gridwidth = 3; c.weightx = 1;
        box.add(txtNuevaDesc, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 4; c.weightx = 1;
        c.fill = java.awt.GridBagConstraints.NONE;
        c.anchor = java.awt.GridBagConstraints.EAST;
        box.add(btnCrearYAsignar, c);

        return box;
    }

    private void wireEvents() {
        btnRefrescar.addActionListener(e -> recargarTabla());
        btnGuardarCambios.addActionListener(e -> guardarCambios());
        btnRevocar.addActionListener(e -> revocarSeleccionado());
        btnCrearYAsignar.addActionListener(e -> crearYAsignar());

        // UX: al cambiar de rol, por defecto mostramos TODOS los permisos (sin filtrar)
        cbRol.addActionListener(e -> {
            if (internalChange) return;
            internalChange = true;
            try {
                cbTipo.setSelectedIndex(0);
                txtBuscar.setText("");
            } finally {
                internalChange = false;
            }
            actualizarEstadoAcciones();
            recargarTabla();
        });

        cbTipo.addActionListener(e -> {
            if (internalChange) return;
            recargarTabla();
        });
    }

    private void cargarCombos() {
        cbTipo.removeAllItems();
        cbTipo.addItem("Seleccione tipo");
        cbTipo.addItem(TipoPermiso.FORMULARIO.toString());
        cbTipo.addItem(TipoPermiso.FUNCIONALIDAD.toString());

        cbRol.removeAllItems();
        cbRol.addItem("Seleccione rol");
        rolesMap.clear();
        rolesMap.put(0, "Seleccione rol");

        List<Rol> roles = rolDAO.obtenerRoles();
        for (Rol r : roles) {
            cbRol.addItem(r.getNombreRol());
            rolesMap.put(r.getIdRol(), r.getNombreRol());
        }

        cbRol.setSelectedIndex(0);
        cbTipo.setSelectedIndex(0);
        txtBuscar.setText("");
        actualizarEstadoAcciones();
    }

    private int getIdRolSeleccionado() {
        String nombre = (String) cbRol.getSelectedItem();
        if (nombre == null) return 0;
        for (Map.Entry<Integer, String> e : rolesMap.entrySet()) {
            if (nombre.equals(e.getValue())) {
                return e.getKey();
            }
        }
        return 0;
    }

    private String getTipoSeleccionado() {
        String tipo = (String) cbTipo.getSelectedItem();
        if (tipo == null || tipo.equals("Seleccione tipo")) return null;
        return tipo;
    }

    private void recargarTabla() {
        actualizarEstadoAcciones();
        int idRol = getIdRolSeleccionado();
        if (idRol <= 0) {
            limpiarTabla();
            return;
        }

        String tipo = getTipoSeleccionado();
        List<Object[]> rows = permisoDAO.obtenerPermisosConAccesoPorRolYTipo(idRol, tipo);
        SwingUtilities.invokeLater(() -> {
            limpiarTabla();
            for (Object[] r : rows) {
                Integer id = (Integer) r[0];
                String nombre = String.valueOf(r[1]);
                String desc = String.valueOf(r[2]);
                String t = String.valueOf(r[3]);
                Boolean acceso = (Boolean) r[4];
                model.addRow(new Object[]{id, nombre, desc, t, acceso});
            }
        });
    }

    private void limpiarTabla() {
        model.setRowCount(0);
    }

    private void guardarCambios() {
        int idRol = getIdRolSeleccionado();
        if (idRol <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un rol.", "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            for (int i = 0; i < model.getRowCount(); i++) {
                int idPermiso = (Integer) model.getValueAt(i, 0);
                boolean acceso = Boolean.TRUE.equals(model.getValueAt(i, 4));
                permisoDAO.setAccesoRolPermiso(idRol, idPermiso, acceso);
            }
            JOptionPane.showMessageDialog(this, "Cambios guardados correctamente.", "Permisos", JOptionPane.INFORMATION_MESSAGE);
            recargarTabla();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar cambios: " + ex.getMessage(), "Permisos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void revocarSeleccionado() {
        int idRol = getIdRolSeleccionado();
        if (idRol <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un rol.", "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int viewRow = tablePermisos.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un permiso en la tabla.", "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tablePermisos.convertRowIndexToModel(viewRow);
        int idPermiso = (Integer) model.getValueAt(modelRow, 0);
        String nombre = String.valueOf(model.getValueAt(modelRow, 1));

        int ok = JOptionPane.showConfirmDialog(this,
                "¿Revocar el permiso '" + nombre + "' para este rol?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            permisoDAO.setAccesoRolPermiso(idRol, idPermiso, false);
            model.setValueAt(Boolean.FALSE, modelRow, 4);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al revocar: " + ex.getMessage(), "Permisos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void crearYAsignar() {
        int idRol = getIdRolSeleccionado();
        if (idRol <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un rol.", "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombre = txtNuevoNombre.getText() != null ? txtNuevoNombre.getText().trim() : "";
        String desc = txtNuevaDesc.getText() != null ? txtNuevaDesc.getText().trim() : "";
        String tipo = (String) cbNuevoTipo.getSelectedItem();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el nombre del permiso.", "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (desc.isEmpty()) {
            desc = "Permiso: " + nombre;
        }
        if (tipo == null || tipo.trim().isEmpty()) {
            tipo = TipoPermiso.FORMULARIO.toString();
        }

        try {
            permisoDAO.crearPermisoSiNoExisteYAsignar(idRol, nombre, desc, tipo, true);
            txtNuevoNombre.setText("");
            txtNuevaDesc.setText("");
            JOptionPane.showMessageDialog(this, "Permiso creado/asignado correctamente.", "Permisos", JOptionPane.INFORMATION_MESSAGE);
            recargarTabla();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear/asignar: " + ex.getMessage(), "Permisos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
