-- VetPRO · Datos históricos de consulta (17/11/2026)
-- Ejecutar en phpMyAdmin sobre la base de PRUEBAS, previa copia de seguridad.
-- No modifica los valores históricos ya registrados: quedan NULL en consultas anteriores.
USE veterinaria_prueba;
ALTER TABLE visita
  ADD COLUMN peso_consulta DECIMAL(8,3) NULL,
  ADD COLUMN especie_consulta VARCHAR(100) NULL,
  ADD COLUMN raza_consulta VARCHAR(100) NULL;
-- El tratamiento ya se amplió a TEXT en 2026-11-17_observacion_y_tratamiento_texto.sql.
