package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import veterinaria.servicio.ConfiguracionService;

/**
 * Configuración por servicio: qué días se atiende Peluquería / Laboratorio / Hospitalización.
 *
 * Nota importante:
 * - La agenda (AgendaSlot) es ÚNICA por veterinario.
 * - Esta pantalla NO define la grilla de horarios; sólo define en qué días cada servicio puede ofrecer turnos.
 * (Deprecated) Esta pantalla quedó obsoleta al centralizarse la configuración en "Turnos / Horarios".
 */
public class PanelConfigServiciosAgenda extends JPanel {

    private final ConfiguracionService configService = new ConfiguracionService();

    private final JLabel lbTitulo = new JLabel("Configuración - Días por Servicio");

    // --- Peluquería ---
    private final JLabel lbPeluq = new JLabel("Peluquería - días habilitados:");
    private final JCheckBox pelLun = new JCheckBox("Lun");
    private final JCheckBox pelMar = new JCheckBox("Mar");
    private final JCheckBox pelMie = new JCheckBox("Mié");
    private final JCheckBox pelJue = new JCheckBox("Jue");
    private final JCheckBox pelVie = new JCheckBox("Vie");
    private final JCheckBox pelSab = new JCheckBox("Sáb");
    private final JCheckBox pelDom = new JCheckBox("Dom");

    // --- Laboratorio ---
    private final JLabel lbLab = new JLabel("Laboratorio - días habilitados:");
    private final JCheckBox labLun = new JCheckBox("Lun");
    private final JCheckBox labMar = new JCheckBox("Mar");
    private final JCheckBox labMie = new JCheckBox("Mié");
    private final JCheckBox labJue = new JCheckBox("Jue");
    private final JCheckBox labVie = new JCheckBox("Vie");
    private final JCheckBox labSab = new JCheckBox("Sáb");
    private final JCheckBox labDom = new JCheckBox("Dom");

    // --- Hospitalización ---
    private final JLabel lbHosp = new JLabel("Hospitalización - días habilitados:");
    private final JCheckBox hosLun = new JCheckBox("Lun");
    private final JCheckBox hosMar = new JCheckBox("Mar");
    private final JCheckBox hosMie = new JCheckBox("Mié");
    private final JCheckBox hosJue = new JCheckBox("Jue");
    private final JCheckBox hosVie = new JCheckBox("Vie");
    private final JCheckBox hosSab = new JCheckBox("Sáb");
    private final JCheckBox hosDom = new JCheckBox("Dom");

    private final JButton btnGuardar = new JButton("Guardar");

    public PanelConfigServiciosAgenda() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lbTitulo.setFont(lbTitulo.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(6, 6, 6, 6);
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;

        int row = 0;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        form.add(lbTitulo, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(new JSeparator(), c);
        row++;

        // Peluquería
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        form.add(lbPeluq, c);
        c.gridx = 1; c.gridy = row; c.weightx = 1;
        form.add(buildDiasPanel(pelLun, pelMar, pelMie, pelJue, pelVie, pelSab, pelDom), c);
        row++;

        // Laboratorio
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        form.add(lbLab, c);
        c.gridx = 1; c.gridy = row; c.weightx = 1;
        form.add(buildDiasPanel(labLun, labMar, labMie, labJue, labVie, labSab, labDom), c);
        row++;

        // Hospitalización
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        form.add(lbHosp, c);
        c.gridx = 1; c.gridy = row; c.weightx = 1;
        form.add(buildDiasPanel(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom), c);
        row++;

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(btnGuardar);

        add(form, BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);

        btnGuardar.addActionListener(e -> guardar());
        cargar();
    }

    private JPanel buildDiasPanel(JCheckBox... cbs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (JCheckBox cb : cbs) p.add(cb);
        return p;
    }

    public final void cargar() {
        setDiasFromConfig(pelLun, pelMar, pelMie, pelJue, pelVie, pelSab, pelDom,
                configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_PELUQUERIA,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_PELUQUERIA
                ));

        setDiasFromConfig(labLun, labMar, labMie, labJue, labVie, labSab, labDom,
                configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_LABORATORIO,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_LABORATORIO
                ));

        setDiasFromConfig(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom,
                configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_HOSPITALIZACION,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_HOSPITALIZACION
                ));

        // Sin configuración adicional para laboratorio.
    }

    private void guardar() {
        String diasPelu = buildDiasConfig(pelLun, pelMar, pelMie, pelJue, pelVie, pelSab, pelDom);
        String diasLab = buildDiasConfig(labLun, labMar, labMie, labJue, labVie, labSab, labDom);
        String diasHosp = buildDiasConfig(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom);

        if (diasPelu.isEmpty() && diasLab.isEmpty() && diasHosp.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe habilitar al menos un día en algún servicio.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok1 = configService.setString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_PELUQUERIA,
                diasPelu,
                "Peluquería - días habilitados (1=Lun ... 7=Dom)"
        );
        boolean ok2 = configService.setString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_LABORATORIO,
                diasLab,
                "Laboratorio - días habilitados (1=Lun ... 7=Dom)"
        );
        boolean ok3 = configService.setString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_HOSPITALIZACION,
                diasHosp,
                "Hospitalización - días habilitados (1=Lun ... 7=Dom)"
        );
        if (ok1 && ok2 && ok3) {
            JOptionPane.showMessageDialog(this, "Configuración guardada.", "OK", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la configuración.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDiasFromConfig(JCheckBox lun, JCheckBox mar, JCheckBox mie, JCheckBox jue, JCheckBox vie, JCheckBox sab, JCheckBox dom,
                                   String csv) {
        lun.setSelected(false);
        mar.setSelected(false);
        mie.setSelected(false);
        jue.setSelected(false);
        vie.setSelected(false);
        sab.setSelected(false);
        dom.setSelected(false);

        if (csv == null || csv.trim().isEmpty()) return;
        String[] parts = csv.split(",");
        for (String p : parts) {
            int d;
            try { d = Integer.parseInt(p.trim()); } catch (Exception ignore) { continue; }
            switch (d) {
                case 1: lun.setSelected(true); break;
                case 2: mar.setSelected(true); break;
                case 3: mie.setSelected(true); break;
                case 4: jue.setSelected(true); break;
                case 5: vie.setSelected(true); break;
                case 6: sab.setSelected(true); break;
                case 7: dom.setSelected(true); break;
                default: break;
            }
        }
    }

    private String buildDiasConfig(JCheckBox lun, JCheckBox mar, JCheckBox mie, JCheckBox jue, JCheckBox vie, JCheckBox sab, JCheckBox dom) {
        List<Integer> days = new ArrayList<>();
        if (lun.isSelected()) days.add(DayOfWeek.MONDAY.getValue());
        if (mar.isSelected()) days.add(DayOfWeek.TUESDAY.getValue());
        if (mie.isSelected()) days.add(DayOfWeek.WEDNESDAY.getValue());
        if (jue.isSelected()) days.add(DayOfWeek.THURSDAY.getValue());
        if (vie.isSelected()) days.add(DayOfWeek.FRIDAY.getValue());
        if (sab.isSelected()) days.add(DayOfWeek.SATURDAY.getValue());
        if (dom.isSelected()) days.add(DayOfWeek.SUNDAY.getValue());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(days.get(i));
        }
        return sb.toString();
    }
}
