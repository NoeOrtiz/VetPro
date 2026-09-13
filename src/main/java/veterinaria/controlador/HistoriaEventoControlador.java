package veterinaria.controlador;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.HistoriaEvento;
import veterinaria.entidad.Hospitalizacion;
import veterinaria.entidad.Laboratorio;
import veterinaria.entidad.Mascota;
import veterinaria.entidad.Peluqueria;
import veterinaria.entidad.Procedimiento;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.Visita;
import veterinaria.persistencia.HistoriaEventoDAO;

public class HistoriaEventoControlador {

    private static final Logger LOG = Logger.getLogger(HistoriaEventoControlador.class.getName());

    private final HistoriaEventoDAO dao = new HistoriaEventoDAO();

    public void crear(HistoriaEvento evento) {
        try {
            if (evento == null) return;
            try {
                if (evento.getRefTabla() != null && evento.getRefId() != null) {
                    if (dao.existePorReferencia(evento.getRefTabla(), evento.getRefId())) {
                        return;
                    }
                }
            } catch (Exception ignore) {
            }
            dao.crear(evento);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo crear HistoriaEvento", e);
        }
    }

    public HistoriaEvento buscarPorId(Integer idEvento) {
        try {
            return dao.buscarPorId(idEvento);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo buscar HistoriaEvento", e);
            return null;
        }
    }

    public List<HistoriaEvento> listarPorMascota(Integer idMascota) {
        try {
            return dao.listarPorMascota(idMascota);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo listar HistoriaEvento", e);
            return null;
        }
    }

    public void registrarDesdeVisita(Visita visita) {
        if (visita == null || visita.getMascota() == null) return;
        LocalDate f = visita.getFecha();
        LocalTime h = visita.getHora();
        LocalDateTime dt = (f != null) ? LocalDateTime.of(f, h != null ? h : LocalTime.MIDNIGHT) : LocalDateTime.now();
        String resumen = safe(visita.getMotivoVisita());
        String obs = buildObs(
                "Diagnóstico", visita.getPatologia(),
                "Tratamiento", visita.getTratamiento()
        );
        crear(new HistoriaEvento(
                visita.getMascota(),
                dt,
                "VISITA",
                safe(visita.getUsuarioAtiende()),
                resumen,
                obs,
                "visita",
                visita.getIdVisita()
        ));
    }

    public void registrarDesdeLaboratorio(Laboratorio lab) {
        if (lab == null || lab.getMascota() == null) return;
        LocalDate f = lab.getFechaExtraccion();
        LocalTime h = lab.getHoraExtraccion();
        LocalDateTime dt = (f != null) ? LocalDateTime.of(f, h != null ? h : LocalTime.MIDNIGHT) : LocalDateTime.now();
        String resumen = (safe(lab.getTipoAnalisis()) + " - " + safe(lab.getMotivoExtraccion())).trim();
        crear(new HistoriaEvento(
                lab.getMascota(),
                dt,
                "LABORATORIO",
                safe(lab.getUsuarioAtiende()),
                resumen,
                safe(lab.getDiagnostico()),
                "laboratorio",
                lab.getIdLaboratorio()
        ));
    }

    public void registrarDesdeHospitalizacion(Hospitalizacion h) {
        if (h == null || h.getMascota() == null) return;
        LocalDate f = h.getFechaIngreso();
        LocalTime t = h.getHora();
        LocalDateTime dt = (f != null) ? LocalDateTime.of(f, t != null ? t : LocalTime.MIDNIGHT) : LocalDateTime.now();
        String prof = nombreUsuario(h.getVeterinario());
        String obs = buildObs(
                "Diagnóstico", h.getDiagnostico(),
                "Tratamiento", h.getTratamiento()
        );
        crear(new HistoriaEvento(
                h.getMascota(),
                dt,
                "HOSPITALIZACION",
                prof,
                safe(h.getMotivo()),
                obs,
                "hospitalizacion",
                h.getIdHospitalizacion()
        ));
    }

    public void registrarDesdeProcedimiento(Procedimiento p) {
        if (p == null || p.getMascota() == null) return;
        LocalDate f = p.getFechaIngreso();
        LocalTime t = p.getHora();
        LocalDateTime dt = (f != null) ? LocalDateTime.of(f, t != null ? t : LocalTime.MIDNIGHT) : LocalDateTime.now();
        String resumen = (safe(p.getProcedimiento()) + " - " + safe(p.getMotivo())).trim();
        String obs = buildObs(
                "Anestesiólogo", p.getAnestesiologo(),
                "Observaciones", p.getObservaciones()
        );
        crear(new HistoriaEvento(
                p.getMascota(),
                dt,
                "PROCEDIMIENTO",
                safe(p.getUsuarioAtiende()),
                resumen,
                obs,
                "procedimiento",
                p.getIdProcedimiento()
        ));
    }

    public void registrarDesdePeluqueria(Peluqueria t) {
        if (t == null || t.getMascota() == null) return;
        LocalDate f = t.getFecha();
        LocalTime h = t.getHora();
        LocalDateTime dt = (f != null) ? LocalDateTime.of(f, h != null ? h : LocalTime.MIDNIGHT) : LocalDateTime.now();
        String resumen = safe(t.getTipoDeCita());
        String obs = safe(t.getPresupuesto());
        crear(new HistoriaEvento(
                t.getMascota(),
                dt,
                "PELUQUERIA",
                safe(t.getUsuarioAtiende()),
                resumen,
                obs,
                "peluqueria",
                t.getIdTurno()
        ));
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private String nombreUsuario(Usuario u) {
        if (u == null || u.getPersona() == null) return "";
        String n = u.getPersona().getNombre() != null ? u.getPersona().getNombre() : "";
        String a = u.getPersona().getApellido() != null ? u.getPersona().getApellido() : "";
        return (n + " " + a).trim();
    }

    private String buildObs(String k1, String v1, String k2, String v2) {
        StringBuilder sb = new StringBuilder();
        if (v1 != null && !v1.trim().isEmpty()) {
            sb.append(k1).append(": ").append(v1.trim());
        }
        if (v2 != null && !v2.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(k2).append(": ").append(v2.trim());
        }
        return sb.toString();
    }
}
