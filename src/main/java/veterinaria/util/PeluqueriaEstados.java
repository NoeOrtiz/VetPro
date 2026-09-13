
package veterinaria.util;

import veterinaria.entidad.EstadoPeluqueriaEnum;

public final class PeluqueriaEstados {

     private PeluqueriaEstados() {
    }

    public static final EstadoPeluqueriaEnum PENDIENTE  = EstadoPeluqueriaEnum.PENDIENTE;
    public static final EstadoPeluqueriaEnum CONFIRMADO = EstadoPeluqueriaEnum.CONFIRMADO;
    public static final EstadoPeluqueriaEnum CANCELADO  = EstadoPeluqueriaEnum.CANCELADO;
    public static final EstadoPeluqueriaEnum ELIMINADO  = EstadoPeluqueriaEnum.ELIMINADO;
    public static final EstadoPeluqueriaEnum COMPLETADO = EstadoPeluqueriaEnum.COMPLETADO;

    public static boolean esTerminal(EstadoPeluqueriaEnum estado) {
        return estado == EstadoPeluqueriaEnum.ELIMINADO
                || estado == EstadoPeluqueriaEnum.CANCELADO
                || estado == EstadoPeluqueriaEnum.COMPLETADO;
    }

    public static boolean esNoEditable(EstadoPeluqueriaEnum estado) {
        return estado == EstadoPeluqueriaEnum.ELIMINADO || estado == EstadoPeluqueriaEnum.CANCELADO;
    }

    public static boolean esTerminal(String estado) {
        return esTerminal(parse(estado));
    }

    public static boolean esNoEditable(String estado) {
        return esNoEditable(parse(estado));
    }

    public static EstadoPeluqueriaEnum parse(String estado) {
        if (estado == null) return null;
        String s = estado.trim();
        if (s.isEmpty()) return null;

        String up = s.toUpperCase();

        try {
            return EstadoPeluqueriaEnum.valueOf(up);
        } catch (Exception ignore) {
        }

        if ("PENDIENTE".equals(up) || "PENDIENTE".equals(up)) return EstadoPeluqueriaEnum.PENDIENTE;
        if ("CONFIRMADO".equals(up)) return EstadoPeluqueriaEnum.CONFIRMADO;
        if ("CANCELADO".equals(up)) return EstadoPeluqueriaEnum.CANCELADO;
        if ("ELIMINADO".equals(up)) return EstadoPeluqueriaEnum.ELIMINADO;
        if ("COMPLETADO".equals(up)) return EstadoPeluqueriaEnum.COMPLETADO;

        if ("PENDIENTE".equals(up)) return EstadoPeluqueriaEnum.PENDIENTE;
        if ("CONFIRMADO".equals(up)) return EstadoPeluqueriaEnum.CONFIRMADO;
        if ("CANCELADO".equals(up)) return EstadoPeluqueriaEnum.CANCELADO;
        if ("ELIMINADO".equals(up)) return EstadoPeluqueriaEnum.ELIMINADO;
        if ("COMPLETADO".equals(up)) return EstadoPeluqueriaEnum.COMPLETADO;

        return null;
    }
}