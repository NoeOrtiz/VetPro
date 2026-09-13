package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.*;

@Entity
@Table(name = "visita")
public class Visita implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idVisita")
    private Integer idVisita;

    private String usuarioGestion;
    private String usuarioAtiende;

    @ManyToOne
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;
    
    @Column (name = "motivoVisita")
    private String motivoVisita;
    
    @Column (name ="patologia")
    private String patologia;
    
    @Column (name ="tratamiento")
    private String tratamiento;
    
    @Column (name ="fecha")
    private LocalDate fecha;
    
    @Column (name ="hora")
    private LocalTime hora;
    
     @Column (name ="estado")
    private String estado;

    public Visita() {
    }

    public Visita(Integer idVisita, String usuarioGestion, String usuarioAtiende, Mascota mascota, Cliente cliente, String motivoVisita, String patologia, String tratamiento, LocalDate fecha, LocalTime hora, String estado) {
        this.idVisita = idVisita;
        this.usuarioGestion = usuarioGestion;
        this.usuarioAtiende = usuarioAtiende;
        this.mascota = mascota;
        this.cliente = cliente;
        this.motivoVisita = motivoVisita;
        this.patologia = patologia;
        this.tratamiento = tratamiento;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
    }

    public Integer getIdVisita() {
        return idVisita;
    }

    public void setIdVisita(Integer idVisita) {
        this.idVisita = idVisita;
    }

    public String getUsuarioGestion() {
        return usuarioGestion;
    }

    public void setUsuarioGestion(String usuarioGestion) {
        this.usuarioGestion = usuarioGestion;
    }

    public String getUsuarioAtiende() {
        return usuarioAtiende;
    }

    public void setUsuarioAtiende(String usuarioAtiende) {
        this.usuarioAtiende = usuarioAtiende;
    }

    public Mascota getMascota() {
        return mascota;
    }

    public void setMascota(Mascota mascota) {
        this.mascota = mascota;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getMotivoVisita() {
        return motivoVisita;
    }

    public void setMotivoVisita(String motivoVisita) {
        this.motivoVisita = motivoVisita;
    }

    public String getPatologia() {
        return patologia;
    }

    public void setPatologia(String patologia) {
        this.patologia = patologia;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

   
 
}
