package veterinaria.vista.componentes;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Dialog simple para mostrar un mensaje predefinido (WhatsApp), con botones
 * para copiar al portapapeles y cerrar.
 */
public class WhatsAppMensajeDialog extends JDialog {

    private final String texto;

    public WhatsAppMensajeDialog(Window owner, String titulo, String texto) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.texto = (texto != null) ? texto : "";
        init();
    }

    private void init() {
        JTextArea ta = new JTextArea(texto, 10, 58);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setEditable(false);
        ta.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(ta);

        JButton btnCopiar = new JButton("Copiar");
        btnCopiar.addActionListener(e -> copiar());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCopiar);
        botones.add(btnCerrar);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.add(sp, BorderLayout.CENTER);
        root.add(botones, BorderLayout.SOUTH);

        getContentPane().add(root);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void copiar() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto), null);
            JOptionPane.showMessageDialog(this, "Texto copiado.\nPegalo directo en WhatsApp.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo copiar el texto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void mostrar(Window owner, String titulo, String texto) {
        WhatsAppMensajeDialog d = new WhatsAppMensajeDialog(owner, titulo, texto);
        d.setVisible(true);
    }
}
