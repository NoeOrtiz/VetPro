package veterinaria.reportes.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import veterinaria.reportes.pdf.PdfDocument;

/** Imagen institucional de la historia clínica, sin deformación ni márgenes transparentes. */
final class LogoHistoriaClinica {
    private LogoHistoriaClinica() {}

    static void dibujar(PdfDocument pdf, float x, float ySuperior,
            float anchoMaximo, float altoMaximo) throws Exception {
        try (InputStream entrada = LogoHistoriaClinica.class
                .getResourceAsStream("/imagenes/logo.png")) {
            if (entrada == null) return;
            BufferedImage original = ImageIO.read(entrada);
            if (original == null) return;

            int minX = original.getWidth(), minY = original.getHeight();
            int maxX = -1, maxY = -1;
            boolean alfa = original.getColorModel().hasAlpha();
            for (int yy = 0; yy < original.getHeight(); yy++) {
                for (int xx = 0; xx < original.getWidth(); xx++) {
                    int argb = original.getRGB(xx, yy);
                    int a = (argb >>> 24) & 0xff;
                    int r = (argb >>> 16) & 0xff;
                    int g = (argb >>> 8) & 0xff;
                    int b = argb & 0xff;
                    // Con transparencia, usar el alfa. Si hay fondo blanco,
                    // ignorar únicamente los píxeles casi blancos.
                    boolean visible = alfa ? a > 20 : (r < 246 || g < 246 || b < 246);
                    if (visible) {
                        minX = Math.min(minX, xx);
                        minY = Math.min(minY, yy);
                        maxX = Math.max(maxX, xx);
                        maxY = Math.max(maxY, yy);
                    }
                }
            }
            if (maxX < minX || maxY < minY) return;

            int margen = 8;
            int x0 = Math.max(0, minX - margen);
            int y0 = Math.max(0, minY - margen);
            int ancho = Math.min(original.getWidth() - x0, maxX - x0 + 1 + margen);
            int alto = Math.min(original.getHeight() - y0, maxY - y0 + 1 + margen);
            BufferedImage recortado = original.getSubimage(x0, y0, ancho, alto);

            float escala = Math.min(anchoMaximo / ancho, altoMaximo / alto);
            float w = ancho * escala;
            float h = alto * escala;
            PDImageXObject logo = LosslessFactory.createFromImage(
                    pdf.getDocument(), recortado);
            pdf.image(logo, x + (anchoMaximo - w) / 2f,
                    ySuperior - (altoMaximo - h) / 2f - h, w, h);
        }
    }
}
