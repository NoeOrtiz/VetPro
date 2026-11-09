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
    protected void renderHeader(PdfDocument pdf, ReporteRequest request) throws Exception {
        float ancho = pdf.getPageW();
        float y = pdf.getY();
        renderLogo(pdf, (ancho - 150f) / 2f, y - 94f, 150f, 94f);
        pdf.setY(y - 115f);
        String titulo = titulo();
        pdf.text(PDType1Font.HELVETICA_BOLD, 15,
                (ancho - pdf.textWidth(PDType1Font.HELVETICA_BOLD, 15, titulo)) / 2f,
                pdf.getY(), titulo);
        pdf.down(22f);
        pdf.line();
        pdf.down(12f);
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Mascota m = request.get("mascota", Mascota.class);
        Cliente cliente = m.getCliente();
        Persona persona = cliente == null ? null : cliente.getPersona();
        String dueno = persona == null ? "Sin especificar"
                : (safe(persona.getNombre()) + " " + safe(persona.getApellido())).trim();
        float izquierda = pdf.getMargin() + 12f;
        float derecha = pdf.getPageW() / 2f + 8f;
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, pdf.getMargin(), pdf.getY(), "DATOS DEL PACIENTE");
        pdf.down(27f);
        campo(pdf, izquierda, "Nombre", safe(m.getNombre()));
        campo(pdf, derecha, "Estado", m.isActivo() ? "Activo" : "Inactivo");
        pdf.down(51f);
        campo(pdf, izquierda, "Especie", safe(m.getEspecie()));
        campo(pdf, derecha, "Raza", safe(m.getRaza()));
        pdf.down(51f);
        campo(pdf, izquierda, "Sexo", safe(m.getSexo()));
        campo(pdf, derecha, "Castrado", safe(m.getCastrado()));
        pdf.down(51f);
        LocalDate nacimiento = m.getFechaNacimiento();
        campo(pdf, izquierda, "Fecha de nacimiento",
                nacimiento == null ? "Sin especificar" : nacimiento.format(DTF_FECHA));
        campo(pdf, derecha, "Edad", calcularEdad(nacimiento));
        pdf.down(51f);
        campo(pdf, izquierda, "Tamaño", safe(m.getTamano()));
        campo(pdf, derecha, "Peso (kg)", safe(m.getPeso()));
        pdf.down(57f);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, pdf.getMargin(), pdf.getY(), "RESPONSABLE");
        pdf.down(26f);
        campo(pdf, izquierda, "Dueño", dueno);
        pdf.down(52f);
        pdf.line();
    }

    private void campo(PdfDocument pdf, float x, String etiqueta, String valor) throws Exception {
        float y = pdf.getY();
        pdf.text(PdfStyle.TXT_FONT, 9, x, y, etiqueta);
        pdf.text(PDType1Font.HELVETICA_BOLD, 11, x, y - 20f, valor);
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
