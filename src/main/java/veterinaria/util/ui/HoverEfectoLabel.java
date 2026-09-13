/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package veterinaria.util.ui;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;

public class HoverEfectoLabel extends MouseAdapter {

    private final Color colorOriginal;
    private final Color colorHover;

    // Constructor que recibe el color base y el color cuando se pasa el mouse
    public HoverEfectoLabel(Color colorOriginal, Color colorHover) {
        this.colorOriginal = colorOriginal;
        this.colorHover = colorHover;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JLabel label = (JLabel) e.getSource();
        label.setForeground(colorHover);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JLabel label = (JLabel) e.getSource();
        label.setForeground(colorOriginal);
    }
}
