package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Document(collection = "logs")
@JsonPropertyOrder({"logId", "type", "reference", "endpoint", "status", "userId", "transactionId", "referenceId", "data",
        "updatedDate", "createdDate"})
public class LogModel {

    @Id
    private ObjectId logId;
    private String type; // "ERROR", "INFO"
    private String reference; // Mensaje para diferenciar
    private String endpoint;
    private String status; // "Grave", "Informe", "Resuelto"
    private String userId;
    private String transactionId;
    private String referenceId; // Un id dinamico, en caso de que no exista 'userId' o 'transactionId'
    private Map<String, Object> data;
    private LocalDateTime updatedDate;
    private LocalDateTime createdDate;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getLogId() {
        return this.logId.toString();
    }

    // Métodos de lógica, propios de la clase
    public Map<String, Object> addData(String key, Object value) {
        this.data.put(key, value);
        return this.data;
    }


}
