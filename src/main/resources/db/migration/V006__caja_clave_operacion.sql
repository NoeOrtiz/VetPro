-- VetPRO - Migracion V006
-- Clave idempotente para impedir que un doble clic o reintento
-- registre dos veces la misma operacion economica.
-- NULL queda permitido para movimientos historicos/legacy.

ALTER TABLE cajamovimiento
    ADD COLUMN claveOperacion VARCHAR(64) NULL AFTER descripcion;

CREATE UNIQUE INDEX uk_cajamovimiento_clave_operacion
    ON cajamovimiento (claveOperacion);
