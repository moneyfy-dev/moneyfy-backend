package com.referidos.app.segurosref.helpers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.dtos.quotation.QuotationPlanDto;
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

        // Funciones de apoyo con data de prueba
        public List<QuotationPlanDto> planList1() {
                List<QuotationPlanDto> list = new ArrayList<>();
                double valueUF = 37000.00;
                String stolenCar = "Valor comercial";
                String workshopType = "Oficial de la marca";
                // Creamos planes de prueba
                QuotationPlanDto plan1 = new QuotationPlanDto("TRACTOR045678987", "TRACTOR045678987",
                                "Tractor Seguros Automotriz", "Plan protector de auto", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(24.86),
                                11, BigDecimal.valueOf(24.86 / 11.0),
                                BigDecimal.valueOf(24.86 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 3,
                                "Deducible 3 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan1, "70%", "800 UF", "90", "3");
                QuotationPlanDto plan2 = new QuotationPlanDto("TRACTOR123456789", "TRACTOR123456789",
                                "Tractor Seguros Automotriz", "Seguro auto completo", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(22.72),
                                11, BigDecimal.valueOf(22.72 / 11.0),
                                BigDecimal.valueOf(22.72 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 5,
                                "Deducible 5 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan2, "80%", "1200 UF", "120", "4");
                QuotationPlanDto plan3 = new QuotationPlanDto("TRACTOR987654321", "TRACTOR987654321",
                                "Tractor Seguros Automotriz", "Plan seguro auto asegurado", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(27.81),
                                11, BigDecimal.valueOf(27.81 / 11.0),
                                BigDecimal.valueOf(27.81 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 0,
                                "Deducible 0 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan3, "60%", "1500 UF", "90", "4");
                QuotationPlanDto plan4 = new QuotationPlanDto("TRACTOR12975678953", "TRACTOR12975678953",
                                "Tractor Seguros Automotriz", "Seguro auto premium", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(20.12),
                                11, BigDecimal.valueOf(20.12 / 11.0),
                                BigDecimal.valueOf(20.12 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 10,
                                "Deducible 10 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan4, "75%", "900 UF", "120", "3");
                list.add(plan1);
                list.add(plan2);
                list.add(plan3);
                list.add(plan4);
                return list;
        }

        public List<QuotationPlanDto> planList2() {
                List<QuotationPlanDto> list = new ArrayList<>();
                double valueUF = 37000.00;
                String stolenCar = "Valor comercial";
                String workshopType = "Oficial de la marca";
                // Creamos planes de prueba
                QuotationPlanDto plan1 = new QuotationPlanDto("SEGUROSALAMEDA045678987", "SEGUROSALAMEDA045678987",
                                "Seguros Alameda", "Asistencia en viaje", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(23.55),
                                11, BigDecimal.valueOf(23.55 / 11.0),
                                BigDecimal.valueOf(23.55 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 5,
                                "Deducible 5 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan1, "70%", "1200 UF", "90", "3");
                QuotationPlanDto plan2 = new QuotationPlanDto("SEGUROSALAMEDA123456789", "SEGUROSALAMEDA123456789",
                                "Seguros Alameda", "Tu trasporte asegurado", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(27.01),
                                11, BigDecimal.valueOf(27.01 / 11.0),
                                BigDecimal.valueOf(27.01 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 3,
                                "Deducible 3 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan2, "80%", "800 UF", "120", "4");
                list.add(plan1);
                list.add(plan2);
                return list;
        }

        public List<QuotationPlanDto> planList3() {
                List<QuotationPlanDto> list = new ArrayList<>();
                double valueUF = 37000.00;
                String stolenCar = "Valor comercial";
                String workshopType = "Oficial de la marca";
                // Creamos planes de prueba
                QuotationPlanDto plan1 = new QuotationPlanDto("LOSALAMOS045678987", "LOSALAMOS045678987",
                                "Los Alamos Seguros Automotriz", "Proteción ultra automóvil",
                                BigDecimal.valueOf(valueUF), BigDecimal.valueOf(22.03),
                                11, BigDecimal.valueOf(22.03 / 11.0),
                                BigDecimal.valueOf(22.03 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 3,
                                "Deducible 3 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan1, "65%", "1500 uf", "180", "3");
                QuotationPlanDto plan2 = new QuotationPlanDto("LOSALAMOS123456789", "LOSALAMOS123456789",
                                "Los Alamos Seguros Automotriz", "Plan de automóvil asegurado",
                                BigDecimal.valueOf(valueUF), BigDecimal.valueOf(21.41),
                                11, BigDecimal.valueOf(21.41 / 11.0),
                                BigDecimal.valueOf(21.41 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 3,
                                "Deducible 3 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan2, "75%", "1000 UF", "120", "4");
                QuotationPlanDto plan3 = new QuotationPlanDto("LOSALAMOS987654321", "LOSALAMOS987654321",
                                "Los Alamos Seguros Automotriz", "Seguro MAX automóvil", BigDecimal.valueOf(valueUF),
                                BigDecimal.valueOf(23.38),
                                11, BigDecimal.valueOf(23.38 / 11.0),
                                BigDecimal.valueOf(23.38 / 11.0).multiply(BigDecimal.valueOf(valueUF)), 5,
                                "Deducible 5 UF",
                                BigDecimal.ZERO, stolenCar, "", "", workshopType, null, "", "", null, null, "", "",
                                null, "", null,
                                null, "", "",
                                "", new HashSet<>());
                this.adjustTestPlan(plan3, "75%", "1200 UF", "90", "3");
                list.add(plan1);
                list.add(plan2);
                list.add(plan3);
                return list;
        }

        public void adjustTestPlan(QuotationPlanDto testPlan, String lossPercentage, String thirdPartyUF,
                        String daysReplacement,
                        String yearsRenewal) {
                // Adjust data
                String totalLoss = "Valor comercial en caso de daños mayores al " + lossPercentage + " del valor";
                String damageThirdParty = "Hasta " + thirdPartyUF + " entre daños emergentes, morales y lucro cesante";
                // Update plan
                testPlan.setTotalLoss(totalLoss);
                testPlan.setDamageThirdParty(damageThirdParty);
        }

        // Creación de un cotizador para los flujos: "Iniciando" o "Cotizando"
        public QuoterModel createQuoteStructure(QuoterOwnerModel quoterOwner, QuoterCarModel quoterCar,
                        QuoterPurchaserModel quoterPurchaser, String quoterStatus, LocalDateTime currentDateTime) {
                // Estructura de los otros objetos del cotizador (vacíos por el momento)
                QuoterPlanModel quoterPlan = new QuoterPlanModel("", "", "", BigDecimal.ZERO, BigDecimal.ZERO, 0,
                                BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, "", "",
                                DataHelper.deprecatedDate(), "", 0);
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
