package veterinaria.servicio;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteRegistry;
import veterinaria.reportes.core.ReporteTipo;
import veterinaria.reportes.pdf.ReporteSelectorUtil;

public class ComprobantePdfService {

    private final ReporteRegistry reporteRegistry = new ReporteRegistry();

    public enum HistorialTurnosModo {
        COMPACTO,
        DETALLADO
    }

    public File generarReciboVentaPdf(long idRecibo) throws IOException {
        return generarDelegado(ReporteTipo.RECIBO_VENTA,
                new ReporteRequest().put("idRecibo", idRecibo));
    }

    public File generarComprobanteCobroCuentaCorrientePdf(int idMovimiento) throws IOException {
        return generarDelegado(ReporteTipo.CC_COBRO,
                new ReporteRequest().put("idMovimiento", idMovimiento));
    }

    public File generarHistorialTurnosPdf(
            javax.swing.JTable table,
            java.util.Map<String, String> params,
            HistorialTurnosModo modo
    ) throws IOException {

        ReporteTipo tipo = (modo == HistorialTurnosModo.COMPACTO)
                ? ReporteTipo.HISTORIAL_TURNOS_LISTADO
                : ReporteTipo.HISTORIAL_TURNOS;

        ReporteRequest request = new ReporteRequest().put("tabla", table);
        if (params != null) {
            request.put("params", params)
                    .put("pTitulo", params.get("pTitulo"))
                    .put("pUsuario", params.get("pUsuario"))
                    .put("pRol", params.get("pRol"))
                    .put("pFechaEmision", params.get("pFechaEmision"))
                    .put("pFiltros", params.get("pFiltros"))
                    .put("pTotalRegistros", params.get("pTotalRegistros"))
                    .put("filtros", params.get("pFiltros"));
        }

        return generarDelegado(tipo, request);
    }

    private File generarDelegado(ReporteTipo tipo, ReporteRequest request) throws IOException {
        try {
            // 1. Extraemos el formato del request (si llega a ser null, por defecto usamos PDF)
            ReporteSelectorUtil.Formato formato = request.get("FORMATO_REPORTE", ReporteSelectorUtil.Formato.class);
            if (formato == null) {
                formato = ReporteSelectorUtil.Formato.PDF;
            }

            // 2. Le pasamos AMBOS datos al registro modificado
            return reporteRegistry.get(tipo, formato).generar(request);

        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e.getMessage(), e);
        }
    }

    public void abrirArchivo(File f) throws IOException {
        if (f == null) {
            return;
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(f);
        }
    }

    public void imprimirArchivo(File f) throws IOException {
        if (f == null) {
            return;
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().print(f);
        }
    }
}
