package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import veterinaria.entidad.Auditoria;
import veterinaria.entidad.Usuario;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class AuditoriaEventoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.AUDITORIA_EVENTO;
    }

    @Override
    protected String titulo() {
        return "AUDITORIA - DETALLE DE EVENTO";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        Auditoria a = request.get("auditoria", Auditoria.class);
        if (a == null) {
            throw new Exception("No hay evento seleccionado para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {
        Auditoria a = request.get("auditoria", Auditoria.class);
        String emisor = request.get("emisor", String.class);
        if (emisor == null) {
            emisor = "";
        }

        final float x = pdf.getMargin();
        final int fsLabel = 10;
        final int fsValue = 10;
        final float lineH = 14f;

        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), "Emitido por: " + safe(emisor));
        pdf.down(16f);

        String fecha = "";
        if (a.getFechaHora() != null) {
            fecha = a.getFechaHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        Usuario u = a.getUsuario();

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Fecha/Hora:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), fecha);
        pdf.down(lineH);

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Usuario:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), u != null ? safe(u.getNombreUsuario()) : "N/D");
        pdf.down(lineH);

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Acción:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), safe(a.getAccion()));
        pdf.down(lineH);

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Entidad:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), safe(a.getEntidad()));
        pdf.down(lineH);

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Entidad ID:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), a.getEntidadId() != null ? String.valueOf(a.getEntidadId()) : "");
        pdf.down(lineH);

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Módulo:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), safe(a.getModulo()));
        pdf.down(lineH);

        pdf.text(PdfStyle.TXT_FONT, fsLabel, x, pdf.getY(), "Resultado:");
        pdf.text(PdfStyle.TXT_FONT, fsValue, x + 120, pdf.getY(), safe(a.getResultado()));
        pdf.down(18f);

        pdf.text(PdfStyle.TXT_BOLD_FONT, 11, x, pdf.getY(), "Descripción:");
        pdf.down(14f);
        pdf.text(PdfStyle.TXT_FONT, 10, x, pdf.getY(), safe(a.getDescripcion()));
        pdf.down(16f);

        if (notEmpty(a.getDatosAntes())) {
            pdf.text(PdfStyle.TXT_BOLD_FONT, 11, x, pdf.getY(), "Datos antes:");
            pdf.down(14f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), a.getDatosAntes());
            pdf.down(16f);
        }

        if (notEmpty(a.getDatosDespues())) {
            pdf.text(PdfStyle.TXT_BOLD_FONT, 11, x, pdf.getY(), "Datos después:");
            pdf.down(14f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), a.getDatosDespues());
            pdf.down(16f);
        }

        if (notEmpty(a.getErrorDetalle())) {
            pdf.text(PdfStyle.TXT_BOLD_FONT, 11, x, pdf.getY(), "Detalle de error:");
            pdf.down(14f);
            pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), a.getErrorDetalle());
            pdf.down(16f);
        }

        pdf.down(10f);
        pdf.text(PdfStyle.TXT_FONT, 9, x, pdf.getY(), "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
