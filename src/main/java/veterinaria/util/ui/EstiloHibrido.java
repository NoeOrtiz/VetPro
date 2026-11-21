package veterinaria.util.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Adapta solo superficies y títulos del diseño visual, sin modificar
 * comportamiento, tamaños de las tablas ni botones.
 */
public final class EstiloHibrido {
    private EstiloHibrido() {}

    public static void superficies(JPanel... paneles) {
        if (paneles == null) return;
        for (JPanel panel : paneles) {
            if (panel != null && panel.isOpaque()) anadirClase(panel, "hibridoSuperficie");
        }
    }

    public static void titulos(JLabel... etiquetas) {
        if (etiquetas == null) return;
        for (JLabel etiqueta : etiquetas) {
            if (etiqueta != null) anadirClase(etiqueta, "hibridoTitulo");
        }
    }

    private static void anadirClase(JComponent componente, String estilo) {
        Object actual = componente.getClientProperty("FlatLaf.styleClass");
        String nombres = actual instanceof String ? ((String) actual).trim() : "";
        if (java.util.Arrays.asList(nombres.split("\\s+")).contains(estilo)) return;
        componente.putClientProperty("FlatLaf.styleClass",
                nombres.isEmpty() ? estilo : nombres + " " + estilo);
    }
}
