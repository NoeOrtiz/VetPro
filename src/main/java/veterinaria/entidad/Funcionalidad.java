
package veterinaria.entidad;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "funcionalidad")
public class Funcionalidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idFuncionalidad")    
    private Integer idFuncionalidad;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRol", referencedColumnName = "idRol")
    private Rol idRol;
    
    @Column(name = "funcionalidad")
    private String funcionalidad;

    public Funcionalidad() {
    }

    public Funcionalidad(Integer idFuncionalidad, Rol idRol, String funcionalidad) {
        this.idFuncionalidad = idFuncionalidad;
        this.idRol = idRol;
        this.funcionalidad = funcionalidad;
    }

    public Integer getIdFuncionalidad() {
        return idFuncionalidad;
    }

    public void setIdFuncionalidad(Integer idFuncionalidad) {
        this.idFuncionalidad = idFuncionalidad;
    }

    public Rol getIdRol() {
        return idRol;
    }

    public void setIdRol(Rol idRol) {
        this.idRol = idRol;
    }

    public String getFuncionalidad() {
        return funcionalidad;
    }

    public void setFuncionalidad(String funcionalidad) {
        this.funcionalidad = funcionalidad;
    }
    
    @Override
    public String toString() {
        return "Funcionalidad{" +
                "idFuncionalidad=" + idFuncionalidad +
                ", idRol=" + (idRol != null ? idRol.getIdRol() : null) +
                ", funcionalidad='" + funcionalidad + '\'' +
                '}';
    }
}
