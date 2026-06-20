package com.referidos.app.segurosref.requests;

public record ManagerRegisterRequest(
    String name,
    String surname,
    String email
) {
}
