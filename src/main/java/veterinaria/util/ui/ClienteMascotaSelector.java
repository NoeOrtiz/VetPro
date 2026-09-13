package veterinaria.util.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;

import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.MascotaControlador;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;

public class ClienteMascotaSelector {

    public interface MascotaListener {
        void onMascotaSeleccionada(Mascota mascota);
    }

    private final JComboBox cbCliente;
    private final JComboBox cbMascota;
    private final ClienteControlador clienteControlador;
    private final MascotaControlador mascotaControlador;
    private final MascotaListener mascotaListener;

    private boolean updating = false;

    public ClienteMascotaSelector(JComboBox cbCliente,
                                 JComboBox cbMascota,
                                 ClienteControlador clienteControlador,
                                 MascotaControlador mascotaControlador,
                                 MascotaListener mascotaListener) {
        this.cbCliente = cbCliente;
        this.cbMascota = cbMascota;
        this.clienteControlador = clienteControlador;
        this.mascotaControlador = mascotaControlador;
        this.mascotaListener = mascotaListener;
    }

    public void init() {
        initRenderers();
        initListeners();
        cargarClientes();
        limpiarComboMascotas();
    }

    public void reset() {
        setUpdating(true);
        try {
            cargarClientes();
            limpiarComboMascotas();
        } finally {
            setUpdating(false);
        }
    }

    public void setUpdating(boolean value) {
        this.updating = value;
    }

    public boolean isUpdating() {
        return updating;
    }

    public Cliente getClienteSeleccionado() {
        Object item = cbCliente.getSelectedItem();
        return (item instanceof Cliente) ? (Cliente) item : null;
    }

    public Mascota getMascotaSeleccionada() {
        Object item = cbMascota.getSelectedItem();
        return (item instanceof Mascota) ? (Mascota) item : null;
    }

    public void seleccionarPorIds(Integer idCliente, Integer idMascota) {
        setUpdating(true);
        try {
            if (idCliente != null) {
                seleccionarClientePorId(idCliente);
                Cliente c = getClienteSeleccionado();
                cargarMascotasParaCliente(c);
            } else {
                cbCliente.setSelectedIndex(0);
                limpiarComboMascotas();
            }

            if (idMascota != null) {
                seleccionarMascotaPorId(idMascota);
            } else {
                cbMascota.setSelectedIndex(0);
            }

            notificarMascota();
        } finally {
            setUpdating(false);
        }
    }

    public void cargarClientes() {
        DefaultComboBoxModel<Object> modelClientes = new DefaultComboBoxModel<>();
        modelClientes.addElement("Seleccionar Dueño");
        try {
            List<Cliente> clientes = clienteControlador.buscarTodosLosClientes();
            if (clientes != null) {
                clientes.sort((a, b) -> {
                    String ka = clienteSortKey(a);
                    String kb = clienteSortKey(b);
                    return ka.compareToIgnoreCase(kb);
                });
                for (Cliente c : clientes) {
                    modelClientes.addElement(c);
                }
            }
        } catch (Exception ignore) {
        }
        cbCliente.setModel(modelClientes);
    }

    public void cargarMascotasParaCliente(Cliente c) {
        limpiarComboMascotas();
        if (c == null || c.getIdCliente() == null) return;

        List<Mascota> todas = mascotaControlador.buscarTodasLasMascotas();
        if (todas == null) return;

        List<Mascota> filtradas = new ArrayList<>();
        for (Mascota m : todas) {
            try {
                if (m != null && m.getCliente() != null && m.getCliente().getIdCliente() != null
                        && m.getCliente().getIdCliente().equals(c.getIdCliente())) {
                    filtradas.add(m);
                }
            } catch (Exception ignore) {
            }
        }

        filtradas.sort((a, b) -> safe(a != null ? a.getNombre() : "").compareToIgnoreCase(safe(b != null ? b.getNombre() : "")));

        DefaultComboBoxModel<Object> modelMascotas = new DefaultComboBoxModel<>();
        modelMascotas.addElement("Seleccionar Mascota");
        for (Mascota m : filtradas) {
            modelMascotas.addElement(m);
        }
        cbMascota.setModel(modelMascotas);
    }

