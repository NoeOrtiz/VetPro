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

        float x = pdf.getMargin();

        Cliente c = (mascota != null) ? mascota.getCliente() : null;
        Persona p = (c != null) ? c.getPersona() : null;
        String dueno = (p != null)
                ? (safe(p.getApellido()) + " " + safe(p.getNombre())).trim()
                : "";

        seccion(pdf, "DATOS DEL PROPIETARIO");
        campoPar(pdf, "Nombre", p == null ? "" : safe(p.getNombre()),
                "Apellido", p == null ? "" : safe(p.getApellido()));
        campoPar(pdf, "DNI", p == null ? "" : safe(p.getDni()),
                "Teléfono", p == null ? "" : safe(p.getTelefono()));
        campoPar(pdf, "Dirección", p == null ? "" : safe(p.getDireccion()),
                "Correo electrónico", c == null ? "" : safe(c.getEmail()));

        seccion(pdf, "RESEÑA DEL PACIENTE");
        campoPar(pdf, "Historia clínica / ID", safe(mascota.getIdMascota()),
                "Paciente", safe(mascota.getNombre()));
        campoPar(pdf, "Especie", safe(mascota.getEspecie()),
                "Raza", safe(mascota.getRaza()));
        campoPar(pdf, "Sexo", safe(mascota.getSexo()),
                "Nacimiento", mascota.getFechaNacimiento() == null ? ""
                        : mascota.getFechaNacimiento().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        campoPar(pdf, "Peso registrado (kg)", safe(mascota.getPeso()),
                "Castrado", safe(mascota.getCastrado()));

        seccion(pdf, "ANAMNESIS");
        pdf.ensureSpace(36f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Dieta, vacunas y antecedentes: sin datos estructurados en esta ficha.");
        pdf.down(16f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Consultar las evoluciones y observaciones de los eventos siguientes.");
        pdf.down(17f);

        // Mantener el encabezado junto al primer evento: evitar títulos
        // abandonados al final de página y el conteo aislado en la siguiente.
        pdf.ensureSpace(350f);
        seccion(pdf, "EVOLUCIÓN E HISTORIAL CLÍNICO");
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Total de eventos registrados: " + eventos.size());
        pdf.down(23f);

        for (HistoriaEvento ev : eventos) {
            int lineasResumen = pdf.wrapText(PdfStyle.TXT_FONT, 10,
                    safe(ev.getResumen()), pdf.getUsableWidth() - 20f).size();
            int lineasObservaciones = pdf.wrapText(PdfStyle.TXT_FONT, 10,
                    safe(ev.getObservaciones()), pdf.getUsableWidth() - 20f).size();
            float reserva = 52f
                    + (safe(ev.getResumen()).isBlank() ? 0f : 32f + lineasResumen * 17f)
                    + (safe(ev.getObservaciones()).isBlank() ? 0f : 32f + lineasObservaciones * 17f)
                    + 47f;
            // Un evento no se parte salvo que exceda una página completa.
            if (reserva < pdf.getUsableHeight() - 110f) {
                pdf.ensureSpace(reserva);
            } else {
                pdf.ensureSpace(80f);
            }

            String fechaTxt = (ev.getFecha() != null) ? ev.getFecha().format(DTF) : "";
            String tituloEv = (safe(ev.getTipo()).isBlank() ? "EVENTO" : safe(ev.getTipo()))
                    + (fechaTxt.isBlank() ? "" : "  -  " + fechaTxt);

            pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), tituloEv);
            pdf.down(18f);

            if (!safe(ev.getProfesional()).isBlank()) {
                pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Profesional: " + safe(ev.getProfesional()));
                pdf.down(17f);
            }

            if (!safe(ev.getResumen()).isBlank()) {
                pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "Resumen:");
                pdf.down(18f);
                renderParagraph(pdf, safe(ev.getResumen()), 10);
                pdf.down(9f);
            }

            if (!safe(ev.getObservaciones()).isBlank()) {
                pdf.text(PDType1Font.HELVETICA_BOLD, 10, x, pdf.getY(), "Observaciones:");
                pdf.down(18f);
                renderParagraph(pdf, importeHistoria(ev.getTipo(), safe(ev.getObservaciones())), 10);
                pdf.down(9f);
            }

            String ref = buildRef(ev);
            if (!ref.isBlank()) {
                pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), ref);
                pdf.down(14f);
            }

            pdf.down(3f);
            pdf.line();
            pdf.down(9f);
        }

        // El usuario y la fecha ya figuran en el pie de todas las páginas.
        // No generar una página adicional solo para repetir esos datos.
    }

    private void seccion(PdfDocument pdf, String titulo) throws Exception {
        pdf.ensureSpace(46f);
        pdf.down(12f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, pdf.getMargin(), pdf.getY(), titulo);
        pdf.down(19f);
        pdf.line();
        pdf.down(10f);
    }

    /** Dos campos alineados; separación horizontal y vertical constante. */
    private void campoPar(PdfDocument pdf, String nombreIzq, String valorIzq,
            String nombreDer, String valorDer) throws Exception {
        float x = pdf.getMargin();
        float separacion = 32f;
        float ancho = (pdf.getUsableWidth() - separacion) / 2f;
        float xDer = x + ancho + separacion;
        String vi = valorIzq == null || valorIzq.isBlank() ? "No registrado" : valorIzq;
        String vd = valorDer == null || valorDer.isBlank() ? "No registrado" : valorDer;
        java.util.List<String> li = pdf.wrapText(PdfStyle.TXT_FONT, 10, vi, ancho - 12f);
        java.util.List<String> ld = pdf.wrapText(PdfStyle.TXT_FONT, 10, vd, ancho - 12f);
        int n = Math.max(li.size(), ld.size());
        float altura = 22f + n * 17f + 12f;
        pdf.ensureSpace(altura + 8f);
        float arriba = pdf.getY();
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, arriba, nombreIzq);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xDer, arriba, nombreDer);
        for (int i = 0; i < li.size(); i++) {
            pdf.text(PdfStyle.TXT_FONT, 10, x + 9f, arriba - 22f - i * 17f, li.get(i));
        }
        for (int i = 0; i < ld.size(); i++) {
            pdf.text(PdfStyle.TXT_FONT, 10, xDer + 9f, arriba - 22f - i * 17f, ld.get(i));
        }
        pdf.down(altura);
    }

    private void campo(PdfDocument pdf, String nombre, String valor) throws Exception {
        float x = pdf.getMargin();
        float ancho = pdf.getUsableWidth() - 18f;
        String texto = valor == null || valor.isBlank() ? "No registrado" : valor;
        java.util.List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 10, texto, ancho);
        float alto = 21f + (lineas.size() * 16f) + 9f;
        pdf.ensureSpace(alto + 8f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), nombre);
        pdf.down(20f);
        for (int i = 0; i < lineas.size(); i++) {
            pdf.textJustified(PdfStyle.TXT_FONT, 10, x + 10f, pdf.getY(),
                    lineas.get(i), ancho, i < lineas.size() - 1);
            pdf.down(16f);
        }
        pdf.down(9f);
    }

    private void renderParagraph(PdfDocument pdf, String text, int fontSize) throws Exception {
        float maxW = pdf.getPageW() - (pdf.getMargin() * 2);
        java.util.List<String> lines = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, text, maxW);
        final float lineH = 17f;
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            pdf.ensureSpace(lineH + 7f);
            pdf.textJustified(PdfStyle.TXT_FONT, fontSize,
                    pdf.getMargin() + 10f, pdf.getY(), ln, maxW - 20f,
                    i < lines.size() - 1 && !ln.isBlank());
            pdf.down(lineH);
        }
    }

    private String importeHistoria(String tipo, String valor) {
        if (tipo == null || !tipo.toUpperCase(java.util.Locale.ROOT).contains("PELUQUER")
                || valor == null || !valor.trim().matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            return valor;
        }
        try {
            java.text.NumberFormat formato =
                    java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "AR"));
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);
            return "$ " + formato.format(new java.math.BigDecimal(valor.trim()));
        } catch (Exception ex) {
            return valor;
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
