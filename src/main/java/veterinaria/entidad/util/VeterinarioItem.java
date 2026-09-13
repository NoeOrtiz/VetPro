package veterinaria.entidad.util;

public class VeterinarioItem {

    private Integer idUsuario;
    private String nombreCompleto;

    public VeterinarioItem(Integer idUsuario, String nombreCompleto) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
    }

    @Override
    public String toString() {
        return nombreCompleto;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }
}
