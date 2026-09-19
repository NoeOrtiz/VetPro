
package veterinaria.entidad;

import java.math.BigDecimal;
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
@Table(name = "cajamovimiento")
public class CajaMovimiento {
    
    public enum TipoMovimiento {
        APERTURA, CIERRE, DEBITO, CREDITO;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMovimiento")
    private Long idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCajaSesion")
    private CajaSesion cajaSesion;
    
    @Column(name = "monto")
    private BigDecimal monto;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoMovimiento")
    private TipoMovimiento tipoMovimiento; 
    
    @Column(name = "fecha")
    @Temporal(TemporalType.DATE)
    private Date fecha;

    @Column(name = "fechaHora")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaHora;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idUsuario", referencedColumnName = "idUsuario")       
    private Usuario usuario; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRecibo", referencedColumnName = "idRecibo", nullable = true)
    private Recibo recibo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idMetodoPago", referencedColumnName = "idMetodoPago", nullable = true)
    private MetodoPago metodoPago;
    
    @Column(name = "descripcion")
    private String descripcion; 

    @Column(name = "eliminado")
    private boolean eliminado;

    @Column(name = "anulado", nullable = false)
    private boolean anulado;

    @Column(name = "fechaAnulacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAnulacion;

    @Column(name = "motivoAnulacion", length = 255)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuarioAnulacion")
    private Usuario usuarioAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idMovimientoReversion")
    private CajaMovimiento movimientoReversion;
    
    public CajaMovimiento() {
    }

    public CajaMovimiento(Long idMovimiento, BigDecimal monto, TipoMovimiento tipoMovimiento, Date fecha, Usuario usuario, String descripcion, boolean eliminado) {
        this.idMovimiento = idMovimiento;
        this.monto = monto;
        this.tipoMovimiento = tipoMovimiento;
        this.fecha = fecha;
        this.usuario = usuario;
        this.descripcion = descripcion;
        this.eliminado = false;
    }

    public Long getIdMovimiento() {
        return idMovimiento;
    }

    public CajaSesion getCajaSesion() {
        return cajaSesion;
    }

    public void setCajaSesion(CajaSesion cajaSesion) {
        this.cajaSesion = cajaSesion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Date getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Date fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Recibo getRecibo() {
        return recibo;
    }

    public void setRecibo(Recibo recibo) {
        this.recibo = recibo;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public boolean isAnulado() {
        return anulado;
    }

    public void setAnulado(boolean anulado) {
        this.anulado = anulado;
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

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public CajaMovimiento getMovimientoReversion() {
        return movimientoReversion;
    }

    public void setMovimientoReversion(CajaMovimiento movimientoReversion) {
        this.movimientoReversion = movimientoReversion;
    }
}
