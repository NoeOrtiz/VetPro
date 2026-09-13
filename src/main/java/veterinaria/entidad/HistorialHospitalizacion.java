package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "HistorialHospitalizacion")
public class HistorialHospitalizacion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHistorialHospitalizacion;

    @ManyToOne
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @Column(name = "fechaIngreso")
    private LocalDate fechaIngreso;

    @Column(name = "fechaAlta")
    private LocalDate fechaAlta;

    @Column(name = "diagnostico")
    private String diagnostico;

    @Column(name = "tratamiento")
    private String tratamiento;

    @Column(name = "estado")
    private String estado;

    public HistorialHospitalizacion() {
    }

    public HistorialHospitalizacion(Integer idHistorialHospitalizacion, Mascota mascota, Cliente cliente, LocalDate fechaIngreso, LocalDate fechaAlta, String diagnostico, String tratamiento, String estado) {
        this.idHistorialHospitalizacion = idHistorialHospitalizacion;
        this.mascota = mascota;
        this.cliente = cliente;
        this.fechaIngreso = fechaIngreso;
        this.fechaAlta = fechaAlta;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.estado = estado;
    }

    public Integer getIdHistorialHospitalizacion() {
        return idHistorialHospitalizacion;
    }

    public void setIdHistorialHospitalizacion(Integer idHistorialHospitalizacion) {
        this.idHistorialHospitalizacion = idHistorialHospitalizacion;
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

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDate fechaAlta) {
        this.fechaAlta = fechaAlta;
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
