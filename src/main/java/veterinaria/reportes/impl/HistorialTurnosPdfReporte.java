
package veterinaria.reportes.impl;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.AbstractPdfReporte;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

public class HistorialTurnosPdfReporte extends AbstractPdfReporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.HISTORIAL_TURNOS;
    }

    @Override
    protected boolean isLandscape() {
        return true;
    }

    @Override
    protected String titulo() {
        return "INFORME - HISTORIAL DE TURNOS";
    }

    @Override
    protected void validar(ReporteRequest request) throws Exception {
        JTable tabla = request.get("tabla", JTable.class);
        if (tabla == null || tabla.getRowCount() == 0) {
            throw new Exception("No hay turnos para imprimir.");
        }
    }

    @Override
    protected void renderBody(PdfDocument pdf, ReporteRequest request) throws Exception {

        String filtros = request.get("filtros", String.class);
        if (filtros != null && !filtros.trim().isEmpty()) {
            pdf.text(PDType1Font.HELVETICA_BOLD, 10, pdf.getMargin(), pdf.getY(), "Filtros aplicados:");
            pdf.down(12f);

            String[] lines = filtros.split("\\r?\\n");
            for (String line : lines) {
                if (line == null) continue;
                String s = line.trim();
                if (s.isEmpty()) continue;
                float maxW = pdf.getPageW() - (pdf.getMargin() * 2);
                java.util.List<String> wrapped = pdf.wrapText(PdfStyle.TXT_FONT, 9, s, maxW);
                for (String wl : wrapped) {
                    pdf.ensureSpace(14f);
                    pdf.text(PdfStyle.TXT_FONT, 9, pdf.getMargin(), pdf.getY(), wl);
                    pdf.down(12f);
                }
            }
            pdf.down(6f);
            pdf.line();
        }

        JTable tabla = request.get("tabla", JTable.class);
        TableModel m = tabla.getModel();

        pdf.text(PdfStyle.TXT_FONT, 10, pdf.getMargin(), pdf.getY(), "Total de turnos listados: " + m.getRowCount());
        pdf.down(16f);
int colNro = 1;
int colFechaTurno = 2;
int colPaciente = 3;
int colCliente = 4;
int colTipo = 5;
int colVet = 6;
int colPrecio = 7;
int colEstado = 8;
int colMotivo = 9;
int colFechaEvento = 10;

boolean mostrarMotivo = false;
for (int rr = 0; rr < m.getRowCount(); rr++) {
    String est = safe(m.getValueAt(rr, colEstado));
    if (est != null && est.trim().equalsIgnoreCase("CANCELADO")) {
        mostrarMotivo = true;
        break;
    }
}

float x = pdf.getMargin();

final int fontSize = 8;
final float lineH = 10f;

final float[] w = mostrarMotivo
        ? new float[] {
            20f,  // N°
            90f,  // Fecha Turno
            80f,  // Paciente
            100f, // Cliente
            70f,  // Tipo
            100f, // Veterinario
            40f,  // Precio
            60f,  // Estado
            110f, // Motivo
            70f   // Fecha EVT
        }
        : new float[] {
            20f,  // N°
            95f,  // Fecha Turno
            85f,  // Paciente
            110f, // Cliente
            80f,  // Tipo
            120f, // Veterinario
            45f,  // Precio
            70f,  // Estado
            117f  // Fecha EVT (se redistribuye el espacio al ocultar Motivo)
        };

final float[] cx = new float[w.length];
cx[0] = x;
for (int i = 1; i < w.length; i++) {
    cx[i] = cx[i - 1] + w[i - 1];
}

pdf.ensureSpace(60f);
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[0], pdf.getY(), "N°");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[1], pdf.getY(), "FECHA TURNO");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[2], pdf.getY(), "PACIENTE");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[3], pdf.getY(), "CLIENTE");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[4], pdf.getY(), "TIPO");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[5], pdf.getY(), "VETERINARIO");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[6], pdf.getY(), "PRECIO");
pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[7], pdf.getY(), "ESTADO");
if (mostrarMotivo) {
    pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[8], pdf.getY(), "MOTIVO");
    pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[9], pdf.getY(), "FECHA");
} else {
    pdf.text(PDType1Font.HELVETICA_BOLD, fontSize, cx[8], pdf.getY(), "FECHA");
}
pdf.down(10f);
pdf.line();

