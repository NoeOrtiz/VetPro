package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class HospitalizacionRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DTF_ALTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.HOSPITALIZACION_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE HOSPITALIZACIÓN";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Hospitalizacion h = request.get("hospitalizacion", Hospitalizacion.class);
        if (h == null) {
            throw new Exception("Hospitalización no proporcionada.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Hospitalizacion h = request.get("hospitalizacion", Hospitalizacion.class);
        String usuario = request.get("usuario", String.class);

        float x = pdf.getMargin();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Nº Hospitalización: " + safe(h.getIdHospitalizacion()));
        pdf.down(16f);

        Mascota m = h.getMascota();
        Cliente c = h.getCliente();
        Usuario v = h.getVeterinario();

        if (m != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Paciente: " + safe(m.getNombre()));
            pdf.down(14f);
        }
        if (c != null && c.getPersona() != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(),
                    "Cliente: " + safe(c.getPersona().getNombre()) + " " + safe(c.getPersona().getApellido()));
            pdf.down(14f);
        }

        String vetTxt = "";
        if (v != null) {
            try {
                if (v.getPersona() != null) {
                    vetTxt = (safe(v.getPersona().getNombre()) + " " + safe(v.getPersona().getApellido())).trim();
                }
            } catch (Exception ignore) {
            }
        }
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Veterinario: " + safe(vetTxt));
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Usuario gestión: " + safe(nombreVisibleUsuarioGestion(h.getUsuarioGestion())));
        pdf.down(14f);

        String ingreso = (h.getFechaIngreso() != null) ? h.getFechaIngreso().format(DF) : "";
        if (h.getHora() != null) {
            ingreso = ingreso + " " + h.getHora().toString();
        }
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Ingreso: " + safe(ingreso).trim());
        pdf.down(14f);

        String alta = (h.getFechaAlta() != null) ? h.getFechaAlta().format(DTF_ALTA) : "";
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Alta: " + safe(alta));
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Estado: " + safe(h.getEstado()));
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Motivo: " + safe(h.getMotivo()));
        pdf.down(14f);

        pdf.line();

        if (usuario != null && !usuario.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + usuario);
            pdf.down(12f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
            pdf.down(14f);
        }

        pdf.line();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Diagnóstico");
        pdf.down(16f);
        for (String linea : wrap(safe(h.getDiagnostico()), 95)) {
            pdf.ensureSpace(18f);
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), linea);
            pdf.down(14f);
        }

        pdf.line();
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Tratamiento");
        pdf.down(16f);
        for (String linea : wrap(safe(h.getTratamiento()), 95)) {
            pdf.ensureSpace(18f);
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), linea);
            pdf.down(14f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String nombreVisibleUsuarioGestion(veterinaria.entidad.Usuario u) {
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

    private java.util.List<String> wrap(String s, int max) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (s == null) return out;
        String txt = s.replace("\r", "");
        for (String rawLine : txt.split("\n")) {
            String line = rawLine;
            while (line.length() > max) {
                int cut = line.lastIndexOf(' ', max);
                if (cut <= 0) cut = max;
                out.add(line.substring(0, cut).trim());
                line = line.substring(cut).trim();
            }
            if (!line.isBlank()) out.add(line);
            if (rawLine.isBlank()) out.add("");
        }
        return out;
    }
}
