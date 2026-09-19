-- VetPRO - Migracion V001
-- Agrega ciclo de vida ACTIVO/INACTIVO a los usuarios sin eliminar historia.
-- Compatible con el esquema de referencia MariaDB/MySQL.
--
-- El DEFAULT 1 migra los registros existentes como activos al crear la columna.
-- No se ejecuta un UPDATE posterior para evitar que una reejecucion accidental
-- reactive usuarios que hayan sido desactivados legitimamente.

ALTER TABLE usuario
    ADD COLUMN activo BIT(1) NOT NULL DEFAULT b'1' AFTER bloqueado_hasta;
