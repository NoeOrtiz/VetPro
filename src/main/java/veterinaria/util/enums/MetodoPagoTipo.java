package veterinaria.util.enums;

public enum MetodoPagoTipo {

    EFECTIVO("Pago Efectivo"),
    CUENTA_CORRIENTE("Cuenta Corriente"),
    TARJETA_CREDITO("Tarjeta de Crédito"),
    TARJETA_DEBITO("Tarjeta de Dédito");

    private final String etiqueta;

    MetodoPagoTipo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static MetodoPagoTipo fromEtiqueta(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (MetodoPagoTipo t : values()) {
            if (t.etiqueta.equalsIgnoreCase(v)) {
                return t;
            }
        }
        return null;
    }
}
