package com.referidos.app.segurosref.integrations.bci.pojos;

import lombok.Data;

@Data
public class BCIQuoteDescriptionPojo {

    private Integer IdDeducible;
    private String DescripcionDeducible;
    private Double PrimaAnualNeta;
    private Double PrimaAnualBruta;
    private Double Impuesto;
    private Integer ValorCuotaPesos;

}
