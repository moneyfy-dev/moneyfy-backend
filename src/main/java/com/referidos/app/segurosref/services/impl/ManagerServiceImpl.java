package com.referidos.app.segurosref.services.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.dtos.manager.DashboardQuoteDto;
import com.referidos.app.segurosref.dtos.manager.DashboardResponseDto;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.services.ManagerService;

@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final UserRepository userRepository;

    private final TransactionRepository transactionRepository;

    @Override
    public DashboardResponseDto getQuotesDashboard() {
        // 1. Obtener todos los usuarios
        List<UserModel> users = userRepository.findAll();
        List<DashboardQuoteDto> dashboardQuotes = new ArrayList<>();

        // 2. Extraer todos los IDs de cotizaciones que tienen estado avanzado
        // Estados avanzados que implican transaccion: PENDIENTE, APROBADO, LIBERADO,
        // etc.
        // O simplemente extraemos todos los IDs de cotizacion en general para hacer
        // match.
        List<String> allQuoterIds = new ArrayList<>();
        for (UserModel user : users) {
            if (user.getQuoters() != null) {
                for (QuoterModel quoter : user.getQuoters()) {
                    allQuoterIds.add(quoter.getQuoterId());
                }
            }
        }

        // 3. Consultar transacciones masivamente y pasarlas a Diccionario (O(1))
        Map<String, TransactionModel> transactionMap = transactionRepository.findByQuoterIdIn(allQuoterIds)
                .stream()
                .collect(Collectors.toMap(TransactionModel::getQuoterId, t -> t, (t1, t2) -> t1));

        // Formateador de fechas
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        // 4. Mapeo a DTO controlando nulos
        for (UserModel user : users) {
            if (user.getQuoters() == null)
                continue;

            for (QuoterModel quoter : user.getQuoters()) {
                DashboardQuoteDto dto = new DashboardQuoteDto();

                // Datos obligatorios
                dto.setIdUser(user.getUserId());
                dto.setUserFullname(user.getPersonalData() != null
                        ? user.getPersonalData().getName() + " " + user.getPersonalData().getSurname()
                        : "N/A");
                dto.setUserEmail(user.getPersonalData() != null ? user.getPersonalData().getEmail() : "N/A");
                dto.setQuoteId(quoter.getQuoterId());
                dto.setQuoteStatus(quoter.getQuoterStatus() != null ? quoter.getQuoterStatus() : "N/A");
                dto.setQuoterCarPpu(quoter.getQuoterCarData() != null && quoter.getQuoterCarData().getPpu() != null
                        ? quoter.getQuoterCarData().getPpu()
                        : "N/A");
                dto.setInicialDate(quoter.getCreatedDate() != null ? quoter.getCreatedDate().format(formatter) : "N/A");

                // Datos opcionales: Owner
                if (quoter.getQuoterOwnerData() != null) {
                    dto.setQuoterOwnerPersonalId(quoter.getQuoterOwnerData().getPersonalId() != null
                            ? quoter.getQuoterOwnerData().getPersonalId()
                            : "N/A");
                    dto.setQuoterOwnerFullname(quoter.getQuoterOwnerData().getName() + " "
                            + quoter.getQuoterOwnerData().getPaternalSurname());
                } else {
                    dto.setQuoterOwnerPersonalId("N/A");
                    dto.setQuoterOwnerFullname("N/A");
                }

                // Datos opcionales: Car
                if (quoter.getQuoterCarData() != null) {
                    dto.setQuoterCarBrand(
                            quoter.getQuoterCarData().getBrand() != null ? quoter.getQuoterCarData().getBrand()
                                    : "N/A");
                    dto.setQuoterCarModel(
                            quoter.getQuoterCarData().getModel() != null ? quoter.getQuoterCarData().getModel()
                                    : "N/A");
                    dto.setQuoterCarYear(
                            quoter.getQuoterCarData().getYear() != null ? quoter.getQuoterCarData().getYear() : "N/A");
                    dto.setQuoterCarType(
                            quoter.getQuoterCarData().getType() != null ? quoter.getQuoterCarData().getType() : "N/A");
                } else {
                    dto.setQuoterCarBrand("N/A");
                    dto.setQuoterCarModel("N/A");
                    dto.setQuoterCarYear("N/A");
                    dto.setQuoterCarType("N/A");
                }

                // Datos opcionales: Buyer
                if (quoter.getQuoterPurchaserData() != null) {
                    dto.setQuoterBuyerPersonalId(quoter.getQuoterPurchaserData().getPersonalId() != null
                            ? quoter.getQuoterPurchaserData().getPersonalId()
                            : "N/A");
                    dto.setQuoterBuyerFullname(quoter.getQuoterPurchaserData().getName() + " "
                            + quoter.getQuoterPurchaserData().getPaternalSurname());
                    dto.setQuoterBuyerEmail(quoter.getQuoterPurchaserData().getEmail() != null
                            ? quoter.getQuoterPurchaserData().getEmail()
                            : "N/A");
                    dto.setQuoterBuyerPhone(quoter.getQuoterPurchaserData().getPhone() != null
                            ? quoter.getQuoterPurchaserData().getPhone()
                            : "N/A");
                } else {
                    dto.setQuoterBuyerPersonalId("N/A");
                    dto.setQuoterBuyerFullname("N/A");
                    dto.setQuoterBuyerEmail("N/A");
                    dto.setQuoterBuyerPhone("N/A");
                }

                // Datos opcionales: Plan
                if (quoter.getQuoterPlanData() != null) {
                    dto.setQuoterPlanInsurer(
                            quoter.getQuoterPlanData().getInsurer() != null ? quoter.getQuoterPlanData().getInsurer()
                                    : "N/A");
                    dto.setQuoterPlanName(
                            quoter.getQuoterPlanData().getPlanName() != null ? quoter.getQuoterPlanData().getPlanName()
                                    : "N/A");
                    dto.setQuoterPlanUf(quoter.getQuoterPlanData().getValueUF() != null
                            ? quoter.getQuoterPlanData().getValueUF().toString()
                            : "0.0");
                    dto.setQuoterPlanMonthlyPriceUF(quoter.getQuoterPlanData().getMonthlyPriceUF() != null
                            ? quoter.getQuoterPlanData().getMonthlyPriceUF()
                            : BigDecimal.ZERO);
                    dto.setQuoterPlanMonthlyPrice(quoter.getQuoterPlanData().getMonthlyPrice() != null
                            ? quoter.getQuoterPlanData().getMonthlyPrice()
                            : BigDecimal.ZERO);
                    dto.setQuoterPlanMonths(12); // Assuming 12 months for plans, or extract if available
                } else {
                    dto.setQuoterPlanInsurer("N/A");
                    dto.setQuoterPlanName("N/A");
                    dto.setQuoterPlanUf("0.0");
                    dto.setQuoterPlanMonthlyPriceUF(BigDecimal.ZERO);
                    dto.setQuoterPlanMonthlyPrice(BigDecimal.ZERO);
                    dto.setQuoterPlanMonths(0);
                }

                // Datos opcionales: Address
                if (quoter.getQuoterAddressData() != null) {
                    dto.setQuoterAddressStreet(quoter.getQuoterAddressData().getStreet() != null
                            ? quoter.getQuoterAddressData().getStreet()
                            : "N/A");
                    dto.setQuoterAddressStreetNumber(quoter.getQuoterAddressData().getStreetNumber() != null
                            ? quoter.getQuoterAddressData().getStreetNumber()
                            : "N/A");
                } else {
                    dto.setQuoterAddressStreet("N/A");
                    dto.setQuoterAddressStreetNumber("N/A");
                }

                // Datos opcionales: Transaction
                TransactionModel trans = transactionMap.get(quoter.getQuoterId());
                if (trans != null) {
                    dto.setTransactionTotalCommission(trans.getCommissionTotal());
                    dto.setTransactionTotalScope(trans.getCommissionScope());
                } else {
                    dto.setTransactionTotalCommission(0);
                    dto.setTransactionTotalScope(0);
                }

                dashboardQuotes.add(dto);
            }
        }

        return new DashboardResponseDto("Cotizaciones recuperadas exitosamente", 200, dashboardQuotes);
    }
}
