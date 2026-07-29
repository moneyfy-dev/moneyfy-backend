package com.referidos.app.segurosref.helpers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.models.QuoterAddressModel;
import com.referidos.app.segurosref.models.QuoterCarModel;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.QuoterOwnerModel;
import com.referidos.app.segurosref.models.QuoterPaymentModel;
import com.referidos.app.segurosref.models.QuoterPlanModel;
import com.referidos.app.segurosref.models.QuoterPurchaserModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;

// Se inyecta como repositorio en el servicio de "Quoter", pero, realizando funcionalidades de servicio
@Component
public class QuoterHelper {

        // Creación de un cotizador para los flujos: "Iniciando" o "Cotizando"
        public QuoterModel createQuoteStructure(QuoterOwnerModel quoterOwner, QuoterCarModel quoterCar,
                        QuoterPurchaserModel quoterPurchaser, String quoterStatus, LocalDateTime currentDateTime) {
                // Estructura de los otros objetos del cotizador (vacíos por el momento)
                QuoterPlanModel quoterPlan = new QuoterPlanModel("", "", "", "", BigDecimal.ZERO, BigDecimal.ZERO, 0,
                                BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, null, "",
                                "", "", null, null, "", "");
                QuoterAddressModel quoterAddress = new QuoterAddressModel("", "", "", "", "");
                QuoterPaymentModel quoterPayment = new QuoterPaymentModel("", "", "", "");
                return new QuoterModel(new ObjectId(), quoterStatus, quoterOwner, quoterCar, quoterPurchaser,
                                quoterPlan,
                                quoterAddress, quoterPayment, currentDateTime, currentDateTime);
        }

        // Generamos el cuerpo de una nueva transacción
        public TransactionModel generateNovaTransactionStructure(String transactionId, String userCId,
                        QuoterModel quoterDB,
                        String status, int commissionTotal, int commissionScope, String observation,
                        LocalDateTime currentDateTime) {
                String planId = quoterDB.getQuoterPlanData().getQuoterPlanId();
                String quoterId = quoterDB.getQuoterId();
                TransactionModel novaTransaction = new TransactionModel(transactionId, planId, userCId, quoterId,
                                status,
                                commissionTotal,
                                commissionScope, true, observation, currentDateTime, currentDateTime,
                                DataHelper.deprecatedDateTime(), DataHelper.deprecatedDateTime());
                novaTransaction.addCommission(new TransactionComissionModel(userCId, commissionTotal, status, "", DataHelper.deprecatedDateTime()));
                return novaTransaction;
        }

}
