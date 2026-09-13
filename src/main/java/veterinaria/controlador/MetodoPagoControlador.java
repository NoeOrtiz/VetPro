
package veterinaria.controlador;

import java.util.List;
import veterinaria.entidad.MetodoPago;
import veterinaria.persistencia.MetodoPagoDAO;

public class MetodoPagoControlador {
    private final MetodoPagoDAO operarMetodoPago = new MetodoPagoDAO();
    
    public List<MetodoPago> obtenerTodosLosMetodosPago(){
        return operarMetodoPago.buscarTodos();
    }
    
    public MetodoPago obtenerMetodoDePagoPorNombre(String nombre){
        return operarMetodoPago.buscarPorNombre(nombre);
    }

    public MetodoPago buscarPorId(Integer idMetodoPago) {
        return operarMetodoPago.buscarPorId(idMetodoPago);
    }
}
