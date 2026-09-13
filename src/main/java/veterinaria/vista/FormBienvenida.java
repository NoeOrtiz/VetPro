
package veterinaria.vista;

import com.formdev.flatlaf.FlatClientProperties;

public class FormBienvenida extends javax.swing.JPanel {

    public FormBienvenida() {
        initComponents();
        //lbl_bienvenidos.putClientProperty(FlatClientProperties.STYLE, ""
          //      + "font:$h1.font");
        
    }
    

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lbl_bienvenida = new javax.swing.JLabel();
        lbl_bienvenidos = new javax.swing.JLabel();

        jPanel1.setLayout(new java.awt.BorderLayout());

        lbl_bienvenida.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_bienvenida.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/bienvenida.png"))); // NOI18N

        lbl_bienvenidos.setFont(new java.awt.Font("Roboto Black", 3, 36)); // NOI18N
        lbl_bienvenidos.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_bienvenidos.setText("Bienvenidos");
        lbl_bienvenidos.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lbl_bienvenida, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(295, 295, 295)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(523, Short.MAX_VALUE))
            .addComponent(lbl_bienvenidos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(73, 73, 73)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_bienvenida, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lbl_bienvenidos)
                .addContainerGap(101, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbl_bienvenida;
    private javax.swing.JLabel lbl_bienvenidos;
    // End of variables declaration//GEN-END:variables
}
