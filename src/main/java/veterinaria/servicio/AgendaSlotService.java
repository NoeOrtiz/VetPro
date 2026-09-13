
package veterinaria.servicio;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.AgendaSlot;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.AgendaSlotDAO;
import veterinaria.persistencia.JPAUtil;

public class AgendaSlotService {

    private final AgendaSlotDAO slotDAO = new AgendaSlotDAO();
    private final ConfiguracionService configService = new ConfiguracionService();

    public int resetSlotsLibresDesdeHoy() {
        return slotDAO.eliminarLibresDesdeFecha(LocalDate.now());
    }

    public void ensureSlots(LocalDate fecha, Usuario veterinario) {
        if (fecha == null || veterinario == null) {
            return;
        }

        if (!esDiaHabilitadoGeneral(fecha)) {
            return;
        }

        long existentes = slotDAO.contarPorFechaYVeterinario(fecha, veterinario);

        boolean debeGenerar = (existentes == 0);
        boolean debeCompletar = (!debeGenerar && slotDAO.contarLibresPorFechaYVeterinario(fecha, veterinario) == 0);

        if (!debeGenerar && !debeCompletar) {
            return;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            List<AgendaSlot> slots = generarSlots(fecha, veterinario);

            if (debeCompletar) {
                List<LocalTime> horasExistentes = slotDAO.listarHorasInicioPorFechaYVeterinario(em, fecha, veterinario);
                java.util.HashSet<LocalTime> set = new java.util.HashSet<>(horasExistentes);

                for (AgendaSlot s : slots) {
                    if (!set.contains(s.getHoraInicio())) {
                        em.persist(s);
                    }
                }
            } else {
                for (AgendaSlot s : slots) {
                    em.persist(s);
                }
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private void reconciliarEstadosSlots(LocalDate fecha, Usuario veterinario) {
        if (fecha == null || veterinario == null) {
            return;
        }
        slotDAO.reconciliarEstadosParaFechaYVeterinario(fecha, veterinario);
    }

    public List<AgendaSlot> listarLibres(LocalDate fecha, Usuario veterinario) {
        ensureSlots(fecha, veterinario);
        reconciliarEstadosSlots(fecha, veterinario);
        return slotDAO.listarLibresPorFechaYVeterinario(fecha, veterinario);
    }

    public List<AgendaSlot> listarTodos(LocalDate fecha, Usuario veterinario) {
        ensureSlots(fecha, veterinario);
        reconciliarEstadosSlots(fecha, veterinario);
        return slotDAO.listarPorFechaYVeterinario(fecha, veterinario);
    }

    public List<AgendaSlot> listarParaEdicion(LocalDate fecha, Usuario veterinario, Long idSlotActual) {
        List<AgendaSlot> all = listarTodos(fecha, veterinario);
        if (idSlotActual == null) {
            List<AgendaSlot> libres = new ArrayList<>();
            for (AgendaSlot s : all) {
                if (s.getEstadoSlot() == AgendaSlot.EstadoSlot.LIBRE) {
                    libres.add(s);
                }
            }
            return libres;
        }
        List<AgendaSlot> res = new ArrayList<>();
        for (AgendaSlot s : all) {
            if (s.getEstadoSlot() == AgendaSlot.EstadoSlot.LIBRE || idSlotActual.equals(s.getIdSlot())) {
                res.add(s);
            }
        }
        return res;
    }

    private List<AgendaSlot> generarSlots(LocalDate fecha, Usuario veterinario) {
        int turnosManana = configService.getInt(
                ConfiguracionService.KEY_TURNOS_PELUQUERIA_MANANA,
                ConfiguracionService.DEFAULT_TURNOS_PELUQUERIA_MANANA
        );
        int turnosTarde = configService.getInt(
                ConfiguracionService.KEY_TURNOS_PELUQUERIA_TARDE,
                ConfiguracionService.DEFAULT_TURNOS_PELUQUERIA_TARDE
        );

        LocalTime mDesde = parseHora(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_MANANA_DESDE,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_MANANA_DESDE
        ));
        LocalTime mHasta = parseHora(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_MANANA_HASTA,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_MANANA_HASTA
        ));

        LocalTime tDesde = parseHora(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_TARDE_DESDE,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_TARDE_DESDE
        ));
        LocalTime tHasta = parseHora(configService.getString(
                ConfiguracionService.KEY_HORARIO_PELUQUERIA_TARDE_HASTA,
                ConfiguracionService.DEFAULT_HORARIO_PELUQUERIA_TARDE_HASTA
        ));

        List<AgendaSlot> slots = new ArrayList<>();
        slots.addAll(generarFranja(fecha, veterinario, mDesde, mHasta, turnosManana));
        slots.addAll(generarFranja(fecha, veterinario, tDesde, tHasta, turnosTarde));
        return slots;
    }

    private List<AgendaSlot> generarFranja(LocalDate fecha, Usuario vet, LocalTime desde, LocalTime hasta, int cantidadTurnos) {
        List<AgendaSlot> res = new ArrayList<>();
        if (cantidadTurnos <= 0 || desde == null || hasta == null || !hasta.isAfter(desde)) {
            return res;
        }

        long totalMin = Duration.between(desde, hasta).toMinutes();
        if (totalMin <= 0) {
            return res;
        }

        long duracion = totalMin / cantidadTurnos;
        if (duracion <= 0) {
            return res;
        }

        LocalTime cursor = desde;
        for (int i = 0; i < cantidadTurnos; i++) {
            LocalTime fin;
            if (i == cantidadTurnos - 1) {
                fin = hasta;
            } else {
                fin = cursor.plusMinutes(duracion);
            }
            if (!fin.isAfter(cursor)) {
                break;
            }
            AgendaSlot s = new AgendaSlot(fecha, cursor, fin, vet, AgendaSlot.EstadoSlot.LIBRE);
            res.add(s);
            cursor = fin;
            if (!cursor.isBefore(hasta)) {
                break;
            }
        }
        return res;
    }

    private boolean esDiaHabilitadoGeneral(LocalDate fecha) {
        int dayValue = fecha.getDayOfWeek().getValue();
        return contieneDia(configService.getString(
                ConfiguracionService.KEY_DIAS_HABILITADOS_PELUQUERIA,
                ConfiguracionService.DEFAULT_DIAS_HABILITADOS_PELUQUERIA
        ), dayValue)
                || contieneDia(configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_LABORATORIO,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_LABORATORIO
                ), dayValue)
                || contieneDia(configService.getString(
                        ConfiguracionService.KEY_DIAS_HABILITADOS_HOSPITALIZACION,
                        ConfiguracionService.DEFAULT_DIAS_HABILITADOS_HOSPITALIZACION
                ), dayValue);
    }

    private boolean contieneDia(String diasCsv, int dayValue) {
        if (diasCsv == null || diasCsv.trim().isEmpty()) {
            return false;
        }
        String[] parts = diasCsv.split(",");
        for (String p : parts) {
            try {
                if (Integer.parseInt(p.trim()) == dayValue) {
                    return true;
                }
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    private LocalTime parseHora(String hhmm) {
        try {
            if (hhmm == null) {
                return null;
            }
            String v = hhmm.trim();
            if (v.isEmpty()) {
                return null;
            }
            return LocalTime.parse(v);
        } catch (Exception e) {
            return null;
        }
    }
}
