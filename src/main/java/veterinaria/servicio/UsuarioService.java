package veterinaria.servicio;

import javax.persistence.EntityManager;

import veterinaria.entidad.Persona;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.UsuarioRol;
import veterinaria.util.JsonUtil;
import veterinaria.util.PasswordSecurityUtil;
import veterinaria.util.Validaciones;

public class UsuarioService {

    private final TxRunner txRunner = new TxRunner();
    private final PersonaService personaService = new PersonaService();
    private final AuditoriaService auditoriaService = new AuditoriaService();
    private final Validaciones validaciones = new Validaciones(); // Instanciamos el validador de seguridad

    public boolean crearUsuario(Persona persona, Usuario usuario) {
        // --- 1. Validación de complejidad estricta antes de persistir ---
        String password = usuario != null ? usuario.getContrasena() : null;
        if (password == null || !validaciones.validarComplejidadContrasena(password)) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad: mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial.");
        }
        // -------------------------------------------------------------

        // --- 2. Marcar que es su primer ingreso para forzar el cambio de clave ---
        if (usuario != null) {
            usuario.setPrimerLogin(true); // O asignarlo según el tipo de dato de tu entidad
            usuario.setActivo(true);
        }
        // ------------------------------------------------------------------------

        final Long[] idOut = new Long[1];
        final String[] datosDespues = new String[1];
        boolean ok = txRunner.runInTx(em -> {
            Persona personaManaged = personaService.persistOrAttach(em, persona);

            Rol rolManaged = attachRol(em, usuario.getRol());

            usuario.setPersona(personaManaged);
            usuario.setRol(rolManaged);
            usuario.setContrasena(PasswordSecurityUtil.normalizeForPersist(usuario.getContrasena()));
            em.persist(usuario);

            em.flush();
            idOut[0] = (usuario.getIdUsuario() != null ? Long.valueOf(usuario.getIdUsuario()) : null);
            datosDespues[0] = JsonUtil.safeToJson(usuario);

            em.persist(new UsuarioRol(usuario, rolManaged));

            return true;
        }, () -> {
            auditoriaService.registrar(
                    "CREATE",
                    "Usuario",
                    idOut[0],
                    "UsuarioService",
                    "Alta de usuario: " + (usuario != null ? usuario.getNombreUsuario() : ""),
                    AuditoriaService.RESULT_OK,
                    null,
                    datosDespues[0],
                    null
            );
        });
        return ok;
    }

    public boolean actualizarUsuario(Persona persona, Usuario usuario, Rol nuevoRol) {
        final Long id = (usuario != null && usuario.getIdUsuario() != null) ? Long.valueOf(usuario.getIdUsuario()) : null;
        final String[] antes = new String[1];
        final String[] despues = new String[1];
        boolean ok = txRunner.runInTx(em -> {
            if (usuario != null && usuario.getIdUsuario() != null) {
                Usuario prev = em.find(Usuario.class, usuario.getIdUsuario());
                if (prev != null) {
                    antes[0] = JsonUtil.safeToJson(prev);
                }
            }

            Persona personaManaged = personaService.merge(em, persona);

            Rol rolManaged = attachRol(em, nuevoRol);
            usuario.setPersona(personaManaged);
            usuario.setRol(rolManaged);
            if (usuario.getContrasena() != null && !usuario.getContrasena().trim().isEmpty()) {
                usuario.setContrasena(PasswordSecurityUtil.normalizeForPersist(usuario.getContrasena()));
            }
            Usuario usuarioManaged = em.merge(usuario);

            em.flush();
            despues[0] = JsonUtil.safeToJson(usuarioManaged);

            em.createQuery("DELETE FROM UsuarioRol ur WHERE ur.usuario.idUsuario = :id")
                    .setParameter("id", usuarioManaged.getIdUsuario())
                    .executeUpdate();
            em.persist(new UsuarioRol(usuarioManaged, rolManaged));

            return true;
        }, () -> {
            auditoriaService.registrar(
                    "UPDATE",
                    "Usuario",
                    id,
                    "UsuarioService",
                    "Actualización de usuario: " + (usuario != null ? usuario.getNombreUsuario() : ""),
                    AuditoriaService.RESULT_OK,
                    antes[0],
                    despues[0],
                    null
            );
        });
        return ok;
    }

    /**
     * Conserva la firma histórica para no romper las pantallas existentes.
     * La baja de un usuario es lógica: nunca se elimina el registro ni se
     * desvinculan sus auditorías.
     */
    public boolean eliminarUsuario(Persona persona, Usuario usuario, Rol rol) {
        return desactivarUsuario(usuario);
    }

    public boolean desactivarUsuario(Usuario usuario) {
        return cambiarEstadoActivo(usuario, false);
    }

    public boolean reactivarUsuario(Usuario usuario) {
        return cambiarEstadoActivo(usuario, true);
    }

    private boolean cambiarEstadoActivo(Usuario usuario, boolean activo) {
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException("El usuario es requerido");
        }

        final Long id = Long.valueOf(usuario.getIdUsuario());
        final String[] antes = new String[1];
        final String[] despues = new String[1];

        return txRunner.runInTx(em -> {
            Usuario managed = em.find(Usuario.class, usuario.getIdUsuario());
            if (managed == null) {
                throw new IllegalArgumentException("El usuario no existe");
            }

            antes[0] = JsonUtil.safeToJson(managed);
            managed.setActivo(activo);
            em.merge(managed);
            em.flush();
            despues[0] = JsonUtil.safeToJson(managed);
            usuario.setActivo(activo);
            return true;
        }, () -> auditoriaService.registrar(
                activo ? "REACTIVAR" : "DESACTIVAR",
                "Usuario",
                id,
                "UsuarioService",
                (activo ? "Reactivación de usuario: " : "Desactivación de usuario: ") + usuario.getNombreUsuario(),
                AuditoriaService.RESULT_OK,
                antes[0],
                despues[0],
                null
        ));
    }

    private Rol attachRol(EntityManager em, Rol rol) {
        if (rol == null || rol.getIdRol() == null) {
            throw new IllegalArgumentException("El rol del usuario es requerido (idRol)");
        }
        Rol rolManaged = em.find(Rol.class, rol.getIdRol());
        if (rolManaged == null) {
            throw new IllegalArgumentException("El rol con id " + rol.getIdRol() + " no existe");
        }
        return rolManaged;
    }
}
