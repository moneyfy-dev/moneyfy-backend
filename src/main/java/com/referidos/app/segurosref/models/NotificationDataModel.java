package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDataModel {

    private ObjectId notifId;
    private String message;
    private String type;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getNotifId() {
        return notifId.toString();
    }

}
