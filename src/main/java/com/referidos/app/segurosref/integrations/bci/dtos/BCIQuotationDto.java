package com.referidos.app.segurosref.integrations.bci.dtos;

import java.math.BigDecimal;
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

    @EqualsAndHashCode.Include
    private Integer idCotizacion;
    private String nroCotizacion;
    private String vigenciaCotizacion;
    private BigDecimal tasaCambioUF;
    private BigDecimal iva;
    private Integer rutCliente;
    private String rutDV;
    private Integer idMarca;
    private Integer idModelo;
    private Integer anioVehiculo;
    private Integer cantidadCuotas;
    private BigDecimal tasaInteresCuota;
    private List<BCIQuotationPlanDto> plans;

    public BCIQuotationDto(Integer internalErrorCode) {
        super(internalErrorCode);
    }
}
