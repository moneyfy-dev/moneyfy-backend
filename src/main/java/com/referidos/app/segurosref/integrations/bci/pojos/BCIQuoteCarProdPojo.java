package com.referidos.app.segurosref.integrations.bci.pojos;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BCIQuoteCarProdPojo {

    private Integer IdProducto;
    private String NombreProducto;
    private List<BCIQuoteCarProdDetailPojo> Tarifas;

}
