package com.referidos.app.segurosref.integrations.bci.pojos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BCIQuoteCarProdDetailPojo {

    private Integer IdDeducible;
    private String DescripcionDeducible;
    private Double PrimaAnualNeta;
    private Double PrimaAnualBruta;
    private Double Impuesto;
    private Double ValorCuotaPesos;

}
