
package veterinaria.controlador;

import veterinaria.entidad.Permiso;
import veterinaria.entidad.Rol;
import java.util.List;
import javax.persistence.EntityManager;
import veterinaria.entidad.RolPermiso;
import veterinaria.persistencia.PermisoDAO;
import veterinaria.persistencia.JPAUtil;
import veterinaria.util.Constantes.TipoPermiso;
import veterinaria.util.AppLog;

public class PermisoControlador {

    private PermisoDAO permisoDAO;

    public PermisoControlador() {
        this.permisoDAO = new PermisoDAO();
    }

    public List<RolPermiso> obtenerPermisosParaSesion(Rol rol) {
        List<RolPermiso> permisos = permisoDAO.obtenerPermisosPorRol(rol);
        return permisos;
    }
    
    public Boolean crearNuevoPermiso(List<Rol> roles, String nombre, String descPermiso, TipoPermiso tipo, Boolean acceso) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Permiso permiso = new Permiso();
            permiso.setNombrePermiso(nombre);
            permiso.setDescPermiso(descPermiso);
            permiso.setTipoPermiso(tipo.toString());
            em.persist(permiso);

            for (Rol rol : roles) {
                RolPermiso rolPermiso = new RolPermiso();
                rolPermiso.setRol(rol);
                rolPermiso.setPermiso(permiso);
                rolPermiso.setAcceso(acceso);

                permiso.getRolPermisos().add(rolPermiso);

                em.persist(rolPermiso);
            }

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            AppLog.error(PermisoControlador.class, "Error al crear un nuevo permiso", e);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            em.close();
        }
    }
}