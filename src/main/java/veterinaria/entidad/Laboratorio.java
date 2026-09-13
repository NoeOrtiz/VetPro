package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "laboratorio")
public class Laboratorio implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLaboratorio")
    private Integer idLaboratorio;

    private String usuarioGestion;
    private String usuarioAtiende;

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

    @Column(name = "motivoExtraccion")
    private String motivoExtraccion;

    @Column(name = "diagnostico")
    private String diagnostico;

    @Column(name = "tipoAnalisis")
    private String tipoAnalisis;

    @Column(name = "fechaExtraccion")
    private LocalDate fechaExtraccion;

    @Column(name = "horaExtraccion")
    private LocalTime horaExtraccion;

    @Column(name = "fechaEnvio")
    private LocalDate fechaEnvio;

    @Column(name = "fechaRecepcion")
    private LocalDate fechaRecepcion;

    @Column(name = "estado")
    private String estado;

    public Laboratorio() {
    }

    public Laboratorio(Integer idLaboratorio, String usuarioGestion, String usuarioAtiende, Mascota mascota, Cliente cliente, String motivoExtraccion, String diagnostico, String tipoAnalisis, LocalDate fechaExtraccion, LocalTime horaExtraccion, LocalDate fechaEnvio, String estado) {
        this.idLaboratorio = idLaboratorio;
        this.usuarioGestion = usuarioGestion;
        this.usuarioAtiende = usuarioAtiende;
        this.mascota = mascota;
        this.cliente = cliente;
        this.motivoExtraccion = motivoExtraccion;
        this.diagnostico = diagnostico;
        this.tipoAnalisis = tipoAnalisis;
        this.fechaExtraccion = fechaExtraccion;
        this.horaExtraccion = horaExtraccion;
        this.fechaEnvio = fechaEnvio;
        this.estado = estado;
    }

    public Integer getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(Integer idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public String getUsuarioGestion() {
        return usuarioGestion;
    }

    public void setUsuarioGestion(String usuarioGestion) {
        this.usuarioGestion = usuarioGestion;
    }

    public String getUsuarioAtiende() {
        if (veterinario != null) {
            try {
                if (veterinario.getPersona() != null) {
                    String n = veterinario.getPersona().getNombre();
                    String a = veterinario.getPersona().getApellido();
                    String full = (n != null ? n : "") + " " + (a != null ? a : "");
                    return full.trim();
                }
            } catch (Exception ignore) {
            }
        }
        return (usuarioAtiende != null) ? usuarioAtiende : "";
    }

    public void setUsuarioAtiende(String usuarioAtiende) {
        this.usuarioAtiende = usuarioAtiende;
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

    public String getMotivoExtraccion() {
        return motivoExtraccion;
    }

    public void setMotivoExtraccion(String motivoExtraccion) {
        this.motivoExtraccion = motivoExtraccion;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTipoAnalisis() {
        return tipoAnalisis;
    }

    public void setTipoAnalisis(String tipoAnalisis) {
        this.tipoAnalisis = tipoAnalisis;
    }

    public LocalDate getFechaExtraccion() {
        return fechaExtraccion;
    }

    public void setFechaExtraccion(LocalDate fechaExtraccion) {
        this.fechaExtraccion = fechaExtraccion;
    }

    public LocalTime getHoraExtraccion() {
        return horaExtraccion;
    }

    public void setHoraExtraccion(LocalTime horaExtraccion) {
        this.horaExtraccion = horaExtraccion;
    }

    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    

    public LocalDate getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDate fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }
public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    

}
