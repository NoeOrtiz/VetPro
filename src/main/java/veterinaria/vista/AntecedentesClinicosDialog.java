package veterinaria.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.*;
import veterinaria.entidad.HistoriaAnamnesis;
import veterinaria.entidad.Mascota;
import veterinaria.persistencia.HistoriaAnamnesisDAO;
import veterinaria.vista.application.Application;

/**
 * Captura solo antecedentes complementarios. Los datos de identidad, edad,
 * peso y propietario pertenecen a Mascota y nunca se duplican aquí.
 */
public final class AntecedentesClinicosDialog {
    private AntecedentesClinicosDialog() {}

    public static boolean mostrar(java.awt.Component padre, Mascota paciente) {
        if (paciente == null || paciente.getIdMascota() == null) {
            JOptionPane.showMessageDialog(padre, "Seleccioná una mascota registrada.");
            return false;
        }
        HistoriaAnamnesisDAO dao = new HistoriaAnamnesisDAO();
        try {
            HistoriaAnamnesis anterior = dao.ultima(paciente.getIdMascota());
            JTextArea dieta = area(anterior == null ? null : anterior.getDieta());
            JTextArea enfermedades = area(anterior == null ? null : anterior.getEnfermedadesPrevias());
            JTextArea cirugias = area(anterior == null ? null : anterior.getCirugiasPrevias());
            JTextArea vacunas = area(anterior == null ? null : anterior.getEsquemaVacunal());
            JTextArea desparasitacion = area(anterior == null ? null : anterior.getDesparasitacion());
            JTextArea tratamientos = area(anterior == null ? null : anterior.getTratamientosRecientes());
            JTextArea viajes = area(anterior == null ? null : anterior.getViajesRecientes());
            JTextArea convivencia = area(anterior == null ? null : anterior.getConviveConAnimales());
            JTextArea comportamiento = area(anterior == null ? null : anterior.getComportamiento());
            JTextArea senales = area(anterior == null ? null : anterior.getSenalesParticulares());
            JComboBox<String> esterilizado = new JComboBox<>(
                    new String[]{"Sin registrar", "Sí", "No"});
            if (anterior != null) esterilizado.setSelectedItem(
                    anterior.getEsterilizacion() == null || anterior.getEsterilizacion().isBlank()
                            ? "Sin registrar" : anterior.getEsterilizacion());
            JTextField partos = new JTextField(anterior == null || anterior.getNumeroPartos() == null
                    ? "" : anterior.getNumeroPartos());

            JPanel raiz = new JPanel();
            raiz.setLayout(new BoxLayout(raiz, BoxLayout.Y_AXIS));
            raiz.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            JLabel explicacion = new JLabel("<html><b>" + html(paciente.getNombre())
                    + "</b> · Los datos registrados de la mascota se completan automáticamente.</html>");
            explicacion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            raiz.add(explicacion);
            raiz.add(Box.createVerticalStrut(15));
            raiz.add(grupo("ANTECEDENTES MÉDICOS",
                    new String[]{"Enfermedades previas", "Cirugías previas", "Tratamientos recientes"},
                    new JComponent[]{enfermedades, cirugias, tratamientos}));
            raiz.add(Box.createVerticalStrut(12));
            raiz.add(grupo("PREVENCIÓN Y REPRODUCCIÓN",
                    new String[]{"Vacunación", "Última desparasitación y producto",
                        "Esterilizado", "Número de partos"},
                    new JComponent[]{vacunas, desparasitacion, esterilizado, partos}));
            raiz.add(Box.createVerticalStrut(12));
            raiz.add(grupo("HÁBITOS Y OTROS DATOS",
                    new String[]{"Dieta", "Comportamiento", "Convive con otros animales",
                        "Viajes recientes", "Señales particulares"},
                    new JComponent[]{dieta, comportamiento, convivencia, viajes, senales}));

            JScrollPane vista = new JScrollPane(raiz);
            vista.getVerticalScrollBar().setUnitIncrement(18);
            vista.setPreferredSize(new Dimension(760, 510));
            int respuesta = JOptionPane.showConfirmDialog(padre, vista,
                    "Antecedentes clínicos · " + paciente.getNombre(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (respuesta != JOptionPane.OK_OPTION) return false;

            HistoriaAnamnesis nuevo = new HistoriaAnamnesis();
            nuevo.setMascota(paciente);
            nuevo.setRegistradoPor(Application.getNombreApellidoUsuarioLogeado());
            nuevo.setDieta(dieta.getText().trim());
            nuevo.setEnfermedadesPrevias(enfermedades.getText().trim());
            nuevo.setCirugiasPrevias(cirugias.getText().trim());
            nuevo.setEsquemaVacunal(vacunas.getText().trim());
            nuevo.setDesparasitacion(desparasitacion.getText().trim());
            nuevo.setTratamientosRecientes(tratamientos.getText().trim());
            nuevo.setEsterilizacion("Sin registrar".equals(esterilizado.getSelectedItem())
                    ? "" : (String) esterilizado.getSelectedItem());
            nuevo.setNumeroPartos(partos.getText().trim());
            nuevo.setViajesRecientes(viajes.getText().trim());
            nuevo.setConviveConAnimales(convivencia.getText().trim());
            nuevo.setComportamiento(comportamiento.getText().trim());
            nuevo.setSenalesParticulares(senales.getText().trim());

            String antes = firma(anterior);
            if (firma(nuevo).equals(antes)) {
                JOptionPane.showMessageDialog(padre, "No hay cambios en los antecedentes.");
                return false;
            }
            dao.guardarVersion(nuevo);
            JOptionPane.showMessageDialog(padre, "Antecedentes guardados correctamente.");
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(padre,
                    "No se pudieron guardar los antecedentes: " + ex.getMessage(),
                    "Antecedentes clínicos", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static String firma(HistoriaAnamnesis a) {
        if (a == null) return "";
        String[] v = {a.getDieta(), a.getEnfermedadesPrevias(), a.getCirugiasPrevias(),
                a.getEsquemaVacunal(), a.getDesparasitacion(), a.getTratamientosRecientes(),
                a.getEsterilizacion(), a.getNumeroPartos(), a.getViajesRecientes(),
                a.getConviveConAnimales(), a.getComportamiento(), a.getSenalesParticulares()};
        StringBuilder sb = new StringBuilder();
        for (String x : v) sb.append(x == null ? "" : x.trim()).append('\u001f');
        return sb.toString();
    }

    private static JTextArea area(String actual) {
        JTextArea result = new JTextArea(actual == null ? "" : actual, 2, 24);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        return result;
    }

    private static JPanel grupo(String titulo, String[] nombres, JComponent[] campos) {
        JPanel grupo = new JPanel(new BorderLayout(0, 8));
        grupo.setOpaque(false);
        grupo.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(tituloLabel.getFont().deriveFont(java.awt.Font.BOLD));
        grupo.add(tituloLabel, BorderLayout.NORTH);
        JPanel cuadricula = new JPanel(new GridLayout(0, 2, 14, 10));
        cuadricula.setOpaque(false);
        for (int i = 0; i < nombres.length; i++) {
            JPanel campo = new JPanel(new BorderLayout(0, 4));
            campo.setOpaque(false);
            campo.add(new JLabel(nombres[i]), BorderLayout.NORTH);
            campo.add(campos[i] instanceof JTextArea
                    ? new JScrollPane(campos[i]) : campos[i], BorderLayout.CENTER);
            cuadricula.add(campo);
        }
        grupo.add(cuadricula, BorderLayout.CENTER);
        return grupo;
    }

    private static String html(String s) {
        return (s == null ? "" : s).replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
