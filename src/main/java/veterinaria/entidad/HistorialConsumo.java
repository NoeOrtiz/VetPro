package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

public class HistorialConsumo implements Serializable {

   /* @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConsumo; // Identificador único

    @Column(name = "fechaIngreso")
    private LocalDate fechaIngreso;

    @Column(name = "tipoServicio")
    private String tipoServicio; // Tipo de servicio (Visita, Hospitalización, etc.)

    @Column(name = "motivo")
    private String motivo; // Motivo del servicio

    @Column(name = "diagnostico")
    private String diagnostico; // Diagnóstico (si aplica)

    @Column(name = "tratamiento")
    private String tratamiento; // Tratamiento realizado (si aplica)

    @Column(name = "estado")
    private String estado; // Estado del servicio (finalizado, en curso, etc.)

    @Column(name = "observaciones")
    private String observaciones; // Observaciones adicionales

    @ManyToOne // Relación muchos a uno con Mascota
    @JoinColumn(name = "idMscota", nullable = false)
    private Mascota mascota;

    @ManyToOne // Relación muchos a uno con Cliente
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    public HistorialConsumo() {
    }

    public HistorialConsumo(Long idConsumo, LocalDate fechaIngreso, String tipoServicio, String motivo, String diagnostico, String tratamiento, String estado, String observaciones, Mascota mascota, Cliente cliente) {
        this.idConsumo = idConsumo;
        this.fechaIngreso = fechaIngreso;
        this.tipoServicio = tipoServicio;
        this.motivo = motivo;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.estado = estado;
        this.observaciones = observaciones;
        this.mascota = mascota;
        this.cliente = cliente;
    }

    public Long getIdConsumo() {
        return idConsumo;
    }

    public void setIdConsumo(Long idConsumo) {
        this.idConsumo = idConsumo;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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

    public void setIdMascota(Integer idMascota) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }*/

}
