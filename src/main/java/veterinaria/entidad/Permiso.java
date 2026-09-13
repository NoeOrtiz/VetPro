
package veterinaria.entidad;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "permiso")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPermiso")
    private Integer idPermiso;

    @Column(name = "nombrePermiso", nullable = false, unique = true)
    private String nombrePermiso;

    @Column(name = "descPermiso", nullable = false)
    private String descPermiso;

    @Column(name = "tipoPermiso")
    private String tipoPermiso;

    @OneToMany(mappedBy = "permiso")
    private List<RolPermiso> rolPermisos = new java.util.ArrayList<>();    
    
    public Integer getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(Integer idPermiso) {
        this.idPermiso = idPermiso;
    }

    public String getNombrePermiso() {
        return nombrePermiso;
    }

    public void setNombrePermiso(String nombrePermiso) {
        this.nombrePermiso = nombrePermiso;
    }

    public String getDescPermiso() {
        return descPermiso;
    }

    public void setDescPermiso(String descPermiso) {
        this.descPermiso = descPermiso;
    }

    public String getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(String tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public List<RolPermiso> getRolPermisos() {
        return rolPermisos;
    }

    public void setRolPermisos(List<RolPermiso> rolPermisos) {
        this.rolPermisos = rolPermisos;
    }

}