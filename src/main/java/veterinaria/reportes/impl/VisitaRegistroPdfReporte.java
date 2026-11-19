package veterinaria.reportes.impl;

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
        pdf.down(14f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 12, x, pdf.getY(),
                "CONSULTA N.º " + v.getIdVisita());
        pdf.down(27f);
        seccion(pdf, "DATOS DE LA ATENCIÓN");
        // Dos columnas de información: mejor lectura y menor desperdicio de papel.
        par(pdf, "Paciente", mascota == null ? "-" : texto(mascota.getNombre()),
                "Especie", texto(v.getEspecieConsulta() == null
                        ? (mascota == null ? null : mascota.getEspecie()) : v.getEspecieConsulta()));
        par(pdf, "Raza", texto(v.getRazaConsulta() == null
                        ? (mascota == null ? null : mascota.getRaza()) : v.getRazaConsulta()),
                "Peso de esta consulta", v.getPesoConsulta() == null
                        ? "No registrado" : v.getPesoConsulta().toPlainString().replace('.', ',') + " kg");
        par(pdf, "Responsable", responsable, "Fecha y hora", fecha + "  " + hora);
        par(pdf, "Estado", texto(v.getEstado()), "Prioridad", texto(v.getPrioridadAtencion()));
        par(pdf, "Motivo de consulta", texto(v.getMotivoVisita()),
                "Veterinario", texto(v.getUsuarioAtiende()));
        if (v.getIdConsultaOrigen() != null || v.getProximoControlRecomendado() != null)
            par(pdf, "Seguimiento de", v.getIdConsultaOrigen() == null
                            ? "Sin vinculación" : "Consulta N.º " + v.getIdConsultaOrigen(),
                    "Próximo control recomendado",
                    v.getProximoControlRecomendado() == null ? "No indicado"
                            : v.getProximoControlRecomendado().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        pdf.down(15f);
        seccion(pdf, "INFORMACIÓN CLÍNICA");
        campo(pdf, "Diagnóstico", texto(v.getPatologia()));
        campo(pdf, "Evolución y tratamiento", texto(v.getTratamiento()));
        pdf.down(14f);
        pdf.ensureSpace(28f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(),
                "Registrado por: " + texto(v.getUsuarioGestion()));
        pdf.down(15f);
        // El emisor y la fecha de impresión ya figuran en el pie de cada página.
    }

    /** Dos datos alineados en una fila, con altura calculada según su texto. */
    private void par(PdfDocument pdf, String etiquetaIzquierda, String valorIzquierda,
            String etiquetaDerecha, String valorDerecha) throws Exception {
        float espacio = 28f;
        float medio = (pdf.getUsableWidth() - espacio) / 2f;
        float x = pdf.getMargin();
        float xDer = x + medio + espacio;
        List<String> izquierda = pdf.wrapText(PdfStyle.TXT_FONT, 10, valorIzquierda, medio - 18f);
        List<String> derecha = pdf.wrapText(PdfStyle.TXT_FONT, 10, valorDerecha, medio - 18f);
        int renglones = Math.max(izquierda.size(), derecha.size());
        float altura = 19f + 15f * renglones + 10f;
        pdf.ensureSpace(altura + 2f);
        float arriba = pdf.getY();
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, arriba, etiquetaIzquierda);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, xDer, arriba, etiquetaDerecha);
        for (int i = 0; i < izquierda.size(); i++)
            pdf.text(PdfStyle.TXT_FONT, 10, x + 9f, arriba - 19f - 15f * i, izquierda.get(i));
        for (int i = 0; i < derecha.size(); i++)
            pdf.text(PdfStyle.TXT_FONT, 10, xDer + 9f, arriba - 19f - 15f * i, derecha.get(i));
        pdf.down(altura);
    }

    private void seccion(PdfDocument pdf, String titulo) throws Exception {
        pdf.ensureSpace(50f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 10, pdf.getMargin(), pdf.getY(), titulo);
        pdf.down(17f);
        pdf.line();
        pdf.down(7f);
    }

    private void campo(PdfDocument pdf, String titulo, String valor) throws Exception {
        float x = pdf.getMargin();
        float ancho = pdf.getUsableWidth() - 22f;
        List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 10, valor, ancho);
        pdf.ensureSpace(21f + lineas.size() * 17f + 14f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 9, x, pdf.getY(), titulo);
        pdf.down(21f);
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            pdf.textJustified(PdfStyle.TXT_FONT, 10, x + 10f, pdf.getY(),
                    linea, ancho, i < lineas.size() - 1 && !linea.isBlank());
            pdf.down(17f);
        }
        pdf.down(13f);
    }

    private String texto(Object value) {
        String s = value == null ? "" : String.valueOf(value).trim();
        return s.isBlank() ? "-" : s;
    }
}
