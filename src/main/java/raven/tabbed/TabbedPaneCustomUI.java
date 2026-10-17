package raven.tabbed;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** Navegación redondeada y legible tanto en FlatLaf claro como oscuro. */
public class TabbedPaneCustomUI extends BasicTabbedPaneUI {
    private final TabbedPaneCustom tabs;

    public TabbedPaneCustomUI(TabbedPaneCustom tabs) {
        this.tabs = tabs;
    }

    @Override
    protected Insets getTabInsets(int placement, int index) {
        return new Insets(9, 15, 9, 15);
    }

    @Override
    protected void paintTabBorder(Graphics g, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        // El borde se dibuja con el fondo redondeado.
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
            int index, Rectangle iconRect, Rectangle textRect, boolean selected) {
    }

    @Override
    protected void paintTabBackground(Graphics g, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background = selected ? tabs.getSelectedColor() : tabs.getUnselectedColor();
        g2.setColor(background);
        g2.fill(new RoundRectangle2D.Float(x + 2, y + 2, width - 4, height - 3, 12, 12));
        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int placement, java.awt.Font font,
            java.awt.FontMetrics metrics, int index, String title,
            Rectangle textRect, boolean selected) {
        Color fg = UIManager.getColor("TabbedPane.foreground");
        if (fg == null) fg = tabs.getForeground();
        g.setColor(fg);
        g.setFont(tabs.getFont());
        int mnemonic = tabs.getDisplayedMnemonicIndexAt(index);
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(
                g, title, mnemonic, textRect.x, textRect.y + metrics.getAscent());
    }

    @Override
    protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
        // Sin marco blanco ajeno al tema.
    }
}
