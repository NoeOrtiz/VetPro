package veterinaria.entidad;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Deprecated
@Converter(autoApply = false)
public class EstadoTurnoConverter implements AttributeConverter<EstadoTurno, String> {

    @Override
    public String convertToDatabaseColumn(EstadoTurno attribute) {
        return attribute != null ? attribute.getDbValue() : null;
    }

    @Override
    public EstadoTurno convertToEntityAttribute(String dbData) {
        return EstadoTurno.fromLabel(dbData);
    }
}
