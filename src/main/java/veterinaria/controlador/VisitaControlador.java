package veterinaria.controlador;

import veterinaria.entidad.Visita;
import veterinaria.persistencia.VisitaDAO;
import veterinaria.util.Constantes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import veterinaria.util.AppLog;

public class VisitaControlador {

    private final VisitaDAO visitaDAO = new VisitaDAO();
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();

    public void crearVisita(Visita visita) {
        visitaDAO.crear(visita);

        try {
            if (visita != null
                    && visita.getEstado() != null
                    && Constantes.EstadoVisita.FINALIZADO.name()
                            .equalsIgnoreCase(visita.getEstado().trim())) {
                historiaEventoControlador.registrarDesdeVisita(visita);
            }
        } catch (Exception ex) {
            AppLog.ignored(VisitaControlador.class, "No se pudo registrar el evento de historia clínica desde visita", ex);
        }
    }

    public Visita buscarVisitaPorId(Integer idVisita) {
        return visitaDAO.buscarPorId(idVisita);
    }

    public List<Visita> obtenerTodasLasVisitas() {
        return visitaDAO.obtenerTodas();
    }

    public List<Visita> obtenerVisitasActivas() {
        return visitaDAO.obtenerVisitasActivas();
    }

    public void actualizarVisita(Visita visita) {
        visitaDAO.actualizar(visita);

        try {
            if (visita != null
                    && visita.getEstado() != null
                    && Constantes.EstadoVisita.FINALIZADO.name()
                            .equalsIgnoreCase(visita.getEstado().trim())) {
                historiaEventoControlador.registrarDesdeVisita(visita);
            }
        } catch (Exception ex) {
            AppLog.ignored(VisitaControlador.class, "No se pudo sincronizar el evento de historia clínica al actualizar visita", ex);
        }
    }

    public boolean eliminarVisita(Visita visita) {
        return cancelarVisita(visita);
    }

    public boolean cancelarVisita(Visita visita) {
        try {
            return visitaDAO.cancelar(visita);
        } catch (Exception ex) {
            Logger.getLogger(VisitaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

}
