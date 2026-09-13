package veterinaria.entidad;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "historia_evento")
public class HistoriaEvento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEvento")
    private Integer idEvento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "tipo", length = 50)
    private String tipo;

    @Column(name = "profesional", length = 255)
    private String profesional;

    @Column(name = "resumen", length = 255)
    private String resumen;

    @Lob
    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "refTabla", length = 50)
    private String refTabla;

    @Column(name = "refId")
    private Integer refId;

    public HistoriaEvento() {
    }

    public HistoriaEvento(Mascota mascota, LocalDateTime fecha, String tipo, String profesional, String resumen, String observaciones, String refTabla, Integer refId) {
        this.mascota = mascota;
        this.fecha = fecha;
        this.tipo = tipo;
        this.profesional = profesional;
        this.resumen = resumen;
        this.observaciones = observaciones;
        this.refTabla = refTabla;
        this.refId = refId;
    }

    public Integer getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Integer idEvento) {
        this.idEvento = idEvento;
    }

    public Mascota getMascota() {
        return mascota;
    }

    public void setMascota(Mascota mascota) {
        this.mascota = mascota;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getProfesional() {
        return profesional;
    }

    public void setProfesional(String profesional) {
        this.profesional = profesional;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getRefTabla() {
        return refTabla;
    }

    public void setRefTabla(String refTabla) {
        this.refTabla = refTabla;
    }

    public Integer getRefId() {
        return refId;
    }

    public void setRefId(Integer refId) {
        this.refId = refId;
    }
}
