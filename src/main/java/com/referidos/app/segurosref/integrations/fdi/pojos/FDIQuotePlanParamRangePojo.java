package com.referidos.app.segurosref.integrations.fdi.pojos;

import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuotePlanParamRangePojo {
    
    @EqualsAndHashCode.Include
    private Integer id;
    private String name;
    private Set<FDIQuotePlanParamRangeValuePojo> values;

}
