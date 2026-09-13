
package veterinaria.persistencia;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.UsuarioRol;
import veterinaria.entidad.UsuarioRolId;

public class UsuarioRolDAO {
    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public void asignarRolAUsuario(Usuario usuario, Rol rol) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Rol rolManaged = em.find(Rol.class, rol.getIdRol());
            if (rolManaged == null) {
                throw new IllegalArgumentException("El rol no existe en la base de datos");
            }
            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            if (usuarioManaged == null) {
                throw new IllegalArgumentException("El usuario no existe en la base de datos");
            }           
            UsuarioRol usuarioRol = new UsuarioRol(usuarioManaged, rolManaged);
            em.persist(usuarioRol);

            em.getTransaction().commit();
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
    }  
    
    public Rol obtenerRol(int idUsuario) {
        EntityManager em = getEntityManager();
        try {
            Rol rol = em.createQuery(
                "SELECT r FROM UsuarioRol ur JOIN ur.rol r WHERE ur.usuario.idUsuario = :idUsuario", Rol.class)
                .setParameter("idUsuario", idUsuario)
                .getSingleResult();
            return rol;
        } catch (NoResultException e) {
            System.out.println("No se encontró ningún rol para el usuario con ID: " + idUsuario);
            return null;
        } catch (NonUniqueResultException e) {
            System.out.println("Se encontraron múltiples roles para el usuario con ID: " + idUsuario);
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public void actualizar(Usuario usuario, Rol nuevoRol) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            if (usuarioManaged == null) {
                throw new IllegalArgumentException("El usuario no existe en la base de datos");
            }

            Rol rolActual = obtenerRol(usuario.getIdUsuario());
            if (rolActual != null) {
                UsuarioRolId usuarioRolId = new UsuarioRolId(usuarioManaged.getIdUsuario(), rolActual.getIdRol());
                UsuarioRol usuarioRol = em.find(UsuarioRol.class, usuarioRolId);
                if (usuarioRol != null) {
                    em.remove(usuarioRol);
                }
            }

            Rol nuevoRolManaged = em.find(Rol.class, nuevoRol.getIdRol());
            if (nuevoRolManaged == null) {
                throw new IllegalArgumentException("El nuevo rol no existe en la base de datos");
            }

            UsuarioRol usuarioRolNuevo = new UsuarioRol(usuarioManaged, nuevoRolManaged);
            em.persist(usuarioRolNuevo);

            em.getTransaction().commit();
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
    }
    
    public void eliminar(Usuario usuario, Rol rol) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            Rol rolManaged = em.find(Rol.class, rol.getIdRol());
            if (rolManaged == null) {
                throw new IllegalArgumentException("El rol no existe en la base de datos");
            }

            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            if (usuarioManaged == null) {
                throw new IllegalArgumentException("El usuario no existe en la base de datos");
            }

            UsuarioRolId usuarioRolId = new UsuarioRolId(usuarioManaged.getIdUsuario(), rolManaged.getIdRol());
            UsuarioRol usuarioRol = em.find(UsuarioRol.class, usuarioRolId);

            if (usuarioRol != null) {
                em.remove(usuarioRol);
                System.out.println("ROL de usuario eliminado correctamente");
            }
            em.getTransaction().commit();
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
    }
    
}
