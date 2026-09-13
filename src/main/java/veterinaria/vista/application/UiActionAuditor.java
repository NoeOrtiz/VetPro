package veterinaria.vista.application;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import veterinaria.servicio.AuditoriaService;

/**
 * Auditoría centralizada de acciones internas en la UI.
 *
 * Objetivo:
 *  - Registrar "acciones internas" dentro de formularios (clicks en botones y menús)
 *    sin tener que instrumentar cada ActionListener en cada Form.
 *
 * Implementación:
 *  - Se instala un AWTEventListener global para ACTION_EVENT_MASK.
 *  - Filtra eventos ruidosos (Timer, combobox, textfields) y se queda con botones/menús.
 *  - No bloquea la UI si falla auditoría.
 *
 * Nota:
 *  - AuditoriaService ya valida sesión activa, por lo que este listener puede quedar
 *    instalado siempre (solo registra post-login).
 */
public final class UiActionAuditor {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    // Deduplicación básica para evitar dobles eventos casi simultáneos
    private static volatile long lastTs = 0L;
    private static volatile String lastKey = "";

    private UiActionAuditor() {
    }

    public static void install(AuditoriaService auditoriaService) {
        if (auditoriaService == null) {
            return;
        }
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!(event instanceof ActionEvent)) {
                    return;
                }
                ActionEvent ae = (ActionEvent) event;
                Object src = ae.getSource();
                if (!(src instanceof Component)) {
                    return;
                }

                // Filtrado por tipo de componente (evitar ruido)
                // - Permitimos: AbstractButton (incluye JButton, JToggleButton, etc.) y JMenuItem.
                // - Ignoramos: eventos de Timer, JComboBox, JTextField, etc.
                if (!(src instanceof AbstractButton) && !(src instanceof JMenuItem)) {
                    return;
                }

                Component c = (Component) src;

                String formName = resolveFormName(c);
                String control = resolveControlLabel(src, ae);
                String actionCmd = safe(ae.getActionCommand());

                String key = formName + "|" + control + "|" + actionCmd;
                long now = System.currentTimeMillis();
                if (key.equals(lastKey) && (now - lastTs) < 400) {
                    return;
                }
                lastKey = key;
                lastTs = now;

                String descripcion = "Acción UI en " + formName + ": " + control;
                if (!actionCmd.isEmpty() && !actionCmd.equals(control)) {
                    descripcion += " (cmd=" + actionCmd + ")";
                }

                try {
                    auditoriaService.registrar(
                            "UI_ACTION",
                            "UI",
                            null,
                            "UI",
                            descripcion,
                            AuditoriaService.RESULT_OK,
                            null,
                            null,
                            null
                    );
                } catch (Exception ex) {
                    System.err.println("[AUDITORIA][UI_ACTION] " + ex.getMessage());
                }
            }
        }, AWTEvent.ACTION_EVENT_MASK);
    }

    private static String resolveFormName(Component c) {
        // Intenta encontrar el panel/form contenedor cuyo nombre empiece por "Form"
        Component current = c;
        while (current != null) {
            String simple = current.getClass().getSimpleName();
            if (simple != null && simple.startsWith("Form")) {
                return simple;
            }
            current = current.getParent();
        }
        // Fallback: ventana
        Component w = SwingUtilities.getWindowAncestor(c);
        if (w != null) {
            return w.getClass().getSimpleName();
        }
        return "(sin-form)";
    }

    private static String resolveControlLabel(Object src, ActionEvent ae) {
        try {
            if (src instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) src;
                String t = safe(b.getText());
                if (!t.isEmpty()) {
                    return t;
                }
                String n = safe(b.getName());
                if (!n.isEmpty()) {
                    return n;
                }
            }
            if (src instanceof JMenuItem) {
                JMenuItem mi = (JMenuItem) src;
                String t = safe(mi.getText());
                if (!t.isEmpty()) {
                    return t;
                }
                String n = safe(mi.getName());
                if (!n.isEmpty()) {
                    return n;
                }
            }
        } catch (Exception ignore) {
        }
        // Fallback: action command o clase
        String cmd = safe(ae.getActionCommand());
        return !cmd.isEmpty() ? cmd : src.getClass().getSimpleName();
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
