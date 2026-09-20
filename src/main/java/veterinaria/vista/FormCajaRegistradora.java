
package veterinaria.vista;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.CajaMovimientoControlador;
import veterinaria.controlador.ClienteControlador;
import veterinaria.controlador.CuentaCorrienteControlador;
import veterinaria.controlador.MetodoPagoControlador;
import veterinaria.controlador.ProductoControlador;
import veterinaria.controlador.ProveedorControlador;
import veterinaria.controlador.RubroControlador;
import veterinaria.servicio.ComprobantePdfService;
import veterinaria.servicio.ConfiguracionService;
import veterinaria.entidad.CajaMovimiento;
import veterinaria.entidad.Cliente;
import veterinaria.entidad.CuentaCorriente;
import veterinaria.entidad.CuentaCorrienteMovimiento;
import veterinaria.entidad.MetodoPago;
import veterinaria.entidad.Rubro;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.Recibo;
import veterinaria.entidad.ReciboMetodoPago;
import veterinaria.entidad.ReciboProductos;
import veterinaria.entidad.Usuario;
import veterinaria.entidad.util.ClienteItem;
import veterinaria.entidad.util.MetodoPagoItem;
import veterinaria.entidad.util.ProductoItem;
import veterinaria.entidad.util.ProveedorItem;
import veterinaria.persistencia.CuentaCorrienteDAO;
import veterinaria.persistencia.CuentaCorrienteMovimientoDAO;
import static veterinaria.util.Constantes.*;
import veterinaria.util.enums.MetodoPagoTipo;
import veterinaria.util.ManejoTablas;
import veterinaria.util.SesionUsuario;
import veterinaria.util.VentaProducto;
import veterinaria.vista.application.Application;
import veterinaria.util.AppLog;
import veterinaria.util.PermisoUI;
import veterinaria.util.AuditoriaLogger;
import veterinaria.util.MoneyUtil;
import veterinaria.persistencia.CompraProveedorDAO;
import veterinaria.persistencia.CuentaCorrienteProveedorDAO;
import veterinaria.persistencia.CuentaCorrienteProveedorMovimientoDAO;
import veterinaria.entidad.CompraProveedor;
import veterinaria.entidad.CompraProveedorPago;
import veterinaria.entidad.CompraProveedorEstado;
import veterinaria.util.Constantes;

public class FormCajaRegistradora extends javax.swing.JPanel {
    private SesionUsuario sesion = Application.getSesionUsuario();
    private final ManejoTablas operarTabla = new ManejoTablas();
    private final CajaMovimientoControlador operarCaja = new CajaMovimientoControlador();
    private final Usuario usuario = Application.getSesionUsuario().getUsuario();
    private final ClienteControlador operandoCliente = new ClienteControlador();
    private final ProductoControlador operandoProducto = new ProductoControlador();
    private final ProveedorControlador operandoProveedor = new ProveedorControlador();
    private final RubroControlador operandoRubro = new RubroControlador();
    private final ConfiguracionService configService = new ConfiguracionService();
    private final ManejoTablas operandoTablas = new ManejoTablas();
    private final MetodoPagoControlador operandoMetodoPago = new MetodoPagoControlador();
    private List<CuentaCorrienteMovimiento> movimientosCC = new ArrayList<>();
    private CuentaCorrienteMovimiento movimientoCuentaC = new CuentaCorrienteMovimiento();
    private Recibo recibo = new Recibo();
    private final CajaMovimiento movimientoEfectivo = new CajaMovimiento();
    private final CuentaCorrienteMovimientoDAO operarCCMovimientos = new CuentaCorrienteMovimientoDAO();
    private List<VentaProducto> listaProductosAVender = new ArrayList<>();
    private VentaProducto productoAVender = new VentaProducto();
    private Producto producto = new Producto();
    private final ComprobantePdfService comprobantePdf = new ComprobantePdfService();

    // Fecha de la caja que se está cerrando (puede ser de días anteriores si quedó abierta sin cierre)
    private Date fechaCajaEnCierre = null;
    // Cuando se cierra una caja pendiente (día anterior), guardamos el ID de la apertura
    // para evitar errores por comparación de fechas.
    private Long aperturaIdEnCierre = null;
    private Long ultimoIdReciboRegistrado = null;
    private Timer relojTimer = null;

