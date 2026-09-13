package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Visita;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class VisitaRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.VISITA_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE VISITA";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Visita v = request.get("visita", Visita.class);
        if (v == null) {
            throw new Exception("Visita no proporcionada.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Visita v = request.get("visita", Visita.class);
        String emisor = request.get("emisor", String.class);

        float x = pdf.getMargin();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Nº de visita: " + safe(v.getIdVisita()));
        pdf.down(16f);

        Mascota m = v.getMascota();
        Cliente c = (m != null) ? m.getCliente() : v.getCliente();
        Persona p = (c != null) ? c.getPersona() : null;

        String mascotaNombre = (m != null) ? safe(m.getNombre()) : "";
        String dueno = (p != null)
                ? (safe(p.getApellido()) + " " + safe(p.getNombre())).trim()
                : "";

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Paciente: " + mascotaNombre);
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Cliente / Dueño: " + dueno);
        pdf.down(14f);

        String fecha = (v.getFecha() != null) ? v.getFecha().toString() : "";
        String hora = (v.getHora() != null) ? v.getHora().toString() : "";
        String fechaHora = (fecha.isBlank() ? "" : fecha) + (hora.isBlank() ? "" : "  " + hora);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha y hora: " + fechaHora.trim());
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Motivo: " + safe(v.getMotivoVisita()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Patología / Diagnóstico: " + safe(v.getPatologia()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Tratamiento: " + safe(v.getTratamiento()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Estado: " + safe(v.getEstado()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Veterinario: " + safe(v.getUsuarioAtiende()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Usuario gestión: " + safe(v.getUsuarioGestion()));
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
