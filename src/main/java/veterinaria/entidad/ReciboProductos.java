
package veterinaria.entidad;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reciboproductos")
public class ReciboProductos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReciboProductos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRecibo", nullable = false)
    private Recibo recibo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idProducto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal ivaUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuentoUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal gananciaUnitario;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalUnitario;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCantidad;
    
    public Long getIdReciboProductos() {
        return idReciboProductos;
    }

    public void setIdReciboProductos(Long idReciboProductos) {
        this.idReciboProductos = idReciboProductos;
    }

    public Recibo getRecibo() {
        return recibo;
    }

    public void setRecibo(Recibo recibo) {
        this.recibo = recibo;
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

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getIvaUnitario() {
        return ivaUnitario;
    }

    public void setIvaUnitario(BigDecimal ivaUnitario) {
        this.ivaUnitario = ivaUnitario;
    }

    public BigDecimal getDescuentoUnitario() {
        return descuentoUnitario;
    }

    public void setDescuentoUnitario(BigDecimal descuentoUnitario) {
        this.descuentoUnitario = descuentoUnitario;
    }

    public BigDecimal getGananciaUnitario() {
        return gananciaUnitario;
    }

    public void setGananciaUnitario(BigDecimal gananciaUnitario) {
        this.gananciaUnitario = gananciaUnitario;
    }

    public BigDecimal getTotalUnitario() {
        return totalUnitario;
    }

    public void setTotalUnitario(BigDecimal totalUnitario) {
        this.totalUnitario = totalUnitario;
    }

    public BigDecimal getTotalCantidad() {
        return totalCantidad;
    }

    public void setTotalCantidad(BigDecimal totalCantidad) {
        this.totalCantidad = totalCantidad;
    }
}