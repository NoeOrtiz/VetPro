-- Configuraciones por defecto (idempotentes si se aplican en una DB vacía)

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_TURNOS_POR_DIA', '10', 'Turnos máximos por día en Peluquería');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_TURNOS_MANANA', '2', 'Turnos máximos por la mañana');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_TURNOS_TARDE', '2', 'Turnos máximos por la tarde');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_DURACION_TURNO_MIN', '60', 'Duración estándar del turno (minutos)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_HORARIO_MANANA_DESDE', '09:00', 'Horario de atención (mañana) - desde (HH:mm)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_HORARIO_MANANA_HASTA', '11:00', 'Horario de atención (mañana) - hasta (HH:mm)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_HORARIO_TARDE_DESDE', '17:00', 'Horario de atención (tarde) - desde (HH:mm)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_HORARIO_TARDE_HASTA', '20:00', 'Horario de atención (tarde) - hasta (HH:mm)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('PELUQUERIA_DIAS_HABILITADOS', '1,2,3,4,5', 'Días habilitados (1=Lun ... 7=Dom)');

-- Laboratorio
INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('LABORATORIO_DIAS_HABILITADOS', '1,3,5', 'Laboratorio - días habilitados (1=Lun ... 7=Dom)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('LABORATORIO_DURACION_TURNO_MIN', '30', 'Laboratorio - duración estándar del turno (minutos)');

-- LABORATORIO_INTERVALO_EXTRACCION_HORAS eliminado: laboratorio usa turnos por duración.

-- Hospitalización
INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('HOSPITALIZACION_DIAS_HABILITADOS', '2,4,6', 'Hospitalización - días habilitados (1=Lun ... 7=Dom)');

INSERT INTO configuracion (clave, valor, descripcion)
VALUES ('HOSPITALIZACION_DURACION_TURNO_MIN', '60', 'Hospitalización - duración estándar del turno (minutos)');
