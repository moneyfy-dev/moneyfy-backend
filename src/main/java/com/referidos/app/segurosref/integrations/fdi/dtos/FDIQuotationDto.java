package com.referidos.app.segurosref.integrations.fdi.dtos;

import java.util.Set;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FDIQuotationDto extends BaseIntegrationResponse {
    
    // Incluir campos necesarios
    @EqualsAndHashCode.Include
    private String dealToken;
    private Integer itemId;
    private Set<FDIQuotationPlanDto> plans;

    public FDIQuotationDto(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
