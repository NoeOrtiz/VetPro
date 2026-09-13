package veterinaria.reportes.impl;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.Persona;

import veterinaria.reportes.core.Reporte;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;

public class ClienteRegistroXlsReporte implements Reporte {

    @Override
    public ReporteTipo tipo() {
        return ReporteTipo.CLIENTE_REGISTRO;
    }

    @Override
    public File generar(ReporteRequest request) throws Exception {
        // 1. Recuperamos los datos que inyectamos en el formulario
        Cliente cliente = request.get("cliente", Cliente.class);
        Persona persona = request.get("persona", Persona.class);
        String emisor = request.get("emisor", String.class);

        if (cliente == null || persona == null) {
            throw new IllegalArgumentException("Datos del cliente incompletos en el request.");
        }

        // 2. Creamos el archivo temporal
        File archivoTemporal = File.createTempFile("ficha_cliente_", ".xls");
        archivoTemporal.deleteOnExit();

        // 3. Diseñamos la ficha en Excel
        try (Workbook libro = new HSSFWorkbook(); FileOutputStream fos = new FileOutputStream(archivoTemporal)) {

            Sheet hoja = libro.createSheet("Ficha de Cliente");

            // Estilos básicos
            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);

            Font fuenteNegrita = libro.createFont();
            fuenteNegrita.setBold(true);
            CellStyle estiloNegrita = libro.createCellStyle();
            estiloNegrita.setFont(fuenteNegrita);

            // --- ARMADO DE LA FICHA ---
            // Fila 0: Título
            Row fila0 = hoja.createRow(0);
            Cell celdaTitulo = fila0.createCell(0);
            celdaTitulo.setCellValue("FICHA DE CLIENTE");
            celdaTitulo.setCellStyle(estiloTitulo);

            // Fila 2: Nombre y Apellido
            Row fila2 = hoja.createRow(2);
            fila2.createCell(0).setCellValue("Nombre Completo:");
            fila2.getCell(0).setCellStyle(estiloNegrita);
            fila2.createCell(1).setCellValue(persona.getApellido() + ", " + persona.getNombre());

            // Fila 3: Documento / CUIT
            Row fila3 = hoja.createRow(3);
            fila3.createCell(0).setCellValue("Documento/Nro:");
            fila3.getCell(0).setCellStyle(estiloNegrita);
            fila3.createCell(1).setCellValue(persona.getDni());

            // Fila 4: Teléfono
            Row fila4 = hoja.createRow(4);
            fila4.createCell(0).setCellValue("Teléfono:");
            fila4.getCell(0).setCellStyle(estiloNegrita);
            fila4.createCell(1).setCellValue(persona.getTelefono());

            // Fila 5: Correo
            Row fila5 = hoja.createRow(5);
            fila5.createCell(0).setCellValue("Email:");
            fila5.getCell(0).setCellStyle(estiloNegrita);

            String email = (cliente.getEmail() != null) ? cliente.getEmail() : "No especificado";
            fila5.createCell(1).setCellValue(email);

            // Fila 7: Pie de reporte
            Row fila7 = hoja.createRow(7);
            fila7.createCell(0).setCellValue("Emitido por:");
            fila7.getCell(0).setCellStyle(estiloNegrita);
            fila7.createCell(1).setCellValue(emisor);

            // Autoajustar columnas
            hoja.autoSizeColumn(0);
            hoja.autoSizeColumn(1);

            libro.write(fos);
        }

        return archivoTemporal;
    }
}
