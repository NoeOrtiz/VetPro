
package veterinaria.entidad;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "peluqueria")
public class Peluqueria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTurno")
    private Integer idTurno;

    @Column(name = "usuarioGestion")
    private String usuarioGestion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idVeterinario")
    private Usuario veterinario;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idSlot", unique = true)
    private AgendaSlot slot;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idMascota")
    private Mascota mascota;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "tipoDeCita") 
    private String tipoDeCita;

    @Column(name = "presupuesto") // Valor presupuestado 
    private String presupuesto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idTipoCitaPeluqueria")
    private TipoCitaPeluqueria tipoCitaPeluqueria;

    @Column(name = "precioCerrado", precision = 10, scale = 2)
    private BigDecimal precioCerrado;

    @Column(name = "estado")
    private EstadoPeluqueriaEnum estado;

    @Column(name = "estadoCobro", length = 20, nullable = false)
    private String estadoCobro = "PENDIENTE";

    @Column(name = "idReciboCobro")
    private Long idReciboCobro;
    
    public Peluqueria() {
    }

    public Peluqueria(Integer idTurno, String usuarioGestion, String usuarioAtiende, Mascota mascota, LocalDate fecha, LocalTime hora, String tipoDeCita, String presupuesto, EstadoPeluqueriaEnum estado) {
        this.idTurno = idTurno;
        this.usuarioGestion = usuarioGestion;
        this.mascota = mascota;
        this.fecha = fecha;
        this.hora = hora;
        this.tipoDeCita = tipoDeCita;
        this.presupuesto = presupuesto;
        this.estado = estado;
    }

    public Integer getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(Integer idTurno) {
        this.idTurno = idTurno;
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
        return "";
    }

    @Deprecated
    public void setUsuarioAtiende(String usuarioAtiende) {
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

    public String getTipoDeCita() {
        return tipoDeCita;
    }

    public void setTipoDeCita(String tipoDeCita) {
        this.tipoDeCita = tipoDeCita;
    }

    public String getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(String presupuesto) {
        this.presupuesto = presupuesto;
    }

    

    public TipoCitaPeluqueria getTipoCitaPeluqueria() {
        return tipoCitaPeluqueria;
    }

    public void setTipoCitaPeluqueria(TipoCitaPeluqueria tipoCitaPeluqueria) {
        this.tipoCitaPeluqueria = tipoCitaPeluqueria;
    }

    public BigDecimal getPrecioCerrado() {
        return precioCerrado;
    }

    public void setPrecioCerrado(BigDecimal precioCerrado) {
        this.precioCerrado = precioCerrado;
    }
    public String getEstadoCobro() {
        return estadoCobro;
    }

    public void setEstadoCobro(String estadoCobro) {
        this.estadoCobro = estadoCobro;
    }

    public Long getIdReciboCobro() {
        return idReciboCobro;
    }

    public void setIdReciboCobro(Long idReciboCobro) {
        this.idReciboCobro = idReciboCobro;
    }

public EstadoPeluqueriaEnum getEstado() {
        return estado;
    }

    public void setEstado(EstadoPeluqueriaEnum estado) {
        this.estado = estado;
    }

    
}
