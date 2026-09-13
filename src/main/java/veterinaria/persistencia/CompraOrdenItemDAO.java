package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CompraOrdenItem;

public class CompraOrdenItemDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public List<CompraOrdenItem> buscarItemsDeOrden(Integer idCompraOrden) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<CompraOrdenItem> q = em.createQuery(
                    "SELECT i FROM CompraOrdenItem i JOIN FETCH i.producto WHERE i.orden.idCompraOrden = :id",
                    CompraOrdenItem.class);
            q.setParameter("id", idCompraOrden);
            return q.getResultList();
        } finally {
            em.close();
        }
    }
}
