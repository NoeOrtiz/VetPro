package veterinaria.entidad;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del modelo de anamnesis; no requieren MySQL. */
class HistoriaAnamnesisTest {

    @Test
    void conservaAntecedentesYDatosDeTrazabilidad() {
        HistoriaAnamnesis ficha = new HistoriaAnamnesis();
        // Mascota no expone setIdMascota: el ID se asigna al crear o persistir.
        Mascota paciente = new Mascota(42, "Paciente de prueba", "Hembra",
                "Mestiza", "Perro", null, "No", null, null, null);
        ficha.setMascota(paciente);
        ficha.setFechaRegistro(LocalDateTime.of(2026, 11, 20, 8, 30));
        ficha.setRegistradoPor("Veterinario de prueba");
        ficha.setDieta("Alimento balanceado");
        ficha.setEnfermedadesPrevias("Ninguna");
        ficha.setCirugiasPrevias("No");
        ficha.setEsquemaVacunal("Al día");
        ficha.setDesparasitacion("Última dosis registrada");
        ficha.setTratamientosRecientes("No");
        ficha.setEsterilizacion("Sí");
        ficha.setNumeroPartos("0");
        ficha.setViajesRecientes("No");
        ficha.setConviveConAnimales("Sí, un gato");
        ficha.setComportamiento("Tranquilo");
        ficha.setSenalesParticulares("Cicatriz en pata");

        assertEquals(42, ficha.getMascota().getIdMascota());
        assertEquals("Veterinario de prueba", ficha.getRegistradoPor());
        assertEquals("Alimento balanceado", ficha.getDieta());
        assertEquals("Ninguna", ficha.getEnfermedadesPrevias());
        assertEquals("No", ficha.getCirugiasPrevias());
        assertEquals("Al día", ficha.getEsquemaVacunal());
        assertEquals("Última dosis registrada", ficha.getDesparasitacion());
        assertEquals("No", ficha.getTratamientosRecientes());
        assertEquals("Sí", ficha.getEsterilizacion());
        assertEquals("0", ficha.getNumeroPartos());
        assertEquals("No", ficha.getViajesRecientes());
        assertEquals("Sí, un gato", ficha.getConviveConAnimales());
        assertEquals("Tranquilo", ficha.getComportamiento());
        assertEquals("Cicatriz en pata", ficha.getSenalesParticulares());
        assertEquals(LocalDateTime.of(2026, 11, 20, 8, 30), ficha.getFechaRegistro());
    }
}
