package veterinaria.servicio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import veterinaria.entidad.HistoriaEvento;

/**
 * Define qué eventos forman parte de la historia clínica.
 * No altera los registros originales ni los otros módulos.
 */
public final class HistoriaClinicaPreferencias {

    public static final String KEY_VISITAS = "HC_MOSTRAR_VISITAS";
    public static final String KEY_LABORATORIO = "HC_MOSTRAR_LABORATORIO";
    public static final String KEY_HOSPITALIZACION = "HC_MOSTRAR_HOSPITALIZACION";
    public static final String KEY_PROCEDIMIENTOS = "HC_MOSTRAR_PROCEDIMIENTOS";
    public static final String KEY_PELUQUERIA = "HC_MOSTRAR_PELUQUERIA";
    public static final String KEY_OTROS = "HC_MOSTRAR_OTROS";

    private HistoriaClinicaPreferencias() {}

    public static boolean incluir(String tipo, ConfiguracionService configuracion) {
        String normalizado = Normalizer.normalize(
                tipo == null ? "" : tipo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        if ("VISITA".equals(normalizado) || "CONSULTA".equals(normalizado)) {
            return configuracion.getBoolean(KEY_VISITAS, true);
        }
        if ("LABORATORIO".equals(normalizado)) {
            return configuracion.getBoolean(KEY_LABORATORIO, true);
        }
        if ("HOSPITALIZACION".equals(normalizado)) {
            return configuracion.getBoolean(KEY_HOSPITALIZACION, true);
        }
        if ("PROCEDIMIENTO".equals(normalizado)) {
            return configuracion.getBoolean(KEY_PROCEDIMIENTOS, true);
        }
        if ("PELUQUERIA".equals(normalizado)) {
            return configuracion.getBoolean(KEY_PELUQUERIA, false);
        }
        return configuracion.getBoolean(KEY_OTROS, true);
    }

    public static List<HistoriaEvento> filtrar(List<HistoriaEvento> eventos) {
        List<HistoriaEvento> resultado = new ArrayList<>();
        if (eventos == null) return resultado;
        ConfiguracionService config = new ConfiguracionService();
        for (HistoriaEvento evento : eventos) {
            if (evento != null && incluir(evento.getTipo(), config)) {
                resultado.add(evento);
            }
        }
        return resultado;
    }
}
