package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "procedimiento")
public class Procedimiento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProcedimiento")
    private Integer idProcedimiento;

    private String usuarioGestion;
    private String usuarioAtiende;

    @ManyToOne
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idHospitalizacion", nullable = true)
    private Hospitalizacion hospitalizacion;

    @Column(name = "fechaIngreso")
    private LocalDate fechaIngreso;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "procedimiento")
    private String procedimiento;

    @Column(name = "motivo")
    private String motivo;
    
    @Column (name = "anestesiologo")
    private String anestesiologo;
    
    @Column(name = "estado")
    private String estado;

    @Column(name = "observaciones")
    private String observaciones;

    public Procedimiento() {
    }

    public Procedimiento(Integer idProcedimiento, String usuarioGestion, String usuarioAtiende, Mascota mascota, Cliente cliente, LocalDate fechaIngreso, LocalTime hora, String procedimiento, String motivo, String anestesiologo, String estado, String observaciones) {
        this.idProcedimiento = idProcedimiento;
        this.usuarioGestion = usuarioGestion;
        this.usuarioAtiende = usuarioAtiende;
        this.mascota = mascota;
        this.cliente = cliente;
        this.fechaIngreso = fechaIngreso;
        this.hora = hora;
        this.procedimiento = procedimiento;
        this.motivo = motivo;
        this.anestesiologo = anestesiologo;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Integer getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(Integer idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
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

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getProcedimiento() {
        return procedimiento;
    }

    public void setProcedimiento(String procedimiento) {
        this.procedimiento = procedimiento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getAnestesiologo() {
        return anestesiologo;
    }

    public void setAnestesiologo(String anestesiologo) {
        this.anestesiologo = anestesiologo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    

    

    

    public Hospitalizacion getHospitalizacion() {
        return hospitalizacion;
    }

    public void setHospitalizacion(Hospitalizacion hospitalizacion) {
        this.hospitalizacion = hospitalizacion;
    }
}
