package veterinaria.reportes.impl;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import veterinaria.entidad.Mascota;
import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;

public class MascotaRegistroXlsReporte implements Reporte {
    @Override public ReporteTipo tipo() { return ReporteTipo.MASCOTA_REGISTRO; }
    @Override public File generar(ReporteRequest request) throws Exception {
        Mascota m = request.get("mascota", Mascota.class);
        if (m == null) throw new IllegalArgumentException("No se proporcionó la mascota.");
        String dueno = m.getCliente() == null || m.getCliente().getPersona() == null ? ""
                : m.getCliente().getPersona().getNombre() + " " + m.getCliente().getPersona().getApellido();
        String[][] datos = {
            {"Nombre", m.getNombre()}, {"Estado", m.isActivo() ? "Activo" : "Inactivo"},
            {"Especie", m.getEspecie()}, {"Raza", m.getRaza()}, {"Sexo", m.getSexo()},
            {"Nacimiento", m.getFechaNacimiento() == null ? "" : m.getFechaNacimiento().toString()},
            {"Castrado", m.getCastrado()}, {"Tamaño", m.getTamano()}, {"Peso", m.getPeso()},
            {"Dueño", dueno}
        };
        File archivo = File.createTempFile("registro_mascota_", ".xls");
        archivo.deleteOnExit();
        try (Workbook libro = new HSSFWorkbook(); FileOutputStream out = new FileOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Ficha de mascota");
            Font negrita = libro.createFont(); negrita.setBold(true);
            CellStyle etiqueta = libro.createCellStyle(); etiqueta.setFont(negrita);
            for (int i = 0; i < datos.length; i++) {
                Row fila = hoja.createRow(i);
                Cell c = fila.createCell(0); c.setCellValue(datos[i][0]); c.setCellStyle(etiqueta);
                fila.createCell(1).setCellValue(datos[i][1] == null ? "" : datos[i][1]);
            }
            hoja.autoSizeColumn(0); hoja.autoSizeColumn(1);
            libro.write(out);
        }
        return archivo;
    }
}
