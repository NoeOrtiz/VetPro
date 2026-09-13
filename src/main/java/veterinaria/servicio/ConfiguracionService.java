
package veterinaria.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import veterinaria.entidad.Configuracion;
import veterinaria.persistencia.ConfiguracionDAO;

public class ConfiguracionService {

    public static final String KEY_TURNOS_PELUQUERIA_MANANA = "PELUQUERIA_TURNOS_MANANA";
    public static final String KEY_TURNOS_PELUQUERIA_TARDE = "PELUQUERIA_TURNOS_TARDE";
    public static final int DEFAULT_TURNOS_PELUQUERIA_MANANA = 5;
    public static final int DEFAULT_TURNOS_PELUQUERIA_TARDE = 5;

    public static final String KEY_DURACION_TURNO_PELUQUERIA_MIN = "PELUQUERIA_DURACION_TURNO_MIN";
    public static final int DEFAULT_DURACION_TURNO_PELUQUERIA_MIN = 60;

    public static final String KEY_HORARIO_PELUQUERIA_MANANA_DESDE = "PELUQUERIA_HORARIO_MANANA_DESDE"; // HH:mm
    public static final String KEY_HORARIO_PELUQUERIA_MANANA_HASTA = "PELUQUERIA_HORARIO_MANANA_HASTA"; // HH:mm
    public static final String KEY_HORARIO_PELUQUERIA_TARDE_DESDE = "PELUQUERIA_HORARIO_TARDE_DESDE";   // HH:mm
    public static final String KEY_HORARIO_PELUQUERIA_TARDE_HASTA = "PELUQUERIA_HORARIO_TARDE_HASTA";   // HH:mm

    public static final String DEFAULT_HORARIO_PELUQUERIA_MANANA_DESDE = "09:00";
    public static final String DEFAULT_HORARIO_PELUQUERIA_MANANA_HASTA = "11:00";
    public static final String DEFAULT_HORARIO_PELUQUERIA_TARDE_DESDE = "17:00";
    public static final String DEFAULT_HORARIO_PELUQUERIA_TARDE_HASTA = "20:00";

    public static final String KEY_DIAS_HABILITADOS_PELUQUERIA = "PELUQUERIA_DIAS_HABILITADOS";
    public static final String DEFAULT_DIAS_HABILITADOS_PELUQUERIA = "1,2,3,4,5";

    public static final String KEY_DURACION_TURNO_LABORATORIO_MIN = "LABORATORIO_DURACION_TURNO_MIN";
    public static final int DEFAULT_DURACION_TURNO_LABORATORIO_MIN = 30;

    public static final String KEY_DIAS_HABILITADOS_LABORATORIO = "LABORATORIO_DIAS_HABILITADOS";
    public static final String DEFAULT_DIAS_HABILITADOS_LABORATORIO = "1,3,5";

    public static final String KEY_DIAS_HABILITADOS_HOSPITALIZACION = "HOSPITALIZACION_DIAS_HABILITADOS";
    public static final String DEFAULT_DIAS_HABILITADOS_HOSPITALIZACION = "2,4,6";

    public static final String KEY_DURACION_TURNO_HOSPITALIZACION_MIN = "HOSPITALIZACION_DURACION_TURNO_MIN";
    public static final int DEFAULT_DURACION_TURNO_HOSPITALIZACION_MIN = 60;

    public static final String KEY_VENTAS_PORC_GANANCIA_GLOBAL = "VENTAS_PORC_GANANCIA_GLOBAL";
    public static final int DEFAULT_VENTAS_PORC_GANANCIA_GLOBAL = 40;

    public static final String KEY_VENTAS_DECIMALES = "VENTAS_DECIMALES";
    public static final int DEFAULT_VENTAS_DECIMALES = 2;

    public static final String KEY_VENTAS_REDONDEO = "VENTAS_REDONDEO";
    public static final String DEFAULT_VENTAS_REDONDEO = "HALF_UP";

