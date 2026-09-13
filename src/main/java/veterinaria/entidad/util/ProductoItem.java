
package veterinaria.entidad.util;

public class ProductoItem {

    private Integer idProducto;
    private String nombre;

    public ProductoItem(Integer idProducto, String nombre) { 
        this.idProducto = idProducto;
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;  
    }

    public Integer getIdProducto() {
        return idProducto;
    }
}