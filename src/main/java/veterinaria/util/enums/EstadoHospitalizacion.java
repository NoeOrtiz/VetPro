
package veterinaria.util.enums;

public enum EstadoHospitalizacion {

    PENDIENTE("Pendiente"),
    INTERNADO("Internado"),
    ALTA("Alta medica"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoHospitalizacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static EstadoHospitalizacion fromEtiqueta(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (EstadoHospitalizacion e : values()) {
            if (e.etiqueta.equalsIgnoreCase(v)) {
                return e;
            }
        }
        return null;
    }
}
