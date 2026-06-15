package com.referidos.app.segurosref.models;

import java.time.LocalDate;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDataModel {

    private String name;
    private String surname;
    @Indexed(name = "email_index", unique = true) // Se agrega un index al email, para buscar registro más rápido
    private String email;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private String status;

    private byte[] profilePicture;

}
