package veterinaria.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.Producto;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.Usuario;
import veterinaria.util.Constantes;
import veterinaria.util.enums.MetodoPagoTipo;

public class CajaRegistradoraTxService {

    private final TxRunner tx = new TxRunner();
    private final StockTxService stockTxService = new StockTxService();

    public static class ResultadoVenta {
        private final boolean ok;
        private final Long idRecibo;
        private final String error;

        public ResultadoVenta(boolean ok, Long idRecibo, String error) {
            this.ok = ok;
            this.idRecibo = idRecibo;
            this.error = error;
        }

        public boolean isOk() {
            return ok;
        }

        public Long getIdRecibo() {
            return idRecibo;
        }

        public String getError() {
            return error;
        }
    }

    public ResultadoVenta registrarVenta(Recibo recibo,
                                         List<ReciboProductos> productos,
                                         List<ReciboMetodoPago> metodosPago,
                                         Usuario usuario) {
        try {
            Long id = tx.runInTx(em -> registrarVentaInternal(em, recibo, productos, metodosPago, usuario));
            return new ResultadoVenta(true, id, null);
        } catch (Exception e) {
            return new ResultadoVenta(false, null, e.getMessage());
        }
    }

    private Long registrarVentaInternal(EntityManager em,
                                        Recibo recibo,
                                        List<ReciboProductos> productos,
                                        List<ReciboMetodoPago> metodosPago,
                                        Usuario usuario) {

        if (recibo == null) {
            throw new IllegalArgumentException("Recibo nulo.");
        }
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario nulo.");
        }
        if (recibo.getCliente() == null) {
            throw new IllegalArgumentException("Debe indicar Cliente en el Recibo.");
        }
        if (recibo.getFecha() == null) {
            recibo.setFecha(new Date());
        }
        if (recibo.getTipo() == null || recibo.getTipo().trim().isEmpty()) {
            recibo.setTipo("Venta");
        }
        recibo.setUsuario(usuario);

        em.persist(recibo);

        if (productos != null) {
            for (ReciboProductos rp : productos) {
                rp.setRecibo(recibo);
                em.persist(rp);
                if (rp.getProducto() != null && rp.getProducto().getIdProducto() != null) {
                    stockTxService.registrarSalidaPorVenta(em, rp.getProducto().getIdProducto(), rp.getCantidad(), recibo, usuario);
                }

            }
        }

        if (metodosPago != null) {
            for (ReciboMetodoPago rmp : metodosPago) {
                rmp.setRecibo(recibo);
                em.persist(rmp);
            }

            BigDecimal totalPagos = BigDecimal.ZERO;
            BigDecimal totalRecibo = (recibo.getTotalRecibo() == null) ? BigDecimal.ZERO : recibo.getTotalRecibo();
            ReciboMetodoPago rmpEfectivo = null;
            MetodoPago mpEfectivoManaged = null;
            MetodoPago mpEfectivoOriginal = null;

            for (ReciboMetodoPago rmp : metodosPago) {
                BigDecimal monto = (rmp.getMonto() == null) ? BigDecimal.ZERO : rmp.getMonto();
                if (monto.compareTo(BigDecimal.ZERO) > 0) {
                    totalPagos = totalPagos.add(monto);
                }

                MetodoPago mpOriginal = rmp.getMetodoPago();
                MetodoPago mp = resolveMetodoPagoManaged(em, mpOriginal);
                if (rmpEfectivo == null && esMetodoEfectivo(em, mp, mpOriginal)) {
                    rmpEfectivo = rmp;
                    mpEfectivoManaged = mp;
                    mpEfectivoOriginal = mpOriginal;
                }
            }

            BigDecimal vuelto = BigDecimal.ZERO;
            if (totalPagos.compareTo(totalRecibo) > 0) {
                vuelto = totalPagos.subtract(totalRecibo);
            }

            for (ReciboMetodoPago rmp : metodosPago) {
                MetodoPago mpOriginal = rmp.getMetodoPago();
                MetodoPago mp = resolveMetodoPagoManaged(em, mpOriginal);
                String nombreMp = (mp != null && mp.getNombre() != null) ? mp.getNombre().trim() : "";
                BigDecimal monto = (rmp.getMonto() == null) ? BigDecimal.ZERO : rmp.getMonto();
                if (monto.compareTo(BigDecimal.ZERO) <= 0) continue;

                if (esMetodoCuentaCorriente(em, mp, mpOriginal)) {
                    registrarDebitoCuentaCorriente(em, recibo, recibo.getCliente(), monto);
                    continue;
                }

                if (rmpEfectivo != null && rmp == rmpEfectivo && vuelto.compareTo(BigDecimal.ZERO) > 0) {
                    registrarCreditoCaja(em, usuario, recibo, mp, monto, "Venta Recibo #" + recibo.getIdRecibo() + " - " + nombreMp);
                    registrarDebitoCaja(em, usuario, recibo, mp,
                            vuelto,
                            "VUELTO Recibo #" + recibo.getIdRecibo() + " - " + nombreMp);
                    continue;
                }

                registrarCreditoCaja(em, usuario, recibo, mp, monto, "Venta Recibo #" + recibo.getIdRecibo() + " - " + nombreMp);
            }
        }

