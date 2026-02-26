package com.referidos.app.segurosref.requests;

public record PasswordResetRequest(
    String email,
    String code,
    String newPwd,
    String repeatedPwd
) {

}
