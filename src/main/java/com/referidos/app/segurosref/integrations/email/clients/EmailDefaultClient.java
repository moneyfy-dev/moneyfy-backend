package com.referidos.app.segurosref.integrations.email.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

@Component
public class EmailDefaultClient {

    @Autowired
    private JavaMailSender mailSender;

    public void send(@NonNull MimeMessage message) throws MailException {
        mailSender.send(message);
    }

    public MimeMessage createMimeMessage() {
        return mailSender.createMimeMessage();
    }

}
