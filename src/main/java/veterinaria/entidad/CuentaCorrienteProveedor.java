package veterinaria.entidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.*;

@Entity
@Table(
        name = "cuentacorriente_proveedor",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "idProveedor")
        }
)
public class CuentaCorrienteProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCuentaCorrienteProveedor")
    private Integer idCuentaCorrienteProveedor;

    @OneToOne
    @JoinColumn(name = "idProveedor", nullable = false)
    private Proveedor proveedor;

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

    public Integer getIdCuentaCorrienteProveedor() {
        return idCuentaCorrienteProveedor;
    }

    public void setIdCuentaCorrienteProveedor(Integer idCuentaCorrienteProveedor) {
        this.idCuentaCorrienteProveedor = idCuentaCorrienteProveedor;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
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
}
