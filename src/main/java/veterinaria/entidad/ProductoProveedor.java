package veterinaria.entidad;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(
    name = "producto_proveedor",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_producto_proveedor", columnNames = {"idProducto", "idProveedor"})
    }
)
public class ProductoProveedor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProductoProveedor")
    private Integer idProductoProveedor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idProducto", referencedColumnName = "idProducto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idProveedor", referencedColumnName = "idProveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "codigoProveedor")
    private String codigoProveedor;

    @Column(name = "costoUltimo", precision = 12, scale = 2)
    private BigDecimal costoUltimo;

    @Column(name = "esDefault")
    private Boolean esDefault = false;

    @Column(name = "activo")
    private Boolean activo = true;

    public ProductoProveedor() {
    }

    public Integer getIdProductoProveedor() {
        return idProductoProveedor;
    }

    public void setIdProductoProveedor(Integer idProductoProveedor) {
        this.idProductoProveedor = idProductoProveedor;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }

    public void setCodigoProveedor(String codigoProveedor) {
        this.codigoProveedor = codigoProveedor;
    }

    public BigDecimal getCostoUltimo() {
        return costoUltimo;
    }

    public void setCostoUltimo(BigDecimal costoUltimo) {
        this.costoUltimo = costoUltimo;
    }

    public Boolean getEsDefault() {
        return esDefault;
    }

    public void setEsDefault(Boolean esDefault) {
        this.esDefault = esDefault;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
