package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import veterinaria.servicio.ConfiguracionService;
import veterinaria.servicio.AgendaSlotService;

/**
 * Configuración de Turnos y Horarios.
 *
 * Centraliza:
 * - Peluquería: turnos (mañana/tarde), horarios (cortado), días habilitados y duración.
 * - Laboratorio: días habilitados y duración.
 * - Hospitalización: días habilitados y duración.
 */
public class PanelConfigPeluqueria extends JPanel {

    private final ConfiguracionService configService = new ConfiguracionService();

    private final AgendaSlotService agendaSlotService = new AgendaSlotService();

    private final JLabel lbTitulo = new JLabel("Configuración - Turnos / Horarios");

    // ---------------- Peluquería ----------------
    private final JLabel lbTurnosManana = new JLabel("Turnos por la mañana:");
    private final JLabel lbTurnosTarde = new JLabel("Turnos por la tarde:");

    private final JSpinner spTurnosManana = new JSpinner(new SpinnerNumberModel(5, 0, 200, 1));
    private final JSpinner spTurnosTarde = new JSpinner(new SpinnerNumberModel(5, 0, 200, 1));

    private final JLabel lbMananaDesde = new JLabel("Mañana - Desde (HH:mm):");
    private final JLabel lbMananaHasta = new JLabel("Mañana - Hasta (HH:mm):");
    private final JLabel lbTardeDesde = new JLabel("Tarde  - Desde (HH:mm):");
    private final JLabel lbTardeHasta = new JLabel("Tarde  - Hasta (HH:mm):");

    private final JTextField txtMananaDesde = new JTextField(8);
    private final JTextField txtMananaHasta = new JTextField(8);
    private final JTextField txtTardeDesde = new JTextField(8);
    private final JTextField txtTardeHasta = new JTextField(8);

    private final JLabel lbDuracion = new JLabel("Duración del turno (min):");
    private final JSpinner spDuracion = new JSpinner(new SpinnerNumberModel(
            ConfiguracionService.DEFAULT_DURACION_TURNO_PELUQUERIA_MIN, 5, 240, 5));

    private final JLabel lbDias = new JLabel("Días habilitados:");
    private final JCheckBox cbLun = new JCheckBox("Lun");
    private final JCheckBox cbMar = new JCheckBox("Mar");
    private final JCheckBox cbMie = new JCheckBox("Mié");
    private final JCheckBox cbJue = new JCheckBox("Jue");
    private final JCheckBox cbVie = new JCheckBox("Vie");
    private final JCheckBox cbSab = new JCheckBox("Sáb");
    private final JCheckBox cbDom = new JCheckBox("Dom");

    // ---------------- Laboratorio ----------------
    private final JLabel lbLabDias = new JLabel("Días habilitados:");
    private final JCheckBox labLun = new JCheckBox("Lun");
    private final JCheckBox labMar = new JCheckBox("Mar");
    private final JCheckBox labMie = new JCheckBox("Mié");
    private final JCheckBox labJue = new JCheckBox("Jue");
    private final JCheckBox labVie = new JCheckBox("Vie");
    private final JCheckBox labSab = new JCheckBox("Sáb");
    private final JCheckBox labDom = new JCheckBox("Dom");

    private final JLabel lbDuracionLab = new JLabel("Duración del turno (min):");
    private final JSpinner spDuracionLab = new JSpinner(new SpinnerNumberModel(
            ConfiguracionService.DEFAULT_DURACION_TURNO_LABORATORIO_MIN, 5, 480, 5));

    // ---------------- Hospitalización ----------------
    private final JLabel lbHospDias = new JLabel("Días habilitados:");
    private final JCheckBox hosLun = new JCheckBox("Lun");
    private final JCheckBox hosMar = new JCheckBox("Mar");
    private final JCheckBox hosMie = new JCheckBox("Mié");
    private final JCheckBox hosJue = new JCheckBox("Jue");
    private final JCheckBox hosVie = new JCheckBox("Vie");
    private final JCheckBox hosSab = new JCheckBox("Sáb");
    private final JCheckBox hosDom = new JCheckBox("Dom");

