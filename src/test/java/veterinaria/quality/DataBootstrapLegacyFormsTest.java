package veterinaria.quality;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import veterinaria.bootstrap.DataBootstrap;

import static org.junit.jupiter.api.Assertions.*;

class DataBootstrapLegacyFormsTest {

    @Test
    void bootstrapNoDebeIncluirFormulariosLegacyDuplicados() throws Exception {
        Method method = DataBootstrap.class.getDeclaredMethod("getFormsList");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Class<?>> forms = (List<Class<?>>) method.invoke(null);
        List<String> names = forms.stream().map(Class::getSimpleName).collect(Collectors.toList());

        assertTrue(names.contains("FormRecibirPedido"));
        assertFalse(names.contains("FormRecepcionarPedido"));
        assertFalse(names.contains("FormActualizarStock"));
        assertFalse(names.contains("FormConsumos"));
        assertFalse(names.contains("FormMascotaConsumos"));
        assertFalse(names.contains("FormMovimientos"));
    }
}
