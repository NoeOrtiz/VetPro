package veterinaria.vista;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import raven.tabbed.TabbedPaneCustom;

/** Configuración con pestañas DJ-Raven, sin iconos. */
public class ClassConfig extends JPanel {

    private final TabbedPaneCustom tabs = new TabbedPaneCustom();

    private final PanelRolesPermisos panelRolesPermisos = new PanelRolesPermisos();
    private final PanelMetodosPago panelMetodosPago = new PanelMetodosPago();
    private final PanelConfigVentas panelConfigVentas = new PanelConfigVentas();
    private final PanelConfigPeluqueria panelConfigPeluqueria = new PanelConfigPeluqueria();
    private final PanelTiposCitaPeluqueria panelTiposCitaPeluqueria = new PanelTiposCitaPeluqueria();
    private final PanelRubros panelRubros = new PanelRubros();

    public ClassConfig() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tabs.addTab("Roles - Permisos", panelRolesPermisos);
        tabs.addTab("Métodos de Pago", panelMetodosPago);
        tabs.addTab("Configurar Ventas", panelConfigVentas);
        tabs.addTab("Turnos - Horarios", panelConfigPeluqueria);
        tabs.addTab("Tipos de Cita (Peluquería)", panelTiposCitaPeluqueria);
        tabs.addTab("Rubros (Stock)", panelRubros);

        tabs.addChangeListener(e -> {
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
        });

        add(tabs, BorderLayout.CENTER);
        tabs.setSelectedIndex(0);
    }
}
