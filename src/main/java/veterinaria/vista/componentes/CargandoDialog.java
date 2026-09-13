package veterinaria.vista.componentes;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import java.awt.Window;

public class CargandoDialog extends JDialog {

    private JLabel lblMensaje;

    public CargandoDialog(Window parent, String mensaje) {
        super(parent, ModalityType.APPLICATION_MODAL);
        initComponents(mensaje);
    }

    private void initComponents(String mensaje) {
        setUndecorated(true); // Sin bordes ni barra de título, queda flotante
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new MigLayout("wrap 1, align center center, insets 25", "[center]"));

        // Opcional: Si tenés un gif animado circular en tus recursos (ej: "/veterinaria/iconos/loading.gif")
        // JLabel lblIcono = new JLabel(new ImageIcon(getClass().getResource("/veterinaria/iconos/loading.gif")));
        // add(lblIcono);
        // Barra de progreso elegante e indeterminada de FlatLaf (hace el efecto de carga continua)
        JProgressBar spinnerBar = new JProgressBar();
        spinnerBar.setIndeterminate(true);
        add(spinnerBar, "width 150!, height 10!");

        // Texto "Loading..." o personalizado
        lblMensaje = new JLabel(mensaje);
        lblMensaje.putClientProperty(FlatClientProperties.STYLE, "font: bold +1");
        add(lblMensaje, "top 10");

        pack();
        setLocationRelativeTo(getOwner());
    }
}
