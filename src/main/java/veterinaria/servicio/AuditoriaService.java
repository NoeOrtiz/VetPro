package veterinaria.servicio;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import veterinaria.entidad.Auditoria;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.AuditoriaDAO;
import veterinaria.util.SesionUsuario;

public class AuditoriaService {

    public static final String RESULT_OK = "OK";
    public static final String RESULT_ERROR = "ERROR";

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public void registrar(String accion, String entidad, Long entidadId, String modulo,
            String descripcion, String resultado,
            String datosAntes, String datosDespues, String errorDetalle) {

        Usuario usuario = SesionUsuario.getInstancia().getUsuario();
        if (usuario == null) {
            return;
        }

        try {
            Auditoria a = new Auditoria();
            a.setFechaHora(LocalDateTime.now());
            a.setUsuario(usuario);
            a.setAccion(accion);
            a.setEntidad(entidad);
            a.setEntidadId(entidadId);
            a.setModulo(modulo);
            a.setDescripcion(descripcion);
            a.setResultado(resultado != null ? resultado : RESULT_OK);
            a.setDatosAntes(datosAntes);
            a.setDatosDespues(datosDespues);
            a.setErrorDetalle(errorDetalle);

            auditoriaDAO.guardar(a);
        } catch (Exception ex) {
            System.err.println("[AUDITORIA] No se pudo registrar evento: " + ex.getMessage());
        }
    }

    // NUEVO MÉTODO: Permite guardar una auditoría asignándole un usuario específico (ideal para logins fallidos)
    public void registrarConUsuario(Usuario usuarioEspecifico, String accion, String entidad, Long entidadId, String modulo,
            String descripcion, String resultado,
            String datosAntes, String datosDespues, String errorDetalle) {
        try {
            Auditoria a = new Auditoria();
            a.setFechaHora(LocalDateTime.now());
            a.setUsuario(usuarioEspecifico); // Si pasamos un usuario existente, se guarda acá y aparece en la tabla
            a.setAccion(accion);
            a.setEntidad(entidad);
            a.setEntidadId(entidadId);
            a.setModulo(modulo);
            a.setDescripcion(descripcion);
            a.setResultado(resultado != null ? resultado : RESULT_OK);
            a.setDatosAntes(datosAntes);
            a.setDatosDespues(datosDespues);
            a.setErrorDetalle(errorDetalle);

            auditoriaDAO.guardar(a);
        } catch (Exception ex) {
            System.err.println("[AUDITORIA] No se pudo registrar evento con usuario específico: " + ex.getMessage());
        }
    }

    public void registrarSinSesion(String accion, String entidad, Long entidadId, String modulo,
            String descripcion, String resultado,
            String datosAntes, String datosDespues, String errorDetalle) {
        try {
            Auditoria a = new Auditoria();
            a.setFechaHora(LocalDateTime.now());
            a.setUsuario(null);
            a.setAccion(accion);
            a.setEntidad(entidad);
            a.setEntidadId(entidadId);
            a.setModulo(modulo);
            a.setDescripcion(descripcion);
            a.setResultado(resultado != null ? resultado : RESULT_OK);
            a.setDatosAntes(datosAntes);
            a.setDatosDespues(datosDespues);
            a.setErrorDetalle(errorDetalle);

            auditoriaDAO.guardar(a);
        } catch (Exception ex) {
            System.err.println("[AUDITORIA] No se pudo registrar evento (sin sesión): " + ex.getMessage());
        }
    }

    public List<Auditoria> buscar(Date desde, Date hasta, Integer usuarioId, String accion, String entidad) {
        java.time.LocalDateTime desdeLDT = null;
        java.time.LocalDateTime hastaLDT = null;

        if (desde != null) {
            desdeLDT = desde.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .atStartOfDay();
        }

        if (hasta != null) {
            hastaLDT = hasta.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .atTime(23, 59, 59);
        }

        return auditoriaDAO.buscar(desdeLDT, hastaLDT, usuarioId, accion, entidad);
    }

    public Auditoria obtenerPorId(Long id) {
        return auditoriaDAO.obtenerPorId(id);
    }
}
