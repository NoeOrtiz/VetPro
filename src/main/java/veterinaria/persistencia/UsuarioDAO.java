
package veterinaria.persistencia;

import java.util.List;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Usuario;
import veterinaria.servicio.UsuarioService;
import veterinaria.servicio.AutenticacionService;

public class UsuarioDAO {
    private final AutenticacionService autenticacionService = new AutenticacionService();

    public EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

    public boolean crearUsuario(Persona persona, Usuario usuario) {
        return new UsuarioService().crearUsuario(persona, usuario);
    }

    public Usuario obtenerPorId(int idUsuario) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Usuario.class, idUsuario);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Usuario> obtenerTodos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u", Usuario.class);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Usuario> obtenerVeterinarios() {
        return obtenerUsuariosVeterinarios();
    }

    public List<Usuario> obtenerUsuariosVeterinarios() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT u FROM Usuario u "
                    + " JOIN FETCH u.rol r "
                    + " LEFT JOIN FETCH u.persona p "
                    + " WHERE lower(trim(r.nombreRol)) IN :roles "
                    + " ORDER BY p.apellido, p.nombre",
                    Usuario.class)
                    .setParameter("roles", Arrays.asList("veterinario", "veterinarios"))
                    .getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario", Usuario.class);
            query.setParameter("nombreUsuario", nombreUsuario);
            return query.getResultStream().findFirst().orElse(null);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public Usuario buscarPorEmail(String emailUsuario) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u WHERE u.email = :emailUsuario", Usuario.class);
            query.setParameter("emailUsuario", emailUsuario);
            return query.getResultStream().findFirst().orElse(null);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public boolean actualizarContrasenaPorRecuperacion(String nombreUsuario, String emailUsuario, String dni, String nuevaContrasena) {
        return autenticacionService.actualizarContrasenaPorRecuperacion(nombreUsuario, emailUsuario, dni, nuevaContrasena);
    }

    public boolean actualizarUsuario(Persona persona, Usuario usuario, Rol rol) {
        return new UsuarioService().actualizarUsuario(persona, usuario, rol);
    }

    public boolean eliminar(Persona persona, Usuario usuario, Rol rol){
        return new UsuarioService().eliminarUsuario(persona, usuario, rol);
    }
    
    public List<Usuario> obtenerPeluqueros() {
    EntityManager em = getEntityManager();
    try {
        TypedQuery<Usuario> query = em.createQuery(
            "SELECT u FROM Usuario u JOIN FETCH u.rol r LEFT JOIN FETCH u.persona p WHERE lower(trim(r.nombreRol)) IN ('peluquero', 'peluqueros') ORDER BY p.apellido, p.nombre", Usuario.class);
        return query.getResultList();
    } finally {
        if (em != null) {
            em.close();
        }
    }
}

    public boolean validarLogin(String nombreUsuario, String contrasena) {
        return autenticacionService.validarLogin(nombreUsuario, contrasena);
    }
}
