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

    public boolean eliminar(Proveedor proveedor) {
        return txRunner.runInTx(em -> {
            Integer idProveedor = proveedor != null ? proveedor.getIdProveedor() : null;
            if (idProveedor == null) {
                throw new IllegalArgumentException("El idProveedor no puede ser null");
            }

            Proveedor managed = em.find(Proveedor.class, idProveedor);
            Integer idPersona = null;
            if (managed != null && managed.getPersona() != null) {
                idPersona = managed.getPersona().getIdPersona();
            }

            if (managed != null) {
                em.remove(managed);
            }

            personaService.deleteIfUnused(em, idPersona);
            return true;
        });
    }
}