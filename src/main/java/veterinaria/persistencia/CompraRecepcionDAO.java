package veterinaria.persistencia;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CompraOrden;
import veterinaria.entidad.CompraOrdenEstado;
import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.CompraRecepcionEstado;
import veterinaria.entidad.CompraRecepcionItem;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Usuario;
import veterinaria.servicio.StockTxService;

public class CompraRecepcionDAO {

    private final StockTxService stockTx = new StockTxService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public List<CompraRecepcion> buscarTodas() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CompraRecepcion> q = em.createQuery(
                    "SELECT r FROM CompraRecepcion r ORDER BY r.fecha DESC",
                    CompraRecepcion.class
            );
            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public CompraRecepcion buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            if (id == null) return null;
            List<CompraRecepcion> res = em.createQuery(
                    "SELECT DISTINCT r FROM CompraRecepcion r "
                    + "LEFT JOIN FETCH r.orden o "
                    + "LEFT JOIN FETCH o.proveedor prov "
                    + "LEFT JOIN FETCH r.usuarioAnulacion ua "
                    + "LEFT JOIN FETCH ua.persona uap "
                    + "LEFT JOIN FETCH r.items it "
                    + "LEFT JOIN FETCH it.producto p "
                    + "LEFT JOIN FETCH p.rubro rub "
                    + "WHERE r.idCompraRecepcion = :id",
                    CompraRecepcion.class
            ).setParameter("id", id).getResultList();
            return res.isEmpty() ? null : res.get(0);
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean anular(Long idCompraRecepcion, Usuario usuario, String motivo) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            CompraRecepcion r = em.find(CompraRecepcion.class, idCompraRecepcion);
            if (r == null) {
                throw new IllegalStateException("Recepción no encontrada (id=" + idCompraRecepcion + ").");
            }
            r = em.createQuery(
                    "SELECT DISTINCT r FROM CompraRecepcion r "
                    + "LEFT JOIN FETCH r.orden o "
                    + "LEFT JOIN FETCH o.items oi "
                    + "LEFT JOIN FETCH oi.producto op "
                    + "LEFT JOIN FETCH r.items it "
                    + "LEFT JOIN FETCH it.producto p "
                    + "WHERE r.idCompraRecepcion = :id",
                    CompraRecepcion.class
            ).setParameter("id", idCompraRecepcion).getSingleResult();

