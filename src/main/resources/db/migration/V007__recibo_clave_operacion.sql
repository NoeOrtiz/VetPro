-- Clave idempotente de la operacion que origina el recibo.
-- NULL conserva compatibilidad con recibos historicos.
ALTER TABLE recibo
    ADD COLUMN claveOperacion VARCHAR(64) NULL AFTER totalRecibo;

CREATE UNIQUE INDEX uk_recibo_clave_operacion
    ON recibo (claveOperacion);
