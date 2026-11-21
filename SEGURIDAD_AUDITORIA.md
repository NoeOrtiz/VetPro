# Auditoría de seguridad — VetPRO híbrido

**Rama aislada:** `seguridad/auditoria-hibrido`, basada en `integracion/estetica-companero-funciones-noe`.
**Estado:** revisiones estáticas y pruebas agregadas; requieren ejecución de `mvn test` y pruebas reales. **No apto para datos reales sin validar.**

## Correcciones aplicadas en esta rama
- Alta del primer administrador: se elimina la contraseña predeterminada `admin/admin`. La primera instalación requiere definir la variable de entorno `VETPRO_BOOTSTRAP_ADMIN_PASSWORD`, con una contraseña robusta de 14 a 128 caracteres, mayúsculas, minúsculas, dígitos y símbolos. Si no está configurada o no es segura, se rechaza el alta inicial.
- Métodos antiguos de cambio/restablecimiento de contraseña sin token: deshabilitados explícitamente con `SecurityException`. El flujo seguro se realiza con códigos de un solo uso. La bandera de primer ingreso se actualiza dentro de la misma transacción del restablecimiento por código.
- Respaldos: se centralizó su generación. Toma host, puerto, base y usuario de la configuración activa de VetPRO, ya no utiliza shell `cmd.exe` ni credenciales fijas. Se borra un archivo incompleto si falla el proceso. El password se pasa por `MYSQL_PWD`, no en la línea de comandos; para instalaciones multiusuario debe preferirse archivo de opciones MySQL con permisos adecuados.
- Se agregó `vetpro.properties.example` para configuración local y reglas `.gitignore` para evitar nuevos commits con credenciales.
- Se agregaron pruebas unitarias y de regresión estática.

## Pendientes de seguridad
1. **Credenciales legacy:** se tolera por compatibilidad la verificación de contraseñas antiguas en texto plano; migrarlas o forzar cambios antes de eliminar esa compatibilidad.
2. **Recuperación:** falta limitar intentos fallidos de códigos por usuario y controlar agotamiento/reenvíos en la base.
3. **Transporte JDBC:** las conexiones que usan `useSSL=false` solo son aceptables en entornos locales aislados; configurar TLS para servidores remotos.
4. **Privacidad del repositorio:** `src/main/resources/vetpro.properties` existe en el historial y la rama híbrida original con datos locales (sin contraseña en la revisión); no agregar credenciales a control de versiones. Eliminar secretos históricos si los hubiera.
5. **Autorización y auditoría:** deben verificarse los permisos en capa de servicio y persistencia, no solo en la UI.
6. **MySQL:** un respaldo se consideró correcto si `mysqldump` finaliza con código 0 y archivo no vacío. Falta probar restauración real de una copia y permisos de acceso al archivo.
7. **Dependencias Maven:** ejecutar un análisis actualizado de vulnerabilidades (OWASP Dependency-Check o equivalente) y revisar actualizaciones.

## Cómo ejecutar las pruebas
Con JDK 21 y Maven, sobre una copia local de esta rama:
```
mvn clean test
```

Después, usar MySQL **solo de prueba**, validar login de administrador inicial configurando `VETPRO_BOOTSTRAP_ADMIN_PASSWORD`, recuperación de contraseñas por código, autorización, backups y restauración.

**No se deben ejecutar migraciones ni pruebas destructivas contra la base real** sin copia de seguridad.

## Revisión posterior de controles originales

- Se verificó que `FormLogin` y `CambiarContrasenaDialog` exigen cambio en primer ingreso, con código enviado por `EmailUtil` y validado en `RecuperacionClaveTokenDAO`.
- Se mantiene el tiempo de bloqueo de **15 minutos**, ajustando el umbral al requisito original de **3 intentos**.
- El bootstrap ahora requiere `VETPRO_BOOTSTRAP_ADMIN_EMAIL` válido además de `VETPRO_BOOTSTRAP_ADMIN_PASSWORD` para permitir el cambio obligatorio de la primera cuenta. No se usa el correo ficticio `admin@local`. El envío depende también de `VETPRO_EMAIL` y `VETPRO_EMAIL_PASSWORD`.
- JasperReports figuraba en `pom.xml`, pero la usuaria confirmó que no lo utiliza. Se retiró la dependencia de esta rama híbrida; la compilación completa debe validar que no se necesita.
- Las cuentas que ya existen en la base conservan su estado `primerLogin` y contraseñas. El cambio de bootstrap se aplica únicamente a instalaciones nuevas sin usuarios.
