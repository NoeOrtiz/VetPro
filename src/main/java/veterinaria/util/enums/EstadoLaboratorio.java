package veterinaria.util.enums;

public enum EstadoLaboratorio {

    PENDIENTE("Pendiente"),
    PROCESADO("Procesado"),
    ENVIADO("Enviado"),
    RECIBIDO("Recibido"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoLaboratorio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static EstadoLaboratorio fromEtiqueta(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (EstadoLaboratorio e : values()) {
            if (e.etiqueta.equalsIgnoreCase(v)) {
                return e;
            }
        }
        return null;
    }
}
