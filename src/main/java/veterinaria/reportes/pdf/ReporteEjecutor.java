package veterinaria.reportes.pdf;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import veterinaria.reportes.core.ReporteService;
import veterinaria.reportes.core.ReporteRequest;
import veterinaria.reportes.core.ReporteTipo;
import com.formdev.flatlaf.FlatClientProperties;

public class ReporteEjecutor {

    public static void ejecutarConAviso(Component componentePadre, ReporteTipo tipo, ReporteRequest request) {
        Window ventanaAnfitriona = SwingUtilities.getWindowAncestor(componentePadre);

        JDialog dialogueCarga = new JDialog(ventanaAnfitriona, "Procesando", Dialog.ModalityType.APPLICATION_MODAL);
        dialogueCarga.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogueCarga.setSize(340, 115);
        dialogueCarga.setLocationRelativeTo(componentePadre);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "border: 16,16,16,16;"
                + "background: $Panel.background"
        );

        Icon iconoCarga = UIManager.getIcon("OptionPane.informationIcon");
        JLabel lblIcono = new JLabel(iconoCarga);

        JLabel lblMensaje = new JLabel("Generando informe, por favor espere...", JLabel.LEFT);
        lblMensaje.setFont(lblMensaje.getFont().deriveFont(Font.BOLD, 12f));

        JPanel panelInfo = new JPanel(new BorderLayout(10, 10));
        panelInfo.setOpaque(false);
        panelInfo.add(lblIcono, BorderLayout.WEST);
        panelInfo.add(lblMensaje, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        panel.add(panelInfo, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        dialogueCarga.add(panel);

        // Extraemos el formato del request para saber qué hacer al finalizar
        // Le pasamos la clave y el tipo "ReporteSelectorUtil.Formato.class"
        ReporteSelectorUtil.Formato formato = request.get("FORMATO_REPORTE", ReporteSelectorUtil.Formato.class);

        SwingWorker<File, Void> trabajador = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                return ReporteService.getInstance().generar(tipo, request);
            }

            @Override

            protected void done() {
                dialogueCarga.dispose(); // Cierra el diálogo de carga sin hacer ruido

                try {
                    File archivoTemporal = get();

                    if (archivoTemporal == null || !archivoTemporal.exists()) {
                        JOptionPane.showMessageDialog(componentePadre,
                                "El motor de reportes devolvió un archivo inexistente o nulo.",
                                "Error de Generación", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ReporteSelectorUtil.Formato formato = request.get("FORMATO_REPORTE", ReporteSelectorUtil.Formato.class);

                    if (formato == ReporteSelectorUtil.Formato.XLS) {
                        JFileChooser guardarDialogo = new JFileChooser();
                        guardarDialogo.setDialogTitle("Descargar Reporte Excel");
                        guardarDialogo.setSelectedFile(new File("Reporte_" + tipo.name().toLowerCase() + ".xls"));
                        guardarDialogo.setFileFilter(new FileNameExtensionFilter("Libros de Excel (*.xls)", "xls"));
                        guardarDialogo.setAcceptAllFileFilterUsed(false);

                        int seleccion = guardarDialogo.showSaveDialog(componentePadre);

                        if (seleccion == JFileChooser.APPROVE_OPTION) {
                            File destinoFinal = guardarDialogo.getSelectedFile();
                            String ruta = destinoFinal.getAbsolutePath();

                            if (!ruta.toLowerCase().endsWith(".xls")) {
                                destinoFinal = new File(ruta + ".xls");
                            }

                            Files.copy(archivoTemporal.toPath(), destinoFinal.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            JOptionPane.showMessageDialog(componentePadre,
                                    "Archivo descargado con éxito.",
                                    "Descarga Completa", JOptionPane.INFORMATION_MESSAGE);
                        }

                    } else {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(archivoTemporal);
                        } else {
                            JOptionPane.showMessageDialog(componentePadre,
                                    "El sistema no soporta la apertura automática de archivos.",
                                    "Advertencia", JOptionPane.WARNING_MESSAGE);
                        }
                    }

                } catch (java.util.concurrent.ExecutionException ee) {
                    // 🌟 NOTA: Los System.err.println de los bloques catch SÍ es buena práctica dejarlos, 
                    // porque si el sistema falla en el futuro por otra cosa, querés saber qué pasó.
                    System.err.println("=== [ERROR] Falló la ejecución del reporte ===");
                    ee.getCause().printStackTrace();
                    JOptionPane.showMessageDialog(componentePadre,
                            "Error interno en el motor de reportes:\n" + ee.getCause().getMessage(),
                            "Error de Ejecución", JOptionPane.ERROR_MESSAGE);

                } catch (Exception e) {
                    System.err.println("=== [ERROR] Error general en el visor de reportes ===");
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(componentePadre,
                            "Error al procesar el reporte: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        trabajador.execute();
        dialogueCarga.setVisible(true);
    }
}
