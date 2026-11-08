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
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        float y = pdf.getY();
        renderLogo(pdf, (pdf.getPageW() - 160f) / 2f, y - 103f, 160f, 103f);
        pdf.setY(y - 116f);
        String t = titulo();
        pdf.text(PdfStyle.H2_FONT, 14, pdf.centerX(pdf.textWidth(PdfStyle.H2_FONT, 14, t)), pdf.getY(), t);
        pdf.down(22f);
        pdf.line();
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Usuario u = request.get("usuario", Usuario.class);
        Persona p = request.get("persona", Persona.class);
        float x = pdf.getMargin() + 10f;
        pdf.down(10f);
        campo(pdf, x, "DATOS PERSONALES", "", true);
        campo(pdf, x, "Nombre y apellido", (p != null ? safe(p.getNombre()) + " " + safe(p.getApellido()) :
                safe(request.get("nombre", String.class)) + " " + safe(request.get("apellido", String.class))), false);
        campo(pdf, x, "DNI", p != null ? safe(p.getDni()) : safe(request.get("dni", String.class)), false);
        campo(pdf, x, "Teléfono", p != null ? safe(p.getTelefono()) : safe(request.get("telefono", String.class)), false);
        campo(pdf, x, "Dirección", p != null ? safe(p.getDireccion()) : safe(request.get("direccion", String.class)), false);
        pdf.down(10f);
        pdf.line();
        pdf.down(12f);
        campo(pdf, x, "DATOS DE CUENTA", "", true);
        campo(pdf, x, "Usuario", u != null ? safe(u.getNombreUsuario()) : safe(request.get("nombreUsuario", String.class)), false);
        campo(pdf, x, "E-mail", u != null ? safe(u.getEmail()) : safe(request.get("email", String.class)), false);
        campo(pdf, x, "Rol", safe(request.get("rol", String.class)), false);
        String estado = request.get("estado", String.class);
        campo(pdf, x, "Estado", estado == null || estado.isBlank() ? "Sin especificar" : estado, false);
        pdf.down(10f);
        pdf.line();
    }

    private void campo(PdfDocument pdf, float x, String etiqueta, String valor, boolean seccion) throws Exception {
        if (seccion) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), etiqueta);
            pdf.down(25f);
        } else {
            pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), etiqueta + ":");
            float valorX = x + 132f;
            java.util.List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 10, valor, pdf.getUsableWidth() - 165f);
            for (int i = 0; i < lineas.size(); i++) {
                pdf.text(PdfStyle.TXT_FONT, 10, valorX, pdf.getY() - i * 14f, lineas.get(i));
            }
            pdf.down(Math.max(1, lineas.size()) * 14f + 9f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
