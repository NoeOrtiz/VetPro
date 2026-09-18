
package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.util.AppLog;
import veterinaria.util.enums.EstadoHospitalizacion;

public class HospitalizacionDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public Hospitalizacion buscarPorSlotId(EntityManager em, Long idSlot) {
        if (em == null || idSlot == null) return null;
        try {
            return em.createQuery(
                    "SELECT h FROM Hospitalizacion h WHERE h.slot IS NOT NULL AND h.slot.idSlot = :id",
                    Hospitalizacion.class
            ).setParameter("id", idSlot)
             .setMaxResults(1)
             .getResultStream()
             .findFirst()
             .orElse(null);
        } catch (Exception e) {
            AppLog.ignored(HospitalizacionDAO.class, "No se pudo buscar hospitalización por slot", e);
            return null;
        }
    }

    public Hospitalizacion buscarPorSlotId(Long idSlot) {
        EntityManager em = getEntityManager();
        try {
            return buscarPorSlotId(em, idSlot);
        } finally {
            em.close();
        }
    }

    public void crear(Hospitalizacion hospitalizacion) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.persist(hospitalizacion);
        em.getTransaction().commit();
        em.close();
    }

    public Hospitalizacion buscarPorId(Integer idHospitalizacion) {
        EntityManager em = getEntityManager();
        try {
            List<Hospitalizacion> res = em.createQuery(
                    "SELECT DISTINCT h FROM Hospitalizacion h "
                    + "LEFT JOIN FETCH h.veterinario v "
                    + "LEFT JOIN FETCH v.persona vp "
                    + "LEFT JOIN FETCH h.usuarioGestion ug "
                    + "LEFT JOIN FETCH ug.persona ugp "
                    + "LEFT JOIN FETCH h.cliente c "
                    + "LEFT JOIN FETCH c.persona cp "
                    + "LEFT JOIN FETCH h.mascota m "
                    + "LEFT JOIN FETCH m.cliente mc "
                    + "LEFT JOIN FETCH mc.persona mcp "
                    + "LEFT JOIN FETCH h.slot s "
                    + "WHERE h.idHospitalizacion = :id",
                    Hospitalizacion.class
            ).setParameter("id", idHospitalizacion)
             .getResultList();
            return (res == null || res.isEmpty()) ? null : res.get(0);
        } finally {
            em.close();
        }
    }

    public List<Hospitalizacion> obtenerTodas() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT h FROM Hospitalizacion h "
                    + "LEFT JOIN FETCH h.veterinario v "
                    + "LEFT JOIN FETCH v.persona vp "
                    + "LEFT JOIN FETCH h.usuarioGestion ug "
                    + "LEFT JOIN FETCH ug.persona ugp "
                    + "LEFT JOIN FETCH h.cliente c "
                    + "LEFT JOIN FETCH c.persona cp "
                    + "LEFT JOIN FETCH h.mascota m "
                    + "LEFT JOIN FETCH m.cliente mc "
                    + "LEFT JOIN FETCH mc.persona mcp "
                    + "LEFT JOIN FETCH h.slot s ",
                    Hospitalizacion.class
            ).getResultList();
        } finally {
            em.close();
        }
    }

    public void actualizar(Hospitalizacion hospitalizacion) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.merge(hospitalizacion);
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Compatibilidad temporal. Las hospitalizaciones son registros clínicos
     * históricos y no se eliminan físicamente, incluso después del alta.
     */
    @Deprecated
    public void eliminar(Integer idHospitalizacion) {
        // Intencionalmente sin borrado físico.
    }

    @Deprecated
    public boolean eliminar(Hospitalizacion hospitalizacion) {
        return false;
    }

    public Hospitalizacion obtenerActivaPorMascota(Integer idMascota) {
        EntityManager em = getEntityManager();
        try {
            List<Hospitalizacion> res = em.createQuery(
                    "SELECT h FROM Hospitalizacion h "
                    + "WHERE h.mascota.idMascota = :idMascota "
                    + "AND LOWER(COALESCE(h.estado, '')) = :estadoInternado "
                    + "ORDER BY h.fechaIngreso DESC, h.hora DESC, h.idHospitalizacion DESC",
                    Hospitalizacion.class
            )
            .setParameter("idMascota", idMascota)
            .setParameter("estadoInternado", EstadoHospitalizacion.INTERNADO.getEtiqueta().toLowerCase())
            .setMaxResults(1)
            .getResultList();

            return (res == null || res.isEmpty()) ? null : res.get(0);
        } finally {
            em.close();
        }
    }

    public boolean existeInternadoPorMascota(Integer idMascota, Integer idExcluir) {
        EntityManager em = getEntityManager();
        try {
            Long c = em.createQuery(
                    "SELECT COUNT(h) FROM Hospitalizacion h "
                    + "WHERE h.mascota.idMascota = :idMascota "
                    + "AND LOWER(COALESCE(h.estado, '')) = :estadoInternado "
                    + "AND (:idExcluir IS NULL OR h.idHospitalizacion <> :idExcluir)",
                    Long.class
            )
                    .setParameter("idMascota", idMascota)
                    .setParameter("estadoInternado", EstadoHospitalizacion.INTERNADO.getEtiqueta().toLowerCase())
                    .setParameter("idExcluir", idExcluir)
                    .getSingleResult();

            return c != null && c.longValue() > 0L;
        } finally {
            em.close();
        }
    }

    public boolean existeInternadoPorMascota(Integer idMascota) {
        return existeInternadoPorMascota(idMascota, null);
    }

}
