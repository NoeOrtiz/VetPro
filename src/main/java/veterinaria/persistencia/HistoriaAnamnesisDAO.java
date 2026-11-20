package veterinaria.persistencia;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.HistoriaAnamnesis;

/** Versiones inmutables de anamnesis, vinculadas a mascota. */
public class HistoriaAnamnesisDAO {

    public HistoriaAnamnesis ultima(Integer idMascota) {
        if (idMascota == null) return null;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<HistoriaAnamnesis> lista = em.createQuery(
                    "SELECT a FROM HistoriaAnamnesis a WHERE a.mascota.idMascota = :id "
                    + "ORDER BY a.fechaRegistro DESC, a.idAnamnesis DESC",
                    HistoriaAnamnesis.class)
                    .setParameter("id", idMascota).setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    public List<HistoriaAnamnesis> listarVersiones(Integer idMascota) {
        if (idMascota == null) return java.util.Collections.emptyList();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM HistoriaAnamnesis a WHERE a.mascota.idMascota = :id "
                            + "ORDER BY a.fechaRegistro DESC, a.idAnamnesis DESC",
                    HistoriaAnamnesis.class)
                    .setParameter("id", idMascota)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void guardarVersion(HistoriaAnamnesis datos) {
        if (datos == null || datos.getMascota() == null
                || datos.getMascota().getIdMascota() == null)
            throw new IllegalArgumentException("Debe seleccionar un paciente válido.");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            datos.setFechaRegistro(LocalDateTime.now());
            datos.setMascota(em.getReference(veterinaria.entidad.Mascota.class,
                    datos.getMascota().getIdMascota()));
            em.persist(datos);
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }
}
