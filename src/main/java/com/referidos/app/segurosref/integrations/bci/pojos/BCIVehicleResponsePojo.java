package com.referidos.app.segurosref.integrations.bci.pojos;

import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BCIVehicleResponsePojo extends BaseIntegrationResponse {

    private String responseBodyStr;
    private String statusOrErrorStr;
    private HttpStatusCode statusResponse;

    @JsonProperty("bool_Estado")
    private Boolean boolEstado;

    @JsonProperty("codigo")
    private Integer codigo;

    @JsonProperty("str_Mensaje")
    private String strMensaje;

    @JsonProperty("resultado")
    private Resultado resultado;

    public BCIVehicleResponsePojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

    public BCIVehicleResponsePojo(Integer internalErrorCode, String responseBodyStr, String statusOrErrorStr, HttpStatusCode statusResponse) {
        super(internalErrorCode);
        this.responseBodyStr = responseBodyStr;
        this.statusOrErrorStr = statusOrErrorStr;
        this.statusResponse = statusResponse;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resultado {

        @JsonProperty("str_Patente")
        private String strPatente;

        @JsonProperty("str_DvPAtente")
        private String strDvPatente;

        @JsonProperty("str_Marca")
        private String strMarca;

        @JsonProperty("str_Modelo")
        private String strModelo;

        @JsonProperty("str_Color")
        private String strColor;

        @JsonProperty("str_TipoVehiculo")
        private String strTipoVehiculo;

        @JsonProperty("int_AnioFabricacion")
        private Integer intAnioFabricacion;

        @JsonProperty("str_NumeroMotor")
        private String strNumeroMotor;

        @JsonProperty("str_NumeroChasis")
        private String strNumeroChasis;

        @JsonProperty("dec_Tasacion")
        private String decTasacion;

        @JsonProperty("int_CodMarcaBci")
        private Integer intCodMarcaBci;

        @JsonProperty("int_CodModeloBci")
        private Integer intCodModeloBci;

    }

}
