package veterinaria.util.ui;

import com.formdev.flatlaf.FlatLaf;
import com.toedter.calendar.JDateChooser;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ContainerEvent;
import javax.swing.JComponent;
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

    private static final String LISTENER_INSTALADO =
            "vetpro.dateChooser.themeListenerInstalled";

    private static void aplicarChooser(JDateChooser chooser) {
        instalarListenerDeFecha(chooser);

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

    /*
     * JTextFieldDateEditor de JCalendar vuelve a asignar su propio foreground
     * cuando cambia la fecha. Por eso no alcanza con tematizar el componente
     * una sola vez al crearlo. Escuchamos el cambio de "date" y restauramos
     * el color del tema después de que JCalendar termina de actualizarlo.
     */
    private static void instalarListenerDeFecha(JDateChooser chooser) {
        Component editor = chooser.getDateEditor().getUiComponent();
        if (!(editor instanceof JComponent jc)) {
            return;
        }
        if (Boolean.TRUE.equals(jc.getClientProperty(LISTENER_INSTALADO))) {
            return;
        }

        jc.putClientProperty(LISTENER_INSTALADO, Boolean.TRUE);
        chooser.getDateEditor().addPropertyChangeListener("date", evt ->
                SwingUtilities.invokeLater(() -> aplicarChooser(chooser)));
    }
}
