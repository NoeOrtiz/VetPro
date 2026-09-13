
package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.Rubro;
import veterinaria.persistencia.RubroDAO;

public class RubroControlador {

    private final RubroDAO rubroDAO = new RubroDAO();

    public List<Rubro> listarActivos() {
        return rubroDAO.listarActivos();
    }

    public List<Rubro> listarTodos() {
        return rubroDAO.listarTodos();
    }

    public Rubro buscarPorId(Integer idRubro) {
        return rubroDAO.buscarPorId(idRubro);
    }

    public Rubro buscarPorNombre(String nombre) {
        return rubroDAO.buscarPorNombre(nombre);
    }

    public boolean crear(Rubro rubro) {
        try {
            return rubroDAO.crear(rubro);
        } catch (Exception ex) {
            Logger.getLogger(RubroControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean actualizar(Rubro rubro) {
        try {
            return rubroDAO.actualizar(rubro);
        } catch (Exception ex) {
            Logger.getLogger(RubroControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean desactivar(Integer idRubro) {
        try {
            return rubroDAO.desactivar(idRubro);
        } catch (Exception ex) {
            Logger.getLogger(RubroControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public Rubro upsertPorNombre(String nombre, Integer stockMinimoDefault, boolean activo) {
        try {
            return rubroDAO.upsertPorNombre(nombre, stockMinimoDefault, activo);
        } catch (Exception ex) {
            Logger.getLogger(RubroControlador.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
