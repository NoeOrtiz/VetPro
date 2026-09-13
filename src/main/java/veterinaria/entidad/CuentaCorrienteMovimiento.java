
package veterinaria.entidad;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuentacorrientemovimiento")
public class CuentaCorrienteMovimiento {

    public enum TipoMovimiento {
        DEBITO,
        CREDITO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMovimiento")
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idCuentaCorriente", referencedColumnName = "idCuentaCorriente")
    private CuentaCorriente idCuentaCorriente;

    @Column(name = "fechaMovimiento")
    private LocalDate fechaMovimiento;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "tipoMovimiento", length = 10)
    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipoMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRecibo", referencedColumnName = "idRecibo", nullable = true)
    private Recibo recibo;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldoResultante", precision = 10, scale = 2)
    private BigDecimal saldoResultante;

    public CuentaCorrienteMovimiento() {
    }

    public CuentaCorrienteMovimiento(Integer idMovimiento, CuentaCorriente idCuentaCorriente, LocalDate fechaMovimiento, String descripcion, TipoMovimiento tipoMovimiento, BigDecimal monto, BigDecimal saldoResultante) {
        this.idMovimiento = idMovimiento;
        this.idCuentaCorriente = idCuentaCorriente;
        this.fechaMovimiento = fechaMovimiento;
        this.descripcion = descripcion;
        this.tipoMovimiento = tipoMovimiento;
        this.monto = monto;
        this.saldoResultante = saldoResultante;
    }

    public Integer getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Integer idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public CuentaCorriente getCuentaCorriente() {
        return idCuentaCorriente;
    }

    public void setCuentaCorriente(CuentaCorriente cuentaCorriente) {
        this.idCuentaCorriente = cuentaCorriente;
    }

    public LocalDate getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDate fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public Recibo getRecibo() {
        return recibo;
    }

    public void setRecibo(Recibo recibo) {
        this.recibo = recibo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getSaldoResultante() {
        return saldoResultante;
    }

    public void setSaldoResultante(BigDecimal saldoResultante) {
        this.saldoResultante = saldoResultante;
    }
    
    @Override
    public String toString() {
        return "MovimientoCuentaCorriente{" +
                "idMovimiento=" + idMovimiento +
                ", cuentaCorriente=" + idCuentaCorriente +
                ", fechaMovimiento=" + fechaMovimiento +
                ", descripcion='" + descripcion + '\'' +
                ", tipoMovimiento='" + tipoMovimiento + '\'' +
                ", monto=" + monto +
                ", saldoResultante=" + saldoResultante +
                '}';
    }
}
