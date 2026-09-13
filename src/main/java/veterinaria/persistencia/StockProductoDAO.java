package veterinaria.persistencia;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import veterinaria.entidad.StockProductoInfo;

public class StockProductoDAO {

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public List<StockProductoInfo> buscarStock(String textoBusqueda, String rubro, Integer idProveedor, StockProductoInfo.EstadoStock estado) {
        EntityManager em = getEntityManager();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            sb.append(" p.idProducto AS idProducto, ");
            sb.append(" p.codigo AS codigo, ");
            sb.append(" p.nombre AS producto, ");
            sb.append(" p.rubro AS rubro, ");
            sb.append(" COALESCE(ppDef.idProveedor, p.idProveedor) AS idProveedor, ");
            sb.append(" COALESCE( ");
            sb.append("   NULLIF(TRIM(prDef.razonSocial), ''), ");
            sb.append("   NULLIF(TRIM(CONCAT(COALESCE(peDef.nombre, ''), ' ', COALESCE(peDef.apellido, ''))), ''), ");
            sb.append("   NULLIF(TRIM(pr.razonSocial), ''), ");
            sb.append("   NULLIF(TRIM(CONCAT(COALESCE(pe.nombre, ''), ' ', COALESCE(pe.apellido, ''))), ''), ");
            sb.append("   'Sin proveedor' ");
            sb.append(" ) AS proveedor, ");
            sb.append(" COALESCE(ppDef.costoUltimo, p.precio_costo, 0) AS precioCompra, ");
            sb.append(" ROUND( ");
            sb.append("   COALESCE(ppDef.costoUltimo, p.precio_costo, 0) ");
            sb.append("   + (COALESCE(ppDef.costoUltimo, p.precio_costo, 0) * COALESCE(CAST(NULLIF(p.iva, '') AS DECIMAL(10,2)), 0) / 100) ");
            sb.append("   + (COALESCE(ppDef.costoUltimo, p.precio_costo, 0) * COALESCE(r.porcentaje_ganancia, CAST(cfgGan.valor AS DECIMAL(10,2)), 0) / 100), ");
            sb.append("   2 ");
            sb.append(" ) AS precioVenta, ");
            sb.append(" p.stock AS stockActual, ");
            sb.append(" COALESCE(NULLIF(p.stock_minimo, 0), r.stock_minimo_default, 0) AS stockMinimo ");
            sb.append("FROM producto p ");
            sb.append("LEFT JOIN rubro r ON r.nombre = p.rubro AND r.activo = 1 ");
            sb.append("LEFT JOIN configuracion cfgGan ON cfgGan.clave = 'VENTAS_PORC_GANANCIA_GLOBAL' ");
            sb.append("LEFT JOIN producto_proveedor ppDef ON ppDef.idProducto = p.idProducto AND ppDef.esDefault = 1 AND ppDef.activo = 1 ");
            sb.append("LEFT JOIN proveedor prDef ON prDef.idProveedor = ppDef.idProveedor ");
            sb.append("LEFT JOIN persona peDef ON peDef.idPersona = prDef.idPersona ");
            sb.append("LEFT JOIN proveedor pr ON pr.idProveedor = p.idProveedor ");
            sb.append("LEFT JOIN persona pe ON pe.idPersona = pr.idPersona ");
            sb.append("WHERE (p.estado IS NULL OR p.estado <> 'Inactivo') ");

            if (textoBusqueda != null && !textoBusqueda.trim().isEmpty()) {
                sb.append(" AND ( ");
                sb.append("   LOWER(TRIM(COALESCE(p.codigo, ''))) LIKE :q ");
                sb.append("   OR LOWER(TRIM(COALESCE(p.nombre, ''))) LIKE :q ");
                sb.append("   OR LOWER(TRIM(COALESCE(p.rubro, ''))) LIKE :q ");
                sb.append("   OR LOWER(TRIM(COALESCE( ");
                sb.append("        prDef.razonSocial, ");
                sb.append("        CONCAT(COALESCE(peDef.nombre, ''), ' ', COALESCE(peDef.apellido, '')), ");
                sb.append("        pr.razonSocial, ");
                sb.append("        CONCAT(COALESCE(pe.nombre, ''), ' ', COALESCE(pe.apellido, '')), ");
                sb.append("        '' ");
                sb.append("   ))) LIKE :q ");
                sb.append(" ) ");
            }
            if (rubro != null && !rubro.trim().isEmpty() && !"Todos".equalsIgnoreCase(rubro.trim())) {
                sb.append(" AND p.rubro = :rubro ");
            }
            if (idProveedor != null && idProveedor > 0) {
                sb.append(" AND COALESCE(ppDef.idProveedor, p.idProveedor) = :idProveedor ");
            }
            if (estado != null) {
                switch (estado) {
                    case SIN_STOCK:
                        sb.append(" AND (p.stock IS NULL OR p.stock <= 0) ");
                        break;
                    case NORMAL:
                        sb.append(" AND (p.stock IS NOT NULL AND p.stock > COALESCE(NULLIF(p.stock_minimo, 0), r.stock_minimo_default, 0)) ");
                        break;
                    case BAJO:
                        sb.append(" AND (p.stock IS NOT NULL AND p.stock > 0) ");
                        sb.append(" AND (p.stock <= COALESCE(NULLIF(p.stock_minimo, 0), r.stock_minimo_default, 0)) ");
                        break;
                    default:
                        break;
                }
            }

            sb.append(" ORDER BY p.nombre ASC ");

            Query q = em.createNativeQuery(sb.toString());
            if (textoBusqueda != null && !textoBusqueda.trim().isEmpty()) {
                q.setParameter("q", "%" + textoBusqueda.trim().toLowerCase() + "%");
            }
            if (rubro != null && !rubro.trim().isEmpty() && !"Todos".equalsIgnoreCase(rubro.trim())) {
                q.setParameter("rubro", rubro.trim());
            }
            if (idProveedor != null && idProveedor > 0) {
                q.setParameter("idProveedor", idProveedor);
            }

            @SuppressWarnings("unchecked")
            List<Object[]> rows = q.getResultList();
            List<StockProductoInfo> out = new ArrayList<>();
            for (Object[] r : rows) {
                Integer _id = (r[0] != null) ? ((Number) r[0]).intValue() : null;
                String _codigo = (String) r[1];
                String _producto = (String) r[2];
                String _rubro = (String) r[3];
                Integer _idProv = (r[4] != null) ? ((Number) r[4]).intValue() : null;
                String _prov = (String) r[5];
                BigDecimal _precioCompra = toBigDecimal(r[6]);
                BigDecimal _precioVenta = toBigDecimal(r[7]);
                Integer _stock = (r[8] != null) ? ((Number) r[8]).intValue() : 0;
                Integer _stockMin = (r[9] != null) ? ((Number) r[9]).intValue() : 0;

                StockProductoInfo dto = new StockProductoInfo(_id, _codigo, _producto, _rubro, _idProv, _prov, _precioCompra, _precioVenta, _stock, _stockMin);
                out.add(dto);
            }
            return out;

        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    public List<String> listarRubros() {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createNativeQuery("SELECT r.nombre FROM rubro r WHERE r.activo = 1 ORDER BY r.nombre ASC");
            @SuppressWarnings("unchecked")
            List<Object> rows = q.getResultList();
            List<String> out = new ArrayList<>();
            for (Object o : rows) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
