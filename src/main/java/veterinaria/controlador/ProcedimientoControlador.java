
package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.util.AppLog;
import veterinaria.entidad.Procedimiento;
import veterinaria.persistencia.ProcedimientoDAO;

public class ProcedimientoControlador {
    
    private final ProcedimientoDAO procedimientoDAO = new ProcedimientoDAO();
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();
    
    
    public void crearProcedimiento(Procedimiento procedimiento) {
        procedimientoDAO.crear(procedimiento);
        try {
            historiaEventoControlador.registrarDesdeProcedimiento(procedimiento);
        } catch (Exception ex) {
            AppLog.ignored(ProcedimientoControlador.class, "No se pudo registrar el evento de historia clínica desde procedimiento", ex);
        }
    }

    public Procedimiento buscarProcedimientoPorId(Integer idProcedimiento) {
        return procedimientoDAO.buscarPorId(idProcedimiento);
    }

    public List<Procedimiento> obtenerTodosLosProcedimientos() {
        return procedimientoDAO.obtenerTodas();
    }

    public void actualizarProcedimiento(Procedimiento procedimiento) {
        procedimientoDAO.actualizar(procedimiento);
    }

    public boolean eliminarProcedimiento(Procedimiento procedimiento) {
        try {
            String estado = procedimiento.getEstado();
            if ("Dado de Alta".equals(estado)) {
                return procedimientoDAO.eliminar(procedimiento);
            }
        } catch (Exception ex) {
            Logger.getLogger(ProcedimientoControlador.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
}
