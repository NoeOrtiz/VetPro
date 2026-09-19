
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

    public List<MetodoPago> obtenerMetodosPagoActivos() {
        return operarMetodoPago.buscarActivos();
    }

    public boolean crear(MetodoPago metodoPago) {
        if (metodoPago == null || metodoPago.getNombre() == null
                || metodoPago.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del metodo de pago es obligatorio.");
        }
        metodoPago.setNombre(metodoPago.getNombre().trim());
        metodoPago.setActivo(true);
        return operarMetodoPago.crear(metodoPago);
    }

    public boolean actualizar(MetodoPago metodoPago) {
        if (metodoPago == null || metodoPago.getIdMetodoPago() == null) {
            throw new IllegalArgumentException("Debe indicar el metodo de pago.");
        }
        if (metodoPago.getNombre() == null || metodoPago.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del metodo de pago es obligatorio.");
        }
        metodoPago.setNombre(metodoPago.getNombre().trim());
        return operarMetodoPago.actualizar(metodoPago);
    }

    public boolean desactivar(Integer idMetodoPago) {
        return operarMetodoPago.desactivar(idMetodoPago);
    }

    public boolean reactivar(Integer idMetodoPago) {
        return operarMetodoPago.reactivar(idMetodoPago);
    }
}
