package veterinaria.util;

import java.lang.reflect.Field;
import javax.swing.AbstractButton;

public final class PermisoUI {

    private PermisoUI() {}

    public static void aplicar(Object form) {
        if (form == null) return;
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (sesion == null || sesion.getUsuario() == null) return;

        String formName = form.getClass().getSimpleName();

        for (Field f : form.getClass().getDeclaredFields()) {
            try {
                if (!AbstractButton.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                f.setAccessible(true);
                AbstractButton btn = (AbstractButton) f.get(form);
                if (btn == null) continue;

                String action = mapActionFromFieldName(f.getName());
                if (action == null) continue; // no mapeado => no tocar

                String permKey = formName + "." + action;

                boolean allowed = sesion.puede(permKey);

                if (isDestructive(action)) {
                    btn.setVisible(allowed);
                } else {
                    btn.setEnabled(allowed);
                }

            } catch (Exception ex) {
            }
        }
    }

    private static boolean isDestructive(String action) {
        return "ELIMINAR".equals(action) || "ANULAR".equals(action) || "QUITAR".equals(action);
    }

    private static String mapActionFromFieldName(String fieldName) {
        if (fieldName == null) return null;
        String n = fieldName.toLowerCase();

        if (n.contains("cerrarcaja")) return "CERRAR_CAJA";
        if (n.contains("abrircaja")) return "ABRIR_CAJA";

        
        if (n.contains("registrarventa") || n.contains("confirmarventa")) return "REGISTRAR_VENTA";
        if (n.contains("registrarcobro")) return "REGISTRAR_COBRO_CC";
        if (n.contains("agregarproductoventa")) return "AGREGAR_PRODUCTO_VENTA";
        if (n.contains("agregarmetodopago")) return "AGREGAR_METODO_PAGO";
        if (n.contains("guardar") || n.contains("registrar") || n.contains("confirmar") || n.contains("grabar")) return "GUARDAR";
        if (n.contains("eliminar") || n.contains("borrar")) return "ELIMINAR";
        if (n.contains("anular")) return "ANULAR";
        if (n.contains("editar") || n.contains("modificar") || n.contains("actualizar")) return "EDITAR";
        if (n.contains("nuevo")) return "NUEVO";
        if (n.contains("agregar") || n.contains("add")) return "AGREGAR";
        if (n.contains("quitar") || n.contains("remover") || n.contains("remove")) return "QUITAR";
        if (n.contains("imprimir") || n.contains("print")) return "IMPRIMIR";
        if (n.contains("export")) return "EXPORTAR";

        return null;
    }
}
