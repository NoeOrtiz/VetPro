package veterinaria.servicio;

import java.util.List;
import java.util.Locale;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Usuario;
import veterinaria.util.JsonUtil;

/** Solicitudes persistentes, con resolución transaccional y trazabilidad. */
public class SolicitudReactivacionService {
    private final TxRunner tx = new TxRunner();
    private final AuditoriaService auditoria = new AuditoriaService();

    private static String validarTexto(String texto, int max, String campo) {
        if (texto == null || texto.trim().isEmpty() || texto.trim().length() > max)
            throw new IllegalArgumentException(campo + " debe tener entre 1 y " + max + " caracteres.");
        return texto.trim();
    }
    public int solicitar(Integer idMascota, Integer idUsuario, String prioridad, String justificacion) {
        String motivo = validarTexto(justificacion, 1000, "La justificación");
        String nivel = "URGENTE".equalsIgnoreCase(prioridad) ? "URGENTE" : "NORMAL";
        final int id = tx.runInTx(em -> {
            Mascota m = em.find(Mascota.class, idMascota);
            if (m == null || m.isActivo()) throw new IllegalStateException("Seleccione un paciente inactivo.");
            if (idUsuario == null || em.find(Usuario.class,idUsuario) == null)
                throw new IllegalStateException("Se requiere una sesión válida.");
            Number duplicadas = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM mascota_solicitud_reactivacion WHERE idMascota=? AND estado='PENDIENTE'")
                .setParameter(1,idMascota).getSingleResult();
            if (duplicadas.longValue() > 0)
                throw new IllegalStateException("Ya existe una solicitud pendiente para este paciente.");
            String original = m.getMotivoBaja();
            String tipo = "Fallecimiento".equalsIgnoreCase(original == null ? "" : original.trim())
                    ? "RECTIFICACION" : "REACTIVACION";
            em.createNativeQuery("INSERT INTO mascota_solicitud_reactivacion "
                    + "(idMascota,idUsuarioSolicitante,tipo,prioridad,justificacion,motivoBajaOriginal) "
                    + "VALUES (?,?,?,?,?,?)")
                    .setParameter(1,idMascota).setParameter(2,idUsuario).setParameter(3,tipo)
                    .setParameter(4,nivel).setParameter(5,motivo).setParameter(6,original).executeUpdate();
            return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
        });
        auditoria.registrar("SOLICITAR_REACTIVACION", "Mascota", Long.valueOf(idMascota),
                "SolicitudReactivacionService", "Solicitud " + id + " " + nivel,
                AuditoriaService.RESULT_OK, null, null, motivo);
        return id;
    }
    public long contarPendientes() {
        return tx.runInTx(em -> ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM mascota_solicitud_reactivacion WHERE estado='PENDIENTE'")
                .getSingleResult()).longValue());
    }
    public List<Object[]> listarPendientes() {
        return tx.runInTx(em -> em.createNativeQuery(
                "SELECT s.idSolicitud,m.nombre,s.tipo,s.prioridad,s.justificacion,"
                + "s.fechaSolicitud,s.idMascota,s.motivoBajaOriginal "
                + "FROM mascota_solicitud_reactivacion s JOIN mascota m ON m.idMascota=s.idMascota "
                + "WHERE s.estado='PENDIENTE' ORDER BY (s.prioridad='URGENTE') DESC,s.fechaSolicitud ASC")
                .getResultList());
    }
    public Integer solicitudUrgentePendiente(Integer mascotaId) {
        if (mascotaId == null) return null;
        return tx.runInTx(em -> {
            java.util.List<?> ids = em.createNativeQuery(
                "SELECT idSolicitud FROM mascota_solicitud_reactivacion "
              + "WHERE idMascota=? AND estado='PENDIENTE' AND prioridad='URGENTE' "
              + "ORDER BY fechaSolicitud DESC LIMIT 1")
                .setParameter(1,mascotaId).getResultList();
            return ids.isEmpty() ? null : ((Number)ids.get(0)).intValue();
        });
    }
    public boolean existeUrgentePendiente(Integer mascotaId) {
        return tx.runInTx(em -> ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM mascota_solicitud_reactivacion "
                + "WHERE idMascota=? AND estado='PENDIENTE' AND prioridad='URGENTE'")
                .setParameter(1,mascotaId).getSingleResult()).longValue()>0);
    }
    public void resolver(int solicitudId, Integer idAdmin, boolean aprobar, String observacion) {
        String nota = validarTexto(observacion, 1000, "La justificación de resolución");
        if (idAdmin == null) throw new IllegalArgumentException("Administrador no identificado.");
        final Long[] idMascota = new Long[1];
        final String[] antes = new String[1], despues = new String[1];
        tx.runInTx(em -> {
            Object[] s = (Object[]) em.createNativeQuery(
                    "SELECT idMascota,tipo,estado FROM mascota_solicitud_reactivacion WHERE idSolicitud=? FOR UPDATE")
                    .setParameter(1,solicitudId).getSingleResult();
            if (!"PENDIENTE".equals(s[2])) throw new IllegalStateException("La solicitud ya fue resuelta.");
            Mascota m = em.find(Mascota.class, ((Number)s[0]).intValue(),
                    javax.persistence.LockModeType.PESSIMISTIC_WRITE);
            Usuario admin = em.find(Usuario.class,idAdmin);
            if (admin == null || admin.getRol() == null
                    || !"Administrador".equalsIgnoreCase(admin.getRol().getNombreRol()))
                throw new SecurityException("Solo el administrador puede resolver solicitudes.");
            if (m == null || m.isActivo()) throw new IllegalStateException("El paciente ya está activo o no existe.");
            idMascota[0] = Long.valueOf(m.getIdMascota());
            antes[0] = JsonUtil.safeToJson(m);
            if (aprobar) {
                if (m.getCliente()==null || !m.getCliente().isActivo())
                    throw new IllegalStateException("Debe reactivarse primero al propietario.");
                boolean fallecido = "Fallecimiento".equalsIgnoreCase(
                        m.getMotivoBaja()==null ? "" : m.getMotivoBaja().trim());
                if (fallecido != "RECTIFICACION".equals(s[1]))
                    throw new IllegalStateException("El motivo de baja cambió. Revise la solicitud.");
                m.setActivo(true);
                m.setMotivoBaja(null);
                m.setBajaPorCliente(false);
                em.flush();
            }
            em.createNativeQuery("UPDATE mascota_solicitud_reactivacion "
                    + "SET estado=?,idUsuarioResolutor=?,fechaResolucion=NOW(),observacionResolucion=? "
                    + "WHERE idSolicitud=?")
                    .setParameter(1,aprobar ? "APROBADA" : "RECHAZADA")
                    .setParameter(2,idAdmin).setParameter(3,nota).setParameter(4,solicitudId)
                    .executeUpdate();
            despues[0] = JsonUtil.safeToJson(m);
            return null;
        });
        auditoria.registrar(aprobar ? "APROBAR_RECTIFICACION" : "RECHAZAR_REACTIVACION",
                "Mascota", idMascota[0], "SolicitudReactivacionService",
                "Solicitud " + solicitudId + ": " + nota, AuditoriaService.RESULT_OK,
                antes[0], despues[0], null);
    }
}
