
package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.util.AppLog;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.persistencia.HospitalizacionDAO;
import veterinaria.util.enums.EstadoHospitalizacion;

public class HospitalizacionControlador {

    private final HospitalizacionDAO hospitalizacionDAO = new HospitalizacionDAO();
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();

    public void crearHospitalizacion(Hospitalizacion hospitalizacion) {
        hospitalizacionDAO.crear(hospitalizacion);
        try {
            historiaEventoControlador.registrarDesdeHospitalizacion(hospitalizacion);
        } catch (Exception ex) {
            AppLog.ignored(HospitalizacionControlador.class, "No se pudo registrar el evento de historia clínica desde hospitalización", ex);
        }
    }

    public Hospitalizacion buscarHospitalizacionPorId(Integer idHospitalizacion) {
        return hospitalizacionDAO.buscarPorId(idHospitalizacion);
    }

    public List<Hospitalizacion> obtenerTodasLasHospitalizacion() {
        return hospitalizacionDAO.obtenerTodas();
    }

    public void actualizarHospitalizacion(Hospitalizacion hospitalizacion) {
        hospitalizacionDAO.actualizar(hospitalizacion);
    }

    public boolean eliminarHospitalizacion(Hospitalizacion hospitalizacion) {
        try {
            String estado = hospitalizacion.getEstado();
            if (EstadoHospitalizacion.ALTA.getEtiqueta().equalsIgnoreCase(estado)) {
                return hospitalizacionDAO.eliminar(hospitalizacion);
            }
        } catch (Exception ex) {
            Logger.getLogger(HospitalizacionControlador.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public Hospitalizacion buscarHospitalizacionActivaPorMascota(Integer idMascota) {
        return hospitalizacionDAO.obtenerActivaPorMascota(idMascota);
    }

    public boolean existeHospitalizacionInternadoPorMascota(Integer idMascota) {
        return hospitalizacionDAO.existeInternadoPorMascota(idMascota);
    }

}
