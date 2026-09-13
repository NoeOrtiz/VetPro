
package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.CompraOrden;
import veterinaria.persistencia.CompraOrdenDAO;

public class CompraOrdenControlador {

    private final CompraOrdenDAO dao = new CompraOrdenDAO();

    public boolean crear(CompraOrden orden) {
        try {
            return dao.crear(orden);
        } catch (Exception ex) {
            Logger.getLogger(CompraOrdenControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean actualizar(CompraOrden orden) {
        try {
            return dao.actualizar(orden);
        } catch (Exception ex) {
            Logger.getLogger(CompraOrdenControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public CompraOrden buscarPorId(Integer id) {
        return dao.buscarPorId(id);
    }

    public List<CompraOrden> buscarTodos(String texto, String estado) {
        return dao.buscarTodos(texto, estado);
    }

    public boolean anular(Integer id) {
        try {
            return dao.anular(id);
        } catch (Exception ex) {
            Logger.getLogger(CompraOrdenControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean existeNumeroOrden(String numero, Integer idExcluida) {
        return dao.existeNumeroOrden(numero, idExcluida);
    }

    public String sugerirProximoNumero() {
        return dao.obtenerProximoNumeroOrden();
    }
}
