
package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.Mascota;
import veterinaria.persistencia.MascotaDAO;

public class MascotaControlador {
    private MascotaDAO mascotaDAO = new MascotaDAO();
    
    public boolean crearMascota(Mascota mascota){
        try {
            return mascotaDAO.crear(mascota);
        } catch (Exception ex) {
            Logger.getLogger(MascotaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public boolean actualizarMascota(Mascota mascota){
        try {
            return mascotaDAO.actualizar(mascota);
        } catch (Exception ex) {
            Logger.getLogger(MascotaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public boolean eliminarMascota(Mascota mascota){
        try {
            return mascotaDAO.eliminar(mascota);
        } catch (Exception ex) {
            Logger.getLogger(MascotaControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public List<Mascota> buscarTodasLasMascotas(){
        return mascotaDAO.buscarTodos();
    }
    
    public Mascota buscarMascotaPorId(Integer idMascota){
        return mascotaDAO.buscarPorId(idMascota);
    }
    
}
