package veterinaria.util.impresion;

import javax.swing.JTable;

@Deprecated
public final class HistorialTurnosImpresion {

    private HistorialTurnosImpresion() {
    }

    public static void imprimir(JTable tabla, String filtrosResumen) {
        veterinaria.reportes.impl.HistorialTurnosImpresion.imprimir(tabla, filtrosResumen);
    }
}
