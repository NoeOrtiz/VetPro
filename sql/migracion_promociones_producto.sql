-- Ejecutar una sola vez sobre la base de datos de VetPRO, con copia de seguridad previa.
-- Si Hibernate está configurado para actualizar el esquema, comprobar primero si ya existen las columnas.
ALTER TABLE producto
    ADD COLUMN promocion_activa TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN promocion_inicio DATE NULL,
    ADD COLUMN promocion_fin DATE NULL;

-- Los productos existentes no quedan en promoción automáticamente.
-- El porcentaje descuento existente permanece sin cambios.
