-- VetPRO - Migracion V002
-- Conserva los movimientos financieros y permite vincular una reversión
-- con el movimiento original en lugar de eliminarlo físicamente.

ALTER TABLE cuentacorrientemovimiento
    ADD COLUMN idMovimientoRevertido INT NULL AFTER saldoResultante,
    ADD COLUMN motivoReversion VARCHAR(255) NULL AFTER idMovimientoRevertido,
    ADD CONSTRAINT fk_ccmov_reversion
        FOREIGN KEY (idMovimientoRevertido)
        REFERENCES cuentacorrientemovimiento (idMovimiento);

CREATE INDEX idx_ccmov_reversion
    ON cuentacorrientemovimiento (idMovimientoRevertido);
