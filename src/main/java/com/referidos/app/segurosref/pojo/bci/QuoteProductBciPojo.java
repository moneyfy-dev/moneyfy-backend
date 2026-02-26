package com.referidos.app.segurosref.pojo.bci;

import java.util.List;

import lombok.Data;

@Data
public class QuoteProductBciPojo {

    private int IdProducto;
    private String NombreProducto;
    private List<QuoteRateBciPojo> Tarifas;

}
