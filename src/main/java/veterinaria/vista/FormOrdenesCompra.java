
package veterinaria.vista;

import com.toedter.calendar.JDateChooser;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import veterinaria.controlador.CompraOrdenControlador;
import veterinaria.controlador.ProductoControlador;
import veterinaria.controlador.ProveedorControlador;
import veterinaria.entidad.CompraOrden;
import veterinaria.entidad.CompraOrdenEstado;
import veterinaria.entidad.CompraOrdenItem;
import veterinaria.entidad.Producto;
import veterinaria.entidad.Proveedor;
import veterinaria.entidad.util.ProductoItem;
import veterinaria.entidad.util.ProveedorItem;
import veterinaria.util.PermisoUI;

public class FormOrdenesCompra extends javax.swing.JPanel {

    // ===== Dependencias (patrón existente del proyecto) =====
    private final CompraOrdenControlador ordenCtrl = new CompraOrdenControlador();
    private final ProveedorControlador proveedorCtrl = new ProveedorControlador();
    private final ProductoControlador productoCtrl = new ProductoControlador();

    // ===== Estado del formulario =====
    private CompraOrden ordenEnEdicion = null; // null = no hay orden cargada
    private boolean modoEdicion = false;

    
    // ===== Selección / Preview =====
    private Integer lastSelectedOrdenId = null;
    private int lastSelectedOrdenRow = -1;
    private boolean internalSelectionChange = false;
// ===== Formatos =====
    private final SimpleDateFormat fmtFecha = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "AR"));
    private final DecimalFormat fmtMoneda = new DecimalFormat("0.00");

    public FormOrdenesCompra() {
        initComponents();
        PermisoUI.aplicar(this);
        postInit();
    }

    private void postInit() {
        // UX
        jpRegistrarPedidos.setVisible(false);
        tableOrdenesDeCompras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableOrdenPedido.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Selección automática + acciones por click/selección
        initTableSelectionBehaviors();

        // Combos + listeners
        initCombos();
        initListeners();

        // Cargar lista
        cargarListaOrdenes();
        setModoEdicion(false);
    }

    // =======================
    // Inicialización de UI
    // =======================
    private void initCombos() {
        // Filtro estados
        DefaultComboBoxModel<String> mFiltro = new DefaultComboBoxModel<>();
        mFiltro.addElement("TODOS");
        mFiltro.addElement("BORRADOR");
        mFiltro.addElement("CONFIRMADA");
        mFiltro.addElement("CERRADA");
        mFiltro.addElement("ANULADA");
        jcbFiltarEstadoOrden.setModel(mFiltro); 

        // Proveedores
        DefaultComboBoxModel<ProveedorItem> mProv = new DefaultComboBoxModel<>();
        mProv.addElement(new ProveedorItem(null, "Seleccionar Proveedor"));
        for (Proveedor p : proveedorCtrl.buscarTodosLosProveedores()) {
            String rs = (p.getRazonSocial() != null && !p.getRazonSocial().isBlank())
                    ? p.getRazonSocial()
                    : (p.getPersona() != null ? (p.getPersona().getNombre() + " " + p.getPersona().getApellido()) : "Proveedor " + p.getIdProveedor());
            mProv.addElement(new ProveedorItem(p.getIdProveedor(), rs));
        }
        jcbProveedor.setModel(mProv);

        // Categorías (rubro de producto) y productos
        refrescarCategoriasYProductos();
    }

    private void initListeners() {
        // Búsqueda en vivo
        txtBusquedaOrdenCompra.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cargarListaOrdenes(); }
            @Override public void removeUpdate(DocumentEvent e) { cargarListaOrdenes(); }
            @Override public void changedUpdate(DocumentEvent e) { cargarListaOrdenes(); }
        });

        jcbFiltarEstadoOrden.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                cargarListaOrdenes();
            }
        });

        jcbCategoriaProducto.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                cargarProductosPorCategoria();
            }
        }); 
    }


    private void initTableSelectionBehaviors() {
        // 1) Tabla de órdenes: al clickear, seleccionar fila y ajustar botones según estado
        tableOrdenesDeCompras.setRowSelectionAllowed(true);
        tableOrdenesDeCompras.setColumnSelectionAllowed(false);
        tableOrdenesDeCompras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onOrdenListaSeleccionada();
            }
        });
        tableOrdenesDeCompras.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tableOrdenesDeCompras.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    tableOrdenesDeCompras.setRowSelectionInterval(row, row);
                    marcarCheckboxFilaOrden(row);
                    onOrdenListaSeleccionada();
                }
            }
        });

        // 2) Tabla de items (productos) de la orden: al clickear, seleccionar fila y rellenar combos + cantidad
        tableOrdenPedido.setRowSelectionAllowed(true);
        tableOrdenPedido.setColumnSelectionAllowed(false);
        tableOrdenPedido.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onItemOrdenSeleccionado();
            }
        });
        tableOrdenPedido.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tableOrdenPedido.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    tableOrdenPedido.setRowSelectionInterval(row, row);
                    onItemOrdenSeleccionado();
                }
            }
        });

        // Estado inicial de botones
        onOrdenListaSeleccionada();
    }

    private void onOrdenListaSeleccionada() {
        if (internalSelectionChange) {
            return;
        }

        int selectedRow = tableOrdenesDeCompras.getSelectedRow();
        if (selectedRow >= 0) {
            marcarCheckboxFilaOrden(selectedRow);
        }

        Integer id = getIdOrdenSeleccionadaEnLista();
        if (id == null) {
            btnEditarOrdenPedido.setEnabled(false);
            btnConfirmarOrden.setEnabled(false);
            btnEliminarOrdenPedido.setEnabled(false);
            jpRegistrarPedidos.setVisible(false);
            ordenEnEdicion = null;
            lastSelectedOrdenId = null;
            lastSelectedOrdenRow = -1;
            return;
        }

        // Preview sin pisar: si estoy editando otra orden, pedir confirmación para descartar
        Integer idActualEdicion = (ordenEnEdicion != null && ordenEnEdicion.getIdCompraOrden() != null) ? ordenEnEdicion.getIdCompraOrden() : null;
        boolean cambiandoDeOrden = (idActualEdicion != null && !idActualEdicion.equals(id));

        if (modoEdicion && cambiandoDeOrden) {
            int r = JOptionPane.showConfirmDialog(
                    this,
                    "Estás editando una orden. Si seleccionás otra, se van a perder los cambios no guardados.\n\n¿Descartar cambios y ver la otra orden?",
                    "Descartar cambios",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (r != JOptionPane.YES_OPTION) {
                // Revertir selección a la orden previa
                revertirSeleccionOrdenAnterior();
                return;
            }

            // Descarta edición actual y pasa a preview de la nueva
            setModoEdicion(false);
        }

        CompraOrden o = ordenCtrl.buscarPorId(id);
        if (o == null || o.getEstado() == null) {
            btnEditarOrdenPedido.setEnabled(false);
            btnConfirmarOrden.setEnabled(false);
            btnEliminarOrdenPedido.setEnabled(false);
            return;
        }

        CompraOrdenEstado estado = o.getEstado();

        boolean esBorrador = (estado == CompraOrdenEstado.BORRADOR);
        boolean esConfirmada = (estado == CompraOrdenEstado.CONFIRMADA);

        btnEditarOrdenPedido.setEnabled(esBorrador);
        btnEliminarOrdenPedido.setEnabled(esBorrador);
        btnConfirmarOrden.setEnabled(esBorrador);

        if (esConfirmada) {
            btnEditarOrdenPedido.setEnabled(false);
            btnEliminarOrdenPedido.setEnabled(false);
            btnConfirmarOrden.setEnabled(false);
        } 

        mostrarPreview(o);

        // Guardar "última selección" exitosa
        lastSelectedOrdenId = id;
        lastSelectedOrdenRow = selectedRow;
    }


    /**
     * Mantiene la lógica de selección por checkbox (columna 0) sincronizada con el click/selección de fila.
     * Marca solo la fila indicada y desmarca el resto.
     */
    private void marcarCheckboxFilaOrden(int row) {
        if (row < 0) return;

        DefaultTableModel model = (DefaultTableModel) tableOrdenesDeCompras.getModel();
        if (row >= model.getRowCount()) return;

        try {
            internalSelectionChange = true;
            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean current = (Boolean) model.getValueAt(i, 0);
                boolean shouldBe = (i == row);
                if (!Boolean.valueOf(shouldBe).equals(current)) {
                    model.setValueAt(shouldBe, i, 0);
                }
            }
        } finally {
            internalSelectionChange = false;
        }
    }

    /**
     * Vuelve a seleccionar la orden previa cuando el usuario decide NO descartar cambios.
     */
    private void revertirSeleccionOrdenAnterior() {
        if (lastSelectedOrdenRow < 0) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) tableOrdenesDeCompras.getModel();
        if (lastSelectedOrdenRow >= model.getRowCount()) {
            return;
        }

        try {
            internalSelectionChange = true;
            tableOrdenesDeCompras.setRowSelectionInterval(lastSelectedOrdenRow, lastSelectedOrdenRow);
            marcarCheckboxFilaOrden(lastSelectedOrdenRow);
        } finally {
            internalSelectionChange = false;
        }
    }

    
    /**
     * Modo "preview": al seleccionar una orden en la lista, se carga en el panel
     * para visualizar detalles sin entrar en edición. Si la orden es BORRADOR,
     * se habilitan los botones Editar/Confirmar (según reglas); si no, quedan deshabilitados.
     */
    private void mostrarPreview(CompraOrden o) {
        if (o == null) return;

        // Mostrar panel de registro para ver datos de la orden
        jpRegistrarPedidos.setVisible(true);

        // Cargar la orden en el formulario en modo solo lectura
        this.ordenEnEdicion = o;
        setModoEdicion(false);

        cargarOrdenEnFormulario(o);

        // En preview, no se debería poder modificar valores desde el teclado aunque estén disabled
        txtNumeroOrden.setEditable(false);
    }


    private void onItemOrdenSeleccionado() {
        int row = tableOrdenPedido.getSelectedRow();
        if (row < 0) return;

        // Columnas esperadas en tabla de items: [0]=sel, [1]=codigo, [2]=producto, [3]=cantidad, ...
        String codigo = safe(tableOrdenPedido.getValueAt(row, 1), "");
        String nombre = safe(tableOrdenPedido.getValueAt(row, 2), "");
        int cantidad = parseIntSafe(tableOrdenPedido.getValueAt(row, 3));

        // Rellenar cantidad
        txtCantidadProducto.setText(cantidad > 0 ? String.valueOf(cantidad) : "");

        // Rellenar categoría + producto buscando el producto real (por código o nombre)
        Producto p = buscarProductoPorCodigoONombre(codigo, nombre);
        if (p != null) {
            // seleccionar categoría
            if (p.getRubro() != null && !p.getRubro().isBlank()) {
                seleccionarCategoriaEnCombo(p.getRubro());
            }
            // cargar productos por categoría (para que el combo se repueble) y luego seleccionar producto
            cargarProductosPorCategoria();
            seleccionarProductoEnCombo(p.getIdProducto());
        }
    }

    private Producto buscarProductoPorCodigoONombre(String codigo, String nombre) {
        List<Producto> productos = productoCtrl.buscarTodosLosProductos();
        if (codigo != null && !codigo.isBlank()) {
            for (Producto p : productos) {
                if (p.getCodigo() != null && p.getCodigo().trim().equalsIgnoreCase(codigo.trim())) {
                    return p;
                }
            }
        }
        if (nombre != null && !nombre.isBlank()) {
            for (Producto p : productos) {
                if (p.getNombre() != null && p.getNombre().trim().equalsIgnoreCase(nombre.trim())) {
                    return p;
                }
            }
        }
        return null;
    }

    private void seleccionarCategoriaEnCombo(String rubro) {
        if (rubro == null) return;
        for (int i = 0; i < jcbCategoriaProducto.getItemCount(); i++) {
            Object it = jcbCategoriaProducto.getItemAt(i);
            if (it != null && it.toString().trim().equalsIgnoreCase(rubro.trim())) {
                jcbCategoriaProducto.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarProductoEnCombo(Integer idProducto) {
        if (idProducto == null) return;
        for (int i = 0; i < jcbProductoProducto.getItemCount(); i++) {
            Object obj = jcbProductoProducto.getItemAt(i);
            if (obj instanceof ProductoItem) {
                ProductoItem it = (ProductoItem) obj;
                if (idProducto.equals(it.getIdProducto())) {
                    jcbProductoProducto.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    
    private CompraOrdenEstado parseEstadoUI(String estado) {
        if (estado == null) return CompraOrdenEstado.BORRADOR;
        String s = estado.trim().toUpperCase(Locale.ROOT);
        if ("CONFIRMADO".equals(s)) s = "CONFIRMADA";
        try {
            return CompraOrdenEstado.valueOf(s);
        } catch (Exception e) {
            return CompraOrdenEstado.BORRADOR;
        }
    }

// =======================
    // Acciones principales
    // =======================

    private void btnNuevaOrdenCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevaOrdenCompraActionPerformed
        crearNuevaOrden();
    }//GEN-LAST:event_btnNuevaOrdenCompraActionPerformed

    private void btnEliminarOrdenPedidoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarOrdenPedidoActionPerformed
        anularOrdenSeleccionada();
    }//GEN-LAST:event_btnEliminarOrdenPedidoActionPerformed

    private void btnGuardarOrdenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarOrdenActionPerformed
        guardarOrden();
    }//GEN-LAST:event_btnGuardarOrdenActionPerformed

    private void btnCancelarOrdenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarOrdenActionPerformed
        cancelarEdicion();
    }//GEN-LAST:event_btnCancelarOrdenActionPerformed

    private void btnLimpiarOrdenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarOrdenActionPerformed
        limpiarFormularioEdicion();
    }//GEN-LAST:event_btnLimpiarOrdenActionPerformed

    private void btnQuitarProductoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitarProductoActionPerformed
        quitarItemsSeleccionados();
    }//GEN-LAST:event_btnQuitarProductoActionPerformed

    private void btnAgregarProductoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarProductoActionPerformed
        agregarProductoALaOrden();
    }//GEN-LAST:event_btnAgregarProductoActionPerformed

    // =======================
    // Lógica de negocio
    // =======================

    private void crearNuevaOrden() {
        ordenEnEdicion = new CompraOrden();
        ordenEnEdicion.setEstado(CompraOrdenEstado.BORRADOR);
        ordenEnEdicion.setFechaPedido(new Date());

        jpRegistrarPedidos.setVisible(true);
        limpiarFormularioEdicion();

        // Defaults
        jdcFechaPedido.setDate(new Date());
        txtNumeroOrden.setText(ordenCtrl.sugerirProximoNumero());
        txtNumeroOrden.setEditable(true);

        setModoEdicion(true);
        aplicarBloqueoPorEstado();
        revalidate();
        repaint();
    }

    private void editarOrdenSeleccionada() {
        Integer id = getIdOrdenSeleccionadaEnLista();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná una orden de la lista.");
            return;
        }

        CompraOrden o = ordenCtrl.buscarPorId(id);
        if (o == null) {
            JOptionPane.showMessageDialog(this, "No se encontró la orden seleccionada.");
            return;
        }

        // Regla: solo se puede editar una orden en BORRADOR
        if (o.getEstado() != null && o.getEstado() != CompraOrdenEstado.BORRADOR) {
            JOptionPane.showMessageDialog(this, "Solo se puede editar una orden en estado BORRADOR.");
            return;
        }

        ordenEnEdicion = o;
        jpRegistrarPedidos.setVisible(true);
        cargarOrdenEnFormulario(o);
        setModoEdicion(true);
        aplicarBloqueoPorEstado();
    }

    private void anularOrdenSeleccionada() {
        Integer id = getIdOrdenSeleccionadaEnLista();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná una orden de la lista.");
            return;
        }

        CompraOrden o = ordenCtrl.buscarPorId(id);
        if (o == null) {
            JOptionPane.showMessageDialog(this, "No se encontró la orden seleccionada.");
            return;
        }
        if (o.getEstado() != null && o.getEstado() != CompraOrdenEstado.BORRADOR) {
            JOptionPane.showMessageDialog(this, "Una orden CONFIRMADA no se puede eliminar/anular.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(
                this,
                "¿Anular la orden seleccionada?\n(No se borrará: quedará como ANULADA)",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        boolean r = ordenCtrl.anular(id);
        if (!r) {
            JOptionPane.showMessageDialog(this, "No se pudo anular la orden.");
            return;
        }
        cargarListaOrdenes();
        JOptionPane.showMessageDialog(this, "Orden anulada.");
    }

    private void confirmarOrdenSeleccionada() {
        Integer id = getIdOrdenSeleccionadaEnLista();

        // Si estoy editando una orden ya guardada, permitir confirmar esa directamente
        if (id == null && modoEdicion && ordenEnEdicion != null && ordenEnEdicion.getIdCompraOrden() != null) {
            id = ordenEnEdicion.getIdCompraOrden();
        }

        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná una orden de la lista para confirmar.");
            return;
        }

        CompraOrden o = ordenCtrl.buscarPorId(id);
        if (o == null) {
            JOptionPane.showMessageDialog(this, "No se encontró la orden seleccionada.");
            return;
        }

        if (o.getEstado() != null && o.getEstado() != CompraOrdenEstado.BORRADOR) {
            JOptionPane.showMessageDialog(this, "Solo se puede confirmar una orden en estado BORRADOR.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmar la orden seleccionada? Luego NO podrá editarse ni eliminarse.",
                "Confirmar orden",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        o.setEstado(CompraOrdenEstado.CONFIRMADA);

        boolean r = ordenCtrl.actualizar(o);
        if (!r) {
            JOptionPane.showMessageDialog(this, "No se pudo confirmar la orden.");
            return;
        }

        // Si estaba abierta en edición, cerrarla
        if (modoEdicion && ordenEnEdicion != null && id.equals(ordenEnEdicion.getIdCompraOrden())) {
            setModoEdicion(false);
            ordenEnEdicion = null;
            jpRegistrarPedidos.setVisible(false);
            limpiarFormularioEdicion();
        }

        cargarListaOrdenes();
        JOptionPane.showMessageDialog(this, "Orden confirmada.");
    }


    private void guardarOrden() {
        if (!modoEdicion || ordenEnEdicion == null) {
            JOptionPane.showMessageDialog(this, "No hay una orden en edición.");
            return;
        }

        if (ordenEnEdicion.getIdCompraOrden() != null && (ordenEnEdicion.getEstado() == CompraOrdenEstado.CONFIRMADA || ordenEnEdicion.getEstado() == CompraOrdenEstado.CERRADA || ordenEnEdicion.getEstado() == CompraOrdenEstado.ANULADA)) {
            JOptionPane.showMessageDialog(this, "La orden está CONFIRMADA y no se puede modificar.\nSi necesitás corregir, anulá y creá una nueva.");
            return;
        }

        // Validaciones cabecera
        ProveedorItem provItem = (ProveedorItem) jcbProveedor.getSelectedItem();
        if (provItem == null || provItem.getIdProveedor() == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un proveedor.");
            return;
        }
        Date fecha = safeDate(jdcFechaPedido);
        if (fecha == null) {
            fecha = new Date();
            jdcFechaPedido.setDate(fecha);
        }

        String numero = txtNumeroOrden.getText() != null ? txtNumeroOrden.getText().trim() : "";
        if (numero.isBlank()) {
            numero = ordenCtrl.sugerirProximoNumero();
            txtNumeroOrden.setText(numero);
        }

        // Validar número único
        Integer idExcluida = ordenEnEdicion.getIdCompraOrden();
        if (ordenCtrl.existeNumeroOrden(numero, idExcluida)) {
            JOptionPane.showMessageDialog(this, "El número de orden ya existe.\nElegí otro o dejá vacío para autogenerar.");
            return;
        }

        // Ítems desde la tabla
        List<CompraOrdenItem> items = construirItemsDesdeTabla();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregá al menos 1 producto a la orden.");
            return;
        }

        // Cargar entidad
        Proveedor proveedor = proveedorCtrl.buscarProveedorPorId(provItem.getIdProveedor());
        if (proveedor == null) {
            JOptionPane.showMessageDialog(this, "Proveedor inválido.");
            return;
        }

        ordenEnEdicion.setNumeroOrden(numero);
        ordenEnEdicion.setFechaPedido(fecha);
        // Estado: siempre BORRADOR al guardar desde este formulario.
        // El cambio a CONFIRMADA se hace exclusivamente desde el botón Confirmar.
        ordenEnEdicion.setEstado(CompraOrdenEstado.BORRADOR);
        ordenEnEdicion.setProveedor(proveedor);

        // Asociar items
        for (CompraOrdenItem it : items) {
            it.setOrden(ordenEnEdicion);
        }
        ordenEnEdicion.setItems(items);

        boolean ok;
        if (ordenEnEdicion.getIdCompraOrden() == null) {
            ok = ordenCtrl.crear(ordenEnEdicion);
        } else {
            ok = ordenCtrl.actualizar(ordenEnEdicion);
        }

        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la orden.");
            return;
        }

        JOptionPane.showMessageDialog(this, "Orden guardada.");
        cargarListaOrdenes();
        setModoEdicion(false);
        ordenEnEdicion = null;
        jpRegistrarPedidos.setVisible(false);
    }

    private void cancelarEdicion() {
        int ok = JOptionPane.showConfirmDialog(this, "¿Cancelar la edición?\nSe perderán cambios no guardados.", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        setModoEdicion(false);
        ordenEnEdicion = null;
        jpRegistrarPedidos.setVisible(false);
        limpiarFormularioEdicion();
    }

    private void agregarProductoALaOrden() {
        if (!modoEdicion || ordenEnEdicion == null) {
            JOptionPane.showMessageDialog(this, "Primero creá o editá una orden.");
            return;
        }
        if (isOrdenConfirmada()) {
            JOptionPane.showMessageDialog(this, "La orden está CONFIRMADA y no se puede modificar.");
            return;
        }

        ProductoItem pItem = (ProductoItem) jcbProductoProducto.getSelectedItem();
        if (pItem == null || pItem.getIdProducto() == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un producto.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidadProducto.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }

        Producto producto = productoCtrl.buscarProductoPorId(pItem.getIdProducto());
        if (producto == null) {
            JOptionPane.showMessageDialog(this, "Producto inválido.");
            return;
        }

        BigDecimal costo = pedirCostoUnitario(producto);
        if (costo == null) return; // canceló

        agregarOAcumularItemEnTabla(producto, cantidad, costo);
        recalcularSubtotalesYTotal();
        txtCantidadProducto.setText("");
    }

    private void quitarItemsSeleccionados() {
        if (!modoEdicion || ordenEnEdicion == null) {
            JOptionPane.showMessageDialog(this, "No hay una orden en edición.");
            return;
        }
        if (isOrdenConfirmada()) {
            JOptionPane.showMessageDialog(this, "La orden está CONFIRMADA y no se puede modificar.");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) tableOrdenPedido.getModel();
        // borrar de abajo hacia arriba
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            Boolean sel = (Boolean) model.getValueAt(i, 0);
            if (Boolean.TRUE.equals(sel)) {
                model.removeRow(i);
            }
        }
        recalcularSubtotalesYTotal();
    }

    // =======================
    // Helpers UI/Modelo
    // =======================

    private void cargarListaOrdenes() {
        String texto = txtBusquedaOrdenCompra.getText() != null ? txtBusquedaOrdenCompra.getText().trim() : "";
        String estado = (String) jcbFiltarEstadoOrden.getSelectedItem();
        List<CompraOrden> ordenes = ordenCtrl.buscarTodos(texto, estado);

        DefaultTableModel model = (DefaultTableModel) tableOrdenesDeCompras.getModel();
        model.setRowCount(0);

        for (CompraOrden o : ordenes) {
            String prov = o.getProveedor() != null ? safe(o.getProveedor().getRazonSocial(), "(Sin proveedor)") : "(Sin proveedor)";

            // resumen productos
            int cantTotal = 0;
            int itemsCount = 0;
            Set<String> productos = new LinkedHashSet<>();
            if (o.getItems() != null) {
                for (CompraOrdenItem it : o.getItems()) {
                    itemsCount++;
                    cantTotal += it.getCantidad() != null ? it.getCantidad() : 0;
                    if (it.getProducto() != null) {
                        productos.add(safe(it.getProducto().getNombre(), "Producto"));
                    
        // Mantener estado de botones acorde a la selección actual
        onOrdenListaSeleccionada();
    }
                }
            }

            String prodResumen = String.join(", ", productos);
            String fecha = o.getFechaPedido() != null ? fmtFecha.format(o.getFechaPedido()) : "";

            model.addRow(new Object[]{
                false,
                o.getIdCompraOrden(),
                prov,
                prodResumen,
                cantTotal,
                (o.getEstado()!=null ? o.getEstado().name() : ""),
                fecha
            });
        }
    }

    private void cargarOrdenEnFormulario(CompraOrden o) {
        limpiarFormularioEdicion();

        txtNumeroOrden.setText(o.getNumeroOrden());
        txtNumeroOrden.setEditable(false); // recomendación: número fijo al editar
        jdcFechaPedido.setDate(o.getFechaPedido());
        // proveedor
        if (o.getProveedor() != null) {
            seleccionarProveedorEnCombo(o.getProveedor().getIdProveedor());
        }

        // items
        DefaultTableModel model = (DefaultTableModel) tableOrdenPedido.getModel();
        model.setRowCount(0);
        if (o.getItems() != null) {
            for (CompraOrdenItem it : o.getItems()) {
                Producto p = it.getProducto();
                model.addRow(new Object[]{
                    false,
                    p != null ? p.getCodigo() : "",
                    p != null ? p.getNombre() : "",
                    it.getCantidad(),
                    it.getCostoUnitario() != null ? it.getCostoUnitario() : BigDecimal.ZERO,
                    it.getSubtotal() != null ? it.getSubtotal() : BigDecimal.ZERO
                });
            }
        }
        recalcularSubtotalesYTotal();
    }

    private void limpiarFormularioEdicion() {
        txtNumeroOrden.setText("");
        txtNumeroOrden.setEditable(true);
        jdcFechaPedido.setDate(new Date());
        jcbProveedor.setSelectedIndex(0);
        txtCantidadProducto.setText("");
        if (jcbCategoriaProducto.getItemCount() > 0) jcbCategoriaProducto.setSelectedIndex(0);
        if (jcbProductoProducto.getItemCount() > 0) jcbProductoProducto.setSelectedIndex(0);
        DefaultTableModel model = (DefaultTableModel) tableOrdenPedido.getModel();
        model.setRowCount(0);
    }

    private void setModoEdicion(boolean enabled) {
        this.modoEdicion = enabled;

        // Header
        btnNuevaOrdenCompra.setEnabled(true);

        // Panel edición
        btnGuardarOrden.setEnabled(enabled);
        btnCancelarOrden.setEnabled(enabled);
        btnLimpiarOrden.setEnabled(enabled);
        btnAgregarProducto.setEnabled(enabled);
        btnQuitarProducto.setEnabled(enabled);
        txtNumeroOrden.setEnabled(enabled);
        jdcFechaPedido.setEnabled(enabled);
        jcbProveedor.setEnabled(enabled);        
        jcbCategoriaProducto.setEnabled(enabled);
        jcbProductoProducto.setEnabled(enabled);
        txtCantidadProducto.setEnabled(enabled);
    }

    private void aplicarBloqueoPorEstado() {
        if (!modoEdicion) return;

        boolean bloqueada = isOrdenConfirmada();

        // Si está confirmada/cerrada/anulada: bloquear edición completa
        txtNumeroOrden.setEnabled(!bloqueada);
        jdcFechaPedido.setEnabled(!bloqueada);
        jcbProveedor.setEnabled(!bloqueada);
        btnAgregarProducto.setEnabled(!bloqueada);
        btnQuitarProducto.setEnabled(!bloqueada);
        jcbCategoriaProducto.setEnabled(!bloqueada);
        jcbProductoProducto.setEnabled(!bloqueada);
        txtCantidadProducto.setEnabled(!bloqueada);

        // Guardar habilitado solo si está en BORRADOR (o si es una nueva sin ID aún)
        boolean nuevaSinId = (ordenEnEdicion != null && ordenEnEdicion.getIdCompraOrden() == null);
        btnGuardarOrden.setEnabled(!bloqueada || nuevaSinId);
    }

    private boolean isOrdenConfirmada() {
        if (ordenEnEdicion == null || ordenEnEdicion.getEstado() == null) return false;
        CompraOrdenEstado e = ordenEnEdicion.getEstado();
        return e == CompraOrdenEstado.CONFIRMADA || e == CompraOrdenEstado.CERRADA || e == CompraOrdenEstado.ANULADA;
    }

    private Integer getIdOrdenSeleccionadaEnLista() {
        int row = tableOrdenesDeCompras.getSelectedRow();
        if (row >= 0) {
            Object id = tableOrdenesDeCompras.getValueAt(row, 1);
            return (id instanceof Integer) ? (Integer) id : (id != null ? Integer.valueOf(id.toString()) : null);
        }

        // fallback: si no hay selectedRow, tomar checkbox
        DefaultTableModel model = (DefaultTableModel) tableOrdenesDeCompras.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean sel = (Boolean) model.getValueAt(i, 0);
            if (Boolean.TRUE.equals(sel)) {
                Object id = model.getValueAt(i, 1);
                return (id instanceof Integer) ? (Integer) id : (id != null ? Integer.valueOf(id.toString()) : null);
            }
        }
        return null;
    }

    private void seleccionarProveedorEnCombo(Integer idProveedor) {

        // fallback por defecto
        jcbProveedor.setSelectedIndex(0);

        if (idProveedor == null) {
            return;
        }

        for (int i = 0; i < jcbProveedor.getItemCount(); i++) {
            Object obj = jcbProveedor.getItemAt(i);

            if (obj instanceof ProveedorItem) {
                ProveedorItem it = (ProveedorItem) obj;
                if (idProveedor.equals(it.getIdProveedor())) {
                    jcbProveedor.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void refrescarCategoriasYProductos() {
        List<Producto> productos = productoCtrl.buscarTodosLosProductos();
        Set<String> rubros = new LinkedHashSet<>();
        for (Producto p : productos) {
            if (p.getRubro() != null && !p.getRubro().isBlank()) {
                rubros.add(p.getRubro().trim());
            }
        }
        DefaultComboBoxModel<String> mCat = new DefaultComboBoxModel<>();
        mCat.addElement("Elije Categoría");
        for (String r : rubros) mCat.addElement(r);
        jcbCategoriaProducto.setModel(mCat);

        // productos inicial
        DefaultComboBoxModel<ProductoItem> mProd = new DefaultComboBoxModel<>();
        mProd.addElement(new ProductoItem(null, "Elije Producto"));
        for (Producto p : productos) {
            mProd.addElement(new ProductoItem(p.getIdProducto(), p.getNombre()));
        }
        jcbProductoProducto.setModel(mProd);
    }

    private void cargarProductosPorCategoria() {
        String cat = (String) jcbCategoriaProducto.getSelectedItem();
        List<Producto> productos = productoCtrl.buscarTodosLosProductos();

        DefaultComboBoxModel<ProductoItem> mProd = new DefaultComboBoxModel<>();
        mProd.addElement(new ProductoItem(null, "Elije Producto"));

        for (Producto p : productos) {
            if (cat == null || "Elije Categoría".equalsIgnoreCase(cat)) {
                mProd.addElement(new ProductoItem(p.getIdProducto(), p.getNombre()));
            } else {
                if (p.getRubro() != null && p.getRubro().trim().equalsIgnoreCase(cat.trim())) {
                    mProd.addElement(new ProductoItem(p.getIdProducto(), p.getNombre()));
                }
            }
        }

        jcbProductoProducto.setModel(mProd);
        if (jcbProductoProducto.getItemCount() > 0) jcbProductoProducto.setSelectedIndex(0);
    }

    private BigDecimal pedirCostoUnitario(Producto producto) {
        // Default: usar precio actual como referencia (el usuario lo puede cambiar)
        String def = producto.getPrecioCosto() != null ? producto.getPrecioCosto().toString() : "0";
        String in = JOptionPane.showInputDialog(this, "Costo unitario para: " + producto.getNombre(), def);
        if (in == null) return null; // canceló
        try {
            BigDecimal v = new BigDecimal(in.trim().replace(",", "."));
            if (v.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException();
            return v;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Costo unitario inválido.");
            return null;
        }
    }

    private void agregarOAcumularItemEnTabla(Producto producto, int cantidad, BigDecimal costo) {
        DefaultTableModel model = (DefaultTableModel) tableOrdenPedido.getModel();

        // Buscar si ya existe por código
        for (int i = 0; i < model.getRowCount(); i++) {
            Object cod = model.getValueAt(i, 1);
            if (cod != null && cod.toString().equalsIgnoreCase(safe(producto.getCodigo(), ""))) {
                int cantActual = parseIntSafe(model.getValueAt(i, 3));
                int nueva = cantActual + cantidad;
                model.setValueAt(nueva, i, 3);
                model.setValueAt(costo, i, 4); // si cambió costo, lo actualizamos
                BigDecimal subtotal = costo.multiply(BigDecimal.valueOf(nueva));
                model.setValueAt(subtotal, i, 5);
                return;
            }
        }

        BigDecimal subtotal = costo.multiply(BigDecimal.valueOf(cantidad));
        model.addRow(new Object[]{
            false,
            producto.getCodigo(),
            producto.getNombre(),
            cantidad,
            costo,
            subtotal
        });
    }

    private void recalcularSubtotalesYTotal() {
        DefaultTableModel model = (DefaultTableModel) tableOrdenPedido.getModel();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < model.getRowCount(); i++) {
            int cant = parseIntSafe(model.getValueAt(i, 3));
            BigDecimal costo = parseBigDecimalSafe(model.getValueAt(i, 4));
            BigDecimal sub = costo.multiply(BigDecimal.valueOf(cant));
            model.setValueAt(sub, i, 5);
            total = total.add(sub);
        }
        // Si querés mostrar total en algún label/campo, lo podrías agregar en el .form.
    }

    private List<CompraOrdenItem> construirItemsDesdeTabla() {
        DefaultTableModel model = (DefaultTableModel) tableOrdenPedido.getModel();
        Map<String, CompraOrdenItem> porCodigo = new LinkedHashMap<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String codigo = safe(model.getValueAt(i, 1), "").trim();
            if (codigo.isBlank()) continue;
            String desc = safe(model.getValueAt(i, 2), "");
            int cant = parseIntSafe(model.getValueAt(i, 3));
            BigDecimal costo = parseBigDecimalSafe(model.getValueAt(i, 4));

            if (cant <= 0 || costo.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Producto producto = buscarProductoPorCodigo(codigo);
            if (producto == null) {
                // fallback: buscar por nombre
                producto = buscarProductoPorNombre(desc);
            }
            if (producto == null) {
                // Si no existe, se descarta para evitar FK inválida
                continue;
            }

            CompraOrdenItem it = new CompraOrdenItem();
            it.setProducto(producto);
            it.setCantidad(cant);
            it.setCostoUnitario(costo);
            it.setSubtotal(costo.multiply(BigDecimal.valueOf(cant)));

            porCodigo.put(codigo, it);
        }

        return new ArrayList<>(porCodigo.values());
    }

    private Producto buscarProductoPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        for (Producto p : productoCtrl.buscarTodosLosProductos()) {
            if (p.getCodigo() != null && p.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return p;
            }
        }
        return null;
    }

    private Producto buscarProductoPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        for (Producto p : productoCtrl.buscarTodosLosProductos()) {
            if (p.getNombre() != null && p.getNombre().equalsIgnoreCase(nombre.trim())) {
                return p;
            }
        }
        return null;
    }

    private static Date safeDate(JDateChooser dc) {
        try {
            return dc.getDate();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(Object v, String def) {
        if (v == null) return def;
        String s = v.toString();
        return s != null ? s : def;
    }

    private static int parseIntSafe(Object v) {
        if (v == null) return 0;
        try {
            if (v instanceof Integer) return (Integer) v;
            return Integer.parseInt(v.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static BigDecimal parseBigDecimalSafe(Object v) {
        if (v == null) return BigDecimal.ZERO;
        try {
            if (v instanceof BigDecimal) return (BigDecimal) v;
            return new BigDecimal(v.toString().trim().replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // =======================
    // NetBeans UI (generated)
    // =======================

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpHeader = new javax.swing.JPanel();
        lbInternaciones = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbBusquedaOrdenCompra = new javax.swing.JLabel();
        lbBuscar = new javax.swing.JLabel();
        txtBusquedaOrdenCompra = new javax.swing.JTextField();
        btnNuevaOrdenCompra = new javax.swing.JButton();
        btnEditarOrdenPedido = new javax.swing.JButton();
        btnEliminarOrdenPedido = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jcbFiltarEstadoOrden = new javax.swing.JComboBox<>();
        lbFiltarEstadoOrden = new javax.swing.JLabel();
        btnConfirmarOrden = new javax.swing.JButton();
        jpRegistrarPedidos = new javax.swing.JPanel();
        lbRegistarIngresos = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jcbProveedor = new javax.swing.JComboBox<>();
        lbProveedor = new javax.swing.JLabel();
        lbCantidad = new javax.swing.JLabel();
        txtCantidadProducto = new javax.swing.JTextField();
        btnGuardarOrden = new javax.swing.JButton();
        btnCancelarOrden = new javax.swing.JButton();
        btnLimpiarOrden = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JSeparator();
        lbFechaPedido = new javax.swing.JLabel();
        jdcFechaPedido = new com.toedter.calendar.JDateChooser();
        scroll4 = new javax.swing.JScrollPane();
        tableOrdenPedido = new veterinaria.vista.table.AutoTable();
        btnQuitarProducto = new javax.swing.JButton();
        txtNumeroOrden = new javax.swing.JTextField();
        lbNumeroOrden = new javax.swing.JLabel();
        jcbCategoriaProducto = new javax.swing.JComboBox<>();
        jcbProductoProducto = new javax.swing.JComboBox<>();
        lbCategoriaProducto = new javax.swing.JLabel();
        lbProductoProducto = new javax.swing.JLabel();
        btnAgregarProducto = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JSeparator();
        jpListaProcedimientos3 = new javax.swing.JPanel();
        lbListaDeOrdenes = new javax.swing.JLabel();
        jSeparator8 = new javax.swing.JSeparator();
        scroll3 = new javax.swing.JScrollPane();
        tableOrdenesDeCompras = new veterinaria.vista.table.AutoTable();

        lbInternaciones.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbInternaciones.setText("Lista de Órdenes de Compra");

        lbBusquedaOrdenCompra.setText("BUSCAR PEDIDO");

        lbBuscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/veterinaria/icon/png/search.png"))); // NOI18N

        btnNuevaOrdenCompra.setText("Nueva");
        btnNuevaOrdenCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevaOrdenCompraActionPerformed(evt);
            }
        });

        btnEditarOrdenPedido.setText("Editar");
        btnEditarOrdenPedido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarOrdenPedidoActionPerformed(evt);
            }
        });

        btnEliminarOrdenPedido.setText("Eliminar");
        btnEliminarOrdenPedido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarOrdenPedidoActionPerformed(evt);
            }
        });

        jcbFiltarEstadoOrden.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar Estado" }));

        lbFiltarEstadoOrden.setText("Estado:");

        btnConfirmarOrden.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnConfirmarOrden.setText("Confirmar");
        btnConfirmarOrden.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarOrdenActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpHeaderLayout = new javax.swing.GroupLayout(jpHeader);
        jpHeader.setLayout(jpHeaderLayout);
        jpHeaderLayout.setHorizontalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(lbInternaciones))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addComponent(lbBusquedaOrdenCompra)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnNuevaOrdenCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEditarOrdenPedido, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(btnEliminarOrdenPedido, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jpHeaderLayout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addComponent(lbBuscar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtBusquedaOrdenCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lbFiltarEstadoOrden)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbFiltarEstadoOrden, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnConfirmarOrden))))
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addComponent(jSeparator2))))
                .addContainerGap())
        );
        jpHeaderLayout.setVerticalGroup(
            jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lbInternaciones)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpHeaderLayout.createSequentialGroup()
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbBusquedaOrdenCompra))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnNuevaOrdenCompra)
                        .addComponent(btnEditarOrdenPedido)
                        .addComponent(btnEliminarOrdenPedido)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtBusquedaOrdenCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jcbFiltarEstadoOrden, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbFiltarEstadoOrden)
                        .addComponent(btnConfirmarOrden))
                    .addComponent(lbBuscar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        lbRegistarIngresos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbRegistarIngresos.setText("Nueva Orden de Compra");

        lbProveedor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbProveedor.setText("Proveedor:");

        lbCantidad.setText("Cantidad:");

        btnGuardarOrden.setText("Guardar Orden");
        btnGuardarOrden.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarOrdenActionPerformed(evt);
            }
        });

        btnCancelarOrden.setText("Cancelar");
        btnCancelarOrden.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarOrdenActionPerformed(evt);
            }
        });

        btnLimpiarOrden.setText("Limpiar");
        btnLimpiarOrden.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarOrdenActionPerformed(evt);
            }
        });

        lbFechaPedido.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbFechaPedido.setText("Fecha de Pedido:");

        scroll4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableOrdenPedido.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Código", "Descripción", "Cantidad", "Costo Unitario", "Subtotal"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableOrdenPedido.setMinimumSize(new java.awt.Dimension(848, 220));
        tableOrdenPedido.setPreferredSize(new java.awt.Dimension(848, 220));
        tableOrdenPedido.getTableHeader().setReorderingAllowed(false);
        scroll4.setViewportView(tableOrdenPedido);

        btnQuitarProducto.setText("Quitar Producto");
        btnQuitarProducto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitarProductoActionPerformed(evt);
            }
        });

        lbNumeroOrden.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbNumeroOrden.setText("Nº Orden:");

        jcbCategoriaProducto.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Elije Categoría" }));

        lbCategoriaProducto.setText("Categoría:");

        lbProductoProducto.setText("Producto:");

        btnAgregarProducto.setText("Agregar Producto");
        btnAgregarProducto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarProductoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpRegistrarPedidosLayout = new javax.swing.GroupLayout(jpRegistrarPedidos);
        jpRegistrarPedidos.setLayout(jpRegistrarPedidosLayout);
        jpRegistrarPedidosLayout.setHorizontalGroup(
            jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator5)
                    .addComponent(scroll4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addComponent(lbCategoriaProducto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbCategoriaProducto, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                        .addComponent(lbProductoProducto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbProductoProducto, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbCantidad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCantidadProducto, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 54, Short.MAX_VALUE)
                        .addComponent(btnAgregarProducto))
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(lbRegistarIngresos)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator4)
                            .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                                .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                                        .addComponent(lbNumeroOrden)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtNumeroOrden, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(53, 53, 53)
                                        .addComponent(lbFechaPedido)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jdcFechaPedido, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                                        .addComponent(lbProveedor)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jcbProveedor, 0, 565, Short.MAX_VALUE)))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addComponent(btnQuitarProducto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnGuardarOrden, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCancelarOrden, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(btnLimpiarOrden, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSeparator6)
                    .addContainerGap()))
        );
        jpRegistrarPedidosLayout.setVerticalGroup(
            jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpRegistrarPedidosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbRegistarIngresos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtNumeroOrden, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbNumeroOrden)
                            .addComponent(lbFechaPedido)))
                    .addComponent(jdcFechaPedido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbProveedor)
                    .addComponent(jcbProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCantidadProducto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbCantidad)
                    .addComponent(jcbCategoriaProducto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcbProductoProducto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbCategoriaProducto)
                    .addComponent(lbProductoProducto)
                    .addComponent(btnAgregarProducto))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll4, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                        .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnLimpiarOrden)
                            .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnGuardarOrden)
                                .addComponent(btnCancelarOrden)
                                .addComponent(btnQuitarProducto)))
                        .addGap(0, 25, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jpRegistrarPedidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jpRegistrarPedidosLayout.createSequentialGroup()
                    .addGap(95, 95, 95)
                    .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(214, Short.MAX_VALUE)))
        );

        lbListaDeOrdenes.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbListaDeOrdenes.setText("Lista de Ordenes de Compras");

        scroll3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableOrdenesDeCompras.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "ID", "Proveedor", "Productos", "Cantidad", "Estado", "Fecha de Pedido"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableOrdenesDeCompras.setMinimumSize(new java.awt.Dimension(848, 0));
        tableOrdenesDeCompras.setPreferredSize(null);
        tableOrdenesDeCompras.getTableHeader().setReorderingAllowed(false);
        scroll3.setViewportView(tableOrdenesDeCompras);
        if (tableOrdenesDeCompras.getColumnModel().getColumnCount() > 0) {
            tableOrdenesDeCompras.getColumnModel().getColumn(6).setHeaderValue("Fecha de Pedido");
        }

        javax.swing.GroupLayout jpListaProcedimientos3Layout = new javax.swing.GroupLayout(jpListaProcedimientos3);
        jpListaProcedimientos3.setLayout(jpListaProcedimientos3Layout);
        jpListaProcedimientos3Layout.setHorizontalGroup(
            jpListaProcedimientos3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaProcedimientos3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpListaProcedimientos3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpListaProcedimientos3Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(lbListaDeOrdenes)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpListaProcedimientos3Layout.createSequentialGroup()
                        .addComponent(jSeparator8)
                        .addGap(6, 6, 6))))
            .addComponent(scroll3)
        );
        jpListaProcedimientos3Layout.setVerticalGroup(
            jpListaProcedimientos3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpListaProcedimientos3Layout.createSequentialGroup()
                .addComponent(lbListaDeOrdenes)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll3, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jpHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpRegistrarPedidos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpListaProcedimientos3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpRegistrarPedidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpListaProcedimientos3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnConfirmarOrdenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarOrdenActionPerformed
        confirmarOrdenSeleccionada();
    }//GEN-LAST:event_btnConfirmarOrdenActionPerformed

    private void btnEditarOrdenPedidoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarOrdenPedidoActionPerformed
        editarOrdenSeleccionada();
    }//GEN-LAST:event_btnEditarOrdenPedidoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregarProducto;
    private javax.swing.JButton btnCancelarOrden;
    private javax.swing.JButton btnConfirmarOrden;
    private javax.swing.JButton btnEditarOrdenPedido;
    private javax.swing.JButton btnEliminarOrdenPedido;
    private javax.swing.JButton btnGuardarOrden;
    private javax.swing.JButton btnLimpiarOrden;
    private javax.swing.JButton btnNuevaOrdenCompra;
    private javax.swing.JButton btnQuitarProducto;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JComboBox<String> jcbCategoriaProducto;
    private javax.swing.JComboBox<String> jcbFiltarEstadoOrden;
    private javax.swing.JComboBox<ProductoItem> jcbProductoProducto;
    private javax.swing.JComboBox<ProveedorItem> jcbProveedor;
    private com.toedter.calendar.JDateChooser jdcFechaPedido;
    private javax.swing.JPanel jpHeader;
    private javax.swing.JPanel jpListaProcedimientos3;
    private javax.swing.JPanel jpRegistrarPedidos;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JLabel lbBusquedaOrdenCompra;
    private javax.swing.JLabel lbCantidad;
    private javax.swing.JLabel lbCategoriaProducto;
    private javax.swing.JLabel lbFechaPedido;
    private javax.swing.JLabel lbFiltarEstadoOrden;
    private javax.swing.JLabel lbInternaciones;
    private javax.swing.JLabel lbListaDeOrdenes;
    private javax.swing.JLabel lbNumeroOrden;
    private javax.swing.JLabel lbProductoProducto;
    private javax.swing.JLabel lbProveedor;
    private javax.swing.JLabel lbRegistarIngresos;
    private javax.swing.JScrollPane scroll3;
    private javax.swing.JScrollPane scroll4;
    private javax.swing.JTable tableOrdenPedido;
    private javax.swing.JTable tableOrdenesDeCompras;
    private javax.swing.JTextField txtBusquedaOrdenCompra;
    private javax.swing.JTextField txtCantidadProducto;
    private javax.swing.JTextField txtNumeroOrden;
    // End of variables declaration//GEN-END:variables
}
