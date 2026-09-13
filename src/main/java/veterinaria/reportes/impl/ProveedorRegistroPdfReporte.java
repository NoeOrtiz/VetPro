package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Persona;
import veterinaria.entidad.Proveedor;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class ProveedorRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.PROVEEDOR_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE PROVEEDOR";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Proveedor p = request.get("proveedor", Proveedor.class);
        if (p == null) {
            throw new Exception("Proveedor no proporcionado.");
        }

        Persona persona = request.get("persona", Persona.class);
        if (persona == null) {
            String contacto = request.get("contacto", String.class);
            String telefono = request.get("telefono", String.class);
            String direccion = request.get("direccion", String.class);
            if ((contacto == null || contacto.isBlank())
                    && (telefono == null || telefono.isBlank())
                    && (direccion == null || direccion.isBlank())) {
                throw new Exception("No se proporcionaron datos de Persona (contacto/teléfono/dirección).\n"
                        + "Solución: cargar la Persona vinculada al proveedor antes de generar el PDF.");
            }
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Proveedor prov = request.get("proveedor", Proveedor.class);
        Persona persona = request.get("persona", Persona.class);
        String emisor = request.get("emisor", String.class);

        String nombre = (persona != null) ? safe(persona.getNombre()) : "";
        String apellido = (persona != null) ? safe(persona.getApellido()) : "";
        String dni = (persona != null) ? safe(persona.getDni()) : "";
        String telefono = (persona != null) ? safe(persona.getTelefono()) : safe(request.get("telefono", String.class));
        String direccion = (persona != null) ? safe(persona.getDireccion()) : safe(request.get("direccion", String.class));

        String contacto = safe(request.get("contacto", String.class));
        if ((contacto == null || contacto.isBlank())) {
            String nyA = (safe(nombre) + " " + safe(apellido)).trim();
            contacto = nyA;
        }

        String razonSocial = safe(request.get("razonSocial", String.class));
        String cuit = safe(request.get("cuit", String.class));
        String email = safe(request.get("email", String.class));
        String rubro = safe(request.get("rubro", String.class));
        String estado = safe(request.get("estado", String.class));

        if ((razonSocial == null || razonSocial.isBlank()) && prov != null) razonSocial = safe(prov.getRazonSocial());
        if ((cuit == null || cuit.isBlank()) && prov != null) cuit = safe(prov.getCuit());
        if ((email == null || email.isBlank()) && prov != null) email = safe(prov.getEmail());
        if ((rubro == null || rubro.isBlank()) && prov != null) rubro = safe(prov.getRubro());
        if ((estado == null || estado.isBlank()) && prov != null) estado = safe(prov.getEstado());

        float x = pdf.getMargin();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "ID Proveedor: " + safe(prov.getIdProveedor()));
        pdf.down(16f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Razón social: " + safe(razonSocial));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "CUIT: " + safe(cuit));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Rubro: " + safe(rubro));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Estado: " + safe(estado));
        pdf.down(14f);

        pdf.line();

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Contacto: " + safe(contacto));
        pdf.down(14f);
        if (persona != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "DNI: " + safe(dni));
            pdf.down(14f);
        }
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Teléfono: " + safe(telefono));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Dirección: " + safe(direccion));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "E-mail: " + safe(email));
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
