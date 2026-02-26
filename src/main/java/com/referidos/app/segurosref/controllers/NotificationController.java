package com.referidos.app.segurosref.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/notifs")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Notification Controller",
    description = "It allows you to charge the notifications that the user already got"
)
public class NotificationController {

    

}
