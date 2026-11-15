package veterinaria.servicio;

import java.util.List;
import java.util.Objects;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Mascota;
import veterinaria.util.JsonUtil;

/**
 * Transferencia de propietario: conserva idMascota y los registros clínicos.
 * Requiere la migración 2026-11-15_historial_propietarios.sql.
 */
public class TransferenciaMascotaService {
    private final TxRunner txRunner = new TxRunner();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    public boolean transferir(Integer idMascota, Integer idClienteNuevo,
                             String motivo, String observaciones, Integer idUsuario) {
        if (idMascota == null || idClienteNuevo == null) {
            throw new IllegalArgumentException("Seleccione un paciente y su nuevo propietario.");
        }
        if (motivo == null || motivo.trim().isEmpty() || motivo.trim().length() > 100) {
            throw new IllegalArgumentException("Indique un motivo de hasta 100 caracteres.");
        }
        if (observaciones != null && observaciones.length() > 500) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 500 caracteres.");
        }
        final String[] antes = new String[1];
        final String[] despues = new String[1];
        final Integer[] anterior = new Integer[1];
        final String[] nombre = new String[1];

        return txRunner.runInTx(em -> {
            Mascota mascota = em.find(Mascota.class, idMascota,
                    javax.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (mascota == null) throw new IllegalArgumentException("No se encontró el paciente.");
            if (!mascota.isActivo() || "Fallecimiento".equalsIgnoreCase(
                    String.valueOf(mascota.getMotivoBaja()).trim())) {
                throw new IllegalStateException("No se puede transferir un paciente inactivo o fallecido.");
            }
            if (mascota.getCliente() == null || mascota.getCliente().getIdCliente() == null) {
                throw new IllegalStateException("El paciente no tiene propietario actual.");
            }
            Cliente actual = mascota.getCliente();
            Cliente nuevo = em.find(Cliente.class, idClienteNuevo);
            if (nuevo == null || !nuevo.isActivo()) {
                throw new IllegalArgumentException("El nuevo propietario debe ser un cliente activo.");
            }
            if (!actual.isActivo()) {
                throw new IllegalStateException("El propietario actual está inactivo. Revise primero su situación.");
            }
            if (Objects.equals(actual.getIdCliente(), nuevo.getIdCliente())) {
                throw new IllegalArgumentException("El nuevo propietario debe ser distinto del actual.");
            }
            if (idUsuario != null && em.find(veterinaria.entidad.Usuario.class, idUsuario) == null) {
                throw new IllegalArgumentException("El usuario responsable no existe.");
            }
            antes[0] = JsonUtil.safeToJson(mascota);
            anterior[0] = actual.getIdCliente();
            nombre[0] = mascota.getNombre();

            em.createNativeQuery("INSERT INTO mascota_propietario_historial "
                    + "(idMascota, idClienteAnterior, idClienteNuevo, motivo, observaciones, idUsuario) "
                    + "VALUES (?, ?, ?, ?, ?, ?)")
                    .setParameter(1, idMascota)
                    .setParameter(2, actual.getIdCliente())
                    .setParameter(3, nuevo.getIdCliente())
                    .setParameter(4, motivo.trim())
                    .setParameter(5, observaciones == null || observaciones.isBlank() ? null : observaciones.trim())
                    .setParameter(6, idUsuario)
                    .executeUpdate();
            mascota.setCliente(nuevo);
            em.flush();
            despues[0] = JsonUtil.safeToJson(mascota);
            return true;
        }, () -> auditoriaService.registrar(
                "TRANSFERIR", "Mascota", Long.valueOf(idMascota),
                "TransferenciaMascotaService",
                "Transferencia de " + nombre[0] + ": cliente " + anterior[0]
                        + " a " + idClienteNuevo + " (" + motivo.trim() + ")",
                AuditoriaService.RESULT_OK, antes[0], despues[0], null));
    }

    public List<Object[]> listarHistorial(Integer idMascota) {
        if (idMascota == null) throw new IllegalArgumentException("Paciente inválido.");
        return new TxRunner().runInTx(em -> em.createNativeQuery(
                "SELECT h.fechaTransferencia, h.idClienteAnterior, h.idClienteNuevo, "
              + "h.motivo, h.observaciones FROM mascota_propietario_historial h "
              + "WHERE h.idMascota = ? ORDER BY h.fechaTransferencia DESC, h.idTransferencia DESC")
                .setParameter(1, idMascota).getResultList());
    }
}
