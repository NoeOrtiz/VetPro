
package veterinaria.reportes.pdf;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class PdfStyle {

    private PdfStyle() {}

    public static final PDFont H1_FONT = PDType1Font.HELVETICA_BOLD;
    public static final PDFont H2_FONT = PDType1Font.HELVETICA_BOLD;
    public static final PDFont TXT_FONT = PDType1Font.HELVETICA;
    public static final PDFont TXT_BOLD_FONT = PDType1Font.HELVETICA_BOLD;
}
