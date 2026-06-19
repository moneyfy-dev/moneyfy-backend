package com.referidos.app.segurosref.integrations.bci.docs;

import java.util.HashSet;
import java.util.Set;

import com.referidos.app.segurosref.dtos.quotation.QuotationPlanCoverDto;

public class BCIDocsHelper {

    public static Set<QuotationPlanCoverDto> buildCoveragesForSolucionMovil2(Integer deductibleValue) {
        Set<QuotationPlanCoverDto> coverages = new HashSet<>();
        int idCount = 1;
        
        // Determinación de texto de deducible para la descripción
        String deductStr = (deductibleValue == null || deductibleValue == 0) ? "Sin Deducible" : "Deducible UF " + deductibleValue + ",0";
        String roboAccDeduct = "Deducible UF 2,5";
        String actosMalDeduct = (deductibleValue == null || deductibleValue == 0) ? "Deducible UF 2,5" : deductStr;

        coverages.add(new QuotationPlanCoverDto(idCount++, "Daños materiales", deductStr, "POL 1 2013 0214", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Robo, hurto o uso no autorizado", deductStr, "POL 1 2013 0214", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Robo de Accesorios", roboAccDeduct, "CAD 1 2013 0335", "Máximo de UF 40"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Responsabilidad civil daño emergente", "Independiente", "POL 1 2013 0214", "UF 500"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Responsabilidad civil daño moral", "Independiente", "POL 1 2013 0214", "UF 500"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Responsabilidad civil lucro cesante", "Independiente", "POL 1 2013 0214", "UF 500"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Huelga y terrorismo", deductStr, "CAD 1 2013 0336", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Actos maliciosos", actosMalDeduct, "CAD 1 2013 0337", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Granizo y Riesgos de la naturaleza", deductStr, "CAD 1 2013 0339 y CAD 1 2013 0338", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Daños materiales por sismo", deductStr, "CAD 1 2013 0340", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Indemnización 0 Km por 12 meses", "Condición Particular", "Cond. Particular", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Asistencia al vehículo", "Condición Particular", "POL 1 2014 0303", "Condición Particular"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "RC por la carga", "Incluido RC", "CAD 1 2013 0345", "Incluido RC"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Daños Materiales por Propia Carga", "Valor Comercial", "CAD 1 2013 0342", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Daños materiales por conductor dependiente", "Valor Comercial", "CAD 1 2013 0346", "Valor Comercial"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Daños a terceros por conductor dependiente", "Incluido RC", "CAD 1 2013 0347", "Incluido RC"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Muerte Accidental Plan A", "Muerte Accidental", "POL 3 2013 0366", "UF 500"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Incapacidad Total Plan B", "Incapacidad Total", "POL 3 2013 0366", "-"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Defensa penal y constitución de fianzas", "Defensa penal", "CAD 1 2013 0344", "UF 150"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Asignación de Taller", "Condición Particular", "Cond. Particular", "Condición Particular"));
        coverages.add(new QuotationPlanCoverDto(idCount++, "Vehículo de reemplazo 30 días", "Condición Particular", "Cond. Particular", "Condición Particular"));

        return coverages;
    }

    public static String getWorkshopType() {
        return "Multimarca";
    }

    public static String getStolenVehicle() {
        return "Valor comercial";
    }

    public static String getTotalLoss() {
        return "Valor comercial";
    }

    public static String getDamageThirdParty() {
        return "Hasta 500 UF entre daño emergente, moral y lucro cesante";
    }
}
