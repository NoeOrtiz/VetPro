package veterinaria.controlador;

import veterinaria.entidad.Usuario;
import java.util.List;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Rol;
import veterinaria.persistencia.UsuarioDAO;
import veterinaria.servicio.AutenticacionService;
import veterinaria.util.Validaciones;

public class UsuarioControlador {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AutenticacionService autenticacionService = new AutenticacionService();
    private final Validaciones validaciones = new Validaciones(); // Instanciamos aquí adentro

    public boolean registrarUsuario(Persona datosPersona, Usuario datosUsuario, Rol rolUsuario) {
        // Validar que la contraseña cumpla con los requisitos complejos
        if (datosUsuario.getContrasena() == null || !validaciones.validarComplejidadContrasena(datosUsuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad (mínimo 8 caracteres, una mayúscula, un número y un carácter especial).");
        }

        datosUsuario.setRol(rolUsuario);
        return usuarioDAO.crearUsuario(datosPersona, datosUsuario);
    }

    public Usuario obtenerUsuario(int idUsuario) {
        return usuarioDAO.obtenerPorId(idUsuario);
    }

    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioDAO.obtenerTodos();
    }

    public Usuario buscarPorUsuario(String nombreUsuario) {
        return usuarioDAO.buscarPorNombreUsuario(nombreUsuario);
    }

    public boolean actualizarUsuario(Persona datosPersona, Usuario datosUsuario, Rol rolUsuario) {
        // Opcional: Si también quieres validar complejidad al actualizar cuando cambian la contraseña
        if (datosUsuario.getContrasena() != null && !datosUsuario.getContrasena().trim().isEmpty()) {
            if (!validaciones.validarComplejidadContrasena(datosUsuario.getContrasena())) {
                throw new IllegalArgumentException("La nueva contraseña no cumple con los requisitos de seguridad.");
            }
        }
        return usuarioDAO.actualizarUsuario(datosPersona, datosUsuario, rolUsuario);
    }

    public boolean eliminarUsuario(Persona persona, Usuario usuario, Rol rol) {
        return usuarioDAO.eliminar(persona, usuario, rol);
    }

    public Boolean iniciarSesionUsuario(String usuario, String contraseña) {
        return autenticacionService.validarLogin(usuario, contraseña);
    }

    public boolean recuperarContrasena(String nombreUsuario, String email, String dni, String nuevaContrasena) {
        return autenticacionService.actualizarContrasenaPorRecuperacion(nombreUsuario, email, dni, nuevaContrasena);
    }

    public String solicitarCodigoRecuperacion(String nombreUsuario, String email, String dni) {
        return autenticacionService.generarCodigoRecuperacion(nombreUsuario, email, dni);
    }

    public boolean restablecerConCodigo(String nombreUsuario, String email, String dni, String codigo, String nuevaContrasena) {
        return autenticacionService.restablecerConCodigo(nombreUsuario, email, dni, codigo, nuevaContrasena);
    }
}