for (int r = 0; r < m.getRowCount(); r++) {

    String nro = safe(m.getValueAt(r, colNro));
    String fechaTurno = safe(m.getValueAt(r, colFechaTurno));
    String paciente = safe(m.getValueAt(r, colPaciente));
    String cliente = safe(m.getValueAt(r, colCliente));
    String tipo = safe(m.getValueAt(r, colTipo));
    String vet = safe(m.getValueAt(r, colVet));
    String precio = safe(m.getValueAt(r, colPrecio));
    String estado = safe(m.getValueAt(r, colEstado));
    String motivo = safe(m.getValueAt(r, colMotivo));
    String fechaEvt = safe(m.getValueAt(r, colFechaEvento));

    java.util.List<String> l0 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, nro, w[0] - 2);
    java.util.List<String> l1 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fechaTurno, w[1] - 2);
    java.util.List<String> l2 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, paciente, w[2] - 2);
    java.util.List<String> l3 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, cliente, w[3] - 2);
    java.util.List<String> l4 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, tipo, w[4] - 2);
    java.util.List<String> l5 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, vet, w[5] - 2);
    java.util.List<String> l6 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, precio, w[6] - 2);
    java.util.List<String> l7 = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, estado, w[7] - 2);

    java.util.List<String> l8Motivo = null;
    java.util.List<String> l9Fecha = null;

    if (mostrarMotivo) {
        String motivoRender = (estado != null && estado.trim().equalsIgnoreCase("CANCELADO")) ? motivo : "";
        l8Motivo = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, motivoRender, w[8] - 2);
        l9Fecha = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fechaEvt, w[9] - 2);
    } else {
        l9Fecha = pdf.wrapText(PdfStyle.TXT_FONT, fontSize, fechaEvt, w[8] - 2);
    }

    int maxLines = mostrarMotivo
            ? max(
                l0.size(), l1.size(), l2.size(), l3.size(), l4.size(),
                l5.size(), l6.size(), l7.size(), l8Motivo.size(), l9Fecha.size()
            )
            : max(
                l0.size(), l1.size(), l2.size(), l3.size(), l4.size(),
                l5.size(), l6.size(), l7.size(), l9Fecha.size()
            );

    float rowH = Math.max(1, maxLines) * lineH;
    pdf.ensureSpace(rowH + 6f);

    float yTop = pdf.getY();
    for (int i = 0; i < maxLines; i++) {
        float yy = yTop - (i * lineH);
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[0], yy, pick(l0, i));
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[1], yy, pick(l1, i));
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[2], yy, pick(l2, i));
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[3], yy, pick(l3, i));
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[4], yy, pick(l4, i));
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[5], yy, pick(l5, i));

        String p = pick(l6, i);
        float pw = pdf.textWidth(PdfStyle.TXT_FONT, fontSize, p);
        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[6] + (w[6] - 2) - pw, yy, p);

        pdf.text(PdfStyle.TXT_FONT, fontSize, cx[7], yy, pick(l7, i));

        if (mostrarMotivo) {
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[8], yy, pick(l8Motivo, i));
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[9], yy, pick(l9Fecha, i));
        } else {
            pdf.text(PdfStyle.TXT_FONT, fontSize, cx[8], yy, pick(l9Fecha, i));
        }
    }

    pdf.down(rowH + 2f);
}
    }

    private String safe(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String pick(java.util.List<String> lines, int idx) {
        if (lines == null || lines.isEmpty()) return "";
        if (idx < 0 || idx >= lines.size()) return "";
        return lines.get(idx) != null ? lines.get(idx) : "";
    }

    private int max(int... v) {
        int m = 0;
        if (v == null) return 0;
        for (int n : v) if (n > m) m = n;
        return m;
    }
}
