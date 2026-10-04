package veterinaria.util.ui;

import com.formdev.flatlaf.FlatLaf;
import com.toedter.calendar.JDateChooser;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ContainerEvent;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;

/**
 * Integración global de JCalendar/JDateChooser con Light/Dark de VetPRO.
 * JCalendar 1.4 fija colores propios en el editor interno y no siempre hereda
 * el foreground de FlatLaf.
 */
public final class DateChooserThemeSupport {

    private static boolean instalado;

    private DateChooserThemeSupport() {
    }

    public static synchronized void instalar() {
        if (instalado) return;
        instalado = true;

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof ContainerEvent ce
                    && ce.getID() == ContainerEvent.COMPONENT_ADDED) {
                Component child = ce.getChild();
                SwingUtilities.invokeLater(() -> aplicar(child));
            }
        }, AWTEvent.CONTAINER_EVENT_MASK);
    }

    public static void aplicarEnVentanasAbiertas() {
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            aplicar(window);
            window.repaint();
        }
    }

    public static void aplicar(Component component) {
        if (component == null) return;

        if (component instanceof JDateChooser chooser) {
            aplicarChooser(chooser);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                aplicar(child);
            }
        }
    }

    private static void aplicarChooser(JDateChooser chooser) {
        Color foreground = FlatLaf.isLafDark()
                ? new Color(240, 240, 240)
                : new Color(60, 60, 60);

        chooser.setForeground(foreground);
        Component editor = chooser.getDateEditor().getUiComponent();
        if (editor != null) {
            editor.setForeground(foreground);
            if (editor instanceof JFormattedTextField field) {
                field.setForeground(foreground);
                field.setDisabledTextColor(foreground);
                field.setSelectedTextColor(foreground);
                field.setCaretColor(foreground);
            }
            editor.repaint();
        }
        chooser.repaint();
    }
}
