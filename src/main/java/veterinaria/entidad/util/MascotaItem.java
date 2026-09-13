package veterinaria.entidad.util;

public class MascotaItem {

    private Integer idMascota;
    private String nombre;

    public MascotaItem(Integer idMascota, String nombre) {
        this.idMascota = idMascota;
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }

    public Integer getIdMascota() {
        return idMascota;
    }

    @Deprecated
    public Integer getIdCliente() {
        return idMascota;
    }
}
