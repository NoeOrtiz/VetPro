package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Producto;
import veterinaria.entidad.ProductoProveedor;
import veterinaria.entidad.Proveedor;

public class ProductoProveedorDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void asegurarVinculo(EntityManager em, Integer idProducto, Integer idProveedor) {
        if (em == null) throw new IllegalArgumentException("EntityManager null");
        if (idProducto == null || idProveedor == null) return;

        Long count = em.createQuery(
                "SELECT COUNT(pp) FROM ProductoProveedor pp "
                + "WHERE pp.producto.idProducto = :p AND pp.proveedor.idProveedor = :v",
                Long.class
        )
        .setParameter("p", idProducto)
        .setParameter("v", idProveedor)
        .getSingleResult();

        if (count != null && count > 0) {
            em.createQuery(
                    "UPDATE ProductoProveedor pp SET pp.activo = true "
                    + "WHERE pp.producto.idProducto = :p AND pp.proveedor.idProveedor = :v"
            )
            .setParameter("p", idProducto)
            .setParameter("v", idProveedor)
            .executeUpdate();
            return;
        }

        Producto prodRef = em.getReference(Producto.class, idProducto);
        Proveedor provRef = em.getReference(Proveedor.class, idProveedor);

        ProductoProveedor pp = new ProductoProveedor();
        pp.setProducto(prodRef);
        pp.setProveedor(provRef);
        pp.setActivo(true);
        pp.setEsDefault(false);

        em.persist(pp);
    }

    public void sincronizarProveedorDefault(EntityManager em, Producto producto) {
        if (em == null) throw new IllegalArgumentException("EntityManager null");
        if (producto == null || producto.getIdProducto() == null) return;

        Integer idProducto = producto.getIdProducto();
        Integer idProveedor = (producto.getProveedor() == null) ? null : producto.getProveedor().getIdProveedor();

        em.createQuery("UPDATE ProductoProveedor pp SET pp.esDefault = false WHERE pp.producto.idProducto = :p")
                .setParameter("p", idProducto)
                .executeUpdate();

        if (idProveedor == null) {
            return;
        }

        asegurarVinculo(em, idProducto, idProveedor);

        em.createQuery(
                "UPDATE ProductoProveedor pp SET pp.esDefault = true, pp.activo = true "
                + "WHERE pp.producto.idProducto = :p AND pp.proveedor.idProveedor = :v"
        )
        .setParameter("p", idProducto)
        .setParameter("v", idProveedor)
        .executeUpdate();
    }

    public List<ProductoProveedor> listarPorProducto(Integer idProducto) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<ProductoProveedor> q = em.createQuery(
                    "SELECT pp FROM ProductoProveedor pp "
                    + "LEFT JOIN FETCH pp.proveedor pr "
                    + "WHERE pp.producto.idProducto = :p AND (pp.activo IS NULL OR pp.activo = true) "
                    + "ORDER BY (CASE WHEN pp.esDefault = true THEN 0 ELSE 1 END), pr.razonSocial",
                    ProductoProveedor.class
            );
            q.setParameter("p", idProducto);
            return q.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }
}
