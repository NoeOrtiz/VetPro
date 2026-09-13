package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.CompraOrden;
import veterinaria.entidad.CompraOrdenEstado;
import veterinaria.entidad.CompraOrdenItem;
import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.CompraRecepcionEstado;
import veterinaria.entidad.CompraRecepcionItem;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.Usuario;

public class CompraTxService {

    private final TxRunner txRunner = new TxRunner();
    private final StockTxService stockTxService = new StockTxService();

    public CompraOrden crearOrden(Proveedor proveedor, List<CompraOrdenItem> items, String observacion) throws Exception {
        if (proveedor == null || proveedor.getIdProveedor() == null) {
            throw new IllegalArgumentException("Proveedor requerido.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La orden debe tener al menos 1 ítem.");
        }

        return txRunner.runInTx((EntityManager em) -> {
            CompraOrden orden = new CompraOrden();
            orden.setFechaPedido(new Date());
            orden.setEstado(CompraOrdenEstado.BORRADOR);
            orden.setProveedor(em.getReference(Proveedor.class, proveedor.getIdProveedor()));
            orden.setObservacion(sanitizeObs(observacion));

            
            if (orden.getNumeroOrden() == null || orden.getNumeroOrden().trim().isEmpty()) {
                orden.setNumeroOrden(generarNumeroOrden(em));
            }

            em.persist(orden);

            for (CompraOrdenItem i : items) {
                validarItemOrden(i);

                CompraOrdenItem item = new CompraOrdenItem();
                item.setOrden(orden);
                item.setProducto(em.getReference(Producto.class, i.getProducto().getIdProducto()));
                item.setCantidad(i.getCantidad());
                item.setCostoUnitario(i.getCostoUnitario());
                item.setSubtotal(calcSubtotal(i.getCostoUnitario(), i.getCantidad()));
                em.persist(item);

                orden.getItems().add(item);
            }

            return orden;
        });
    }

    public CompraRecepcion confirmarRecepcion(
            CompraOrden orden,
            List<CompraRecepcionItem> itemsRecibidos,
            String numeroRemito,
            String numeroFactura,
            Usuario usuario) throws Exception {

        if (orden == null || orden.getIdCompraOrden() == null) {
            throw new IllegalArgumentException("Orden requerida.");
        }
        if (itemsRecibidos == null || itemsRecibidos.isEmpty()) {
            throw new IllegalArgumentException("La recepción debe tener al menos 1 ítem.");
        }

        return txRunner.runInTx((EntityManager em) -> {
            CompraOrden ordenDb = em.find(CompraOrden.class, orden.getIdCompraOrden());
            if (ordenDb == null) {
                throw new IllegalStateException("Orden no encontrada (id=" + orden.getIdCompraOrden() + ").");
            }

            if (ordenDb.getEstado() == CompraOrdenEstado.ANULADA) {
                throw new IllegalStateException("No se puede recepcionar una orden ANULADA.");
            }
            if (ordenDb.getEstado() == CompraOrdenEstado.CERRADA) {
                throw new IllegalStateException("La orden ya está CERRADA.");
            }

            CompraRecepcion recep = new CompraRecepcion();
            recep.setFecha(new Date());
            recep.setEstado(CompraRecepcionEstado.CONFIRMADA);
            recep.setOrden(ordenDb);
            recep.setNumeroRemito(sanitizeDoc(numeroRemito));
            recep.setNumeroFactura(sanitizeDoc(numeroFactura));
            em.persist(recep);

            int itemsOk = 0;

            for (CompraRecepcionItem i : itemsRecibidos) {
                if (i == null || i.getProducto() == null || i.getProducto().getIdProducto() == null) {
                    continue;
                }
                if (i.getCantidadRecibida() == null || i.getCantidadRecibida() <= 0) {
                    continue;
                }

                CompraRecepcionItem item = new CompraRecepcionItem();
                item.setRecepcion(recep);
                item.setProducto(em.getReference(Producto.class, i.getProducto().getIdProducto()));
                item.setCantidadRecibida(i.getCantidadRecibida());
                item.setPrecioCostoUnitario(i.getPrecioCostoUnitario()); // opcional
                em.persist(item);

                stockTxService.registrarEntradaPorCompra(
                        em,
                        i.getProducto().getIdProducto(),
                        i.getCantidadRecibida(),
                        recep,
                        usuario,
                        "Entrada por compra (recepción)"
                );

                itemsOk++;
            }

            if (itemsOk <= 0) {
                throw new IllegalArgumentException("La recepción no tiene ítems válidos (cantidad > 0).");
            }

            ordenDb.setEstado(CompraOrdenEstado.CERRADA);
            em.merge(ordenDb);

            return recep;
        });
    }

    private void validarItemOrden(CompraOrdenItem i) {
        if (i == null) {
            throw new IllegalArgumentException("Ítem inválido.");
        }
        if (i.getProducto() == null || i.getProducto().getIdProducto() == null) {
            throw new IllegalArgumentException("Ítem inválido: producto requerido.");
        }
        if (i.getCantidad() == null || i.getCantidad() <= 0) {
            throw new IllegalArgumentException("Ítem inválido: cantidad.");
        }
        if (i.getCostoUnitario() == null || i.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ítem inválido: costo unitario.");
        }
    }

    private BigDecimal calcSubtotal(BigDecimal costo, Integer cantidad) {
        return costo.multiply(new BigDecimal(cantidad));
    }

    private String sanitizeObs(String obs) {
        if (obs == null) return null;
        String s = obs.trim();
        return s.isEmpty() ? null : (s.length() > 255 ? s.substring(0, 255) : s);
    }

    
    private String generarNumeroOrden(EntityManager em) {
        try {
            Long max = em.createQuery("SELECT COALESCE(MAX(o.idCompraOrden), 0) FROM CompraOrden o", Long.class)
                    .getSingleResult();
            long next = (max == null ? 1 : max + 1);
            return String.format("OC-%06d", next);
        } catch (Exception e) {
            return "OC-" + System.currentTimeMillis();
        }
    }

    private String sanitizeDoc(String doc) {
        if (doc == null) return null;
        String s = doc.trim();
        return s.isEmpty() ? null : (s.length() > 64 ? s.substring(0, 64) : s);
    }
}
