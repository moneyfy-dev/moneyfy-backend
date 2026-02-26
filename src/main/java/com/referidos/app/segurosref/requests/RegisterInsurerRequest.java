package com.referidos.app.segurosref.requests;

public record RegisterInsurerRequest(
    String key,
    String name,
    String alias,
    String endpoint,
    String darkLogo,
    String lightLogo
) {

}
