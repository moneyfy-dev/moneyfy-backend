package com.referidos.app.segurosref.integrations.bci.dtos;

import java.util.List;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BCIQuotationDto extends BaseIntegrationResponse {

    // Datos entregables
    @EqualsAndHashCode.Include
    private Integer IdCotizacion;
    private String VigenciaCotizacion;
    private String RutaDocumento;
    private Double TasaCambioUF;
    private Double TasaInteresCuota;
    private Double IVA;
    private Integer RutCliente;
    private String RutDV;
    private Integer IdFormaPago;
    private Integer CantidadCuotas;
    private Double Descuento;
    private Integer TipoVehiculo;
    private Integer UsoVehiculo;
    private Integer IdMarca;
    private Integer IdModelo;
    private Integer AnioVehiculo;
    private List<BCIQuotationPlanDto> plans;

    public BCIQuotationDto(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
