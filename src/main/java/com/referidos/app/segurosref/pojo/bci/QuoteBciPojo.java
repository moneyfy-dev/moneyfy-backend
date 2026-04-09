package com.referidos.app.segurosref.pojo.bci;

import java.util.List;

import lombok.Data;

@Data
public class QuoteBciPojo {

    // Estructura de campos para la API de BCI
    private String RutaDocumento;
    private Double TasaCambioUF;
    private Double TasaInteresCuota;
    private Double IVA;
    private Long RutCliente;
    private String RutDV;
    private Integer TipoVehiculo;
    private Integer UsoVehiculo;
    private Integer IdMarca;
    private Integer IdModelo;
    private Integer CantidadCuotas;
    private Integer IdFormaPago;
    private Double Descuento;
    private Long IdCotizacion;
    private String VigenciaCotizacion; // Formato: "dd-mm-yyyy"
    private List<QuoteProductBciPojo> Productos;
    private Integer Error;

}
