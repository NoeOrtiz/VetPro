package veterinaria.vista;

import javax.swing.*;
import java.awt.*;
import veterinaria.util.BackupAlertaUtil;
import veterinaria.util.BackupUtil;

public class FormBackUp extends JPanel {

    private final JButton btnGenerar = new JButton("Generar Copia de Seguridad Ahora");
    private final JTextArea txtResultado = new JTextArea(8, 40);

    public FormBackUp() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtResultado.setEditable(false);
        txtResultado.setText("Haga clic en el botón superior para generar un archivo .sql de respaldo de la base de datos.");

        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnGenerar.setPreferredSize(new Dimension(250, 40));
        panelNorte.add(new JLabel("Módulo de Respaldo de Base de Datos"));

        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.add(btnGenerar, BorderLayout.NORTH);
        panelCentro.add(new JScrollPane(txtResultado), BorderLayout.CENTER);

        add(panelNorte, BorderLayout.NORTH);
        add(panelCentro, BorderLayout.CENTER);

        // Evento del botón
        btnGenerar.addActionListener(e -> ejecutarBackup());
    }

    private void ejecutarBackup() {

        try {

            String ruta = BackupUtil.generarBackup();

            BackupAlertaUtil.registrarNuevoBackup();

            txtResultado.setText(
                    "✓ Copia de seguridad realizada correctamente.\n\n"
                    + "Archivo generado:\n"
                    + ruta
            );

            JOptionPane.showMessageDialog(
                    this,
                    "La copia de seguridad se generó correctamente.",
                    "Backup realizado",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {

            txtResultado.setText(
                    "✗ Error al generar la copia de seguridad.\n\n"
                    + ex.getMessage()
            );

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
