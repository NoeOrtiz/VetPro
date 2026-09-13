
package veterinaria.controlador;

import java.util.List;
import java.time.LocalDate;
import javax.swing.JOptionPane;
import veterinaria.entidad.EstadoPeluqueriaEnum;
import veterinaria.entidad.Peluqueria;
import veterinaria.persistencia.PeluqueriaDAO;
import veterinaria.servicio.PeluqueriaService;
import veterinaria.util.Constantes.ResultadoEliminarPeluqueria;

public class PeluqueriaControlador {

    private final PeluqueriaDAO turnoDAO = new PeluqueriaDAO();
    private final PeluqueriaService turnoService = new PeluqueriaService();
    private final HistoriaEventoControlador historiaEventoControlador = new HistoriaEventoControlador();

    public boolean crearTurnoMascota(Peluqueria turno) {

        if (turno == null || turno.getSlot() == null || turno.getSlot().getIdSlot() == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "Debe seleccionar un horario disponible.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        try {
            boolean ok = turnoService.reservarTurno(
                    turno,
                    turno.getSlot().getIdSlot()
            );

            if (ok) {
                try {
                    historiaEventoControlador.registrarDesdePeluqueria(turno);
                } catch (Exception ignore) {
                }
            }

            if (!ok) {
                JOptionPane.showMessageDialog(
                        null,
                        "El horario seleccionado ya fue tomado.\nPor favor, recargue y elija otro.",
                        "Horario no disponible",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            return ok;

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                String msg = e.getMessage();
                if (msg != null && !msg.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(
                            null,
                            msg,
                            "Validación",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return false;
                }
            }

            Throwable c = e;
            while (c.getCause() != null) {
                c = c.getCause();
            }
            String root = c.getMessage();

            String mensaje
                    = "No se pudo guardar el turno.\n"
                    + (root != null ? "Detalle: " + root + "\n" : "")
                    + "Recargue la grilla y seleccione otro horario.";

            JOptionPane.showMessageDialog(
                    null,
                    mensaje,
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;
        }
    }

    public List<Peluqueria> obtenerTodosLosTurnos() {
        return turnoDAO.buscarVisibles();
    }

    public List<Peluqueria> obtenerTodosLosTurnosSinFiltro() {
        return turnoDAO.buscarTodos();
    }

    public List<Peluqueria> obtenerTurnosPorFechaYEstados(LocalDate fecha, List<EstadoPeluqueriaEnum> estados) {
        return turnoDAO.buscarPorFechaYEstados(fecha, estados);
    }

    public List<Peluqueria> obtenerTurnosPorEstados(List<EstadoPeluqueriaEnum> estados) {
        return turnoDAO.buscarPorEstados(estados);
    }

    public Peluqueria buscarTurnoPorId(Long idTurno) {
        return turnoDAO.buscarPorId(idTurno);
    }

    public void actualizarTurno(Peluqueria turno) {
        if (turno == null || turno.getSlot() == null || turno.getSlot().getIdSlot() == null) {
            JOptionPane.showMessageDialog(null, "Debe seleccionar un horario disponible.");
            return;
        }
        boolean ok = turnoService.actualizarTurnoReasignandoSlot(turno, turno.getSlot().getIdSlot());
        if (!ok) {
            JOptionPane.showMessageDialog(null, "No se pudo actualizar: el horario ya fue tomado.");
        }
    }

    public boolean cancelarTurno(Long idTurno, String motivo, Integer idUsuario) {
        if (idTurno == null) return false;
        Peluqueria db = turnoDAO.buscarPorId(idTurno);
        if (db == null) return false;
        return turnoService.cancelarTurno(db.getIdTurno(), motivo, idUsuario);
    }

    public ResultadoEliminarPeluqueria eliminarTurno(Long idTurno, String motivo, Integer idUsuario) {
        try {
            Peluqueria db = turnoDAO.buscarPorId(idTurno);
            if (db == null) {
                return ResultadoEliminarPeluqueria.NO_EXISTE;
            }

            EstadoPeluqueriaEnum estado = db.getEstado();
            if (estado == EstadoPeluqueriaEnum.COMPLETADO
                    || estado == EstadoPeluqueriaEnum.CANCELADO
                    || estado == EstadoPeluqueriaEnum.ELIMINADO) {
                return ResultadoEliminarPeluqueria.ESTADO_NO_PERMITIDO;
            }

            boolean ok = turnoService.eliminarTurno(db.getIdTurno(), motivo, idUsuario);
            return ok ? ResultadoEliminarPeluqueria.OK : ResultadoEliminarPeluqueria.ERROR;
        } catch (Exception e) {
            return ResultadoEliminarPeluqueria.ERROR;
        }
    }

    public boolean confirmarTurno(Long idTurno) {
        if (idTurno == null) return false;
        Peluqueria db = turnoDAO.buscarPorId(idTurno);
        if (db == null) return false;
        return turnoService.confirmarTurno(db.getIdTurno());
    }

    public boolean cancelarTurnoSinHistorial(Long idTurno) {
        if (idTurno == null) return false;
        Peluqueria db = turnoDAO.buscarPorId(idTurno);
        if (db == null) return false;
        return turnoService.cancelarTurnoSinHistorial(db.getIdTurno());
    }

    public boolean completarTurno(Long idTurno, Integer idUsuario) {
        if (idTurno == null) return false;
        Peluqueria db = turnoDAO.buscarPorId(idTurno);
        if (db == null) return false;
        return turnoService.completarTurno(db.getIdTurno(), idUsuario);
    }
}
