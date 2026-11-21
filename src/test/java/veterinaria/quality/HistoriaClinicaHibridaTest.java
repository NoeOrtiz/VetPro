package veterinaria.quality;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HistoriaClinicaHibridaTest {
    @Test
    void conservaInteraccionesOriginalesYPresentaDatosEnTarjeta() throws Exception {
        String codigo = Files.readString(Path.of(
                "src/main/java/veterinaria/vista/FormHistoriaClinica.java"));
        assertTrue(codigo.contains("new java.awt.GridLayout(3, 2, 13, 8)"));
        assertTrue(codigo.contains("campoFicha(lbDueño, txtDueño)"));
        assertTrue(codigo.contains("campoFicha(lbPeso, txtPeso)"));
        assertTrue(codigo.contains("paginacionHistoria.activarBusquedaPorPrefijo()"));
        assertTrue(codigo.contains("seleccionarFilaUnica"));
        assertTrue(codigo.contains("jPanel2.add(contenido, java.awt.BorderLayout.CENTER)"));
        assertTrue(codigo.contains("btnImprimirHistoria"));
        assertTrue(codigo.contains("btnImprimirListaHistorias"));
    }
}
