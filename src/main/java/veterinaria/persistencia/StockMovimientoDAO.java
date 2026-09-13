package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.StockMovimiento;

public class StockMovimientoDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public List<StockMovimiento> listarPorProducto(Integer idProducto, Integer maxResults) {
        if (idProducto == null) {
            throw new IllegalArgumentException("idProducto requerido");
        }
        EntityManager em = getEntityManager();
        try {
            TypedQuery<StockMovimiento> q = em.createQuery(
                    "SELECT sm FROM StockMovimiento sm "
                    + "LEFT JOIN FETCH sm.usuario u "
                    + "LEFT JOIN FETCH sm.recibo r "
                    + "LEFT JOIN FETCH sm.compraRecepcion cr "
                    + "WHERE sm.producto.idProducto = :id "
                    + "ORDER BY sm.fecha DESC, sm.idStockMovimiento DESC",
                    StockMovimiento.class
            );
            q.setParameter("id", idProducto);
            if (maxResults != null && maxResults > 0) {
                q.setMaxResults(maxResults);
            }
            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }
}
