package veterinaria.quality;

import org.junit.jupiter.api.Test;
import veterinaria.vista.FormRecepcionarPedido;
import veterinaria.vista.FormRecibirPedido;

import static org.junit.jupiter.api.Assertions.*;

class LegacyFormCompatibilityTest {

    @Test
    void formularioLegacyDebeDelegarEnFormularioActivo() {
        assertTrue(FormRecibirPedido.class.isAssignableFrom(FormRecepcionarPedido.class));
        assertNotNull(FormRecepcionarPedido.class.getAnnotation(Deprecated.class));
    }
}
