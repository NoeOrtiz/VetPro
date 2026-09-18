package veterinaria.servicio;

import veterinaria.entidad.Persona;
import veterinaria.entidad.Proveedor;
import veterinaria.servicio.AuditoriaService;

public class ProveedorService {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    private final TxRunner txRunner = new TxRunner();
    private final PersonaService personaService = new PersonaService();

    public boolean crear(Persona persona, Proveedor proveedor) {
        return txRunner.runInTx(em -> {
            Persona personaManaged = personaService.persistOrAttach(em, persona);
            proveedor.setPersona(personaManaged);
            if (proveedor.getEstado() == null || proveedor.getEstado().trim().isEmpty()) {
                proveedor.setEstado("Activo");
            }
            em.persist(proveedor);
            auditoriaService.registrar("CREATE", "Proveedor", (proveedor!=null? Long.valueOf(proveedor.getIdProveedor()): null), "ProveedorService", "Alta de proveedor: " + (proveedor!=null? proveedor.getRazonSocial():""), AuditoriaService.RESULT_OK, null, null, null);
            return true;
        });
    }

    public boolean actualizar(Persona persona, Proveedor proveedor) {
        return txRunner.runInTx(em -> {
            Persona personaManaged = personaService.merge(em, persona);
            proveedor.setPersona(personaManaged);
            em.merge(proveedor);
            auditoriaService.registrar("UPDATE", "Proveedor", (proveedor!=null? Long.valueOf(proveedor.getIdProveedor()): null), "ProveedorService", "Actualización de proveedor: " + (proveedor!=null? proveedor.getRazonSocial():""), AuditoriaService.RESULT_OK, null, null, null);
            return true;
        });
    }

    /**
     * Compatibilidad con las pantallas actuales: eliminar ahora significa
     * desactivar. El proveedor y su Persona se conservan para mantener la
     * trazabilidad de compras, productos y movimientos históricos.
     */
    public boolean eliminar(Proveedor proveedor) {
        return desactivar(proveedor);
    }

    public boolean desactivar(Proveedor proveedor) {
        return cambiarEstado(proveedor, "Inactivo", "DESACTIVAR");
    }

    public boolean reactivar(Proveedor proveedor) {
        return cambiarEstado(proveedor, "Activo", "REACTIVAR");
    }

    private boolean cambiarEstado(Proveedor proveedor, String estado, String accionAuditoria) {
        if (proveedor == null || proveedor.getIdProveedor() == null) {
            throw new IllegalArgumentException("El proveedor es requerido");
        }

        return txRunner.runInTx(em -> {
            Proveedor managed = em.find(Proveedor.class, proveedor.getIdProveedor());
            if (managed == null) {
                throw new IllegalArgumentException("El proveedor no existe");
            }

            managed.setEstado(estado);
            em.merge(managed);
            proveedor.setEstado(estado);

            auditoriaService.registrar(
                    accionAuditoria,
                    "Proveedor",
                    Long.valueOf(managed.getIdProveedor()),
                    "ProveedorService",
                    ("Activo".equals(estado) ? "Reactivación de proveedor: " : "Desactivación de proveedor: ")
                            + managed.getRazonSocial(),
                    AuditoriaService.RESULT_OK,
                    null,
                    null,
                    null
            );
            return true;
        });
    }

}