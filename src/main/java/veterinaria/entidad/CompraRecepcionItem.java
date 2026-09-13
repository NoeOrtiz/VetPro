package veterinaria.entidad;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "compra_recepcion_item")
public class CompraRecepcionItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCompraRecepcionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCompraRecepcion", nullable = false)
    private CompraRecepcion recepcion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idProducto", nullable = false)
    private Producto producto;

    @Column(name = "cantidadRecibida", nullable = false)
    private Integer cantidadRecibida;

    @Column(name = "precioCostoUnitario", precision = 12, scale = 2, nullable = true)
    private BigDecimal precioCostoUnitario;

    public Long getIdCompraRecepcionItem() {
        return idCompraRecepcionItem;
    }

    public void setIdCompraRecepcionItem(Long idCompraRecepcionItem) {
        this.idCompraRecepcionItem = idCompraRecepcionItem;
    }

    public CompraRecepcion getRecepcion() {
        return recepcion;
    }

    public void setRecepcion(CompraRecepcion recepcion) {
        this.recepcion = recepcion;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(Integer cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

    public BigDecimal getPrecioCostoUnitario() {
        return precioCostoUnitario;
    }

    public void setPrecioCostoUnitario(BigDecimal precioCostoUnitario) {
        this.precioCostoUnitario = precioCostoUnitario;
    }
}
