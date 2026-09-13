package veterinaria.util;

import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Rubro;
import veterinaria.entidad.Usuario;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String safeToJson(Object o) {
        try {
            if (o == null) return null;

            if (o instanceof Usuario) {
                Usuario u = (Usuario) o;
                return "{" +
                        kv("idUsuario", u.getIdUsuario()) + "," +
                        kv("nombreUsuario", u.getNombreUsuario()) + "," +
                        kv("email", u.getEmail()) + "," +
                        kv("rol", (u.getRol() != null ? u.getRol().getNombreRol() : null)) + "," +
                        kv("persona", personaMini(u.getPersona())) +
                        "}";
            }

            if (o instanceof Cliente) {
                Cliente c = (Cliente) o;
                return "{" +
                        kv("idCliente", c.getIdCliente()) + "," +
                        kv("razonSocial", c.getRazonSocial()) + "," +
                        kv("email", c.getEmail()) + "," +
                        kv("cuit", c.getCuit()) + "," +
                        kv("persona", personaMini(c.getPersona())) +
                        "}";
            }

            if (o instanceof Persona) {
                Persona p = (Persona) o;
                return personaMini(p);
            }

            if (o instanceof Rol) {
                Rol r = (Rol) o;
                return "{" + kv("idRol", r.getIdRol()) + "," + kv("nombreRol", r.getNombreRol()) + "}";
            }

            if (o instanceof Rubro) {
                Rubro r = (Rubro) o;
                return "{" +
                        kv("idRubro", r.getIdRubro()) + "," +
                        kv("nombre", r.getNombre()) + "," +
                        kv("stockMinimoDefault", r.getStockMinimoDefault()) + "," +
                        kv("activo", r.isActivo()) +
                        "}";
            }

            return String.valueOf(o);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String personaMini(Persona p) {
        if (p == null) return "null";
        return "{" +
                kv("idPersona", p.getIdPersona()) + "," +
                kv("nombre", p.getNombre()) + "," +
                kv("apellido", p.getApellido()) +
                "}";
    }

    private static String kv(String k, Object v) {
        return "\"" + esc(k) + "\":" + val(v);
    }

    private static String val(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        String s = String.valueOf(v);
        if (s.startsWith("{") && s.endsWith("}")) return s;
        return "\"" + esc(s) + "\"";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .trim();
    }
}
