package veterinaria.entidad;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "producto")
public class Producto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProducto")
    private Integer idProducto;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "rubro", nullable = false)
    private String rubro;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio_costo")
    private Double precioCosto = 0.0;

    @Column(name = "iva")
    private String iva = "21.0";

    @Column(name = "descuento")
    private String descuento = "0";

    @Column(name = "umedida")
    private String umedida;

    @Column(name = "stock")
    private Integer stock = 0;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idProveedor", referencedColumnName = "idProveedor")
    private Proveedor proveedor;

    @Column(name = "estado")
    private String estado = "Activo";

    @Column(name = "precio")
    private Double precio; // Precio manual opcional

    public Producto() {
    }

    public Producto(
            Integer idProducto,
            String codigo,
            String nombre,
            String rubro,
            String descripcion,
            Double precioCosto,
            String iva,
            String descuento,
            String umedida,
            Integer stock,
            Integer stockMinimo,
            Proveedor proveedor,
            String estado
    ) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.rubro = rubro;
        this.descripcion = descripcion;
        this.precioCosto = (precioCosto == null ? 0.0 : precioCosto);
        this.iva = (iva == null ? "21.0" : iva);
        this.descuento = (descuento == null ? "0" : descuento);
        this.umedida = umedida;
        this.stock = (stock == null ? 0 : stock);
        this.stockMinimo = (stockMinimo == null ? 0 : stockMinimo);
        this.proveedor = proveedor;
        this.estado = (estado == null ? "Activo" : estado);
    }

    // Getters y Setters tradicionales
    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getPrecioCosto() {
        return precioCosto;
    }

    public void setPrecioCosto(Double precioCosto) {
        this.precioCosto = precioCosto;
    }

    public String getIva() {
        return iva;
    }

    public void setIva(String iva) {
        this.iva = iva;
    }

    public String getDescuento() {
        return descuento;
    }

    public void setDescuento(String descuento) {
        this.descuento = descuento;
    }

    public String getUmedida() {
        return umedida;
    }

    public void setUmedida(String umedida) {
        this.umedida = umedida;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = (stockMinimo == null ? 0 : stockMinimo);
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
}
