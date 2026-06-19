package com.referidos.app.segurosref.dtos.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDto {
    private String userId;
    private String userName;
    private String message;
}
