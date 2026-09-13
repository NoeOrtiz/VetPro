package veterinaria.persistencia;

import veterinaria.entidad.HistorialConsumo;

import javax.persistence.*;
import java.util.List;

public class HistorialConsumoDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void guardarHistorial(HistorialConsumo historial) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            em.persist(historial);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void crearHistorialConsumo(HistorialConsumo historial) {
        guardarHistorial(historial);
    }

    public List<HistorialConsumo> obtenerHistorialPorMascota(Integer idMascota) {
        EntityManager em = getEntityManager();
        List<HistorialConsumo> historial;

        try {
            historial = em.createQuery("SELECT h FROM HistorialConsumo h WHERE h.mascota.id = :idMascota", HistorialConsumo.class)
                    .setParameter("idMascota", idMascota)
                    .getResultList();
        } finally {
            em.close();
        }

        return historial;
    }

    public List<HistorialConsumo> obtenerHistorialPorMascotaYTipo(Integer idMascota, String tipoServicio) {
        EntityManager em = getEntityManager();
        List<HistorialConsumo> historial;

        try {
            historial = em.createQuery("SELECT h FROM HistorialConsumo h WHERE h.mascota.id = :idMascota AND h.tipoServicio = :tipoServicio", HistorialConsumo.class)
                    .setParameter("idMascota", idMascota)
                    .setParameter("tipoServicio", tipoServicio)
                    .getResultList();
        } finally {
            em.close();
        }

        return historial;
    }
}
