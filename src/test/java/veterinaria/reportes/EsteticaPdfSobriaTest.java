package veterinaria.reportes;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.swing.JTable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.impl.ClienteListadoPdfReporte;
import veterinaria.reportes.impl.HistoriaClinicaListadoPdfReporte;
import veterinaria.reportes.pdf.PdfStyle;

/** Pruebas de impresión PDF sin acceso a MySQL ni datos de usuarios reales. */
class EsteticaPdfSobriaTest {
    @Test
    void paletaSinNaranja() {
        assertEquals(new Color(0x17, 0x76, 0x82), PdfStyle.ACCENT);
        assertEquals(new Color(0x0E, 0x4D, 0x5B), PdfStyle.ACCENT_DARK);
        assertNotEquals(new Color(0xE8, 0x89, 0x0C), PdfStyle.ACCENT);
    }

    @Test
    void listadoClientesSeGeneraConDatosOriginales() throws Exception {
        Object[][] datos = new Object[][] {
            {false, 1, "Cliente", "Demostración", "30000001",
             "Calle de pruebas 1234 y esquina de las Flores",
             "demo.qa@example.test", "03764000000", "Veterinaria de prueba",
             "20-30000001-0", "ACTIVO", "21/11/2026"},
            {false, 2, "Segundo", "Ejemplo", "30000002",
             "Avenida extensa 999", "segunda.qa@example.test",
             "03764000001", "Comercio de prueba", "20-30000002-1",
             "INACTIVO", "20/11/2026"}
        };
        String[] columnas = {"Seleccionar","ID","Nombre","Apellido","DNI",
            "Dirección","Email","Teléfono","Razón Social","CUIT","Estado","Alta"};
        JTable table = new JTable(datos, columnas);
        ReporteRequest request = new ReporteRequest()
            .put("tabla", table).put("estadoFiltro", "TODOS").put("emisor", "QA VetPRO");
        File generated = new ClienteListadoPdfReporte().generar(request);
        validarYGuardarMuestra(generated, "clientes-demo.pdf", "LISTA DE CLIENTES");
    }

    @Test
    void historiaClinicaSeGeneraConDatosClinicosOriginales() throws Exception {
        Object[][] datos = new Object[][] {
            {false, 1, "21/11/2026 10:00", "VISITA", "Veterinaria de prueba",
             "Consulta de control", "Diagnóstico: Control; tratamiento: seguimiento."},
            {false, 2, "20/11/2026 16:20", "LABORATORIO", "QA VetPRO",
             "Análisis de laboratorio", "Resultados revisados."}
        };
        String[] columnas = {"Seleccionar","ID","Fecha","Tipo","Profesional","Resumen","Observaciones"};
        JTable table = new JTable(datos, columnas);
        ReporteRequest request = new ReporteRequest()
            .put("tabla", table).put("emisor", "QA VetPRO");
        File generated = new HistoriaClinicaListadoPdfReporte().generar(request);
        validarYGuardarMuestra(generated, "historia-clinica-demo.pdf", "ATENCIONES");
    }

    private static void validarYGuardarMuestra(File generated, String name,
            String titulo) throws Exception {
        assertTrue(generated.exists(), "El reporte PDF debe existir");
        try (PDDocument pdf = PDDocument.load(generated)) {
            assertTrue(pdf.getNumberOfPages() > 0);
            String texto = new PDFTextStripper().getText(pdf);
            assertTrue(texto.contains(titulo), "El PDF debe conservar su título y texto");
            assertTrue(texto.contains("VetPRO"), "La marca debe estar en el PDF");
        }
        Path destino = Path.of("target", "pdf-preview", name);
        Files.createDirectories(destino.getParent());
        Files.copy(generated.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(generated.toPath());
    }
}
