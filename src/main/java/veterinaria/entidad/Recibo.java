
package veterinaria.entidad;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "recibo")
public class Recibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRecibo;

    @Column(nullable = false)
    private String tipo; // Ejemplo: "Venta", "Devolución"

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotalRecibo;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDescuento;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalIva;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalRecibo;

    @Column(name = "claveOperacion", length = 64, unique = true)
    private String claveOperacion;

    @OneToMany(mappedBy = "recibo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReciboProductos> productos;

    @OneToMany(mappedBy = "recibo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReciboMetodoPago> metodosPago;
    
    public Long getIdRecibo() {
        return idRecibo;
    }

    public void setIdRecibo(Long idRecibo) {
        this.idRecibo = idRecibo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public BigDecimal getSubtotalRecibo() {
        return subtotalRecibo;
    }

    public void setSubtotalRecibo(BigDecimal subtotalRecibo) {
        this.subtotalRecibo = subtotalRecibo;
    }

    public BigDecimal getTotalDescuento() {
        return totalDescuento;
    }

    public void setTotalDescuento(BigDecimal totalDescuento) {
        this.totalDescuento = totalDescuento;
    }

    public BigDecimal getTotalIva() {
        return totalIva;
    }

    public void setTotalIva(BigDecimal totalIva) {
        this.totalIva = totalIva;
    }

    public BigDecimal getTotalRecibo() {
        return totalRecibo;
    }

    public void setTotalRecibo(BigDecimal totalRecibo) {
        this.totalRecibo = totalRecibo;
    }

    public String getClaveOperacion() {
        return claveOperacion;
    }

    public void setClaveOperacion(String claveOperacion) {
        this.claveOperacion = claveOperacion;
    }

    public List<ReciboProductos> getProductos() {
        return productos;
    }

    public void setProductos(List<ReciboProductos> productos) {
        this.productos = productos;
    }
    
    public List<ReciboMetodoPago> getMetodosPago() {
        return metodosPago;
    }

    public void setMetodosPago(List<ReciboMetodoPago> metodosPago) {
        this.metodosPago = metodosPago;
    }    
}