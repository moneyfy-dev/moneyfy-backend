package com.referidos.app.segurosref.integrations.bci.pojos;

import java.util.List;

import lombok.Data;

@Data
public class BCIQuoteProductPojo {

    private Long IdProducto;
    private String NombreProducto;
    private List<BCIQuoteDescriptionPojo> Tarifas;

}
