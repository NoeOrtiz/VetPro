-- VetPRO - historial de transferencias de propietario
-- Ejecutar en phpMyAdmin sobre veterinaria_prueba DESPUES de realizar backup.
-- Verificar previamente que los ID son INT y las tablas usan InnoDB.
USE veterinaria_prueba;

CREATE TABLE IF NOT EXISTS mascota_propietario_historial (
    idTransferencia INT NOT NULL AUTO_INCREMENT,
    idMascota INT NOT NULL,
    idClienteAnterior INT NOT NULL,
    idClienteNuevo INT NOT NULL,
    fechaTransferencia DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo VARCHAR(100) NOT NULL,
    observaciones VARCHAR(500) NULL,
    idUsuario INT NULL,
    PRIMARY KEY (idTransferencia),
    INDEX idx_transferencia_mascota (idMascota),
    INDEX idx_transferencia_cliente_anterior (idClienteAnterior),
    INDEX idx_transferencia_cliente_nuevo (idClienteNuevo),
    INDEX idx_transferencia_usuario (idUsuario),
    CONSTRAINT fk_transferencia_mascota FOREIGN KEY (idMascota) REFERENCES mascota(idMascota),
    CONSTRAINT fk_transferencia_anterior FOREIGN KEY (idClienteAnterior) REFERENCES cliente(idCliente),
    CONSTRAINT fk_transferencia_nuevo FOREIGN KEY (idClienteNuevo) REFERENCES cliente(idCliente),
    CONSTRAINT fk_transferencia_usuario FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT chk_transferencia_distinta CHECK (idClienteAnterior <> idClienteNuevo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
