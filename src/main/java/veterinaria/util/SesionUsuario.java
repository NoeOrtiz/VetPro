package veterinaria.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import veterinaria.controlador.PermisoControlador;
import veterinaria.entidad.Permiso;
import veterinaria.entidad.Rol;
import veterinaria.entidad.RolPermiso;
import veterinaria.entidad.Usuario;
import veterinaria.persistencia.PermisoDAO;
import veterinaria.util.Constantes;

public class SesionUsuario {

    private static SesionUsuario instancia;
    private Usuario usuario;
    private Rol rol;
    private List<RolPermiso> permisos;          // permisos asignados al rol (rol_permiso)
    private Set<String> permisosSistema;        // permisos existentes en el sistema (tabla permiso)
    private String estado;
    private String nombreApellido;

    private SesionUsuario() {}

    public static SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    public void iniciarSesion(Usuario usuario) {
        this.usuario = usuario;
        this.rol = (usuario != null) ? usuario.getRol() : null;
        cargarPermisos();
        this.estado = "Usuario Logeado";
        if (usuario != null && usuario.getPersona() != null) {
            this.nombreApellido = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
        } else {
            this.nombreApellido = null;
        }
    }

    private void cargarPermisos() {
        PermisoControlador permisoControlador = new PermisoControlador();
        this.permisos = (this.rol != null) ? permisoControlador.obtenerPermisosParaSesion(this.rol) : null;

        this.permisosSistema = new HashSet<>();
        try {
            PermisoDAO dao = new PermisoDAO();
            List<Permiso> todos = dao.obtenerPermisos();
            if (todos != null) {
                for (Permiso p : todos) {
                    if (p != null && p.getNombrePermiso() != null) {
                        permisosSistema.add(p.getNombrePermiso().trim().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            AppLog.error(SesionUsuario.class, "Error al cargar permisos del sistema", e);
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }

    public List<RolPermiso> getPermisos() {
        return permisos;
    }

    public void cerrarSesion() {
        usuario = null;
        rol = null;
        permisos = null;
        permisosSistema = null;
        estado = null;
        nombreApellido = null;
    }

    public String getNombreApellido() {
        return nombreApellido;
    }

    private boolean isAdministrador() {
        if (rol != null && rol.getIdRol() != null && rol.getIdRol() == 1) {
            return true;
        }
        return rol != null
                && rol.getNombreRol() != null
                && "Administrador".equalsIgnoreCase(rol.getNombreRol().trim());
    }

    private Set<String> aliasesPermiso(String nombrePermiso) {
        Set<String> aliases = new HashSet<>();
        if (nombrePermiso == null) {
            return aliases;
        }
        String key = nombrePermiso.trim();
        if (key.isEmpty()) {
            return aliases;
        }
        aliases.add(key);
        if ("ClassConfig".equalsIgnoreCase(key)) {
            aliases.add("FormConfiguracion");
        } else if ("FormConfiguracion".equalsIgnoreCase(key)) {
            aliases.add("ClassConfig");
        }
        return aliases;
    }

    public boolean tienePermiso(String nombrePermiso) {
        if (permisos == null || nombrePermiso == null) return false;
        final Set<String> buscados = aliasesPermiso(nombrePermiso);
        return permisos.stream().anyMatch(rp ->
            rp != null &&
            rp.getPermiso() != null &&
            rp.getPermiso().getNombrePermiso() != null &&
            buscados.stream().anyMatch(alias -> alias.equalsIgnoreCase(rp.getPermiso().getNombrePermiso())) &&
            Boolean.TRUE.equals(rp.getAcceso())
        );
    }

    public boolean isPermisoEnSistema(String nombrePermiso) {
        if (nombrePermiso == null) return false;
        if (permisosSistema == null) return false;
        for (String alias : aliasesPermiso(nombrePermiso)) {
            if (permisosSistema.contains(alias.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean puede(String nombrePermiso) {
        if (nombrePermiso == null) return false;
        String key = nombrePermiso.trim();
        if (key.isEmpty()) return false;

        if (isAdministrador()) {
            if (!isPermisoEnSistema(key)) {
                try {
                    PermisoControlador pc = new PermisoControlador();
                    if (permisosSistema != null) {
                        for (String alias : aliasesPermiso(key)) {
                            permisosSistema.add(alias.toLowerCase());
                        }
                    }
                } catch (Exception ex) {
                    AppLog.error(SesionUsuario.class, "Error al autoprovisionar permiso para administrador", ex);
                }
            }
            return true;
        }

        if (!isPermisoEnSistema(key)) {
            return false;
        }

        return tienePermiso(key);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