    public FormCajaRegistradora() {
        initComponents();
        txtTotalFacturaCompra.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autocompletarMontoFacturaCompraDesdeTotal();
            }
        });        
        PermisoUI.aplicar(this);
        configurarAtajosReimpresion();
        configurarCamposMonetarios();
        actualizarForm();
        jpAperturaCaja.setVisible(false);
        jpCierreCaja.setVisible(false);
        jpRegistrarVentas.setVisible(false);
        jpRegistrarCobros.setVisible(false);
        jpRegistrarCompras.setVisible(false);
        cargarDatosComboBoxes();
        this.revalidate();
        this.repaint();
    }

    /**
     * Evita errores de parseo ("12.075,00") unificando: - Input: solo números y
     * '.' (se acepta ',' y se normaliza a '.') - Display: siempre 2 decimales
     * estilo estándar (12075.00)
     */
    private void configurarCamposMonetarios() {
        // Campos donde el usuario escribe montos
        aplicarFiltroMonetario(txtMontoInicialCaja);
        aplicarFiltroMonetario(txtMontoCierreCaja);
        aplicarFiltroMonetario(txtMontoFormaDePago);
        aplicarFiltroMonetario(txtMontoPagoCC);
        aplicarFiltroMonetario(txtTotalFacturaCompra);
        aplicarFiltroMonetario(txtMontoFacturaCompra);

        // Campos calculados (solo lectura) que luego se vuelven a parsear: asegurar estándar
        normalizarEnFocusLost(txtSaldoPendiente);
        normalizarEnFocusLost(txtSubtotalVenta);
        normalizarEnFocusLost(txtDescuentoVenta);
        normalizarEnFocusLost(txtIvaVenta);
        normalizarEnFocusLost(txtTotalVenta);
        normalizarEnFocusLost(txtIngresoCierreCaja);
        normalizarEnFocusLost(txtEgresosCierreCaja);
        normalizarEnFocusLost(txtTotalFacturaCompra);
        normalizarEnFocusLost(txtMontoFacturaCompra);
    }

    private void aplicarFiltroMonetario(javax.swing.JTextField field) {
        if (field == null) {
            return;
        }
        if (field.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new MoneyDocumentFilter());
        }
        normalizarEnFocusLost(field);
    }

    private void normalizarEnFocusLost(javax.swing.JTextField field) {
        if (field == null) {
            return;
        }
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String raw = field.getText();
                if (raw == null) {
                    return;
                }
                raw = raw.trim();
                if (raw.isEmpty()) {
                    return;
                }
                try {
                    BigDecimal v = MoneyUtil.parse(raw);
                    field.setText(MoneyUtil.formatStandard(v));
                } catch (Exception ignore) {
                    // si el usuario ingresó algo inválido, lo dejamos tal cual y validará donde corresponda
                }
            }
        });
    }

    /**
     * Compras: copia el Total de Factura al campo Monto Factura al salir del Total (por TAB o cambio de foco).
     */
    private void autocompletarMontoFacturaCompraDesdeTotal() {
        if (txtTotalFacturaCompra == null || txtMontoFacturaCompra == null) {
            return;
        }
        String raw = txtTotalFacturaCompra.getText();
        if (raw == null) {
            return;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return;
        }
        try {
            java.math.BigDecimal total = veterinaria.util.MoneyUtil.parse(raw);
            txtMontoFacturaCompra.setText(veterinaria.util.MoneyUtil.format(total));
        } catch (Exception ex) {
            // Si el total no es válido, no autocompletar.
        }
    }


    /**
     * DocumentFilter para permitir únicamente: - dígitos - un solo '.' decimal
     * - convierte ',' a '.' - ignora separadores de miles (.) adicionales
     */
    private static class MoneyDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) {
                return;
            }
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) {
                return;
            }
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String before = current.substring(0, offset);
            String after = current.substring(offset + length);

            String incoming = text.replace(',', '.');

            // permitir solo dígitos y '.'
            incoming = incoming.replaceAll("[^0-9.]", "");

            // construir el candidato
            String candidate = before + incoming + after;

            // si hay más de un punto, mantener solo el primero como decimal
            int firstDot = candidate.indexOf('.');
            if (firstDot != -1) {
                String left = candidate.substring(0, firstDot + 1);
                String right = candidate.substring(firstDot + 1).replace(".", "");
                candidate = left + right;
            }

            // aplicar: reemplazar por el incoming "limpio" pero respetando corrección del candidato
            // Para simplificar, seteamos todo el documento al candidate final.
            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, candidate, attrs);
        }
    }

    private void configurarAtajosReimpresion() {
        javax.swing.InputMap im = this.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.ActionMap am = this.getActionMap();

        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0), "REIMPRIMIR_RECIBO");
        am.put("REIMPRIMIR_RECIBO", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reimprimirReciboPorId();
            }
        });

        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0), "REIMPRIMIR_COBRO_CC");
        am.put("REIMPRIMIR_COBRO_CC", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reimprimirCobroCCPorId();
            }
        });
    }

    private boolean puedeReimprimir() {
        // Permiso fino (si no existe en el esquema actual, SesionUsuario.puede suele devolver true)
        return Application.getSesionUsuario().puede("FormCajaRegistradora.REIMPRIMIR_COMPROBANTES");
    }

    private void reimprimirReciboPorId() {
        if (!puedeReimprimir()) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para reimprimir comprobantes.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Ingrese ID de Recibo a reimprimir:");
        if (input == null) {
            return;
        }
        input = input.trim();
        if (input.isEmpty()) {
            return;
        }
        try {
            long id = Long.parseLong(input);
            File pdf = comprobantePdf.generarReciboVentaPdf(id);
            AuditoriaLogger.evento("REIMPRESION_RECIBO_VENTA", "idRecibo=" + id + " file=" + pdf.getAbsolutePath(), usuario);
            abrirPdf(pdf);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo reimprimir el recibo: " + ex.getMessage());
        }
    }

    private void reimprimirCobroCCPorId() {
        if (!puedeReimprimir()) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para reimprimir comprobantes.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Ingrese ID de Movimiento (Cobro CC) a reimprimir:");
        if (input == null) {
            return;
        }
        input = input.trim();
        if (input.isEmpty()) {
            return;
        }
        try {
            int id = Integer.parseInt(input);
            File pdf = comprobantePdf.generarComprobanteCobroCuentaCorrientePdf(id);
            AuditoriaLogger.evento("REIMPRESION_COBRO_CC", "idMovimiento=" + id + " file=" + pdf.getAbsolutePath(), usuario);
            abrirPdf(pdf);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo reimprimir el comprobante: " + ex.getMessage());
        }
    }

    private void abrirPdf(File pdf) {
        try {
            if (pdf != null && pdf.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(pdf);
            }
        } catch (Exception ex) {
            AppLog.warning(FormCajaRegistradora.class, "No se pudo abrir el PDF generado: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHadear = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        lbCajaRegistradora = new javax.swing.JLabel();
        btnCerrarCaja = new javax.swing.JButton();
        btnAbrirCaja = new javax.swing.JButton();
        btnVentas = new javax.swing.JButton();
        btnCobros = new javax.swing.JButton();
        btnCompras = new javax.swing.JButton();
        jpAperturaCaja = new javax.swing.JPanel();
        lbApertura = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        txtMontoInicialCaja = new javax.swing.JTextField();
        lbMontoInicial = new javax.swing.JLabel();
        lbSingoP = new javax.swing.JLabel();
        btnRegistrarAperturaCaja = new javax.swing.JButton();
        btnCancelarAperturaCaja = new javax.swing.JButton();
        jpCierreCaja = new javax.swing.JPanel();
        lbCierreCaja = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        txtMontoCierreCaja = new javax.swing.JTextField();
        lbMontoFinal = new javax.swing.JLabel();
        lbSignoPeso = new javax.swing.JLabel();
        lbIngreso = new javax.swing.JLabel();
        lbSignoPeso1 = new javax.swing.JLabel();
        txtIngresoCierreCaja = new javax.swing.JTextField();
        btnRegistrarCierreCaja = new javax.swing.JButton();
        lbEgreso = new javax.swing.JLabel();
        lbSignoPeso2 = new javax.swing.JLabel();
        txtEgresosCierreCaja = new javax.swing.JTextField();
        btnCancelarCierreCaja = new javax.swing.JButton();
        jpRegistrarVentas = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        lbCliente = new javax.swing.JLabel();
        jcbClienteVenta = new javax.swing.JComboBox<>();
        lbProducto = new javax.swing.JLabel();
        jcbProductoVenta = new javax.swing.JComboBox<>();
        lbCantidad = new javax.swing.JLabel();
        txtCantidadVenta = new javax.swing.JTextField();
        btnAgregarProductoVenta = new javax.swing.JButton();
        scroll1 = new javax.swing.JScrollPane();
        tableVentas = new veterinaria.vista.table.AutoTable();
        lbSubTotal = new javax.swing.JLabel();
        txtSubtotalVenta = new javax.swing.JTextField();
        lbDescuento = new javax.swing.JLabel();
        lbIva = new javax.swing.JLabel();
        lbTotal = new javax.swing.JLabel();
        txtTotalVenta = new javax.swing.JTextField();
        txtIvaVenta = new javax.swing.JTextField();
        jSeparator7 = new javax.swing.JSeparator();
        txtDescuentoVenta = new javax.swing.JTextField();
        btnConfirmarVenta = new javax.swing.JButton();
        btnLimpiarVenta = new javax.swing.JButton();
        jpFormaDePago = new javax.swing.JPanel();
        lbFormaDePago1 = new javax.swing.JLabel();
        lbFormaDePago = new javax.swing.JLabel();
        jcbMetodoDePago = new javax.swing.JComboBox<>();
        txtMontoFormaDePago = new javax.swing.JTextField();
        lbMontoDesc = new javax.swing.JLabel();
        scroll2 = new javax.swing.JScrollPane();
        tableFormaDePago = new javax.swing.JTable();
        btnLimpiarPagos = new javax.swing.JButton();
        btnCancelarRegistroVenta = new javax.swing.JButton();
        btnRegistrarVenta = new javax.swing.JButton();
        btnAgregarMetodoPago = new javax.swing.JButton();
        txtSaldoPendiente = new javax.swing.JTextField();
        lbMontoDesc1 = new javax.swing.JLabel();
        jSeparator9 = new javax.swing.JSeparator();
        lbSignoPeso3 = new javax.swing.JLabel();
        lbSignoPeso4 = new javax.swing.JLabel();
        jpRegistrarCobros = new javax.swing.JPanel();
        jSeparator8 = new javax.swing.JSeparator();
        lbFormaDePago2 = new javax.swing.JLabel();
        jcbClienteCobro = new javax.swing.JComboBox<>();
        lbCliente1 = new javax.swing.JLabel();
        txtMontoPagoCC = new javax.swing.JTextField();
        lbSignoPeso5 = new javax.swing.JLabel();
        lbMontoDesc2 = new javax.swing.JLabel();
        scroll3 = new javax.swing.JScrollPane();
        tableCuentaCorriente = new javax.swing.JTable();
        btnRegistrarCobro = new javax.swing.JButton();
        txtSaldoCuentaCorriente = new javax.swing.JTextField();
        lbMontoDesc3 = new javax.swing.JLabel();
        lbSignoPeso6 = new javax.swing.JLabel();
        btnCancelarRegistroVenta1 = new javax.swing.JButton();
        jpInfoCaja = new javax.swing.JPanel();
        jSeparator5 = new javax.swing.JSeparator();
        lbFechaDesc = new javax.swing.JLabel();
        lblFechaActual = new javax.swing.JLabel();
        lblHoraActual = new javax.swing.JLabel();
        lbUsuarioDesc = new javax.swing.JLabel();
        lblUsuario = new javax.swing.JLabel();
        lbEstadoCajaDesc = new javax.swing.JLabel();
        lblEstadoCaja = new javax.swing.JLabel();
        jpRegistrarCompras = new javax.swing.JPanel();
        jSeparator10 = new javax.swing.JSeparator();
        lbCompras = new javax.swing.JLabel();
        jcbProveedorCompra = new javax.swing.JComboBox<>();
        lbProveedorCompra = new javax.swing.JLabel();
        lbNumeroFactura = new javax.swing.JLabel();
        txtMontoFacturaCompra = new javax.swing.JTextField();
        lbSignoPesoCompra = new javax.swing.JLabel();
        lbMontoCompra = new javax.swing.JLabel();
        scroll4 = new javax.swing.JScrollPane();
        tablePagosCompra = new javax.swing.JTable();
        btnRegistrarPagoCompra = new javax.swing.JButton();
        txtTotalFacturaCompra = new javax.swing.JTextField();
        lbTotalFacturaCompra = new javax.swing.JLabel();
        lbSignoPeso8 = new javax.swing.JLabel();
        btnCancelarCompra = new javax.swing.JButton();
        lbFormaDePagoCompra = new javax.swing.JLabel();
        jcbMetodoDePagoCompra = new javax.swing.JComboBox<>();
        btnRegistrarCompra = new javax.swing.JButton();
        txtNumeroFactura = new javax.swing.JTextField();
        lbNumeroFacturaCompra = new javax.swing.JLabel();
        jdcFechaFacturaDeCompra = new com.toedter.calendar.JDateChooser();
        lbFechaFacturaCompra = new javax.swing.JLabel();
        btnLimpiarPagosCompras = new javax.swing.JButton();

        lbCajaRegistradora.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbCajaRegistradora.setText("Movimientos de Caja");

        btnCerrarCaja.setText("Cerrar Caja");
        btnCerrarCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarCajaActionPerformed(evt);
            }
        });

        btnAbrirCaja.setText("Abrir Caja");
        btnAbrirCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbrirCajaActionPerformed(evt);
            }
        });

        btnVentas.setText("Ventas");
        btnVentas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVentasActionPerformed(evt);
            }
        });

        btnCobros.setText("Cobros");
        btnCobros.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCobrosActionPerformed(evt);
            }
        });

        btnCompras.setText("Compras");
        btnCompras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnComprasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpHadearLayout = new javax.swing.GroupLayout(jpHadear);
        jpHadear.setLayout(jpHadearLayout);
        jpHadearLayout.setHorizontalGroup(
            jpHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHadearLayout.createSequentialGroup()
                .addComponent(jSeparator1)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHadearLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbCajaRegistradora)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAbrirCaja)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCerrarCaja)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnVentas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCobros)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCompras)
                .addGap(8, 8, 8))
        );
        jpHadearLayout.setVerticalGroup(
            jpHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHadearLayout.createSequentialGroup()
                .addContainerGap(19, Short.MAX_VALUE)
                .addGroup(jpHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHadearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCerrarCaja)
                        .addComponent(btnAbrirCaja)
                        .addComponent(btnVentas)
                        .addComponent(btnCobros)
                        .addComponent(btnCompras))
                    .addComponent(lbCajaRegistradora))
                .addGap(10, 10, 10)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        lbApertura.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbApertura.setText("Apertura de Caja");

        lbMontoInicial.setText("Monto Inicial:");

        lbSingoP.setText("$");

        btnRegistrarAperturaCaja.setText("Registrar Apertura Caja");
        btnRegistrarAperturaCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarAperturaCajaActionPerformed(evt);
            }
        });

        btnCancelarAperturaCaja.setText("Cancelar");
        btnCancelarAperturaCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarAperturaCajaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpAperturaCajaLayout = new javax.swing.GroupLayout(jpAperturaCaja);
        jpAperturaCaja.setLayout(jpAperturaCajaLayout);
        jpAperturaCajaLayout.setHorizontalGroup(
            jpAperturaCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpAperturaCajaLayout.createSequentialGroup()
                .addGroup(jpAperturaCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator2)
                    .addGroup(jpAperturaCajaLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpAperturaCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpAperturaCajaLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(lbSingoP)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpAperturaCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpAperturaCajaLayout.createSequentialGroup()
                                        .addComponent(lbMontoInicial)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(jpAperturaCajaLayout.createSequentialGroup()
                                        .addComponent(txtMontoInicialCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnRegistrarAperturaCaja)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnCancelarAperturaCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jpAperturaCajaLayout.createSequentialGroup()
                                .addComponent(lbApertura)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jpAperturaCajaLayout.setVerticalGroup(
            jpAperturaCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAperturaCajaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbApertura, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbMontoInicial)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpAperturaCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMontoInicialCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbSingoP)
                    .addComponent(btnRegistrarAperturaCaja)
                    .addComponent(btnCancelarAperturaCaja))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbCierreCaja.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbCierreCaja.setText("Cierre de Caja");

        lbMontoFinal.setText("Monto Final:");

        lbSignoPeso.setText("$");

        lbIngreso.setText("Ingresos:");

        lbSignoPeso1.setText("$");

        btnRegistrarCierreCaja.setText("Registrar Cierre Caja");
        btnRegistrarCierreCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarCierreCajaActionPerformed(evt);
            }
        });

        lbEgreso.setText("Egreso:");

        lbSignoPeso2.setText("$");

        btnCancelarCierreCaja.setText("Cancelar");
        btnCancelarCierreCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarCierreCajaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpCierreCajaLayout = new javax.swing.GroupLayout(jpCierreCaja);
        jpCierreCaja.setLayout(jpCierreCajaLayout);
        jpCierreCajaLayout.setHorizontalGroup(
            jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCierreCajaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator3)
                    .addGroup(jpCierreCajaLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lbSignoPeso)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbMontoFinal)
                            .addComponent(txtMontoCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(lbSignoPeso1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtIngresoCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbIngreso))
                        .addGap(18, 18, 18)
                        .addComponent(lbSignoPeso2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpCierreCajaLayout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(lbEgreso))
                            .addComponent(txtEgresosCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRegistrarCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelarCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(jpCierreCajaLayout.createSequentialGroup()
                        .addComponent(lbCierreCaja)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jpCierreCajaLayout.setVerticalGroup(
            jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCierreCajaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbCierreCaja)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpCierreCajaLayout.createSequentialGroup()
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpCierreCajaLayout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addComponent(lbMontoFinal)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtMontoCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbSignoPeso)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnRegistrarCierreCaja)
                                .addComponent(btnCancelarCierreCaja))))
                    .addGroup(jpCierreCajaLayout.createSequentialGroup()
                        .addComponent(lbIngreso)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtIngresoCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbSignoPeso1)))
                    .addGroup(jpCierreCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jpCierreCajaLayout.createSequentialGroup()
                            .addGap(22, 22, 22)
                            .addComponent(lbSignoPeso2, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpCierreCajaLayout.createSequentialGroup()
                            .addComponent(lbEgreso)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtEgresosCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("Registro de Ventas");

        lbCliente.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbCliente.setText("Cliente:");

        lbProducto.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbProducto.setText("Producto:");

        lbCantidad.setText("Cantidad:");

        btnAgregarProductoVenta.setText("Agregar");
        btnAgregarProductoVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarProductoVentaActionPerformed(evt);
            }
        });

        scroll1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableVentas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Código", "Producto", "Precio Unidad", "IVA Acumulado", "Descuento Acumulado", "Cantidad", "Total"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableVentas.setMinimumSize(new java.awt.Dimension(848, 220));
        tableVentas.setPreferredSize(new java.awt.Dimension(848, 220));
        tableVentas.getTableHeader().setReorderingAllowed(false);
        scroll1.setViewportView(tableVentas);

        lbSubTotal.setText("Subtotal:");

        lbDescuento.setText("Descuento:");

        lbIva.setText("IVA:");

        lbTotal.setText("Total productos:");

        btnConfirmarVenta.setText("Confirmar");
        btnConfirmarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarVentaActionPerformed(evt);
            }
        });

        btnLimpiarVenta.setText("Limpiar");
        btnLimpiarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarVentaActionPerformed(evt);
            }
        });

        lbFormaDePago1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbFormaDePago1.setText("Forma de Pago:");

        lbFormaDePago.setText("Metodo de Pago:");

        lbMontoDesc.setText("Monto:");

        scroll2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableFormaDePago.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Metodo de Pago", "Monto", "Descripción"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableFormaDePago.setMinimumSize(new java.awt.Dimension(848, 220));
        tableFormaDePago.setPreferredSize(new java.awt.Dimension(848, 220));
        tableFormaDePago.getTableHeader().setReorderingAllowed(false);
        scroll2.setViewportView(tableFormaDePago);

        btnLimpiarPagos.setText("Limpiar Pagos");
        btnLimpiarPagos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarPagosActionPerformed(evt);
            }
        });

        btnCancelarRegistroVenta.setText("Cancelar");
        btnCancelarRegistroVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarRegistroVentaActionPerformed(evt);
            }
        });

        btnRegistrarVenta.setText("Registrar Venta");
        btnRegistrarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarVentaActionPerformed(evt);
            }
        });

        btnAgregarMetodoPago.setText("Agregar");
        btnAgregarMetodoPago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarMetodoPagoActionPerformed(evt);
            }
        });

        txtSaldoPendiente.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        txtSaldoPendiente.setForeground(new java.awt.Color(255, 0, 0));
        txtSaldoPendiente.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtSaldoPendienteMouseClicked(evt);
            }
        });

        lbMontoDesc1.setText("Saldo:");

        lbSignoPeso3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbSignoPeso3.setText("$");

        lbSignoPeso4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbSignoPeso4.setText("$");

        javax.swing.GroupLayout jpFormaDePagoLayout = new javax.swing.GroupLayout(jpFormaDePago);
        jpFormaDePago.setLayout(jpFormaDePagoLayout);
        jpFormaDePagoLayout.setHorizontalGroup(
            jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbFormaDePago1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbFormaDePago)
                            .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                                .addComponent(jcbMetodoDePago, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbSignoPeso4, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbMontoDesc)
                            .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                                .addComponent(txtMontoFormaDePago, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAgregarMetodoPago)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbSignoPeso3, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(3, 3, 3)
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbMontoDesc1, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtSaldoPendiente, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpFormaDePagoLayout.createSequentialGroup()
                        .addComponent(btnLimpiarPagos, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCancelarRegistroVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(btnRegistrarVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addComponent(scroll2)
            .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                    .addGap(3, 3, 3)
                    .addComponent(jSeparator9, javax.swing.GroupLayout.DEFAULT_SIZE, 830, Short.MAX_VALUE)
                    .addGap(3, 3, 3)))
        );
        jpFormaDePagoLayout.setVerticalGroup(
            jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbFormaDePago1)
                .addGap(18, 18, 18)
                .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbFormaDePago)
                            .addComponent(lbMontoDesc))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jcbMetodoDePago, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtMontoFormaDePago, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnAgregarMetodoPago)
                            .addComponent(lbSignoPeso4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scroll2, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnRegistrarVenta)
                            .addComponent(btnCancelarRegistroVenta)
                            .addComponent(btnLimpiarPagos)))
                    .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                        .addComponent(lbMontoDesc1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSaldoPendiente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbSignoPeso3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))))
            .addGroup(jpFormaDePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpFormaDePagoLayout.createSequentialGroup()
                    .addGap(29, 29, 29)
                    .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(209, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout jpRegistrarVentasLayout = new javax.swing.GroupLayout(jpRegistrarVentas);
        jpRegistrarVentas.setLayout(jpRegistrarVentasLayout);
        jpRegistrarVentasLayout.setHorizontalGroup(
            jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scroll1)
                    .addComponent(jpFormaDePago, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                        .addComponent(jcbProductoVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbCantidad)
                                            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                                .addComponent(txtCantidadVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnAgregarProductoVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addComponent(jcbClienteVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbProducto)
                                            .addComponent(lbCliente))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator4))
                            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                .addComponent(lbSubTotal)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSubtotalVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbDescuento)
                                .addGap(12, 12, 12)
                                .addComponent(txtDescuentoVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(lbIva)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtIvaVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(27, 27, 27)
                                .addComponent(lbTotal)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtTotalVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnLimpiarVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnConfirmarVenta)))))
                .addContainerGap())
            .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarVentasLayout.createSequentialGroup()
                    .addComponent(jSeparator7, javax.swing.GroupLayout.DEFAULT_SIZE, 836, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jpRegistrarVentasLayout.setVerticalGroup(
            jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                        .addComponent(lbCliente)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbClienteVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                .addComponent(lbProducto)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbProductoVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                                .addComponent(lbCantidad)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtCantidadVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnAgregarProductoVenta))))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scroll1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbSubTotal)
                    .addComponent(lbDescuento)
                    .addComponent(lbIva)
                    .addComponent(lbTotal)
                    .addComponent(txtSubtotalVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTotalVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtIvaVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDescuentoVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnConfirmarVenta)
                    .addComponent(btnLimpiarVenta))
                .addGap(20, 20, 20)
                .addComponent(jpFormaDePago, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(9, Short.MAX_VALUE))
            .addGroup(jpRegistrarVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpRegistrarVentasLayout.createSequentialGroup()
                    .addGap(286, 286, 286)
                    .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(302, Short.MAX_VALUE)))
        );

        lbFormaDePago2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbFormaDePago2.setText("Registro de Cobros Cuenta Corriente:");

        lbCliente1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbCliente1.setText("Cliente:");

        lbSignoPeso5.setText("$");

        lbMontoDesc2.setText("Monto:");

        scroll3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableCuentaCorriente.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Código", "Descripción", "Tipo Movimiento", "Fecha", "Monto"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableCuentaCorriente.setMinimumSize(new java.awt.Dimension(848, 220));
        tableCuentaCorriente.setPreferredSize(new java.awt.Dimension(848, 220));
        tableCuentaCorriente.getTableHeader().setReorderingAllowed(false);
        scroll3.setViewportView(tableCuentaCorriente);
        if (tableCuentaCorriente.getColumnModel().getColumnCount() > 0) {
            tableCuentaCorriente.getColumnModel().getColumn(1).setHeaderValue("Descripción");
            tableCuentaCorriente.getColumnModel().getColumn(2).setHeaderValue("Tipo Movimiento");
            tableCuentaCorriente.getColumnModel().getColumn(3).setHeaderValue("Fecha");
        }

        btnRegistrarCobro.setText("Registrar pago");
        btnRegistrarCobro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarCobroActionPerformed(evt);
            }
        });

        lbMontoDesc3.setText("Saldo:");

        lbSignoPeso6.setText("$");

        btnCancelarRegistroVenta1.setText("Cancelar");
        btnCancelarRegistroVenta1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarRegistroVenta1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpRegistrarCobrosLayout = new javax.swing.GroupLayout(jpRegistrarCobros);
        jpRegistrarCobros.setLayout(jpRegistrarCobrosLayout);
        jpRegistrarCobrosLayout.setHorizontalGroup(
            jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator8)
            .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scroll3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                        .addComponent(lbFormaDePago2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                        .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbCliente1)
                            .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                                .addComponent(jcbClienteCobro, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbSignoPeso6)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtSaldoCuentaCorriente, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbMontoDesc3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbSignoPeso5, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbMontoDesc2)
                            .addComponent(txtMontoPagoCC, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnRegistrarCobro))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarCobrosLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnCancelarRegistroVenta1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jpRegistrarCobrosLayout.setVerticalGroup(
            jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnRegistrarCobro)
                    .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                        .addComponent(lbFormaDePago2)
                        .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                                .addGap(5, 5, 5)
                                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lbCliente1)
                                .addGap(5, 5, 5)
                                .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jcbClienteCobro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lbSignoPeso6, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarCobrosLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbMontoDesc3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSaldoCuentaCorriente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jpRegistrarCobrosLayout.createSequentialGroup()
                        .addComponent(lbMontoDesc2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarCobrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtMontoPagoCC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbSignoPeso5, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll3, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelarRegistroVenta1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbFechaDesc.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbFechaDesc.setText("Fecha: ");

        lblFechaActual.setText("06/10/24");

        lblHoraActual.setText("08:45:00");

        lbUsuarioDesc.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbUsuarioDesc.setText("Usuario:");

        lblUsuario.setText("Samuel Alejandro");

        lbEstadoCajaDesc.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbEstadoCajaDesc.setText("Estado Caja:");

        lblEstadoCaja.setText("Caja abierta");

        javax.swing.GroupLayout jpInfoCajaLayout = new javax.swing.GroupLayout(jpInfoCaja);
        jpInfoCaja.setLayout(jpInfoCajaLayout);
        jpInfoCajaLayout.setHorizontalGroup(
            jpInfoCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpInfoCajaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbFechaDesc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblFechaActual, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblHoraActual)
                .addGap(39, 39, 39)
                .addComponent(lbUsuarioDesc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblUsuario)
                .addGap(92, 92, 92)
                .addComponent(lbEstadoCajaDesc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblEstadoCaja)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpInfoCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpInfoCajaLayout.createSequentialGroup()
                    .addComponent(jSeparator5, javax.swing.GroupLayout.DEFAULT_SIZE, 845, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jpInfoCajaLayout.setVerticalGroup(
            jpInfoCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpInfoCajaLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jpInfoCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbFechaDesc)
                    .addComponent(lblFechaActual)
                    .addComponent(lblHoraActual)
                    .addComponent(lbUsuarioDesc)
                    .addComponent(lblUsuario)
                    .addComponent(lbEstadoCajaDesc)
                    .addComponent(lblEstadoCaja))
                .addContainerGap(19, Short.MAX_VALUE))
            .addGroup(jpInfoCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpInfoCajaLayout.createSequentialGroup()
                    .addContainerGap(45, Short.MAX_VALUE)
                    .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );

        lbCompras.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbCompras.setText("Compras / Salida / Egreso de Caja");

        lbProveedorCompra.setText("Proveedor:");

        lbNumeroFactura.setText("Nº Factura:");

        lbSignoPesoCompra.setText(" $");

        lbMontoCompra.setText("Monto Pago:");

        scroll4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tablePagosCompra.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Método Pago", "Monto"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablePagosCompra.setMinimumSize(new java.awt.Dimension(848, 220));
        tablePagosCompra.setPreferredSize(new java.awt.Dimension(848, 220));
        tablePagosCompra.getTableHeader().setReorderingAllowed(false);
        scroll4.setViewportView(tablePagosCompra);

        btnRegistrarPagoCompra.setText("Agregar Pago");
        btnRegistrarPagoCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarPagoCompraActionPerformed(evt);
            }
        });

        lbTotalFacturaCompra.setText("Total Factura:");

        lbSignoPeso8.setText(" $");

        btnCancelarCompra.setText("Cancelar");
        btnCancelarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarCompraActionPerformed(evt);
            }
        });

        lbFormaDePagoCompra.setText("Metodo de Pago:");

        btnRegistrarCompra.setText("Registrar Compra");
        btnRegistrarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarCompraActionPerformed(evt);
            }
        });

        lbNumeroFacturaCompra.setText("Nº Factura:");

        lbFechaFacturaCompra.setText("Fecha Factura:");

        btnLimpiarPagosCompras.setText("Limpiar Pagos");
        btnLimpiarPagosCompras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarPagosComprasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpRegistrarComprasLayout = new javax.swing.GroupLayout(jpRegistrarCompras);
        jpRegistrarCompras.setLayout(jpRegistrarComprasLayout);
        jpRegistrarComprasLayout.setHorizontalGroup(
            jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator10)
            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                        .addComponent(btnLimpiarPagosCompras)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRegistrarCompra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelarCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lbProveedorCompra)
                                    .addComponent(lbFormaDePagoCompra)
                                    .addComponent(jcbMetodoDePagoCompra, 0, 260, Short.MAX_VALUE)
                                    .addComponent(jcbProveedorCompra, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                        .addComponent(jdcFechaFacturaDeCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lbSignoPesoCompra)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtMontoFacturaCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                                .addComponent(txtNumeroFactura, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(lbSignoPeso8))
                                            .addComponent(lbNumeroFacturaCompra)
                                            .addComponent(lbFechaFacturaCompra))
                                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(lbTotalFacturaCompra)
                                                    .addComponent(txtTotalFacturaCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                                .addGap(6, 6, 6)
                                                .addComponent(lbMontoCompra))))))
                            .addComponent(lbCompras)
                            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                                .addComponent(scroll4, javax.swing.GroupLayout.PREFERRED_SIZE, 514, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRegistrarPagoCompra)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpRegistrarComprasLayout.setVerticalGroup(
            jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbCompras)
                .addGap(5, 5, 5)
                .addComponent(jSeparator10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                        .addComponent(lbProveedorCompra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbProveedorCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbTotalFacturaCompra)
                            .addComponent(lbNumeroFacturaCompra))
                        .addGap(5, 5, 5)
                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtNumeroFactura, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbSignoPeso8, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTotalFacturaCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(12, 12, 12)
                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtMontoFacturaCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbSignoPesoCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarComprasLayout.createSequentialGroup()
                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbFechaFacturaCompra)
                            .addComponent(lbMontoCompra))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdcFechaFacturaDeCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarComprasLayout.createSequentialGroup()
                        .addComponent(lbFormaDePagoCompra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbMetodoDePagoCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpRegistrarComprasLayout.createSequentialGroup()
                        .addComponent(scroll4, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCancelarCompra)
                            .addComponent(btnRegistrarCompra)
                            .addComponent(btnLimpiarPagosCompras)))
                    .addComponent(btnRegistrarPagoCompra))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jpInfoCaja, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpHadear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpRegistrarVentas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpCierreCaja, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpAperturaCaja, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpRegistrarCobros, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jpRegistrarCompras, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHadear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpInfoCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpAperturaCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpCierreCaja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpRegistrarVentas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpRegistrarCobros, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpRegistrarCompras, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnRegistrarAperturaCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarAperturaCajaActionPerformed
        if (txtMontoInicialCaja.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un monto de apertura de caja!");
            return;
        }
        try {
            BigDecimal monto = MoneyUtil.parse(txtMontoInicialCaja.getText());
            if (monto == null || monto.signum() < 0) {
                throw new IllegalArgumentException("El monto de apertura no puede ser negativo.");
            }
            if (!operarCaja.registrarApertura(monto, usuario)) {
                throw new IllegalStateException(operarCaja.getUltimoError() == null
                        ? "No se pudo abrir la caja." : operarCaja.getUltimoError());
            }
            JOptionPane.showMessageDialog(this, "Caja abierta correctamente.");
            jpAperturaCaja.setVisible(false);
            actualizarForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se puede abrir caja", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnRegistrarAperturaCajaActionPerformed

    private void btnCerrarCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarCajaActionPerformed
        String perm = "FormCajaRegistradora.CERRAR_CAJA";
        if (sesion != null && sesion.puede(perm)) {
            cierreDeCajaDiario();
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "No tienes permisos suficientes para cerrar caja.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }//GEN-LAST:event_btnCerrarCajaActionPerformed

    private void btnAbrirCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbrirCajaActionPerformed
        if ("ABIERTA".equals(operarCaja.estadoCaja())) {
            JOptionPane.showMessageDialog(this, "Ya existe una sesión de caja abierta.");
            return;
        }
        jpRegistrarCobros.setVisible(false);
        jpCierreCaja.setVisible(false);
        jpRegistrarVentas.setVisible(false);
        jpRegistrarCompras.setVisible(false);
        jpAperturaCaja.setVisible(true);
        txtMontoInicialCaja.setText("0.00");
    }//GEN-LAST:event_btnAbrirCajaActionPerformed

    private void btnVentasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVentasActionPerformed
        String estadoCaja = operarCaja.estadoCaja();
        jpRegistrarCobros.setVisible(false);
        jpAperturaCaja.setVisible(false);
        jpCierreCaja.setVisible(false);
        jpRegistrarCompras.setVisible(false);

        if ("ABIERTA".equals(estadoCaja)) {
            jpRegistrarVentas.setVisible(true);
            jpFormaDePago.setVisible(false);
            this.revalidate();
            this.repaint();
            txtCantidadVenta.setText("1");
        } else {
            JOptionPane.showMessageDialog(this, "No es posible acceder a Ventas. La caja aún está cerrada!");
        }


    }//GEN-LAST:event_btnVentasActionPerformed

    private void btnCobrosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCobrosActionPerformed
        if (!"ABIERTA".equals(operarCaja.estadoCaja())) {
            JOptionPane.showMessageDialog(this, "No es posible acceder a Cobros. La caja aún está cerrada!");
            return;
        }
        jpRegistrarCobros.setVisible(true);
        jpAperturaCaja.setVisible(false);
        jpCierreCaja.setVisible(false);
        jpRegistrarVentas.setVisible(false);
        jpRegistrarCompras.setVisible(false);
    }//GEN-LAST:event_btnCobrosActionPerformed

    private void btnAgregarProductoVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarProductoVentaActionPerformed
        Integer idProductoSelec = obtenerIdProductoSeleccionado();
        if (idProductoSelec == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un producto.");
            return;
        }

        Integer cantidadProducto = obtenerCantidadVentaValidada();
        if (cantidadProducto == null) {
            return;
        }

        producto = operandoProducto.buscarProductoPorId(idProductoSelec);
        if (producto == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el producto seleccionado.");
            return;
        }

        Integer stockProducto = producto.getStock();
        if (validarStockProducto(stockProducto, cantidadProducto, idProductoSelec)) {
            calcularMontosProductoUnitario(productoAVender, idProductoSelec, cantidadProducto);
            controlarProductosIgualesEnTabla();
            cargarProductosEnTabla(listaProductosAVender);
        } else {
            JOptionPane.showMessageDialog(this, "No existe suficiente stock para agregar a la venta.");
        }
    }//GEN-LAST:event_btnAgregarProductoVentaActionPerformed

    private void btnCancelarAperturaCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarAperturaCajaActionPerformed
        txtMontoInicialCaja.setText("");
        jpAperturaCaja.setVisible(false);
    }//GEN-LAST:event_btnCancelarAperturaCajaActionPerformed

    private void btnRegistrarCierreCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarCierreCajaActionPerformed
        if (txtMontoCierreCaja.getText() == null || txtMontoCierreCaja.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el monto físico de cierre.");
            return;
        }

        BigDecimal monto;
        try {
            monto = MoneyUtil.parse(txtMontoCierreCaja.getText());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "El monto físico de cierre no es válido.");
            return;
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            JOptionPane.showMessageDialog(this, "El monto físico de cierre no puede ser negativo.");
            return;
        }

        Date fechaCierre = (fechaCajaEnCierre != null) ? fechaCajaEnCierre : new Date();

        boolean ok;
        if (aperturaIdEnCierre != null) {
            // Cierre robusto basado en la apertura (evita mismatch de fechas)
            ok = operarCaja.registrarCierrePorAperturaId(monto, usuario, aperturaIdEnCierre);
        } else {
            ok = operarCaja.registrarCierre(monto, usuario, fechaCierre);
        }

        if (ok) {
            JOptionPane.showMessageDialog(this, "Cierre de caja diaria registrado!");
            jpCierreCaja.setVisible(false);
            fechaCajaEnCierre = null;
            aperturaIdEnCierre = null;
            actualizarForm();
        } else {
            if (operarCaja.getUltimoError() != null) {
                JOptionPane.showMessageDialog(this, operarCaja.getUltimoError(), "No se puede cerrar caja", JOptionPane.WARNING_MESSAGE);
            } else if (operarCaja.estadoCaja().equals("CERRADA")) {
                JOptionPane.showMessageDialog(this, "La caja ya se encuentra cerrada!");
                jpCierreCaja.setVisible(false);
            }
        }
    }//GEN-LAST:event_btnRegistrarCierreCajaActionPerformed

    private void btnConfirmarVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarVentaActionPerformed
        Integer idClienteSelec = obtenerIdClienteVentaSeleccionado();

        if (idClienteSelec != null) {
            if (operandoTablas.isTableNotEmpty(tableVentas)) {
                jpFormaDePago.setVisible(true);
                btnLimpiarVenta.setEnabled(false);
                btnConfirmarVenta.setEnabled(false);
                String monto = txtTotalVenta.getText();
                txtMontoFormaDePago.setText(monto);
                jcbClienteVenta.setEnabled(false);
                jcbProductoVenta.setEnabled(false);
                txtCantidadVenta.setEnabled(false);
                btnAgregarProductoVenta.setEnabled(false);
                txtSaldoPendiente.setText(monto);
            } else {
                JOptionPane.showMessageDialog(this, "Debe agregar al menos un producto!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un cliente antes de continuar!");
        }

    }//GEN-LAST:event_btnConfirmarVentaActionPerformed

    private void btnLimpiarVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarVentaActionPerformed
        DefaultTableModel modelo = (DefaultTableModel) tableVentas.getModel();
        modelo.setRowCount(0);
        txtCantidadVenta.setText("1");
        txtSubtotalVenta.setText("");
        txtDescuentoVenta.setText("");
        txtIvaVenta.setText("");
        txtTotalVenta.setText("");
        producto = new Producto();
        listaProductosAVender = new ArrayList<>();
        productoAVender = new VentaProducto();
        jcbClienteVenta.setSelectedIndex(0);
        jcbProductoVenta.setSelectedIndex(0);
        tableVentas.setEnabled(false);
    }//GEN-LAST:event_btnLimpiarVentaActionPerformed

    private void btnAgregarMetodoPagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarMetodoPagoActionPerformed
        MetodoPagoItem mpSeleccionado = (MetodoPagoItem) jcbMetodoDePago.getSelectedItem();
        CuentaCorrienteControlador cuentaC = new CuentaCorrienteControlador();
        Integer idCliente = obtenerIdClienteVentaSeleccionado();

        if (mpSeleccionado == null || mpSeleccionado.getIdMetodoPago() == null || mpSeleccionado.getIdMetodoPago() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un Método de Pago.");
            return;
        }

        if (idCliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un cliente válido antes de agregar el pago.");
            return;
        }

        if (MetodoPagoTipo.CUENTA_CORRIENTE == MetodoPagoTipo.fromEtiqueta(mpSeleccionado.toString())) {
            if (!cuentaC.cuentaCorrienteActiva(idCliente)) {
                JOptionPane.showMessageDialog(this, "La Cuenta Corriente del cliente está INACTIVA o no existe.");
                return;
            }

            BigDecimal margen = cuentaC.obtenerMargenDisponible(idCliente);
            BigDecimal montoSolicitado;
            try {
                montoSolicitado = MoneyUtil.parse(txtMontoFormaDePago.getText());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Monto inválido para Cuenta Corriente.");
                return;
            }

            if (montoSolicitado.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "El monto debe ser mayor a 0.");
                return;
            }
            if (montoSolicitado.compareTo(margen) > 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Margen insuficiente para Cuenta Corriente.\n"
                        + "Margen disponible: " + MoneyUtil.formatStandard(margen) + "\n"
                        + "Monto solicitado: " + MoneyUtil.formatStandard(montoSolicitado),
                        "Cuenta Corriente",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            cargarMetodoPagoTabla(mpSeleccionado.toString());
        }
        if (MetodoPagoTipo.EFECTIVO == MetodoPagoTipo.fromEtiqueta(mpSeleccionado.toString())) {
            cargarMetodoPagoTabla(mpSeleccionado.toString());
        }

        calcularSaldoFormaDePago();
    }//GEN-LAST:event_btnAgregarMetodoPagoActionPerformed

    private void btnRegistrarVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarVentaActionPerformed
        Integer idCliente = obtenerIdClienteVentaSeleccionado();
        if (idCliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un cliente válido para registrar la venta.");
            return;
        }
        DefaultTableModel modeloMetodosPago = (DefaultTableModel) tableFormaDePago.getModel();
        // Reset montos acumulados para soportar pagos mixtos
        movimientoCuentaC.setMonto(BigDecimal.ZERO);
        movimientoEfectivo.setMonto(BigDecimal.ZERO);

        for (int i = 0; i < modeloMetodosPago.getRowCount(); i++) {
            String metodoPago = (String) modeloMetodosPago.getValueAt(i, 0);
            Object montomasObj = modeloMetodosPago.getValueAt(i, 1);

            BigDecimal monto;

            if (montomasObj instanceof Double) {
                monto = MoneyUtil.of((Double) montomasObj);
            } else if (montomasObj instanceof String) {
                // Puede venir como String ya formateado; parse tolerante + normalización a BigDecimal.
                monto = MoneyUtil.parse((String) montomasObj);
            } else {
                throw new IllegalArgumentException("Tipo de dato inesperado para monto: " + montomasObj.getClass().getName());
            }

            // Comparar método de pago con constante
            if (MetodoPagoTipo.CUENTA_CORRIENTE == MetodoPagoTipo.fromEtiqueta(metodoPago)) {
                movimientoCuentaC.setMonto(monto);
            }
            if (MetodoPagoTipo.EFECTIVO == MetodoPagoTipo.fromEtiqueta(metodoPago)) {
                movimientoEfectivo.setMonto(monto);
            }
        }

        // Validación en tiempo real (antes de grabar) para Cuenta Corriente
        if (movimientoCuentaC.getMonto() != null && movimientoCuentaC.getMonto().compareTo(BigDecimal.ZERO) > 0) {
            CuentaCorrienteControlador cuentaC = new CuentaCorrienteControlador();
            if (!cuentaC.cuentaCorrienteActiva(idCliente)) {
                JOptionPane.showMessageDialog(this,
                        "La Cuenta Corriente del cliente está INACTIVA o no existe.\n"
                        + "No se puede registrar la venta con este método.",
                        "Cuenta Corriente",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            BigDecimal margen = cuentaC.obtenerMargenDisponible(idCliente);
            if (movimientoCuentaC.getMonto().compareTo(margen) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Margen insuficiente para Cuenta Corriente.\n"
                        + "Margen disponible: " + MoneyUtil.formatStandard(margen) + "\n"
                        + "Monto a debitar: " + MoneyUtil.formatStandard(movimientoCuentaC.getMonto()),
                        "Cuenta Corriente",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this, "¿Desea finalizar el recibo y registrar la venta?",
                "Confirmar recibo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (respuesta == JOptionPane.YES_OPTION) {
            // Persistir recibo + items + métodos de pago + movimientos (CAJA / CUENTA CORRIENTE)
            // de forma ATÓMICA en una sola transacción.
            if (!persistirRecibo()) {
                // persistirRecibo ya muestra el error
                return;
            }
            respuesta = JOptionPane.showConfirmDialog(
                    this, "¿Desea imprimir el comprobante?",
                    "Visualización e Impresión",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    Long idReciboLong = (ultimoIdReciboRegistrado != null) ? ultimoIdReciboRegistrado : recibo.getIdRecibo();
                    if (idReciboLong == null) {
                        throw new IllegalStateException("No se obtuvo el ID del recibo registrado.");
                    }
                    Integer idRecibo = Math.toIntExact(idReciboLong);
                    AuditoriaLogger.evento("RECIBO_IMPRESION", "idRecibo=" + idReciboLong, usuario);
                    generarReporte(idRecibo);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "No se pudo imprimir el recibo: " + ex.getMessage(), "Impresión", JOptionPane.ERROR_MESSAGE);
                }
            }

            // Stock se descuenta de forma atómica dentro de CajaRegistradoraTxService (etapa 1)
            reiniciarFormulario();
        }
    }//GEN-LAST:event_btnRegistrarVentaActionPerformed

    private void btnLimpiarPagosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarPagosActionPerformed
        DefaultTableModel modelo = (DefaultTableModel) tableFormaDePago.getModel();
        modelo.setRowCount(0);
        txtSaldoPendiente.setText(txtTotalVenta.getText());
        txtMontoFormaDePago.setText(txtTotalVenta.getText());
    }//GEN-LAST:event_btnLimpiarPagosActionPerformed

    private void txtSaldoPendienteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtSaldoPendienteMouseClicked
        txtMontoFormaDePago.setText(txtSaldoPendiente.getText());
    }//GEN-LAST:event_txtSaldoPendienteMouseClicked

    private void btnCancelarCierreCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarCierreCajaActionPerformed
        jpCierreCaja.setVisible(false);
        fechaCajaEnCierre = null;
        aperturaIdEnCierre = null;
    }//GEN-LAST:event_btnCancelarCierreCajaActionPerformed

    private void btnCancelarRegistroVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarRegistroVentaActionPerformed
        reiniciarFormulario();
    }//GEN-LAST:event_btnCancelarRegistroVentaActionPerformed

    private void btnCancelarRegistroVenta1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarRegistroVenta1ActionPerformed
        jpRegistrarCobros.setVisible(false);
        reiniciarFormulario();
    }//GEN-LAST:event_btnCancelarRegistroVenta1ActionPerformed

    private void btnRegistrarCobroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarCobroActionPerformed
        // Regla de negocio: Cobros CC solo con caja ABIERTA
        if (!"ABIERTA".equals(operarCaja.estadoCaja())) {
            JOptionPane.showMessageDialog(this, "Para registrar un cobro de Cuenta Corriente la caja debe estar ABIERTA.");
            AuditoriaLogger.evento("COBRO_CC_BLOQUEADO", "caja=" + operarCaja.estadoCaja(), usuario);
            return;
        }

        movimientoCuentaC = new CuentaCorrienteMovimiento();
        BigDecimal monto;
        try {
            monto = MoneyUtil.parse(txtMontoPagoCC.getText());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "El monto ingresado no es válido.");
            return;
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un monto mayor a 0!");
            return;
        }

        CuentaCorrienteControlador operandoCC = new CuentaCorrienteControlador();
        CuentaCorrienteMovimientoDAO operandoMovimientoCC = new CuentaCorrienteMovimientoDAO();
        ClienteItem clienteSeleccionado = (ClienteItem) jcbClienteCobro.getSelectedItem();
        if (clienteSeleccionado == null || clienteSeleccionado.getIdCliente() == null || clienteSeleccionado.getIdCliente() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un cliente válido.");
            return;
        }

        CuentaCorriente cuenta = operandoCC.buscarCuentaCorrientePorIdCliente(clienteSeleccionado.getIdCliente());
        if (cuenta == null) {
            JOptionPane.showMessageDialog(this, "El cliente seleccionado no posee una Cuenta Corriente activa.");
            return;
        }

        String estadoCuenta = cuenta.getEstado();
        if (estadoCuenta == null || !"Activa".equalsIgnoreCase(estadoCuenta)) {
            JOptionPane.showMessageDialog(this, "La Cuenta Corriente del cliente está INACTIVA.");
            return;
        }

        movimientoCuentaC.setCuentaCorriente(cuenta);
        movimientoCuentaC.setDescripcion("Pago Cuenta Corriente");
        Date fecha = new Date();
        LocalDate fechaMovimiento = fecha.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        movimientoCuentaC.setFechaMovimiento(fechaMovimiento);
        movimientoCuentaC.setMonto(monto);
        movimientoCuentaC.setTipoMovimiento(CuentaCorrienteMovimiento.TipoMovimiento.CREDITO);

        if (!operandoMovimientoCC.crear(movimientoCuentaC)) {
            JOptionPane.showMessageDialog(this, "No se pudo registrar el pago de Cuenta Corriente.");
            return;
        }

        try {
            AuditoriaLogger.evento(
                    "COBRO_CC_REGISTRADO",
                    "idMov=" + movimientoCuentaC.getIdMovimiento() + " clienteId=" + clienteSeleccionado.getIdCliente() + " monto=" + monto,
                    usuario
            );
        } catch (Exception ignore) {
        }

        JOptionPane.showMessageDialog(this, "Pago a Cuenta Corriente registrado con éxito!");
        txtMontoPagoCC.setText("");
        onClienteCCSeleccionado();

        int resp = JOptionPane.showConfirmDialog(
                this,
                "¿Desea imprimir el comprobante de cobro?",
                "Comprobante de Cobro",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (resp == JOptionPane.YES_OPTION) {
            try {
                Integer idMov = movimientoCuentaC.getIdMovimiento();
                if (idMov == null) {
                    throw new IOException("No se obtuvo el ID del movimiento de cobro.");
                }
                java.io.File pdf = comprobantePdf.generarComprobanteCobroCuentaCorrientePdf(idMov);
                AuditoriaLogger.evento("COBRO_CC_IMPRESION", "idMovimiento=" + idMov + " file=" + pdf.getAbsolutePath(), usuario);
                comprobantePdf.abrirArchivo(pdf);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo generar/abrir el comprobante PDF. " + ex.getMessage(),
                        "Comprobante de Cobro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnRegistrarCobroActionPerformed

    private void btnRegistrarPagoCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarPagoCompraActionPerformed

        MetodoPagoItem mpSeleccionado = (MetodoPagoItem) jcbMetodoDePagoCompra.getSelectedItem();
        if (mpSeleccionado == null || mpSeleccionado.getIdMetodoPago() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un método de pago.");
            return;
        }
        BigDecimal monto;
        try {
            monto = MoneyUtil.parse(txtMontoFacturaCompra.getText());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Monto inválido.");
            return;
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "El monto debe ser mayor a 0.");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) tablePagosCompra.getModel();
        boolean existe = false;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object metodoEnTabla = modelo.getValueAt(i, 0);
            MetodoPagoItem mpEnTabla = null;
            if (metodoEnTabla instanceof MetodoPagoItem) {
                mpEnTabla = (MetodoPagoItem) metodoEnTabla;
            }
            if (mpEnTabla != null && mpEnTabla.getIdMetodoPago() == mpSeleccionado.getIdMetodoPago()) {
                Object val = modelo.getValueAt(i, 1);
                BigDecimal actual = BigDecimal.ZERO;
                if (val instanceof Number) {
                    actual = BigDecimal.valueOf(((Number) val).doubleValue());
                } else if (val != null) {
                    try {
                        actual = MoneyUtil.parse(val.toString());
                    } catch (Exception ignore) {
                    }
                }
                modelo.setValueAt(actual.add(monto).doubleValue(), i, 1);
                existe = true;
                break;
            }
        }
        if (!existe) {
            // Guardamos el objeto MetodoPagoItem para conservar el ID.
            // En UI se renderiza via toString() (nombre del método).
            modelo.addRow(new Object[]{mpSeleccionado, monto.doubleValue()});
        }

        // reset método de pago para permitir cargar múltiples pagos
        jcbMetodoDePagoCompra.setSelectedIndex(0);
        txtMontoFacturaCompra.setText("");

    }//GEN-LAST:event_btnRegistrarPagoCompraActionPerformed

    private void btnCancelarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarCompraActionPerformed

        limpiarCompra();
        jpRegistrarCompras.setVisible(false);
        this.revalidate();
        this.repaint();

    }//GEN-LAST:event_btnCancelarCompraActionPerformed

    private void btnRegistrarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarCompraActionPerformed
        // Validaciones básicas
        ProveedorItem proveedorSel = (ProveedorItem) jcbProveedorCompra.getSelectedItem();
        if (proveedorSel == null || proveedorSel.getIdProveedor() == 0) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un proveedor.");
            return;
        }
        String numeroFactura = (txtNumeroFactura.getText() == null) ? "" : txtNumeroFactura.getText().trim();
        if (numeroFactura.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el número de factura.");
            return;
        }
        BigDecimal totalFactura;
        try {
            totalFactura = MoneyUtil.parse(txtTotalFacturaCompra.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Total de factura inválido.");
            return;
        }
        if (totalFactura == null || totalFactura.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "El total de la factura debe ser mayor a 0.");
            return;
        }
        Date fechaFactura = jdcFechaFacturaDeCompra.getDate();
        if (fechaFactura == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar la fecha de la factura.");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) tablePagosCompra.getModel();
        if (modelo.getRowCount() <= 0) {
            JOptionPane.showMessageDialog(this, "Debe cargar al menos un pago (o Cuenta Corriente) antes de registrar la compra.");
            return;
        }

        // Validación de pagos cargados: permite múltiples medios (ej. EFECTIVO + CUENTA CORRIENTE)
        boolean hayCuentaC = false;
        boolean hayEfectivo = false;
        BigDecimal sumaPagos = BigDecimal.ZERO;
        BigDecimal montoCuentaCorriente = BigDecimal.ZERO;

        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object mpObj = modelo.getValueAt(i, 0);
            if (!(mpObj instanceof MetodoPagoItem)) {
                JOptionPane.showMessageDialog(this, "Error interno: método de pago inválido en la tabla.");
                return;
            }
            MetodoPagoItem mp = (MetodoPagoItem) mpObj;
            Object val = modelo.getValueAt(i, 1);
            BigDecimal monto = BigDecimal.ZERO;
            if (val instanceof Number) {
                monto = BigDecimal.valueOf(((Number) val).doubleValue());
            } else if (val != null) {
                try {
                    monto = MoneyUtil.parse(val.toString());
                } catch (Exception ignore) {
                }
            }
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "El monto del pago debe ser mayor a 0.");
                return;
            }
            sumaPagos = sumaPagos.add(monto);
            // Sólo soportamos EFECTIVO y CUENTAC en este flujo
            if (MetodoPagoTipo.CUENTA_CORRIENTE == MetodoPagoTipo.fromEtiqueta(mp.getNombre())) {
                hayCuentaC = true;
                montoCuentaCorriente = montoCuentaCorriente.add(monto);
            } else if (MetodoPagoTipo.EFECTIVO == MetodoPagoTipo.fromEtiqueta(mp.getNombre())) {
                hayEfectivo = true;
            } else {
                JOptionPane.showMessageDialog(this, "Método de pago no soportado para compras: " + mp.getNombre()
                        + ". Solo se permite 'Efectivo' y 'Cuenta Corriente'.");
                return;
            }
        }

        // Regla: la suma de pagos (incluida la parte a Cuenta Corriente) debe cubrir el total
        if (sumaPagos.compareTo(totalFactura) != 0) {
            JOptionPane.showMessageDialog(this, "La suma de pagos debe ser igual al total de la factura.");
            return;
        }

        // Control de duplicidad de factura por proveedor
        try {
            CompraProveedorDAO compraDAOCheck = new CompraProveedorDAO();
            if (compraDAOCheck.existeFacturaProveedor(proveedorSel.getIdProveedor(), numeroFactura)) {
                JOptionPane.showMessageDialog(this,
                        "No se puede registrar la compra.\n\n"
                        + "Ya existe una factura con el número '" + numeroFactura + "' cargada para el proveedor seleccionado.",
                        "Compras",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo validar el número de factura. " + ex.getMessage(),
                    "Compras",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirmación final antes de registrar
        try {
            Object[] opciones = {"Sí", "No"};
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String fechaStr = (fechaFactura != null) ? sdf.format(fechaFactura) : ""; 
            String totalStr = MoneyUtil.formatStandard(totalFactura);
            int confirm = JOptionPane.showOptionDialog(
                    this,
                    "¿Confirmar registro de compra?\n\n"
                    + "Proveedor: " + String.valueOf(proveedorSel) + "\n"
                    + "Factura: " + numeroFactura + "\n"
                    + "Fecha: " + fechaStr + "\n"
                    + "Total: " + totalStr,
                    "Confirmar compra",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        } catch (Exception ignore) {
            // Si falla el formateo, no bloqueamos: el registro seguirá con datos ya validados.
        }

        try {
            // Cargar entidades base
            Proveedor proveedor = operandoProveedor.buscarProveedorPorId(proveedorSel.getIdProveedor());
            if (proveedor == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el proveedor seleccionado.");
                return;
            }

            CompraProveedor compra = new CompraProveedor();
            compra.setEstado(CompraProveedorEstado.REGISTRADA);
            compra.setFecha(fechaFactura);
            compra.setNumeroFactura(numeroFactura);
            compra.setTotalFactura(totalFactura);
            // Saldo pendiente = monto que queda a CUENTA CORRIENTE (si se pagó mixto, sólo esa parte)
            compra.setSaldoPendiente(montoCuentaCorriente);
            compra.setProveedor(proveedor);
            compra.setUsuario(usuario);

            List<CompraProveedorPago> pagos = new ArrayList<>();

            // Crear pagos según tabla
            for (int i = 0; i < modelo.getRowCount(); i++) {
                MetodoPagoItem mpItem = (MetodoPagoItem) modelo.getValueAt(i, 0);
                Object val = modelo.getValueAt(i, 1);

                BigDecimal monto = BigDecimal.ZERO;
                if (val instanceof Number) {
                    monto = BigDecimal.valueOf(((Number) val).doubleValue());
                } else if (val != null) {
                    monto = MoneyUtil.parse(val.toString());
                }

                MetodoPago metodoPago = operandoMetodoPago.buscarPorId(mpItem.getIdMetodoPago());
                if (metodoPago == null) {
                    JOptionPane.showMessageDialog(this, "No se encontró el método de pago seleccionado: " + mpItem.getNombre());
                    return;
                }

                CompraProveedorPago pago = new CompraProveedorPago();
                pago.setMetodoPago(metodoPago);
                pago.setMonto(monto);
                pagos.add(pago);
            }

            compra.setPagos(pagos);

            // Persistencia atómica + movimientos (caja / cuenta corriente) según medios
            CompraProveedorDAO compraDAO = new CompraProveedorDAO();
            CuentaCorrienteProveedorDAO ccpDAO = new CuentaCorrienteProveedorDAO();
            CuentaCorrienteProveedorMovimientoDAO ccpMovDAO = new CuentaCorrienteProveedorMovimientoDAO();

            compraDAO.crearCompraConPagos(compra, pagos, usuario, ccpDAO, ccpMovDAO,
                    METODO_DE_PAGO_EFECTIVO, METODO_DE_PAGO_CUENTAC);

            if (hayCuentaC && hayEfectivo) {
                JOptionPane.showMessageDialog(this, "Compra registrada con pagos mixtos (Efectivo + Cuenta Corriente). ");
                AuditoriaLogger.evento("COMPRA_PROVEEDOR_MIXTA",
                        "proveedorId=" + proveedor.getIdProveedor() + " factura=" + numeroFactura + " total=" + totalFactura + " cc=" + montoCuentaCorriente,
                        usuario);
            } else if (hayCuentaC) {
                JOptionPane.showMessageDialog(this, "Compra registrada a Cuenta Corriente del proveedor.");
                AuditoriaLogger.evento("COMPRA_PROVEEDOR_CC",
                        "proveedorId=" + proveedor.getIdProveedor() + " factura=" + numeroFactura + " total=" + totalFactura,
                        usuario);
            } else {
                JOptionPane.showMessageDialog(this, "Compra registrada como salida de caja (Efectivo). ");
                AuditoriaLogger.evento("COMPRA_PROVEEDOR_EFECTIVO",
                        "proveedorId=" + proveedor.getIdProveedor() + " factura=" + numeroFactura + " total=" + totalFactura,
                        usuario);
            }

            limpiarCompra();
            jpRegistrarCompras.setVisible(false);
            this.revalidate();
            this.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo registrar la compra. " + ex.getMessage(), "Compras", JOptionPane.ERROR_MESSAGE);
            AuditoriaLogger.evento("COMPRA_PROVEEDOR_ERROR", "error=" + ex.getMessage(), usuario);
        }
    }//GEN-LAST:event_btnRegistrarCompraActionPerformed

    private void btnComprasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnComprasActionPerformed

        String estadoCaja = operarCaja.estadoCaja();
        jpRegistrarCobros.setVisible(false);
        jpRegistrarVentas.setVisible(false);
        jpAperturaCaja.setVisible(false);
        jpCierreCaja.setVisible(false);

        if (!"ABIERTA".equals(estadoCaja)) {
            JOptionPane.showMessageDialog(this, "No es posible acceder a Compras. La caja aún está cerrada!");
            return;
        }

        limpiarCompra();
        jpRegistrarCompras.setVisible(true);
        this.revalidate();
        this.repaint();

    }//GEN-LAST:event_btnComprasActionPerformed

    private void btnLimpiarPagosComprasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarPagosComprasActionPerformed
        DefaultTableModel modelo = (DefaultTableModel) tablePagosCompra.getModel();
        modelo.setRowCount(0);
        // Vuelve a sugerir el monto total como monto de pago
        try {
            BigDecimal total = MoneyUtil.parse(txtTotalFacturaCompra.getText());
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                txtMontoFacturaCompra.setText(MoneyUtil.formatStandard(total));
            } else {
                txtMontoFacturaCompra.setText("");
            }
        } catch (Exception ignore) {
            txtMontoFacturaCompra.setText("");
        }
    }//GEN-LAST:event_btnLimpiarPagosComprasActionPerformed

    private void limpiarCompra() {
        if (jcbProveedorCompra.getItemCount() > 0) {
            jcbProveedorCompra.setSelectedIndex(0);
        }
        txtNumeroFactura.setText("");
        txtTotalFacturaCompra.setText("");
        jdcFechaFacturaDeCompra.setDate(new Date());
        if (jcbMetodoDePagoCompra.getItemCount() > 0) {
            jcbMetodoDePagoCompra.setSelectedIndex(0);
        }
        txtMontoFacturaCompra.setText("");
        DefaultTableModel modelo = (DefaultTableModel) tablePagosCompra.getModel();
        modelo.setRowCount(0);
    }

    private Integer obtenerIdProductoSeleccionado() {
        ProductoItem item = (ProductoItem) jcbProductoVenta.getSelectedItem();
        if (item == null || item.getIdProducto() == null || item.getIdProducto() == 0) {
            return null;
        }
        return item.getIdProducto();
    }

    private Integer obtenerIdClienteVentaSeleccionado() {
        ClienteItem item = (ClienteItem) jcbClienteVenta.getSelectedItem();
        if (item == null || item.getIdCliente() == null || item.getIdCliente() == 0) {
            return null;
        }
        return item.getIdCliente();
    }

    private Integer obtenerCantidadVentaValidada() {
        String raw = txtCantidadVenta.getText();
        if (raw == null) {
            JOptionPane.showMessageDialog(this, "Debe ingresar una cantidad válida.");
            return null;
        }

        raw = raw.trim();
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar una cantidad.");
            return null;
        }
        if (!raw.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número entero positivo.");
            return null;
        }

        try {
            int cantidad = Integer.parseInt(raw);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0.");
                return null;
            }
            if (cantidad > 1000000) {
                JOptionPane.showMessageDialog(this, "La cantidad ingresada es demasiado grande.");
                return null;
            }
            return cantidad;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad ingresada no es válida.");
            return null;
        }
    }

    private void cargarMetodoPagoTabla(String metodoPagoCliente) {
        Double monto;
        try {
            monto = MoneyUtil.parse(txtMontoFormaDePago.getText()).doubleValue();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Monto inválido.");
            return;
        }
        String descripcion = "Metodo Pago " + metodoPagoCliente + " rc." + recibo.getIdRecibo();
        DefaultTableModel modelo = (DefaultTableModel) tableFormaDePago.getModel();

        boolean metodoExistente = false;

        // Recorrer la tabla para buscar si el método de pago ya existe
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String metodoEnTabla = (String) modelo.getValueAt(i, 0);
            if (metodoEnTabla.equals(metodoPagoCliente)) {
                // Si el método de pago ya existe, actualiza el monto sumando el nuevo valor
                Double montoActual = (Double) modelo.getValueAt(i, 1);
                modelo.setValueAt(montoActual + monto, i, 1);
                metodoExistente = true;
                break;
            }
        }

        // Si no se encontró el método de pago en la tabla, agregar una nueva fila
        if (!metodoExistente) {
            Object[] fila = new Object[]{
                metodoPagoCliente,
                monto,
                descripcion
            };
            modelo.addRow(fila);
        }
    }

    private boolean persistirRecibo() {
        recibo.setFecha(new Date());
        recibo.setTipo("Venta");
        Integer idCliente = obtenerIdClienteVentaSeleccionado();
        if (idCliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un cliente válido.", "Venta", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        Cliente cliente = operandoCliente.buscarPorId(idCliente);
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el cliente seleccionado.", "Venta", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        recibo.setCliente(cliente);
        recibo.setUsuario(usuario);
        try {
            recibo.setSubtotalRecibo(MoneyUtil.parse(txtSubtotalVenta.getText()));
            recibo.setTotalDescuento(MoneyUtil.parse(txtDescuentoVenta.getText()));
            recibo.setTotalIva(MoneyUtil.parse(txtIvaVenta.getText()));
            recibo.setTotalRecibo(MoneyUtil.parse(txtTotalVenta.getText()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Los totales de la venta no son válidos.", "Venta", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (listaProductosAVender == null || listaProductosAVender.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe agregar al menos un producto a la venta.", "Venta", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        List<ReciboProductos> listaProductos = new ArrayList<>();
        for (VentaProducto vp : listaProductosAVender) {
            ReciboProductos rp = new ReciboProductos();

            Producto productoTemp = operandoProducto.buscarProductoPorId(vp.getIdProducto());
            if (productoTemp == null) {
                JOptionPane.showMessageDialog(this, "No se encontró uno de los productos seleccionados para la venta.", "Venta", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            rp.setProducto(productoTemp);

            rp.setCantidad(vp.getCantidadProducto());

            // ✅ si VentaProducto ya maneja BigDecimal (recomendado con el Punto 2)
            rp.setPrecioUnitario(vp.getPrecioBase());
            rp.setIvaUnitario(vp.getMontoIVAUnitario());
            rp.setDescuentoUnitario(vp.getMontoDescuentoUnitario());
            rp.setGananciaUnitario(vp.getMontoGananciaUnitario());
            rp.setTotalUnitario(vp.getMontoTotalUnitario());

            BigDecimal cantidad = BigDecimal.valueOf(vp.getCantidadProducto());
            BigDecimal totalCantidad = cantidad.multiply(vp.getMontoTotalUnitario());
            rp.setTotalCantidad(totalCantidad);

            rp.setRecibo(recibo);
            listaProductos.add(rp);
        }
        recibo.setProductos(listaProductos);

        List<ReciboMetodoPago> listaMetodosPago = new ArrayList<>();
        DefaultTableModel modeloMetodosPago = (DefaultTableModel) tableFormaDePago.getModel();
        // Reset montos acumulados para soportar pagos mixtos
        movimientoCuentaC.setMonto(BigDecimal.ZERO);
        movimientoEfectivo.setMonto(BigDecimal.ZERO);

        for (int i = 0; i < modeloMetodosPago.getRowCount(); i++) {
            ReciboMetodoPago reciboMetodoPago = new ReciboMetodoPago();

            String nombreMetodoPago = (String) modeloMetodosPago.getValueAt(i, 0);
            MetodoPago metodoPago = operandoMetodoPago.obtenerMetodoDePagoPorNombre(nombreMetodoPago); // ID del método de pago
            if (metodoPago != null) {
                reciboMetodoPago.setMetodoPago(metodoPago);
                reciboMetodoPago.setMonto(MoneyUtil.parse(modeloMetodosPago.getValueAt(i, 1).toString()));

                reciboMetodoPago.setRecibo(recibo); // Asignar el recibo
                listaMetodosPago.add(reciboMetodoPago);
            }
        }
        if (listaMetodosPago.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe agregar al menos un método de pago.", "Venta", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        recibo.setMetodosPago(listaMetodosPago);

        // Persistir el recibo con sus relaciones + generar movimientos de forma atómica
        veterinaria.servicio.CajaRegistradoraTxService svc = new veterinaria.servicio.CajaRegistradoraTxService();
        veterinaria.servicio.CajaRegistradoraTxService.ResultadoVenta res
                = svc.registrarVenta(recibo, listaProductos, listaMetodosPago, usuario);

        if (!res.isOk()) {
            JOptionPane.showMessageDialog(this,
                    "Error al registrar la venta: " + res.getError(),
                    "Venta",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        ultimoIdReciboRegistrado = res.getIdRecibo();
        if (ultimoIdReciboRegistrado != null) {
            recibo.setIdRecibo(ultimoIdReciboRegistrado);
        }

        // Auditoría mínima
        try {
            Long id = res.getIdRecibo();
            AuditoriaLogger.evento(
                    "VENTA_REGISTRADA",
                    "idRecibo=" + id + " total=" + recibo.getTotalRecibo() + " clienteId=" + (recibo.getCliente() != null ? recibo.getCliente().getIdCliente() : null),
                    usuario
            );
        } catch (Exception ignore) {
        }

        // Asegurar que el ID quede disponible para impresión
        // (en general ya queda seteado por JPA, pero usamos el retorno por robustez)
        try {
            if (res.getIdRecibo() != null) {
                // No hay setter público en la entidad, así que solo aseguramos usar res.getIdRecibo() donde haga falta.
            }
        } catch (Exception ignore) {
        }

        JOptionPane.showMessageDialog(this, "Recibo registrado con éxito.");
        return true;
    }

    private void calcularSaldoFormaDePago() {
        DefaultTableModel modeloMetodosPago = (DefaultTableModel) tableFormaDePago.getModel();
        BigDecimal acumulaMontoPago = BigDecimal.ZERO;
        BigDecimal totalVenta;
        try {
            totalVenta = MoneyUtil.parse(txtTotalVenta.getText());
        } catch (Exception ex) {
            totalVenta = BigDecimal.ZERO;
        }

        for (int i = 0; i < modeloMetodosPago.getRowCount(); i++) {
            Object montoObj = modeloMetodosPago.getValueAt(i, 1);
            try {
                BigDecimal monto = (montoObj instanceof Number)
                        ? BigDecimal.valueOf(((Number) montoObj).doubleValue())
                        : MoneyUtil.parse(String.valueOf(montoObj));
                acumulaMontoPago = acumulaMontoPago.add(monto);
            } catch (Exception ignore) {
            }
        }
        BigDecimal saldo = totalVenta.subtract(acumulaMontoPago);
        txtSaldoPendiente.setText(MoneyUtil.formatStandard(saldo));
        txtMontoFormaDePago.setText(MoneyUtil.formatStandard(saldo));
    }

    private Boolean validarStockProducto(Integer stockProducto, Integer cantidad, Integer idProducto) {
        // Si se permite stock negativo, no se bloquea la venta
        boolean permitirNegativo = configService.getBoolean(ConfiguracionService.KEY_VENTAS_PERMITIR_STOCK_NEGATIVO,
                ConfiguracionService.DEFAULT_VENTAS_PERMITIR_STOCK_NEGATIVO != 0);
        if (permitirNegativo) {
            return true;
        }
        if (cantidad == null || cantidad <= 0) {
            return false;
        }
        if (stockProducto == null) {
            return false;
        }
        int acumulaProductos = 0;
        for (VentaProducto productoBucle : listaProductosAVender) {
            if (Objects.equals(productoBucle.getIdProducto(), idProducto) && productoBucle.getCantidadProducto() != null) {
                acumulaProductos += productoBucle.getCantidadProducto();
            }
        }
        int limite = acumulaProductos + cantidad;
        return limite <= stockProducto;
    }


private void seleccionarClienteDefault() {
    try {
        int idDefault = configService.getInt(ConfiguracionService.KEY_VENTAS_CLIENTE_DEFAULT_ID,
                ConfiguracionService.DEFAULT_VENTAS_CLIENTE_DEFAULT_ID);
        if (idDefault <= 0) return;

        for (int i = 0; i < jcbClienteVenta.getItemCount(); i++) {
            ClienteItem it = (ClienteItem) jcbClienteVenta.getItemAt(i);
            if (it != null && it.getIdCliente() == idDefault) {
                jcbClienteVenta.setSelectedIndex(i);
                break;
            }
        }
    } catch (Exception ignored) {}
}

private void seleccionarMetodoPagoDefault() {
    try {
        int idDefault = configService.getInt(ConfiguracionService.KEY_VENTAS_METODO_PAGO_DEFAULT_ID,
                ConfiguracionService.DEFAULT_VENTAS_METODO_PAGO_DEFAULT_ID);
        if (idDefault <= 0) return;

        for (int i = 0; i < jcbMetodoDePago.getItemCount(); i++) {
            MetodoPagoItem it = (MetodoPagoItem) jcbMetodoDePago.getItemAt(i);
            if (it != null && it.getIdMetodoPago() == idDefault) {
                jcbMetodoDePago.setSelectedIndex(i);
                break;
            }
        }
    } catch (Exception ignored) {}
}

private BigDecimal obtenerPorcentajeGananciaProducto(Producto producto) {
    // 1) rubro específico si existe
    try {
        if (producto != null && producto.getRubro() != null && !producto.getRubro().trim().isEmpty()) {
            Rubro r = operandoRubro.buscarPorNombre(producto.getRubro().trim());
            if (r != null && r.getPorcentajeGanancia() != null) {
                return r.getPorcentajeGanancia();
            }
        }
    } catch (Exception ignored) {}

    // 2) fallback global
    int global = configService.getInt(ConfiguracionService.KEY_VENTAS_PORC_GANANCIA_GLOBAL,
            ConfiguracionService.DEFAULT_VENTAS_PORC_GANANCIA_GLOBAL);
    return BigDecimal.valueOf(global);
}

    private void reiniciarFormulario() {
        jpRegistrarVentas.setVisible(false);
        jpFormaDePago.setVisible(false);
        btnConfirmarVenta.setEnabled(true);
        btnLimpiarVenta.setEnabled(true);
        tableVentas.setEnabled(true);
        txtCantidadVenta.setEnabled(true);
        btnAgregarProductoVenta.setEnabled(true);
        jcbProductoVenta.setEnabled(true);
        if (jcbProductoVenta.getItemCount() > 0) {
            jcbProductoVenta.setSelectedIndex(0);
        }
        jcbClienteVenta.setEnabled(true);
        if (jcbClienteVenta.getItemCount() > 0) {
            jcbClienteVenta.setSelectedIndex(0);
        }
        if (jcbClienteCobro.getItemCount() > 0) {
            jcbClienteCobro.setSelectedIndex(0);
        }
        txtCantidadVenta.setText("1");
        txtSubtotalVenta.setText("");
        txtDescuentoVenta.setText("");
        txtIvaVenta.setText("");
        txtTotalVenta.setText("");
        txtSaldoPendiente.setText("");
        txtMontoFormaDePago.setText("");
        txtSaldoCuentaCorriente.setText("");
        txtMontoPagoCC.setText("");
        ((DefaultTableModel) tableVentas.getModel()).setRowCount(0);
        ((DefaultTableModel) tableFormaDePago.getModel()).setRowCount(0);
        ((DefaultTableModel) tableCuentaCorriente.getModel()).setRowCount(0);
        movimientoCuentaC = new CuentaCorrienteMovimiento();
        producto = new Producto();
        listaProductosAVender = new ArrayList<>();
        productoAVender = new VentaProducto();
        recibo = new Recibo();
        ultimoIdReciboRegistrado = null;

    }

    private void cargarDatosComboBoxes() {
        List<Cliente> listaClientes = operandoCliente.buscarTodosLosClientes();
        List<Producto> listaProductos = operandoProducto.buscarProductosDisponiblesParaVenta();
        ClienteItem clienteItem = new ClienteItem(0, "Seleccionar Cliente");
        jcbClienteVenta.addItem(clienteItem);
        jcbClienteCobro.addItem(clienteItem);
        ProductoItem productoItem = new ProductoItem(0, "Seleccionar Producto");
        jcbProductoVenta.addItem(productoItem);

        for (Cliente cliente : listaClientes) {
            String nombreApellido = cliente.getPersona().getNombre() + " " + cliente.getPersona().getApellido();
            clienteItem = new ClienteItem(cliente.getIdCliente(), nombreApellido);
            jcbClienteVenta.addItem(clienteItem);
            jcbClienteCobro.addItem(clienteItem);
        }


        // Selección cliente por defecto (si está configurado)
        seleccionarClienteDefault();

        for (Producto producto : listaProductos) {
            String nombre = producto.getCodigo() + " - " + producto.getNombre();
            productoItem = new ProductoItem(producto.getIdProducto(), nombre);
            jcbProductoVenta.addItem(productoItem);
        }

        // Cargar Proveedores (Compras)
        List<Proveedor> listaProveedores = operandoProveedor.buscarTodosLosProveedores();
        ProveedorItem proveedorItem = new ProveedorItem(0, "Seleccionar Proveedor");
        jcbProveedorCompra.addItem(proveedorItem);
        for (Proveedor proveedor : listaProveedores) {
            String nombre = (proveedor.getPersona() != null)
                    ? (proveedor.getPersona().getNombre() + " " + proveedor.getPersona().getApellido())
                    : proveedor.getRazonSocial();
            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = (proveedor.getRazonSocial() != null) ? proveedor.getRazonSocial() : ("Proveedor " + proveedor.getIdProveedor());
            }
            proveedorItem = new ProveedorItem(proveedor.getIdProveedor(), nombre);
            jcbProveedorCompra.addItem(proveedorItem);
        }

        // Cargar Medios de Pago
        List<MetodoPago> listaMetodoPagos = operandoMetodoPago.obtenerMetodosPagoActivos();
        MetodoPagoItem metodoPagoItem = new MetodoPagoItem(0, "Seleccionar");
        jcbMetodoDePago.addItem(metodoPagoItem);  // Añadir opción por defecto

        for (MetodoPago metodoPago : listaMetodoPagos) {
            String nombre = metodoPago.getNombre();
            metodoPagoItem = new MetodoPagoItem(metodoPago.getIdMetodoPago(), nombre);
            jcbMetodoDePago.addItem(metodoPagoItem);
        }
        seleccionarMetodoPagoDefault();
                jcbClienteCobro.addActionListener(e -> onClienteCCSeleccionado());

        // Compras: permitir EFECTIVO y CUENTA CORRIENTE (también pagos mixtos)
        jcbMetodoDePagoCompra.removeAllItems();
        jcbMetodoDePagoCompra.addItem(new MetodoPagoItem(0, "Seleccionar"));

        MetodoPago metodoEfectivo = listaMetodoPagos.stream()
                .filter(mp -> Constantes.METODO_DE_PAGO_EFECTIVO.equals(mp.getNombre()))
                .findFirst()
                .orElse(null);
        MetodoPago metodoCuentaCorriente = listaMetodoPagos.stream()
                .filter(mp -> Constantes.METODO_DE_PAGO_CUENTAC.equals(mp.getNombre()))
                .findFirst()
                .orElse(null);

        if (metodoEfectivo == null && metodoCuentaCorriente == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se encontraron los métodos de pago permitidos para compras (EFECTIVO / CUENTA CORRIENTE).",
                    "Error de configuración",
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            if (metodoEfectivo != null) {
                jcbMetodoDePagoCompra.addItem(new MetodoPagoItem(metodoEfectivo.getIdMetodoPago(), metodoEfectivo.getNombre()));
            }
            if (metodoCuentaCorriente != null) {
                jcbMetodoDePagoCompra.addItem(new MetodoPagoItem(metodoCuentaCorriente.getIdMetodoPago(), metodoCuentaCorriente.getNombre()));
            }
            jcbMetodoDePagoCompra.setSelectedIndex(0);
            jcbMetodoDePagoCompra.setEnabled(true);
        }

        // Autocompletar monto con el total de la factura
        try {
            BigDecimal total = MoneyUtil.parse(txtTotalFacturaCompra.getText());
            if (total != null) {
                txtMontoFacturaCompra.setText(MoneyUtil.formatStandard(total));
            }
        } catch (Exception ignore) {
        }
    }

    private void actualizarForm() {
        fechaHora();
        lblUsuario.setText(Application.getNombreApellidoUsuarioLogeado());
        lblEstadoCaja.setText(operarCaja.estadoCaja());
    }

    private void fechaHora() {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        lblFechaActual.setText(formatoFecha.format(new Date()));

        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm:ss");
        lblHoraActual.setText(formatoHora.format(new Date()));

        if (relojTimer == null) {
            relojTimer = new Timer(1000, e -> lblHoraActual.setText(formatoHora.format(new Date())));
            relojTimer.start();
        }
    }

    private void calcularMontosProductoUnitario(VentaProducto productoAVender, Integer idProducto, Integer cantidadVenta) {

        producto = operandoProducto.buscarProductoPorId(idProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No se encontró el producto seleccionado.");
        }

        productoAVender.setIdProducto(idProducto);
        productoAVender.setCantidadProducto(cantidadVenta);

        // Precio base
        BigDecimal precio = MoneyUtil.of(producto.getPrecioCosto());

        // Porcentajes (ej: "21" -> 0.21)
        BigDecimal porDescuento = producto.getDescuento() != null && !producto.getDescuento().isEmpty()
                ? new BigDecimal(producto.getDescuento()).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        int decVentas = configService.getInt(ConfiguracionService.KEY_VENTAS_DECIMALES, ConfiguracionService.DEFAULT_VENTAS_DECIMALES);
        RoundingMode rmVentas = configService.getRoundingModeVentas();

        BigDecimal porIva = producto.getIva() != null && !producto.getIva().isEmpty()
                ? new BigDecimal(producto.getIva()).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal porGanancia = obtenerPorcentajeGananciaProducto(producto)
                .divide(BigDecimal.valueOf(100)); // ej: 40 -> 0.40

        // Cálculos unitarios
        BigDecimal descuentoUnitario = precio.multiply(porDescuento);
        BigDecimal ivaUnitario = precio.multiply(porIva);
        BigDecimal gananciaUnitario = precio.multiply(porGanancia);

        BigDecimal totalUnitario = precio
                .add(gananciaUnitario)
                .add(ivaUnitario)
                .subtract(descuentoUnitario);

        // Setters (AHORA COINCIDEN LOS TIPOS)
        productoAVender.setPrecioBase(precio);
        productoAVender.setMontoDescuentoUnitario(descuentoUnitario);
        productoAVender.setMontoIVAUnitario(ivaUnitario);
        productoAVender.setMontoGananciaUnitario(gananciaUnitario);
        productoAVender.setMontoTotalUnitario(totalUnitario);
    }

    private void controlarProductosIgualesEnTabla() {
        boolean productoExistente = false;
        for (VentaProducto productoLista : listaProductosAVender) {
            if (Objects.equals(productoLista.getIdProducto(), productoAVender.getIdProducto())) {
                productoLista.setCantidadProducto(productoLista.getCantidadProducto() + productoAVender.getCantidadProducto());
                productoExistente = true;
                break;
            }
        }

        if (!productoExistente) {
            VentaProducto nuevoProducto = new VentaProducto(
                    productoAVender.getIdProducto(),
                    productoAVender.getCantidadProducto(),
                    productoAVender.getPrecioBase(),
                    productoAVender.getMontoDescuentoUnitario(),
                    productoAVender.getMontoIVAUnitario(),
                    productoAVender.getMontoGananciaUnitario(),
                    productoAVender.getMontoTotalUnitario()
            );
            listaProductosAVender.add(nuevoProducto);
        }
    }

    private void cargarProductosEnTabla(List<VentaProducto> listaProductos) {
        DefaultTableModel modelo = (DefaultTableModel) tableVentas.getModel();
        modelo.setRowCount(0);
        BigDecimal acumulaSubTotal = BigDecimal.ZERO;
        BigDecimal acumulaDescuento = BigDecimal.ZERO;
        BigDecimal acumulaIva = BigDecimal.ZERO;
        BigDecimal acumulaTotalTabla = BigDecimal.ZERO;

        for (VentaProducto productoCarga : listaProductos) {
            Producto producto = operandoProducto.buscarProductoPorId(productoCarga.getIdProducto());
            if (producto == null) {
                AppLog.warning(FormCajaRegistradora.class, "Producto no encontrado al cargar tabla de ventas. id=" + productoCarga.getIdProducto());
                continue;
            }

            BigDecimal cantidad = BigDecimal.valueOf(productoCarga.getCantidadProducto());

            // Montos unitarios desde VentaProducto (ya BigDecimal)
            BigDecimal totalUnitario = productoCarga.getMontoTotalUnitario();
            BigDecimal ivaUnitario = productoCarga.getMontoIVAUnitario();
            BigDecimal descuentoUnitario = productoCarga.getMontoDescuentoUnitario();
            BigDecimal gananciaUnitario = productoCarga.getMontoGananciaUnitario();

            BigDecimal precioBase = BigDecimal.valueOf(producto.getPrecioCosto());

            BigDecimal totalProducto = totalUnitario.multiply(cantidad);
            BigDecimal totalIva = ivaUnitario.multiply(cantidad);
            BigDecimal totalDescuento = descuentoUnitario.multiply(cantidad);

            // Subtotal = (precio base + ganancia) * cantidad
            BigDecimal subTotalProducto = precioBase.add(gananciaUnitario).multiply(cantidad);

            acumulaSubTotal = acumulaSubTotal.add(subTotalProducto);
            acumulaDescuento = acumulaDescuento.add(totalDescuento);
            acumulaIva = acumulaIva.add(totalIva);
            acumulaTotalTabla = acumulaTotalTabla.add(totalProducto);

            Object[] fila = new Object[]{
                producto.getCodigo(),
                producto.getNombre(),
                totalUnitario, // si querés mostrar formateado, ver nota abajo
                totalIva,
                totalDescuento,
                productoCarga.getCantidadProducto(),
                totalProducto
            };

            modelo.addRow(fila);
        }

// Mostrar montos formateados (usa tu MoneyUtil del Punto 2)
        txtSubtotalVenta.setText(MoneyUtil.formatStandard(acumulaSubTotal));
        txtDescuentoVenta.setText(MoneyUtil.formatStandard(acumulaDescuento));
        txtIvaVenta.setText(MoneyUtil.formatStandard(acumulaIva));
        txtTotalVenta.setText(MoneyUtil.formatStandard(acumulaTotalTabla));

        operandoTablas.asegurarSeleccionUnica(tableVentas);
    }

    
private void generarReporte(Integer idRecibo) {
        // Reemplazo Jasper -> PDFBox (ComprobantePdfService)
        try {
            java.io.File pdf = comprobantePdf.generarReciboVentaPdf(idRecibo.longValue());

            boolean imprimirAuto = configService.getBoolean(
                    ConfiguracionService.KEY_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO,
                    ConfiguracionService.DEFAULT_VENTAS_IMPRIMIR_COMPROBANTE_AUTOMATICO != 0
            );
            boolean abrirPdf = configService.getBoolean(
                    ConfiguracionService.KEY_VENTAS_ABRIR_COMPROBANTE_PDF,
                    ConfiguracionService.DEFAULT_VENTAS_ABRIR_COMPROBANTE_PDF != 0
            );

            if (imprimirAuto) {
                try {
                    comprobantePdf.imprimirArchivo(pdf);
                } catch (Exception e) {
                    // No abortar por falla de impresión; dejamos el PDF generado.
                    AppLog.warning(FormCajaRegistradora.class, "No se pudo imprimir automáticamente: " + e.getMessage(), e);
                }
            }
            if (abrirPdf) {
                comprobantePdf.abrirArchivo(pdf);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo generar/abrir el comprobante PDF. " + ex.getMessage(),
                    "Comprobante",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onClienteCCSeleccionado() {
        ClienteItem clienteSeleccionado = (ClienteItem) jcbClienteCobro.getSelectedItem();
        if (clienteSeleccionado == null || clienteSeleccionado.getIdCliente() == null || clienteSeleccionado.getIdCliente() == 0) {
            txtSaldoCuentaCorriente.setText("");
            ((DefaultTableModel) tableCuentaCorriente.getModel()).setRowCount(0);
            return;
        }

        Cliente cliente = operandoCliente.buscarPorId(clienteSeleccionado.getIdCliente());
        if (cliente == null) {
            txtSaldoCuentaCorriente.setText("");
            ((DefaultTableModel) tableCuentaCorriente.getModel()).setRowCount(0);
            JOptionPane.showMessageDialog(this, "No se encontró el cliente seleccionado.");
            return;
        }

        CuentaCorrienteDAO operandoCC = new CuentaCorrienteDAO();
        CuentaCorriente cuentaCorriente = operandoCC.buscarPorIdCliente(cliente.getIdCliente());
        if (cuentaCorriente == null) {
            txtSaldoCuentaCorriente.setText("");
            ((DefaultTableModel) tableCuentaCorriente.getModel()).setRowCount(0);
            JOptionPane.showMessageDialog(this, "El cliente seleccionado no posee Cuenta Corriente.");
            return;
        }

        if (cuentaCorriente.getEstado() != null
                && "INACTIVO".equalsIgnoreCase(cuentaCorriente.getEstado().trim())) {
            txtSaldoCuentaCorriente.setText("");
            ((DefaultTableModel) tableCuentaCorriente.getModel()).setRowCount(0);
            JOptionPane.showMessageDialog(this, "Cuenta Inactiva!");
            return;
        }

        int idCuentaCorriente = cuentaCorriente.getIdCuentaCorriente();
        AppLog.info(FormCajaRegistradora.class, "ID CUENTA CORRIENTE: " + idCuentaCorriente);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        Date fechaDesde = calendar.getTime();
        java.util.Date fechaHasta = new Date();
        java.sql.Date sqlFechaDesde = (fechaDesde != null) ? new java.sql.Date(fechaDesde.getTime()) : null;
        java.sql.Date sqlFechaHasta = (fechaHasta != null) ? new java.sql.Date(fechaHasta.getTime()) : null;
        movimientosCC = operarCCMovimientos.obtenerMovimientosEnRango(idCuentaCorriente, sqlFechaDesde, sqlFechaHasta);
        cargarCCenTabla(movimientosCC);
        CuentaCorrienteMovimientoDAO operandoMovimientosCC = new CuentaCorrienteMovimientoDAO();
        BigDecimal creditos = operandoMovimientosCC.sumarCreditos(idCuentaCorriente);
        BigDecimal debitos = operandoMovimientosCC.sumarDebitos(idCuentaCorriente);
        BigDecimal saldoInterno = creditos.subtract(debitos);
        BigDecimal deudaVisible = saldoInterno.signum() < 0 ? saldoInterno.abs() : BigDecimal.ZERO;
        txtSaldoCuentaCorriente.setText(MoneyUtil.formatStandard(deudaVisible));
    }

    private void cargarCCenTabla(List<CuentaCorrienteMovimiento> movimientos) {
        DefaultTableModel modelo = (DefaultTableModel) tableCuentaCorriente.getModel();
        modelo.setRowCount(0);
        AppLog.info(FormCajaRegistradora.class, "Cargando movimientos en tabla Cobros Cuenta Corriente..");
        AppLog.info(FormCajaRegistradora.class, "Movimientos: " + movimientos.size());
        for (CuentaCorrienteMovimiento movimiento : movimientos) {
            Object[] fila = new Object[]{
                movimiento.getIdMovimiento(),
                movimiento.getDescripcion(),
                movimiento.getTipoMovimiento(),
                movimiento.getFechaMovimiento(),
                movimiento.getMonto(),};
            modelo.addRow(fila);
        }
        tableCuentaCorriente.repaint();
        tableCuentaCorriente.revalidate();
        operarTabla.asegurarSeleccionUnica(tableCuentaCorriente);
    }

    private void cierreDeCajaDiario() {
        // Si existe una caja abierta de días anteriores, cerrar esa.
        CajaMovimiento aperturaPendiente = operarCaja.obtenerAperturaPendienteCierre();
        if (aperturaPendiente != null) {
            cierreDeCajaPorApertura(aperturaPendiente);
            return;
        }
        cierreDeCajaPorFecha(new Date());
    }

    /**
     * Cierre basado directamente en la APERTURA (evita depender de igualdad de
     * fechas).
     */
    private void cierreDeCajaPorApertura(CajaMovimiento apertura) {
        if (apertura == null) {
            JOptionPane.showMessageDialog(this, "No se encontró una apertura válida para cerrar.", "Cierre de caja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        jpRegistrarCobros.setVisible(false);
        jpAperturaCaja.setVisible(false);
        jpRegistrarVentas.setVisible(false);
        jpRegistrarCompras.setVisible(false);
        jpCierreCaja.setVisible(true);

        fechaCajaEnCierre = truncDate(apertura.getFecha());
        aperturaIdEnCierre = apertura.getIdMovimiento();

        if (apertura.getMonto() == null) {
            JOptionPane.showMessageDialog(this,
                    "La apertura encontrada no tiene monto. No se puede cerrar.",
                    "Cierre de caja",
                    JOptionPane.WARNING_MESSAGE);
            jpCierreCaja.setVisible(false);
            fechaCajaEnCierre = null;
            aperturaIdEnCierre = null;
            return;
        }

        BigDecimal montoApertura = apertura.getMonto();
        BigDecimal montoIngresos = operarCaja.obtenerTotalIngresosPorFecha(fechaCajaEnCierre);
        BigDecimal montoEgresos = operarCaja.obtenerTotalEgresosPorFecha(fechaCajaEnCierre);
        BigDecimal montoCierreEsperado = montoIngresos.add(montoApertura).subtract(montoEgresos);

        txtEgresosCierreCaja.setText(montoEgresos.toString());
        txtIngresoCierreCaja.setText(montoIngresos.toString());
        txtMontoCierreCaja.setText(montoCierreEsperado.toString());
    }

    private String formatFecha(Date fecha) {
        if (fecha == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }

    private void cierreDeCajaPorFecha(Date fecha) {
        jpRegistrarCobros.setVisible(false);
        jpAperturaCaja.setVisible(false);
        jpRegistrarVentas.setVisible(false);
        jpRegistrarCompras.setVisible(false);
        jpCierreCaja.setVisible(true);

        // Guardar fecha objetivo del cierre (se usa luego al registrar cierre)
        fechaCajaEnCierre = truncDate(fecha);

        // Buscar apertura de esa fecha y guardar su ID (cierre robusto)
        CajaMovimiento apertura = operarCaja.obtenerAperturaDeCajaPorFecha(fechaCajaEnCierre);
        if (apertura == null || apertura.getMonto() == null) {
            JOptionPane.showMessageDialog(this,
                    "No existe apertura de caja para la fecha " + formatFecha(fechaCajaEnCierre) + ".",
                    "Cierre de caja",
                    JOptionPane.WARNING_MESSAGE);
            jpCierreCaja.setVisible(false);
            fechaCajaEnCierre = null;
            aperturaIdEnCierre = null;
            return;
        }
        aperturaIdEnCierre = apertura.getIdMovimiento();

        BigDecimal montoApertura = apertura.getMonto();

        BigDecimal montoIngresos = operarCaja.obtenerTotalIngresosPorFecha(fechaCajaEnCierre);
        BigDecimal montoEgresos = operarCaja.obtenerTotalEgresosPorFecha(fechaCajaEnCierre);
        BigDecimal montoCierreEsperado = montoIngresos.add(montoApertura).subtract(montoEgresos);
        txtEgresosCierreCaja.setText(montoEgresos.toString());
        txtIngresoCierreCaja.setText(montoIngresos.toString());
        // Se precarga el esperado, pero el usuario debe ingresar el monto físico y ahora se valida.
        txtMontoCierreCaja.setText(montoCierreEsperado.toString());
    }

    private Date truncDate(Date d) {
        if (d == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbrirCaja;
    private javax.swing.JButton btnAgregarMetodoPago;
    private javax.swing.JButton btnAgregarProductoVenta;
    private javax.swing.JButton btnCancelarAperturaCaja;
    private javax.swing.JButton btnCancelarCierreCaja;
    private javax.swing.JButton btnCancelarCompra;
    private javax.swing.JButton btnCancelarRegistroVenta;
    private javax.swing.JButton btnCancelarRegistroVenta1;
    private javax.swing.JButton btnCerrarCaja;
    private javax.swing.JButton btnCobros;
    private javax.swing.JButton btnCompras;
    private javax.swing.JButton btnConfirmarVenta;
    private javax.swing.JButton btnLimpiarPagos;
    private javax.swing.JButton btnLimpiarPagosCompras;
    private javax.swing.JButton btnLimpiarVenta;
    private javax.swing.JButton btnRegistrarAperturaCaja;
    private javax.swing.JButton btnRegistrarCierreCaja;
    private javax.swing.JButton btnRegistrarCobro;
    private javax.swing.JButton btnRegistrarCompra;
    private javax.swing.JButton btnRegistrarPagoCompra;
    private javax.swing.JButton btnRegistrarVenta;
    private javax.swing.JButton btnVentas;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JComboBox<ClienteItem> jcbClienteCobro;
    private javax.swing.JComboBox<ClienteItem> jcbClienteVenta;
    private javax.swing.JComboBox<MetodoPagoItem> jcbMetodoDePago;
    private javax.swing.JComboBox<MetodoPagoItem> jcbMetodoDePagoCompra;
    private javax.swing.JComboBox<ProductoItem> jcbProductoVenta;
    private javax.swing.JComboBox<ProveedorItem> jcbProveedorCompra;
    private com.toedter.calendar.JDateChooser jdcFechaFacturaDeCompra;
    private javax.swing.JPanel jpAperturaCaja;
    private javax.swing.JPanel jpCierreCaja;
    private javax.swing.JPanel jpFormaDePago;
    private javax.swing.JPanel jpHadear;
    private javax.swing.JPanel jpInfoCaja;
    private javax.swing.JPanel jpRegistrarCobros;
    private javax.swing.JPanel jpRegistrarCompras;
    private javax.swing.JPanel jpRegistrarVentas;
    private javax.swing.JLabel lbApertura;
    private javax.swing.JLabel lbCajaRegistradora;
    private javax.swing.JLabel lbCantidad;
    private javax.swing.JLabel lbCierreCaja;
    private javax.swing.JLabel lbCliente;
    private javax.swing.JLabel lbCliente1;
    private javax.swing.JLabel lbCompras;
    private javax.swing.JLabel lbDescuento;
    private javax.swing.JLabel lbEgreso;
    private javax.swing.JLabel lbEstadoCajaDesc;
    private javax.swing.JLabel lbFechaDesc;
    private javax.swing.JLabel lbFechaFacturaCompra;
    private javax.swing.JLabel lbFormaDePago;
    private javax.swing.JLabel lbFormaDePago1;
    private javax.swing.JLabel lbFormaDePago2;
    private javax.swing.JLabel lbFormaDePagoCompra;
    private javax.swing.JLabel lbIngreso;
    private javax.swing.JLabel lbIva;
    private javax.swing.JLabel lbMontoCompra;
    private javax.swing.JLabel lbMontoDesc;
    private javax.swing.JLabel lbMontoDesc1;
    private javax.swing.JLabel lbMontoDesc2;
    private javax.swing.JLabel lbMontoDesc3;
    private javax.swing.JLabel lbMontoFinal;
    private javax.swing.JLabel lbMontoInicial;
    private javax.swing.JLabel lbNumeroFactura;
    private javax.swing.JLabel lbNumeroFacturaCompra;
    private javax.swing.JLabel lbProducto;
    private javax.swing.JLabel lbProveedorCompra;
    private javax.swing.JLabel lbSignoPeso;
    private javax.swing.JLabel lbSignoPeso1;
    private javax.swing.JLabel lbSignoPeso2;
    private javax.swing.JLabel lbSignoPeso3;
    private javax.swing.JLabel lbSignoPeso4;
    private javax.swing.JLabel lbSignoPeso5;
    private javax.swing.JLabel lbSignoPeso6;
    private javax.swing.JLabel lbSignoPeso8;
    private javax.swing.JLabel lbSignoPesoCompra;
    private javax.swing.JLabel lbSingoP;
    private javax.swing.JLabel lbSubTotal;
    private javax.swing.JLabel lbTotal;
    private javax.swing.JLabel lbTotalFacturaCompra;
    private javax.swing.JLabel lbUsuarioDesc;
    private javax.swing.JLabel lblEstadoCaja;
    private javax.swing.JLabel lblFechaActual;
    private javax.swing.JLabel lblHoraActual;
    private javax.swing.JLabel lblUsuario;
    private javax.swing.JScrollPane scroll1;
    private javax.swing.JScrollPane scroll2;
    private javax.swing.JScrollPane scroll3;
    private javax.swing.JScrollPane scroll4;
    private javax.swing.JTable tableCuentaCorriente;
    private javax.swing.JTable tableFormaDePago;
    private javax.swing.JTable tablePagosCompra;
    private javax.swing.JTable tableVentas;
    private javax.swing.JTextField txtCantidadVenta;
    private javax.swing.JTextField txtDescuentoVenta;
    private javax.swing.JTextField txtEgresosCierreCaja;
    private javax.swing.JTextField txtIngresoCierreCaja;
    private javax.swing.JTextField txtIvaVenta;
    private javax.swing.JTextField txtMontoCierreCaja;
    private javax.swing.JTextField txtMontoFacturaCompra;
    private javax.swing.JTextField txtMontoFormaDePago;
    private javax.swing.JTextField txtMontoInicialCaja;
    private javax.swing.JTextField txtMontoPagoCC;
    private javax.swing.JTextField txtNumeroFactura;
    private javax.swing.JTextField txtSaldoCuentaCorriente;
    private javax.swing.JTextField txtSaldoPendiente;
    private javax.swing.JTextField txtSubtotalVenta;
    private javax.swing.JTextField txtTotalFacturaCompra;
    private javax.swing.JTextField txtTotalVenta;
    // End of variables declaration//GEN-END:variables

}
