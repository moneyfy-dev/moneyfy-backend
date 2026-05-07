package com.referidos.app.segurosref.integrations.fdi.pojos;

import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded =  true)
public class FDIQuotePlanParamPojo {

    @EqualsAndHashCode.Include
    private Integer id;
    private String name;
    private String type;
    private Object value_0;
    private String valueType_0;
    private Set<String> rangeDescriptions;
    private List<FDIQuotePlanParamRangePojo> ranges;


}
