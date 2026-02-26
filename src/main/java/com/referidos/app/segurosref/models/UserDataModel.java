package com.referidos.app.segurosref.models;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

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

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String pwd;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String pin;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String profileRole;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String codeAuth;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private LocalDateTime codeExpirationTime;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String sessionToken;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String refreshToken;

    // MÉTODOS DE LÓGICA, PROPIOS DE LA CLASE
    // Generar un código aleatorio
    public String generateRandomCode() {
        StringBuilder sb = new StringBuilder("");
        for(int i=0; i<6; i++) {
            sb.append( ((int) (Math.random() * 10)) );
        }
        return sb.toString();
    }

    // Validación del código
    public boolean isCodeActive(LocalDateTime verificationDateTime, int expirationMinutes) {
        long minutesDifference  = ChronoUnit.MINUTES.between(this.codeExpirationTime, verificationDateTime);
        // LOGGER_MESSAGES.info("Los minutos transcurridos son: " + minutesDifference);
        if(minutesDifference < expirationMinutes) {
            return true;
        } else if(minutesDifference  == expirationMinutes) {
            long secondsDifference = ChronoUnit.SECONDS.between(this.codeExpirationTime, verificationDateTime);
            // LOGGER_MESSAGES.info("Los segundos transcurridos son: " + secondsDifference);
            return secondsDifference <= 0;
        }
        return false;
    }
    

}
