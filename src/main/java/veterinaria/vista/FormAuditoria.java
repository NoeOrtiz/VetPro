
package veterinaria.vista;

import javax.swing.JOptionPane;
import veterinaria.util.PermisoUI;
import veterinaria.util.ui.AuditoriaFormBinder;

public class FormAuditoria extends javax.swing.JPanel {

    public FormAuditoria() {
        initComponents();
        PermisoUI.aplicar(this);
        bindAuditoria();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void bindAuditoria() {
        try {
            AuditoriaFormBinder binder = new AuditoriaFormBinder(
                    this,
                    tableAuditoria,
                    (javax.swing.JComboBox) jcbUsuario,
                    (javax.swing.JComboBox) jcbAccion,
                    (javax.swing.JComboBox) jcbEntidad,
                    jDateDesde,
                    jDateHasta,
                    btnBuscar,
                    btnLimpiar,
                    btnVer,
                    btnImprimir,
                    btnImprimirLista
            );
            binder.init();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo inicializar Auditoría: " + ex.getMessage(),
                    "Auditoría",
                    JOptionPane.ERROR_MESSAGE);
        }



    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbAuditoriaDeEventos = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jcbUsuario = new javax.swing.JComboBox<>();
        lbUsuario = new javax.swing.JLabel();
        lbDesde = new javax.swing.JLabel();
        btnImprimir = new javax.swing.JButton();
        btnVer = new javax.swing.JButton();
        jDateDesde = new com.toedter.calendar.JDateChooser();
        txtBusqueda = new javax.swing.JTextField();
        lbBuscar = new javax.swing.JLabel();
        lbBuscarEvento = new javax.swing.JLabel();
        jDateHasta = new com.toedter.calendar.JDateChooser();
        lbHasta = new javax.swing.JLabel();
        jcbAccion = new javax.swing.JComboBox<>();
        lbAccion = new javax.swing.JLabel();
        lbUsuario1 = new javax.swing.JLabel();
        btnBuscar = new javax.swing.JButton();
        jcbEntidad = new javax.swing.JComboBox<>();
        lbAccion1 = new javax.swing.JLabel();
        btnLimpiar = new javax.swing.JButton();
        jpListaAuditoria = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        lbListaServicios = new javax.swing.JLabel();
        btnImprimirLista = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        scroll1 = new javax.swing.JScrollPane();
        tableAuditoria = new veterinaria.vista.table.AutoTable();

        lbAuditoriaDeEventos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbAuditoriaDeEventos.setText("Auditoría de Eventos");

        jcbUsuario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Usuario" }));

        lbUsuario.setText("Usuario: ");

        lbDesde.setText("Desde:");

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        btnVer.setText("Ver ");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        lbBuscarEvento.setText("BUSCAR:");

        lbHasta.setText("Hasta:");

        jcbAccion.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos" }));

        lbAccion.setText("Accion:");

        lbUsuario1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbUsuario1.setText("Filtros:");

        btnBuscar.setText("Buscar");

        jcbEntidad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos" }));

        lbAccion1.setText("Entidad:");

        btnLimpiar.setText("Limpiar");

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbAuditoriaDeEventos)
                                .addGap(0, 950, Short.MAX_VALUE))
                            .addComponent(jSeparator1)))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addComponent(lbUsuario)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(lbBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbBuscarEvento)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnVer)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnImprimir)))))
                .addContainerGap())
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addComponent(jcbUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbDesde)
                            .addComponent(jDateDesde, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbHasta)
                            .addComponent(jDateHasta, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbAccion)
                            .addComponent(jcbAccion, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbAccion1)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(jcbEntidad, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnLimpiar))))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lbUsuario1)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbAuditoriaDeEventos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnImprimir)
                                .addComponent(btnVer))
                            .addComponent(lbBuscarEvento))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbBuscar))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbUsuario1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbUsuario, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lbDesde))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jcbUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jDateDesde, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbHasta)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDateHasta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbAccion)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnBuscar)
                                        .addComponent(btnLimpiar))
                                    .addComponent(jcbAccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addComponent(lbAccion1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbEntidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        lbListaServicios.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaServicios.setText("Lista de Eventos Auditados");

        btnImprimirLista.setText("Imprimir Lista");
        btnImprimirLista.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirListaActionPerformed(evt);
            }
        });

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableAuditoria.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Fecha/Hora", "Usuario", "Acción", "Entidad", "Módulo", "Descripción", "Resultado"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableAuditoria.setMinimumSize(new java.awt.Dimension(848, 220));
        tableAuditoria.setPreferredSize(new java.awt.Dimension(848, 220));
        tableAuditoria.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableAuditoria);

        javax.swing.GroupLayout jpListaAuditoriaLayout = new javax.swing.GroupLayout(jpListaAuditoria);
        jpListaAuditoria.setLayout(jpListaAuditoriaLayout);
        jpListaAuditoriaLayout.setHorizontalGroup(
            jpListaAuditoriaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaAuditoriaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaAuditoriaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpListaAuditoriaLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lbListaServicios)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnImprimirLista))
                    .addComponent(jSeparator3)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 1110, Short.MAX_VALUE)
        );
        jpListaAuditoriaLayout.setVerticalGroup(
            jpListaAuditoriaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaAuditoriaLayout.createSequentialGroup()
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpListaAuditoriaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbListaServicios)
                    .addComponent(btnImprimirLista))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpListaAuditoria, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jpListaAuditoria, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed

    }//GEN-LAST:event_btnImprimirActionPerformed

    private void btnImprimirListaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirListaActionPerformed

    }//GEN-LAST:event_btnImprimirListaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnImprimirLista;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnVer;
    private com.toedter.calendar.JDateChooser jDateDesde;
    private com.toedter.calendar.JDateChooser jDateHasta;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JComboBox<String> jcbAccion;
    private javax.swing.JComboBox<String> jcbEntidad;
    private javax.swing.JComboBox<String> jcbUsuario;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaAuditoria;
    private javax.swing.JLabel lbAccion;
    private javax.swing.JLabel lbAccion1;
    private javax.swing.JLabel lbAuditoriaDeEventos;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbBuscarEvento;
    private javax.swing.JLabel lbDesde;
    private javax.swing.JLabel lbHasta;
    private javax.swing.JLabel lbListaServicios;
    private javax.swing.JLabel lbUsuario;
    private javax.swing.JLabel lbUsuario1;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JTable tableAuditoria;
    private javax.swing.JTextField txtBusqueda;
    // End of variables declaration//GEN-END:variables
}
