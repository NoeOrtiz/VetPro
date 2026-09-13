
package veterinaria.util;

import veterinaria.entidad.EstadoPeluqueriaEnum;

@Deprecated
public final class TurnoEstados {

    private TurnoEstados() {}

    public static final EstadoPeluqueriaEnum PENDIENTE  = PeluqueriaEstados.PENDIENTE;
    public static final EstadoPeluqueriaEnum CONFIRMADO = PeluqueriaEstados.CONFIRMADO;
    public static final EstadoPeluqueriaEnum CANCELADO  = PeluqueriaEstados.CANCELADO;
    public static final EstadoPeluqueriaEnum ELIMINADO  = PeluqueriaEstados.ELIMINADO;
    public static final EstadoPeluqueriaEnum COMPLETADO = PeluqueriaEstados.COMPLETADO;

    public static boolean esTerminal(EstadoPeluqueriaEnum estado) { return PeluqueriaEstados.esTerminal(estado); }
    public static boolean esNoEditable(EstadoPeluqueriaEnum estado) { return PeluqueriaEstados.esNoEditable(estado); }
    public static boolean esTerminal(String estado) { return PeluqueriaEstados.esTerminal(estado); }
    public static boolean esNoEditable(String estado) { return PeluqueriaEstados.esNoEditable(estado); }
    public static EstadoPeluqueriaEnum parse(String estado) { return PeluqueriaEstados.parse(estado); }
}
