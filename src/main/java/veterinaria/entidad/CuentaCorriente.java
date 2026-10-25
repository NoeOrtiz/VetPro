
package veterinaria.entidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    name = "cuentacorriente",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "idCliente")
    }
)
public class CuentaCorriente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCuentaCorriente")
    private Integer idCuentaCorriente;
    
    @OneToOne
    @JoinColumn(
            name = "idCliente",
            nullable = false
    )
    private Cliente cliente;
    
    @Column(name = "saldoInicial", precision = 10, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "saldoActual", precision = 10, scale = 2)
    private BigDecimal saldoActual;
    
    @Column(name = "limiteCredito", precision = 10, scale = 2)
    private BigDecimal limiteCredito;
    
    @Column(name = "estado", length = 20)
    private String estado;
    
    @Column(name = "fechaCreacion")
    private LocalDate fechaCreacion;
    
    @Column(name = "ultimaEdicion")
    private LocalDate ultimaEdicion;    

    // Datos de suspensión y rehabilitación; se conserva el historial en Auditoría.
    @Column(name = "fechaSuspension")
    private LocalDate fechaSuspension;

    @Column(name = "motivoSuspension", length = 255)
    private String motivoSuspension;

    @Column(name = "fechaRehabilitacion")
    private LocalDate fechaRehabilitacion;

    @Column(name = "motivoRehabilitacion", length = 255)
    private String motivoRehabilitacion;

    public LocalDate getFechaSuspension() { return fechaSuspension; }
    public void setFechaSuspension(LocalDate fechaSuspension) { this.fechaSuspension = fechaSuspension; }
    public String getMotivoSuspension() { return motivoSuspension; }
    public void setMotivoSuspension(String motivoSuspension) { this.motivoSuspension = motivoSuspension; }
    public LocalDate getFechaRehabilitacion() { return fechaRehabilitacion; }
    public void setFechaRehabilitacion(LocalDate fechaRehabilitacion) { this.fechaRehabilitacion = fechaRehabilitacion; }
    public String getMotivoRehabilitacion() { return motivoRehabilitacion; }
    public void setMotivoRehabilitacion(String motivoRehabilitacion) { this.motivoRehabilitacion = motivoRehabilitacion; }

    public CuentaCorriente() {
    }

    public CuentaCorriente(Integer idCuentaCorriente, Cliente cliente, BigDecimal saldoInicial, BigDecimal saldoActual, 
                           BigDecimal limiteCredito, String estado, LocalDate fechaCreacion, LocalDate ultimaEdicion) {
        this.idCuentaCorriente = idCuentaCorriente;
        this.cliente = cliente;
        this.saldoInicial = saldoInicial;
        this.saldoActual = saldoActual;
        this.limiteCredito = limiteCredito;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.ultimaEdicion = ultimaEdicion;
    }

    public Integer getIdCuentaCorriente() {
        return idCuentaCorriente;
    }

    public void setIdCuentaCorriente(Integer idCuentaCorriente) {
        this.idCuentaCorriente = idCuentaCorriente;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(BigDecimal saldoActual) {
        this.saldoActual = saldoActual;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDate getUltimaEdicion() {
        return ultimaEdicion;
    }

    public void setUltimaEdicion(LocalDate ultimaEdicion) {
        this.ultimaEdicion = ultimaEdicion;
    }

    @Override
    public String toString() {
        return "CuentaCorriente{" +
                "idCuentaCorriente=" + idCuentaCorriente +
                ", cliente=" + cliente +
                ", saldoInicial=" + saldoInicial +
                ", saldoActual=" + saldoActual +
                ", limiteCredito=" + limiteCredito +
                ", estado='" + estado + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", ultimaEdicion=" + ultimaEdicion +
                '}';
    }
}
