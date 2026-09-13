
package veterinaria.util.enums;

public enum EstadoProcedimiento {

    SELECCIONAR_ESTADO("Seleccionar Estado"),
    EN_PREPARACION("En Preparación"),
    EN_PROCEDIMIENTO_QUIRURGICO("En Procedimiento Quirúrgico"),
    EN_RECUPERACION_POSTQUIRURGICA("En Recuperación Postquirúrgica"),
    DADO_DE_ALTA("Dado de Alta"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoProcedimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static EstadoProcedimiento fromEtiqueta(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (EstadoProcedimiento e : values()) {
            if (e.etiqueta.equalsIgnoreCase(v)) {
                return e;
            }
        }
        if ("Procedimiento Cancelado".equalsIgnoreCase(v)) {
            return CANCELADO;
        }
        return null;
    }
}
