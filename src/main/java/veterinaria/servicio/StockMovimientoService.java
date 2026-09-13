package veterinaria.servicio;

import java.util.List;
import veterinaria.entidad.StockMovimiento;
import veterinaria.persistencia.StockMovimientoDAO;

public class StockMovimientoService {

    private final StockMovimientoDAO dao = new StockMovimientoDAO();

    public List<StockMovimiento> listarPorProducto(Integer idProducto, int maxResults) {
        Integer max = maxResults > 0 ? maxResults : null;
        return dao.listarPorProducto(idProducto, max);
    }
}
