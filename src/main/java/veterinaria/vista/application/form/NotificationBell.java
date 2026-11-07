package veterinaria.vista.application.form;

import java.awt.*;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import veterinaria.servicio.ResumenInicioService;

/** Campanita persistente de notificaciones con indicadores reales de la base. */
public class NotificationBell extends JPanel {
    private final JButton boton = new JButton("\uD83D\uDD14");
    private final JPopupMenu popup = new JPopupMenu();
    private final Timer actualizador;
    private boolean cargando;
    private ResumenInicioService.Indicadores ultimos;

    public NotificationBell() {
        setOpaque(false);
        setLayout(new BorderLayout());
        boton.setFont(boton.getFont().deriveFont(22f));
        boton.setToolTipText("Notificaciones");
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(3, 8, 3, 8));
        boton.setContentAreaFilled(false);
        boton.addActionListener(e -> {
            construirPopup();
            popup.show(boton, Math.min(0, boton.getWidth() - 300), boton.getHeight());
            refrescar();
        });
        add(boton, BorderLayout.CENTER);
        actualizador = new Timer(60000, e -> refrescar());
        actualizador.start();
        refrescar();
    }

    private void refrescar() {
        if (cargando) return;
        cargando = true;
        new SwingWorker<ResumenInicioService.Indicadores, Void>() {
            @Override
            protected ResumenInicioService.Indicadores doInBackground() {
                return new ResumenInicioService().obtener(LocalDate.now());
            }

            @Override
            protected void done() {
                cargando = false;
                try {
                    ultimos = get();
                    long total = ultimos.productosStockCritico
                            + (ultimos.pacientesEnEspera > 0 ? 1 : 0);
                    boton.setText("\uD83D\uDD14" + (total > 0 ? "  " + total : ""));
                    boton.setForeground(total > 0 ? new Color(235, 99, 83)
                            : UIManager.getColor("Button.foreground"));
                    if (popup.isVisible()) construirPopup();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Logger.getLogger(NotificationBell.class.getName())
                            .log(Level.WARNING, "No se pudieron consultar las alertas", ex.getCause());
                    boton.setToolTipText("No se pudieron actualizar las notificaciones");
                }
            }
        }.execute();
    }

    private void construirPopup() {
        popup.removeAll();
        popup.add(titulo("Notificaciones de VetPRO"));
        popup.addSeparator();
        if (ultimos == null) {
            popup.add(titulo("Cargando notificaciones..."));
            return;
        }
        if (ultimos.productosStockCritico > 0) {
            popup.add(titulo("Stock crítico: " + ultimos.productosStockCritico
                    + " producto(s) en mínimo o por debajo"));
        }
        if (ultimos.pacientesEnEspera > 0) {
            popup.add(titulo("Pacientes en espera: " + ultimos.pacientesEnEspera));
        }
        if (ultimos.productosStockCritico == 0 && ultimos.pacientesEnEspera == 0) {
            popup.add(titulo("Sin alertas pendientes"));
        }
        popup.addSeparator();
        popup.add(titulo("Alertas de vacunas y turnos: próximamente"));
    }

    private JMenuItem titulo(String texto) {
        JMenuItem item = new JMenuItem(texto);
        item.setEnabled(false);
        item.setBorder(new EmptyBorder(8, 12, 8, 12));
        return item;
    }

    @Override
    public void removeNotify() {
        actualizador.stop();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (actualizador != null) actualizador.start();
    }
}
