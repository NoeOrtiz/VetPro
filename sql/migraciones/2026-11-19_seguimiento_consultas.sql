-- VetPRO: seguimiento opcional entre consultas (2026-11-19).
-- Ejecutar UNA sola vez sobre veterinaria_prueba, después de realizar respaldo.
-- La consulta de seguimiento es una nueva visita/ticket con fecha y peso propios.
-- No crea automáticamente turnos y no modifica consultas anteriores.
ALTER TABLE visita
  ADD COLUMN id_consulta_origen INT NULL,
  ADD COLUMN proximo_control_recomendado DATE NULL,
  ADD INDEX idx_visita_consulta_origen (id_consulta_origen),
  ADD CONSTRAINT fk_visita_consulta_origen
    FOREIGN KEY (id_consulta_origen) REFERENCES visita(idVisita);
