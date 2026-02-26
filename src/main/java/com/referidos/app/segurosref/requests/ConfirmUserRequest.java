package com.referidos.app.segurosref.requests;

public record ConfirmUserRequest(
    String email,
    String code
) {

}
