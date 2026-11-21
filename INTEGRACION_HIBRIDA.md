# VetPRO híbrido — primera integración visual

**Estado:** trabajo inicial. No verificado con NetBeans ni aprobado para uso real.

La rama `integracion/estetica-companero-funciones-noe` parte de `redisenio-vetpro` de `NoeOrtiz/VetPro`. Su referencia estética es `NoeOrtiz/VetPRO_Companero/main`.

## Qué conserva
- Toda la lógica y las pantallas originales, con sus archivos `.form` de NetBeans.
- La búsqueda por prefijo de Consultas, las reglas de estado, los datos clínicos ampliados y los scripts de migración del original.
- Permisos, servicios, informes y dependencias Maven existentes.
- Los tamaños y controles de botones y tablas del original, sin imponer los estilos discutidos de la otra versión.

## Qué cambia
- Se incorporan colores para tarjetas, superficies, ayudas y estados desde el diseño del compañero.
- El Inicio se presenta con tres métricas separadas dentro de la tarjeta de resumen; mantiene exactamente sus accesos originales y sus permisos. Se aplican además superficies y títulos visuales de forma selectiva en Clientes, Mascotas, Consultas, Historia Clínica y Usuarios, sin cambiar sus archivos .form ni sus handlers.
- Se añaden clases de estilo optativas que podrán adoptarse por formulario sin alterar todo el sistema.
- Se suma una prueba básica de preservación de campos clínicos.

## Estado y pruebas pendientes
No se ha ejecutado `mvn clean test`, ni un `Clean and Build` en NetBeans 17, ni una prueba contra MySQL. Se requiere validar visualmente cada formulario y comprobar las funciones antes de considerar la versión final. Esta rama todavía no replica todos los formularios modificados por el compañero.

Verificar en este orden: JDK 21 y Maven; login e Inicio con temas claro/oscuro; Clientes, Mascotas, Consultas e Historia Clínica; Caja y Cuenta Corriente; inventario y proveedores; PDF; permisos; backups.

**Base de datos:** usar copia aislada y respaldada. No aplicar scripts nuevos a la base real sin revisión.

## Versiones protegidas
Esta integración no modifica ni `redisenio-vetpro` ni `VetPRO_Companero/main`.
