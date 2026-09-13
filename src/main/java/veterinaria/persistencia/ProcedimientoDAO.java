
package veterinaria.persistencia;

import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.Procedimiento;

public class ProcedimientoDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void crear(Procedimiento procedimiento) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.persist(procedimiento);
        em.getTransaction().commit();
        em.close();
    }

    public Procedimiento buscarPorId(Integer iProcedimiento) {
        EntityManager em = getEntityManager();
        Procedimiento procedimiento = em.find(Procedimiento.class, iProcedimiento);
        em.close();
        return procedimiento;
    }

    public List<Procedimiento> obtenerTodas() {
        EntityManager em = getEntityManager();
        List<Procedimiento> procedimiento = em.createQuery("SELECT p FROM Procedimiento p", Procedimiento.class).getResultList();
        em.close();
        return procedimiento;
    }

    public void actualizar(Procedimiento procedimiento) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.merge(procedimiento);
        em.getTransaction().commit();
        em.close();
    }

    public void eliminar(Integer idProcedimiento) {
        EntityManager em = getEntityManager();
        Procedimiento procedimiento = em.find(Procedimiento.class, idProcedimiento);
        if (procedimiento != null) {
            em.getTransaction().begin();
            em.remove(procedimiento);
            em.getTransaction().commit();
        }
        em.close();
    }

    public boolean eliminar(Procedimiento hospitalizacion) throws Exception {
        EntityManager em = getEntityManager();
        boolean state = false;
        try {
            em.getTransaction().begin();
            Procedimiento procedimientoManaged = em.find(Procedimiento.class, hospitalizacion.getIdProcedimiento());
            if (procedimientoManaged != null) {
                String estado = procedimientoManaged.getEstado();
                if ("Dado de Alta".equals(estado)) {
                    em.remove(procedimientoManaged);
                    em.getTransaction().commit();
                    state = true;
                } else {
                    em.getTransaction().rollback();

                }
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return state;
    }

    

    public List<Procedimiento> obtenerPorHospitalizacion(Integer idHospitalizacion) {
        EntityManager em = getEntityManager();
        List<Procedimiento> lista = em.createQuery(
                "SELECT p FROM Procedimiento p WHERE p.hospitalizacion.idHospitalizacion = :idHosp",
                Procedimiento.class)
                .setParameter("idHosp", idHospitalizacion)
                .getResultList();
        em.close();
        return lista;
    }

}