package veterinaria.entidad;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(name = "compra_proveedor_pago")
public class CompraProveedorPago implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCompraProveedorPago;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCompraProveedor", nullable = false)
    private CompraProveedor compra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idMetodoPago", nullable = false)
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCajaMovimiento")
    private CajaMovimiento cajaMovimiento;

    public Long getIdCompraProveedorPago() {
        return idCompraProveedorPago;
    }

    public void setIdCompraProveedorPago(Long idCompraProveedorPago) {
        this.idCompraProveedorPago = idCompraProveedorPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public CompraProveedor getCompra() {
        return compra;
    }

    public void setCompra(CompraProveedor compra) {
        this.compra = compra;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public CajaMovimiento getCajaMovimiento() {
        return cajaMovimiento;
    }

    public void setCajaMovimiento(CajaMovimiento cajaMovimiento) {
        this.cajaMovimiento = cajaMovimiento;
    }
}
