-- VetPRO - anamnesis clínica versionada (MySQL 8).
-- Ejecutar en la base veterinaria_prueba antes de usar el nuevo formulario.
-- No modifica visitas ni historia_evento.
CREATE TABLE IF NOT EXISTS historia_anamnesis (
  idAnamnesis BIGINT NOT NULL AUTO_INCREMENT,
  idMascota INT NOT NULL,
  fechaRegistro DATETIME NOT NULL,
  registradoPor VARCHAR(200),
  dieta TEXT,
  enfermedadesPrevias TEXT,
  cirugiasPrevias TEXT,
  esquemaVacunal TEXT,
  desparasitacion TEXT,
  tratamientosRecientes TEXT,
  esterilizacion VARCHAR(30),
  numeroPartos VARCHAR(40),
  viajesRecientes TEXT,
  conviveConAnimales TEXT,
  comportamiento TEXT,
  senalesParticulares TEXT,
  PRIMARY KEY (idAnamnesis),
  KEY ix_anamnesis_mascota_fecha (idMascota, fechaRegistro),
  CONSTRAINT fk_anamnesis_mascota FOREIGN KEY (idMascota)
    REFERENCES mascota (idMascota)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
