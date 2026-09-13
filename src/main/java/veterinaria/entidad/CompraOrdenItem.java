package veterinaria.entidad;

import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(name = "compraordenitem")
public class CompraOrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCompraOrdenItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCompraOrden", nullable = false)
    private CompraOrden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idProducto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal costoUnitario;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    public CompraOrdenItem() {
    }

    public Long getIdCompraOrdenItem() {
        return idCompraOrdenItem;
    }

    public void setIdCompraOrdenItem(Long idCompraOrdenItem) {
        this.idCompraOrdenItem = idCompraOrdenItem;
    }

    public CompraOrden getOrden() {
        return orden;
    }

    public void setOrden(CompraOrden orden) {
        this.orden = orden;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
