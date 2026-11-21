package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import veterinaria.entidad.Visita;

/** Contrato mínimo de continuidad clínica. No requiere MySQL. */
class IntegracionHibridaContinuidadTest {
    @Test
    void conservaCamposDelCircuitoClinicoOriginal() throws NoSuchFieldException {
        for (String nombre : new String[]{
                "pesoConsulta", "especieConsulta", "razaConsulta",
                "idConsultaOrigen", "proximoControlRecomendado",
                "prioridadAtencion", "atencionProvisional", "idSolicitudReactivacion"}) {
            Field campo = Visita.class.getDeclaredField(nombre);
            assertNotNull(campo, "El campo " + nombre + " debe conservarse.");
        }
    }
}
