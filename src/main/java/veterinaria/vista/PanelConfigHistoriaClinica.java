package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.*;
import veterinaria.servicio.ConfiguracionService;
import veterinaria.servicio.HistoriaClinicaPreferencias;

/** Preferencias de visibilidad de eventos, solo para Historia Clínica y sus reportes. */
public class PanelConfigHistoriaClinica extends JPanel {

    private final ConfiguracionService servicio = new ConfiguracionService();
    private final JCheckBox visitas = new JCheckBox("Consultas veterinarias");
    private final JCheckBox laboratorio = new JCheckBox("Laboratorio");
    private final JCheckBox hospitalizacion = new JCheckBox("Hospitalizaciones");
    private final JCheckBox procedimientos = new JCheckBox("Procedimientos");
    private final JCheckBox peluqueria = new JCheckBox("Peluquería");
    private final JCheckBox otros = new JCheckBox("Otros tipos de eventos");

    public PanelConfigHistoriaClinica() {
        setLayout(new BorderLayout(10, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        JPanel cuerpo = new JPanel(new BorderLayout(0, 18));
        JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, 7));
        encabezado.add(new JLabel("EVENTOS INCLUIDOS EN LA HISTORIA CLÍNICA"));
        encabezado.add(new JLabel("Elegí qué actividades se muestran en la pantalla y en los PDF."));
        encabezado.add(new JLabel("Los registros ocultos no se eliminan ni se modifican."));
        cuerpo.add(encabezado, BorderLayout.NORTH);
        JPanel opciones = new JPanel(new GridLayout(0, 2, 28, 18));
        for (JCheckBox check : new JCheckBox[]{
                visitas, laboratorio, hospitalizacion, procedimientos, peluqueria, otros}) {
            check.setOpaque(false);
            opciones.add(check);
        }
        cuerpo.add(opciones, BorderLayout.CENTER);
        add(cuerpo, BorderLayout.NORTH);
        JButton guardar = new JButton("Guardar configuración");
        guardar.addActionListener(e -> guardar());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(guardar);
        add(acciones, BorderLayout.SOUTH);
        cargar();
    }

    public final void cargar() {
        visitas.setSelected(servicio.getBoolean(HistoriaClinicaPreferencias.KEY_VISITAS, true));
        laboratorio.setSelected(servicio.getBoolean(HistoriaClinicaPreferencias.KEY_LABORATORIO, true));
        hospitalizacion.setSelected(servicio.getBoolean(HistoriaClinicaPreferencias.KEY_HOSPITALIZACION, true));
        procedimientos.setSelected(servicio.getBoolean(HistoriaClinicaPreferencias.KEY_PROCEDIMIENTOS, true));
        peluqueria.setSelected(servicio.getBoolean(HistoriaClinicaPreferencias.KEY_PELUQUERIA, false));
        otros.setSelected(servicio.getBoolean(HistoriaClinicaPreferencias.KEY_OTROS, true));
    }

    private void guardar() {
        boolean ok =
                servicio.setBoolean(HistoriaClinicaPreferencias.KEY_VISITAS, visitas.isSelected(),
                        "Incluir consultas veterinarias en la historia clínica");
        ok = servicio.setBoolean(HistoriaClinicaPreferencias.KEY_LABORATORIO, laboratorio.isSelected(),
                        "Incluir laboratorio en la historia clínica") && ok;
        ok = servicio.setBoolean(HistoriaClinicaPreferencias.KEY_HOSPITALIZACION, hospitalizacion.isSelected(),
                        "Incluir hospitalizaciones en la historia clínica") && ok;
        ok = servicio.setBoolean(HistoriaClinicaPreferencias.KEY_PROCEDIMIENTOS, procedimientos.isSelected(),
                        "Incluir procedimientos en la historia clínica") && ok;
        ok = servicio.setBoolean(HistoriaClinicaPreferencias.KEY_PELUQUERIA, peluqueria.isSelected(),
                        "Incluir peluquería en la historia clínica") && ok;
        ok = servicio.setBoolean(HistoriaClinicaPreferencias.KEY_OTROS, otros.isSelected(),
                        "Incluir otros eventos en la historia clínica") && ok;
        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "Configuración guardada. La Historia Clínica aplicará los cambios al volver a consultarla.");
        } else {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron guardar todas las preferencias. Revisá la conexión a MySQL.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
