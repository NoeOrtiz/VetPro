package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.ProductoProveedor;
import veterinaria.persistencia.ProductoProveedorDAO;

public class ProductoProveedorControlador {

    private final ProductoProveedorDAO dao = new ProductoProveedorDAO();

    public List<ProductoProveedor> listarPorProducto(Integer idProducto) {
        try {
            return dao.listarPorProducto(idProducto);
        } catch (Exception ex) {
            Logger.getLogger(ProductoProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return List.of();
        }
    }
}
