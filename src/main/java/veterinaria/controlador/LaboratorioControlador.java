
package veterinaria.controlador;

import java.time.LocalDate;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.util.AppLog;
import veterinaria.entidad.Laboratorio;
import veterinaria.persistencia.LaboratorioDAO;
import veterinaria.util.enums.EstadoLaboratorio;

public class LaboratorioControlador {

    private final LaboratorioDAO laboratorioDAO = new LaboratorioDAO();
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();

    public boolean crearLaboratorio(Laboratorio laboratorio) {
        boolean ok = laboratorioDAO.guardar(laboratorio);
        if (!ok) {
            return false;
        }

        try {
            if (laboratorio != null && EstadoLaboratorio.COMPLETADO == EstadoLaboratorio.fromEtiqueta(laboratorio.getEstado())) {
                historiaEventoControlador.registrarDesdeLaboratorio(laboratorio);
            }
        } catch (Exception ex) {
            AppLog.ignored(LaboratorioControlador.class, "No se pudo registrar el evento de historia clínica desde laboratorio", ex);
        }
        return true;
    }

    public boolean actualizarLaboratorio(Laboratorio laboratorio) {
        boolean ok = laboratorioDAO.actualizar(laboratorio);
        if (!ok) {
            return false;
        }

        try {
            if (laboratorio != null && EstadoLaboratorio.COMPLETADO == EstadoLaboratorio.fromEtiqueta(laboratorio.getEstado())) {
                historiaEventoControlador.registrarDesdeLaboratorio(laboratorio);
            }
        } catch (Exception ex) {
            AppLog.ignored(LaboratorioControlador.class, "No se pudo sincronizar el evento de historia clínica al actualizar laboratorio", ex);
        }
        return true;
    }

   public boolean eliminarLaboratorio(Laboratorio laboratorio) {
        try {
            String estado = laboratorio.getEstado();
            if ("Procesado".equals(estado)) {
                return laboratorioDAO.eliminar(laboratorio);
            }
        } catch (Exception ex) {
            Logger.getLogger(HospitalizacionControlador.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public Laboratorio obtenerLaboratorioPorId(Integer idLaboratorio) {
        return laboratorioDAO.buscarPorId(idLaboratorio);
    }

    public List<Laboratorio> obtenerTodosLosLaboratorios() {
        return laboratorioDAO.obtenerTodos();
    }

    public List<Laboratorio> obtenerLaboratoriosPorUsuarioGestion(String usuarioGestion) {
        return laboratorioDAO.obtenerPorUsuarioGestion(usuarioGestion);
    }

public long contarPorFechaExtraccion(LocalDate fecha) {
    return laboratorioDAO.contarPorFechaExtraccion(fecha);
}

}
