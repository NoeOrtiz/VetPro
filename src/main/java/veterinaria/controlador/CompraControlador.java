
package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.CompraOrden;
import veterinaria.entidad.CompraOrdenItem;
import veterinaria.entidad.CompraRecepcion;
import veterinaria.entidad.CompraRecepcionItem;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.CompraOrdenDAO;
import veterinaria.persistencia.CompraRecepcionDAO;
import veterinaria.servicio.CompraTxService;

public class CompraControlador {

    private static final Logger LOG = Logger.getLogger(CompraControlador.class.getName());

    private final CompraTxService compraTxService = new CompraTxService();
    private final CompraOrdenDAO ordenDAO = new CompraOrdenDAO();
    private final CompraRecepcionDAO recepcionDAO = new CompraRecepcionDAO();

    public CompraOrden crearOrden(Proveedor proveedor, List<CompraOrdenItem> items, String observacion) {
        try {
            return compraTxService.crearOrden(proveedor, items, observacion);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public CompraRecepcion confirmarRecepcion(CompraOrden orden, List<CompraRecepcionItem> items,
            String remito, String factura, Usuario usuario) {
        try {
            return compraTxService.confirmarRecepcion(orden, items, remito, factura, usuario);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public List<CompraOrden> listarOrdenes() {
        return ordenDAO.buscarTodos("", "");
    }

    public List<CompraOrden> listarOrdenesPorEstado(String estado) {
        return ordenDAO.buscarTodos("", estado);
    }

    public List<veterinaria.entidad.CompraRecepcion> listarRecepciones() {
        return recepcionDAO.buscarTodas();
    }
}
