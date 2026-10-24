-- VetPRO: respaldo previo obligatorio. Ejecutar UNA sola vez y solo
-- si las columnas no existen; no ejecutar sobre producción sin validar.
ALTER TABLE metodopago
    ADD COLUMN recargo_porcentaje DECIMAL(8,3) NOT NULL DEFAULT 0.000,
    ADD COLUMN descuento_porcentaje DECIMAL(8,3) NOT NULL DEFAULT 0.000;
-- La migración no aplica cargos ni altera recibos existentes.
