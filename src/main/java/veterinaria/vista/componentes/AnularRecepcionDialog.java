package veterinaria.vista.componentes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Diálogo PRO para pedir Motivo de Anulación (obligatorio).
 * Devuelve el motivo (trim) o null si cancela.
 */
public class AnularRecepcionDialog extends JDialog {

    private final JTextArea taMotivo = new JTextArea(6, 46);
    private final JLabel lblCount = new JLabel("0/255", SwingConstants.RIGHT);
    private final JButton btnOk = new JButton("Anular");
    private final JButton btnCancel = new JButton("Cancelar");

    private String motivo = null;

    public static String pedirMotivo(Window parent, String infoLinea, String usuarioLinea) {
        AnularRecepcionDialog d = new AnularRecepcionDialog(parent, infoLinea, usuarioLinea);
        d.setVisible(true);
        return d.motivo;
    }

    private AnularRecepcionDialog(Window parent, String infoLinea, String usuarioLinea) {
        super(parent, "Motivo de anulación", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel head = new JPanel(new BorderLayout(6, 6));
        JLabel lblInfo = new JLabel(infoLinea == null ? "" : infoLinea);
        JLabel lblUsuario = new JLabel(usuarioLinea == null ? "" : usuarioLinea);
        head.add(lblInfo, BorderLayout.NORTH);
        head.add(lblUsuario, BorderLayout.SOUTH);
        root.add(head, BorderLayout.NORTH);

        taMotivo.setLineWrap(true);
        taMotivo.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(taMotivo);
        sp.setPreferredSize(new Dimension(520, 140));
        root.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(6, 6));
        south.add(lblCount, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(btnCancel);
        actions.add(btnOk);
        south.add(actions, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(parent);

        btnOk.setEnabled(false);

        taMotivo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange(); }

            private void onChange() {
                String t = taMotivo.getText() == null ? "" : taMotivo.getText();
                if (t.length() > 255) {
                    taMotivo.setText(t.substring(0, 255));
                    t = taMotivo.getText();
                }
                lblCount.setText(t.length() + "/255");
                btnOk.setEnabled(!t.trim().isEmpty());
            }
        });

        btnCancel.addActionListener(e -> {
            motivo = null;
            dispose();
        });

        btnOk.addActionListener(e -> {
            String t = taMotivo.getText() == null ? "" : taMotivo.getText().trim();
            if (t.isEmpty()) {
                btnOk.setEnabled(false);
                return;
            }
            motivo = (t.length() > 255) ? t.substring(0, 255) : t;
            dispose();
        });
    }
}
