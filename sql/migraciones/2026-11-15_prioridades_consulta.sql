-- VetPRO | Clasificacion de prioridad de atencion
-- Realizar copia de seguridad de veterinaria_prueba antes de ejecutar.
USE veterinaria_prueba;

ALTER TABLE visita
  ADD COLUMN prioridad_atencion VARCHAR(12) NOT NULL DEFAULT 'CONTROL';

CREATE INDEX idx_visita_cola_prioridad
  ON visita (estado, prioridad_atencion, fecha, hora, idVisita);

-- Los registros historicos quedan clasificados CONTROL hasta su revision.
