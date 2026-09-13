package veterinaria.vista.componentes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;

import veterinaria.entidad.Auditoria;
import veterinaria.entidad.Usuario;

public class AuditoriaDetalleDialog extends JDialog {

    public AuditoriaDetalleDialog(JFrame parent, Auditoria a) {
        super(parent, "Detalle de Auditoría", true);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String fecha = "";
        if (a.getFechaHora() != null) {
            // Como ya es LocalDateTime, usamos el formateador directo
            fecha = a.getFechaHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        Usuario u = a.getUsuario();

        header.add(new JLabel("Fecha/Hora: " + fecha));
        header.add(new JLabel("Usuario: " + (u != null ? u.getNombreUsuario() : "N/D")));
        header.add(new JLabel("Acción: " + safe(a.getAccion())));
        header.add(new JLabel("Entidad: " + safe(a.getEntidad()) + (a.getEntidadId() != null ? (" #" + a.getEntidadId()) : "")));
        header.add(new JLabel("Módulo: " + safe(a.getModulo())));
        header.add(new JLabel("Resultado: " + safe(a.getResultado())));
        add(header, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        StringBuilder sb = new StringBuilder();
        sb.append("Descripción:\n").append(safe(a.getDescripcion())).append("\n\n");

        if (notEmpty(a.getDatosAntes())) {
            sb.append("Datos antes:\n").append(a.getDatosAntes()).append("\n\n");
        }
        if (notEmpty(a.getDatosDespues())) {
            sb.append("Datos después:\n").append(a.getDatosDespues()).append("\n\n");
        }
        if (notEmpty(a.getErrorDetalle())) {
            sb.append("Detalle de error:\n").append(a.getErrorDetalle()).append("\n\n");
        }

        area.setText(sb.toString());

        add(new JScrollPane(area), BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());

        JPanel footer = new JPanel();
        footer.add(cerrar);
        add(footer, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(700, 500));
        pack();
        setLocationRelativeTo(parent);
    }

    private boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
