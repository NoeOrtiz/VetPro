# Migraciones de base de datos de VetPRO

Esta carpeta contiene los cambios de esquema y datos necesarios para evolucionar VetPRO de forma controlada.

## Reglas

- No modificar manualmente la base de producción para incorporar cambios del rediseño.
- Cada cambio de esquema debe quedar documentado en un script versionado.
- Los scripts se ejecutarán primero sobre una copia restaurada del backup de referencia.
- No eliminar tablas, columnas ni datos históricos hasta verificar que el código vigente ya no los utiliza.
- Las migraciones destructivas deben ir separadas de las migraciones aditivas.
- Cada migración debe poder auditarse y asociarse a un cambio concreto del sistema.

## Convención

Los archivos SQL usarán el formato:

`VNNN__descripcion.sql`

Ejemplo:

`V001__usuario_estado_activo.sql`

## Estado actual

La rama `redisenio-vetpro` se utilizará para el rediseño. La rama `main` se mantiene como referencia estable.

Durante la primera fase Hibernate continúa con la configuración existente. No se cambiará `hbm2ddl.auto` hasta que los cambios de esquema necesarios estén representados y probados mediante migraciones.
