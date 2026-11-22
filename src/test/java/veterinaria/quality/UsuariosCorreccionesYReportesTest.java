package veterinaria.quality;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import veterinaria.entidad.Persona;
import veterinaria.entidad.Usuario;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.impl.UsuarioListadoPdfReporte;
import veterinaria.reportes.impl.UsuarioRegistroPdfReporte;
import veterinaria.reportes.pdf.PdfStyle;
import veterinaria.util.GestorTablas;
import veterinaria.util.ui.EstiloHibrido;

/** No necesita MySQL y no utiliza datos reales. */
class UsuariosCorreccionesYReportesTest {

    private static String fuente(String path) throws Exception {
        return Files.readString(Path.of("src/main/java/veterinaria", path));
    }

    @Test
    void laBarraAlineaNuevoUsuarioEImprimirListado() throws Exception {
        String code = fuente("vista/FormUsuario.java");
        assertTrue(code.contains("crearBarraUsuariosAlineada("));
        assertTrue(code.contains("filas.add(acciones[i], celda)"));
        assertTrue(code.contains("filas.add(cancelarSeleccion, limpiarCelda)"));
        assertFalse(code.contains("acciones.add(btnCancelarSeleccion);"));
        assertTrue(code.contains("solicitarFormatoCombo("));
        assertTrue(code.contains("Configurar informe de usuario"));
    }

    @Test
    void editorCompactoMantieneLosCamposYAccionesOriginales() throws Exception {
        String code = fuente("vista/FormUsuario.java");
        assertTrue(code.contains("[52!][52!][52!]"));
        assertTrue(code.contains("new java.awt.Dimension(780, 236)"));
        assertTrue(code.contains("new java.awt.Dimension(780, 174)"));
        for (String campo : new String[]{"txtNombre", "txtApellido", "txtDNI",
                "txtTelefono", "txtDireccion", "txtEmail",
                "jcbRol", "txtUsuario", "txtContraseña"}) {
            assertTrue(code.contains("campoUsuario(") && code.contains(campo),
                    "No se debe eliminar el dato " + campo);
        }
        assertTrue(code.contains("btnGuardarActionPerformed"));
        assertTrue(code.contains("btnCancelarActionPerformed"));
        assertTrue(code.contains("btnLimpiarActionPerformed"));
        assertTrue(code.contains("procesarYGuardarImagenPerfil()"));
    }

