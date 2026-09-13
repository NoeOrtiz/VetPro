
package veterinaria.entidad;

import javax.persistence.*;

@Entity
@Table(name = "rol_permiso")
public class RolPermiso {

    @EmbeddedId
    private RolPermisoId id;

    @ManyToOne
    @MapsId("idRol")
    @JoinColumn(name = "idRol")
    private Rol rol;

    @ManyToOne
    @MapsId("idPermiso")
    @JoinColumn(name = "idPermiso")
    private Permiso permiso;

    @Column(name = "acceso", nullable = false)
    private Boolean acceso;

    public RolPermiso() {}

    public RolPermiso(Rol rol, Permiso permiso, Boolean acceso) {
        this.rol = rol;
        this.permiso = permiso;
        this.acceso = acceso;
        if (rol != null && permiso != null) {
            this.id = new RolPermisoId(rol.getIdRol(), permiso.getIdPermiso());
        }
    }

    public RolPermisoId getId() {
        return id;
    }

    public void setId(RolPermisoId id) {
        this.id = id;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Permiso getPermiso() {
        return permiso;
    }

    public void setPermiso(Permiso permiso) {
        this.permiso = permiso;
    }

    public Boolean getAcceso() {
        return acceso;
    }

    public void setAcceso(Boolean acceso) {
        this.acceso = acceso;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolPermiso that = (RolPermiso) o;
        if (id == null || that.id == null) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}