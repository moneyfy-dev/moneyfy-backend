package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"userId", "codeToRefer", "disableAccount", "personalData", "wallet", "notifPreference", "accounts", "quoters"})
@Document(collection="users")
public class UserModel {

    @Id
    private ObjectId userId;
    private String codeToRefer;
    private LocalDateTime disableAccount;
    private UserDataModel personalData;
    private WalletModel wallet;
    private NotificationModel notifPreference;
    private List<AccountModel> accounts;
    private List<QuoterModel> quoters;
    
    // Constructor personalizado
    public UserModel(String codeToRefer, LocalDateTime disableAccount, UserDataModel personalData, WalletModel wallet,
            NotificationModel notifPreference) {
        // Iniciamos la listas de objetos
        this.accounts = new ArrayList<>();
        this.quoters = new ArrayList<>();
        // Asignamos los demás valores
        this.codeToRefer = codeToRefer;
        this.disableAccount = disableAccount;
        this.personalData = personalData;
        this.wallet = wallet;
        this.notifPreference = notifPreference;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getUserId() {
        return userId.toString();
    }

    // Métodos de lógica, propios de la clase
    public List<AccountModel> addAccount(AccountModel accountModel) {
        this.accounts.add(accountModel);
        return this.accounts;
    }

    public List<QuoterModel> addQuoter(QuoterModel quoterModel) {
        this.quoters.add(quoterModel);
        return this.quoters;
    }

}
