package com.referidos.app.segurosref.integrations.fdi.pojos;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuotePlanCoverPojo {

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
