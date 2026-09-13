
package veterinaria.servicio;

import java.util.List;
import veterinaria.entidad.Rubro;
import veterinaria.persistencia.RubroDAO;

public class RubroService {

    private final RubroDAO rubroDAO;

    public RubroService() {
        this.rubroDAO = new RubroDAO();
    }

    public RubroService(RubroDAO rubroDAO) {
        this.rubroDAO = rubroDAO;
    }

    public List<Rubro> listarActivos() {
        return rubroDAO.listarActivos();
    }

    public List<String> listarNombresActivos() {
        return rubroDAO.listarNombresActivos();
    }

    public int sincronizarDesdeProductos() {
        return rubroDAO.sincronizarDesdeProductos();
    }

    public List<Rubro> listarTodos() {
        return rubroDAO.listarTodos();
    }

    public Rubro buscarPorNombre(String nombre) {
        return rubroDAO.buscarPorNombre(nombre);
    }

    public boolean crear(Rubro rubro) {
        return rubroDAO.crear(rubro);
    }

    public boolean actualizar(Rubro rubro) {
        return rubroDAO.actualizar(rubro);
    }

    public boolean desactivar(Integer idRubro) {
        return rubroDAO.desactivar(idRubro);
    }

    public Rubro upsertPorNombre(String nombre, Integer stockMinimoDefault, boolean activo) {
        return rubroDAO.upsertPorNombre(nombre, stockMinimoDefault, activo);
    }
}
