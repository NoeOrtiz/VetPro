
package veterinaria.entidad;

import java.io.Serializable;
import java.util.Objects;

public class UsuarioRolId implements Serializable {

    private Integer usuario;
    private Integer rol;

    public UsuarioRolId() {}

    public UsuarioRolId(int usuario, int rol) {
        this.usuario = usuario;
        this.rol = rol;
    }

    public int getUsuario() {
        return usuario;
    }

    public void setUsuario(int usuario) {
        this.usuario = usuario;
    }

    public int getRol() {
        return rol;
    }

    public void setRol(int rol) {
        this.rol = rol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuario, rol);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioRolId that = (UsuarioRolId) o;
        return usuario == that.usuario && rol == that.rol;
    }
}