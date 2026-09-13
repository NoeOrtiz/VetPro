package veterinaria.reportes.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Persona;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class MascotaRegistroPdfReporte extends AbstractPdfReporte {

    private static final DateTimeFormatter DTF_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DTF_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.MASCOTA_REGISTRO;
    }

    @Override
    protected String titulo() {
        return "REGISTRO DE MASCOTA";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Mascota m = request.get("mascota", Mascota.class);
        if (m == null) {
            throw new Exception("Mascota no proporcionada.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Mascota m = request.get("mascota", Mascota.class);
        String emisor = request.get("emisor", String.class);

        float x = pdf.getMargin();

        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, pdf.getY(), "ID Mascota: " + safe(m.getIdMascota()));
        pdf.down(16f);

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Nombre: " + safe(m.getNombre()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Especie: " + safe(m.getEspecie()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Raza: " + safe(m.getRaza()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Sexo: " + safe(m.getSexo()));
        pdf.down(14f);

        LocalDate fn = m.getFechaNacimiento();
        String fechaNac = (fn != null) ? fn.format(DTF_FECHA) : "";
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Fecha de nacimiento: " + fechaNac);
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Edad: " + calcularEdad(fn));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Castrado: " + safe(m.getCastrado()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Tamaño: " + safe(m.getTamano()));
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Peso: " + safe(m.getPeso()));
        pdf.down(14f);

        Cliente c = m.getCliente();
        Persona p = (c != null) ? c.getPersona() : null;
        String dueno = "";
        if (p != null) {
            dueno = (safe(p.getApellido()) + " " + safe(p.getNombre())).trim();
        }
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Dueño: " + dueno);
        pdf.down(14f);

        pdf.line();

        if (emisor != null && !emisor.isBlank()) {
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Emitido por: " + emisor);
            pdf.down(12f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Fecha y hora: " + LocalDateTime.now().format(DTF_EMISION));
            pdf.down(14f);
        }
    }

    private String calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) return "";
        try {
            Period p = Period.between(fechaNacimiento, LocalDate.now());
            int anios = p.getYears();
            int meses = p.getMonths();
            if (anios > 0 && meses > 0) return anios + " años, " + meses + " meses";
            if (anios > 0) return anios + " años";
            if (meses > 0) return meses + " meses";
            int dias = p.getDays();
            return dias + " días";
        } catch (Exception ignore) {
            return "";
        }
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
