-- VetPRO - Migracion V001
-- Agrega ciclo de vida ACTIVO/INACTIVO a los usuarios sin eliminar historia.
-- Compatible con el esquema de referencia MariaDB/MySQL.

ALTER TABLE usuario
    ADD COLUMN activo BIT(1) NOT NULL DEFAULT b'1' AFTER bloqueado_hasta;

-- Todos los usuarios existentes se consideran activos al migrar.
UPDATE usuario
SET activo = b'1'
WHERE activo IS NULL OR activo = b'0';
