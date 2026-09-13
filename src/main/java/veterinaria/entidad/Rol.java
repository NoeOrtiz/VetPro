
package veterinaria.entidad;

import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRol")
    private Integer idRol;

    @Column(name = "nombreRol", nullable = false, unique = true)
    private String nombreRol;

    @OneToMany(mappedBy = "rol")
    private List<RolPermiso> rolPermisos;

    public Rol() {
    }

    public Rol(Integer idRol, String nombreRol, List<RolPermiso> rolPermisos) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.rolPermisos = rolPermisos;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public List<RolPermiso> getRolPermisos() {
        return rolPermisos;
    }

    public void setRolPermisos(List<RolPermiso> rolPermisos) {
        this.rolPermisos = rolPermisos;
    }

}