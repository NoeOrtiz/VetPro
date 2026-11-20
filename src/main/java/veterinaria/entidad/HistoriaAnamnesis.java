package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "historia_anamnesis")
public class HistoriaAnamnesis implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAnamnesis")
    private Long idAnamnesis;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @Column(name = "fechaRegistro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "registradoPor", length = 200)
    private String registradoPor;

    @Column(name = "dieta", columnDefinition = "TEXT")
    private String dieta;
    @Column(name = "enfermedadesPrevias", columnDefinition = "TEXT")
    private String enfermedadesPrevias;
    @Column(name = "cirugiasPrevias", columnDefinition = "TEXT")
    private String cirugiasPrevias;
    @Column(name = "esquemaVacunal", columnDefinition = "TEXT")
    private String esquemaVacunal;
    @Column(name = "desparasitacion", columnDefinition = "TEXT")
    private String desparasitacion;
    @Column(name = "tratamientosRecientes", columnDefinition = "TEXT")
    private String tratamientosRecientes;
    @Column(name = "esterilizacion", length = 30)
    private String esterilizacion;
    @Column(name = "numeroPartos", length = 40)
    private String numeroPartos;
    @Column(name = "viajesRecientes", columnDefinition = "TEXT")
    private String viajesRecientes;
    @Column(name = "conviveConAnimales", columnDefinition = "TEXT")
    private String conviveConAnimales;
    @Column(name = "comportamiento", columnDefinition = "TEXT")
    private String comportamiento;
    @Column(name = "senalesParticulares", columnDefinition = "TEXT")
    private String senalesParticulares;

    public Long getIdAnamnesis() { return idAnamnesis; }
    public Mascota getMascota() { return mascota; }
    public void setMascota(Mascota mascota) { this.mascota = mascota; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public String getDieta() { return dieta; }
    public void setDieta(String v) { dieta = v; }
    public String getEnfermedadesPrevias() { return enfermedadesPrevias; }
    public void setEnfermedadesPrevias(String v) { enfermedadesPrevias = v; }
    public String getCirugiasPrevias() { return cirugiasPrevias; }
    public void setCirugiasPrevias(String v) { cirugiasPrevias = v; }
    public String getEsquemaVacunal() { return esquemaVacunal; }
    public void setEsquemaVacunal(String v) { esquemaVacunal = v; }
    public String getDesparasitacion() { return desparasitacion; }
    public void setDesparasitacion(String v) { desparasitacion = v; }
    public String getTratamientosRecientes() { return tratamientosRecientes; }
    public void setTratamientosRecientes(String v) { tratamientosRecientes = v; }
    public String getEsterilizacion() { return esterilizacion; }
    public void setEsterilizacion(String v) { esterilizacion = v; }
    public String getNumeroPartos() { return numeroPartos; }
    public void setNumeroPartos(String v) { numeroPartos = v; }
    public String getViajesRecientes() { return viajesRecientes; }
    public void setViajesRecientes(String v) { viajesRecientes = v; }
    public String getConviveConAnimales() { return conviveConAnimales; }
    public void setConviveConAnimales(String v) { conviveConAnimales = v; }
    public String getComportamiento() { return comportamiento; }
    public void setComportamiento(String v) { comportamiento = v; }
    public String getSenalesParticulares() { return senalesParticulares; }
    public void setSenalesParticulares(String v) { senalesParticulares = v; }
}
