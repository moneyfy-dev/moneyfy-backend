package com.referidos.app.segurosref.pojo.bci;

import lombok.Data;

@Data
public class QuoteRateBciPojo {

    private Integer IdDeducible;
    private String DescripcionDeducible;
    private Double PrimaAnualNeta;
    private Double PrimaAnualBruta;
    private Double Impuesto;
    private Integer ValorCuotaPesos;

}
