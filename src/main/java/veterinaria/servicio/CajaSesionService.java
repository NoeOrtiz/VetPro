package veterinaria.servicio;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.CajaSesion;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Usuario;

public class CajaSesionService {

    private final TxRunner tx = new TxRunner();

    public CajaSesion obtenerSesionAbierta() {
        return tx.runInTx(em -> em.createQuery(
                "SELECT s FROM CajaSesion s WHERE s.estado = :estado ORDER BY s.fechaApertura DESC",
                CajaSesion.class)
                .setParameter("estado", CajaSesion.Estado.ABIERTA)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    public CajaSesion abrir(BigDecimal montoInicial, Usuario usuario) {
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El efectivo inicial no puede ser negativo.");
        }
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario que abre la caja.");
        }

        return tx.runInTx(em -> {
            Long abiertas = em.createQuery(
                    "SELECT COUNT(s) FROM CajaSesion s WHERE s.estado = :estado", Long.class)
                    .setParameter("estado", CajaSesion.Estado.ABIERTA)
                    .getSingleResult();
            if (abiertas != null && abiertas > 0L) {
                throw new IllegalStateException("Ya existe una sesión de caja abierta.");
            }

            Usuario managed = em.find(Usuario.class, usuario.getIdUsuario());
            if (managed == null || !managed.isActivo()) {
                throw new IllegalStateException("El usuario de apertura no existe o esta inactivo.");
            }

            CajaSesion sesion = new CajaSesion();
            sesion.setFechaApertura(new Date());
            sesion.setMontoInicial(montoInicial);
            sesion.setEstado(CajaSesion.Estado.ABIERTA);
            sesion.setUsuarioApertura(managed);
            em.persist(sesion);
            return sesion;
        });
    }

    public BigDecimal calcularEfectivoEsperado(Long idSesion) {
        return tx.runInTx(em -> calcularEfectivoEsperado(em, idSesion));
    }

    public CajaSesion cerrar(Long idSesion, BigDecimal efectivoContado, String motivoDiferencia, Usuario usuario) {
        if (efectivoContado == null || efectivoContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El efectivo contado no puede ser negativo.");
        }
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario que cierra la caja.");
        }

        return tx.runInTx(em -> {
            CajaSesion sesion = em.find(CajaSesion.class, idSesion, LockModeType.PESSIMISTIC_WRITE);
            if (sesion == null || sesion.getEstado() != CajaSesion.Estado.ABIERTA) {
                throw new IllegalStateException("La sesión de caja no está abierta.");
            }

            BigDecimal esperado = calcularEfectivoEsperado(em, idSesion);
            BigDecimal diferencia = efectivoContado.subtract(esperado);
            String motivo = motivoDiferencia == null ? "" : motivoDiferencia.trim();
            if (diferencia.compareTo(BigDecimal.ZERO) != 0 && motivo.isEmpty()) {
                throw new IllegalStateException("Debe indicar el motivo del sobrante o faltante de caja.");
            }

            Usuario managed = em.find(Usuario.class, usuario.getIdUsuario());
            if (managed == null || !managed.isActivo()) {
                throw new IllegalStateException("El usuario de cierre no existe o esta inactivo.");
            }

            sesion.setEfectivoEsperado(esperado);
            sesion.setEfectivoContado(efectivoContado);
            sesion.setDiferencia(diferencia);
            sesion.setMotivoDiferencia(diferencia.signum() == 0 ? null : motivo);
            sesion.setFechaCierre(new Date());
            sesion.setUsuarioCierre(managed);
            sesion.setEstado(CajaSesion.Estado.CERRADA);
            em.merge(sesion);
            return sesion;
        });
    }

    private BigDecimal calcularEfectivoEsperado(EntityManager em, Long idSesion) {
        CajaSesion sesion = em.find(CajaSesion.class, idSesion);
        if (sesion == null) {
            throw new IllegalStateException("La sesión de caja no existe.");
        }

        BigDecimal inicial = sesion.getMontoInicial() == null ? BigDecimal.ZERO : sesion.getMontoInicial();
        BigDecimal ingresos = sumarEfectivo(em, idSesion, CajaMovimiento.TipoMovimiento.CREDITO);
        BigDecimal egresos = sumarEfectivo(em, idSesion, CajaMovimiento.TipoMovimiento.DEBITO);
        return inicial.add(ingresos).subtract(egresos);
    }

    private BigDecimal sumarEfectivo(EntityManager em, Long idSesion, CajaMovimiento.TipoMovimiento tipo) {
        BigDecimal total = em.createQuery(
                "SELECT COALESCE(SUM(m.monto), 0) FROM CajaMovimiento m "
                + "WHERE m.cajaSesion.idCajaSesion = :idSesion "
                + "AND m.tipoMovimiento = :tipo "
                + "AND m.anulado = false "
                + "AND m.metodoPago.afectaEfectivo = true",
                BigDecimal.class)
                .setParameter("idSesion", idSesion)
                .setParameter("tipo", tipo)
                .getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }
}
