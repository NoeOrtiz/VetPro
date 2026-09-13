package veterinaria.servicio;

import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.StockMovimiento;
import veterinaria.entidad.StockMovimientoTipo;
import veterinaria.entidad.Usuario;

public class StockTxService {

    public void registrarSalidaPorVenta(EntityManager em, Integer idProducto, Integer cantidad, Recibo recibo, Usuario usuario) {
        if (em == null) {
            throw new IllegalArgumentException("EntityManager requerido.");
        }
        if (idProducto == null) {
            throw new IllegalArgumentException("Producto requerido.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }

        Producto p = em.find(Producto.class, idProducto, LockModeType.PESSIMISTIC_WRITE);
        if (p == null) {
            throw new IllegalStateException("Producto no encontrado (id=" + idProducto + ").");
        }

        int antes = (p.getStock() == null) ? 0 : p.getStock();
        int despues = antes - cantidad;

        if (despues < 0) {
            throw new IllegalStateException("Stock insuficiente para '" + p.getNombre()
                    + "' (stock=" + antes + ", requerido=" + cantidad + ").");
        }

        p.setStock(despues);

        StockMovimiento mov = new StockMovimiento();
        mov.setFecha(new Date());
        mov.setTipo(StockMovimientoTipo.SALIDA_VENTA);
        mov.setCantidad(cantidad);
        mov.setStockAntes(antes);
        mov.setStockDespues(despues);
        mov.setProducto(p);
        mov.setRecibo(recibo);
        mov.setUsuario(usuario);
        mov.setObservacion("Salida por venta");
        em.persist(mov);
    }

    public void registrarEntradaPorCompra(EntityManager em, Integer idProducto, Integer cantidad, CompraRecepcion recepcion, Usuario usuario, String obs) {
        if (em == null) {
            throw new IllegalArgumentException("EntityManager requerido.");
        }
        if (idProducto == null) {
            throw new IllegalArgumentException("Producto requerido.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }

        Producto p = em.find(Producto.class, idProducto, LockModeType.PESSIMISTIC_WRITE);
        if (p == null) {
            throw new IllegalStateException("Producto no encontrado (id=" + idProducto + ").");
        }

        int antes = (p.getStock() == null) ? 0 : p.getStock();
        int despues = antes + cantidad;

        p.setStock(despues);

        StockMovimiento mov = new StockMovimiento();
        mov.setFecha(new Date());
        mov.setTipo(StockMovimientoTipo.ENTRADA_COMPRA);
        mov.setCantidad(cantidad);
        mov.setStockAntes(antes);
        mov.setStockDespues(despues);
        mov.setProducto(p);
        mov.setCompraRecepcion(recepcion);
        mov.setUsuario(usuario);
        mov.setObservacion((obs == null || obs.trim().isEmpty()) ? "Entrada por compra" : obs.trim());
        em.persist(mov);
    }

    public void registrarSalidaPorAnulacionCompra(EntityManager em, Integer idProducto, Integer cantidad, CompraRecepcion recepcion, Usuario usuario, String obs) {
        if (em == null) {
            throw new IllegalArgumentException("EntityManager requerido.");
        }
        if (idProducto == null) {
            throw new IllegalArgumentException("Producto requerido.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }

        Producto p = em.find(Producto.class, idProducto, LockModeType.PESSIMISTIC_WRITE);
        if (p == null) {
            throw new IllegalStateException("Producto no encontrado (id=" + idProducto + ").");
        }

        int antes = (p.getStock() == null) ? 0 : p.getStock();
        int despues = antes - cantidad;
        if (despues < 0) {
            throw new IllegalStateException("No se puede anular la recepción: el stock quedaría negativo para '" + p.getNombre()
                    + "' (stock=" + antes + ", a revertir=" + cantidad + ").");
        }
        p.setStock(despues);

        StockMovimiento mov = new StockMovimiento();
        mov.setFecha(new Date());
        mov.setTipo(StockMovimientoTipo.AJUSTE_NEGATIVO);
        mov.setCantidad(cantidad);
        mov.setStockAntes(antes);
        mov.setStockDespues(despues);
        mov.setProducto(p);
        mov.setCompraRecepcion(recepcion);
        mov.setUsuario(usuario);
        mov.setObservacion((obs == null || obs.trim().isEmpty()) ? "Anulación de recepción de compra" : obs.trim());
        em.persist(mov);
    }
}
