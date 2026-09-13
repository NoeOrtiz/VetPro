package veterinaria.bootstrap;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import veterinaria.entidad.Configuracion;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Permiso;
import veterinaria.entidad.Rol;
import veterinaria.entidad.RolPermiso;
import veterinaria.entidad.RolPermisoId;
import veterinaria.persistencia.JPAUtil;
import veterinaria.util.Constantes;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DataBootstrap {

    private static final Logger LOG = Logger.getLogger(DataBootstrap.class.getName());

    private DataBootstrap() {
    }

    public static void ensureBaseSecurityData() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            LOG.info("[BOOTSTRAP] Iniciando bootstrap de seguridad y datos base...");

            Rol rolAdmin = ensureRol(em, "Administrador");
            ensureRol(em, "Asistente");
            ensureRol(em, "Veterinario");

            seedPermisos(em);

            syncRolPermiso(em);

            seedConfiguraciones(em);

            seedMetodosPago(em);

            ensureProductoPrecioCostoSchema(em);

            ensureAuditoriaSchema(em);

            LOG.info("[BOOTSTRAP] Bootstrap finalizado OK.");

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOG.log(Level.SEVERE, "[BOOTSTRAP] Error ejecutando bootstrap. Se hace rollback.", e);
            throw e;
        } finally {
            em.close();
        }
    }

    private static void seedMetodosPago(EntityManager em) {
        ensureMetodoPago(em, "Pago Efectivo", "Pago en efectivo");
        ensureMetodoPago(em, "Cuenta Corriente", "Pago a cuenta corriente / fiado");
    }

    private static MetodoPago ensureMetodoPago(EntityManager em, String nombre, String descripcion) {
        try {
            return em.createQuery("SELECT m FROM MetodoPago m WHERE m.nombre = :n", MetodoPago.class)
                    .setParameter("n", nombre)
                    .getSingleResult();
        } catch (NoResultException ex) {
            MetodoPago m = new MetodoPago();
            m.setNombre(nombre);
            m.setDescripcion(descripcion);
            em.persist(m);
            em.flush();
            return m;
        }
    }

    private static void seedConfiguraciones(EntityManager em) {

        ensureConfig(em, "PELUQUERIA_TURNOS_POR_DIA", "4", "(DEPRECATED) Turnos maximos por dia");
        ensureConfig(em, "PELUQUERIA_TURNOS_MANANA", "2", "Turnos maximos por la manana");
        ensureConfig(em, "PELUQUERIA_TURNOS_TARDE", "2", "Turnos maximos por la tarde");

        ensureConfig(em, "PELUQUERIA_DURACION_TURNO_MIN", "60", "Duracion del turno en minutos");

        ensureConfig(em, "PELUQUERIA_HORARIO_DESDE", "09:00", "(DEPRECATED) Horario desde (HH:mm)");
        ensureConfig(em, "PELUQUERIA_HORARIO_HASTA", "11:00", "(DEPRECATED) Horario hasta (HH:mm)");

        ensureConfig(em, "PELUQUERIA_HORARIO_MANANA_DESDE", "09:00", "Horario manana desde (HH:mm)");
        ensureConfig(em, "PELUQUERIA_HORARIO_MANANA_HASTA", "11:00", "Horario manana hasta (HH:mm)");
        ensureConfig(em, "PELUQUERIA_HORARIO_TARDE_DESDE", "17:00", "Horario tarde desde (HH:mm)");
        ensureConfig(em, "PELUQUERIA_HORARIO_TARDE_HASTA", "20:00", "Horario tarde hasta (HH:mm)");

        ensureConfig(em, "PELUQUERIA_DIAS_HABILITADOS", "1,2,3,4,5", "Dias habilitados (1=Lun..7=Dom)");

        ensureConfig(em, "LABORATORIO_DIAS_HABILITADOS", "1,3,5", "Laboratorio - dias habilitados (1=Lun..7=Dom)");
        ensureConfig(em, "LABORATORIO_DURACION_TURNO_MIN", "30", "Laboratorio - duracion del turno en minutos");

        ensureConfig(em, "HOSPITALIZACION_DIAS_HABILITADOS", "2,4,6", "Hospitalizacion - dias habilitados (1=Lun..7=Dom)");
        ensureConfig(em, "HOSPITALIZACION_DURACION_TURNO_MIN", "60", "Hospitalizacion - duracion del turno en minutos");

    }

    private static void ensureConfig(EntityManager em, String clave, String valorDefault, String descripcion) {
        try {
            em.createQuery("SELECT c FROM Configuracion c WHERE c.clave = :k", Configuracion.class)
                    .setParameter("k", clave)
                    .getSingleResult();
            return;
        } catch (NoResultException ex) {
            Configuracion c = new Configuracion();
            c.setClave(clave);
            c.setValor(valorDefault);
            c.setDescripcion(descripcion);
            em.persist(c);
        }
    }

    private static Rol ensureRol(EntityManager em, String nombreRol) {
        try {
            return em.createQuery("SELECT r FROM Rol r WHERE r.nombreRol = :n", Rol.class)
                    .setParameter("n", nombreRol)
                    .getSingleResult();
        } catch (NoResultException ex) {
            Rol r = new Rol();
            r.setNombreRol(nombreRol);
            em.persist(r);
            em.flush();
            return r;
        }
    }

    private static void seedPermisos(EntityManager em) {
        if (seedPermisosFromResource(em)) {
            return;
        }

        Set<String> permisos = new LinkedHashSet<>();

        List<Class<?>> forms = getFormsList();
        for (Class<?> formClass : forms) {
            String formName = formClass.getSimpleName();
            permisos.add(formName);

            for (Field f : formClass.getDeclaredFields()) {
                if (!JButton.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                String action = mapActionFromFieldName(f.getName());
                if (action != null) {
                    permisos.add(formName + "." + action);
                }
            }
        }

        permisos.add("ClassConfig");
        permisos.add("FormConfiguracion");

        for (String nombrePermiso : permisos) {
            ensurePermiso(em, nombrePermiso, null, null);
        }
    }

    private static void ensurePermiso(EntityManager em, String nombrePermiso, String desc, String tipo) {
        Permiso p;
        try {
            p = em.createQuery("SELECT p FROM Permiso p WHERE p.nombrePermiso = :n", Permiso.class)
                    .setParameter("n", nombrePermiso)
                    .getSingleResult();
        } catch (NoResultException ex) {
            p = null;
        }

        if (p != null) {
            return;
        }

        Permiso nuevo = new Permiso();
        nuevo.setNombrePermiso(nombrePermiso);

        if (tipo != null && !tipo.trim().isEmpty()) {
            nuevo.setTipoPermiso(tipo);
        } else {
            nuevo.setTipoPermiso(nombrePermiso.contains(".")
                    ? Constantes.TipoPermiso.FUNCIONALIDAD.name()
                    : Constantes.TipoPermiso.FORMULARIO.name());
        }

        if (desc != null && !desc.trim().isEmpty()) {
            nuevo.setDescPermiso(desc);
        } else {
            if (nombrePermiso.contains(".")) {
                String[] parts = nombrePermiso.split("\\.");
                String form = parts.length > 0 ? parts[0] : "Formulario";
                String action = parts.length > 1 ? parts[1] : "ACCION";
                nuevo.setDescPermiso(actionToDesc(action) + " en " + prettyFormName(form));
            } else {
                nuevo.setDescPermiso("Acceso al formulario de " + prettyFormName(nombrePermiso));
            }
        }

        em.persist(nuevo);
        em.flush();
    }

    private static boolean seedPermisosFromResource(EntityManager em) {
        try {
            InputStream is = DataBootstrap.class.getClassLoader().getResourceAsStream("bootstrap/permisos_seed.csv");
            if (is == null) {
                return false;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = br.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split(";", -1);
                    if (parts.length < 1) {
                        continue;
                    }
                    String nombre = parts[0].trim();
                    String desc = parts.length > 1 ? parts[1].trim() : null;
                    String tipo = parts.length > 2 ? parts[2].trim() : null;
                    if (!nombre.isEmpty()) {
                        ensurePermiso(em, nombre, desc, tipo);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void syncRolPermiso(EntityManager em) {
        Long total = em.createQuery("SELECT COUNT(rp) FROM RolPermiso rp", Long.class).getSingleResult();
        LOG.info("[BOOTSTRAP] rol_permiso existentes: " + (total == null ? 0 : total));

        boolean seeded = seedRolPermisoFromResourceIdempotent(em);
        if (seeded) {
            LOG.info("[BOOTSTRAP] rol_permiso sincronizado desde CSV (idempotente).");
            return;
        }

        Rol admin = ensureRol(em, "Administrador");
        Rol asist = ensureRol(em, "Asistente");
        Rol vet = ensureRol(em, "Veterinario");

        List<Permiso> allPerms = em.createQuery("SELECT p FROM Permiso p", Permiso.class).getResultList();

        int created = 0;
        created += ensureRoleMatrixDefault(em, admin, allPerms);
        created += ensureRoleMatrixDefault(em, asist, allPerms);
        created += ensureRoleMatrixDefault(em, vet, allPerms);

        LOG.info("[BOOTSTRAP] rol_permiso default sincronizado. Nuevos registros creados: " + created);
    }

    private static boolean seedRolPermisoFromResourceIdempotent(EntityManager em) {
        try {
            InputStream is = DataBootstrap.class.getClassLoader().getResourceAsStream("bootstrap/rol_permiso_seed.csv");
            if (is == null) {
                return false;
            }

            int created = 0;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = br.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split(";", -1);
                    if (parts.length < 3) {
                        continue;
                    }
                    String nombreRol = parts[0].trim();
                    String nombrePermiso = parts[1].trim();
                    String accesoRaw = parts[2].trim();
                    boolean acceso = "1".equals(accesoRaw)
                            || "true".equalsIgnoreCase(accesoRaw)
                            || "b'1'".equalsIgnoreCase(accesoRaw);

                    Rol rol = ensureRol(em, nombreRol);
                    Permiso permiso;
                    try {
                        permiso = em.createQuery("SELECT p FROM Permiso p WHERE p.nombrePermiso = :n", Permiso.class)
                                .setParameter("n", nombrePermiso)
                                .getSingleResult();
                    } catch (NoResultException ex) {
                        ensurePermiso(em, nombrePermiso, null, null);
                        permiso = em.createQuery("SELECT p FROM Permiso p WHERE p.nombrePermiso = :n", Permiso.class)
                                .setParameter("n", nombrePermiso)
                                .getSingleResult();
                    }

                    if (ensureRolPermisoIfMissing(em, rol, permiso, acceso)) {
                        created++;
                    }
                }
            }

            LOG.info("[BOOTSTRAP] CSV rol_permiso procesado. Nuevos registros: " + created);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[BOOTSTRAP] No se pudo cargar rol_permiso_seed.csv. Se aplica fallback default.", e);
            return false;
        }
    }

    private static int ensureRoleMatrixDefault(EntityManager em, Rol rol, List<Permiso> permisos) {
        if (rol == null || rol.getIdRol() == null) {
            return 0;
        }

        List<Integer> existing = em.createQuery(
                "SELECT rp.permiso.idPermiso FROM RolPermiso rp WHERE rp.rol.idRol = :rid",
                Integer.class)
                .setParameter("rid", rol.getIdRol())
                .getResultList();
        HashSet<Integer> existingSet = new HashSet<>(existing);

        int created = 0;
        for (Permiso p : permisos) {
            if (p == null || p.getIdPermiso() == null) {
                continue;
            }
            if (existingSet.contains(p.getIdPermiso())) {
                continue; // ya existe
            }

            boolean acceso = defaultAccessForRole(rol.getNombreRol(), p.getNombrePermiso());
            RolPermiso rp = new RolPermiso();
            rp.setRol(rol);
            rp.setPermiso(p);
            rp.setAcceso(acceso);
            rp.setId(new RolPermisoId(rol.getIdRol(), p.getIdPermiso()));
            em.persist(rp);
            created++;
        }

        if (created > 0) {
            LOG.info("[BOOTSTRAP] Se asignaron " + created + " permisos faltantes al rol '" + rol.getNombreRol() + "'.");
        } else {
            LOG.info("[BOOTSTRAP] Rol '" + rol.getNombreRol() + "' ya tenía permisos (o no había nuevos para asignar).");
        }
        return created;
    }

    private static boolean ensureRolPermisoIfMissing(EntityManager em, Rol rol, Permiso permiso, boolean acceso) {
        if (rol == null || permiso == null || rol.getIdRol() == null || permiso.getIdPermiso() == null) {
            return false;
        }

        Long exists = em.createQuery(
                "SELECT COUNT(rp) FROM RolPermiso rp WHERE rp.id.idRol = :rid AND rp.id.idPermiso = :pid",
                Long.class)
                .setParameter("rid", rol.getIdRol())
                .setParameter("pid", permiso.getIdPermiso())
                .getSingleResult();

        if (exists != null && exists > 0) {
            return false;
        }

        RolPermiso rp = new RolPermiso();
        rp.setRol(rol);
        rp.setPermiso(permiso);
        rp.setAcceso(acceso);
        rp.setId(new RolPermisoId(rol.getIdRol(), permiso.getIdPermiso()));
        em.persist(rp);
        return true;
    }

    private static boolean defaultAccessForRole(String nombreRol, String nombrePermiso) {
        if (nombreRol == null || nombrePermiso == null) {
            return false;
        }

        if ("Administrador".equalsIgnoreCase(nombreRol)) {
            return true;
        }

        String form = nombrePermiso;
        int dot = nombrePermiso.indexOf('.');
        if (dot > 0) {
            form = nombrePermiso.substring(0, dot);
        }

        boolean isSecurityOrConfig = "FormUsuario".equalsIgnoreCase(form)
                || "ClassConfig".equalsIgnoreCase(form)
                || "FormConfiguracion".equalsIgnoreCase(form);

        if ("Asistente".equalsIgnoreCase(nombreRol)) {
            if (isSecurityOrConfig) {
                return false;
            }
            return true;
        }

        if ("Veterinario".equalsIgnoreCase(nombreRol)) {
            if (isSecurityOrConfig) {
                return false;
            }

            if (form.startsWith("FormCaja")
                    || form.startsWith("FormInformesCaja")
                    || form.startsWith("FormProducto")
                    || form.startsWith("FormStock")
                    || form.startsWith("FormProveedores")
                    || form.startsWith("FormOrdenesCompra")
                    || form.startsWith("FormRecibirPedido")
                    || form.startsWith("FormGestionCuentasCorrientes")
                    || form.startsWith("FormCuentaCorriente")) {
                return false;
            }

            if (form.startsWith("FormCliente")
                    || form.startsWith("FormMascota")
                    || form.startsWith("FormVisita")
                    || form.startsWith("FormHistoriaClinica")
                    || form.startsWith("FormLaboratorio")
                    || form.startsWith("FormHospitalizaciones")
                    || form.startsWith("FormProcedimientos")
                    || form.startsWith("FormTurnosPeluqueria")
                    || form.startsWith("FormHistorialTurnos")
                    || form.startsWith("FormAccesoRestringido")) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static List<Class<?>> getFormsList() {
        List<Class<?>> list = new ArrayList<>();

        list.addAll(Arrays.asList(
                veterinaria.vista.FormUsuario.class,
                veterinaria.vista.FormCliente.class,
                veterinaria.vista.FormMascota.class,
                veterinaria.vista.FormConsulta.class,
                veterinaria.vista.FormHistoriaClinica.class,
                veterinaria.vista.FormGestionCuentasCorrientes.class,
                veterinaria.vista.FormCuentaCorrienteMovimientos.class,
                veterinaria.vista.FormCajaRegistradora.class,
                veterinaria.vista.FormInformesCajaMovimientos.class,
                veterinaria.vista.FormProducto.class,
                veterinaria.vista.FormStockProducto.class,
                veterinaria.vista.FormProveedores.class,
                veterinaria.vista.FormOrdenesCompra.class,
                veterinaria.vista.FormRecibirPedido.class,
                veterinaria.vista.FormLaboratorio.class,
                veterinaria.vista.FormHospitalizaciones.class,
                veterinaria.vista.FormProcedimientos.class,
                veterinaria.vista.FormTurnosPeluqueria.class,
                veterinaria.vista.FormHistorialTurnos.class,
                veterinaria.vista.PanelTiposCitaPeluqueria.class,
                veterinaria.vista.ClassConfig.class,
                veterinaria.vista.FormAuditoria.class,
                veterinaria.vista.FormAccesoRestringido.class
        ));
        return list;
    }

    private static String prettyFormName(String formSimpleName) {
        if (formSimpleName == null) {
            return "Formulario";
        }
        String n = formSimpleName;
        if (n.startsWith("Form")) {
            n = n.substring(4);
        }
        if (n.startsWith("Class")) {
            n = n.substring(5);
        }
        if (n.isEmpty()) {
            return "Formulario";
        }
        StringBuilder sb = new StringBuilder();
        char[] arr = n.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char c = arr[i];
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(arr[i - 1])) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private static String actionToDesc(String action) {
        if (action == null) {
            return "Acción";
        }
        switch (action) {
            case "GUARDAR":
                return "Guardar/Registrar";
            case "EDITAR":
                return "Editar/Modificar";
            case "ELIMINAR":
                return "Eliminar";
            case "NUEVO":
                return "Nuevo";
            case "IMPRIMIR":
                return "Imprimir";
            case "EXPORTAR":
                return "Exportar";
            case "REGISTRAR_VENTA":
                return "Registrar venta";
            case "REGISTRAR_COBRO_CC":
                return "Registrar cobro (CC)";
            case "AGREGAR_PRODUCTO_VENTA":
                return "Agregar producto a venta";
            case "AGREGAR_METODO_PAGO":
                return "Agregar método de pago";
            case "ABRIR_CAJA":
                return "Abrir caja";
            case "CERRAR_CAJA":
                return "Cerrar caja";
            case "ANULAR":
                return "Anular";
            case "AGREGAR":
                return "Agregar";
            case "QUITAR":
                return "Quitar";
            default:
                return "Permite " + action;
        }
    }

    private static String mapActionFromFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        String n = fieldName.toLowerCase();

        if (n.contains("cerrarcaja")) {
            return "CERRAR_CAJA";
        }
        if (n.contains("abrircaja")) {
            return "ABRIR_CAJA";
        }

        if (n.contains("registrarventa") || n.contains("confirmarventa")) {
            return "REGISTRAR_VENTA";
        }
        if (n.contains("registrarcobro")) {
            return "REGISTRAR_COBRO_CC";
        }
        if (n.contains("agregarproductoventa")) {
            return "AGREGAR_PRODUCTO_VENTA";
        }
        if (n.contains("agregarmetodopago")) {
            return "AGREGAR_METODO_PAGO";
        }

        if (n.contains("guardar") || n.contains("registrar") || n.contains("confirmar") || n.contains("grabar")) {
            return "GUARDAR";
        }
        if (n.contains("eliminar") || n.contains("borrar")) {
            return "ELIMINAR";
        }
        if (n.contains("anular")) {
            return "ANULAR";
        }
        if (n.contains("editar") || n.contains("modificar") || n.contains("actualizar")) {
            return "EDITAR";
        }
        if (n.contains("nuevo")) {
            return "NUEVO";
        }
        if (n.contains("agregar") || n.contains("add")) {
            return "AGREGAR";
        }
        if (n.contains("quitar") || n.contains("remover") || n.contains("remove")) {
            return "QUITAR";
        }
        if (n.contains("imprimir") || n.contains("print")) {
            return "IMPRIMIR";
        }
        if (n.contains("export")) {
            return "EXPORTAR";
        }

        return null;
    }

    private static void ensureAuditoriaSchema(EntityManager em) {
        try {
            List<?> t = em.createNativeQuery("SHOW TABLES LIKE 'auditoria'").getResultList();
            if (t == null || t.isEmpty()) {
                return;
            }

            // 1. Verificamos si la columna id_auditoria ya existe
            List<?> colIdAud = em.createNativeQuery("SHOW COLUMNS FROM auditoria LIKE 'id_auditoria'").getResultList();

            // SI YA EXISTE 'id_auditoria', la tabla está lista. Salimos sin ejecutar NINGÚN ALTER TABLE.
            if (colIdAud != null && !colIdAud.isEmpty()) {
                return;
            }

            // 2. Si NO existe 'id_auditoria', pero existe la columna vieja 'id', la renombramos
            List<?> colId = em.createNativeQuery("SHOW COLUMNS FROM auditoria LIKE 'id'").getResultList();
            if (colId != null && !colId.isEmpty()) {
                em.createNativeQuery("ALTER TABLE auditoria CHANGE COLUMN id id_auditoria BIGINT NOT NULL AUTO_INCREMENT").executeUpdate();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "[BOOTSTRAP] Aviso esquema AUDITORIA: " + ex.getMessage());
        }
    }

    private static void ensureProductoPrecioCostoSchema(EntityManager em) {
        try {
            List<?> t = em.createNativeQuery("SHOW TABLES LIKE 'producto'").getResultList();
            if (t == null || t.isEmpty()) {
                return;
            }

            List<?> colCosto = em.createNativeQuery("SHOW COLUMNS FROM producto LIKE 'precio_costo'").getResultList();
            if (colCosto != null && !colCosto.isEmpty()) {
                return;
            }

            List<?> colPrecio = em.createNativeQuery("SHOW COLUMNS FROM producto LIKE 'precio'").getResultList();
            if (colPrecio != null && !colPrecio.isEmpty()) {
                em.createNativeQuery("ALTER TABLE producto CHANGE COLUMN precio precio_costo DOUBLE NULL").executeUpdate();
                LOG.info("[BOOTSTRAP] Columna producto.precio migrada a producto.precio_costo");
                return;
            }

            em.createNativeQuery("ALTER TABLE producto ADD COLUMN precio_costo DOUBLE NULL").executeUpdate();
            LOG.info("[BOOTSTRAP] Columna producto.precio_costo creada");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "[BOOTSTRAP] No se pudo asegurar esquema de PRODUCTO.precio_costo: " + ex.getMessage());
        }
    }
}
