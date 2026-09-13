package veterinaria.servicio;

import veterinaria.entidad.Mascota;
import veterinaria.util.JsonUtil;

/**
 * Gestiona las altas, modificaciones y bajas lógicas de Mascota.
 * Todas las operaciones se ejecutan en una transacción y quedan auditadas.
 */
public class MascotaService {

    private final TxRunner txRunner = new TxRunner();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    public boolean crear(Mascota mascota) {
        final Long[] idOut = new Long[1];
        final String[] despues = new String[1];

        return txRunner.runInTx(em -> {
            if (mascota == null) {
                throw new IllegalArgumentException("La mascota no puede ser null.");
            }

            mascota.setActivo(true);
            em.persist(mascota);
            em.flush();

            idOut[0] = mascota.getIdMascota() != null
                    ? Long.valueOf(mascota.getIdMascota()) : null;
            despues[0] = JsonUtil.safeToJson(mascota);
            return true;
        }, () -> auditoriaService.registrar(
                "CREATE",
                "Mascota",
                idOut[0],
                "MascotaService",
                "Alta de mascota: " + nombreSeguro(mascota),
                AuditoriaService.RESULT_OK,
                null,
                despues[0],
                null
        ));
    }

    public boolean actualizar(Mascota mascota) {
        final Long id = mascota != null && mascota.getIdMascota() != null
                ? Long.valueOf(mascota.getIdMascota()) : null;
        final String[] antes = new String[1];
        final String[] despues = new String[1];

        return txRunner.runInTx(em -> {
            if (mascota == null || mascota.getIdMascota() == null) {
                throw new IllegalArgumentException("La mascota seleccionada no es válida.");
            }

            Mascota previa = em.find(Mascota.class, mascota.getIdMascota());
            if (previa == null) {
                throw new IllegalArgumentException("No se encontró la mascota seleccionada.");
            }

            antes[0] = JsonUtil.safeToJson(previa);
            Mascota managed = em.merge(mascota);
            em.flush();
            despues[0] = JsonUtil.safeToJson(managed);
            return true;
        }, () -> auditoriaService.registrar(
                "UPDATE",
                "Mascota",
                id,
                "MascotaService",
                "Actualización de mascota: " + nombreSeguro(mascota),
                AuditoriaService.RESULT_OK,
                antes[0],
                despues[0],
                null
        ));
    }

    public boolean desactivar(Mascota mascota) {
        final Long id = mascota != null && mascota.getIdMascota() != null
                ? Long.valueOf(mascota.getIdMascota()) : null;
        final String[] antes = new String[1];
        final String[] despues = new String[1];

        return txRunner.runInTx(em -> {
            if (mascota == null || mascota.getIdMascota() == null) {
                throw new IllegalArgumentException("La mascota seleccionada no es válida.");
            }

            Mascota managed = em.find(Mascota.class, mascota.getIdMascota());
            if (managed == null) {
                throw new IllegalArgumentException("No se encontró la mascota seleccionada.");
            }

            antes[0] = JsonUtil.safeToJson(managed);
            managed.setActivo(false);
            em.flush();
            despues[0] = JsonUtil.safeToJson(managed);
            return true;
        }, () -> auditoriaService.registrar(
                "DESACTIVAR",
                "Mascota",
                id,
                "MascotaService",
                "Baja lógica de mascota: " + nombreSeguro(mascota),
                AuditoriaService.RESULT_OK,
                antes[0],
                despues[0],
                null
        ));
    }

    private static String nombreSeguro(Mascota mascota) {
        return mascota != null && mascota.getNombre() != null
                ? mascota.getNombre() : "";
    }
}
