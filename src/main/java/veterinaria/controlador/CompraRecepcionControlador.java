package veterinaria.controlador;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.CompraRecepcionDAO;

public class CompraRecepcionControlador {

    private final CompraRecepcionDAO dao = new CompraRecepcionDAO();

    public boolean crear(CompraRecepcion recepcion, Usuario usuario) {
        try {
            return dao.crear(recepcion, usuario);
        } catch (Exception ex) {
            Logger.getLogger(CompraRecepcionControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public CompraRecepcion buscarPorId(Long id) {
        return dao.buscarPorId(id);
    }

    public List<CompraRecepcion> buscarPorOrden(Integer idCompraOrden) {
        return dao.buscarPorOrden(idCompraOrden);
    }

    public Map<Integer, Integer> sumarRecibidoPorOrden(Integer idCompraOrden) {
        return dao.sumarRecibidoPorOrden(idCompraOrden);
    }

    public boolean anular(Long idCompraRecepcion, Usuario usuario, String motivo) {
        try {
            return dao.anular(idCompraRecepcion, usuario, motivo);
        } catch (Exception ex) {
            Logger.getLogger(CompraRecepcionControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
}
