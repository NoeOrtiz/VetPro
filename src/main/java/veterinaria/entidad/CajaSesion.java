package veterinaria.entidad;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "caja_sesion")
public class CajaSesion {

    public enum Estado {
        ABIERTA, CERRADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCajaSesion")
    private Long idCajaSesion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fechaApertura", nullable = false)
    private Date fechaApertura;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fechaCierre")
    private Date fechaCierre;

    @Column(name = "montoInicial", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(name = "efectivoEsperado", precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivoContado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(name = "diferencia", precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "motivoDiferencia", length = 255)
    private String motivoDiferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private Estado estado = Estado.ABIERTA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuarioApertura", nullable = false)
    private Usuario usuarioApertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuarioCierre")
    private Usuario usuarioCierre;

    public Long getIdCajaSesion() { return idCajaSesion; }
    public Date getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(Date fechaApertura) { this.fechaApertura = fechaApertura; }
    public Date getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Date fechaCierre) { this.fechaCierre = fechaCierre; }
    public BigDecimal getMontoInicial() { return montoInicial; }
    public void setMontoInicial(BigDecimal montoInicial) { this.montoInicial = montoInicial; }
    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public String getMotivoDiferencia() { return motivoDiferencia; }
    public void setMotivoDiferencia(String motivoDiferencia) { this.motivoDiferencia = motivoDiferencia; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Usuario getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(Usuario usuarioApertura) { this.usuarioApertura = usuarioApertura; }
    public Usuario getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(Usuario usuarioCierre) { this.usuarioCierre = usuarioCierre; }
}
