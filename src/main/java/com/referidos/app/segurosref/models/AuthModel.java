package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "auth_credentials")
public class AuthModel {

    @Id
    private ObjectId id;

    // Llave de enlace con el usuario o administrador
    @Indexed(name = "email_index", unique = true)
    private String email;

    private String pwd;
    
    private String role; // "USER", "ADMIN", etc.

    private boolean accountConfirmed;

    // Fecha a partir de la cual los tokens emitidos antes de este instante son inválidos
    private LocalDateTime tokenRevocationDate;

    // Códigos de recuperación / registro
    private String codeAuth;
    private LocalDateTime codeExpirationTime;

    // Getter personalizado para obtener el ID como String
    public String getIdAsString() {
        return id != null ? id.toString() : null;
    }
}
