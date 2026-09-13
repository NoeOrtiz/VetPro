package veterinaria.persistencia;

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CompraOrden;
import veterinaria.entidad.CompraOrdenEstado;
import veterinaria.entidad.CompraOrdenItem;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Proveedor;

public class CompraOrdenDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(CompraOrden orden) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            prepararParaPersistencia(em, orden);
            em.persist(orden); // cascada persiste items
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean actualizar(CompraOrden orden) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            prepararParaPersistencia(em, orden);
            em.merge(orden);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    public CompraOrden buscarPorId(Integer idCompraOrden) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CompraOrden> q = em.createQuery(
                    "SELECT DISTINCT o FROM CompraOrden o "
                    + "LEFT JOIN FETCH o.proveedor p "
                    + "LEFT JOIN FETCH o.items i "
                    + "LEFT JOIN FETCH i.producto pr "
                    + "WHERE o.idCompraOrden = :id",
                    CompraOrden.class
            );
            q.setParameter("id", idCompraOrden);
            List<CompraOrden> res = q.getResultList();
            return res.isEmpty() ? null : res.get(0);
        } finally {
            if (em != null) em.close();
        }
    }

    public List<CompraOrden> buscarTodos(String texto, String estado) {
        EntityManager em = getEntityManager();
        try {
            String jpql = "SELECT DISTINCT o FROM CompraOrden o "
                    + "LEFT JOIN FETCH o.proveedor p "
                    + "LEFT JOIN FETCH o.items i "
                    + "LEFT JOIN FETCH i.producto pr "
                    + "WHERE 1=1";

            boolean hasTexto = texto != null && !texto.isBlank();
            CompraOrdenEstado estadoEnum = parseEstado(estado);
            boolean hasEstado = estadoEnum != null;

            if (hasTexto) {
                jpql += " AND (LOWER(o.numeroOrden) LIKE :t OR LOWER(p.razonSocial) LIKE :t)";
            }
            if (hasEstado) {
                jpql += " AND o.estado = :e";
            } else {
                jpql += " AND o.estado <> :anulada";
            }

            jpql += " ORDER BY o.fechaPedido DESC";

            TypedQuery<CompraOrden> q = em.createQuery(jpql, CompraOrden.class);

            if (hasTexto) {
                q.setParameter("t", "%" + texto.toLowerCase() + "%");
            }
            if (hasEstado) {
                q.setParameter("e", estadoEnum);
            } else {
                q.setParameter("anulada", CompraOrdenEstado.ANULADA);
            }

            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean anular(Integer idCompraOrden) throws Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            CompraOrden o = em.find(CompraOrden.class, idCompraOrden);
            if (o != null) {
                o.setEstado(CompraOrdenEstado.ANULADA);
                em.merge(o);
            }
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean existeNumeroOrden(String numeroOrden, Integer idExcluida) {
        if (numeroOrden == null || numeroOrden.isBlank()) return false;

        EntityManager em = getEntityManager();
        try {
            String jpql = "SELECT COUNT(o) FROM CompraOrden o WHERE o.numeroOrden = :n";
            if (idExcluida != null) jpql += " AND o.idCompraOrden <> :id";

            TypedQuery<Long> q = em.createQuery(jpql, Long.class);
            q.setParameter("n", numeroOrden.trim());
            if (idExcluida != null) q.setParameter("id", idExcluida);

            Long c = q.getSingleResult();
            return c != null && c > 0;
        } finally {
            if (em != null) em.close();
        }
    }

    private CompraOrdenEstado parseEstado(String estado) {
        if (estado == null) return null;
        String s = estado.trim().toUpperCase();

        if ("CONFIRMADO".equals(s)) s = "CONFIRMADA";

        try {
            return CompraOrdenEstado.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String obtenerProximoNumeroOrden() {
        EntityManager em = getEntityManager();
        try {
            String ultimo = em.createQuery(
                    "SELECT o.numeroOrden FROM CompraOrden o "
                    + "WHERE o.numeroOrden IS NOT NULL "
                    + "ORDER BY o.idCompraOrden DESC",
                    String.class
            )
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (ultimo == null || ultimo.isBlank()) {
                return "OC-000001";
            }

            String soloNumero = ultimo.replaceAll("[^0-9]", "");
            int num = Integer.parseInt(soloNumero);

            return String.format("OC-%06d", num + 1);

        } catch (Exception e) {
            return "OC-" + System.currentTimeMillis();
        } finally {
            em.close();
        }
    }

    private void prepararParaPersistencia(EntityManager em, CompraOrden orden) {
        if (orden == null) {
            throw new IllegalArgumentException("La orden es null");
        }

        if (orden.getProveedor() == null || orden.getProveedor().getIdProveedor() == null) {
            throw new IllegalArgumentException("Proveedor requerido");
        }
        Proveedor provRef = em.getReference(Proveedor.class, orden.getProveedor().getIdProveedor());
        orden.setProveedor(provRef);

        ProductoProveedorDAO ppDao = new ProductoProveedorDAO();

        if (orden.getItems() != null) {
            for (CompraOrdenItem it : orden.getItems()) {
                if (it == null) {
                    continue;
                }

                it.setOrden(orden);

                if (it.getProducto() == null || it.getProducto().getIdProducto() == null) {
                    throw new IllegalArgumentException("Producto requerido en ítem");
                }
                Producto prodRef = em.getReference(Producto.class, it.getProducto().getIdProducto());
                it.setProducto(prodRef);

                ppDao.asegurarVinculo(em, prodRef.getIdProducto(), provRef.getIdProveedor());

                if (it.getCantidad() == null) {
                    it.setCantidad(0);
                }
                if (it.getCostoUnitario() == null) {
                    it.setCostoUnitario(BigDecimal.ZERO);
                }
                if (it.getSubtotal() == null) {
                    it.setSubtotal(it.getCostoUnitario().multiply(BigDecimal.valueOf(it.getCantidad())));
                }
            }
        }
    }

}
