package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.persistence.*;

@Entity
@Table(name = "hospitalizacion")
public class Hospitalizacion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHospitalizacion")
    private Integer idHospitalizacion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarioGestion", referencedColumnName = "idUsuario")
    private Usuario usuarioGestion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idVeterinario")
    private Usuario veterinario;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idSlot", unique = true)
    private AgendaSlot slot;

    @ManyToOne
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;
    
    @Column (name ="fechaIngreso")
    private LocalDate fechaIngreso;
    
    @Column (name ="fechaAlta")
    private LocalDateTime fechaAlta;
    
    @Column (name ="hora")
    private LocalTime hora;
    
    @Column (name = "motivo")
    private String motivo;
    
    @Column (name ="diagnostico")
    private String diagnostico;
    
     @Column (name ="tratamiento")
    private String tratamiento;
    
    @Column (name ="estado")
    private String estado;

    public Hospitalizacion() {
    }

    public Hospitalizacion(Integer idHospitalizacion, Usuario usuarioGestion, String usuarioAtiende, Mascota mascota, Cliente cliente, LocalDate fechaIngreso, LocalDateTime fechaAlta, LocalTime hora, String motivo, String diagnostico, String tratamiento, String estado) {
        this.idHospitalizacion = idHospitalizacion;
        this.usuarioGestion = usuarioGestion;
        this.mascota = mascota;
        this.cliente = cliente;
        this.fechaIngreso = fechaIngreso;
        this.fechaAlta = fechaAlta;
        this.hora = hora;
        this.motivo = motivo;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.estado = estado;
    }

    public Integer getIdHospitalizacion() {
        return idHospitalizacion;
    }

    public void setIdHospitalizacion(Integer idHospitalizacion) {
        this.idHospitalizacion = idHospitalizacion;
    }

    public Usuario getUsuarioGestion() {
        return usuarioGestion;
    }

    public void setUsuarioGestion(Usuario usuarioGestion) {
        this.usuarioGestion = usuarioGestion;
    }

    public Usuario getVeterinario() {
        return veterinario;
    }

    public void setVeterinario(Usuario veterinario) {
        this.veterinario = veterinario;
    }

    public AgendaSlot getSlot() {
        return slot;
    }

    public void setSlot(AgendaSlot slot) {
        this.slot = slot;
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

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    

    

   

}