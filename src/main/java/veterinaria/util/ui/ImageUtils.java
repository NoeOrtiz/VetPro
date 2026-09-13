package veterinaria.util.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static ImageIcon toCircularIcon(ImageIcon source, int sizePx) {
        if (source == null || source.getImage() == null) {
            return null;
        }
        BufferedImage buffered = toBufferedImage(source.getImage());
        if (buffered == null) {
            return null;
        }
        BufferedImage scaled = scaleToSquare(buffered, sizePx);
        return new ImageIcon(circleCrop(scaled));
    }

    public static ImageIcon loadCircularFromUrl(String url, int sizePx) throws Exception {
        BufferedImage img = loadImageFromUrl(url);
        if (img == null) {
            return null;
        }
        BufferedImage scaled = scaleToSquare(img, sizePx);
        return new ImageIcon(circleCrop(scaled));
    }

    private static BufferedImage loadImageFromUrl(String url) throws Exception {
        URLConnection con = new URL(url).openConnection();
        con.setConnectTimeout(2500);
        con.setReadTimeout(4000);
        con.setUseCaches(false);

        try (InputStream is = new BufferedInputStream(con.getInputStream())) {
            return ImageIO.read(is);
        }
    }

    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        int w = Math.max(1, img.getWidth(null));
        int h = Math.max(1, img.getHeight(null));
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = b.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return b;
    }

    private static BufferedImage scaleToSquare(BufferedImage src, int size) {
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = Math.max((double) size / src.getWidth(), (double) size / src.getHeight());
        int sw = (int) Math.round(src.getWidth() * scale);
        int sh = (int) Math.round(src.getHeight() * scale);
        int x = (size - sw) / 2;
        int y = (size - sh) / 2;

        g2.drawImage(src, x, y, sw, sh, null);
        g2.dispose();
        return dst;
    }

    private static BufferedImage circleCrop(BufferedImage square) {
        int size = Math.min(square.getWidth(), square.getHeight());
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Double(0, 0, size, size));
        g2.drawImage(square, 0, 0, size, size, null);
        g2.dispose();
        return out;
    }
}
