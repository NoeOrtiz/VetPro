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
import veterinaria.entidad.Hospitalizacion;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;

/**
 * Dialog simple para visualizar un registro completo de Hospitalización.
 * (Sin GUI Builder, para no romper .form)
 */
public class RegistroHospitalizacionDialog extends JDialog {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DTF_ALTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JTextArea ta = new JTextArea();

    public RegistroHospitalizacionDialog(Window owner, Hospitalizacion h) {
        super(owner, "Registro de Hospitalización", ModalityType.APPLICATION_MODAL);

        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setText(buildText(h));
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

        setPreferredSize(new Dimension(680, 560));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void mostrar(Window owner, Hospitalizacion h) {
        RegistroHospitalizacionDialog d = new RegistroHospitalizacionDialog(owner, h);
        d.setVisible(true);
    }

    private String buildText(Hospitalizacion h) {
        if (h == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Nº Hospitalización: ").append(n(h.getIdHospitalizacion())).append("\n\n");

        Cliente c = h.getCliente();
        if (c != null && c.getPersona() != null) {
            sb.append("Cliente: ")
              .append(n(c.getPersona().getNombre())).append(" ")
              .append(n(c.getPersona().getApellido())).append("\n");
        }

        Mascota m = h.getMascota();
        if (m != null) {
            sb.append("Paciente: ").append(n(m.getNombre()));
            String especie = (m.getEspecie() != null) ? m.getEspecie().trim() : "";
            String raza = (m.getRaza() != null) ? m.getRaza().trim() : "";
            if (!especie.isBlank() || !raza.isBlank()) {
                sb.append(" (")
                  .append(!especie.isBlank() ? especie : "")
                  .append(!especie.isBlank() && !raza.isBlank() ? "/" : "")
                  .append(!raza.isBlank() ? raza : "")
                  .append(")");
            }
            sb.append("\n");
        }

        Usuario v = h.getVeterinario();
        String vetTxt = "";
        if (v != null) {
            try {
                if (v.getPersona() != null) {
                    vetTxt = (n(v.getPersona().getApellido()) + " " + n(v.getPersona().getNombre())).trim();
                }
            } catch (Exception ignore) {
            }
        }
        sb.append("Veterinario: ").append(n(vetTxt)).append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("Usuario gestión: ").append(n(nombreVisibleUsuarioGestion(h.getUsuarioGestion()))).append("\n");
        sb.append("Emisión: ").append(LocalDateTime.now().format(DTF_EMISION)).append("\n");
        sb.append("--------------------------------------------------------------------------------\n");

        if (h.getFechaIngreso() != null) {
            sb.append("Fecha ingreso: ").append(h.getFechaIngreso().format(DF));
            if (h.getHora() != null) {
                sb.append(" ").append(h.getHora().toString());
            }
            sb.append("\n");
        } else {
            sb.append("Fecha ingreso: \n");
        }

        if (h.getFechaAlta() != null) {
            sb.append("Fecha alta: ").append(h.getFechaAlta().format(DTF_ALTA)).append("\n");
        } else {
            sb.append("Fecha alta: \n");
        }

        sb.append("Estado: ").append(n(h.getEstado())).append("\n");

        sb.append("Motivo: ").append(n(h.getMotivo())).append("\n\n");

        sb.append("Diagnóstico:\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(n(h.getDiagnostico())).append("\n\n");

        sb.append("Tratamiento:\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(n(h.getTratamiento())).append("\n");

        
        // ================= PROCEDIMIENTOS ASOCIADOS =================
        try {
            veterinaria.persistencia.ProcedimientoDAO pdao = new veterinaria.persistencia.ProcedimientoDAO();
            java.util.List<veterinaria.entidad.Procedimiento> lista =
                    pdao.obtenerPorHospitalizacion(h.getIdHospitalizacion());

            if (lista != null && !lista.isEmpty()) {
                sb.append("\n\nProcedimientos asociados:\n");
                sb.append("--------------------------------------------------------------------------------\n");
                for (veterinaria.entidad.Procedimiento p : lista) {
                    sb.append("• Fecha: ").append(p.getFechaIngreso() != null ? p.getFechaIngreso().toString() : "")
                      .append(" Hora: ").append(p.getHora() != null ? p.getHora().toString() : "")
                      .append("\n");
                    sb.append("  Procedimiento: ").append(p.getProcedimiento() != null ? p.getProcedimiento() : "").append("\n");
                    sb.append("  Motivo: ").append(p.getMotivo() != null ? p.getMotivo() : "").append("\n");
                    if (p.getObservaciones() != null && !p.getObservaciones().isBlank()) {
                        sb.append("  Observaciones: ").append(p.getObservaciones()).append("\n");
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            // No interrumpir visualización del registro si falla consulta
        }

return sb.toString();
    }

    private String n(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String nombreVisibleUsuarioGestion(Usuario u) {
        if (u == null) return "";
        try {
            if (u.getPersona() != null) {
                String ap = (u.getPersona().getApellido() != null) ? u.getPersona().getApellido().trim() : "";
                String no = (u.getPersona().getNombre() != null) ? u.getPersona().getNombre().trim() : "";
                String full = (ap + " " + no).trim();
                if (!full.isBlank()) return full;
            }
            if (u.getNombreUsuario() != null && !u.getNombreUsuario().isBlank()) return u.getNombreUsuario();
        } catch (Exception ignore) {
        }
        return "";
    }
}
