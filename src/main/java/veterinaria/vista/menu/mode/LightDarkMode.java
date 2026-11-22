
package veterinaria.vista.menu.mode;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import veterinaria.util.ui.DateChooserThemeSupport;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Noelia
 */
public class LightDarkMode extends JPanel {

    public void setMenuFull(boolean menuFull) {
        this.menuFull = menuFull;
        if (menuFull) {
            buttonLight.setVisible(true);
            buttonDark.setVisible(true);
            buttonLighDark.setVisible(false);
        } else {
            buttonLight.setVisible(false);
            buttonDark.setVisible(false);
            buttonLighDark.setVisible(true);
        }
    }

    private boolean menuFull = true;

    public LightDarkMode() {
        init();
    }

    private void init() {
        setBorder(new EmptyBorder(2, 2, 2, 2));
        setLayout(new LightDarkModeLayout());
        // Sin el rectángulo que envolvía los dos botones: se ve el verde
        // original del menú. Solo la opción activa lleva una pastilla.
        setOpaque(false);
        buttonLight = new JButton("Claro", new FlatSVGIcon("veterinaria/menu/mode/light.svg"));
        buttonDark = new JButton("Oscuro", new FlatSVGIcon("veterinaria/menu/mode/dark.svg"));
        configurarBotonDeModo(buttonLight);
        configurarBotonDeModo(buttonDark);
        buttonLighDark = new JButton();
        configurarBotonDeModo(buttonLighDark);
        buttonLighDark.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Menu.lightdark.button.background;"
                + "foreground:$Menu.foreground;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");
        buttonLighDark.addActionListener((ActionEvent e) -> {
            changeMode(!FlatLaf.isLafDark());
        });
        checkStyle();
        buttonDark.addActionListener((ActionEvent e) -> {
            changeMode(true);
        });
        buttonLight.addActionListener((ActionEvent e) -> {
            changeMode(false);
        });

        add(buttonLight);
        add(buttonDark);
        add(buttonLighDark);
    }

    private void changeMode(boolean dark) {
        if (FlatLaf.isLafDark() != dark) {
            if (dark) {
                EventQueue.invokeLater(() -> {
                    FlatAnimatedLafChange.showSnapshot();
                    FlatMacDarkLaf.setup();
                    FlatLaf.updateUI();
                    aplicarColoresComponentesEspeciales();
                    checkStyle();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                });
            } else {
                EventQueue.invokeLater(() -> {
                    FlatAnimatedLafChange.showSnapshot();
                    FlatMacLightLaf.setup();
                    FlatLaf.updateUI();
                    aplicarColoresComponentesEspeciales();
                    checkStyle();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                });
            }
        }
    }

    private void aplicarColoresComponentesEspeciales() {
        DateChooserThemeSupport.aplicarEnVentanasAbiertas();
    }

    private void checkStyle() {
        // FlatLaf.updateUI() no debe reconstruir el panel rectangular.
        setOpaque(false);
        boolean isDark = FlatLaf.isLafDark();
        addStyle(buttonLight, !isDark);
        addStyle(buttonDark, isDark);
        if (isDark) {
            buttonLighDark.setIcon(new FlatSVGIcon("veterinaria/menu/mode/dark.svg"));
        } else {
            buttonLighDark.setIcon(new FlatSVGIcon("veterinaria/menu/mode/light.svg"));
        }
    }

    private void configurarBotonDeModo(JButton button) {
        // FlatLaf renderiza la forma redondeada de manera nativa.
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, "roundRect");
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
    }

    private void addStyle(JButton button, boolean activo) {
        // La opción inactiva no tiene fondo ni borde; es texto e ícono
        // directamente sobre el verde del menú, como en la segunda captura.
        button.setContentAreaFilled(activo);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.putClientProperty(FlatClientProperties.STYLE, activo
                ? "background:$Menu.lightdark.button.background;"
                    + "foreground:$Menu.foreground;"
                    + "focusWidth:0;innerFocusWidth:0"
                : "foreground:$Menu.foreground;"
                    + "focusWidth:0;innerFocusWidth:0");
    }

    private JButton buttonLight;
    private JButton buttonDark;
    private JButton buttonLighDark;

    private class LightDarkModeLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(5, buttonDark.getPreferredSize().height + (menuFull ? 0 : 5));
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(0, 0);
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int gap = 5;
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int buttonWidth = (width - gap) / 2;
                if (menuFull) {
                    buttonLight.setBounds(x, y, buttonWidth, height);
                    buttonDark.setBounds(x + buttonWidth + gap, y, buttonWidth, height);
                } else {
                    buttonLighDark.setBounds(x, y, width, height);
                }
            }
        }
    }
}

