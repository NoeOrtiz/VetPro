package veterinaria.reportes.pdf;
import java.awt.Color;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/** Paleta de impresión VetPRO: sobria, sin naranja y legible en escala de grises. */
public final class PdfStyle {
    private PdfStyle() {}
    public static final PDFont H1_FONT = PDType1Font.HELVETICA_BOLD;
    public static final PDFont H2_FONT = PDType1Font.HELVETICA_BOLD;
    public static final PDFont TXT_FONT = PDType1Font.HELVETICA;
    public static final PDFont TXT_BOLD_FONT = PDType1Font.HELVETICA_BOLD;

    public static final Color INK = new Color(0x20, 0x30, 0x3D);
    public static final Color MUTED = new Color(0x64, 0x74, 0x82);
    public static final Color HAIR = new Color(0xD6, 0xE1, 0xE5);
    public static final Color ACCENT = new Color(0x17, 0x76, 0x82);
    public static final Color ACCENT_DARK = new Color(0x0E, 0x4D, 0x5B);
    public static final Color ACCENT_SOFT = new Color(0xE8, 0xF5, 0xF5);
    public static final Color ZEBRA = new Color(0xF6, 0xF9, 0xFA);
    public static final Color PANEL = new Color(0xED, 0xF3, 0xF5);
}
