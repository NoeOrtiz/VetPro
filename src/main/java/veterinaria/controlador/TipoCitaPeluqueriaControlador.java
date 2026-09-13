package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.TipoCitaPeluqueria;
import veterinaria.persistencia.TipoCitaPeluqueriaDAO;

public class TipoCitaPeluqueriaControlador {

    private final TipoCitaPeluqueriaDAO dao = new TipoCitaPeluqueriaDAO();

    public boolean crear(TipoCitaPeluqueria tipo) {
        try {
            return dao.crear(tipo);
        } catch (Exception ex) {
            Logger.getLogger(TipoCitaPeluqueriaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean actualizar(TipoCitaPeluqueria tipo) {
        try {
            return dao.actualizar(tipo);
        } catch (Exception ex) {
            Logger.getLogger(TipoCitaPeluqueriaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean eliminarLogico(Integer id) {
        try {
            return dao.eliminarLogico(id);
        } catch (Exception ex) {
            Logger.getLogger(TipoCitaPeluqueriaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public List<TipoCitaPeluqueria> buscarActivos() {
        return dao.buscarActivos();
    }

    public List<TipoCitaPeluqueria> buscarTodos() {
        return dao.buscarTodos();
    }

    public TipoCitaPeluqueria buscarPorDescripcion(String d) {
        return dao.buscarPorDescripcion(d);
    }

    public TipoCitaPeluqueria buscarPorId(Integer id) {
        return dao.buscarPorId(id);
    }
}