        return recibo.getIdRecibo();
    }

    private void registrarCreditoCaja(EntityManager em, Usuario usuario, Recibo recibo, MetodoPago metodoPago, BigDecimal monto, String descripcion) {
        CajaMovimiento mov = new CajaMovimiento();
        mov.setFecha(new Date());
        mov.setUsuario(usuario);
        mov.setRecibo(recibo);
        mov.setMetodoPago(metodoPago);
        mov.setMonto(monto);
        mov.setTipoMovimiento(CajaMovimiento.TipoMovimiento.CREDITO);
        mov.setDescripcion(descripcion);
        mov.setEliminado(false);
        em.persist(mov);
    }

    private void registrarDebitoCaja(EntityManager em, Usuario usuario, Recibo recibo, MetodoPago metodoPago, BigDecimal monto, String descripcion) {
        CajaMovimiento mov = new CajaMovimiento();
        mov.setFecha(new Date());
        mov.setUsuario(usuario);
        mov.setRecibo(recibo);
        mov.setMetodoPago(metodoPago);
        mov.setMonto(monto);
        mov.setTipoMovimiento(CajaMovimiento.TipoMovimiento.DEBITO);
        mov.setDescripcion(descripcion);
        mov.setEliminado(false);
        em.persist(mov);
    }

    private void registrarDebitoCuentaCorriente(EntityManager em, Recibo recibo, Cliente cliente, BigDecimal monto) {
        if (cliente == null || cliente.getIdCliente() == null) {
            throw new IllegalArgumentException("Cliente inválido para débito de Cuenta Corriente.");
        }

        CuentaCorriente cc = obtenerCuentaCorrienteActiva(em, cliente.getIdCliente());
        if (cc == null) {
            throw new IllegalStateException("El cliente no tiene Cuenta Corriente ACTIVA.");
        }

        BigDecimal saldoActual = (cc.getSaldoActual() != null) ? cc.getSaldoActual() :
                ((cc.getSaldoInicial() != null) ? cc.getSaldoInicial() : BigDecimal.ZERO);

        BigDecimal saldoResultante = saldoActual.subtract(monto);

        CuentaCorrienteMovimiento mov = new CuentaCorrienteMovimiento();
        mov.setCuentaCorriente(cc);
        mov.setFechaMovimiento(LocalDate.now());
        mov.setTipoMovimiento(CuentaCorrienteMovimiento.TipoMovimiento.DEBITO);
        mov.setMonto(monto);
        mov.setSaldoResultante(saldoResultante);
        mov.setDescripcion("Venta");
        mov.setRecibo(recibo);
        em.persist(mov);

        cc.setSaldoActual(saldoResultante);
        cc.setUltimaEdicion(LocalDate.now());
        em.merge(cc);
    }

    private MetodoPago resolveMetodoPagoManaged(EntityManager em, MetodoPago mp) {
        if (mp == null) return null;
        if (mp.getIdMetodoPago() != null) {
            MetodoPago managed = em.find(MetodoPago.class, mp.getIdMetodoPago());
            return (managed != null) ? managed : mp;
        }
        return mp;
    }

    private boolean esMetodoCuentaCorriente(EntityManager em, MetodoPago mpManaged, MetodoPago mpOriginal) {
        String nombre = (mpManaged != null && mpManaged.getNombre() != null) ? mpManaged.getNombre().trim() : "";
        String descripcion = (mpManaged != null && mpManaged.getDescripcion() != null) ? mpManaged.getDescripcion().trim() : "";

        if (nombre.isEmpty() && mpOriginal != null && mpOriginal.getNombre() != null) {
            nombre = mpOriginal.getNombre().trim();
        }

        if (esCuentaCorrientePorTexto(nombre) || esCuentaCorrientePorTexto(descripcion)) {
            return true;
        }

        Integer id = (mpManaged != null)
                ? mpManaged.getIdMetodoPago()
                : (mpOriginal != null ? mpOriginal.getIdMetodoPago() : null);

        if (id != null) {
            MetodoPago db = em.find(MetodoPago.class, id);
            String n = (db != null && db.getNombre() != null) ? db.getNombre().trim() : "";
            String d = (db != null && db.getDescripcion() != null) ? db.getDescripcion().trim() : "";
            if (esCuentaCorrientePorTexto(n) || esCuentaCorrientePorTexto(d)) {
                return true;
            }
        }

        return false;
    }

    private boolean esCuentaCorrientePorTexto(String texto) {
        if (texto == null) return false;
        String t = normalizarTexto(texto);

        String k = normalizarTexto(MetodoPagoTipo.CUENTA_CORRIENTE.getEtiqueta());
        if (!k.isEmpty() && k.equals(t)) return true;

        boolean contieneCuenta = t.contains("cuenta");
        boolean contieneCorriente = t.contains("corriente");
        boolean contieneCte = t.contains("cte");
        boolean contieneCta = t.contains("cta");

        if (contieneCuenta && (contieneCorriente || contieneCte)) return true;
        if (contieneCta && contieneCte) return true;

        return false;
    }

    private boolean esMetodoEfectivo(EntityManager em, MetodoPago mpManaged, MetodoPago mpOriginal) {
        String nombre = (mpManaged != null && mpManaged.getNombre() != null) ? mpManaged.getNombre().trim() : "";
        String descripcion = (mpManaged != null && mpManaged.getDescripcion() != null) ? mpManaged.getDescripcion().trim() : "";
        if (nombre.isEmpty() && mpOriginal != null && mpOriginal.getNombre() != null) {
            nombre = mpOriginal.getNombre().trim();
        }
        if (esEfectivoPorTexto(nombre) || esEfectivoPorTexto(descripcion)) {
            return true;
        }
        Integer id = (mpManaged != null)
                ? mpManaged.getIdMetodoPago()
                : (mpOriginal != null ? mpOriginal.getIdMetodoPago() : null);
        if (id != null) {
            MetodoPago db = em.find(MetodoPago.class, id);
            String n = (db != null && db.getNombre() != null) ? db.getNombre().trim() : "";
            String d = (db != null && db.getDescripcion() != null) ? db.getDescripcion().trim() : "";
            return esEfectivoPorTexto(n) || esEfectivoPorTexto(d);
        }
        return false;
    }

    private boolean esEfectivoPorTexto(String texto) {
        if (texto == null) return false;
        String t = normalizarTexto(texto);
        String k = normalizarTexto(MetodoPagoTipo.EFECTIVO.getEtiqueta());
        if (!k.isEmpty() && k.equals(t)) return true;
        return t.contains("efectivo") || t.equals("cash");
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String t = texto.trim().toLowerCase();
        t = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        t = t.replaceAll("[^a-z0-9\\s]", " ");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
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
