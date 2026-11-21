# VetPRO híbrido — Usuarios y menú lateral

- Se adopta la presentación visual del compañero **sin copiar su lógica**: fichas, foto, cabeceras de tabla, badges verdes/rojos, botones diferenciados.
- Se conservan los objetos de campos de usuario originales, validaciones, guardado, edición, confirmaciones, historial de estado, casillas de selección, impresión, permisos y paginación.
- Las tarjetas de datos y acceso usan GridLayout y el formulario completo dispone de scroll para evitar recortes en 1366x768.
- El menú lateral cierra la sección anterior cuando se despliega otra, marca la selección con verde de la marca y no ejecuta navegación sin permisos.
- Se suprimen estilos FlatLaf incompatibles reportados por el usuario: `arc` en Button y `alternateRowColor` en Table.styleClass.
- No se cambian consultas SQL, DAOs ni servicios de negocio.

**Pendiente**: verificación visual y funcional en Windows / NetBeans usando datos de prueba. Las pruebas sin MySQL no demuestran que el 100% de los circuitos de la aplicación funcionen. No reemplazar automáticamente la base real.
