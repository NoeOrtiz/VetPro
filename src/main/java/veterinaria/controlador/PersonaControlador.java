
package veterinaria.controlador;

import veterinaria.entidad.Persona;
import veterinaria.persistencia.PersonaDAO;

public class PersonaControlador {
    private final PersonaDAO personaDAO = new PersonaDAO();
    
    public Persona crearPersona(Persona persona) {
        personaDAO.crear(persona);
        return persona;
    }
    
    public Persona actualizarPersona(Persona persona){
        personaDAO.actualizar(persona);        
        return persona;
    }
    
    public void buscarPorId(int idPersona) {
        personaDAO.obtenerPorId(idPersona);
    }
    
    public void eliminarPersona(Integer idPersona){
        personaDAO.eliminar(idPersona);
    }
}
