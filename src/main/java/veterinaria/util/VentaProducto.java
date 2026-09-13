
package veterinaria.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VentaProducto {
    private Integer idProducto;
    private Integer cantidadProducto = 0;
    private BigDecimal precioBase = BigDecimal.ZERO;
    private BigDecimal montoDescuentoUnitario = BigDecimal.ZERO;
    private BigDecimal montoIVAUnitario = BigDecimal.ZERO;
    private BigDecimal montoGananciaUnitario = BigDecimal.ZERO;
    private BigDecimal montoTotalUnitario = BigDecimal.ZERO;
    private BigDecimal acumulaSubtotal = BigDecimal.ZERO;
    private BigDecimal acumulaDescuento = BigDecimal.ZERO;
    private BigDecimal acumulaIVA = BigDecimal.ZERO;
    private BigDecimal acumulaTotal = BigDecimal.ZERO;
    
    
    public VentaProducto() {
    }

    public VentaProducto(Integer idProducto, Integer cantidadProducto, BigDecimal precioBase, BigDecimal montoDescuentoUnitario, BigDecimal montoIVAUnitario, BigDecimal montoGananciaUnitario, BigDecimal montoTotalUnitario) {
        this.idProducto = idProducto;
        this.cantidadProducto = cantidadProducto;
        this.precioBase = money(precioBase);
        this.montoDescuentoUnitario = money(montoDescuentoUnitario);
        this.montoIVAUnitario = money(montoIVAUnitario);
        this.montoGananciaUnitario = money(montoGananciaUnitario);
        this.montoTotalUnitario = money(montoTotalUnitario);
    }

    private static BigDecimal money(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getCantidadProducto() {
        return cantidadProducto;
    }

    public void setCantidadProducto(Integer cantidadProducto) {
        this.cantidadProducto = cantidadProducto;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = money(precioBase);
    }

    public BigDecimal getMontoDescuentoUnitario() {
        return montoDescuentoUnitario;
    }

    public void setMontoDescuentoUnitario(BigDecimal montoDescuentoUnitario) {
        this.montoDescuentoUnitario = money(montoDescuentoUnitario);
    }

    public BigDecimal getMontoIVAUnitario() {
        return montoIVAUnitario;
    }

    public void setMontoIVAUnitario(BigDecimal montoIVAUnitario) {
        this.montoIVAUnitario = money(montoIVAUnitario);
    }

    public BigDecimal getMontoGananciaUnitario() {
        return montoGananciaUnitario;
    }

    public void setMontoGananciaUnitario(BigDecimal montoGananciaUnitario) {
        this.montoGananciaUnitario = money(montoGananciaUnitario);
    }

    public BigDecimal getMontoTotalUnitario() {
        return montoTotalUnitario;
    }

    public void setMontoTotalUnitario(BigDecimal montoTotalUnitario) {
        this.montoTotalUnitario = money(montoTotalUnitario);
    }

    public BigDecimal getAcumulaSubtotal() {
        return acumulaSubtotal;
    }

    public void setAcumulaSubtotal(BigDecimal acumulaSubtotal) {
        this.acumulaSubtotal = money(acumulaSubtotal);
    }

    public BigDecimal getAcumulaDescuento() {
        return acumulaDescuento;
    }

    public void setAcumulaDescuento(BigDecimal acumulaDescuento) {
        this.acumulaDescuento = money(acumulaDescuento);
    }

    public BigDecimal getAcumulaIVA() {
        return acumulaIVA;
    }

    public void setAcumulaIVA(BigDecimal acumulaIVA) {
        this.acumulaIVA = money(acumulaIVA);
    }

    public BigDecimal getAcumulaTotal() {
        return acumulaTotal;
    }

    public void setAcumulaTotal(BigDecimal acumulaTotal) {
        this.acumulaTotal = money(acumulaTotal);
    }

}
