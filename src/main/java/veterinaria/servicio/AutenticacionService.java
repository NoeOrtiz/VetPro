package veterinaria.servicio;

import java.time.LocalDateTime;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Rol;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.UsuarioRol;
import veterinaria.persistencia.JPAUtil;
import veterinaria.persistencia.RecuperacionClaveTokenDAO;
import veterinaria.persistencia.UsuarioRolDAO;
import veterinaria.util.PasswordSecurityUtil;
import veterinaria.util.BootstrapSecurity;
import veterinaria.util.SesionUsuario;

public class AutenticacionService {

    private static final int MAX_INTENTOS_LOGIN = 3;

    private final AuditoriaService auditoriaService = new AuditoriaService();
    private final RecuperacionClaveTokenDAO recuperacionClaveTokenDAO = new RecuperacionClaveTokenDAO();

    public boolean validarLogin(String nombreUsuario, String contrasena) {
        EntityManager em = JPAUtil.getEntityManager();
        UsuarioRolDAO rolDAO = new UsuarioRolDAO();
        try {
            Long totalUsuarios = em.createQuery("SELECT COUNT(u) FROM Usuario u", Long.class)
                    .getSingleResult();

            if (totalUsuarios != null && totalUsuarios == 0L) {
                return validarBootstrapInicial(em, rolDAO, nombreUsuario, contrasena);
            }

            Usuario usuario = buscarUsuarioParaLogin(em, nombreUsuario);
            if (usuario == null) {
                registrarLoginFallido(null, nombreUsuario, "Usuario no encontrado");
                return false;
            }

            if (!usuario.isActivo()) {
                registrarLoginFallido(usuario, nombreUsuario, "Cuenta de usuario inactiva");
                throw new SecurityException("La cuenta de usuario se encuentra inactiva. Contacte a un administrador.");
            }

            LocalDateTime ahora = LocalDateTime.now();

            // 1. VALIDAR AL PRINCIPIO: ¿Está bloqueado actualmente?
            if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
                long minutosRestantes = java.time.Duration.between(ahora, usuario.getBloqueadoHasta()).toMinutes();
                minutosRestantes = Math.max(minutosRestantes, 1);

                registrarLoginFallido(usuario, nombreUsuario,
                        "Intento de login con cuenta bloqueada temporalmente. Faltan " + minutosRestantes + " minutos.");

                throw new SecurityException("Demasiados intentos fallidos. Su cuenta está bloqueada temporalmente por seguridad. Intente en " + minutosRestantes + " minutos.");
            }

            // 2. Verificar si la contraseña es correcta
            if (!PasswordSecurityUtil.matches(contrasena, usuario.getContrasena())) {

                em.getTransaction().begin();
                int intentosActuales = usuario.getIntentosFallidos() + 1;
                usuario.setIntentosFallidos(intentosActuales);

                if (intentosActuales >= MAX_INTENTOS_LOGIN) {
                    LocalDateTime tiempoBloqueo = ahora.plusMinutes(15);
                    usuario.setBloqueadoHasta(tiempoBloqueo);
                    em.merge(usuario);
                    em.getTransaction().commit();

                    registrarLoginFallido(usuario, nombreUsuario, "Cuenta bloqueada por 15 minutos tras " + MAX_INTENTOS_LOGIN + " intentos fallidos.");
                    throw new SecurityException("Demasiados intentos fallidos. Su cuenta ha sido bloqueada temporalmente por 15 minutos.");
                } else {
                    em.merge(usuario);
                    em.getTransaction().commit();

                    registrarLoginFallido(usuario, nombreUsuario, "Credenciales inválidas (Intento " + intentosActuales + "/" + MAX_INTENTOS_LOGIN + ")");
                }
                return false;
            }

            // 3. CASO ACIERTO: Reiniciar contadores de bloqueo y fallos
            if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
                em.getTransaction().begin();
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(null);
                em.merge(usuario);
                em.getTransaction().commit();
            }

            migrarHashSiCorresponde(em, usuario, contrasena);
            asegurarRol(usuario, rolDAO);

