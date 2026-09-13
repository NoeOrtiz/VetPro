package veterinaria.servicio;

import javax.persistence.EntityManager;
import veterinaria.persistencia.JPAUtil;

public class TxRunner {

    @FunctionalInterface
    public interface TxWork<T> {
        T apply(EntityManager em) throws Exception;
    }

    public <T> T runInTx(TxWork<T> work) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            T result = work.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public <T> T runInTx(TxWork<T> work, Runnable afterCommit) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            T result = work.apply(em);
            em.getTransaction().commit();
            if (afterCommit != null) {
                try {
                    afterCommit.run();
                } catch (Exception hookEx) {
                    System.err.println("[TX] afterCommit falló: " + hookEx.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void runInTxVoid(TxWork<Void> work) {
        runInTx(work);
    }
}
