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
    
    // Evolución clínica extensa: no limitar a los 255 caracteres por defecto de VARCHAR.
    @Column(name = "tratamiento", columnDefinition = "TEXT")
    private String tratamiento;
    
    @Column (name ="fecha")
    private LocalDate fecha;
    
    @Column (name ="hora")
    private LocalTime hora;
    
     @Column (name ="estado")
    private String estado;

    /** Medición tomada al ingresar a esta consulta (histórica, independiente de Mascota.peso). */
    @Column(name = "peso_consulta", precision = 8, scale = 3)
    private java.math.BigDecimal pesoConsulta;

    @Column(name = "especie_consulta", length = 100)
    private String especieConsulta;

    @Column(name = "raza_consulta", length = 100)
    private String razaConsulta;

    public java.math.BigDecimal getPesoConsulta() { return pesoConsulta; }
    public void setPesoConsulta(java.math.BigDecimal peso) { pesoConsulta = peso; }
    public String getEspecieConsulta() { return especieConsulta; }
    public void setEspecieConsulta(String especie) { especieConsulta = especie; }
    public String getRazaConsulta() { return razaConsulta; }
    public void setRazaConsulta(String raza) { razaConsulta = raza; }

    @Column(name = "prioridad_atencion", nullable = false, length = 12)
    private String prioridadAtencion = "CONTROL";

    public String getPrioridadAtencion() { return prioridadAtencion; }
    public void setPrioridadAtencion(String prioridad) {
        if (!"EMERGENCIA".equals(prioridad) && !"URGENCIA".equals(prioridad)
                && !"CONTROL".equals(prioridad))
            throw new IllegalArgumentException("Seleccione Emergencia, Urgencia o Control.");
        this.prioridadAtencion = prioridad;
    }

    @Column(name = "atencion_provisional", nullable = false)
    private boolean atencionProvisional = false;

    @Column(name = "idSolicitudReactivacion")
    private Integer idSolicitudReactivacion;

    public boolean isAtencionProvisional() { return atencionProvisional; }
    public void setAtencionProvisional(boolean valor) { this.atencionProvisional = valor; }
    public Integer getIdSolicitudReactivacion() { return idSolicitudReactivacion; }
    public void setIdSolicitudReactivacion(Integer id) { this.idSolicitudReactivacion = id; }


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
