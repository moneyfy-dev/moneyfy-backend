package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "devices")
public class DeviceModel {

    @Id
    private ObjectId deviceId;
    private String device;
    private String user;
    private String refreshToken;
    private Set<String> ips;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Constructor personalizado
    public DeviceModel(String device, String user, String refreshToken, Set<String> ips, LocalDateTime createdDate,
            LocalDateTime updatedDate) {
        this.device = device;
        this.user = user;
        this.refreshToken = refreshToken;
        this.ips = ips;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getDeviceId() {
        return deviceId.toString();
    }

    // Métodos de lógica, propios de la clase
    public Set<String> addIp(String ip) {
        this.ips.add(ip);
        return this.ips;
    }

}
