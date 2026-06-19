package com.referidos.app.segurosref.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "managers")
public class ManagerModel {

    @Id
    private ObjectId managerId;
    private String email;
    private String name;
    private String surname;
    private String status;

    public String getManagerId() {
        return managerId != null ? managerId.toString() : null;
    }
}
