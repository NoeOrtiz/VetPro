-- VetPRO - Migracion V005
-- El estado operativo del servicio no implica que el servicio este pagado.

ALTER TABLE peluqueria
    ADD COLUMN estadoCobro VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' AFTER estado,
    ADD COLUMN idReciboCobro BIGINT NULL AFTER estadoCobro;

ALTER TABLE peluqueria
    ADD CONSTRAINT fk_peluqueria_recibo_cobro
    FOREIGN KEY (idReciboCobro) REFERENCES recibo (idRecibo);

CREATE INDEX idx_peluqueria_estado_cobro ON peluqueria (estadoCobro);
CREATE INDEX idx_peluqueria_recibo_cobro ON peluqueria (idReciboCobro);
