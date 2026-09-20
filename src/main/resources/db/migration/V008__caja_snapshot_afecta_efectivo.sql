-- Congela el efecto sobre efectivo de cada movimiento para preservar el arqueo historico.
-- Un cambio posterior en la configuracion de un medio de pago no altera cajas ya registradas.
ALTER TABLE cajamovimiento
    ADD COLUMN afectaEfectivo BIT(1) NOT NULL DEFAULT b'0' AFTER idMetodoPago;

-- Inicializa movimientos historicos usando la configuracion vigente al migrar.
UPDATE cajamovimiento cm
JOIN metodopago mp ON mp.idMetodoPago = cm.idMetodoPago
SET cm.afectaEfectivo = mp.afectaEfectivo;
