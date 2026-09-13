
package veterinaria.entidad.util;

public class ClienteItem {
    private Integer idCliente;
    private String nombre;

    public ClienteItem(Integer idCliente, String nombre) {
        this.idCliente = idCliente;
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;  
    }

    public Integer getIdCliente() {
        return idCliente;
    }
}