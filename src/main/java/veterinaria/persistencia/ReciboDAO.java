
package veterinaria.persistencia;

import javax.persistence.EntityManager;
import java.util.List;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;

public class ReciboDAO {
    private EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public Recibo crearRecibo(Recibo recibo, List<ReciboProductos> productos, List<ReciboMetodoPago> metodosPago) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(recibo);
            em.flush();
            for (ReciboProductos producto : productos) {
                producto.setRecibo(recibo); // Vincula el producto al recibo
                em.persist(producto);
            }

            for (ReciboMetodoPago metodoPago : metodosPago) {
                metodoPago.setRecibo(recibo); // Vincula el método de pago al recibo
                em.persist(metodoPago);
            }

            em.getTransaction().commit();
            return recibo;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public Recibo obtenerReciboConDetalles(Long idRecibo) {
        EntityManager em = getEntityManager();
        try {
            Recibo recibo = em.createQuery(
                    "SELECT r FROM Recibo r "
                    + "LEFT JOIN FETCH r.cliente c "
                    + "JOIN FETCH r.usuario u "
                    + "LEFT JOIN FETCH u.persona up "
                    + "WHERE r.idRecibo = :id", Recibo.class)
                    .setParameter("id", idRecibo)
                    .getSingleResult();

            if (recibo.getMetodosPago() != null) {
                recibo.getMetodosPago().size();
                for (ReciboMetodoPago mp : recibo.getMetodosPago()) {
                    if (mp != null && mp.getMetodoPago() != null) {
                        mp.getMetodoPago().getNombre();
                    }
                }
            }

            List<ReciboProductos> items = em.createQuery(
                    "SELECT rp FROM ReciboProductos rp "
                    + "JOIN FETCH rp.producto "
                    + "WHERE rp.recibo.idRecibo = :id", ReciboProductos.class)
                    .setParameter("id", idRecibo)
                    .getResultList();

            try {
                recibo.setProductos(items);
            } catch (Exception ignore) {
                for (ReciboProductos rp : items) {
                    if (rp != null && rp.getProducto() != null) {
                        rp.getProducto().getIdProducto();
                    }
                }
                if (recibo.getProductos() != null) {
                    recibo.getProductos().size();
                    for (ReciboProductos rp : recibo.getProductos()) {
                        if (rp != null && rp.getProducto() != null) {
                            rp.getProducto().getIdProducto();
                        }
                    }
                }
            }

            return recibo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }


    /**
     * Consulta histórica de recibos. Proyección de solo lectura para evitar
     * asociaciones LAZY y no modificar registros de ventas.
     */
    public List<Object[]> buscarRecibos(String texto) {
        EntityManager em = getEntityManager();
        try {
            String filtro = texto == null ? "" : texto.trim().toLowerCase(java.util.Locale.ROOT);
            String jpql = "SELECT r.idRecibo, r.fecha, p.nombre, p.apellido, r.totalRecibo, r.tipo "
                    + "FROM Recibo r LEFT JOIN r.cliente c LEFT JOIN c.persona p "
                    + "WHERE LOWER(r.tipo) = 'venta' ";
            Long id = null;
            try {
                String numerico = filtro.toUpperCase(java.util.Locale.ROOT)
                        .replace("RC-", "").replaceAll("[^0-9]", "");
                if (!numerico.isEmpty() && filtro.matches("(?i)(rc-)?[0-9]+")) {
                    id = Long.valueOf(numerico);
                }
            } catch (NumberFormatException ignore) {
                // El texto se interpreta como nombre de cliente.
            }
            if (id != null) {
                jpql += "AND r.idRecibo = :id ";
            } else if (!filtro.isEmpty()) {
                jpql += "AND (LOWER(COALESCE(p.nombre, '')) LIKE :texto "
                        + "OR LOWER(COALESCE(p.apellido, '')) LIKE :texto) ";
            }
            jpql += "ORDER BY r.fecha DESC, r.idRecibo DESC";
            javax.persistence.TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (id != null) query.setParameter("id", id);
            else if (!filtro.isEmpty()) query.setParameter("texto", "%" + filtro + "%");
            return query.setMaxResults(500).getResultList();
        } finally {
            em.close();
        }
    }
}