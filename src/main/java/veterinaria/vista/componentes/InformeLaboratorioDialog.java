package veterinaria.vista.componentes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Laboratorio;
import veterinaria.entidad.Mascota;

/**
 * Dialog simple para visualizar el informe completo de un registro de Laboratorio.
 * No depende del GUI Builder (evita romper .form).
 */
public class InformeLaboratorioDialog extends JDialog {
    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JTextArea ta = new JTextArea();

    public InformeLaboratorioDialog(Window owner, Laboratorio lab) {
        super(owner, "Informe de Laboratorio", ModalityType.APPLICATION_MODAL);

        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setText(buildText(lab));
        ta.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel south = new JPanel();
        south.add(btnCerrar);

        setLayout(new BorderLayout());
        add(sp, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(650, 520));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void mostrar(Window owner, Laboratorio lab) {
        InformeLaboratorioDialog d = new InformeLaboratorioDialog(owner, lab);
        d.setVisible(true);
    }

    private String buildText(Laboratorio lab) {
        if (lab == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Nº Solicitud de Análisis: ").append(n(lab.getIdLaboratorio())).append("\n");

        Cliente c = lab.getCliente();
        if (c != null && c.getPersona() != null) {
            sb.append("Cliente: ")
              .append(n(c.getPersona().getNombre())).append(" ")
              .append(n(c.getPersona().getApellido())).append("\n");
        }
        
        Mascota m = lab.getMascota();
        if (m != null) {
            sb.append("Paciente: ").append(n(m.getNombre())).append("\n");
        }

        sb.append("Veterinario asignado: ")
                .append(n(
                        lab.getVeterinario() != null && lab.getVeterinario().getPersona() != null
                        ? lab.getVeterinario().getPersona().getApellido() + " "
                        + lab.getVeterinario().getPersona().getNombre()
                        : null
                ))
                .append("\n\n");
        sb.append("Usuario gestión: ").append(n(lab.getUsuarioGestion())).append("\n");
        sb.append("Fecha y hora: ").append(LocalDateTime.now().format(DTF_EMISION)).append("\n\n");

        if (lab.getFechaExtraccion() != null) {
            sb.append("Fecha de turno para extracción de muestra: ").append(lab.getFechaExtraccion().format(DF));
            if (lab.getHoraExtraccion() != null) {
                sb.append(" ").append(lab.getHoraExtraccion().toString());
            }
            sb.append("\n");
        }else{
            sb.append("Fecha de extracción de muestra: ").append("\n");
        }
        if (lab.getFechaEnvio() != null) {
            sb.append("Fecha de envío a laboratorio: ").append(lab.getFechaEnvio().format(DF)).append("\n");
        }else{
            sb.append("Fecha de envío a laboratorio: ").append("\n");
        }
        if (lab.getFechaRecepcion() != null) {
            sb.append("Fecha de recepción del análisis: ").append(lab.getFechaRecepcion().format(DF)).append("\n");
        } else {
            sb.append("Fecha de recepción del análisis: ").append("\n");
        }

        sb.append("Estado: ").append(n(lab.getEstado())).append("\n\n");

        sb.append("Motivo de extracción: ").append(n(lab.getMotivoExtraccion())).append("\n");
        sb.append("Tipo de análisis: ").append(n(lab.getTipoAnalisis())).append("\n\n");

        sb.append("Informe / Diagnóstico:\n");
        sb.append("----------------------------------------\n");
        sb.append(n(lab.getDiagnostico())).append("\n");

        return sb.toString();
    }

    private String n(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
