package veterinaria.entidad;

import java.math.BigDecimal;

public class StockProductoInfo {

    public enum EstadoStock {
        NORMAL,
        BAJO,
        SIN_STOCK
    }

    private Integer idProducto;
    private String codigo;
    private String producto;
    private String rubro;
    private Integer idProveedor;
    private String proveedor;
    private BigDecimal precioCompra;   // usa costoUltimo del proveedor default; fallback a producto.precio_costo
    private BigDecimal precioVenta;    // calculado desde el costo efectivo + IVA + ganancia
    private Integer stockActual;
    private Integer stockMinimo;       // opcional (si no existe en BD, puede ser null)

    public StockProductoInfo() {
    }

    public StockProductoInfo(Integer idProducto, String codigo, String producto, String rubro,
                             Integer idProveedor, String proveedor,
                             BigDecimal precioCompra, BigDecimal precioVenta,
                             Integer stockActual, Integer stockMinimo) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.producto = producto;
        this.rubro = rubro;
        this.idProveedor = idProveedor;
        this.proveedor = proveedor;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

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

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public int getDiferencia() {
        int actual = (stockActual == null) ? 0 : stockActual;
        int minimo = (stockMinimo == null) ? 0 : stockMinimo;
        return actual - minimo;
    }

    public EstadoStock getEstado() {
        int actual = (stockActual == null) ? 0 : stockActual;
        if (actual <= 0) {
            return EstadoStock.SIN_STOCK;
        }
        if (stockMinimo != null) {
            int minimo = stockMinimo;
            if (actual <= minimo) {
                return EstadoStock.BAJO;
            }
        }
        return EstadoStock.NORMAL;
    }

    public BigDecimal getValorEnStock() {
        int actual = (stockActual == null) ? 0 : stockActual;
        if (actual <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = precioCompra != null ? precioCompra : BigDecimal.ZERO;
        return base.multiply(BigDecimal.valueOf(actual));
    }
}
