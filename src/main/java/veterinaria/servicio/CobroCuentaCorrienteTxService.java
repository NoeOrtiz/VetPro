package veterinaria.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Usuario;

public class CobroCuentaCorrienteTxService {

    private final TxRunner tx = new TxRunner();

    public static class ResultadoCobro {
        private final boolean ok;
        private final Integer idMovimientoCuentaCorriente;
        private final String error;

        public ResultadoCobro(boolean ok, Integer idMovimientoCuentaCorriente, String error) {
            this.ok = ok;
            this.idMovimientoCuentaCorriente = idMovimientoCuentaCorriente;
            this.error = error;
        }

        public boolean isOk() {
            return ok;
        }

        public Integer getIdMovimientoCuentaCorriente() {
            return idMovimientoCuentaCorriente;
        }

        public String getError() {
            return error;
        }
    }

    public ResultadoCobro registrarCobro(Integer idCliente,
                                         BigDecimal monto,
                                         MetodoPago metodoPago,
                                         Usuario usuario,
                                         String descripcion) {
        try {
            Integer idMov = tx.runInTx(em -> registrarCobroInternal(em, idCliente, monto, metodoPago, usuario, descripcion));
            return new ResultadoCobro(true, idMov, null);
        } catch (Exception e) {
            return new ResultadoCobro(false, null, e.getMessage());
        }
    }

    private Integer registrarCobroInternal(EntityManager em,
                                          Integer idCliente,
                                          BigDecimal monto,
                                          MetodoPago metodoPago,
                                          Usuario usuario,
                                          String descripcion) {

        if (idCliente == null || idCliente <= 0) {
            throw new IllegalArgumentException("Cliente inválido para cobro de Cuenta Corriente.");
        }
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario nulo.");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del cobro debe ser mayor a 0.");
        }

        Cliente cliente = em.find(Cliente.class, idCliente);
        if (cliente == null) {
            throw new IllegalStateException("No se encontró el cliente seleccionado.");
        }

        CuentaCorriente cc = obtenerCuentaCorrienteActiva(em, idCliente);
        if (cc == null) {
            throw new IllegalStateException("El cliente no tiene Cuenta Corriente ACTIVA.");
        }

        BigDecimal saldoActual = (cc.getSaldoActual() != null)
                ? cc.getSaldoActual()
                : ((cc.getSaldoInicial() != null) ? cc.getSaldoInicial() : BigDecimal.ZERO);

        BigDecimal saldoResultante = saldoActual.add(monto);

        CuentaCorrienteMovimiento movCC = new CuentaCorrienteMovimiento();
        movCC.setCuentaCorriente(cc);
        movCC.setFechaMovimiento(LocalDate.now());
        movCC.setTipoMovimiento(CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);
        movCC.setMonto(monto);
        movCC.setSaldoResultante(saldoResultante);
        movCC.setDescripcion((descripcion != null && !descripcion.trim().isEmpty()) ? descripcion.trim() : "Pago Cuenta Corriente");
        em.persist(movCC);

        cc.setSaldoActual(saldoResultante);
        cc.setUltimaEdicion(LocalDate.now());
        em.merge(cc);

        MetodoPago mpManaged = resolveMetodoPagoManaged(em, metodoPago);
        CajaMovimiento movCaja = new CajaMovimiento();
        movCaja.setFecha(onlyDate(new Date()));
        movCaja.setUsuario(usuario);
        movCaja.setRecibo(null);
        movCaja.setMetodoPago(mpManaged);
        movCaja.setMonto(monto);
        movCaja.setTipoMovimiento(CajaMovimiento.TipoMovimiento.CREDITO);
        String descCaja = "Cobro CC - " + safeCliente(cliente);
        if (movCC.getIdMovimiento() != null) {
            descCaja += " (MovCC #" + movCC.getIdMovimiento() + ")";
        }
        if (mpManaged != null && mpManaged.getNombre() != null && !mpManaged.getNombre().trim().isEmpty()) {
            descCaja += " - " + mpManaged.getNombre().trim();
        }
        movCaja.setDescripcion(descCaja);
        movCaja.setEliminado(false);
        em.persist(movCaja);

        return movCC.getIdMovimiento();
    }

    private String safeCliente(Cliente c) {
        if (c == null) return "Cliente";
        if (c.getRazonSocial() != null && !c.getRazonSocial().trim().isEmpty()) return c.getRazonSocial().trim();
        return "Cliente #" + c.getIdCliente();
    }

    private Date onlyDate(Date d) {
        if (d == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private MetodoPago resolveMetodoPagoManaged(EntityManager em, MetodoPago mp) {
        if (mp == null) return null;
        if (mp.getIdMetodoPago() != null) {
            MetodoPago managed = em.find(MetodoPago.class, mp.getIdMetodoPago());
            return (managed != null) ? managed : mp;
        }
        return mp;
    }

    private CuentaCorriente obtenerCuentaCorrienteActiva(EntityManager em, Integer idCliente) {
        try {
            TypedQuery<CuentaCorriente> query = em.createQuery(
                    "SELECT c FROM CuentaCorriente c WHERE c.cliente.idCliente = :idCliente AND UPPER(c.estado) IN ('ACTIVO','ACTIVA')",
                    CuentaCorriente.class
            );
            query.setParameter("idCliente", idCliente);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
