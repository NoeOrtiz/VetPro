package veterinaria.persistencia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Rubro;
import veterinaria.servicio.AuditoriaService;
import veterinaria.util.JsonUtil;

public class RubroDAO {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crear(Rubro rubro) {
        EntityManager em = getEntityManager();
        boolean ok = false;
        try {
            em.getTransaction().begin();
            em.persist(rubro);
            em.getTransaction().commit();
            ok = true;

            auditoriaService.registrar(
                    "CREATE",
                    "Rubro",
                    (rubro != null && rubro.getIdRubro() != null ? Long.valueOf(rubro.getIdRubro()) : null),
                    "RubroDAO",
                    "Alta de rubro: " + (rubro != null ? rubro.getNombre() : ""),
                    AuditoriaService.RESULT_OK,
                    null,
                    JsonUtil.safeToJson(rubro),
                    null
            );
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("Error al crear Rubro: " + e.getMessage());
        } finally {
            if (em != null) em.close();
        }
        return ok;
    }

    public boolean actualizar(Rubro rubro) {
        EntityManager em = getEntityManager();
        boolean ok = false;
        try {
            String antes = null;
            if (rubro != null && rubro.getIdRubro() != null) {
                Rubro prev = em.find(Rubro.class, rubro.getIdRubro());
                if (prev != null) antes = JsonUtil.safeToJson(prev);
            }
            em.getTransaction().begin();
            Rubro managed = em.merge(rubro);
            em.getTransaction().commit();
            ok = true;

            auditoriaService.registrar(
                    "UPDATE",
                    "Rubro",
                    (managed != null && managed.getIdRubro() != null ? Long.valueOf(managed.getIdRubro()) : null),
                    "RubroDAO",
                    "Actualización de rubro: " + (managed != null ? managed.getNombre() : ""),
                    AuditoriaService.RESULT_OK,
                    antes,
                    JsonUtil.safeToJson(managed),
                    null
            );
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("Error al actualizar Rubro: " + e.getMessage());
        } finally {
            if (em != null) em.close();
        }
        return ok;
    }

    public Rubro buscarPorId(Integer idRubro) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Rubro.class, idRubro);
        } finally {
            if (em != null) em.close();
        }
    }

    public Rubro buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Rubro> q = em.createQuery(
                    "SELECT r FROM Rubro r WHERE LOWER(r.nombre) = LOWER(:n)", Rubro.class);
            q.setParameter("n", nombre.trim());
            List<Rubro> res = q.setMaxResults(1).getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception e) {
            System.out.println("Error al buscar Rubro por nombre: " + e.getMessage());
            return null;
        } finally {
            if (em != null) em.close();
        }
    }

    public List<Rubro> listarTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Rubro> q = em.createQuery("SELECT r FROM Rubro r ORDER BY r.nombre", Rubro.class);
            return q.getResultList();
        } catch (Exception e) {
            System.out.println("Error al listar Rubros: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            if (em != null) em.close();
        }
    }

    public List<Rubro> listarActivos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Rubro> q = em.createQuery(
                    "SELECT r FROM Rubro r WHERE r.activo = true ORDER BY r.nombre", Rubro.class);
            return q.getResultList();
        } catch (Exception e) {
            System.out.println("Error al listar Rubros activos: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            if (em != null) em.close();
        }
    }

    public List<String> listarNombresActivos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<String> q = em.createQuery(
                    "SELECT r.nombre FROM Rubro r WHERE r.activo = true ORDER BY r.nombre", String.class);
            return q.getResultList();
        } catch (Exception e) {
            System.out.println("Error al listar nombres de Rubros activos: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            if (em != null) em.close();
        }
    }

    public int sincronizarDesdeProductos() {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Query q = em.createNativeQuery(
                "INSERT IGNORE INTO rubro (nombre, stock_minimo_default, activo) "
              + "SELECT DISTINCT TRIM(p.rubro), 0, 1 "
              + "FROM producto p "
              + "WHERE p.rubro IS NOT NULL AND TRIM(p.rubro) <> ''"
            );
            int affected = q.executeUpdate();

            em.getTransaction().commit();
            return affected;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("Error al sincronizar Rubros desde Productos: " + e.getMessage());
            return 0;
        } finally {
            if (em != null) em.close();
        }
    }

    public boolean desactivar(Integer idRubro) {
        if (idRubro == null) return false;
        EntityManager em = getEntityManager();
        boolean ok = false;
        try {
            em.getTransaction().begin();
            Rubro rubro = em.find(Rubro.class, idRubro);
            if (rubro != null) {
                String antes = JsonUtil.safeToJson(rubro);
                rubro.setActivo(false);
                em.merge(rubro);
                ok = true;
                String despues = JsonUtil.safeToJson(rubro);
                em.getTransaction().commit();

                auditoriaService.registrar(
                        "UPDATE",
                        "Rubro",
                        Long.valueOf(idRubro),
                        "RubroDAO",
                        "Desactivación de rubro: " + rubro.getNombre(),
                        AuditoriaService.RESULT_OK,
                        antes,
                        despues,
                        null
                );
                return true;
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("Error al desactivar Rubro: " + e.getMessage());
        } finally {
            if (em != null) em.close();
        }
        return ok;
    }

    public Rubro upsertPorNombre(String nombre, Integer stockMinimoDefault, boolean activo) {
        if (nombre == null || nombre.trim().isEmpty()) return null;

        final String nombreTrim = nombre.trim();
        EntityManager em = getEntityManager();

        try {
            em.getTransaction().begin();

            TypedQuery<Rubro> q = em.createQuery(
                    "SELECT r FROM Rubro r WHERE LOWER(r.nombre) = LOWER(:n)", Rubro.class);
            q.setParameter("n", nombreTrim);
            List<Rubro> res = q.setMaxResults(1).getResultList();

            Rubro rubro;
            if (!res.isEmpty()) {
                rubro = res.get(0);
                String antes = JsonUtil.safeToJson(rubro);
                if (stockMinimoDefault != null) {
                    rubro.setStockMinimoDefault(Math.max(stockMinimoDefault, 0));
                }
                rubro.setActivo(activo);
                rubro = em.merge(rubro);
                em.getTransaction().commit();
                auditoriaService.registrar(
                        "UPDATE",
                        "Rubro",
                        (rubro != null && rubro.getIdRubro() != null ? Long.valueOf(rubro.getIdRubro()) : null),
                        "RubroDAO",
                        "Upsert (update) rubro: " + (rubro != null ? rubro.getNombre() : nombreTrim),
                        AuditoriaService.RESULT_OK,
                        antes,
                        JsonUtil.safeToJson(rubro),
                        null
                );
                return rubro;
            } else {
                rubro = new Rubro();
                rubro.setNombre(nombreTrim);
                rubro.setStockMinimoDefault(Math.max(stockMinimoDefault != null ? stockMinimoDefault : 0, 0));
                rubro.setActivo(activo);
                em.persist(rubro);
                em.getTransaction().commit();
                auditoriaService.registrar(
                        "CREATE",
                        "Rubro",
                        (rubro != null && rubro.getIdRubro() != null ? Long.valueOf(rubro.getIdRubro()) : null),
                        "RubroDAO",
                        "Upsert (create) rubro: " + (rubro != null ? rubro.getNombre() : nombreTrim),
                        AuditoriaService.RESULT_OK,
                        null,
                        JsonUtil.safeToJson(rubro),
                        null
                );
                return rubro;
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("Error en upsertPorNombre (Rubro): " + e.getMessage());
            return null;
        } finally {
            if (em != null) em.close();
        }
    }
}
