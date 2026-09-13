package veterinaria.entidad;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "compra_proveedor")
public class CompraProveedor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCompraProveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private CompraProveedorEstado estado = CompraProveedorEstado.REGISTRADA;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha", nullable = false)
    private Date fecha;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fechaAnulacion")
    private Date fechaAnulacion;

    @Column(name = "motivoAnulacion", length = 255)
    private String motivoAnulacion;

    @Column(name = "numeroFactura", length = 64)
    private String numeroFactura;

    @Temporal(TemporalType.DATE)
    @Column(name = "fechaVencimiento")
    private Date fechaVencimiento;

    @Column(name = "totalFactura", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFactura = BigDecimal.ZERO;

    @Column(name = "saldoPendiente", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idProveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuarioAnulacion")
    private Usuario usuarioAnulacion;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraProveedorPago> pagos = new ArrayList<>();

    public Long getIdCompraProveedor() {
        return idCompraProveedor;
    }

    public void setIdCompraProveedor(Long idCompraProveedor) {
        this.idCompraProveedor = idCompraProveedor;
    }

    public CompraProveedorEstado getEstado() {
        return estado;
    }

    public void setEstado(CompraProveedorEstado estado) {
        this.estado = estado;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Date getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(Date fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public Date getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(Date fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getTotalFactura() {
        return totalFactura;
    }

    public void setTotalFactura(BigDecimal totalFactura) {
        this.totalFactura = totalFactura;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public List<CompraProveedorPago> getPagos() {
        return pagos;
    }

    public void setPagos(List<CompraProveedorPago> pagos) {
        this.pagos = pagos;
        if (this.pagos != null) {
            for (CompraProveedorPago p : this.pagos) {
                if (p != null) p.setCompra(this);
            }
        }
    }
}
