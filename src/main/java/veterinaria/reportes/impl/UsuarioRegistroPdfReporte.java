package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Persona;
import veterinaria.entidad.Usuario;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class UsuarioRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.USUARIO_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE USUARIO";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Usuario u = request.get("usuario", Usuario.class);
        if (u == null) {
            throw new Exception("Usuario no proporcionado.");
        }
        Persona p = request.get("persona", Persona.class);
        if (p == null) {
            String nombre = request.get("nombre", String.class);
            String apellido = request.get("apellido", String.class);
            String dni = request.get("dni", String.class);
            if ((nombre == null || nombre.isBlank()) && (apellido == null || apellido.isBlank()) && (dni == null || dni.isBlank())) {
                throw new Exception("No se proporcionaron datos de Persona (nombre/apellido/dni).\n" +
                        "Solución: cargar la Persona vinculada al usuario antes de generar el PDF.");
            }
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Usuario u = request.get("usuario", Usuario.class);
        Persona p = request.get("persona", Persona.class);

        String usuarioEmisor = request.get("emisor", String.class);
        String rol = request.get("rol", String.class);

        Object idObj = request.get("idUsuario", Object.class);
        String idUsuario = "";
        if (idObj != null) {
            idUsuario = String.valueOf(idObj);
        } else if (u != null) {
            idUsuario = safe(u.getIdUsuario());
        }

        String nombre = (p != null) ? safe(p.getNombre()) : safe(request.get("nombre", String.class));
        String apellido = (p != null) ? safe(p.getApellido()) : safe(request.get("apellido", String.class));
        String dni = (p != null) ? safe(p.getDni()) : safe(request.get("dni", String.class));
        String telefono = (p != null) ? safe(p.getTelefono()) : safe(request.get("telefono", String.class));
        String direccion = (p != null) ? safe(p.getDireccion()) : safe(request.get("direccion", String.class));

        String nombreUsuario = safe(request.get("nombreUsuario", String.class));
        String email = safe(request.get("email", String.class));

        if ((nombreUsuario == null || nombreUsuario.isBlank()) && u != null) {
            nombreUsuario = safe(u.getNombreUsuario());
        }
        if ((email == null || email.isBlank()) && u != null) {
            email = safe(u.getEmail());
        }

        float x = pdf.getMargin();
        
        if (usuarioEmisor != null && !usuarioEmisor.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + usuarioEmisor);
            pdf.down(12f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
            pdf.down(14f);
        }
        pdf.line();
        
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "ID Usuario: " + safe(idUsuario));
        pdf.down(16f);
        String nyA = (safe(nombre) + " " + safe(apellido)).trim();
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Nombre y apellido: " + nyA);
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "DNI: " + safe(dni));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Teléfono: " + safe(telefono));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Dirección: " + safe(direccion));
        pdf.down(14f);

        pdf.line();

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Usuario: " + safe(nombreUsuario));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "E-mail: " + safe(email));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Rol: " + safe(rol));
        pdf.down(14f);

        pdf.line();

    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
