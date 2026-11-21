package veterinaria.quality;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Evita que una mejora estética quite las validaciones de seguridad. */
class PerfilYCambioClaveEstiloTest {
    private String source(String path) throws Exception {
        return Files.readString(Path.of("src/main/java/veterinaria", path));
    }

    @Test
    void menuEsMiPerfilYNoGestionGeneral() throws Exception {
        String s = source("vista/application/form/MainForm.java");
        assertTrue(s.contains("new javax.swing.JMenuItem(\"Mi perfil\")"));
        assertTrue(s.contains("new MiPerfilDialog(ventana, Application.getSesionUsuario())"));
        assertTrue(s.contains("new javax.swing.JMenuItem(\"Cambiar contraseña\")"));
        assertTrue(s.contains("new javax.swing.JMenuItem(\"Cerrar sesión\")"));
        assertFalse(s.contains("new javax.swing.JMenuItem(\"Gestión de usuarios\")"));
    }

    @Test
    void miPerfilSoloConsultaLaCuentaAutenticada() throws Exception {
        String s = source("vista/application/form/MiPerfilDialog.java");
        assertTrue(s.contains("sesion.getUsuario().getIdUsuario()"));
        assertTrue(s.contains("WHERE u.idUsuario = :id"));
        assertTrue(s.contains(".setParameter(\"id\", idSesion)"));
        assertTrue(s.contains("LEFT JOIN FETCH u.persona"));
        assertTrue(s.contains("LEFT JOIN FETCH u.rol"));
        assertFalse(s.contains("getContrasena()"));
        assertFalse(s.contains("setContrasena("));
    }

    @Test
    void cambioClaveMantieneIdentidadCodigoYVerificacion() throws Exception {
        String s = source("vista/application/form/RecuperarContrasenaDialog.java");
        assertTrue(s.contains("this(owner, usuarioInicial, false)"));
        assertTrue(s.contains("txtUsuario.setEditable(false)"));
        assertTrue(s.contains("generarCodigoRecuperacion(usuario, email, dni)"));
        assertTrue(s.contains("EmailUtil.enviarCodigo(email, codigoPlano)"));
        assertTrue(s.contains("restablecerConCodigo(usuario, email, dni, codigo, nuevaContrasena)"));
        assertTrue(s.contains("validaciones.validarComplejidadContrasena(nuevaContrasena)"));
        assertFalse(s.contains("formularioCentrado"));
        assertTrue(s.contains("panelPrincipal.add(panelFormulario, BorderLayout.CENTER)"));
        assertTrue(s.contains("[60!][grow,fill]"));
        assertTrue(s.contains("btnCancelar.putClientProperty(FlatClientProperties.STYLE, estiloBoton)"));
    }
}
