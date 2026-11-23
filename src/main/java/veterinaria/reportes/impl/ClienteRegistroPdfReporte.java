package veterinaria.reportes.impl;

import java.time.format.DateTimeFormatter;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;

public class ClienteRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter FECHA_ALTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        renderHeaderConLogo(pdf, 110f, 110f, true);
    }

    @Override
    protected boolean mostrarFechaEnPie() {
        return false;
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Cliente cliente = request.get("cliente", Cliente.class);
        Persona persona = request.get("persona", Persona.class);
        String nombre = persona == null ? request.get("nombre", String.class) : persona.getNombre();
        String apellido = persona == null ? request.get("apellido", String.class) : persona.getApellido();
        String dni = persona == null ? request.get("dni", String.class) : safe(persona.getDni());
        String telefono = persona == null ? request.get("telefono", String.class) : persona.getTelefono();
        String direccion = persona == null ? request.get("direccion", String.class) : persona.getDireccion();

        UsuarioPdfPresentacion.resumen(pdf,
                new String[]{"Cliente N.º", safe(cliente.getIdCliente())},
                new String[]{"Estado", cliente.isActivo() ? "Activo" : "Inactivo"});
        UsuarioPdfPresentacion.seccion(pdf, "Datos personales");
        UsuarioPdfPresentacion.campos(pdf, new String[][]{
                {"Nombre y apellido", (safe(nombre) + " " + safe(apellido)).trim()},
                {"DNI", dni},
                {"Teléfono", telefono},
                {"Correo electrónico", cliente.getEmail()},
                {"Dirección", direccion}
        }, 2);
        UsuarioPdfPresentacion.seccion(pdf, "Datos fiscales y administrativos");
        UsuarioPdfPresentacion.campos(pdf, new String[][]{
                {"Razón social", cliente.getRazonSocial()},
                {"CUIT", cliente.getCuit()},
                {"Fecha de alta", cliente.getFechaAlta() == null
                        ? "" : cliente.getFechaAlta().format(FECHA_ALTA)}
        }, 2);
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
