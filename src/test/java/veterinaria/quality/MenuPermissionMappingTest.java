package veterinaria.quality;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import veterinaria.vista.menu.Menu;

import static org.junit.jupiter.api.Assertions.*;

class MenuPermissionMappingTest {

    @Test
    void permisosDeProveedoresYConfiguracionDebenMapearAlFormularioCorrecto() throws Exception {
        Method method = Menu.class.getDeclaredMethod("mapPermiso", int.class, int.class);
        method.setAccessible(true);

        Menu menu = new Menu();

        assertEquals("FormOrdenesCompra", method.invoke(menu, 7, 2));
        assertEquals("FormRecibirPedido", method.invoke(menu, 7, 3));
        assertEquals("ClassConfig", method.invoke(menu, 13, 1));
        assertEquals("FormAuditoria", method.invoke(menu, 12, 0));
    }
}