    private final JLabel lbDuracionHosp = new JLabel("Duración del turno (min):");
    private final JSpinner spDuracionHosp = new JSpinner(new SpinnerNumberModel(
            ConfiguracionService.DEFAULT_DURACION_TURNO_HOSPITALIZACION_MIN, 5, 480, 5));

    // ---------------- Acciones ----------------
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnRestaurar = new JButton("Restaurar por defecto");

    // Micro-ayuda (UI)
    private final JLabel helpPelu = buildHelpLabel(
            "La <b>duración</b> define cuánto dura cada turno al reservar (ej: 30 min). "
            + "Los <b>horarios</b> y la <b>cantidad de turnos</b> por franja determinan los horarios disponibles.");
    private final JLabel helpLab = buildHelpLabel(
            "La <b>duración</b> define el tamaño de cada bloque de agenda (ej: 15/30 min). "
            + "Con esto se calculan los turnos disponibles según el horario de atención del servicio.");
    private final JLabel helpHosp = buildHelpLabel(
            "La <b>duración</b> define el tamaño de cada bloque de agenda (ej: 30/60 min). "
            + "Con esto se calculan los turnos disponibles según el horario de atención del servicio.");

    public PanelConfigPeluqueria() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lbTitulo.setFont(lbTitulo.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        // Mejoras visuales: scroll + secciones con borde titulado
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        contenido.add(buildPeluqueriaSection());
        contenido.add(Box.createVerticalStrut(10));
        contenido.add(buildLaboratorioSection());
        contenido.add(Box.createVerticalStrut(10));
        contenido.add(buildHospitalizacionSection());

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(btnRestaurar);
        acciones.add(btnGuardar);

        add(lbTitulo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);

        // Tamaños consistentes
        spTurnosManana.setPreferredSize(new java.awt.Dimension(140, 26));
        spTurnosTarde.setPreferredSize(new java.awt.Dimension(140, 26));
        spDuracion.setPreferredSize(new java.awt.Dimension(140, 26));
        spDuracionLab.setPreferredSize(new java.awt.Dimension(140, 26));
        spDuracionHosp.setPreferredSize(new java.awt.Dimension(140, 26));

        txtMananaDesde.setToolTipText("Ej: 08:00");
        txtMananaHasta.setToolTipText("Ej: 12:00");
        txtTardeDesde.setToolTipText("Ej: 15:00");
        txtTardeHasta.setToolTipText("Ej: 19:00");

        wireEvents();
        cargar();
    }

    // ---------------- UI Builders ----------------

