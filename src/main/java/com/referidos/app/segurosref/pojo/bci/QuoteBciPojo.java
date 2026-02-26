package com.referidos.app.segurosref.pojo.bci;

import java.util.List;

import lombok.Data;

@Data
public class QuoteBciPojo {

    // Estructura de campos para la API de BCI
    private String RutaDocumento;
    private double TasaCambioUF;
    private int CantidadCuotas;
    private int IdFormaPago;
    private double Descuento;
    private int IdCotizacion;
    private String VigenciaCotizacion; // Formato: "dd-mm-yyyy"
    private List<QuoteProductBciPojo> Productos;
    private Integer Error;

}
