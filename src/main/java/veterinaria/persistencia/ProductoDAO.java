package veterinaria.persistencia;

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Producto;
import veterinaria.entidad.StockMovimiento;
import veterinaria.entidad.StockMovimientoTipo;
import veterinaria.servicio.AuditoriaService;
import veterinaria.util.AppLog;

public class ProductoDAO {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(Producto producto) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            em.persist(producto);

            em.flush();

            new ProductoProveedorDAO().sincronizarProveedorDefault(em, producto);

            Integer stockInicial = (producto.getStock() == null) ? 0 : producto.getStock();
            if (stockInicial > 0) {
                StockMovimiento mov = new StockMovimiento();
                mov.setFecha(new Date());
                mov.setTipo(StockMovimientoTipo.STOCK_INICIAL);
                mov.setCantidad(stockInicial);
                mov.setStockAntes(0);
                mov.setStockDespues(stockInicial);
                mov.setProducto(producto);
                mov.setObservacion("Stock inicial");
                em.persist(mov);
            }

            em.getTransaction().commit();
            auditoriaService.registrar("CREATE", "Producto", (producto != null ? Long.valueOf(producto.getIdProducto()) : null), "ProductoDAO", "Alta de producto: " + (producto != null ? producto.getNombre() : ""), AuditoriaService.RESULT_OK, null, null, null);
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Producto> buscarTodos() {
        EntityManager em = getEntityManager();
        try {
            // 🌟 Ordenamos primero por rubro y luego por nombre
            TypedQuery<Producto> query = em.createQuery("SELECT p FROM Producto p ORDER BY p.rubro ASC, p.nombre ASC", Producto.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Producto> buscarPorEstado(String estadoFiltro) {
        EntityManager em = getEntityManager();
        try {
            String jpql = "SELECT p FROM Producto p";

            if ("Activos".equalsIgnoreCase(estadoFiltro)) {
                jpql += " WHERE p.estado = 'Activo' OR p.estado IS NULL OR TRIM(p.estado) = ''";
            } else if ("Eliminados".equalsIgnoreCase(estadoFiltro)) {
                jpql += " WHERE p.estado = 'Inactivo'";
            }

            // 🌟 Aseguramos también el orden por rubro y nombre aquí
            jpql += " ORDER BY p.rubro ASC, p.nombre ASC";

            TypedQuery<Producto> query = em.createQuery(jpql, Producto.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Productos seleccionables en una venta: activos y con existencia real.
     * Los productos con stock 0 permanecen en Inventario, pero no se ofrecen para vender.
     */
    public List<Producto> buscarDisponiblesParaVenta() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Producto> query = em.createQuery(
                    "SELECT p FROM Producto p "
                    + "WHERE (p.estado = 'Activo' OR p.estado IS NULL OR TRIM(p.estado) = '') "
                    + "AND COALESCE(p.stock, 0) > 0 "
                    + "ORDER BY p.rubro ASC, p.nombre ASC", Producto.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Producto buscarPorId(Integer idProducto) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Producto.class, idProducto);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean actualizar(Producto producto) throws Exception {
        EntityManager em = getEntityManager();
        boolean retornar = false;
        try {
            em.getTransaction().begin();

            if (producto == null || producto.getIdProducto() == null) {
                throw new IllegalArgumentException("Producto inválido para actualizar.");
            }

            Producto managed = em.find(Producto.class, producto.getIdProducto());
            if (managed == null) {
                throw new IllegalStateException("Producto no encontrado (id=" + producto.getIdProducto() + ").");
            }

            Integer stockAntes = (managed.getStock() == null) ? 0 : managed.getStock();
            Integer stockNuevo = (producto.getStock() == null) ? 0 : producto.getStock();

            managed.setCodigo(producto.getCodigo());
            managed.setNombre(producto.getNombre());
            managed.setRubro(producto.getRubro());
            managed.setDescripcion(producto.getDescripcion());
            managed.setPrecioCosto(producto.getPrecioCosto());
            managed.setIva(producto.getIva());
            managed.setDescuento(producto.getDescuento());
            managed.setUmedida(producto.getUmedida());
            managed.setProveedor(producto.getProveedor());
            managed.setEstado(producto.getEstado());
            try {
                managed.setStockMinimo(producto.getStockMinimo());
            } catch (Exception ex) {
            }

            new ProductoProveedorDAO().sincronizarProveedorDefault(em, managed);

            if (!stockNuevo.equals(stockAntes)) {
                int delta = stockNuevo - stockAntes;

                managed.setStock(stockNuevo);

                StockMovimiento mov = new StockMovimiento();
                mov.setFecha(new Date());
                mov.setTipo(delta > 0 ? StockMovimientoTipo.AJUSTE_POSITIVO : StockMovimientoTipo.AJUSTE_NEGATIVO);
                mov.setCantidad(Math.abs(delta));
                mov.setStockAntes(stockAntes);
                mov.setStockDespues(stockNuevo);
                mov.setProducto(managed);
                mov.setObservacion("Ajuste por edición de producto");
                em.persist(mov);
            }

            em.getTransaction().commit();
            auditoriaService.registrar("UPDATE", "Producto", (producto != null ? Long.valueOf(producto.getIdProducto()) : null), "ProductoDAO", "Actualización de producto: " + (producto != null ? producto.getNombre() : ""), AuditoriaService.RESULT_OK, null, null, null);
            retornar = true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return retornar;
    }

    public boolean eliminar(Producto producto) throws Exception {
        EntityManager em = getEntityManager();
        boolean state = false;
        try {
            em.getTransaction().begin();
            Producto productoManaged = em.find(Producto.class, producto.getIdProducto());
            if (productoManaged != null) {
                productoManaged.setEstado("Inactivo");
                em.merge(productoManaged);
                state = true;
            }
            em.getTransaction().commit();

            auditoriaService.registrar("DELETE", "Producto", (producto != null ? Long.valueOf(producto.getIdProducto()) : null), "ProductoDAO", "Baja de producto: " + (producto != null ? producto.getNombre() : ""), AuditoriaService.RESULT_OK, null, null, null);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return state;
    }

    public boolean actualizarStockVenta(Integer idProducto, Integer cantidad) {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = null;
        try {
            if (idProducto == null) {
                AppLog.warning(ProductoDAO.class, "No se pudo descontar stock: idProducto nulo.");
                return false;
            }
            if (cantidad == null || cantidad <= 0) {
                AppLog.warning(ProductoDAO.class, "No se pudo descontar stock del producto " + idProducto + ": cantidad inválida=" + cantidad);
                return false;
            }

            transaction = em.getTransaction();
            transaction.begin();

            Producto producto = em.find(Producto.class, idProducto);
            if (producto == null) {
                AppLog.warning(ProductoDAO.class, "Producto no encontrado con id: " + idProducto);
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                return false;
            }

            int stockActual = (producto.getStock() == null) ? 0 : producto.getStock();
            int nuevoStock = stockActual - cantidad;
            if (nuevoStock < 0) {
                AppLog.warning(ProductoDAO.class,
                        "Stock insuficiente para producto id=" + idProducto + ". Stock actual=" + stockActual + ", cantidad solicitada=" + cantidad);
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                return false;
            }

            producto.setStock(nuevoStock);
            em.merge(producto);

            StockMovimiento mov = new StockMovimiento();
            mov.setFecha(new Date());
            mov.setTipo(StockMovimientoTipo.SALIDA_VENTA);
            mov.setCantidad(cantidad);
            mov.setStockAntes(stockActual);
            mov.setStockDespues(nuevoStock);
            mov.setProducto(producto);
            mov.setObservacion("Descuento automático por venta");
            em.persist(mov);

            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            AppLog.error(ProductoDAO.class, "Error al actualizar stock por venta del producto id=" + idProducto, e);
            return false;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
