
package veterinaria.persistencia;

import veterinaria.entidad.Permiso;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.NoResultException;
import java.util.List;
import veterinaria.entidad.Rol;
import veterinaria.entidad.RolPermiso;
import veterinaria.entidad.RolPermisoId;

public class PermisoDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void crearPermiso(Permiso permiso) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(permiso);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<Permiso> obtenerPermisos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Permiso p", Permiso.class).getResultList();
        } finally {
            em.close();
        }
    }

    public void actualizarPermiso(Permiso permiso) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(permiso);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void eliminarPermiso(int idPermiso) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Permiso permiso = em.find(Permiso.class, idPermiso);
            if (permiso != null) {
                em.remove(permiso);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
    
    public List<RolPermiso> obtenerPermisosPorRol(Rol rol) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT rp FROM RolPermiso rp JOIN FETCH rp.rol r WHERE r.idRol = :idRol", RolPermiso.class)
                    .setParameter("idRol", rol.getIdRol())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Object[]> obtenerPermisosConAccesoPorRolYTipo(int idRol, String tipoPermiso) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Object[]> q = em.createQuery(
                "SELECT p.idPermiso, p.nombrePermiso, p.descPermiso, p.tipoPermiso, "
                + "COALESCE(rp.acceso, false) "
                + "FROM Permiso p "
                + "LEFT JOIN RolPermiso rp ON rp.permiso.idPermiso = p.idPermiso AND rp.rol.idRol = :idRol "
                + "WHERE (:tipo IS NULL OR :tipo = '' OR p.tipoPermiso = :tipo) "
                + "ORDER BY p.nombrePermiso",
                Object[].class
            );
            q.setParameter("idRol", idRol);
            q.setParameter("tipo", tipoPermiso);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public void setAccesoRolPermiso(int idRol, int idPermiso, boolean acceso) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            RolPermiso rp;
            try {
                rp = em.createQuery(
                        "SELECT rp FROM RolPermiso rp WHERE rp.rol.idRol = :idRol AND rp.permiso.idPermiso = :idPermiso",
                        RolPermiso.class)
                    .setParameter("idRol", idRol)
                    .setParameter("idPermiso", idPermiso)
                    .getSingleResult();
            } catch (NoResultException ex) {
                rp = null;
            }

            if (rp == null) {
                Rol rol = em.find(Rol.class, idRol);
                Permiso permiso = em.find(Permiso.class, idPermiso);
                if (rol == null || permiso == null) {
                    throw new IllegalArgumentException("Rol o Permiso inexistente (idRol=" + idRol + ", idPermiso=" + idPermiso + ")");
                }
                rp = new RolPermiso();
                RolPermisoId rid = new RolPermisoId(idRol, idPermiso);
                try {
                    java.lang.reflect.Field f = RolPermiso.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(rp, rid);
                } catch (Exception re) {
                    throw new RuntimeException("No se pudo asignar RolPermisoId", re);
                }
                rp.setRol(rol);
                rp.setPermiso(permiso);
                rp.setAcceso(acceso);
                em.persist(rp);
            } else {
                rp.setAcceso(acceso);
                em.merge(rp);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public int crearPermisoSiNoExisteYAsignar(int idRol, String nombrePermiso, String descPermiso, String tipoPermiso, boolean acceso) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Permiso permiso;
            try {
                permiso = em.createQuery("SELECT p FROM Permiso p WHERE p.nombrePermiso = :nombre", Permiso.class)
                        .setParameter("nombre", nombrePermiso)
                        .getSingleResult();
            } catch (NoResultException ex) {
                permiso = null;
            }

            if (permiso == null) {
                permiso = new Permiso();
                permiso.setNombrePermiso(nombrePermiso);
                permiso.setDescPermiso(descPermiso);
                permiso.setTipoPermiso(tipoPermiso);
                em.persist(permiso);
                em.flush();
            }

            Rol rol = em.find(Rol.class, idRol);
            if (rol == null) {
                throw new IllegalArgumentException("Rol inexistente (idRol=" + idRol + ")");
            }

            RolPermiso rp;
            try {
                rp = em.createQuery(
                        "SELECT rp FROM RolPermiso rp WHERE rp.rol.idRol = :idRol AND rp.permiso.idPermiso = :idPermiso",
                        RolPermiso.class)
                    .setParameter("idRol", idRol)
                    .setParameter("idPermiso", permiso.getIdPermiso())
                    .getSingleResult();
            } catch (NoResultException ex) {
                rp = null;
            }

            if (rp == null) {
                RolPermisoId rid = new RolPermisoId(idRol, permiso.getIdPermiso());
                rp = new RolPermiso();
                try {
                    java.lang.reflect.Field f = RolPermiso.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(rp, rid);
                } catch (Exception re) {
                    throw new RuntimeException("No se pudo asignar RolPermisoId", re);
                }
                rp.setRol(rol);
                rp.setPermiso(permiso);
                rp.setAcceso(acceso);
                em.persist(rp);
            } else {
                rp.setAcceso(acceso);
                em.merge(rp);
            }

            em.getTransaction().commit();
            return permiso.getIdPermiso();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
        

    public Permiso obtenerPermisoPorNombre(String nombrePermiso) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Permiso> q = em.createQuery(
                "SELECT p FROM Permiso p WHERE p.nombrePermiso = :n", Permiso.class);
            q.setParameter("n", nombrePermiso);
            return q.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        } finally {
            em.close();
        }
    }
}
