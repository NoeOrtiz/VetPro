-- Migración manual opcional para MySQL / XAMPP
-- Renombra la columna producto.precio a producto.precio_costo
ALTER TABLE producto CHANGE COLUMN precio precio_costo DOUBLE NULL;
