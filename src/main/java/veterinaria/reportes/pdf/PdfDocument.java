package veterinaria.reportes.pdf;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class PdfDocument implements Closeable {

    private final PDDocument doc;
    private final PDRectangle baseRect;
    private final boolean landscape;
    private float pageH;

    private PDPage page;
    private PDPageContentStream cs;

    private float margin = 50f;
    private float y;
    private float pageW;
    private int currentPage = 0;

    public PdfDocument() throws IOException {
        this(PDRectangle.A4, false);
    }

    public PdfDocument(boolean landscape) throws IOException {
        this(PDRectangle.A4, landscape);
    }

    public PdfDocument(PDRectangle baseRect, boolean landscape) throws IOException {
        this.doc = new PDDocument();
        this.baseRect = baseRect != null ? baseRect : PDRectangle.A4;
        this.landscape = landscape;
        nuevaPagina();
    }

    private PDRectangle currentRect() {
        if (!landscape) {
            return baseRect;
        }
        return new PDRectangle(baseRect.getHeight(), baseRect.getWidth());
    }

    public void nuevaPagina() throws IOException {

        if (cs != null) {
            cs.close();
        }

        page = new PDPage(currentRect());
        doc.addPage(page);

        currentPage++;

        cs = new PDPageContentStream(doc, page);

        pageW = page.getMediaBox().getWidth();
        pageH = page.getMediaBox().getHeight();

        y = pageH - margin;
    }

    public float getPageH() {
        return pageH;
    }

    public float getPageWidth() {
        return pageW;
    }

    public float getPageHeight() {
        return pageH;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public float getMargin() {
        return margin;
    }

    public float getY() {
        return y;
    }

    public float getPageW() {
        return pageW;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void rect(float x, float y, float w, float h) throws IOException {
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    public void line() throws IOException {

        cs.moveTo(margin, y);
        cs.lineTo(pageW - margin, y);
        cs.stroke();

        y -= 8f;
    }

    public void text(PDFont font, int size, float x, float y, String t) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(t != null ? t : "");
        cs.endText();
    }

    public void textLeft(PDFont font, int size, float x, String t) throws IOException {
        text(font, size, x, y, t);
    }

    public void textRight(PDFont font, int size, float xRight, String t) throws IOException {
        String s = t != null ? t : "";
        float w = font.getStringWidth(s) / 1000f * size;
        text(font, size, xRight - w, y, s);
    }

    public float textWidth(PDFont font, int size, String t) throws IOException {
        String s = (t != null) ? t : "";
        return font.getStringWidth(s) / 1000f * size;
    }

    public List<String> wrapText(PDFont font, int size, String text, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        if (text == null) {
            out.add("");
            return out;
        }
        String[] paragraphs = text.replace("\r", "").split("\n", -1);
        for (String p : paragraphs) {
            String s = (p == null) ? "" : p.trim();
            if (s.isEmpty()) {
                out.add("");
                continue;
            }

            String[] words = s.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (w == null || w.isEmpty()) {
                    continue;
                }

                if (textWidth(font, size, w) > maxWidth) {
                    if (line.length() > 0) {
                        out.add(line.toString());
                        line.setLength(0);
                    }
                    out.addAll(splitLongWord(font, size, w, maxWidth));
                    continue;
                }

                String candidate = (line.length() == 0) ? w : (line + " " + w);
                if (textWidth(font, size, candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                }
            }
            if (line.length() > 0) {
                out.add(line.toString());
            }
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }

    private List<String> splitLongWord(PDFont font, int size, String word, float maxWidth) throws IOException {
        List<String> parts = new ArrayList<>();
        if (word == null || word.isEmpty()) {
            parts.add("");
            return parts;
        }
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String candidate = chunk.toString() + c;
            if (textWidth(font, size, candidate) <= maxWidth) {
                chunk.append(c);
            } else {
                if (chunk.length() > 0) {
                    parts.add(chunk.toString());
                }
                chunk.setLength(0);
                chunk.append(c);
            }
        }
        if (chunk.length() > 0) {
            parts.add(chunk.toString());
        }
        return parts;
    }

    public void down(float dy) {
        y -= dy;
    }

    public void save(File out) throws IOException {

        if (cs != null) {
            cs.close();
            cs = null;
        }

        doc.save(out);
    }

    public void ensureSpace(float needed) throws IOException {
        if (y - needed < margin) {
            nuevaPagina();
        }
    }

    public PDDocument getDocument() {
        return doc;
    }

    public void image(PDImageXObject image,
            float x,
            float y,
            float width,
            float height) throws IOException {

        cs.drawImage(image, x, y, width, height);
    }

    public float getUsableWidth() {
        return pageW - (margin * 2);
    }

    public float getUsableHeight() {
        return pageH - (margin * 2);
    }

    public float centerX(float textWidth) {
        return (pageW - textWidth) / 2f;
    }

    public boolean isLandscape() {
        return landscape;
    }

    @Override
    public void close() throws IOException {

        if (cs != null) {
            cs.close();
            cs = null;
        }

        doc.close();
    }
}
