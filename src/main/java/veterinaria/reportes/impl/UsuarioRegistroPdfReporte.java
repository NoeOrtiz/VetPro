package veterinaria.reportes.impl;

import veterinaria.entidad.Persona;
import veterinaria.entidad.Usuario;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;

/**
 * Ficha de usuario: toda la información original, con presentación sobria
 * inspirada en las tarjetas del reporte aportado como referencia.
 */
public class UsuarioRegistroPdfReporte extends AbstractPdfReporte {

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
        Usuario usuario = request.get("usuario", Usuario.class);
        if (usuario == null) throw new Exception("Usuario no proporcionado.");
        Persona persona = request.get("persona", Persona.class);
        if (persona == null) {
            String nombre = request.get("nombre", String.class);
            String apellido = request.get("apellido", String.class);
            String dni = request.get("dni", String.class);
            if ((nombre == null || nombre.isBlank())
                    && (apellido == null || apellido.isBlank())
                    && (dni == null || dni.isBlank()))
                throw new Exception("Faltan los datos personales del usuario.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Usuario u = request.get("usuario", Usuario.class);
        Persona p = request.get("persona", Persona.class);

        String nombre = p != null ? safe(p.getNombre()) : safe(request.get("nombre", String.class));
        String apellido = p != null ? safe(p.getApellido()) : safe(request.get("apellido", String.class));
        String dni = p != null ? safe(p.getDni()) : safe(request.get("dni", String.class));
        String telefono = p != null ? safe(p.getTelefono()) : safe(request.get("telefono", String.class));
        String direccion = p != null ? safe(p.getDireccion()) : safe(request.get("direccion", String.class));
        String email = safe(u.getEmail());
        String rol = safe(request.get("rol", String.class));
        String estado = safe(request.get("estado", String.class));
        if (estado.isBlank()) estado = u.isActivo() ? "Activo" : "Inactivo";

        UsuarioPdfPresentacion.resumen(pdf,
                new String[]{"Usuario N°", u.getIdUsuario() == null ? "-" : u.getIdUsuario().toString()},
                new String[]{"Rol", rol},
                new String[]{"Estado", estado});

        UsuarioPdfPresentacion.seccion(pdf, "Datos personales");
        UsuarioPdfPresentacion.campos(pdf, new String[][]{
                {"Nombre y apellido", (nombre + " " + apellido).trim()},
                {"DNI", dni},
                {"Teléfono", telefono},
                {"Dirección", direccion}
        }, 2);

        UsuarioPdfPresentacion.seccion(pdf, "Cuenta de acceso");
        UsuarioPdfPresentacion.campos(pdf, new String[][]{
                {"Usuario", safe(u.getNombreUsuario())},
                {"E-mail", email},
                {"Rol", rol},
                {"Estado", estado}
        }, 2);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
