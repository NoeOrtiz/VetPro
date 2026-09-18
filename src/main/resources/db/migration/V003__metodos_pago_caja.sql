-- VetPRO - Migracion V003
-- Los medios de pago son configurables y se desactivan, no se eliminan.
-- afectaEfectivo indica si el medio integra el arqueo físico de caja.

ALTER TABLE metodopago
    ADD COLUMN activo BIT(1) NOT NULL DEFAULT b'1' AFTER descripcion,
    ADD COLUMN afectaEfectivo BIT(1) NOT NULL DEFAULT b'0' AFTER activo;

-- Efectivo es el único medio estándar que afecta el dinero físico del cajón.
UPDATE metodopago
SET afectaEfectivo = b'1'
WHERE UPPER(TRIM(nombre)) = 'EFECTIVO';

-- Métodos estándar. INSERT ... SELECT evita duplicar nombres existentes.
INSERT INTO metodopago (nombre, descripcion, activo, afectaEfectivo)
SELECT 'Efectivo', 'Pago en efectivo', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM metodopago WHERE UPPER(TRIM(nombre)) = 'EFECTIVO');

INSERT INTO metodopago (nombre, descripcion, activo, afectaEfectivo)
SELECT 'Transferencia bancaria', 'Pago mediante transferencia bancaria', b'1', b'0'
WHERE NOT EXISTS (SELECT 1 FROM metodopago WHERE UPPER(TRIM(nombre)) = 'TRANSFERENCIA BANCARIA');

INSERT INTO metodopago (nombre, descripcion, activo, afectaEfectivo)
SELECT 'Tarjeta de débito', 'Pago con tarjeta de débito', b'1', b'0'
WHERE NOT EXISTS (SELECT 1 FROM metodopago WHERE UPPER(TRIM(nombre)) = 'TARJETA DE DÉBITO');

INSERT INTO metodopago (nombre, descripcion, activo, afectaEfectivo)
SELECT 'Tarjeta de crédito', 'Pago con tarjeta de crédito', b'1', b'0'
WHERE NOT EXISTS (SELECT 1 FROM metodopago WHERE UPPER(TRIM(nombre)) = 'TARJETA DE CRÉDITO');

INSERT INTO metodopago (nombre, descripcion, activo, afectaEfectivo)
SELECT 'Billetera virtual', 'Pago mediante billetera virtual; la referencia puede identificar el proveedor utilizado', b'1', b'0'
WHERE NOT EXISTS (SELECT 1 FROM metodopago WHERE UPPER(TRIM(nombre)) = 'BILLETERA VIRTUAL');

INSERT INTO metodopago (nombre, descripcion, activo, afectaEfectivo)
SELECT 'Otro', 'Otro medio de pago configurable', b'1', b'0'
WHERE NOT EXISTS (SELECT 1 FROM metodopago WHERE UPPER(TRIM(nombre)) = 'OTRO');
