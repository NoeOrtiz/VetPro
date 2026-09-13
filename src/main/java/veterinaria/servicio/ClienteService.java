package veterinaria.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.util.JsonUtil;

public class ClienteService {

    private final TxRunner txRunner = new TxRunner();
    private final PersonaService personaService = new PersonaService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    public boolean crear(Persona persona, Cliente cliente) {
        final Long[] idOut = new Long[1];
        final String[] despues = new String[1];
        boolean ok = txRunner.runInTx(em -> {
            Persona personaManaged = personaService.persistOrAttach(em, persona);
            cliente.setPersona(personaManaged);

            // Campos de gestión: respetar el estado elegido y registrar la fecha real.
            cliente.setEstado(cliente.isActivo() ? "ACTIVO" : "INACTIVO");
            if (cliente.getFechaAlta() == null) {
                cliente.setFechaAlta(LocalDateTime.now());
            }

            em.persist(cliente);
            em.flush();
            idOut[0] = (cliente.getIdCliente() != null ? Long.valueOf(cliente.getIdCliente()) : null);
            despues[0] = JsonUtil.safeToJson(cliente);
            return true;
        }, () -> {
            auditoriaService.registrar(
                    "CREATE",
                    "Cliente",
                    idOut[0],
                    "ClienteService",
                    "Alta de cliente: " + (cliente != null ? cliente.getRazonSocial() : ""),
                    AuditoriaService.RESULT_OK,
                    null,
                    despues[0],
                    null
            );
        });
        return ok;
    }

    public boolean actualizar(Persona persona, Cliente cliente) {
        final Long id = (cliente != null && cliente.getIdCliente() != null) ? Long.valueOf(cliente.getIdCliente()) : null;
        final String[] antes = new String[1];
        final String[] despues = new String[1];
        boolean ok = txRunner.runInTx(em -> {
            if (cliente != null && cliente.getIdCliente() != null) {
                Cliente prev = em.find(Cliente.class, cliente.getIdCliente());
                if (prev != null) {
                    antes[0] = JsonUtil.safeToJson(prev);
                }
            }
            Persona personaManaged = personaService.merge(em, persona);
            cliente.setPersona(personaManaged);
            cliente.setEstado(cliente.isActivo() ? "ACTIVO" : "INACTIVO");
            Cliente managed = em.merge(cliente);
            em.flush();
            despues[0] = JsonUtil.safeToJson(managed);
            return true;
        }, () -> {
            auditoriaService.registrar(
                    "UPDATE",
                    "Cliente",
                    id,
                    "ClienteService",
                    "Actualización de cliente: " + (cliente != null ? cliente.getRazonSocial() : ""),
                    AuditoriaService.RESULT_OK,
                    antes[0],
                    despues[0],
                    null
            );
        });
        return ok;
    }

    public boolean eliminar(Persona persona, Cliente cliente) {
        final Long id = (cliente != null && cliente.getIdCliente() != null) ? Long.valueOf(cliente.getIdCliente()) : null;
        final String[] antes = new String[1];
        final String[] despues = new String[1];
        boolean ok = txRunner.runInTx(em -> {
            Integer idCliente = cliente != null ? cliente.getIdCliente() : null;

            if (idCliente == null) {
                throw new IllegalArgumentException("El idCliente no puede ser null");
            }

            Cliente managed = em.find(Cliente.class, idCliente);
            if (managed != null) {
                antes[0] = JsonUtil.safeToJson(managed);

                // 1. Desactivamos el cliente lógicamente
                managed.setActivo(false);
                managed.setEstado("INACTIVO");

                // 2. Buscamos y desactivamos en cascada todas las mascotas de este cliente
                java.util.List<veterinaria.entidad.Mascota> mascotas = em.createQuery(
                        "SELECT m FROM Mascota m WHERE m.cliente.idCliente = :idCliente", veterinaria.entidad.Mascota.class)
                        .setParameter("idCliente", idCliente)
                        .getResultList();

                if (mascotas != null) {
                    for (veterinaria.entidad.Mascota mascota : mascotas) {
                        mascota.setActivo(false);
                        em.merge(mascota); // Guardamos el cambio de estado de cada mascota
                    }
                }

                em.flush();
                despues[0] = JsonUtil.safeToJson(managed);
            }

            return true;
        }, () -> {
            auditoriaService.registrar(
                    "DESACTIVAR",
                    "Cliente",
                    id,
                    "ClienteService",
                    "Baja lógica de cliente y mascotas en cascada: " + (cliente != null ? cliente.getRazonSocial() : ""),
                    AuditoriaService.RESULT_OK,
                    antes[0],
                    despues[0],
                    null
            );
        });
        return ok;
    }
}
