package com.referidos.app.segurosref.requests;

public record UserRegisterRequest(
    String name,
    String surname,
    String email,
    String pwd,
    String codeToRefer,
    String profileRole
) {

}