    private JPanel buildPeluqueriaSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Peluquería",
                TitledBorder.LEFT,
                TitledBorder.TOP));

        GridBagConstraints c = baseGbc();
        int row = 0;

        // Turnos
        addRow(panel, c, row++, lbTurnosManana, spTurnosManana);
        addRow(panel, c, row++, lbTurnosTarde, spTurnosTarde);

        addSeparator(panel, c, row++);

        // Horarios
        addRow(panel, c, row++, lbMananaDesde, txtMananaDesde);
        addRow(panel, c, row++, lbMananaHasta, txtMananaHasta);
        addRow(panel, c, row++, lbTardeDesde, txtTardeDesde);
        addRow(panel, c, row++, lbTardeHasta, txtTardeHasta);

        addSeparator(panel, c, row++);

        // Duración + días
        addRow(panel, c, row++, lbDuracion, spDuracion);
        addHelpRow(panel, c, row++, helpPelu);
        addRow(panel, c, row++, lbDias, buildDiasPanel(cbLun, cbMar, cbMie, cbJue, cbVie, cbSab, cbDom));

        return panel;
    }

    private JPanel buildLaboratorioSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Laboratorio",
                TitledBorder.LEFT,
                TitledBorder.TOP));

        GridBagConstraints c = baseGbc();
        int row = 0;

        addRow(panel, c, row++, lbLabDias, buildDiasPanel(labLun, labMar, labMie, labJue, labVie, labSab, labDom));
        addRow(panel, c, row++, lbDuracionLab, spDuracionLab);
        addHelpRow(panel, c, row++, helpLab);

        return panel;
    }

    private JPanel buildHospitalizacionSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Hospitalización",
                TitledBorder.LEFT,
                TitledBorder.TOP));

        GridBagConstraints c = baseGbc();
        int row = 0;

        addRow(panel, c, row++, lbHospDias, buildDiasPanel(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom));
        addRow(panel, c, row++, lbDuracionHosp, spDuracionHosp);
        addHelpRow(panel, c, row++, helpHosp);

        return panel;
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, java.awt.Component left, java.awt.Component right) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(left, c);

        c.gridx = 1;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 1;
        panel.add(right, c);
    }

    private void addSeparator(JPanel panel, GridBagConstraints c, int row) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(new JSeparator(), c);
    }

    private static JLabel buildHelpLabel(String htmlText) {
        JLabel l = new JLabel("<html><span style='color:#666666;'>" + htmlText + "</span></html>");
        l.setForeground(new Color(102, 102, 102));
        l.setFont(l.getFont().deriveFont(11f));
        return l;
    }

    private void addHelpRow(JPanel panel, GridBagConstraints c, int row, JLabel helpLabel) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(helpLabel, c);
    }

    private JPanel buildDiasPanel(JCheckBox... checks) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (JCheckBox cb : checks) p.add(cb);
        return p;
    }

    // ---------------- Events ----------------

    private void wireEvents() {
        btnGuardar.addActionListener(e -> guardar());
        btnRestaurar.addActionListener(e -> {
            // Peluquería
            spTurnosManana.setValue(ConfiguracionService.DEFAULT_TURNOS_PELUQUERIA_MANANA);
            spTurnosTarde.setValue(ConfiguracionService.DEFAULT_TURNOS_PELUQUERIA_TARDE);

            txtMananaDesde.setText(ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_MANANA_DESDE);
            txtMananaHasta.setText(ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_MANANA_HASTA);
            txtTardeDesde.setText(ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_TARDE_DESDE);
            txtTardeHasta.setText(ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_TARDE_HASTA);

            spDuracion.setValue(ConfiguracionService.DEFAULT_DURACION_TURNO_PELUQUERIA_MIN);
            setDiasFromConfig(cbLun, cbMar, cbMie, cbJue, cbVie, cbSab, cbDom,
                    ConfiguracionService.DEFAULT_DIAS_HABILITADOS_PELUQUERIA);

            // Laboratorio
            spDuracionLab.setValue(ConfiguracionService.DEFAULT_DURACION_TURNO_LABORATORIO_MIN);
            setDiasFromConfig(labLun, labMar, labMie, labJue, labVie, labSab, labDom,
                    ConfiguracionService.DEFAULT_DIAS_HABILITADOS_LABORATORIO);

            // Hospitalización
            spDuracionHosp.setValue(ConfiguracionService.DEFAULT_DURACION_TURNO_HOSPITALIZACION_MIN);
            setDiasFromConfig(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom,
                    ConfiguracionService.DEFAULT_DIAS_HABILITADOS_HOSPITALIZACION);

            guardar();
        });
    }

    /** Re-lee valores desde la fuente de configuración (DB). */
    public final void cargar() {
        // Peluquería
        spTurnosManana.setValue(configService.getInt(
                ConfiguracionService.KEY_TURNOS_PELUQUERIA_MANANA,
                ConfiguracionService.DEFAULT_TURNOS_PELUQUERIA_MANANA));

        spTurnosTarde.setValue(configService.getInt(
                ConfiguracionService.KEY_TURNOS_PELUQUERIA_TARDE,
                ConfiguracionService.DEFAULT_TURNOS_PELUQUERIA_TARDE));

        txtMananaDesde.setText(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_MANANA_DESDE,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_MANANA_DESDE));

        txtMananaHasta.setText(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_MANANA_HASTA,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_MANANA_HASTA));

        txtTardeDesde.setText(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_TARDE_DESDE,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_TARDE_DESDE));

        txtTardeHasta.setText(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_TARDE_HASTA,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_TARDE_HASTA));

        spDuracion.setValue(configService.getInt(
                ConfiguracionService.KEY_DURACION_TURNO_PELUQUERIA_MIN,
                ConfiguracionService.DEFAULT_DURACION_TURNO_PELUQUERIA_MIN));

        setDiasFromConfig(cbLun, cbMar, cbMie, cbJue, cbVie, cbSab, cbDom,
                configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_PELUQUERIA,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_PELUQUERIA));

        // Laboratorio
        spDuracionLab.setValue(configService.getInt(
                ConfiguracionService.KEY_DURACION_TURNO_LABORATORIO_MIN,
                ConfiguracionService.DEFAULT_DURACION_TURNO_LABORATORIO_MIN));

        setDiasFromConfig(labLun, labMar, labMie, labJue, labVie, labSab, labDom,
                configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_LABORATORIO,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_LABORATORIO));

        // Hospitalización
        spDuracionHosp.setValue(configService.getInt(
                ConfiguracionService.KEY_DURACION_TURNO_HOSPITALIZACION_MIN,
                ConfiguracionService.DEFAULT_DURACION_TURNO_HOSPITALIZACION_MIN));

        setDiasFromConfig(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom,
                configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_HOSPITALIZACION,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_HOSPITALIZACION));
    }

    private void guardar() {
        // Validación básica Peluquería
        int turnosManana = (Integer) spTurnosManana.getValue();
        int turnosTarde = (Integer) spTurnosTarde.getValue();

        if (turnosManana < 0 || turnosTarde < 0) {
            JOptionPane.showMessageDialog(this, "Los turnos por franja no pueden ser negativos.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ((turnosManana + turnosTarde) < 1) {
            JOptionPane.showMessageDialog(this, "Debe habilitar al menos 1 turno entre mañana y tarde.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int duracion = (Integer) spDuracion.getValue();
        if (duracion < 5) {
            JOptionPane.showMessageDialog(this, "La duración debe ser mayor o igual a 5 minutos.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String manDesdeTxt = txtMananaDesde.getText().trim();
        String manHastaTxt = txtMananaHasta.getText().trim();
        String tarDesdeTxt = txtTardeDesde.getText().trim();
        String tarHastaTxt = txtTardeHasta.getText().trim();

        LocalTime manDesde = parseHora(manDesdeTxt);
        LocalTime manHasta = parseHora(manHastaTxt);
        LocalTime tarDesde = parseHora(tarDesdeTxt);
        LocalTime tarHasta = parseHora(tarHastaTxt);

        if (turnosManana > 0) {
            if (manDesde == null || manHasta == null || !manHasta.isAfter(manDesde)) {
                JOptionPane.showMessageDialog(this,
                        "Horario de mañana inválido. Use formato HH:mm y verifique que 'hasta' sea mayor que 'desde'.",
                        "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        if (turnosTarde > 0) {
            if (tarDesde == null || tarHasta == null || !tarHasta.isAfter(tarDesde)) {
                JOptionPane.showMessageDialog(this,
                        "Horario de tarde inválido. Use formato HH:mm y verifique que 'hasta' sea mayor que 'desde'.",
                        "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        if (turnosManana > 0 && turnosTarde > 0) {
            if (manHasta != null && tarDesde != null && manHasta.isAfter(tarDesde)) {
                JOptionPane.showMessageDialog(this,
                        "Los horarios se superponen. Ajuste para que el horario de mañana termine antes (o igual) al inicio de la tarde.",
                        "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String diasPelu = buildDiasConfig(cbLun, cbMar, cbMie, cbJue, cbVie, cbSab, cbDom);
        if (diasPelu.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe habilitar al menos un día de atención en Peluquería.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Laboratorio
        int durLab = (Integer) spDuracionLab.getValue();
        String diasLab = buildDiasConfig(labLun, labMar, labMie, labJue, labVie, labSab, labDom);
        if (diasLab.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe habilitar al menos un día para Laboratorio.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Hospitalización
        int durHosp = (Integer) spDuracionHosp.getValue();
        String diasHosp = buildDiasConfig(hosLun, hosMar, hosMie, hosJue, hosVie, hosSab, hosDom);
        if (diasHosp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe habilitar al menos un día para Hospitalización.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Persistencia
        boolean ok = true;

        ok &= configService.setInt(ConfiguracionService.KEY_TURNOS_PELUQUERIA_MANANA, turnosManana,
                "Turnos máximos por la mañana");
        ok &= configService.setInt(ConfiguracionService.KEY_TURNOS_PELUQUERIA_TARDE, turnosTarde,
                "Turnos máximos por la tarde");
        ok &= configService.setInt(ConfiguracionService.KEY_DURACION_TURNO_PELUQUERIA_MIN, duracion,
                "Duración estándar del turno (minutos)");

        ok &= configService.setString(ConfiguracionService.KEY_HORARIO_PELUQUERIA_MANANA_DESDE,
                (manDesde != null ? manDesde.toString() : ""),
                "Horario de atención (mañana) - desde (HH:mm)");
        ok &= configService.setString(ConfiguracionService.KEY_HORARIO_PELUQUERIA_MANANA_HASTA,
                (manHasta != null ? manHasta.toString() : ""),
                "Horario de atención (mañana) - hasta (HH:mm)");
        ok &= configService.setString(ConfiguracionService.KEY_HORARIO_PELUQUERIA_TARDE_DESDE,
                (tarDesde != null ? tarDesde.toString() : ""),
                "Horario de atención (tarde) - desde (HH:mm)");
        ok &= configService.setString(ConfiguracionService.KEY_HORARIO_PELUQUERIA_TARDE_HASTA,
                (tarHasta != null ? tarHasta.toString() : ""),
                "Horario de atención (tarde) - hasta (HH:mm)");

        ok &= configService.setString(ConfiguracionService.KEY_DIAS_HABILITADOS_PELUQUERIA, diasPelu,
                "Días habilitados para Peluquería (1=Lun ... 7=Dom)");

        // Laboratorio
        ok &= configService.setString(ConfiguracionService.KEY_DIAS_HABILITADOS_LABORATORIO, diasLab,
                "Días habilitados para Laboratorio (1=Lun ... 7=Dom)");
        ok &= configService.setInt(ConfiguracionService.KEY_DURACION_TURNO_LABORATORIO_MIN, durLab,
                "Duración estándar del turno de Laboratorio (minutos)");

        // Hospitalización
        ok &= configService.setString(ConfiguracionService.KEY_DIAS_HABILITADOS_HOSPITALIZACION, diasHosp,
                "Días habilitados para Hospitalización (1=Lun ... 7=Dom)");
        ok &= configService.setInt(ConfiguracionService.KEY_DURACION_TURNO_HOSPITALIZACION_MIN, durHosp,
                "Duración estándar del turno de Hospitalización (minutos)");

        if (ok) {
            int borrados = 0;
            try {
                // Evita que queden "sobrantes" de horarios viejos al cambiar turnos/horarios/duración.
                borrados = agendaSlotService.resetSlotsLibresDesdeHoy();
            } catch (Exception ignore) {
                // no bloqueamos el guardado de configuración si falla el purge
            }

            String msg = "Configuración guardada.";
            if (borrados > 0) {
                msg += "Se limpiaron " + borrados + " slots libres para regenerar la grilla con la nueva configuración.";
            }
            JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la configuración.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalTime parseHora(String hhmm) {
        try {
            if (hhmm == null) return null;
            String v = hhmm.trim();
            if (v.isEmpty()) return null;
            return LocalTime.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private void setDiasFromConfig(JCheckBox lun, JCheckBox mar, JCheckBox mie, JCheckBox jue,
                                   JCheckBox vie, JCheckBox sab, JCheckBox dom, String csv) {
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
            try {
                d = Integer.parseInt(p.trim());
            } catch (Exception ignore) {
                continue;
            }
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

    private String buildDiasConfig(JCheckBox lun, JCheckBox mar, JCheckBox mie, JCheckBox jue,
                                  JCheckBox vie, JCheckBox sab, JCheckBox dom) {
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
