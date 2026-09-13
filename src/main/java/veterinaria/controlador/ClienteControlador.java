package veterinaria.controlador;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;
import veterinaria.persistencia.ClienteDAO;
import veterinaria.util.enums.EstadoCliente;

public class ClienteControlador {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    public boolean crearPersonaCliente(Persona persona, Cliente cliente) throws Exception {
        validarDatos(cliente);
        return clienteDAO.crear(persona, cliente);
    }

    private void validarDatos(Cliente cliente) throws Exception {
        if (cliente.getEmail() == null || cliente.getEmail().isEmpty()) {
            throw new Exception("El email no puede estar vacío.");
        }
    }

    public List<Cliente> buscarTodosLosClientes() {
        return clienteDAO.buscarTodos();
    }

    public List<Cliente> buscarClientesActivos() {
        return clienteDAO.buscarActivos();
    }

    public List<Cliente> buscarClientesSinCuentaCorriente() {
        return clienteDAO.buscarClientesSinCuentaCorriente();
    }

    public Cliente buscarPorEmail(String cliente) {
        return clienteDAO.buscarPorEmail(cliente);
    }

    public Cliente buscarPorId(Integer idCliente) {
        return clienteDAO.buscarPorId(idCliente);
    }

    public Boolean actualizarCliente(Persona persona, Cliente cliente) {
        return clienteDAO.actualizar(persona, cliente);
    }

    public boolean eliminarCliente(Persona persona, Cliente cliente) {
        try {
            return clienteDAO.eliminar(persona, cliente);
        } catch (Exception ex) {
            Logger.getLogger(ClienteControlador.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    // Adaptado a campos Booleanos y LocalDate de la Entidad
    public boolean procesarGuardado(
            String estadoUsuario, Cliente clienteEdicion, Persona personaEdicion,
            String nombre, String apellido, String dni, String direccion,
            String telefono, String email, String cuit, String razonSocial, String estadoFormulario) throws Exception {

        // Convertimos el String del combo ("ACTIVO"/"INACTIVO") a boolean para tu entidad
        //boolean esActivo = "ACTIVO".equalsIgnoreCase(estadoFormulario);
        boolean esActivo = EstadoCliente.ACTIVO.getDescripcion().equalsIgnoreCase(estadoFormulario);

        if ("CreandoCliente".equals(estadoUsuario)) {
            Persona nuevaPersona = new Persona();
            nuevaPersona.setNombre(nombre);
            nuevaPersona.setApellido(apellido);
            nuevaPersona.setDni(dni);
            nuevaPersona.setDireccion(direccion);
            nuevaPersona.setTelefono(telefono);

            Cliente nuevoCliente = new Cliente();
            nuevoCliente.setPersona(nuevaPersona);
            nuevoCliente.setEmail(email);
            nuevoCliente.setRazonSocial(razonSocial);
            nuevoCliente.setCuit(cuit);

            // Sincronizado con Entidad Cliente
            nuevoCliente.setActivo(esActivo);
            nuevoCliente.setEstado(esActivo ? "ACTIVO" : "INACTIVO");
            nuevoCliente.setFechaAlta(java.time.LocalDateTime.now());

            return this.crearPersonaCliente(nuevaPersona, nuevoCliente);
        }

        if ("EditandoCliente".equals(estadoUsuario)) {
            if (personaEdicion == null || clienteEdicion == null) {
                throw new Exception("No hay un cliente seleccionado para editar.");
            }

            personaEdicion.setNombre(nombre);
            personaEdicion.setApellido(apellido);
            personaEdicion.setDni(dni);
            personaEdicion.setDireccion(direccion);
            personaEdicion.setTelefono(telefono);

            clienteEdicion.setEmail(email);
            clienteEdicion.setRazonSocial(razonSocial);
            clienteEdicion.setCuit(cuit);

            // Sincronizado con Entidad Cliente
            clienteEdicion.setActivo(esActivo);
            clienteEdicion.setEstado(esActivo ? "ACTIVO" : "INACTIVO");

            return this.actualizarCliente(personaEdicion, clienteEdicion);
        }

        return false;
    }

    public boolean procesarBajaPorId(Integer idCliente) throws Exception {
        if (idCliente == null) {
            throw new Exception("El ID del cliente no es válido.");
        }

        // El controlador maneja los DAOs
        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            throw new Exception("No se encontró el cliente seleccionado.");
        }

        if (cliente.getPersona() == null || cliente.getPersona().getIdPersona() == null) {
            throw new Exception("El cliente no tiene una persona asociada válida.");
        }

        Persona persona = cliente.getPersona();

        // Ejecutamos la baja física o lógica usando el método existente
        return this.eliminarCliente(persona, cliente);
    }

    public Cliente obtenerClienteParaEdicion(Integer idCliente) throws Exception {
        if (idCliente == null) {
            throw new Exception("No se pudo determinar el ID del cliente seleccionado.");
        }

        Cliente cliente = this.buscarPorId(idCliente);
        if (cliente == null) {
            throw new Exception("No se encontró el cliente seleccionado.");
        }

        if (cliente.getPersona() == null) {
            throw new Exception("No se encontró la persona asociada al cliente.");
        }

        return cliente;
    }

    public Cliente obtenerClienteParaReporte(Integer idCliente) throws Exception {
        if (idCliente == null) {
            throw new Exception("ID inválido");
        }

        // Aquí el controlador se encarga de usar el DAO de forma segura
        Cliente c = clienteDAO.buscarPorId(idCliente);
        if (c == null) {
            throw new Exception("No se encontraron los datos del cliente.");
        }

        return c;
    }

    public List<Cliente> buscarClientePorEstado(String estadoFiltro) {
        return clienteDAO.buscarPorEstado(estadoFiltro);
    }
}
