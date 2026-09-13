package veterinaria.entidad;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "rubro")
public class Rubro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRubro")
    private Integer idRubro;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "stock_minimo_default", nullable = false)
    private int stockMinimoDefault;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "porcentaje_ganancia")
    private BigDecimal porcentajeGanancia;

    public Rubro() {
    }

    public Integer getIdRubro() {
        return idRubro;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getStockMinimoDefault() {
        return stockMinimoDefault;
    }

    public void setStockMinimoDefault(int stockMinimoDefault) {
        this.stockMinimoDefault = stockMinimoDefault;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public BigDecimal getPorcentajeGanancia() {
        return porcentajeGanancia;
    }

    public void setPorcentajeGanancia(BigDecimal porcentajeGanancia) {
        this.porcentajeGanancia = porcentajeGanancia;
    }
}

