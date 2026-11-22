package veterinaria.quality;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import veterinaria.util.ui.EstiloHibrido;
import veterinaria.vista.menu.MenuItem;
import veterinaria.vista.menu.PopupSubmenu;
import veterinaria.vista.table.TableGradientCheckBoxRenderer;
import veterinaria.vista.table.TableRowAppearance;

/**
 * Pruebas de contraste y uniformidad en Claro/Oscuro.
 * Usa datos ficticios; no abre ventanas ni requiere base de datos.
 */
class MenuClaroFilasUniformesTest {

    @Test
    void menusMantienenContrasteYSeleccionEnAmbosTemas() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        for (boolean oscuro : new boolean[]{false, true}) {
            SwingUtilities.invokeAndWait(() -> {
                if (oscuro) FlatMacDarkLaf.setup();
                else FlatMacLightLaf.setup();
                Color fondo = UIManager.getColor("Menu.background");
                Color texto = UIManager.getColor("Menu.foreground");
                Color seleccionado = UIManager.getColor("Menu.button.selectedBackground");
                Color textoSeleccionado = UIManager.getColor("Menu.button.selectedForeground");
                Color hover = UIManager.getColor("Menu.button.hoverBackground");
                assertNotNull(fondo);
                assertNotNull(texto);
                assertNotNull(seleccionado);
                assertNotNull(textoSeleccionado);
                assertNotNull(hover);
                assertTrue(contraste(fondo, texto) > 4.5,
                        "Sidebar con contraste insuficiente");
                assertTrue(contraste(seleccionado, textoSeleccionado) > 4.5,
                        "La selección debe ser legible");
                assertTrue(contraste(hover, texto) > 4.5,
                        "El hover del menú no debe blanquear el texto");
                MenuItem menu = new MenuItem(null,
                        new String[]{"CLIENTE", "Gestión de cliente"}, 1, java.util.List.of());
                assertEquals(2, menu.getComponentCount());
                for (Component c : menu.getComponents())
                    assertInstanceOf(JButton.class, c);
                PopupSubmenu popup = new PopupSubmenu(
                        java.awt.ComponentOrientation.LEFT_TO_RIGHT, null, 1,
                        new String[]{"CLIENTE","Gestión de cliente"});
                assertEquals(1, popup.getComponentCount());
                SwingUtilities.updateComponentTreeUI(menu);
                SwingUtilities.updateComponentTreeUI(popup);
            });
        }
    }

    @Test
    void filasZebraYSeleccionLleganDeLaPrimeraALaUltimaColumna() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        for (boolean oscuro : new boolean[]{false, true}) {
            SwingUtilities.invokeAndWait(() -> {
                if (oscuro) FlatMacDarkLaf.setup();
                else FlatMacLightLaf.setup();
                DefaultTableModel modelo = new DefaultTableModel(
                        new String[]{"Seleccionar","Nombre","Usuario","Estado"},0) {
                    @Override public Class<?> getColumnClass(int col) {
                        return col == 0 ? Boolean.class : String.class;
                    }
                    @Override public boolean isCellEditable(int row, int col) {
                        return col == 0;
                    }
                };
                modelo.addRow(new Object[]{false, "Usuaria", "prueba", "Activo"});
                modelo.addRow(new Object[]{false, "Otro", "usuario2", "Inactivo"});
                JTable tabla = new JTable(modelo);
                tabla.setSize(824, 90);
                EstiloHibrido.tablaClientes(tabla, 3);
                tabla.getColumnModel().getColumn(0).setCellRenderer(
                        new TableGradientCheckBoxRenderer());
                tabla.doLayout();
                assertEquals(0,tabla.getRowMargin());
                assertEquals(0,tabla.getColumnModel().getColumnMargin());

                for (int fila = 0; fila < 2; fila++) {
                    Color esperado = TableRowAppearance.fondo(tabla, fila);
                    for (int col = 0; col < 4; col++) {
                        Color actual = pintarYLeer(tabla, fila, col);
                        assertColorSimilar(esperado, actual, 3,
                                "Fondo de fila " + fila + " columna " + col);
                    }
                }
                modelo.setValueAt(Boolean.TRUE, 0, 0);
                for (int col = 0; col < 3; col++) {
                    Color a = pintarYLeer(tabla, 0, col);
                    Color b = pintarYLeer(tabla, 0, col + 1);
                    // El degradado es horizontal: puede cambiar de color
                    // entre centros, pero nunca regresar a un gris plano.
                    assertNotEquals(TableRowAppearance.fondo(tabla,0).getRGB(), a.getRGB(),
                            "Selección incompleta en columna " + col);
                    assertNotEquals(TableRowAppearance.fondo(tabla,0).getRGB(), b.getRGB(),
                            "Selección incompleta en columna " + (col+1));
                }
                modelo.setValueAt(Boolean.FALSE, 0, 0);
                assertColorSimilar(TableRowAppearance.fondo(tabla,0),
                        pintarYLeer(tabla,0,3),3,"Restaura fondo al desmarcar");
            });
        }
    }

    private static Color pintarYLeer(JTable tabla, int fila, int col) {
        TableCellRenderer renderer = tabla.getCellRenderer(fila,col);
        Component comp = renderer.getTableCellRendererComponent(tabla,
                tabla.getValueAt(fila,col),false,false,fila,col);
        int w = tabla.getCellRect(fila,col,true).width;
        int h = tabla.getRowHeight(fila);
        comp.setSize(w,h);
        BufferedImage imagen = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        try {comp.paint(g);}finally {g.dispose();}
        return new Color(imagen.getRGB(Math.max(1,w-3),Math.max(1,h-3)), true);
    }

    private static void assertColorSimilar(Color esperado, Color actual, int tolerancia,
            String mensaje) {
        assertTrue(Math.abs(esperado.getRed()-actual.getRed())<=tolerancia
              && Math.abs(esperado.getGreen()-actual.getGreen())<=tolerancia
              && Math.abs(esperado.getBlue()-actual.getBlue())<=tolerancia,
              mensaje + ": esperado " + esperado + " recibido " + actual);
    }

    private static double luminancia(Color c) {
        double[] rgb = {c.getRed()/255d,c.getGreen()/255d,c.getBlue()/255d};
        double l=0d;
        for (int i=0;i<3;i++) {
            double v=rgb[i];v=v<=0.04045?v/12.92:Math.pow((v+0.055)/1.055,2.4);
            l+=v*(i==0?0.2126:i==1?0.7152:0.0722);
        }
        return l;
    }

    private static double contraste(Color a, Color b) {
        double x=luminancia(a),y=luminancia(b);
        return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05);
    }
}
