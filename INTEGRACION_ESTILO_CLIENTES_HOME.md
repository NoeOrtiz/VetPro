# Estilo visual de Clientes y Home — integración con el funcionamiento original

**Rama:** `integracion/clientes-y-home-estilo-companero`, parte de `integracion/estetica-companero-funciones-noe`.

## Corrección de la primera integración

La primera integración modificaba solamente tarjetas y algunos rótulos, pero no había
aplicado los estilos de botones y tablas del compañero. Esta rama aplica ahora
los roles de botón, cabeceras, márgenes, filas alternadas y etiquetas de estado
directamente en `FormCliente`.

- **Modelo original conservado:** casilla de selección, identificador oculto,
  columnas, filtro Activo/Inactivo, búsqueda por prefijo, paginación y permisos.
- **Botones con presentación del compañero:** alta como acción principal,
  edición/impresión como secundarias, desactivación como destructiva y limpieza como sutil.
  El botón cambia de categoría visual cuando cambia a «Reactivar».
- **Tabla:** cabecera 34px, filas al menos 32px, márgenes de 8px, fondos alternados,
  columnas sin cambios, y estado presentado como etiqueta verde/roja.
- **Home:** los accesos siguen siendo Nueva Consulta, Agenda/Calendario, Pacientes (Fichas)
  y Caja, con sus permisos y navegación originales; tarjetas redondeadas y estilo
  de interacción del compañero, sin sustituirlos por «Consultas».
- **PDF:** conservan la estética azul petróleo que ya fue integrada y probada.

## Observación sobre las capturas

La pantalla que el usuario identificó como «híbrida» tenía el botón «Nuevo cliente»,
pero carecía de casillas en la lista. Eso coincide con los cambios funcionales
del repo del compañero, no con esta rama, en la que la selección original sigue presente.
Comprobar en NetBeans la ruta física de proyecto cargada y cambiar explícitamente a
`integracion/estetica-companero-funciones-noe` luego de integrar esta rama.

## Pruebas pendientes

La compilación y las pruebas automáticas confirman la integridad del código, pero
se requiere revisión visual real en Windows, ambos temas (claro/oscuro), con datos
de prueba, selección y desactivación/reactivación.
