
package veterinaria.controlador;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.Persona;
import veterinaria.entidad.Proveedor;
import veterinaria.persistencia.ProveedorDAO;

public class ProveedorControlador {
    ProveedorDAO proveedorDAO = new ProveedorDAO();
    
    public boolean crearProveedor(Persona persona, Proveedor proveedor){
        try {
            return proveedorDAO.crear(persona, proveedor);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public boolean actualizarProveedor(Persona persona, Proveedor proveedor){
        try {
            return proveedorDAO.actualizar(persona, proveedor);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }  
    
    public boolean eliminarProveedor(Proveedor proveedor){
        return desactivarProveedor(proveedor);
    }

    public boolean desactivarProveedor(Proveedor proveedor){
        try {
            return proveedorDAO.desactivar(proveedor);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean reactivarProveedor(Proveedor proveedor){
        try {
            return proveedorDAO.reactivar(proveedor);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public List<Proveedor> buscarTodosLosProveedores(){
        return proveedorDAO.buscarTodos();
    }
    
    public Proveedor buscarProveedorPorId(Integer idProveedor){
        return proveedorDAO.buscarPorId(idProveedor);
    }
}
