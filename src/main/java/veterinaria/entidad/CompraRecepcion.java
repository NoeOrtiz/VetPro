package veterinaria.entidad;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "compra_recepcion")
public class CompraRecepcion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCompraRecepcion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha", nullable = false)
    private Date fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private CompraRecepcionEstado estado = CompraRecepcionEstado.CONFIRMADA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idCompraOrden", nullable = false)
    private CompraOrden orden;

    @Column(name = "numeroRemito", length = 64)
    private String numeroRemito;

    @Column(name = "numeroFactura", length = 64)
    private String numeroFactura;

    @Column(name = "motivoAnulacion", length = 255)
    private String motivoAnulacion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fechaAnulacion")
    private Date fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuarioAnulacion")
    private Usuario usuarioAnulacion;

    @OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraRecepcionItem> items = new ArrayList<>();

    public Long getIdCompraRecepcion() {
        return idCompraRecepcion;
    }

    public void setIdCompraRecepcion(Long idCompraRecepcion) {
        this.idCompraRecepcion = idCompraRecepcion;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public CompraRecepcionEstado getEstado() {
        return estado;
    }

    public void setEstado(CompraRecepcionEstado estado) {
        this.estado = estado;
    }

    public CompraOrden getOrden() {
        return orden;
    }

    public void setOrden(CompraOrden orden) {
        this.orden = orden;
    }

    public String getNumeroRemito() {
        return numeroRemito;
    }

    public void setNumeroRemito(String numeroRemito) {
        this.numeroRemito = numeroRemito;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public Date getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(Date fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public List<CompraRecepcionItem> getItems() {
        return items;
    }

    public void setItems(List<CompraRecepcionItem> items) {
        this.items = items;
    }
}
