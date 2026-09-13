
package veterinaria.entidad.util;

public class MetodoPagoItem {
    private Integer idMetodoPago;
    private String nombre; 

    public MetodoPagoItem(Integer idMetodoPago, String nombre) {
        this.idMetodoPago = idMetodoPago;
        this.nombre = nombre;
    }
    
    @Override
    public String toString() {
        return nombre;  
    }

    public Integer getIdMetodoPago() {
        return idMetodoPago;
    }

    public String getNombre() {
        return nombre;
    }  
}