    public static final String KEY_VENTAS_PERMITIR_STOCK_NEGATIVO = "VENTAS_PERMITIR_STOCK_NEGATIVO";
    public static final int DEFAULT_VENTAS_PERMITIR_STOCK_NEGATIVO = 0;

    public static final String KEY_VENTAS_ABRIR_COMPROBANTE_PDF = "VENTAS_ABRIR_COMPROBANTE_PDF";
    public static final int DEFAULT_VENTAS_ABRIR_COMPROBANTE_PDF = 1;

    public static final String KEY_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO = "VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO";
    public static final int DEFAULT_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO = 0;

    public static final String KEY_VENTAS_CLIENTE_DEFAULT_ID = "VENTAS_CLIENTE_DEFAULT_ID";
    public static final int DEFAULT_VENTAS_CLIENTE_DEFAULT_ID = 0;

    public static final String KEY_VENTAS_METODO_PAGO_DEFAULT_ID = "VENTAS_METODO_PAGO_DEFAULT_ID";
    public static final int DEFAULT_VENTAS_METODO_PAGO_DEFAULT_ID = 0;

    private static final ConfiguracionDAO DAO = new ConfiguracionDAO();

    private static volatile Map<String, String> CACHE = null;

    public static void invalidateCache() {
        CACHE = null;
    }

    private static Map<String, String> getCache() {
        Map<String, String> c = CACHE;
        if (c != null) return c;

        synchronized (ConfiguracionService.class) {
            if (CACHE == null) {
                Map<String, String> tmp = new HashMap<>();
                try {
                    List<Configuracion> all = DAO.listarTodos();
                    if (all != null) {
                        for (Configuracion cfg : all) {
                            if (cfg != null && cfg.getClave() != null) {
                                tmp.put(cfg.getClave(), cfg.getValor());
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // si falla, dejamos cache vacío (no rompemos la app)
                }
                CACHE = tmp;
            }
            return CACHE;
        }
    }

    public int getInt(String clave, int defaultValue) {
        try {
            String v = getCache().get(clave);
            if (v == null) {
                Configuracion c = DAO.buscarPorClave(clave);
                v = (c == null ? null : c.getValor());
            }
            if (v == null) return defaultValue;
            v = v.trim();
            if (v.isEmpty()) return defaultValue;
            return Integer.parseInt(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean setInt(String clave, int valor, String descripcion) {
        boolean ok = DAO.upsert(clave, String.valueOf(valor), descripcion);
        if (ok) invalidateCache();
        return ok;
    }

    public String getString(String clave, String defaultValue) {
        try {
            String v = getCache().get(clave);
            if (v == null) {
                Configuracion c = DAO.buscarPorClave(clave);
                v = (c == null ? null : c.getValor());
            }
            if (v == null) return defaultValue;
            v = v.trim();
            return v.isEmpty() ? defaultValue : v;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean setString(String clave, String valor, String descripcion) {
        if (valor == null) valor = "";
        boolean ok = DAO.upsert(clave, valor, descripcion);
        if (ok) invalidateCache();
        return ok;
    }

    public boolean getBoolean(String clave, boolean defaultValue) {
        int def = defaultValue ? 1 : 0;
        int v = getInt(clave, def);
        return v != 0;
    }

    public boolean setBoolean(String clave, boolean valor, String descripcion) {
        return setInt(clave, valor ? 1 : 0, descripcion);
    }

    public BigDecimal getBigDecimal(String clave, BigDecimal defaultValue) {
        try {
            String v = getString(clave, null);
            if (v == null) return defaultValue;
            v = v.trim();
            if (v.isEmpty()) return defaultValue;
            return new BigDecimal(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public RoundingMode getRoundingModeVentas() {
        String mode = getString(KEY_VENTAS_REDONDEO, DEFAULT_VENTAS_REDONDEO);
        try {
            return RoundingMode.valueOf(mode);
        } catch (Exception e) {
            return RoundingMode.HALF_UP;
        }
    }
}
