package com.referidos.app.segurosref.integrations.fdi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuotationPlanCoverDto {

    @EqualsAndHashCode.Include
    private Integer id;
    private String name;
    private String mainDescription;
    private String generalDescription;
    private Integer isMain;
    private Integer isParam;
    private String valueDescription;
    private String polCad;
    private String value;

}
