package veterinaria.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PasswordSecurityUtilTest {

    @Test
    void debeHashearYValidarConPbkdf2() {
        String hash = PasswordSecurityUtil.hashPassword("ClaveSegura123");

        assertNotNull(hash);
        assertTrue(hash.startsWith("PBKDF2$"));
        assertTrue(PasswordSecurityUtil.matches("ClaveSegura123", hash));
        assertFalse(PasswordSecurityUtil.matches("otra-clave", hash));
    }

    @Test
    void normalizeForPersistDebeMigrarTextoPlanoAPbkdf2() {
        String persisted = PasswordSecurityUtil.normalizeForPersist("admin");

        assertTrue(persisted.startsWith("PBKDF2$"));
        assertTrue(PasswordSecurityUtil.matches("admin", persisted));
        assertFalse(PasswordSecurityUtil.needsMigration(persisted));
    }

    @Test
    void debeMantenerHashFuerteSinRehashear() {
        String hash = PasswordSecurityUtil.hashPassword("MiClave");

        assertEquals(hash, PasswordSecurityUtil.normalizeForPersist(hash));
    }

    @Test
    void debeAceptarCompatibilidadConTextoPlanoLegacy() {
        assertTrue(PasswordSecurityUtil.matches("admin", "admin"));
        assertTrue(PasswordSecurityUtil.needsMigration("admin"));
    }

    @Test
    void debeGenerarCodigoRecuperacionConCantidadDeDigitosPedida() {
        String codigo = PasswordSecurityUtil.generateRecoveryCode(6);

        assertEquals(6, codigo.length());
        assertTrue(codigo.matches("\\d{6}"));
    }
}
