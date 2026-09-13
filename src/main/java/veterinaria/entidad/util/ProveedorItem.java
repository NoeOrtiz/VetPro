
package veterinaria.entidad.util;

import java.util.Objects;

public class ProveedorItem {
    private Integer idProveedor;
    private String razonSocial;

    public ProveedorItem(Integer idProveedor, String nombre) {
        this.idProveedor = idProveedor;
        this.razonSocial = nombre;
    }

    @Override
    public String toString() {
        return razonSocial;  
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProveedorItem that = (ProveedorItem) o;
        return Objects.equals(idProveedor, that.idProveedor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProveedor); // Asegurarse de que hashCode también esté basado en idProveedor
    }
}    

