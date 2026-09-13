package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Laboratorio;
import veterinaria.entidad.Mascota;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class LaboratorioInformePdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.LABORATORIO_INFORME;
    }

    @Override
    protected String titulo() {
        return "INFORME DE LABORATORIO";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Laboratorio lab = request.get("laboratorio", Laboratorio.class);
        if (lab == null) {
            throw new Exception("Laboratorio no proporcionado.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Laboratorio lab = request.get("laboratorio", Laboratorio.class);
        String usuario = request.get("usuario", String.class);

        float x = pdf.getMargin();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Nº Extracción: " + safe(lab.getIdLaboratorio()));
        pdf.down(16f);

        Mascota m = lab.getMascota();
        Cliente c = lab.getCliente();

        if (m != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Paciente: " + safe(m.getNombre()));
            pdf.down(14f);
        }
        if (c != null && c.getPersona() != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(),
                    "Cliente: " + safe(c.getPersona().getNombre()) + " " + safe(c.getPersona().getApellido()));
            pdf.down(14f);
        }

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Veterinario: " + safe(lab.getUsuarioAtiende()));
        pdf.down(14f);

        if (lab.getFechaExtraccion() != null) {
            String fx = lab.getFechaExtraccion().format(DF);
            if (lab.getHoraExtraccion() != null) {
                fx += " " + lab.getHoraExtraccion().toString();
            }
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha extracción: " + fx);
            pdf.down(14f);
        }else {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha extracción: ");
            pdf.down(14f);
        }

        if (lab.getFechaEnvio() != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha envío: " + lab.getFechaEnvio().format(DF));
            pdf.down(14f);
        }else {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha envío: ");
            pdf.down(14f);
        }

        if (lab.getFechaRecepcion() != null) {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha recepción: " + lab.getFechaRecepcion().format(DF));
            pdf.down(14f);
        }else {
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha recepción: ");
            pdf.down(14f);
        }

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Estado: " + safe(lab.getEstado()));
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Motivo: " + safe(lab.getMotivoExtraccion()));
        pdf.down(14f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Tipo de análisis: " + safe(lab.getTipoAnalisis()));
        pdf.down(14f);
        
        pdf.line();

        if (usuario != null && !usuario.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + usuario);
            pdf.down(12f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
            pdf.down(14f);
        }

        pdf.line();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "Informe / Diagnóstico");
        pdf.down(16f);

        String texto = safe(lab.getDiagnostico()).trim();
        for (String linea : wrap(texto, 95)) {
            pdf.ensureSpace(18f);
            pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), linea);
            pdf.down(14f);
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
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
            if (rawLine.isBlank()) out.add(""); // mantiene saltos de párrafo
        }
        return out;
    }
}
