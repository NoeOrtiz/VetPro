
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

    // Porcentajes configurables para operaciones futuras. No se aplican
    // automáticamente hasta integrar el circuito de cobro y comprobantes.
    @Column(name = "recargo_porcentaje", precision = 8, scale = 3)
    private java.math.BigDecimal recargoPorcentaje = java.math.BigDecimal.ZERO;

    @Column(name = "descuento_porcentaje", precision = 8, scale = 3)
    private java.math.BigDecimal descuentoPorcentaje = java.math.BigDecimal.ZERO;

    public java.math.BigDecimal getRecargoPorcentaje() {
        return recargoPorcentaje == null ? java.math.BigDecimal.ZERO : recargoPorcentaje;
    }

    public void setRecargoPorcentaje(java.math.BigDecimal valor) {
        validarPorcentaje(valor);
        this.recargoPorcentaje = valor;
    }

    public java.math.BigDecimal getDescuentoPorcentaje() {
        return descuentoPorcentaje == null ? java.math.BigDecimal.ZERO : descuentoPorcentaje;
    }

    public void setDescuentoPorcentaje(java.math.BigDecimal valor) {
        validarPorcentaje(valor);
        this.descuentoPorcentaje = valor;
    }

    private static void validarPorcentaje(java.math.BigDecimal valor) {
        if (valor == null || valor.signum() < 0
                || valor.compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100.");
        }
    }

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