    @Test
    void degradadoLlegaHastaEstadoConCambioInmediato() throws Exception {
        FlatLaf.registerCustomDefaultsSource("veterinaria.theme");
        FlatMacDarkLaf.setup();
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel modelo = new DefaultTableModel(
                    new Object[]{"Seleccionar", "Usuario", "Estado"},0) {
                @Override public Class<?> getColumnClass(int col) {
                    return col == 0 ? Boolean.class : String.class;
                }
                @Override public boolean isCellEditable(int row, int col) {
                    return col == 0;
                }
            };
            modelo.addRow(new Object[]{false, "qausuario", "Activo"});
            JTable table = new JTable(modelo);
            table.putClientProperty("vetpro.gradienteCompanero", true);
            table.setSize(620, 62);
            table.doLayout();
            EstiloHibrido.tablaClientes(table, 2);
            AtomicInteger notificaciones = new AtomicInteger();
            table.putClientProperty("vetpro.seleccion.rapida", Boolean.TRUE);
            GestorTablas.instalarSeleccion(table, notificaciones::incrementAndGet);
            modelo.setValueAt(true, 0, 0);
            assertTrue(notificaciones.get() > 0,
                    "La selección debe actualizarse sin otro invokeLater");
            TableCellRenderer renderer = table.getColumnModel().getColumn(2).getCellRenderer();
            Component estado = renderer.getTableCellRendererComponent(
                    table, "Activo", false, false, 0, 2);
            int w = table.getCellRect(0,2,true).width;
            int h = table.getRowHeight();
            estado.setSize(w, h);
            BufferedImage seleccionado = new BufferedImage(
                    w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = seleccionado.createGraphics();
            estado.paint(g);
            g.dispose();
            int rgb = seleccionado.getRGB(w - 3, 4);
            int red = (rgb >> 16) & 0xff;
            int blue = rgb & 0xff;
            assertTrue(blue > red + 25,
                    "El azul del degradado debe llegar a la columna Estado");
            modelo.setValueAt(false, 0, 0);
            assertTrue(notificaciones.get() >= 2);
        });
    }

    @Test
    void registroPdfEsProfesionalYSinNaranja() throws Exception {
        Persona p = new Persona();
        p.setNombre("Carolina");
        p.setApellido("Ejemplo");
        p.setDni("30000123");
        p.setTelefono("03764000000");
        p.setDireccion("Avenida de prueba 1500");
        Usuario u = new Usuario();
        u.setNombreUsuario("qa.usuario");
        u.setEmail("ejemplo.usuarios@demo.invalid");
        u.setActivo(true);
        ReporteRequest request = new ReporteRequest()
                .put("usuario",u).put("persona",p)
                .put("rol","Veterinario").put("estado","Activo")
                .put("emisor","Control de calidad");
        File archivo = new UsuarioRegistroPdfReporte().generar(request);
        try {
            try (PDDocument doc = PDDocument.load(archivo)) {
                String contenido = new PDFTextStripper().getText(doc);
                for (String valor : new String[]{"REGISTRO DE USUARIO",
                        "DATOS PERSONALES", "Carolina Ejemplo", "30000123",
                        "Avenida de prueba 1500", "CUENTA DE ACCESO",
                        "qa.usuario", "ejemplo.usuarios@demo.invalid",
                        "Veterinario", "Activo"}) {
                    assertTrue(contenido.contains(valor), "Falta dato: " + valor);
                }
                assertTrue(doc.getPage(0).getMediaBox().getHeight()
                        > doc.getPage(0).getMediaBox().getWidth());
            }
            guardarVista(archivo, "usuarios-ficha-sobria-demo.pdf");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
        assertNotEquals(new java.awt.Color(232, 137, 12), PdfStyle.ACCENT);
    }

    @Test
    void listadoPdfConservaEstadoYPaginaHorizontal() throws Exception {
        Object[][] datos = {
            {false, "Carolina", "Ejemplo", "30000123", "03764000000",
                "Avenida de prueba 1500", "Veterinario", "qa.usuario",
                "ejemplo.usuarios@demo.invalid", "Activo"},
            {false, "Fernando", "Demostración", "30000124", "03764000001",
                "Calle ficticia 200", "Asistente", "qa.fernando",
                "fernando.archivo.largo@demo.invalid", "Inactivo"}
        };
        String[] columnas = {"Seleccionar", "Nombre", "Apellido", "DNI",
            "Teléfono", "Dirección", "Rol", "Usuario", "E-mail", "Estado"};
        ReporteRequest request = new ReporteRequest()
                .put("tabla",new JTable(datos,columnas))
                .put("estadoFiltro","TODOS")
                .put("emisor","Control de calidad");
        File archivo = new UsuarioListadoPdfReporte().generar(request);
        try {
            try (PDDocument doc = PDDocument.load(archivo)) {
                String contenido = new PDFTextStripper().getText(doc);
                for(String valor : new String[]{"LISTA DE USUARIOS",
                        "Carolina", "Fernando", "qa.usuario",
                        "qa.fernando", "Activo", "Inactivo", "ESTADO"}) {
                    assertTrue(contenido.contains(valor), "Falta: " + valor);
                }
                assertTrue(doc.getPage(0).getMediaBox().getWidth()
                        > doc.getPage(0).getMediaBox().getHeight());
            }
            guardarVista(archivo, "usuarios-listado-sobrio-demo.pdf");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    private static void guardarVista(File generado, String nombre) throws Exception {
        Path destino = Path.of("target", "pdf-preview", nombre);
        Files.createDirectories(destino.getParent());
        Files.copy(generado.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
    }
}
