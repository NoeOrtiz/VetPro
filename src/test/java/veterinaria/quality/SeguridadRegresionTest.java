package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protección estática adicional frente a la reintroducción de vulnerabilidades. */
class SeguridadRegresionTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src/main/java/veterinaria", relative));
    }

    @Test
    void noPermiteVolverAUsarAdminAdmin() throws Exception {
        String auth = source("servicio/AutenticacionService.java");
        assertTrue(auth.contains("BootstrapSecurity.passwordConfigurada()"));
        assertFalse(auth.contains("normalizeForPersist(\"admin\")"));
        assertFalse(auth.contains("\"admin\".equals(contrasena)"));
    }

    @Test
    void noPermiteRestablecerClaveSinCodigo() throws Exception {
        String auth = source("servicio/AutenticacionService.java");
        assertTrue(auth.contains("El cambio obligatorio requiere código de verificación"));
        assertTrue(auth.contains("Utilice restablecerConCodigo"));
        assertTrue(auth.contains("La marca de primer ingreso no puede actualizarse sin autorización"));
        String tokens = source("persistencia/RecuperacionClaveTokenDAO.java");
        assertTrue(tokens.contains("usuarioManaged.setPrimerLogin(false)"));
    }

    @Test
    void protegeTresIntentosYCambioEnPrimerIngreso() throws Exception {
        String auth = source("servicio/AutenticacionService.java");
        assertTrue(auth.contains("MAX_INTENTOS_LOGIN = 3"));
        assertTrue(auth.contains("intentosActuales >= MAX_INTENTOS_LOGIN"));
        assertTrue(auth.contains("usuario.setPrimerLogin(true)"));
        assertTrue(auth.contains("bootstrapCrearPrimerAdmin(em, passwordInicial, emailInicial)"));
        String login = source("vista/application/form/FormLogin.java");
        assertTrue(login.contains("usuarioLogueado.isPrimerLogin()"));
        assertTrue(login.contains("dialog.isContraseñaCambiada()"));
        assertTrue(login.contains("cerrarSesion()"));
        String code = source("persistencia/RecuperacionClaveTokenDAO.java");
        assertTrue(code.contains("LockModeType.PESSIMISTIC_WRITE"));
        assertTrue(code.contains("PasswordSecurityUtil.matches("));
    }

    @Test
    void noDependeDeJasperReports() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertFalse(pom.contains("<artifactId>jasperreports</artifactId>"));
    }

    @Test
    void backupsNoContienenClavesNiShell() throws Exception {
        String backup = source("util/BackupUtil.java");
        assertTrue(backup.contains("PersistenceConfig.getDbUrl()"));
        assertTrue(backup.contains("PersistenceConfig.getDbUser()"));
        assertFalse(backup.contains("cmd.exe"));
        assertFalse(backup.contains(" -p%s"));
        assertTrue(backup.contains("procesoBuilder.environment().put(\"MYSQL_PWD\", password)"));
    }
}
