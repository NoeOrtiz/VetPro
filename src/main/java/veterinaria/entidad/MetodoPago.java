
package veterinaria.entidad;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "metodopago")
public class MetodoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMetodoPago")
    private Integer idMetodoPago;
    
    @Column(name = "nombre")
    private String nombre;
        
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "afectaEfectivo", nullable = false)
    private boolean afectaEfectivo;

    public MetodoPago() {
    }

    public MetodoPago(Integer idMetodoPago, String nombre, String descripcion) {
        this.idMetodoPago = idMetodoPago;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Integer getIdMetodoPago() {
        return idMetodoPago;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isAfectaEfectivo() {
        return afectaEfectivo;
    }

    public void setAfectaEfectivo(boolean afectaEfectivo) {
        this.afectaEfectivo = afectaEfectivo;
    }

    @Override
    public String toString() {
        return nombre == null ? "" : nombre;
    }
    
    
}
