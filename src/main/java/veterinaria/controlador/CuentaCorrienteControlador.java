
package veterinaria.controlador;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.persistencia.CuentaCorrienteDAO;
import veterinaria.servicio.CuentaCorrienteService;

public class CuentaCorrienteControlador {
    CuentaCorrienteDAO cuentaCorrienteDAO = new CuentaCorrienteDAO();
    private final CuentaCorrienteService cuentaCorrienteService = new CuentaCorrienteService();
    
    public boolean crearCuentaCorriente(CuentaCorriente cuentaCorriente){
        try {
            return cuentaCorrienteDAO.crear(cuentaCorriente);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public boolean actualizarCuentaCorriente(CuentaCorriente cuentaCorriente){
        try {
            return cuentaCorrienteDAO.actualizar(cuentaCorriente);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    } 

    public boolean desactivarCuentaCorriente(CuentaCorriente cuentaCorriente){
        try {
            return cuentaCorrienteDAO.desactivar(cuentaCorriente);
        } catch (Exception ex) {
            Logger.getLogger(ProveedorControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public List<CuentaCorriente> buscarTodasLasCuentasCorrientes(){
        return cuentaCorrienteDAO.buscarTodas();
    }
    
    public CuentaCorriente buscarCuentaCorrientePorId(Integer idCC){
        return cuentaCorrienteDAO.buscarPorId(idCC);
    }  
    
    public CuentaCorriente buscarCuentaCorrientePorIdCliente(Integer idCliente){
        return cuentaCorrienteDAO.buscarPorIdCliente(idCliente);
    }   
    
    /** Sincroniza el saldo incluyendo el saldo inicial, sin alterar el historial. */
    public boolean actualizarSaldoActualCC(CuentaCorriente cuenta) {
        if (cuenta == null || cuenta.getIdCuentaCorriente() == null) {
            return false;
        }
        return cuentaCorrienteService.sincronizarSaldo(cuenta.getIdCuentaCorriente());
    }

    public boolean actualizarTodosLosSaldosCC() {
        for (CuentaCorriente cuenta : cuentaCorrienteDAO.buscarTodas()) {
            if (!actualizarSaldoActualCC(cuenta)) {
                return false;
            }
        }
        return true;
    }

    public Boolean cuentaCorrienteActiva(Integer idCliente){
        CuentaCorriente cuentaC = cuentaCorrienteDAO.buscarPorIdCliente(idCliente);
        if (cuentaC == null || cuentaC.getEstado() == null) return false;
        String est = cuentaC.getEstado().trim().toUpperCase();
        return est.equals("ACTIVO") || est.equals("ACTIVA");
    }

    public BigDecimal obtenerSaldoReal(Integer idCuentaCorriente) {
        return cuentaCorrienteService.recalcularSaldo(idCuentaCorriente);
    }

    public BigDecimal obtenerMargenDisponible(Integer idCliente){
        CuentaCorriente cuenta = cuentaCorrienteDAO.buscarPorIdCliente(idCliente);
        if (cuenta == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal limite = cuenta.getLimiteCredito() == null ? BigDecimal.ZERO : cuenta.getLimiteCredito();
        BigDecimal saldo = obtenerSaldoReal(cuenta.getIdCuentaCorriente());
        return limite.add(saldo);
    }
}
