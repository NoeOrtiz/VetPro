
package veterinaria.entidad;

public enum EstadoPeluqueriaEnum {

    PENDIENTE(
            "Pendiente",
            true, // activo
            false // histórico
    ),
    CONFIRMADO(
            "Confirmado",
            true,
            false
    ),
    COMPLETADO(
            "Completado",
            false,
            true
    ),
    CANCELADO(
            "Cancelado",
            false,
            true
    ),
    ELIMINADO(
            "Eliminado",
            false,
            true
    );

    private final String label;
    private final boolean activo;
    private final boolean historico;

    EstadoPeluqueriaEnum(String label, boolean activo, boolean historico) {
        this.label = label;
        this.activo = activo;
        this.historico = historico;
    }

    public String getLabel() {
        return label;
    }

    public boolean esActivo() {
        return activo;
    }

    public boolean esHistorico() {
        return historico;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getDbValue() {
        return label;
    }

    public static EstadoPeluqueriaEnum fromLabel(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        for (EstadoPeluqueriaEnum e : values()) {
            if (e.label.equalsIgnoreCase(v)) {
                return e;
            }
        }
        try {
            return EstadoPeluqueriaEnum.valueOf(v.toUpperCase());
        } catch (Exception ignore) {
            return null;
        }
    }
}
