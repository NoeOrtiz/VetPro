
package veterinaria.util.state;

import java.util.List;
import veterinaria.entidad.RolPermiso;
import veterinaria.util.SesionUsuario;

public class EstadoAsistente implements EstadoUsuario {
    private static final SesionUsuario sesionUsuario = SesionUsuario.getInstancia();
    private final List<RolPermiso> rolPermisos;

    public EstadoAsistente(List<RolPermiso> rolPermisos) {
        this.rolPermisos = rolPermisos;
    }

    @Override
    public boolean tienePermiso(String nombrePermiso) {
        List<RolPermiso> rolPermisosUsuario = sesionUsuario.getPermisos();
        for (RolPermiso rolPermiso : rolPermisosUsuario) {
            String permisoActual = rolPermiso.getPermiso().getNombrePermiso();
            boolean acceso = rolPermiso.getAcceso();

            if (permisoActual.equals(nombrePermiso) && acceso) {
                return true;
            }
        }
        return false;
    }
}