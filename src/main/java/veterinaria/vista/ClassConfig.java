package veterinaria.vista;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

public class ClassConfig extends JPanel {

    private final JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    private final PanelRolesPermisos panelRolesPermisos = new PanelRolesPermisos();
    private final PanelMetodosPago panelMetodosPago = new PanelMetodosPago();
    private final PanelConfigVentas panelConfigVentas = new PanelConfigVentas();
    private final PanelConfigPeluqueria panelConfigPeluqueria = new PanelConfigPeluqueria();
    private final PanelTiposCitaPeluqueria panelTiposCitaPeluqueria = new PanelTiposCitaPeluqueria();
    private final PanelRubros panelRubros = new PanelRubros();

    public ClassConfig() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabs.addTab("Roles y Permisos", panelRolesPermisos);
        tabs.addTab("Métodos de Pago", panelMetodosPago);
        tabs.addTab("Ventas", panelConfigVentas);
        tabs.addTab("Turnos y Horarios", panelConfigPeluqueria);
        tabs.addTab("Tipos de Cita", panelTiposCitaPeluqueria);
        tabs.addTab("Rubros / Stock", panelRubros);

        tabs.addChangeListener(e -> cargarSolapaSeleccionada());
        add(tabs, BorderLayout.CENTER);

        tabs.setSelectedIndex(0);
    }

    private void cargarSolapaSeleccionada() {
        switch (tabs.getSelectedIndex()) {
            case 1:
                panelMetodosPago.refrescarTabla();
                break;
            case 2:
                panelConfigVentas.cargar();
                break;
            case 3:
                panelConfigPeluqueria.cargar();
                break;
            case 5:
                panelRubros.refrescarTabla();
                break;
            default:
                break;
        }
    }
}
