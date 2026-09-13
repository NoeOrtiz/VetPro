package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import veterinaria.entidad.TipoCitaPeluqueria;
import veterinaria.persistencia.TipoCitaPeluqueriaDAO;

public class TipoCitaPeluqueriaService {

    private final TipoCitaPeluqueriaDAO dao = new TipoCitaPeluqueriaDAO();

    public void ensureSeedFromDefaults(List<String> defaults) {
        long count = dao.countAll();
        if (count > 0) return;

        if (defaults == null) defaults = new ArrayList<>();
        int iOrden = 1;
        for (String d : defaults) {
            if (d == null) continue;
            String desc = d.trim();
            if (desc.isEmpty()) continue;
            if (desc.toLowerCase().startsWith("seleccione")) continue;
            if (dao.findByDescripcion(desc) != null) continue;

            TipoCitaPeluqueria t = new TipoCitaPeluqueria(desc, BigDecimal.ZERO, true);
            t.setOrden(iOrden++);
            dao.save(t);
        }
    }

    public List<TipoCitaPeluqueria> listarActivos() {
        return dao.findActivosOrderByDescripcion();
    }

    public List<TipoCitaPeluqueria> listarTodos() {
        return dao.findAllOrderByDescripcion();
    }

    public TipoCitaPeluqueria buscarPorDescripcion(String descripcion) {
        return dao.findByDescripcion(descripcion);
    }

    public TipoCitaPeluqueria guardarNuevo(String descripcion, BigDecimal precio) {
        TipoCitaPeluqueria existente = dao.findByDescripcion(descripcion);
        if (existente != null) {
            existente.setActivo(true);
            existente.setPrecio(precio);
            return dao.update(existente);
        }
        TipoCitaPeluqueria t = new TipoCitaPeluqueria(descripcion, precio, true);
        return dao.save(t);
    }

    public TipoCitaPeluqueria actualizar(Integer id, String descripcion, BigDecimal precio, boolean activo) {
        TipoCitaPeluqueria t = dao.findById(id);
        if (t == null) return null;
        t.setDescripcion(descripcion);
        t.setPrecio(precio);
        t.setActivo(activo);
        return dao.update(t);
    }

    public TipoCitaPeluqueria setActivo(Integer id, boolean activo) {
        TipoCitaPeluqueria t = dao.findById(id);
        if (t == null) return null;
        t.setActivo(activo);
        return dao.update(t);
    }

    public boolean swapOrden(Integer idA, Integer idB) {
        return dao.swapOrden(idA, idB);
    }

    public boolean setOrden(Integer id, Integer orden) {
        return dao.setOrden(id, orden);
    }
}
