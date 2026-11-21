package veterinaria.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BootstrapSecurityTest {
    @Test
    void prohíbeClavesPredeterminadasYDebiles() {
        assertFalse(BootstrapSecurity.esContrasenaSegura(null));
        assertFalse(BootstrapSecurity.esContrasenaSegura("admin"));
        assertFalse(BootstrapSecurity.esContrasenaSegura("admin/admin"));
        assertFalse(BootstrapSecurity.esContrasenaSegura("1234567890123456"));
        assertFalse(BootstrapSecurity.esContrasenaSegura("SoloMinusculas12345"));
        assertFalse(BootstrapSecurity.esContrasenaSegura("Clave Sin Espacios1!"));
    }

    @Test
    void exigeCorreoRealParaCambioInicial() {
        assertFalse(BootstrapSecurity.esEmailValido(null));
        assertFalse(BootstrapSecurity.esEmailValido("admin@local"));
        assertFalse(BootstrapSecurity.esEmailValido("correo_invalido"));
        assertTrue(BootstrapSecurity.esEmailValido("admin@veterinaria.example"));
    }

    @Test
    void aceptaClaveInicialRobusta() {
        assertTrue(BootstrapSecurity.esContrasenaSegura("Veterinaria!Seguro2026"));
    }
}