            if (r.getEstado() == CompraRecepcionEstado.ANULADA) {
                throw new IllegalStateException("La recepción ya se encuentra ANULADA.");
            }
            if (r.getEstado() != CompraRecepcionEstado.CONFIRMADA) {
                throw new IllegalStateException("Solo se puede anular una recepción CONFIRMADA.");
            }
            if (r.getItems() == null || r.getItems().isEmpty()) {
                throw new IllegalStateException("La recepción no tiene items.");
            }

            
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new IllegalStateException("Debe indicar el motivo de anulación.");
            }
            String motivoLimpio = motivo.trim();
            if (motivoLimpio.length() > 255) {
                motivoLimpio = motivoLimpio.substring(0, 255);
            }

            for (CompraRecepcionItem it : r.getItems()) {
                if (it == null || it.getProducto() == null || it.getProducto().getIdProducto() == null) continue;
                Integer cant = it.getCantidadRecibida();
                if (cant == null || cant <= 0) continue;

                stockTx.registrarSalidaPorAnulacionCompra(
                        em,
                        it.getProducto().getIdProducto(),
                        cant,
                        r,
                        usuario,
                        "Anulación recepción OC " + (r.getOrden() != null ? r.getOrden().getNumeroOrden() : "") + " | Motivo: " + motivoLimpio
                );
            }

            r.setEstado(CompraRecepcionEstado.ANULADA);
            r.setMotivoAnulacion(motivoLimpio);
            r.setFechaAnulacion(new java.util.Date());
            if (usuario != null) {
                r.setUsuarioAnulacion(usuario);
            }
            em.merge(r);

            CompraOrden o = r.getOrden();
            if (o != null && o.getIdCompraOrden() != null) {
                o = em.find(CompraOrden.class, o.getIdCompraOrden());
                if (o != null) {
                    Map<Integer, Integer> recibido = sumarRecibidoPorOrdenTx(em, o.getIdCompraOrden());
                    boolean completa = true;
                    for (var oi : o.getItems()) {
                        Integer idProd = oi.getProducto().getIdProducto();
                        int pedida = (oi.getCantidad() == null) ? 0 : oi.getCantidad();
                        int rec = recibido.getOrDefault(idProd, 0);
                        if (pedida - rec > 0) {
                            completa = false;
                            break;
                        }
                    }

                    if (!completa && o.getEstado() == CompraOrdenEstado.CERRADA) {
                        o.setEstado(CompraOrdenEstado.CONFIRMADA);
                        em.merge(o);
                    }
                }
            }

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    public List<CompraRecepcion> buscarPorOrden(Integer idCompraOrden) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CompraRecepcion> q = em.createQuery(
                    "SELECT DISTINCT r FROM CompraRecepcion r "
                    + "LEFT JOIN FETCH r.orden o "
                    + "LEFT JOIN FETCH r.items it "
                    + "LEFT JOIN FETCH it.producto p "
                    + "WHERE o.idCompraOrden = :id "
                    + "ORDER BY r.fecha DESC",
                    CompraRecepcion.class
            );
            q.setParameter("id", idCompraOrden);
            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public Map<Integer, Integer> sumarRecibidoPorOrden(Integer idCompraOrden) {
        EntityManager em = getEntityManager();
        try {
            List<Object[]> rows = em.createQuery(
                    "SELECT it.producto.idProducto, SUM(it.cantidadRecibida) "
                    + "FROM CompraRecepcionItem it "
                    + "WHERE it.recepcion.orden.idCompraOrden = :id "
                    + "AND it.recepcion.estado <> :anulada "
                    + "GROUP BY it.producto.idProducto",
                    Object[].class
            ).setParameter("id", idCompraOrden)
             .setParameter("anulada", CompraRecepcionEstado.ANULADA)
             .getResultList();

            Map<Integer, Integer> map = new HashMap<>();
            for (Object[] r : rows) {
                Integer idProd = (Integer) r[0];
                Number sum = (Number) r[1];
                map.put(idProd, (sum == null) ? 0 : sum.intValue());
            }
            return map;
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean crear(CompraRecepcion recepcion, Usuario usuario) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            if (recepcion == null) {
                throw new IllegalArgumentException("Recepción requerida.");
            }
            if (recepcion.getOrden() == null || recepcion.getOrden().getIdCompraOrden() == null) {
                throw new IllegalArgumentException("Orden requerida.");
            }
            if (recepcion.getItems() == null || recepcion.getItems().isEmpty()) {
                throw new IllegalArgumentException("Debe ingresar al menos un item recibido.");
            }

            CompraOrden orden = em.find(CompraOrden.class, recepcion.getOrden().getIdCompraOrden(), LockModeType.PESSIMISTIC_WRITE);
            if (orden == null) {
                throw new IllegalStateException("Orden no encontrada (id=" + recepcion.getOrden().getIdCompraOrden() + ").");
            }
            if (orden.getEstado() != CompraOrdenEstado.CONFIRMADA) {
                throw new IllegalStateException("Solo se puede recepcionar una Orden CONFIRMADA.");
            }

            validarRecepcionNoExcedaOrden(em, orden, recepcion);

            recepcion.setOrden(orden);
            if (recepcion.getEstado() == null) {
                recepcion.setEstado(CompraRecepcionEstado.CONFIRMADA);
            }

            for (CompraRecepcionItem it : recepcion.getItems()) {
                if (it == null) continue;

                if (it.getProducto() == null || it.getProducto().getIdProducto() == null) {
                    throw new IllegalArgumentException("Producto requerido en los items.");
                }
                if (it.getCantidadRecibida() == null || it.getCantidadRecibida() <= 0) {
                    throw new IllegalArgumentException("Cantidad recibida inválida.");
                }

                Producto prodRef = em.getReference(Producto.class, it.getProducto().getIdProducto());
                it.setProducto(prodRef);
                it.setRecepcion(recepcion);
            }

            em.persist(recepcion); // cascada persiste items
            em.flush();

            for (CompraRecepcionItem it : recepcion.getItems()) {
                stockTx.registrarEntradaPorCompra(
                        em,
                        it.getProducto().getIdProducto(),
                        it.getCantidadRecibida(),
                        recepcion,
                        usuario,
                        "Recepción OC " + orden.getNumeroOrden()
                );
            }

            Map<Integer, Integer> recibido = sumarRecibidoPorOrdenTx(em, orden.getIdCompraOrden());

            boolean completa = true;
            orden = em.find(CompraOrden.class, orden.getIdCompraOrden()); // managed
            orden.getItems().forEach(oi -> {
            });

            for (var oi : orden.getItems()) {
                Integer idProd = oi.getProducto().getIdProducto();
                int pedida = (oi.getCantidad() == null) ? 0 : oi.getCantidad();
                int rec = recibido.getOrDefault(idProd, 0);
                int pendiente = pedida - rec;
                if (pendiente > 0) {
                    completa = false;
                    break;
                }
            }

            if (completa) {
                orden.setEstado(CompraOrdenEstado.CERRADA);
                em.merge(orden);
            }

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    private void validarRecepcionNoExcedaOrden(EntityManager em, CompraOrden orden, CompraRecepcion recepcion) {
        if (orden == null || recepcion == null || recepcion.getItems() == null) {
            throw new IllegalArgumentException("Recepción inválida.");
        }

        Map<Integer, Integer> cantidadesPedidas = new HashMap<>();
        if (orden.getItems() != null) {
            orden.getItems().forEach(oi -> {
                if (oi != null && oi.getProducto() != null && oi.getProducto().getIdProducto() != null) {
                    cantidadesPedidas.put(
                            oi.getProducto().getIdProducto(),
                            (oi.getCantidad() == null) ? 0 : oi.getCantidad()
                    );
                }
            });
        }

        if (cantidadesPedidas.isEmpty()) {
            throw new IllegalStateException("La orden no tiene items válidos.");
        }

        Map<Integer, Integer> recibidoPrevio = sumarRecibidoPorOrdenTx(em, orden.getIdCompraOrden());
        Map<Integer, Integer> recibidoEnEstaRecepcion = new HashMap<>();
        Set<Integer> productosFueraDeOrden = new HashSet<>();

        for (CompraRecepcionItem it : recepcion.getItems()) {
            if (it == null || it.getProducto() == null || it.getProducto().getIdProducto() == null) {
                continue;
            }

            Integer idProducto = it.getProducto().getIdProducto();
            int cantidad = (it.getCantidadRecibida() == null) ? 0 : it.getCantidadRecibida();
            recibidoEnEstaRecepcion.merge(idProducto, cantidad, Integer::sum);

            if (!cantidadesPedidas.containsKey(idProducto)) {
                productosFueraDeOrden.add(idProducto);
            }
        }

        if (!productosFueraDeOrden.isEmpty()) {
            throw new IllegalStateException("La recepción contiene productos que no pertenecen a la orden.");
        }

        for (Map.Entry<Integer, Integer> entry : recibidoEnEstaRecepcion.entrySet()) {
            Integer idProducto = entry.getKey();
            int pedido = cantidadesPedidas.getOrDefault(idProducto, 0);
            int previo = recibidoPrevio.getOrDefault(idProducto, 0);
            int actual = entry.getValue();
            int total = previo + actual;

            if (actual <= 0) {
                throw new IllegalArgumentException("Cantidad recibida inválida.");
            }
            if (total > pedido) {
                throw new IllegalStateException(
                        "La recepción excede la cantidad pedida para el producto ID "
                                + idProducto + " (pedido=" + pedido + ", ya recibido=" + previo + ", intentando recibir=" + actual + ")."
                );
            }
        }
    }

    private Map<Integer, Integer> sumarRecibidoPorOrdenTx(EntityManager em, Integer idCompraOrden) {
        List<Object[]> rows = em.createQuery(
                "SELECT it.producto.idProducto, SUM(it.cantidadRecibida) "
                + "FROM CompraRecepcionItem it "
                + "WHERE it.recepcion.orden.idCompraOrden = :id "
                + "AND it.recepcion.estado <> :anulada "
                + "GROUP BY it.producto.idProducto",
                Object[].class
        ).setParameter("id", idCompraOrden)
         .setParameter("anulada", CompraRecepcionEstado.ANULADA)
         .getResultList();

        Map<Integer, Integer> map = new HashMap<>();
        for (Object[] r : rows) {
            Integer idProd = (Integer) r[0];
            Number sum = (Number) r[1];
            map.put(idProd, (sum == null) ? 0 : sum.intValue());
        }
        return map;
    }
}
