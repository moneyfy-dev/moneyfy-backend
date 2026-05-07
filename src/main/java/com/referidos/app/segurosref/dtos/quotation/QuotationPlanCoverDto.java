package com.referidos.app.segurosref.dtos.quotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QuotationPlanCoverDto {

    @EqualsAndHashCode.Include
    private Integer id;
    private String name;
    private String generalDescription;
    private String polCad;
    private String value;

}
