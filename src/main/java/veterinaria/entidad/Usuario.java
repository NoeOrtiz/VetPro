package veterinaria.entidad;

import java.time.LocalDateTime;
import javax.persistence.*;
import veterinaria.util.state.EstadoAdministrador;
import veterinaria.util.state.EstadoAsistente;
import veterinaria.util.state.EstadoVeterinario;
import veterinaria.util.state.EstadoUsuario;
import veterinaria.util.SesionUsuario;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPersona", referencedColumnName = "idPersona")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRol", nullable = false)
    private Rol rol;

    @Column(name = "email")
    private String email;

    @Column(name = "contraseña", nullable = false)
    private String contrasena;

    @Column(name = "nombreUsuario", nullable = false, unique = true)
    private String nombreUsuario;

    @Column(name = "imagen_perfil")
    private String rutaImagenPerfil;

    @Transient
    private EstadoUsuario estado;

    @Column(name = "primer_login", nullable = false)
    private boolean primerLogin;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(String nombreUsuario, String contrasena, Rol rol) {
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
        setRol(rol);  // Usa setRol para asegurar que configurarEstado() se llame
        this.primerLogin = true; // Por defecto nace requiriendo cambio de clave

    }

    public Usuario(String nombreUsuario, String contrasena, Rol rol, Persona persona, String email, String rutaImagenPerfil) {
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.persona = persona;
        this.email = email;
        this.rutaImagenPerfil = rutaImagenPerfil;
        setRol(rol);  // Usa setRol para asegurar que configurarEstado() se llame
        this.primerLogin = true; // Por defecto nace requiriendo cambio de clave

    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
        configurarEstado(); // Configura el estado cuando cambia el rol
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getRutaImagenPerfil() {
        return rutaImagenPerfil;
    }

    public void setRutaImagenPerfil(String rutaImagenPerfil) {
        this.rutaImagenPerfil = rutaImagenPerfil;
    }

    public boolean isPrimerLogin() {
        return primerLogin;
    }

    public void setPrimerLogin(boolean primerLogin) {
        this.primerLogin = primerLogin;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    private void configurarEstado() {
        if (rol != null) {
            switch (rol.getNombreRol()) {
                case "Administrador":
                    this.estado = new EstadoAdministrador(rol.getRolPermisos());
                    break;
                case "Asistente":
                    this.estado = new EstadoAsistente(rol.getRolPermisos());
                    break;
                case "Veterinario":
                    this.estado = new EstadoVeterinario(rol.getRolPermisos());
                    break;
                default:
                    this.estado = null;
            }
        } else {
        }
    }

    public boolean tienePermiso(String permiso) {
        if (estado != null) {
            return estado.tienePermiso(permiso);
        }
        return SesionUsuario.getInstancia().tienePermiso(permiso);
    }
}
