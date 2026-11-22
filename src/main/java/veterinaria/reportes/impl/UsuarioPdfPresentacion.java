package veterinaria.reportes.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.font.PDFont;
import veterinaria.reportes.pdf.PdfDocument;
import veterinaria.reportes.pdf.PdfStyle;

/**
 * Presentación de los informes de Usuarios, con tarjetas, chips y tabla
 * en gris/azul petróleo. Reutiliza el contenido y los controles de VetPRO.
 * No contiene consultas a BD ni cambia los datos solicitados.
 */
final class UsuarioPdfPresentacion {

    private UsuarioPdfPresentacion() {}

    private static String txt(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor.trim();
    }

    static void resumen(PdfDocument pdf, String[]... pares) throws Exception {
        pdf.ensureSpace(42f);
        float x = pdf.getMargin(), limite = pdf.getPageW() - pdf.getMargin();
        float y = pdf.getY();
        for (String[] par : pares) {
            if (par == null || par.length < 2) continue;
            String etiqueta = txt(par[0]).toUpperCase(Locale.ROOT);
            String valor = txt(par[1]);
            float et = pdf.textWidth(PdfStyle.TXT_BOLD_FONT, 7, etiqueta);
            float va = pdf.textWidth(PdfStyle.TXT_BOLD_FONT, 9, valor);
            float ancho = et + va + 31f;
            if (x + ancho > limite && x > pdf.getMargin()) {
                x = pdf.getMargin();
                y -= 29f;
            }
            pdf.roundRect(x, y - 20f, ancho, 23f, 11f,
                    PdfStyle.ACCENT_SOFT, null, 0f);
            pdf.text(PdfStyle.TXT_BOLD_FONT, 7, x + 10f, y - 12f,
                    etiqueta, PdfStyle.ACCENT_DARK);
            pdf.text(PdfStyle.TXT_BOLD_FONT, 9, x + 18f + et, y - 12f,
                    valor, PdfStyle.INK);
            x += ancho + 8f;
        }
        pdf.setY(y - 35f);
    }

    static void seccion(PdfDocument pdf, String nombre) throws Exception {
        pdf.ensureSpace(38f);
        float m = pdf.getMargin();
        float y = pdf.getY();
        String texto = txt(nombre).toUpperCase(Locale.ROOT);
        pdf.fillRect(m, y - 4f, 3f, 11f, PdfStyle.ACCENT);
        pdf.text(PdfStyle.TXT_BOLD_FONT, 9, m + 11f, y, texto, PdfStyle.INK);
        float fin = m + 24f + pdf.textWidth(PdfStyle.TXT_BOLD_FONT, 9, texto);
        pdf.hline(fin, pdf.getPageW() - m, y + 2f, PdfStyle.HAIR, 0.65f);
        pdf.down(23f);
    }

