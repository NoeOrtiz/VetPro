-- VetPRO: solicitudes de reactivacion / rectificacion
-- Respaldar veterinaria_prueba antes de ejecutar en phpMyAdmin.
USE veterinaria_prueba;
CREATE TABLE IF NOT EXISTS mascota_solicitud_reactivacion (
 idSolicitud INT NOT NULL AUTO_INCREMENT,
 idMascota INT NOT NULL,
 idUsuarioSolicitante INT NOT NULL,
 tipo VARCHAR(24) NOT NULL,
 prioridad VARCHAR(12) NOT NULL DEFAULT 'NORMAL',
 justificacion VARCHAR(1000) NOT NULL,
 estado VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE',
 fechaSolicitud DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 idUsuarioResolutor INT NULL,
 fechaResolucion DATETIME NULL,
 observacionResolucion VARCHAR(1000) NULL,
 motivoBajaOriginal VARCHAR(255) NULL,
 PRIMARY KEY (idSolicitud),
 INDEX idx_solicitud_estado (estado, prioridad, fechaSolicitud),
 INDEX idx_solicitud_paciente (idMascota, estado),
 CONSTRAINT fk_solicitud_mascota FOREIGN KEY (idMascota) REFERENCES mascota(idMascota),
 CONSTRAINT fk_solicitud_solicitante FOREIGN KEY (idUsuarioSolicitante) REFERENCES usuario(idUsuario),
 CONSTRAINT fk_solicitud_resolutor FOREIGN KEY (idUsuarioResolutor) REFERENCES usuario(idUsuario),
 CONSTRAINT chk_solicitud_tipo CHECK (tipo IN ('REACTIVACION','RECTIFICACION')),
 CONSTRAINT chk_solicitud_prioridad CHECK (prioridad IN ('NORMAL','URGENTE')),
 CONSTRAINT chk_solicitud_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
