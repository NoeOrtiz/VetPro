package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.*;
import veterinaria.entidad.Usuario;

@Entity
@Table(name = "peluqueria_historial")
public class PeluqueriaHistorial implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHistorial")
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idTurno", nullable = false)
    private Peluqueria turno;

    @Column(name = "evento", nullable = false, length = 30)
    private String evento;

    @Column(name = "fechaEvento", nullable = false)
    private LocalDateTime fechaEvento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @Column(name = "motivo", length = 500)
    private String motivo;

    public PeluqueriaHistorial() {}

    public PeluqueriaHistorial(Peluqueria turno, String evento, Usuario usuario, String motivo) {
        this.turno = turno;
        this.evento = evento;
        this.usuario = usuario;
        this.motivo = motivo;
        this.fechaEvento = LocalDateTime.now();
    }

    public Integer getIdHistorial() { return idHistorial; }

    public Peluqueria getTurno() { return turno; }
    public void setTurno(Peluqueria turno) { this.turno = turno; }

    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }

    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
