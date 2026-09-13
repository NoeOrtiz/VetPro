package veterinaria.entidad;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "compraorden")
public class CompraOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCompraOrden;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroOrden;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date fechaPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CompraOrdenEstado estado = CompraOrdenEstado.BORRADOR;

    @Column(length = 255)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idProveedor", nullable = false)
    private Proveedor proveedor;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraOrdenItem> items = new ArrayList<>();

    public Integer getIdCompraOrden() {
        return idCompraOrden;
    }

    public void setIdCompraOrden(Integer idCompraOrden) {
        this.idCompraOrden = idCompraOrden;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public Date getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(Date fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public CompraOrdenEstado getEstado() {
        return estado;
    }

    public void setEstado(CompraOrdenEstado estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public List<CompraOrdenItem> getItems() {
        return items;
    }

    public void setItems(List<CompraOrdenItem> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

}
