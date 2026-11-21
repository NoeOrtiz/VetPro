package veterinaria.util.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Puente entre el estilo visual del compañero y los formularios funcionales
 * originales. No modifica modelos, listeners, filtros, columnas, ni permisos.
 */
public final class EstiloHibrido {
    private EstiloHibrido() {}

    public static void superficies(JPanel... paneles) {
        if (paneles == null) return;
        for (JPanel p : paneles) {
            if (p != null && p.isOpaque()) aplicarClase(p, "hibridoSuperficie");
        }
    }

    public static void titulos(JLabel... etiquetas) {
        if (etiquetas == null) return;
        for (JLabel l : etiquetas) if (l != null) aplicarClase(l, "hibridoTitulo");
    }

    public static void botonesPrimarios(JButton... botones) {
        botonesConClase("hibridoPrimario", false, botones);
    }

    public static void botonesSecundarios(JButton... botones) {
        botonesConClase("hibridoSecundario", false, botones);
    }

    public static void botonesPeligro(JButton... botones) {
        botonesConClase("hibridoPeligro", false, botones);
    }

    public static void botonesSutiles(JButton... botones) {
        botonesConClase("hibridoSutil", true, botones);
    }

    private static void botonesConClase(String clase, boolean sinBorde, JButton... botones) {
        if (botones == null) return;
        for (JButton boton : botones) {
            if (boton == null) continue;
            aplicarClase(boton, clase);
            if (sinBorde) boton.putClientProperty(
                    FlatClientProperties.BUTTON_TYPE,
                    FlatClientProperties.BUTTON_TYPE_BORDERLESS);
            else boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, null);
        }
    }

    /**
     * Conserva la casilla de selección y el modelo de columnas del original.
     * Solo copia dimensiones, bandas y visualización del estado de la otra UI.
     * Llamar después de configurar el modelo/renderizadores originales.
     */
    public static void tablaClientes(JTable tabla, int columnaEstadoModelo) {
        if (tabla == null || tabla.getColumnModel() == null) return;
        tabla.setRowHeight(Math.max(32, tabla.getRowHeight()));
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.putClientProperty("FlatLaf.styleClass", "hibridoTabla");
        if (tabla.getTableHeader() != null) {
            Dimension original = tabla.getTableHeader().getPreferredSize();
            tabla.getTableHeader().setPreferredSize(
                    new Dimension(original == null ? 0 : original.width, 34));
            tabla.getTableHeader().setReorderingAllowed(false);
        }
        for (int i = 0; i < tabla.getColumnModel().getColumnCount(); i++) {
            TableColumn columna = tabla.getColumnModel().getColumn(i);
            if (columna.getModelIndex() == columnaEstadoModelo) {
                columna.setCellRenderer(new EstadoClienteRenderer());
                break;
            }
        }
        tabla.repaint();
    }

    private static void aplicarClase(JComponent componente, String clase) {
        // Una clase de presentación por rol visual: no acumular estilos antiguos
        // que pueden impedir cambios de tema.
        componente.putClientProperty(FlatClientProperties.STYLE_CLASS, clase);
    }

    private static Color color(String key, Color fallback) {
        Color fromUI = UIManager.getColor(key);
        return fromUI == null ? fallback : fromUI;
    }

    /**
     * Píldora "Activo / Inactivo" como en la UI del compañero. La selección
     * continúa controlada por el checkbox y por el modelo original.
     */
    private static final class EstadoClienteRenderer extends JPanel
            implements TableCellRenderer {
        private final EstadoEtiqueta etiqueta = new EstadoEtiqueta();

        private EstadoClienteRenderer() {
            super(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
            add(etiqueta);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String estado = value == null ? "" : String.valueOf(value).trim();
            boolean activo = estado.equalsIgnoreCase("Activo");
            boolean inactivo = estado.equalsIgnoreCase("Inactivo");
            etiqueta.setText(estado);
            etiqueta.setFont(tabla.getFont().deriveFont(
                    Math.max(11f, tabla.getFont().getSize2D() - 1f)));
            etiqueta.fondo = activo
                    ? color("App.status.ok.background", new Color(31, 77, 58))
                    : inactivo
                        ? color("App.status.error.background", new Color(85, 52, 58))
                        : color("App.status.warning.background", new Color(87, 69, 32));
            etiqueta.setForeground(activo
                    ? color("App.status.ok.foreground", new Color(143, 227, 186))
                    : inactivo
                        ? color("App.status.error.foreground", new Color(243, 169, 169))
                        : color("App.status.warning.foreground", new Color(242, 205, 122)));
            Color alternate = UIManager.getColor("Table.alternateRowColor");
            setBackground((row % 2 == 1 && alternate != null)
                    ? alternate : tabla.getBackground());
            // No ocultar la selección del registro.
            if (isSelected) setBackground(tabla.getSelectionBackground());
            return this;
        }
    }

    private static final class EstadoEtiqueta extends JLabel {
        private Color fondo = new Color(31, 77, 58);

        EstadoEtiqueta() {
            setHorizontalAlignment(JLabel.CENTER);
            setBorder(BorderFactory.createEmptyBorder(2, 9, 2, 9));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 1, getWidth(), Math.max(1, getHeight() - 2), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
