
package veterinaria.util.enums;

public enum EstadoPeluqueria {

    PENDIENTE("Pendiente"),
    CONFIRMADO("Confirmado"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPeluqueria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static EstadoPeluqueria fromEtiqueta(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (EstadoPeluqueria e : values()) {
            if (e.etiqueta.equalsIgnoreCase(v)) {
                return e;
            }
        }
        return null;
    }
}
