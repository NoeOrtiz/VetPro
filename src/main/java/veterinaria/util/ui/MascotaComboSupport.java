
package veterinaria.util.ui;

import java.awt.Component;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import veterinaria.controlador.MascotaControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Persona;

public final class MascotaComboSupport {

    private MascotaComboSupport() {}

    public static String buildMascotaDisplay(Mascota mascota) {
        return label(mascota);
    }

    public static String label(Mascota mascota) {
        if (mascota == null) return "";
        String id = "";
        try {
            id = String.valueOf(mascota.getIdMascota());
        } catch (Exception ex) {
            // ignore
        }

        String nombre = "";
        try {
            nombre = mascota.getNombre() != null ? mascota.getNombre() : "";
        } catch (Exception ex) {
            // ignore
        }

        String duenio = "";
        try {
            Cliente c = mascota.getCliente();
            if (c != null) {
                Persona p = c.getPersona();
                if (p != null) {
                    String nom = p.getNombre() != null ? p.getNombre() : "";
                    String ape = p.getApellido() != null ? p.getApellido() : "";
                    duenio = (nom + " " + ape).trim();
                }
                if (duenio.isEmpty() && c.getRazonSocial() != null) {
                    duenio = c.getRazonSocial().trim();
                }
            }
        } catch (Exception ex) {
            // ignore
        }

        if (!duenio.isEmpty()) {
            return id + " - " + nombre + " (" + duenio + ")";
        }
        return id + " - " + nombre;
    }

    public static void initRenderer(JComboBox<?> combo) {
        if (combo == null) return;

        @SuppressWarnings("unchecked")
        JComboBox<Object> cb = (JComboBox<Object>) combo;

        ListCellRenderer<? super Object> base = cb.getRenderer();
        cb.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                Object displayValue = value;
                if (value instanceof Mascota) {
                    displayValue = label((Mascota) value);
                } else if (value == null) {
                    displayValue = "";
                }

                Component c = super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);

                if (base != null && base.getClass() != BasicComboBoxRenderer.class) {
                
                }
                return c;
            }
        });
    }

    public static void cargarTodas(JComboBox<?> combo, MascotaControlador controlador, String placeholder) {
        if (combo == null || controlador == null) return;

        @SuppressWarnings("unchecked")
        JComboBox<Object> cb = (JComboBox<Object>) combo;

        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        if (placeholder != null && !placeholder.isBlank()) {
            model.addElement(placeholder);
        }

        List<Mascota> mascotas = controlador.buscarTodasLasMascotas();
        if (mascotas != null) {
            for (Mascota m : mascotas) {
                model.addElement(m);
            }
        }

        cb.setModel(model);
        if (model.getSize() > 0) {
            cb.setSelectedIndex(0);
        }
    }

    public static Mascota getSeleccionada(JComboBox<?> combo) {
        if (combo == null) return null;
        Object item = combo.getSelectedItem();
        if (item instanceof Mascota) {
            return (Mascota) item;
        }
        return null;
    }

    public static void seleccionarPorId(JComboBox<?> combo, Integer idMascota) {
        if (combo == null || idMascota == null) return;

        int n = combo.getItemCount();
        for (int i = 0; i < n; i++) {
            Object item = combo.getItemAt(i);
            if (item instanceof Mascota) {
                Mascota m = (Mascota) item;
                try {
                    if (m.getIdMascota() != null && m.getIdMascota().intValue() == idMascota.intValue()) {
                        combo.setSelectedIndex(i);
                        return;
                    }
                } catch (Exception ex) {

                }
            }
        }
    }
}
