package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

/** Ficha clínica de una consulta, preparada para texto largo sin superposiciones. */
public class VisitaRegistroPdfReporte extends AbstractPdfReporte {
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public ReporteTipo tipo() { return ReporteTipo.VISITA_REGISTRO; }
    @Override protected String titulo() { return "REGISTRO DE CONSULTA VETERINARIA"; }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        if (request.get("visita", Visita.class) == null)
            throw new Exception("Seleccione una consulta para imprimir.");
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Visita v = request.get("visita", Visita.class);
        String emisor = request.get("emisor", String.class);
        Mascota mascota = v.getMascota();
        // El titular histórico corresponde a la consulta, no al dueño actual.
        Cliente cliente = v.getCliente();
        Persona persona = cliente == null ? null : cliente.getPersona();
        String responsable = persona == null ? "-"
                : (texto(persona.getNombre()) + " " + texto(persona.getApellido())).trim();
        String fecha = v.getFecha() == null ? "-"
                : v.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = v.getHora() == null ? "-"
                : v.getHora().format(DateTimeFormatter.ofPattern("HH:mm"));

        float x = pdf.getMargin();
        pdf.down(10f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 12, x, pdf.getY(),
                "CONSULTA N.º " + v.getIdVisita());
        pdf.down(24f);
        seccion(pdf, "DATOS DE LA ATENCIÓN");
        campo(pdf, "Paciente", mascota == null ? "-" : texto(mascota.getNombre()));
        campo(pdf, "Responsable", responsable);
        campo(pdf, "Fecha y hora", fecha + "  " + hora);
        campo(pdf, "Estado", texto(v.getEstado()));
        campo(pdf, "Prioridad", texto(v.getPrioridadAtencion()));
        campo(pdf, "Motivo de consulta", texto(v.getMotivoVisita()));
        campo(pdf, "Veterinario", texto(v.getUsuarioAtiende()));
        pdf.down(7f);
        seccion(pdf, "INFORMACIÓN CLÍNICA");
        campo(pdf, "Diagnóstico", texto(v.getPatologia()));
        campo(pdf, "Evolución y tratamiento", texto(v.getTratamiento()));
        pdf.down(15f);
        pdf.ensureSpace(65f);
        pdf.line();
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Registrado por: " + texto(v.getUsuarioGestion()));
        pdf.down(15f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Emitido por: " + texto(emisor));
        pdf.down(15f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Fecha de emisión: " + LocalDateTime.now().format(FECHA));
        pdf.down(15f);
    }

    private void seccion(PdfDocument pdf, String titulo) throws Exception {
        pdf.ensureSpace(42f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, pdf.getMargin(), pdf.getY(), titulo);
        pdf.down(13f);
        pdf.line();
        pdf.down(4f);
    }

    private void campo(PdfDocument pdf, String titulo, String valor) throws Exception {
        float x = pdf.getMargin();
        float ancho = pdf.getUsableWidth() - 14f;
        List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 10, valor, ancho);
        pdf.ensureSpace(15f + lineas.size() * 13f + 12f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), titulo);
        pdf.down(14f);
        for (String linea : lineas) {
            pdf.text(PdfStyle.TXT_FONT, 10, x + 8f, pdf.getY(), linea);
            pdf.down(13f);
        }
        pdf.down(7f);
    }

    private String texto(Object value) {
        String s = value == null ? "" : String.valueOf(value).trim();
        return s.isBlank() ? "-" : s;
    }
}
