package veterinaria.reportes.core;

import java.util.HashMap;
import java.util.Map;

// Importaciones de tus PDFs actuales
import veterinaria.reportes.impl.*;
import veterinaria.reportes.pdf.ReporteSelectorUtil;
// 🌟 NOTA: Acá vas a importar tus futuros reportes de Excel a medida que los crees
// import veterinaria.reportes.impl.xls.*; 

public class ReporteRegistry {

    // 🌟 Ahora el mapa guarda: Tipo -> (Formato -> Instancia del Reporte)
    private final Map<ReporteTipo, Map<ReporteSelectorUtil.Formato, Reporte>> map = new HashMap<>();

    public ReporteRegistry() {
        // === CONFIGURACIÓN DE REPORTES PDF ===
        registrar(ReporteSelectorUtil.Formato.PDF, new ReciboVentaPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new CuentaCorrienteCobroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new CuentaCorrienteMovimientosPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new CuentaCorrienteListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new CompraRecepcionPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new LaboratorioInformePdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new HospitalizacionRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new HistorialTurnosPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new HistorialTurnosListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new UsuarioRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new UsuarioListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new MascotaRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new MascotaListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new ClienteRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new ClienteListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new ProductoRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new ProductoListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new StockProductoRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new StockProductoListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new ProveedorRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new ProveedorListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new VisitaRegistroPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new VisitaListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new HistoriaClinicaPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new HistoriaClinicaListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new CajaMovimientoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new CajaMovimientosListadoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new AuditoriaEventoPdfReporte());
        registrar(ReporteSelectorUtil.Formato.PDF, new AuditoriaListadoPdfReporte());

        // === CONFIGURACIÓN DE REPORTES EXCEL (XLS) ===
        // Buscá el constructor de tu ReporteRegistry y agregá esta línea:
        registrar(ReporteSelectorUtil.Formato.XLS, new ClienteListadoXlsReporte());
        registrar(ReporteSelectorUtil.Formato.XLS, new ClienteRegistroXlsReporte());
        registrar(ReporteSelectorUtil.Formato.XLS, new ProductoListadoXlsReporte()
        );
    }

    // Método modificado para aceptar el formato explícitamente
    private void registrar(ReporteSelectorUtil.Formato formato, Reporte reporte) {
        map.computeIfAbsent(reporte.tipo(), k -> new HashMap<>()).put(formato, reporte);
    }

    // Ahora el método 'get' busca usando el Tipo Y el Formato
    public Reporte get(ReporteTipo tipo, ReporteSelectorUtil.Formato formato) {
        Map<ReporteSelectorUtil.Formato, Reporte> formatosDisponibles = map.get(tipo);

        if (formatosDisponibles == null) {
            throw new IllegalArgumentException("No hay reportes registrados para el tipo: " + tipo);
        }

        Reporte r = formatosDisponibles.get(formato);
        if (r == null) {
            throw new IllegalArgumentException("El reporte de tipo [" + tipo + "] no está disponible en formato: " + formato);
        }
        return r;
    }
}
