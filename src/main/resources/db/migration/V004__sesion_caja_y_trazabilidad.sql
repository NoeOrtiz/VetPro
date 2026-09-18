-- VetPRO - Migracion V004
-- Introduce una sesión de caja explícita: apertura -> operaciones -> arqueo -> cierre.
-- No elimina ni transforma los movimientos históricos existentes.

CREATE TABLE caja_sesion (
    idCajaSesion BIGINT NOT NULL AUTO_INCREMENT,
    fechaApertura DATETIME NOT NULL,
    fechaCierre DATETIME NULL,
    montoInicial DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    efectivoEsperado DECIMAL(12,2) NULL,
    efectivoContado DECIMAL(12,2) NULL,
    diferencia DECIMAL(12,2) NULL,
    motivoDiferencia VARCHAR(255) NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA',
    idUsuarioApertura INT NOT NULL,
    idUsuarioCierre INT NULL,
    PRIMARY KEY (idCajaSesion),
    CONSTRAINT fk_caja_sesion_usuario_apertura
        FOREIGN KEY (idUsuarioApertura) REFERENCES usuario (idUsuario),
    CONSTRAINT fk_caja_sesion_usuario_cierre
        FOREIGN KEY (idUsuarioCierre) REFERENCES usuario (idUsuario)
);

CREATE INDEX idx_caja_sesion_estado
    ON caja_sesion (estado);

CREATE INDEX idx_caja_sesion_fecha_apertura
    ON caja_sesion (fechaApertura);

ALTER TABLE cajamovimiento
    ADD COLUMN idCajaSesion BIGINT NULL AFTER idMovimiento,
    ADD COLUMN fechaHora DATETIME NULL AFTER fecha,
    ADD COLUMN anulado BIT(1) NOT NULL DEFAULT b'0' AFTER eliminado,
    ADD COLUMN fechaAnulacion DATETIME NULL AFTER anulado,
    ADD COLUMN motivoAnulacion VARCHAR(255) NULL AFTER fechaAnulacion,
    ADD COLUMN idUsuarioAnulacion INT NULL AFTER motivoAnulacion,
    ADD COLUMN idMovimientoReversion BIGINT NULL AFTER idUsuarioAnulacion,
    ADD CONSTRAINT fk_cajamov_sesion
        FOREIGN KEY (idCajaSesion) REFERENCES caja_sesion (idCajaSesion),
    ADD CONSTRAINT fk_cajamov_usuario_anulacion
        FOREIGN KEY (idUsuarioAnulacion) REFERENCES usuario (idUsuario),
    ADD CONSTRAINT fk_cajamov_reversion
        FOREIGN KEY (idMovimientoReversion) REFERENCES cajamovimiento (idMovimiento);

CREATE INDEX idx_cajamov_sesion
    ON cajamovimiento (idCajaSesion);

CREATE INDEX idx_cajamov_reversion
    ON cajamovimiento (idMovimientoReversion);

-- 'eliminado' se conserva temporalmente por compatibilidad con el código legado.
-- Se retirará únicamente después de migrar y probar todas las consultas/pantallas.
