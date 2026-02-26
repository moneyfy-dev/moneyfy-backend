package com.referidos.app.segurosref.dtos;

public record UserSimpleDto(
    String id,
    String name,
    String surname,
    String email,
    String phone,
    String profileRole,
    String dateOfBirth,
    String status
) {

}
