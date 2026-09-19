package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.Producto;
import veterinaria.persistencia.ProductoDAO;

public class ProductoControlador {

    private ProductoDAO productoDAO = new ProductoDAO();

    public boolean crearProducto(Producto producto) {
        try {
            if (producto.getCodigo() == null || producto.getCodigo().trim().isEmpty()) {
                throw new IllegalArgumentException("El código del producto es obligatorio.");
            }
            if (producto.getRubro() == null || producto.getRubro().trim().isEmpty()) {
                throw new IllegalArgumentException("El rubro del producto es obligatorio.");
            }
            return productoDAO.crear(producto);
        } catch (Exception ex) {
            Logger.getLogger(ProductoControlador.class.getName()).log(Level.SEVERE, "Error al crear producto", ex);
            return false;
        }
    }

    public boolean actualizarProducto(Producto producto) {
        try {
            return productoDAO.actualizar(producto);
        } catch (Exception ex) {
            Logger.getLogger(ProductoControlador.class.getName()).log(Level.SEVERE, "Error al actualizar producto", ex);
            return false;
        }
    }

    public boolean validarYGuardarProducto(Producto producto, boolean esNuevo, String codigoOriginal) throws IllegalArgumentException {

        // 0. Calculamos el precio de venta antes de validar y guardar
        calcularYAsignarPrecioVenta(producto);

        // 1. Validaciones de campos obligatorios y reglas de negocio
        if (producto.getCodigo() == null || producto.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("El código del producto no puede estar vacío.");
        }

        if (esNuevo || !producto.getCodigo().trim().equalsIgnoreCase(codigoOriginal)) {
            if (verificarCodigoDuplicado(producto.getCodigo().trim())) {
                throw new IllegalArgumentException("Ya existe un producto registrado con ese código.");
            }
        }

        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
        }

        if (producto.getRubro() == null || producto.getRubro().trim().isEmpty() || producto.getRubro().equals("Seleccionar Rubro")) {
            throw new IllegalArgumentException("Debe seleccionar un rubro válido.");
        }

        if (producto.getPrecioCosto() == null || producto.getPrecioCosto() <= 0) {
            throw new IllegalArgumentException("El precio de costo debe ser mayor a 0.");
        }

        if (producto.getStock() == null || producto.getStock() < 0 || producto.getStockMinimo() == null || producto.getStockMinimo() < 0) {
            throw new IllegalArgumentException("El stock inicial y mínimo no pueden ser negativos.");
        }

        // 2. Persistencia según corresponda
        try {
            if (esNuevo) {
                return productoDAO.crear(producto);
            } else {
                return productoDAO.actualizar(producto);
            }
        } catch (Exception ex) {
            // Registramos el error internamente y lanzamos una excepción limpia hacia la vista
            Logger.getLogger(ProductoControlador.class.getName()).log(Level.SEVERE, "Error al persistir el producto", ex);
            throw new IllegalArgumentException("Error en la base de datos al guardar el producto: " + ex.getMessage());
        }
    }

    public boolean verificarCodigoDuplicado(String codigo) {
        List<Producto> productos = productoDAO.buscarTodos();
        if (productos == null) {
            return false;
        }

        for (Producto existente : productos) {
            if (existente != null && existente.getCodigo() != null) {
                if (existente.getCodigo().trim().equalsIgnoreCase(codigo)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void calcularYAsignarPrecioVenta(Producto producto) {
        if (producto.getPrecioCosto() != null) {
            double costo = producto.getPrecioCosto();

            double iva = 21.0; // Valor por defecto por si viene nulo
            try {
                if (producto.getIva() != null) {
                    String ivaLimpio = producto.getIva().toString().replaceAll("[^0-9.]", "");
                    if (!ivaLimpio.isEmpty()) {
                        iva = Double.parseDouble(ivaLimpio);
                    }
                }
            } catch (Exception e) {
                iva = 0.0;
            }

            // Fórmula de cálculo del precio final
            double precioFinal = costo * (1 + (iva / 100));
            producto.setPrecio(Math.round(precioFinal * 100.0) / 100.0);
        }

    }

    public boolean eliminarProductoPorId(Integer idProducto) {
        try {
            Producto p = productoDAO.buscarPorId(idProducto);
            if (p == null) {
                return false;
            }
            p.setEstado("Inactivo"); // Baja lógica
            return productoDAO.actualizar(p);
        } catch (Exception ex) {
            Logger.getLogger(ProductoControlador.class.getName()).log(Level.SEVERE, "Error al eliminar producto", ex);
            return false;
        }
    }

    public List<Producto> buscarTodosLosProductos() {
        return productoDAO.buscarTodos();
    }

    public List<Producto> buscarProductosDisponiblesParaVenta() {
        return productoDAO.buscarDisponiblesParaVenta();
    }

    public List<Producto> buscarProductosPorEstado(String estadoFiltro) {
        return productoDAO.buscarPorEstado(estadoFiltro);
    }

    public Producto buscarProductoPorId(Integer idProducto) {
        return productoDAO.buscarPorId(idProducto);
    }

    public boolean actualizarStockVentaProducto(Integer idProducto, Integer cantidad) {
        return productoDAO.actualizarStockVenta(idProducto, cantidad);
    }
}
