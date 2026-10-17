package raven.tabbed;

import java.awt.Color;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

/** Pestañas de estilo Raven adaptadas a los colores activos de FlatLaf. */
public class TabbedPaneCustom extends JTabbedPane {
    private Color selectedColor;
    private Color unselectedColor;

    public TabbedPaneCustom() {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        setOpaque(false);
        setUI(new TabbedPaneCustomUI(this));
    }

    public Color getSelectedColor() {
        Color fallback = UIManager.getColor("TabbedPane.selectedBackground");
        if (fallback == null) fallback = UIManager.getColor("Component.focusColor");
        if (fallback == null) fallback = UIManager.getColor("Table.selectionBackground");
        return selectedColor != null ? selectedColor : fallback != null ? fallback : getBackground();
    }

    public void setSelectedColor(Color color) {
        selectedColor = color;
        repaint();
    }

    public Color getUnselectedColor() {
        Color color = UIManager.getColor("TabbedPane.background");
        return unselectedColor != null ? unselectedColor : color != null ? color : getBackground();
    }

    public void setUnselectedColor(Color color) {
        unselectedColor = color;
        repaint();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setUI(new TabbedPaneCustomUI(this));
    }
}
