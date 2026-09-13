package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "agenda_slot",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_agenda_slot_fecha_hora_vet",
                    columnNames = {"fecha", "horaInicio", "idVeterinario"}
            )
        }
)
public class AgendaSlot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idSlot")
    private Long idSlot;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "horaInicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "horaFin", nullable = false)
    private LocalTime horaFin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idVeterinario", nullable = false)
    private Usuario veterinario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoSlot", nullable = false)
    private EstadoSlot estadoSlot = EstadoSlot.LIBRE;

    @Version
    @Column(name = "version")
    private Long version;

    public AgendaSlot() {
    }

    public AgendaSlot(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Usuario veterinario, EstadoSlot estadoSlot) {
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.veterinario = veterinario;
        this.estadoSlot = estadoSlot;
    }

    public Long getIdSlot() {
        return idSlot;
    }

    public void setIdSlot(Long idSlot) {
        this.idSlot = idSlot;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public Usuario getVeterinario() {
        return veterinario;
    }

    public void setVeterinario(Usuario veterinario) {
        this.veterinario = veterinario;
    }

    public EstadoSlot getEstadoSlot() {
        return estadoSlot;
    }

    public void setEstadoSlot(EstadoSlot estadoSlot) {
        this.estadoSlot = estadoSlot;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        String hi = horaInicio != null ? horaInicio.toString() : "--:--";
        String hf = horaFin != null ? horaFin.toString() : "--:--";
        return hi + " - " + hf;
    }

    public enum EstadoSlot {
        LIBRE,
        RESERVADO,
        BLOQUEADO
    }
}
