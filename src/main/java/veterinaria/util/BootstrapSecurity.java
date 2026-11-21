package veterinaria.util;

/**
 * Protege el alta de la primera cuenta cuando la base todavía no tiene usuarios.
 * Nunca se permite un usuario administrador con contraseña predeterminada.
 */
public final class BootstrapSecurity {

    public static final String ENV_PASSWORD = "VETPRO_BOOTSTRAP_ADMIN_PASSWORD";

    private BootstrapSecurity() {}

    public static String passwordConfigurada() {
        String valor = System.getenv(ENV_PASSWORD);
        if (valor == null || !esContrasenaSegura(valor)) {
            return null;
        }
        return valor;
    }

    public static boolean esContrasenaSegura(String password) {
        if (password == null || password.length() < 14 || password.length() > 128) {
            return false;
        }
        boolean mayuscula = false;
        boolean minuscula = false;
        boolean digito = false;
        boolean simbolo = false;
        for (int i = 0; i < password.length(); i++) {
            char caracter = password.charAt(i);
            if (Character.isISOControl(caracter) || Character.isWhitespace(caracter)) return false;
            if (Character.isUpperCase(caracter)) mayuscula = true;
            else if (Character.isLowerCase(caracter)) minuscula = true;
            else if (Character.isDigit(caracter)) digito = true;
            else simbolo = true;
        }
        return mayuscula && minuscula && digito && simbolo
                && !password.equalsIgnoreCase("admin")
                && !password.equalsIgnoreCase("password");
    }
}
