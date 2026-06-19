package com.referidos.app.segurosref.dtos.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDto {
    private String managerId;
    private String name;
    private String surname;
    private String email;
    private String status;
}