    /**
     * Tarjeta con las etiquetas arriba y el contenido debajo.
     * Calcula la altura a partir de las líneas reales de texto.
     */
    static void campos(PdfDocument pdf, String[][] datos, int columnas) throws Exception {
        if (datos == null || datos.length == 0) return;
        int cols = Math.max(1, columnas);
        float m = pdf.getMargin();
        float ancho = pdf.getUsableWidth();
        float separacion = 16f;
        float anchoCampo = (ancho - 29f - (cols - 1) * separacion) / cols;
        int filas = (datos.length + cols - 1) / cols;
        List<List<List<String>>> celdas = new ArrayList<>();
        List<Float> alturas = new ArrayList<>();
        float alto = 23f;
        for (int fila = 0; fila < filas; fila++) {
            List<List<String>> valores = new ArrayList<>();
            int lineasMaximas = 1;
            for (int col = 0; col < cols; col++) {
                int idx = fila * cols + col;
                String valor = idx < datos.length && datos[idx] != null && datos[idx].length > 1
                        ? txt(datos[idx][1]) : "";
                List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 10,
                        valor, Math.max(18f, anchoCampo - 4f));
                lineasMaximas = Math.max(lineasMaximas, lineas.size());
                valores.add(lineas);
            }
            float altoFila = 22f + 13f * lineasMaximas;
            alturas.add(altoFila);
            celdas.add(valores);
            alto += altoFila;
        }
        pdf.ensureSpace(alto + 19f);
        float ySup = pdf.getY() + 3f;
        pdf.fillRect(m, ySup - alto, ancho, alto, PdfStyle.PANEL);
        pdf.fillRect(m, ySup - alto, 3.2f, alto, PdfStyle.ACCENT);
        float cursor = ySup - 17f;
        for (int fila = 0; fila < filas; fila++) {
            for (int col = 0; col < cols; col++) {
                int idx = fila * cols + col;
                if (idx >= datos.length) continue;
                float x = m + 15f + col * (anchoCampo + separacion);
                String etiqueta = txt(datos[idx][0]).toUpperCase(Locale.ROOT);
                pdf.text(PdfStyle.TXT_BOLD_FONT, 7, x, cursor,
                        etiqueta, PdfStyle.MUTED);
                List<String> lineas = celdas.get(fila).get(col);
                for (int l = 0; l < lineas.size(); l++) {
                    pdf.text(PdfStyle.TXT_FONT, 10, x, cursor - 14f - 13f * l,
                            lineas.get(l), PdfStyle.INK);
                }
            }
            cursor -= alturas.get(fila);
        }
        pdf.setY(ySup - alto - 17f);
    }

    static void tabla(PdfDocument pdf, String[] titulos, float[] pesos,
            List<String[]> filas) throws Exception {
        if (titulos == null || pesos == null || titulos.length != pesos.length)
            throw new IllegalArgumentException("Columnas de informe inconsistentes.");
        float[] anchos = new float[pesos.length];
        float[] posiciones = new float[pesos.length];
        float total = 0f;
        for (float peso : pesos) total += peso;
        float x = pdf.getMargin();
        for (int i = 0; i < pesos.length; i++) {
            anchos[i] = pdf.getUsableWidth() * pesos[i] / total;
            posiciones[i] = x;
            x += anchos[i];
        }
        cabeceraTabla(pdf, titulos, posiciones, anchos);
        int nroFila = 0;
        for (String[] datos : filas) {
            List<List<String>> celdas = new ArrayList<>();
            int cantidadLineas = 1;
            for (int i = 0; i < titulos.length; i++) {
                String valor = datos != null && i < datos.length ? txt(datos[i]) : "-";
                List<String> lineas = pdf.wrapText(PdfStyle.TXT_FONT, 8,
                        valor, Math.max(15f, anchos[i] - 10f));
                celdas.add(lineas);
                cantidadLineas = Math.max(cantidadLineas, lineas.size());
            }
            float alto = Math.max(20f, 8f + cantidadLineas * 11f);
            if (pdf.getY() - alto < pdf.getMargin() + 6f) {
                pdf.nuevaPagina();
                pdf.down(8f);
                cabeceraTabla(pdf, titulos, posiciones, anchos);
            }
            float y = pdf.getY();
            if (nroFila % 2 == 1) {
                pdf.fillRect(pdf.getMargin(), y - alto + 3f,
                        pdf.getUsableWidth(), alto, PdfStyle.ZEBRA);
            }
            for (int i = 0; i < titulos.length; i++) {
                for (int l = 0; l < celdas.get(i).size(); l++) {
                    pdf.text(PdfStyle.TXT_FONT, 8, posiciones[i] + 5f,
                            y - 11f - l * 11f, celdas.get(i).get(l), PdfStyle.INK);
                }
            }
            pdf.hline(pdf.getMargin(), pdf.getPageW() - pdf.getMargin(),
                    y - alto + 3f, PdfStyle.HAIR, 0.4f);
            pdf.down(alto);
            nroFila++;
        }
    }

    private static void cabeceraTabla(PdfDocument pdf, String[] titulos,
            float[] posiciones, float[] anchos) throws Exception {
        pdf.ensureSpace(40f);
        float y = pdf.getY();
        pdf.fillRect(pdf.getMargin(), y - 23f,
                pdf.getUsableWidth(), 27f, PdfStyle.PANEL);
        pdf.hline(pdf.getMargin(), pdf.getPageW() - pdf.getMargin(),
                y - 23f, PdfStyle.ACCENT, 1.2f);
        for (int i = 0; i < titulos.length; i++) {
            String t = txt(titulos[i]).toUpperCase(Locale.ROOT);
            pdf.text(PdfStyle.TXT_BOLD_FONT, 8, posiciones[i] + 5f,
                    y - 13f, t, PdfStyle.ACCENT_DARK);
        }
        pdf.down(28f);
    }
}
