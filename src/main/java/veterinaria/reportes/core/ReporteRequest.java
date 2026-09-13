package veterinaria.reportes.core;

import java.util.HashMap;
import java.util.Map;

public class ReporteRequest {

    private final Map<String, Object> data = new HashMap<>();

    public ReporteRequest put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object v = data.get(key);
        if (v == null) {
            return null;
        }
        return (T) v;
    }

    public Map<String, Object> raw() {
        return data;
    }
}
