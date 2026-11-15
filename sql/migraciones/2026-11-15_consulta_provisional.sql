-- VetPRO - vínculo de la consulta urgente con una solicitud pendiente.
-- Ejecutar DESPUES de 2026-11-15_solicitudes_reactivacion.sql.
USE veterinaria_prueba;
ALTER TABLE visita
  ADD COLUMN atencion_provisional TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN idSolicitudReactivacion INT NULL,
  ADD INDEX idx_visita_solicitud (idSolicitudReactivacion),
  ADD CONSTRAINT fk_visita_solicitud
    FOREIGN KEY (idSolicitudReactivacion)
    REFERENCES mascota_solicitud_reactivacion(idSolicitud);
