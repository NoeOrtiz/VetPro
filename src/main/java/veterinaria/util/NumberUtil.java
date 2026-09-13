package veterinaria.util;

public final class NumberUtil {

    private NumberUtil() {
    }

    public static int parseInt(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Número vacío");
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Número vacío");
        }
        s = s.replace(" ", "").replace(".", "");
        int comma = s.indexOf(',');
        if (comma >= 0) {
            s = s.substring(0, comma);
        }
        int dot = s.indexOf('.');
        if (dot >= 0) {
            s = s.substring(0, dot);
        }
        return Integer.parseInt(s);
    }

    public static int parsePositiveInt(String raw) {
        int v = parseInt(raw);
        if (v <= 0) {
            throw new IllegalArgumentException("El número debe ser mayor a 0");
        }
        return v;
    }
}
