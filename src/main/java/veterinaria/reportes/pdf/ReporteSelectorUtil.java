package veterinaria.reportes.pdf;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class ReporteSelectorUtil {

// Definimos un Enum interno para representar la opción elegida de forma limpia
    public enum Formato {
        PDF,
        XLS
    }

    /**
     * Muestra la ventana para configurar y elegir el formato del reporte.
     *
     * * @param padre Componente visual padre (generalmente "this" desde el
     * formulario).
     * @return El Formato seleccionado (PDF o XLS), o null si el usuario canceló
     * la acción.
     */
    public static Formato solicitarFormato(Component padre) {
        // 1. Crear el panel con el diseño y estilos alineados
        JPanel panelConfiguracion = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JLabel lblFormato = new JLabel("Formato de Salida:");
        lblFormato.setFont(lblFormato.getFont().deriveFont(Font.BOLD, 12f));

        // Botones de opción
        JRadioButton rbtnPdf = new JRadioButton("PDF", true); // PDF por defecto
        JRadioButton rbtnXls = new JRadioButton("XLS");

        ButtonGroup grupoFormatos = new ButtonGroup();
        grupoFormatos.add(rbtnPdf);
        grupoFormatos.add(rbtnXls);

        panelConfiguracion.add(lblFormato);
        panelConfiguracion.add(rbtnPdf);
        panelConfiguracion.add(rbtnXls);

        Object[] opcionesBotones = {"Aceptar", "Cancelar"};

        // 2. Mostrar el diálogo nativo de FlatLaf
        int respuesta = JOptionPane.showOptionDialog(
                padre,
                panelConfiguracion,
                "Configurar Reporte",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                opcionesBotones,
                opcionesBotones[0]
        );

        // 3. Retornar la respuesta como Enum o null si canceló
        if (respuesta == JOptionPane.OK_OPTION) {
            if (rbtnPdf.isSelected()) {
                return Formato.PDF;
            } else if (rbtnXls.isSelected()) {
                return Formato.XLS;
            }
        }

        return null; // El usuario cerró o canceló la ventana
    }
}
