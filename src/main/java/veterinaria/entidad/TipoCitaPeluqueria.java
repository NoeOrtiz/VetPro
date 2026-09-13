package veterinaria.entidad;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(
        name = "tipo_cita_peluqueria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tipo_cita_peluqueria_nombre",
                columnNames = {"nombre"}
        )
)
public class TipoCitaPeluqueria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_cita_peluqueria")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public TipoCitaPeluqueria() {
    }

    public TipoCitaPeluqueria(String descripcion, BigDecimal precio, boolean activo) {
        this.descripcion = descripcion;
        this.precio = (precio == null) ? BigDecimal.ZERO : precio;
        this.activo = activo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio == null ? BigDecimal.ZERO : precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = (precio == null) ? BigDecimal.ZERO : precio;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = (orden == null) ? 0 : orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
