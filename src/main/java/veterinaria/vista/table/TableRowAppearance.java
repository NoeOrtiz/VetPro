package veterinaria.vista.table;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JTable;
import javax.swing.UIManager;

/**
 * Pintura uniforme de todas las columnas de una fila (checkbox, textos y
 * estado). Los renderizadores comparten colores y origen del degradado.
 * No altera modelo, selección, filtros, permisos ni tamaños de las tablas.
 */
public final class TableRowAppearance {
    private TableRowAppearance() {}

    public static boolean temaOscuro() {
        Color fondo = UIManager.getColor("Panel.background");
        return fondo != null &&
                (fondo.getRed() * 299 + fondo.getGreen() * 587
                 + fondo.getBlue() * 114) / 1000 < 128;
    }

    public static Color fondo(JTable tabla, int fila) {
        Color alterna = UIManager.getColor("Table.alternateRowColor");
        return fila % 2 == 1 && alterna != null
                ? alterna : tabla.getBackground();
    }

    public static Color inicio(JTable tabla) {
        if (Boolean.TRUE.equals(tabla.getClientProperty("vetpro.gradienteCompanero"))) {
            return temaOscuro() ? new Color(169, 65, 44) : new Color(246, 193, 140);
        }
        return temaOscuro() ? new Color(42, 88, 104) : new Color(192, 223, 226);
    }

    public static Color fin(JTable tabla) {
        if (Boolean.TRUE.equals(tabla.getClientProperty("vetpro.gradienteCompanero"))) {
            return temaOscuro() ? new Color(44, 105, 177) : new Color(169, 203, 240);
        }
        return temaOscuro() ? new Color(54, 110, 137) : new Color(205, 229, 240);
    }

    public static void pintar(Graphics graphics, int ancho, int alto,
            boolean marcada, int desplazamientoX, int anchoTabla,
            Color inicio, Color fin, Color fondo) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            if (marcada) {
                g2.setPaint(new GradientPaint(desplazamientoX, 0, inicio,
                        desplazamientoX + Math.max(1, anchoTabla), 0, fin));
            } else {
                g2.setColor(fondo);
            }
            g2.fillRect(0, 0, ancho, alto);
        } finally {
            g2.dispose();
        }
    }
}
