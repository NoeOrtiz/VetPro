
package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ClassConfig extends JPanel {

    private final JButton btnRolesPermisos = new JButton("Roles - Permisos");
    private final JButton btnMetodosPago = new JButton("Métodos de Pago");
    private final JButton btnConfigVentas = new JButton("Configurar Ventas");
    private final JButton btnPeluqueria = new JButton("Turnos - Horarios");
    private final JButton btnTiposCitaPeluqueria = new JButton("Tipos de Cita (Peluquería)");
    private final JButton btnRubros = new JButton("Rubros (Stock)");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);


    private final PanelRolesPermisos panelRolesPermisos = new PanelRolesPermisos();
    private final PanelMetodosPago panelMetodosPago = new PanelMetodosPago();
    private final PanelConfigVentas panelConfigVentas = new PanelConfigVentas();
    private final PanelConfigPeluqueria panelConfigPeluqueria = new PanelConfigPeluqueria();
    private final PanelTiposCitaPeluqueria panelTiposCitaPeluqueria = new PanelTiposCitaPeluqueria();
    private final PanelRubros panelRubros = new PanelRubros();
    public ClassConfig() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRolesPermisos.setPreferredSize(new Dimension(170, 32));
        btnMetodosPago.setPreferredSize(new Dimension(170, 32));
        btnConfigVentas.setPreferredSize(new Dimension(170, 32));
        btnPeluqueria.setPreferredSize(new Dimension(170, 32));
        top.add(btnRolesPermisos);
        top.add(btnMetodosPago);
        top.add(btnConfigVentas);
        top.add(btnPeluqueria);
        top.add(btnTiposCitaPeluqueria);
        top.add(btnRubros);
// Cards
        cardPanel.add(panelRolesPermisos, "ROLES");
        cardPanel.add(panelMetodosPago, "PAGOS");
        cardPanel.add(panelConfigVentas, "VENTAS");
        cardPanel.add(panelConfigPeluqueria, "TURNOS_HORARIOS");
        cardPanel.add(panelTiposCitaPeluqueria, "TIPOS_CITA_PELUQUERIA");
        cardPanel.add(panelRubros, "RUBROS");
add(top, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        wireEvents();

        // default
        cardLayout.show(cardPanel, "ROLES");
    }

    private void wireEvents() {
        btnRolesPermisos.addActionListener(e -> cardLayout.show(cardPanel, "ROLES"));
        btnMetodosPago.addActionListener(e -> {
            panelMetodosPago.refrescarTabla();
            cardLayout.show(cardPanel, "PAGOS");
        });
        btnConfigVentas.addActionListener(e -> {
            cardLayout.show(cardPanel, "VENTAS");
            panelConfigVentas.cargar();
        });
        btnPeluqueria.addActionListener(e -> {
            panelConfigPeluqueria.cargar();
            cardLayout.show(cardPanel, "TURNOS_HORARIOS");
        });
        btnTiposCitaPeluqueria.addActionListener(e -> {
            cardLayout.show(cardPanel, "TIPOS_CITA_PELUQUERIA");
        });

        btnRubros.addActionListener(e -> {
            panelRubros.refrescarTabla();
            cardLayout.show(cardPanel, "RUBROS");
        });
    }
}

