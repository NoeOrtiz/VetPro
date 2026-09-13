
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
                    + "JOIN FETCH r.cliente c "
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

}