-- Ejecutar una sola vez en la base de datos de VetPRO (MySQL).
-- Permite ventas sin cliente registrado; las ventas a cuenta corriente
-- siguen requiriendo un cliente identificado desde el servicio de ventas.
ALTER TABLE recibo MODIFY COLUMN idCliente INT NULL;
