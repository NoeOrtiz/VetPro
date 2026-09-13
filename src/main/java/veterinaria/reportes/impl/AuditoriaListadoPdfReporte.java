package veterinaria.reportes.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class AuditoriaListadoPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.AUDITORIA_LISTADO;
    }

    @Override
    protected String titulo() {
        return "AUDITORIA - LISTADO DE EVENTOS";
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay eventos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {

        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        String filtros = request.get("filtros", String.class);
        if (filtros == null) {
            filtros = "";
        }

        if (!filtros.isEmpty()) {
            pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(),
                    "Filtros: " + filtros);
            pdf.down(12f);
        }

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(),
                "Total de eventos: " + m.getRowCount());

        pdf.down(14f);

        int colFecha = 0;
        int colUsuario = 1;
        int colAccion = 2;
        int colEntidad = 3;
        int colEntidadId = 4;
        int colModulo = 5;
        int colResultado = 6;

        final int fontSize = 9;

        // Antes era 11
        final float lineH = 12f;

        // Nuevos anchos
        final float[] w = {
            95f, // Fecha/Hora
            65f, // Usuario
            60f, // Acción
            65f, // Entidad
            60f, // Módulo
            235f, // Descripción
            45f // Resultado
        };

        float x = pdf.getMargin();
        float y = pdf.getY();

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Fecha/Hora");
        x += w[0];

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Usuario");
        x += w[1];

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Acción");
        x += w[2];

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Entidad");
        x += w[3];

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Módulo");
        x += w[4];

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Descripción");
        x += w[5];

        pdf.text(PdfStyle.TXT_BOLD_FONT, fontSize, x, y, "Resultado.");

        pdf.down(10f);
        pdf.line();
        pdf.down(5f);

        for (int r = 0; r < m.getRowCount(); r++) {

            pdf.ensureSpace(15f);

            x = pdf.getMargin();
            y = pdf.getY();

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colFecha)));
            x += w[0];

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colUsuario)));
            x += w[1];

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colAccion)));
            x += w[2];

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colEntidad)));
            x += w[3];

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colEntidadId)));
            x += w[4];

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colModulo)));
            x += w[5];

            pdf.text(PdfStyle.TXT_FONT, fontSize, x, y,
                    safe(m.getValueAt(r, colResultado)));

            pdf.down(lineH);
        }

        pdf.down(8f);

        pdf.text(
                PdfStyle.TXT_FONT,
                9,
                pdf.getMargin(),
                pdf.getY(),
                "Generado: "
                + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    private String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
