package com.referidos.app.segurosref.requests;

import org.springframework.web.multipart.MultipartFile;

public record UserUpdateRequest(
    String name,
    String surname,
    String phone,
    String address,
    String dateOfBirth,
    MultipartFile profilePicture
) {

}
