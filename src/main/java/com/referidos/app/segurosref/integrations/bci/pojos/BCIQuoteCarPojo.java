package com.referidos.app.segurosref.integrations.bci.pojos;

import java.util.List;

import org.springframework.http.HttpStatusCode;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BCIQuoteCarPojo extends BaseIntegrationResponse {

    // Campos únicos de clase (AJUSTAR ESTRUCTURA DESCONOCIDA DEPENDIENDO DE RESPUESTA Y AJUSTAR EQUALSANDHASHCODE)
    private String responseBodyStr;
    private String statusOrErrorStr;
    private HttpStatusCode statusResponse;

    private String RutaDocumento;
    private Double TasaCambioUF;
    private Double TasaInteresCuota;
    private Double IVA;
    private Integer RutCliente;
    private String RutDV;
    private Integer TipoVehiculo;
    private Integer UsoVehiculo;
    private Integer IdMarca;
    private Integer IdModelo;
    private Integer CantidadCuotas;
    private Integer IdFormaPago;
    private Double Descuento;
    private Integer AnioVehiculo;
    private Integer IdCotizacion;
    private String VigenciaCotizacion; // Formato: "dd-mm-yyyy"
    private Integer Error;
    private List<BCIQuoteCarProdPojo> Productos;

    public BCIQuoteCarPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

    public BCIQuoteCarPojo(Integer internalErrorCode, String responseBodyStr, String statusOrErrorStr, HttpStatusCode statusResponse) {
        super(internalErrorCode);
        this.responseBodyStr = responseBodyStr;
        this.statusOrErrorStr = statusOrErrorStr;
        this.statusResponse = statusResponse;
    }

}
