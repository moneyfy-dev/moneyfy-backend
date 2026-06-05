package com.referidos.app.segurosref.integrations.bci.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BCIQuotationPlanCoverDto {

    @EqualsAndHashCode.Include
    private Integer id;
    private String name;
    private String generalDescription;
    private String polCad;
    private String value;

}
