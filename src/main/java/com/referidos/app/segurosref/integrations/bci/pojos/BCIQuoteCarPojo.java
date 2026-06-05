package com.referidos.app.segurosref.integrations.bci.pojos;

import java.util.List;

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
public class BCIQuoteCarPojo extends BaseIntegrationResponse {

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

    public BCIQuoteCarPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

    public BCIQuoteCarPojo(Integer internalErrorCode, String responseBodyStr, String statusOrErrorStr, HttpStatusCode statusResponse) {
        super(internalErrorCode);
        this.responseBodyStr = responseBodyStr;
        this.statusOrErrorStr = statusOrErrorStr;
        this.statusResponse = statusResponse;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resultado {
        @JsonProperty("str_NroCotizacion")
        private String strNroCotizacion;

        @JsonProperty("dec_TasaDeCambioUF")
        private Double decTasaDeCambioUF;

        @JsonProperty("dec_Iva")
        private Double decIva;

        @JsonProperty("bool_IsPinValido")
        private Boolean boolIsPinValido;

        @JsonProperty("lst_Productos")
        private List<ProductoWrapper> lstProductos;

        @JsonProperty("int_NroTarificacion")
        private Integer intNroTarificacion;

        @JsonProperty("dt_FinVigencia")
        private String dtFinVigencia;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductoWrapper {
        @JsonProperty("dec_TasaInteresCuota")
        private Double decTasaInteresCuota;

        @JsonProperty("producto")
        private Producto producto;

        @JsonProperty("lst_TarifaVehiculos")
        private List<TarifaVehiculo> lstTarifaVehiculos;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Producto {
        @JsonProperty("int_Codigo_Producto")
        private Integer intCodigoProducto;

        @JsonProperty("int_CantidadCuotas")
        private Integer intCantidadCuotas;

        @JsonProperty("nombreProducto")
        private String nombreProducto;

        @JsonProperty("lst_CodigoCoberturasFlexibles")
        private List<Object> lstCodigoCoberturasFlexibles;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TarifaVehiculo {
        @JsonProperty("lst_Tarifa")
        private List<Tarifa> lstTarifa;

        @JsonProperty("estadoTarifa")
        private EstadoTarifa estadoTarifa;

        @JsonProperty("str_TipoVehiculo")
        private String strTipoVehiculo;

        @JsonProperty("int_Marca")
        private Integer intMarca;

        @JsonProperty("str_Marca")
        private String strMarca;

        @JsonProperty("int_Modelo")
        private Integer intModelo;

        @JsonProperty("str_Modelo")
        private String strModelo;

        @JsonProperty("int_Ano")
        private Integer intAno;

        @JsonProperty("propietario")
        private Propietario propietario;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EstadoTarifa {
        @JsonProperty("codigo")
        private Integer codigo;

        @JsonProperty("mensaje")
        private String mensaje;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Propietario {
        @JsonProperty("int_Rut")
        private Integer intRut;

        @JsonProperty("str_Dv")
        private String strDv;

        @JsonProperty("int_Edad")
        private Integer intEdad;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tarifa {
        @JsonProperty("int_IdDeducible")
        private Integer intIdDeducible;

        @JsonProperty("str_Deducible")
        private String strDeducible;

        @JsonProperty("dec_ValorNetoUfConInteres")
        private Double decValorNetoUfConInteres;

        @JsonProperty("dec_ValorBrutoUfConInteres")
        private Double decValorBrutoUfConInteres;

        @JsonProperty("dec_ImpuestoConInteres")
        private Double decImpuestoConInteres;

        @JsonProperty("dec_ValorNetoUfSinInteres")
        private Double decValorNetoUfSinInteres;

        @JsonProperty("dec_ValorBrutoUfSinInteres")
        private Double decValorBrutoUfSinInteres;

        @JsonProperty("dec_ImpuestoSinInteres")
        private Double decImpuestoSinInteres;

        @JsonProperty("int_ValorBrutoPesos")
        private Double intValorBrutoPesos;

        @JsonProperty("int_ValorCuotaPesos")
        private Double intValorCuotaPesos;

        @JsonProperty("int_ValorCuotaUf")
        private Double intValorCuotaUf;

        @JsonProperty("int_ValorUltimaCuotaUf")
        private Double intValorUltimaCuotaUf;

        @JsonProperty("bool_EsPrimaMinima")
        private Boolean boolEsPrimaMinima;

        @JsonProperty("int_CantidadCuotas")
        private Integer intCantidadCuotas;

        @JsonProperty("dec_ValorNetoUfConDescuentos")
        private Double decValorNetoUfConDescuentos;

        @JsonProperty("int_UfDeducible")
        private Integer intUfDeducible;
    }
}
