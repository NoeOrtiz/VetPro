
package veterinaria.entidad;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class EstadoPeluqueriaEnumConverter implements AttributeConverter<EstadoPeluqueriaEnum, String> {

    @Override
    public String convertToDatabaseColumn(EstadoPeluqueriaEnum attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public EstadoPeluqueriaEnum convertToEntityAttribute(String dbData) {
        return EstadoPeluqueriaEnum.fromLabel(dbData);
    }
}
