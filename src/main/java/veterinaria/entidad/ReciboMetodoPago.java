
package veterinaria.entidad;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recibometodopago")
public class ReciboMetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReciboMetodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRecibo", nullable = false)
    private Recibo recibo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idMetodoPago", nullable = false)
    private MetodoPago metodoPago;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @Lob
    @Column(name = "detalleJson")
    private String detalleJson;

    public Long getIdReciboMetodoPago() {
        return idReciboMetodoPago;
    }

    public void setIdReciboMetodoPago(Long idReciboMetodoPago) {
        this.idReciboMetodoPago = idReciboMetodoPago;
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

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getDetalleJson() {
        return detalleJson;
    }

    public void setDetalleJson(String detalleJson) {
        this.detalleJson = detalleJson;
    }
}