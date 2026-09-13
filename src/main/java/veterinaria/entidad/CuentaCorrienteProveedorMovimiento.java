package veterinaria.entidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.*;

@Entity
@Table(name = "cuentacorriente_proveedor_movimiento")
public class CuentaCorrienteProveedorMovimiento {

    public enum TipoMovimiento {
        DEBITO,
        CREDITO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMovimiento")
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idCuentaCorrienteProveedor", referencedColumnName = "idCuentaCorrienteProveedor")
    private CuentaCorrienteProveedor cuentaCorrienteProveedor;

    @Column(name = "fechaMovimiento")
    private LocalDate fechaMovimiento;

    @Column(name = "fechaVencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "tipoMovimiento", length = 10)
    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldoResultante", precision = 10, scale = 2)
    private BigDecimal saldoResultante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCompraProveedor", referencedColumnName = "idCompraProveedor", nullable = true)
    private CompraProveedor compraProveedor;

    public Integer getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Integer idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public CuentaCorrienteProveedor getCuentaCorrienteProveedor() {
        return cuentaCorrienteProveedor;
    }

    public void setCuentaCorrienteProveedor(CuentaCorrienteProveedor cuentaCorrienteProveedor) {
        this.cuentaCorrienteProveedor = cuentaCorrienteProveedor;
    }

    public LocalDate getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDate fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
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

    public CompraProveedor getCompraProveedor() {
        return compraProveedor;
    }

    public void setCompraProveedor(CompraProveedor compraProveedor) {
        this.compraProveedor = compraProveedor;
    }
}
