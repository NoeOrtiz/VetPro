package veterinaria.entidad;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "configuracion")
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idConfiguracion")
    private Integer idConfiguracion;

    @Column(name = "clave", unique = true, nullable = false, length = 120)
    private String clave;

    @Column(name = "valor", nullable = false, length = 500)
    private String valor;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    public Configuracion() {
    }

    public Configuracion(Integer idConfiguracion, String clave, String valor, String descripcion) {
        this.idConfiguracion = idConfiguracion;
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public Integer getIdConfiguracion() {
        return idConfiguracion;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
