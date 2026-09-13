package veterinaria.servicio;

public class RecuperacionClaveService {

    private final AutenticacionService autenticacionService = new AutenticacionService();
    private final AuditoriaService auditoriaService = new AuditoriaService(); // 1. Instanciamos el servicio de auditoría

    public String generarCodigoRecuperacion(String nombreUsuario, String email, String dni) {
        String resultado = autenticacionService.generarCodigoRecuperacion(nombreUsuario, email, dni);

        // 2. Evaluamos si se generó/envió con éxito o falló
        if (resultado != null && !resultado.isEmpty()) {
            auditoriaService.registrarSinSesion(
                    "PASSWORD_RESET_REQUEST",
                    "Usuario",
                    null,
                    "RecuperacionClaveService",
                    "Código de recuperación enviado a: " + email,
                    AuditoriaService.RESULT_OK,
                    null, null, null
            );
        } else {
            auditoriaService.registrarSinSesion(
                    "PASSWORD_RESET_FAIL",
                    "Usuario",
                    null,
                    "RecuperacionClaveService",
                    "Intento fallido de recuperación para usuario: " + nombreUsuario,
                    AuditoriaService.RESULT_ERROR,
                    null, null, "Datos incorrectos o error al enviar mail"
            );
        }

        return resultado;
    }

    public boolean restablecerConCodigo(String nombreUsuario, String email, String dni, String codigo, String nuevaContrasena) {
        boolean actualizado = autenticacionService.restablecerConCodigo(nombreUsuario, email, dni, codigo, nuevaContrasena);

        // Si se actualizó correctamente con código, nos aseguramos de apagar la bandera de primer login
        if (actualizado) {
            try {
                autenticacionService.actualizarPrimerLogin(nombreUsuario, false);
            } catch (Exception e) {
                // Si el método en autenticacionService ya lo maneja por dentro, puedes omitir este bloque o dejarlo controlado
            }
        }

        // 3. Registramos el resultado del cambio de contraseña
        if (actualizado) {
            auditoriaService.registrarSinSesion(
                    "PASSWORD_RESET",
                    "Usuario",
                    null,
                    "RecuperacionClaveService",
                    "Contraseña restablecida exitosamente para el usuario: " + nombreUsuario,
                    AuditoriaService.RESULT_OK,
                    null, null, null
            );
        } else {
            auditoriaService.registrarSinSesion(
                    "PASSWORD_RESET_FAIL",
                    "Usuario",
                    null,
                    "RecuperacionClaveService",
                    "Fallo al actualizar contraseña con código para: " + nombreUsuario,
                    AuditoriaService.RESULT_ERROR,
                    null, null, "Código inválido o datos incorrectos"
            );
        }

        return actualizado;
    }
}
