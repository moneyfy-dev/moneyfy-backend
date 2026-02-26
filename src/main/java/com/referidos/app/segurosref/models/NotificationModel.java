package com.referidos.app.segurosref.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationModel {

    private boolean byEmail;
    private boolean byPush;
    private boolean commissionUpdate;
    private boolean saleState;
    private boolean withdrawalAvailability;
    private boolean referredRegistered;
    private boolean withdrawalReminder;
    private boolean specialOffers;
    private boolean paymentProblems;
    private List<NotificationDataModel> data;

    // Métodos de lógica, propios de la clase
    public List<NotificationDataModel> addNotif(NotificationDataModel notif) {
        this.data.add(notif);
        return this.data;
    }

}
