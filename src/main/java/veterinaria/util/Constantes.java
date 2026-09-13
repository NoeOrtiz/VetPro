
package veterinaria.util;

import veterinaria.util.enums.EstadoHospitalizacion;
import veterinaria.util.enums.EstadoLaboratorio;
import veterinaria.util.enums.EstadoPeluqueria;
import veterinaria.util.enums.MetodoPagoTipo;

public class Constantes {
    private Constantes() {}   
    
    public enum TipoPermiso {
        FUNCIONALIDAD,
        FORMULARIO
    }

    public static final String METODO_DE_PAGO_EFECTIVO = MetodoPagoTipo.EFECTIVO.getEtiqueta();

    public static final String METODO_DE_PAGO_CUENTAC = MetodoPagoTipo.CUENTA_CORRIENTE.getEtiqueta();

    public static final String METODO_DE_PAGO_TARJETAC = MetodoPagoTipo.TARJETA_CREDITO.getEtiqueta();

    public static final String METODO_DE_PAGO_TARJETAD = MetodoPagoTipo.TARJETA_DEBITO.getEtiqueta();

    public static final String ESTADO_LABORATORIO_PENDIENTE = EstadoLaboratorio.PENDIENTE.getEtiqueta();

    public static final String ESTADO_LABORATORIO_PROCESADO = EstadoLaboratorio.PROCESADO.getEtiqueta();

    public static final String ESTADO_LABORATORIO_ENVIADO = EstadoLaboratorio.ENVIADO.getEtiqueta();

    public static final String ESTADO_LABORATORIO_RECIBIDO = EstadoLaboratorio.RECIBIDO.getEtiqueta();

    public static final String ESTADO_LABORATORIO_COMPLETADO = EstadoLaboratorio.COMPLETADO.getEtiqueta();

    public static final String ESTADO_LABORATORIO_CANCELADO = EstadoLaboratorio.CANCELADO.getEtiqueta();

    public static final String ESTADO_HOSPITALIZACION_PENDIENTE = EstadoHospitalizacion.PENDIENTE.getEtiqueta();

    public static final String ESTADO_HOSPITALIZACION_INTERNADO = EstadoHospitalizacion.INTERNADO.getEtiqueta();

    public static final String ESTADO_HOSPITALIZACION_ALTA = EstadoHospitalizacion.ALTA.getEtiqueta();

    public static final String ESTADO_HOSPITALIZACION_CANCELADO = EstadoHospitalizacion.CANCELADO.getEtiqueta();

    public static final String ESTADO_PELUQUERIA_PENDIENTE = EstadoPeluqueria.PENDIENTE.getEtiqueta();

    public static final String ESTADO_PELUQUERIA_CONFIRMADO = EstadoPeluqueria.CONFIRMADO.getEtiqueta();    

    public static final String ESTADO_PELUQUERIA_COMPLETADO = EstadoPeluqueria.COMPLETADO.getEtiqueta();

    public static final String ESTADO_PELUQUERIA_CANCELADO = EstadoPeluqueria.CANCELADO.getEtiqueta();
 
    public enum EstadoVisita {
        ATENDIENDO,
        FINALIZADO,
        CANCELADO
    }
 
    public static final String TEXT_IVA_SELECTION = "Seleccionar % IVA";
    public static final String TEXT_IVA_21 = "21.0";
    public static final String TEXT_IVA_105 = "10.5";
    public static final String TEXT_IVA_00 = "0.0";
    
    public static final String TEXT_ESTADO_SELECTION = "Seleccionar Estado";
    public static final String TEXT_ESTADO_ACTIVO = "Activo";
    public static final String TEXT_ESTADO_INACTIVO = "Inactivo";
    
    public static final String TEXT_UNIDADMEDIDA_SELECTION = "Seleccionar";
    public static final String TEXT_UNIDADMEDIDA_UNIDAD = "Unidad";
    public static final String TEXT_UNIDADMEDIDA_CAJA = "Caja";
    public static final String TEXT_UNIDADMEDIDA_LITROS = "Litros";
    public static final String TEXT_UNIDADMEDIDA_MILIMETROS = "Mililitros";
    public static final String TEXT_UNIDADMEDIDA_KILOGRAMOS = "Kilogramos";
    public static final String TEXT_UNIDADMEDIDA_PAQUETE = "Paquete";
    public static final String TEXT_UNIDADMEDIDA_FRASCO = "Frasco";
    public static final String TEXT_UNIDADMEDIDA_BOTELLA = "Botella";
    public static final String TEXT_UNIDADMEDIDA_AMPOLLA = "Ampolla";
    public static final String TEXT_UNIDADMEDIDA_SACHET = "Sachet";
    public static final String TEXT_UNIDADMEDIDA_TABLET = "Tablet";
    
    public enum ResultadoEliminarPeluqueria {
        OK,
        NO_EXISTE,
        ESTADO_NO_PERMITIDO,
        ERROR
    }

    enum TipoOperacionCaja {
        CREDITO,
        DEBITO
    }
            
}