package veterinaria.quality;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceQualityGateTest {

    @Test
    void archivosCriticosNoDebenTenerDebugNiErroresTragados() throws IOException {
        assertArchivoLimpio("src/main/java/veterinaria/controlador/HospitalizacionControlador.java");
        assertArchivoLimpio("src/main/java/veterinaria/controlador/LaboratorioControlador.java");
        assertArchivoLimpio("src/main/java/veterinaria/controlador/ProcedimientoControlador.java");
        assertArchivoLimpio("src/main/java/veterinaria/controlador/VisitaControlador.java");
        assertArchivoLimpio("src/main/java/veterinaria/controlador/PermisoControlador.java");
        assertArchivoLimpio("src/main/java/veterinaria/util/SesionUsuario.java");
        assertArchivoLimpio("src/main/java/veterinaria/vista/application/form/MainForm.java");
        assertArchivoLimpio("src/main/java/veterinaria/vista/FormRecepcionarPedido.java");
    }

    private static void assertArchivoLimpio(String relativePath) throws IOException {
        String source = Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
        assertFalse(source.contains("System.out.println"), () -> "System.out detectado en " + relativePath);
        assertFalse(source.contains("printStackTrace()"), () -> "printStackTrace detectado en " + relativePath);
        assertFalse(source.contains("catch (Exception ignore)"), () -> "catch ignore detectado en " + relativePath);
    }
}
