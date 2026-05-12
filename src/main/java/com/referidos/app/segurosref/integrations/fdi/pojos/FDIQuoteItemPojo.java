package com.referidos.app.segurosref.integrations.fdi.pojos;

import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuoteItemPojo {

    @EqualsAndHashCode.Include
    private Integer itemId;
    private Set<FDIQuoteDetailPojo> quotations;

}