    public void limpiarComboMascotas() {
        DefaultComboBoxModel<Object> modelMascotas = new DefaultComboBoxModel<>();
        modelMascotas.addElement("Seleccionar Mascota");
        cbMascota.setModel(modelMascotas);
    }

    private void initRenderers() {
        cbCliente.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente) {
                    setText(nombreCliente((Cliente) value));
                } else if (value != null) {
                    setText(value.toString());
                } else {
                    setText("");
                }
                return this;
            }
        });

        cbMascota.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Mascota) {
                    setText(nombreMascota((Mascota) value));
                } else if (value != null) {
                    setText(value.toString());
                } else {
                    setText("");
                }
                return this;
            }
        });
    }

    private void initListeners() {
        cbCliente.addActionListener(e -> {
            if (updating) return;
            try {
                Cliente c = getClienteSeleccionado();
                setUpdating(true);
                try {
                    cargarMascotasParaCliente(c);
                    cbMascota.setSelectedIndex(0);
                } finally {
                    setUpdating(false);
                }
                notificarMascota();
            } catch (Exception ignore) {
            }
        });

        cbMascota.addActionListener(e -> {
            if (updating) return;
            notificarMascota();
        });
    }

    private void notificarMascota() {
        if (mascotaListener == null) return;
        try {
            mascotaListener.onMascotaSeleccionada(getMascotaSeleccionada());
        } catch (Exception ignore) {
        }
    }

    private void seleccionarClientePorId(Integer idCliente) {
        if (idCliente == null) return;
        for (int i = 0; i < cbCliente.getItemCount(); i++) {
            Object it = cbCliente.getItemAt(i);
            if (it instanceof Cliente) {
                Cliente c = (Cliente) it;
                if (c.getIdCliente() != null && c.getIdCliente().equals(idCliente)) {
                    cbCliente.setSelectedIndex(i);
                    return;
                }
            }
        }
        cbCliente.setSelectedIndex(0);
    }

    private void seleccionarMascotaPorId(Integer idMascota) {
        if (idMascota == null) return;
        for (int i = 0; i < cbMascota.getItemCount(); i++) {
            Object it = cbMascota.getItemAt(i);
            if (it instanceof Mascota) {
                Mascota m = (Mascota) it;
                if (m.getIdMascota() != null && m.getIdMascota().equals(idMascota)) {
                    cbMascota.setSelectedIndex(i);
                    return;
                }
            }
        }
        cbMascota.setSelectedIndex(0);
    }

    private String clienteSortKey(Cliente c) {
        if (c == null) return "";
        if (c.getPersona() != null) {
            String a = safe(c.getPersona().getApellido());
            String n = safe(c.getPersona().getNombre());
            String key = (a + " " + n).trim();
            if (!key.isBlank()) return key;
        }
        if (c.getRazonSocial() != null && !c.getRazonSocial().isBlank()) return c.getRazonSocial().trim();
        return (c.getIdCliente() != null) ? ("Cliente " + c.getIdCliente()) : "Cliente";
    }

    private String nombreCliente(Cliente c) {
        if (c == null) return "";
        if (c.getPersona() != null) {
            String n = c.getPersona().getNombre() != null ? c.getPersona().getNombre().trim() : "";
            String a = c.getPersona().getApellido() != null ? c.getPersona().getApellido().trim() : "";
            String full = (n + " " + a).trim();
            if (!full.isBlank()) return full;
        }
        if (c.getRazonSocial() != null && !c.getRazonSocial().isBlank()) return c.getRazonSocial();
        return (c.getIdCliente() != null) ? ("Cliente #" + c.getIdCliente()) : "Cliente";
    }

    private String nombreMascota(Mascota m) {
        if (m == null) return "";
        String nombre = safe(m.getNombre());
        if (nombre.isBlank()) nombre = (m.getIdMascota() != null) ? ("Mascota #" + m.getIdMascota()) : "Mascota";
        String especie = safe(m.getEspecie());
        String raza = safe(m.getRaza());
        if (especie.isBlank() && raza.isBlank()) {
            return nombre;
        }
        if (!especie.isBlank() && !raza.isBlank()) {
            return nombre + " (" + especie + "/" + raza + ")";
        }
        return nombre + " (" + (!especie.isBlank() ? especie : raza) + ")";
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
