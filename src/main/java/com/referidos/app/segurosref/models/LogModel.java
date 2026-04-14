package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Document(collection = "logs")
@JsonPropertyOrder({"logId", "userId", "type", "message", "updatedDate", "createdDate"})
public class LogModel {

    @Id
    private ObjectId logId;
    private String userId;
    private String type;
    private String message;
    private LocalDateTime updatedDate;
    private LocalDateTime createdDate;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getLogId() {
        return this.logId.toString();
    }

}
