package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.HistoriaEvento;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Persona;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class HistoriaClinicaPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.HISTORIA_CLINICA;
    }

    @Override
    protected String titulo() {
        return "HISTORIA CLÍNICA - DETALLADO";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Mascota mascota = request.get("mascota", Mascota.class);
        @SuppressWarnings("unchecked")
        List<HistoriaEvento> eventos = request.get("eventos", List.class);
        if (mascota == null) {
            throw new Exception("Mascota no proporcionada.");
        }
        if (eventos == null || eventos.isEmpty()) {
            throw new Exception("La mascota no tiene eventos de historia clínica para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Mascota mascota = request.get("mascota", Mascota.class);
        @SuppressWarnings("unchecked")
        List<HistoriaEvento> eventos = request.get("eventos", List.class);
        String emisor = request.get("emisor", String.class);

        float x = pdf.getMargin();

        Cliente c = (mascota != null) ? mascota.getCliente() : null;
        Persona p = (c != null) ? c.getPersona() : null;
        String dueno = (p != null)
                ? (safe(p.getApellido()) + " " + safe(p.getNombre())).trim()
                : "";

        seccion(pdf, "DATOS DEL PROPIETARIO");
        campo(pdf, "Nombre", p == null ? "" : safe(p.getNombre()));
        campo(pdf, "Apellido", p == null ? "" : safe(p.getApellido()));
        campo(pdf, "DNI", p == null ? "" : safe(p.getDni()));
        campo(pdf, "Dirección", p == null ? "" : safe(p.getDireccion()));
        campo(pdf, "Teléfono", p == null ? "" : safe(p.getTelefono()));
        campo(pdf, "Correo electrónico", c == null ? "" : safe(c.getEmail()));

        seccion(pdf, "RESEÑA DEL PACIENTE");
        campo(pdf, "Historia clínica / ID", safe(mascota.getIdMascota()));
        campo(pdf, "Paciente", safe(mascota.getNombre()));
        campo(pdf, "Especie", safe(mascota.getEspecie()));
        campo(pdf, "Raza", safe(mascota.getRaza()));
        campo(pdf, "Sexo", safe(mascota.getSexo()));
        campo(pdf, "Nacimiento", mascota.getFechaNacimiento() == null ? ""
                : mascota.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        campo(pdf, "Peso registrado (kg)", safe(mascota.getPeso()));
        campo(pdf, "Castrado", safe(mascota.getCastrado()));

        seccion(pdf, "ANAMNESIS");
        pdf.ensureSpace(36f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Dieta, vacunas y antecedentes: sin datos estructurados en esta ficha.");
        pdf.down(16f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Consultar las evoluciones y observaciones de los eventos siguientes.");
        pdf.down(17f);

        seccion(pdf, "EVOLUCIÓN E HISTORIAL CLÍNICO");
        campo(pdf, "Total de eventos", String.valueOf(eventos.size()));

        for (HistoriaEvento ev : eventos) {
            pdf.ensureSpace(80f);

            String fechaTxt = (ev.getFecha() != null) ? ev.getFecha().format(DTF) : "";
            String tituloEv = (safe(ev.getTipo()).isBlank() ? "EVENTO" : safe(ev.getTipo()))
                    + (fechaTxt.isBlank() ? "" : "  -  " + fechaTxt);

            pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), tituloEv);
            pdf.down(14f);

            if (!safe(ev.getProfesional()).isBlank()) {
                pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Profesional: " + safe(ev.getProfesional()));
                pdf.down(12f);
            }

            if (!safe(ev.getResumen()).isBlank()) {
                pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "Resumen:");
                pdf.down(12f);
                renderParagraph(pdf, safe(ev.getResumen()), 10);
                pdf.down(8f);
            }

            if (!safe(ev.getObservaciones()).isBlank()) {
                pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "Observaciones:");
                pdf.down(12f);
                renderParagraph(pdf, safe(ev.getObservaciones()), 10);
                pdf.down(8f);
            }

            String ref = buildRef(ev);
            if (!ref.isBlank()) {
                pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), ref);
                pdf.down(12f);
            }

            pdf.line();
        }

        if (emisor != null && !emisor.isBlank()) {
            pdf.ensureSpace(40f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + emisor);
            pdf.down(12f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF));
            pdf.down(12f);
        }
    }

    private void seccion(PdfDocument pdf, String titulo) throws Exception {
        pdf.ensureSpace(34f);
        pdf.down(7f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, pdf.getMargin(), pdf.getY(), titulo);
        pdf.down(15f);
        pdf.line();
        pdf.down(5f);
    }

    private void campo(PdfDocument pdf, String nombre, String valor) throws Exception {
        pdf.ensureSpace(20f);
        float x = pdf.getMargin();
        String etiqueta = nombre + ": ";
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), etiqueta);
        float offset = pdf.textWidth(PDType1Font.HELVETICA_BOLD, 9, etiqueta);
        String texto = valor.isBlank() ? "No registrado" : valor;
        java.util.List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 9, texto,
                pdf.getUsableWidth() - offset - 4f);
        pdf.text(PdfStyle.TXT_FONT, 9, x + offset, pdf.getY(), lineas.get(0));
        pdf.down(13f);
        for (int i = 1; i < lineas.size(); i++) {
            pdf.ensureSpace(16f);
            pdf.text(PdfStyle.TXT_FONT, 9, x + offset, pdf.getY(), lineas.get(i));
            pdf.down(13f);
        }
    }

    private void renderParagraph(PdfDocument pdf, String text, int fontSize) throws Exception {
        float maxW = pdf.getPageW() - (pdf.getMargin() * 2);
        java.util.List<String> lines = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, text, maxW);
        final float lineH = 12f;
        for (String ln : lines) {
            pdf.ensureSpace(lineH + 4f);
            pdf.text(PdfStyle.TXT_FONT, fontSize, pdf.getMargin(), pdf.getY(), ln);
            pdf.down(lineH);
        }
    }

    private String buildRef(HistoriaEvento ev) {
        String tabla = safe(ev.getRefTabla());
        String id = ev.getRefId() != null ? String.valueOf(ev.getRefId()) : "";
        if (tabla.isBlank() && id.isBlank()) return "";
        if (!tabla.isBlank() && !id.isBlank()) return "Referencia: " + tabla + " (#" + id + ")";
        if (!tabla.isBlank()) return "Referencia: " + tabla;
        return "Referencia ID: " + id;
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