            SesionUsuario.getInstancia().iniciarSesion(usuario);
            auditoriaService.registrar(
                    "LOGIN",
                    "Usuario",
                    Long.valueOf(usuario.getIdUsuario()),
                    "FormLogin",
                    "Inicio de sesión",
                    AuditoriaService.RESULT_OK,
                    null,
                    null,
                    null
            );
            return true;
        } catch (SecurityException se) {
            throw se;
        } catch (Exception ex) {
            registrarLoginFallido(null, nombreUsuario, ex.getMessage());
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Método legacy inseguro: no permite restablecer la clave mediante
     * datos personales sin un código de un solo uso.
     */
    @Deprecated
    public boolean actualizarContrasenaPorRecuperacion(String nombreUsuario, String emailUsuario,
            String dni, String nuevaContrasena) {
        throw new SecurityException("Utilice restablecerConCodigo, con verificación de código vigente.");
    }

    public String generarCodigoCambioObligatorio(int idUsuario) {
        return recuperacionClaveTokenDAO.generarCodigoCambioObligatorio(idUsuario);
    }

    public boolean confirmarCambioObligatorio(int idUsuario, String codigo, String nuevaContrasena) {
        return recuperacionClaveTokenDAO.confirmarCambioObligatorio(idUsuario, codigo, nuevaContrasena);
    }

    /**
     * API anterior deshabilitada. La ruta segura verifica código y cuenta
     * dentro de una misma transacción en RecuperacionClaveTokenDAO.
     */
    @Deprecated
    public boolean actualizarContrasenaObligatoria(int idUsuario, String nuevaContrasena) {
        throw new SecurityException("El cambio obligatorio requiere código de verificación.");
    }

    public String generarCodigoRecuperacion(String nombreUsuario, String email, String dni) {
        return recuperacionClaveTokenDAO.generarCodigoRecuperacion(nombreUsuario, email, dni, 10);
    }

    public boolean restablecerConCodigo(String nombreUsuario, String email, String dni, String codigo, String nuevaContrasena) {
        return recuperacionClaveTokenDAO.restablecerConCodigo(nombreUsuario, email, dni, codigo, nuevaContrasena);
    }

    /** La marca primerLogin solo puede cambiarse en un flujo de recuperación validado. */
    @Deprecated
    public void actualizarPrimerLogin(String nombreUsuario, boolean valor) {
        throw new SecurityException("La marca de primer ingreso no puede actualizarse sin autorización.");
    }

    private boolean validarBootstrapInicial(EntityManager em, UsuarioRolDAO rolDAO, String nombreUsuario, String contrasena) {
        String passwordInicial = BootstrapSecurity.passwordConfigurada();
        if (passwordInicial == null) {
            throw new SecurityException("Configuración inicial requerida: establezca "
                    + BootstrapSecurity.ENV_PASSWORD
                    + " con una contraseña robusta de al menos 14 caracteres.");
        }
        if (!"admin".equalsIgnoreCase(nombreUsuario) || !passwordInicial.equals(contrasena)) {
            registrarLoginFallido(null, nombreUsuario, "Credenciales inválidas");
            return false;
        }

        String emailInicial = BootstrapSecurity.emailConfigurado();
        if (emailInicial == null) {
            throw new SecurityException("Configuración inicial requerida: establezca "
                    + BootstrapSecurity.ENV_EMAIL
                    + " con un correo real, para recibir el código de cambio obligatorio.");
        }
        Usuario admin = bootstrapCrearPrimerAdmin(em, passwordInicial, emailInicial);
        asegurarRol(admin, rolDAO);
        SesionUsuario.getInstancia().iniciarSesion(admin);
        auditoriaService.registrar(
                "LOGIN",
                "Usuario",
                Long.valueOf(admin.getIdUsuario()),
                "FormLogin",
                "Inicio de sesión (bootstrap)",
                AuditoriaService.RESULT_OK,
                null,
                null,
                null
        );
        return true;
    }

    private Usuario buscarUsuarioParaLogin(EntityManager em, String nombreUsuario) {
        try {
            return em.createQuery(
                    "SELECT u FROM Usuario u "
                    + "LEFT JOIN FETCH u.rol "
                    + "LEFT JOIN FETCH u.persona "
                    + "WHERE lower(trim(u.nombreUsuario)) = :nombreUsuario",
                    Usuario.class)
                    .setParameter("nombreUsuario", nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase())
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    private void migrarHashSiCorresponde(EntityManager em, Usuario usuario, String rawPassword) {
        if (usuario == null || usuario.getIdUsuario() == null || !PasswordSecurityUtil.needsMigration(usuario.getContrasena())) {
            return;
        }

        try {
            em.getTransaction().begin();
            Usuario managed = em.find(Usuario.class, usuario.getIdUsuario());
            if (managed != null) {
                String nuevoHash = PasswordSecurityUtil.normalizeForPersist(rawPassword);
                managed.setContrasena(nuevoHash);
                em.merge(managed);
                usuario.setContrasena(nuevoHash);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }

    private void asegurarRol(Usuario usuario, UsuarioRolDAO rolDAO) {
        if (usuario != null && usuario.getRol() == null && usuario.getIdUsuario() != null) {
            Rol rol = rolDAO.obtenerRol(usuario.getIdUsuario());
            usuario.setRol(rol);
        }
    }

    private void registrarLoginFallido(Usuario usuario, String nombreUsuario, String detalle) {
        Long entidadId = (usuario != null && usuario.getIdUsuario() != null) ? Long.valueOf(usuario.getIdUsuario()) : null;

        // Usamos el nuevo método para que guarde al usuario si existe, o null si no se encontró en la BD
        auditoriaService.registrarConUsuario(
                usuario,
                "LOGIN_FAIL",
                "Usuario",
                entidadId,
                "FormLogin",
                "Intento de inicio de sesión fallido. usuario=" + nombreUsuario,
                AuditoriaService.RESULT_ERROR,
                null,
                null,
                detalle
        );
    }

    private Usuario bootstrapCrearPrimerAdmin(EntityManager em, String passwordInicial, String emailInicial) {
        em.getTransaction().begin();
        try {
            Rol rolAdmin = em.createQuery("SELECT r FROM Rol r WHERE r.nombreRol = :nombre", Rol.class)
                    .setParameter("nombre", "Administrador")
                    .setMaxResults(1)
                    .getSingleResult();

            Persona persona = new Persona();
            persona.setNombre("Administrador");
            persona.setApellido("Inicial");
            persona.setDireccion("");
            persona.setTelefono("");
            persona.setDni("0");
            em.persist(persona);

            Usuario usuario = new Usuario();
            usuario.setPersona(persona);
            usuario.setEmail(emailInicial);
            usuario.setNombreUsuario("admin");
            usuario.setContrasena(PasswordSecurityUtil.hashPassword(passwordInicial));
            usuario.setPrimerLogin(true); // Exigir código por correo y nueva contraseña en el primer acceso.
            usuario.setRol(rolAdmin);
            usuario.setActivo(true);
            em.persist(usuario);

            UsuarioRol ur = new UsuarioRol();
            ur.setUsuario(usuario);
            ur.setRol(rolAdmin);
            em.persist(ur);

            em.getTransaction().commit();
            return usuario;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        }
    }
}
