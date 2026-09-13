
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
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Cliente c = request.get("cliente", Cliente.class);
        Persona p = request.get("persona", Persona.class);

        String emisor = request.get("emisor", String.class);

        String idCliente = "";
        Object idObj = request.get("idCliente", Object.class);
        if (idObj != null) {
            idCliente = String.valueOf(idObj);
        } else if (c != null) {
            idCliente = safe(c.getIdCliente());
        }

        String nombre = (p != null) ? safe(p.getNombre()) : safe(request.get("nombre", String.class));
        String apellido = (p != null) ? safe(p.getApellido()) : safe(request.get("apellido", String.class));
        String dni = (p != null) ? safe(p.getDni()) : safe(request.get("dni", String.class));
        String telefono = (p != null) ? safe(p.getTelefono()) : safe(request.get("telefono", String.class));
        String direccion = (p != null) ? safe(p.getDireccion()) : safe(request.get("direccion", String.class));

        String email = safe(request.get("email", String.class));
        String razonSocial = safe(request.get("razonSocial", String.class));
        String cuit = safe(request.get("cuit", String.class));

        if ((email == null || email.isBlank()) && c != null) {
            email = safe(c.getEmail());
        }
        if ((razonSocial == null || razonSocial.isBlank()) && c != null) {
            razonSocial = safe(c.getRazonSocial());
        }
        if ((cuit == null || cuit.isBlank()) && c != null) {
            cuit = safe(c.getCuit());
        }

        float x = pdf.getMargin();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "ID Cliente: " + safe(idCliente));
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

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "E-mail: " + safe(email));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Razón social: " + safe(razonSocial));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "CUIT: " + safe(cuit));
        pdf.down(14f);

        pdf.line();

        if (emisor != null && !emisor.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + emisor);
            pdf.down(12f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
            pdf.down(14f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
