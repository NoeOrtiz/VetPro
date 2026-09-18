
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

    /**
     * Compatibilidad temporal. Un procedimiento clínico no se elimina
     * físicamente porque debe conservar su trazabilidad histórica.
     */
    @Deprecated
    public void eliminar(Integer idProcedimiento) {
        // Intencionalmente sin borrado físico.
    }

    @Deprecated
    public boolean eliminar(Procedimiento procedimiento) {
        return false;
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