package veterinaria.entidad;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "stock_movimiento")
public class StockMovimiento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idStockMovimiento;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StockMovimientoTipo tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockAntes;

    @Column(nullable = false)
    private Integer stockDespues;

    @Column(length = 255)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idProducto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuario", nullable = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRecibo", nullable = true)
    private Recibo recibo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCompraRecepcion", nullable = true)
    private CompraRecepcion compraRecepcion;

    public Long getIdStockMovimiento() {
        return idStockMovimiento;
    }

    public void setIdStockMovimiento(Long idStockMovimiento) {
        this.idStockMovimiento = idStockMovimiento;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public StockMovimientoTipo getTipo() {
        return tipo;
    }

    public void setTipo(StockMovimientoTipo tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getStockAntes() {
        return stockAntes;
    }

    public void setStockAntes(Integer stockAntes) {
        this.stockAntes = stockAntes;
    }

    public Integer getStockDespues() {
        return stockDespues;
    }

    public void setStockDespues(Integer stockDespues) {
        this.stockDespues = stockDespues;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Recibo getRecibo() {
        return recibo;
    }

    public void setRecibo(Recibo recibo) {
        this.recibo = recibo;
    }

    public CompraRecepcion getCompraRecepcion() {
        return compraRecepcion;
    }

    public void setCompraRecepcion(CompraRecepcion compraRecepcion) {
        this.compraRecepcion = compraRecepcion;
    }
}
