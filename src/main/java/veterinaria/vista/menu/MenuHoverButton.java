package veterinaria.vista.menu;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.UIManager;

/**
 * Submenú integrado al lateral. Solo destaca al pasar el mouse con un
 * rectángulo de esquinas redondeadas; nunca deja placas visibles en reposo.
 * Consulta la paleta de FlatLaf al pintar para respetar los temas existentes.
 */
public final class MenuHoverButton extends JButton {

    public MenuHoverButton(String text) {
        super(text);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isRollover() || getModel().isPressed()) {
            Color color = UIManager.getColor(getModel().isPressed()
                    ? "Menu.button.pressedBackground" : "Menu.button.hoverBackground");
            if (color != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    int margen = 2;
                    g2.setColor(color);
                    g2.fillRoundRect(margen, margen,
                            Math.max(0, getWidth() - margen * 2),
                            Math.max(0, getHeight() - margen * 2), 12, 12);
                } finally {
                    g2.dispose();
                }
            }
        }
        // El texto/icono, foco y accesibilidad siguen siendo los de JButton.
        super.paintComponent(g);
    }
}
