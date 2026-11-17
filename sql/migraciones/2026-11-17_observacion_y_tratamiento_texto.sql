-- VetPRO: permitir evolución clínica extensa y estado OBSERVACION.
-- Ejecutar en phpMyAdmin sobre la base de pruebas, previa copia de seguridad.
-- Cambios conservativos: no borran ni reemplazan registros existentes.
USE veterinaria_prueba;

ALTER TABLE visita
  MODIFY COLUMN tratamiento TEXT NULL;

-- Amplía la capacidad para registrar el nuevo estado clínico.
ALTER TABLE visita
  MODIFY COLUMN estado VARCHAR(20) NOT NULL;

-- Verificación opcional:
-- SHOW COLUMNS FROM visita LIKE 'tratamiento';
-- SHOW COLUMNS FROM visita LIKE 'estado';
