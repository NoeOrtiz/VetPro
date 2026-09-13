package veterinaria.servicio;

import java.util.List;
import veterinaria.entidad.StockProductoInfo;
import veterinaria.persistencia.StockProductoDAO;

public class StockProductoService {

    private final StockProductoDAO dao = new StockProductoDAO();

    public List<StockProductoInfo> buscar(String textoBusqueda, String rubro, Integer idProveedor, StockProductoInfo.EstadoStock estado) {
        return dao.buscarStock(textoBusqueda, rubro, idProveedor, estado);
    }

    public List<String> listarRubros() {
        return dao.listarRubros();
    }
}
