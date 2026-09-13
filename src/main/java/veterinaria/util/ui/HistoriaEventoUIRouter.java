package veterinaria.util.ui;

import javax.swing.JOptionPane;
import veterinaria.entidad.HistoriaEvento;
import veterinaria.vista.FormHospitalizaciones;
import veterinaria.vista.FormLaboratorio;
import veterinaria.vista.FormProcedimientos;
import veterinaria.vista.FormTurnosPeluqueria;
import veterinaria.vista.FormConsulta;
import veterinaria.vista.application.Application;

public class HistoriaEventoUIRouter {

    private HistoriaEventoUIRouter() {
    }

    public static void abrir(HistoriaEvento ev) {
        if (ev == null) {
            return;
        }
        String refTabla = ev.getRefTabla();
        Integer refId = ev.getRefId();

        if (refTabla == null || refTabla.trim().isEmpty() || refId == null) {
            JOptionPane.showMessageDialog(null,
                    "El evento no tiene vínculo (refTabla/refId).\n" +
                    "Detalle: " + safe(ev.getTipo()) + " - " + safe(ev.getResumen()),
                    "Sin vínculo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String key = refTabla.trim().toLowerCase();
        Object form;
        switch (key) {
            case "visita":
                FormConsulta fv = new FormConsulta();
                fv.setOrigenHistoriaClinica(true);
                form = fv;
                break;
            case "laboratorio":
                form = new FormLaboratorio();
                break;
            case "hospitalizacion":
            case "hospitalización":
                form = new FormHospitalizaciones();
                break;
            case "procedimiento":
            case "procedimientos":
                form = new FormProcedimientos();
                break;
            case "peluqueria":
            case "peluquería":
                form = new FormTurnosPeluqueria();
                break;
            default:
                JOptionPane.showMessageDialog(null,
                        "Aún no hay navegación implementada para refTabla='" + refTabla + "'.\n" +
                        "refId=" + refId,
                        "Navegación no disponible",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
        }

        Application.showForm((java.awt.Component) form);

        if (form instanceof HistoriaEventoAbrible) {
            ((HistoriaEventoAbrible) form).abrirDetallePorId(refId);
        }
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
