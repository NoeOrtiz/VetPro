
package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class ClienteRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CLIENTE_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE CLIENTE";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Cliente c = request.get("cliente", Cliente.class);
        if (c == null) {
            throw new Exception("Cliente no proporcionado.");
        }
        Persona p = request.get("persona", Persona.class);
        if (p == null) {
            String nombre = request.get("nombre", String.class);
            String apellido = request.get("apellido", String.class);
            String dni = request.get("dni", String.class);
            if ((nombre == null || nombre.isBlank()) && (apellido == null || apellido.isBlank()) && (dni == null || dni.isBlank())) {
                throw new Exception("No se proporcionaron datos de Persona (nombre/apellido/dni).\n" +
                        "Solución: cargar la Persona vinculada al cliente antes de generar el PDF.");
            }
        }
    }

    @Override
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        float y = pdf.getY();
        renderLogo(pdf, (pdf.getPageW() - 160f) / 2f, y - 103f, 160f, 103f);
        pdf.setY(y - 116f);
        String t = titulo();
        pdf.text(PdfStyle.H2_FONT, 14,
                pdf.centerX(pdf.textWidth(PdfStyle.H2_FONT, 14, t)), pdf.getY(), t);
        pdf.down(22f);
        pdf.line();
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Cliente c = request.get("cliente", Cliente.class);
        Persona p = request.get("persona", Persona.class);
        float x = pdf.getMargin() + 10f;
        pdf.down(10f);
        campo(pdf, x, "DATOS PERSONALES", "", true);
        campo(pdf, x, "Nombre y apellido", p == null ? "" :
                safe(p.getNombre()) + " " + safe(p.getApellido()), false);
        campo(pdf, x, "DNI", p == null ? "" : safe(p.getDni()), false);
        campo(pdf, x, "Teléfono", p == null ? "" : safe(p.getTelefono()), false);
        campo(pdf, x, "Dirección", p == null ? "" : safe(p.getDireccion()), false);
        campo(pdf, x, "E-mail", c == null ? "" : safe(c.getEmail()), false);
        campo(pdf, x, "Estado", c != null && c.isActivo() ? "Activo" : "Inactivo", false);
        pdf.down(10f);
        pdf.line();
        pdf.down(12f);
        campo(pdf, x, "DATOS FISCALES Y ADMINISTRATIVOS", "", true);
        campo(pdf, x, "Razón social", c == null ? "" : safe(c.getRazonSocial()), false);
        campo(pdf, x, "CUIT", c == null ? "" : safe(c.getCuit()), false);
        campo(pdf, x, "Fecha de alta", c == null || c.getFechaAlta() == null
                ? "" : c.getFechaAlta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), false);
        pdf.down(10f);
        pdf.line();
    }

    private void campo(PdfDocument pdf, float x, String etiqueta, String valor, boolean seccion) throws Exception {
        if (seccion) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), etiqueta);
            pdf.down(25f);
        } else {
            pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), etiqueta + ":");
            java.util.List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 10,
                    valor, pdf.getUsableWidth() - 165f);
            for (int i = 0; i < lineas.size(); i++) {
                pdf.text(PdfStyle.TXT_FONT, 10, x + 132f, pdf.getY() - i * 14f, lineas.get(i));
            }
            pdf.down(Math.max(1, lineas.size()) * 14f + 9f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
