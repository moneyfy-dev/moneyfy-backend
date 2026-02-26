package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.dtos.TestPlanDto;
import com.referidos.app.segurosref.dtos.commission.CommissionAccountDto;
import com.referidos.app.segurosref.dtos.commission.CommissionConflictDto;
import com.referidos.app.segurosref.dtos.commission.CommissionDataDto;
import com.referidos.app.segurosref.dtos.commission.CommissionPaymentDto;
import com.referidos.app.segurosref.dtos.commission.CommissionReportDto;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandInsurerModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.models.LogModel;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.QuoterAddressModel;
import com.referidos.app.segurosref.models.QuoterCarModel;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.QuoterOwnerModel;
import com.referidos.app.segurosref.models.QuoterPaymentModel;
import com.referidos.app.segurosref.models.QuoterPlanModel;
import com.referidos.app.segurosref.models.QuoterPurchaserModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.repositories.LogRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.CommissionReportRequest;

// Se inyecta como repositorio en el servicio de "Quoter", pero, realizando funcionalidades de servicio
@Component 
public class QuoterHelper {

    @Value(value="${commission.cutoff-date}")
    private int commissionCutoffDate;

    @Value(value="${commission.payment-date}")
    private int commissionPaymentDate;

    // LÓGICA PARA ACTUALIZAR LA DATA MARCA/MODELOS EN LA BASE DE DATOS
    public List<BrandModel> updateVehicleBrands(BrandRepository brandRepository, List<BrandModel> brands) {
        // Recuperamos todas las marcas actuales registradas en la base de datos
        List<BrandModel> brandsDB = brandRepository.findAll();
        // Vamos revisando las marcas de vehículos comparando con la base de datos.
        // 1. Si existe la marca, se revisa si tiene los ids de la marca de las aseguradoras, y si no existe la marca se genera objeto completo.
        // 2. Si existe la marca, hay que revisar si tiene los modelos.
        // 3. Si existe el modelo, se revisa si tiene los ids de los modelos de las aseguradoras, y si no existe el modelo se genera el objeto completo.
        for(BrandModel brandModel : brands) {
            String brandName = (!DataHelper.isNull(brandModel.getBrand())) ? brandModel.getBrand().trim().toUpperCase() : null;
            List<BrandInsurerModel> insurersBrandId = brandModel.getInsurersId();
            List<BrandDataModel> models = brandModel.getModels();
            if(brandName == null || insurersBrandId == null || models == null) {
                return null; // "data incorrecta"
            }
            boolean existBrandDB = false;
            for(BrandModel brandDB : brandsDB) {
                if(brandName.equals(brandDB.getBrand())) {
                    // EXISTE LA MARCA, ahora verificamos si existen los ids de la marca de las aseguradoras en la BD
                    existBrandDB = true;
                    for(BrandInsurerModel insurerBrandId : insurersBrandId) {
                        String insurerNameForBrandId = (!DataHelper.isNull(insurerBrandId.getName())) ? insurerBrandId.getName().trim() : null;
                        insurerBrandId.setName(insurerNameForBrandId);
                        if(insurerNameForBrandId == null) {
                            return null; // "el nombre de la aseguradora para el id de la marca, no es válido"
                        }
                        boolean existsInsurerBrandId = false;
                        for(BrandInsurerModel insurerBrandIdDB : brandDB.getInsurersId()) {
                            String insurerNameForBrandIdDB = insurerBrandIdDB.getName();
                            if(insurerNameForBrandId.equals(insurerNameForBrandIdDB)) {
                                existsInsurerBrandId = true;
                                break;
                            }
                        }
                        if(!existsInsurerBrandId) { // Si no existe el id de la marca de la aseguradora en la base de datos, se agrega el objeto completo.
                            brandDB.addInsurerBrandId(insurerBrandId);
                        }
                    }
                    // EXISTE LA MARCA, y nos aseguramos de saber que los ids de la marca de las aseguradoras este en nuestra bd.
                    // Ahora, queremos saber si existen los modelos de la marca en la BD
                    for(BrandDataModel dataModel : models) {
                        String modelName = (!DataHelper.isNull(dataModel.getModel())) ? dataModel.getModel().trim().toUpperCase() : null;
                        if(modelName == null) {
                            return null; // "el nombre del modelo de la marca, no es válido"
                        }
                        boolean existModelName = false;
                        for(BrandDataModel dataModelDB : brandDB.getModels()) {
                            String modelNameDB = dataModelDB.getModel();
                            if(modelName.equals(modelNameDB)) {
                                // EXISTE EL MODELO EN LA MARCA, ahora hay que verificar si están los ids del modelo de las aseguradoras en la BD.
                                existModelName = true;
                                for(BrandInsurerModel insurerModelId : dataModel.getInsurersId()) {
                                    String insurerNameForModelId = (!DataHelper.isNull(insurerModelId.getName())) ? insurerModelId.getName().trim() : null;
                                    insurerModelId.setName(insurerNameForModelId);
                                    if(insurerNameForModelId == null) {
                                        return null; // "el nombre de la aseguradora para el id del modelo, no es válido"
                                    }
                                    boolean existsInsurerModelId = false;
                                    for(BrandInsurerModel insurerModelIdDB : dataModelDB.getInsurersId()) {
                                        String insurerNameForModelIdDB = insurerModelIdDB.getName();
                                        if(insurerNameForModelId.equals(insurerNameForModelIdDB)) {
                                            existsInsurerModelId = true;
                                            break; // Ya se verificó que existe el id del modelo de la aseguradora en la BD.
                                        }
                                    }
                                    if(!existsInsurerModelId) { // Si no existe el id del modelo de la aseguradora en la base de datos, se agrega el objeto completo.
                                        // Existe el modelo, solo que no existe el id de ese modelo, de la aseguradora.
                                        dataModelDB.addInsurerModelId(insurerModelId);
                                    }
                                }
                                // Como existe el modelo y ya se verifico que existen los ids de modelo de las asegurados, se sale del for
                                break;
                            }
                        }
                        if(!existModelName) { // No existe el modelo de la marca encontrada en la base de datos, por lo tanto, se agrega el objeto completo
                            dataModel.setModelId(new ObjectId()); // Se le debe crear un id al modelo, para la aplicación, porque es un nuevo modelo que es agregado.
                            dataModel.setModel(modelName);
                            for(BrandInsurerModel insurerModelId : dataModel.getInsurersId()) { // Nos aseguramos que los nombres de las aseguradoras estén sin espacios
                                String insurerNameForModelId = (!DataHelper.isNull(insurerModelId.getName())) ? insurerModelId.getName().trim() : null;
                                if(insurerNameForModelId == null) {
                                    return null; // "el nombre de la aseguradora para el id del modelo, no es válido"
                                }
                                insurerModelId.setName(insurerNameForModelId);
                            }
                            brandDB.addModel(dataModel);
                        }
                    }
                    // Como sabemos que existe la marca y ya se actualizo lo que se debía de actualizar, se sale del for
                    break;
                }
            }
            // Si no se encontró la marca del vehículo, con el objeto proporcionado, se agrega el objeto a la estructura
            // actual de la base de datos, pero, a cada uno de los modelos de la marca se le genera un id, antes de agregar
            // el objeto, ya que, como es primera vez del ingreso de la marca, los modelos no tienen id en la aplicación
            if(!existBrandDB) {
                // Además, si se registra la marca, se tiene que mantener un único formato: marca/modelo, en mayúsculas y
                // sin saltos de líneas extra, y los nombres de las aseguradoras: sin saltos extras.
                for(BrandInsurerModel insurerBrandId : insurersBrandId) {
                    String insurerNameForBrandId = (!DataHelper.isNull(insurerBrandId.getName())) ? insurerBrandId.getName().trim() : null;
                    if(insurerNameForBrandId == null) {
                        return null; // "el nombre de la aseguradora para el id de la marca, no es válido"
                    }
                    insurerBrandId.setName(insurerNameForBrandId);
                }
                for(BrandDataModel model : models) {
                    String modelName = (!DataHelper.isNull(model.getModel())) ? model.getModel().trim().toUpperCase() : null;
                    if(modelName == null) {
                        return null; // "el nombre del modelo de la marca, no es válido"
                    }
                    model.setModelId(new ObjectId());
                    model.setModel(modelName);
                    for(BrandInsurerModel insurerModelId : model.getInsurersId()) {
                        String insurerNameForModelId = (!DataHelper.isNull(insurerModelId.getName())) ? insurerModelId.getName().trim() : null;
                        if(insurerNameForModelId == null) {
                            return null; // "el nombre de la aseguradora para el id del modelo, no es válido"
                        }
                        insurerModelId.setName(insurerNameForModelId);
                    }
                }
                brandsDB.add(brandModel);
            }
        } // FIN DEL FOR QUE ESTÁ VERIFICANDO SI EXISTE LA MARCA
        return brandsDB;
    }

    // FUNCIONES DE APOYO - DATOS DE PRUEBA - LOGICA
    public List<QuoterCarModel> vehicleList() {
        List<QuoterCarModel> list = new ArrayList<>();

        // Generar la info del auto buscado por el cotizador
        QuoterCarModel car1 = new QuoterCarModel("11AA22", "Chevrolet", "Captiva", "2021",
            "Plateado", "AA1234BB5678", "FAEBDC892354A1B3C6", "SAIC-GM-Wuling");

        QuoterCarModel car2 = new QuoterCarModel("AB1234", "Toyota", "Corolla", "2019",
            "Blanco", "123ABC456DEF", "789GHI012JKL", "Toyota Motor Corporation");

        QuoterCarModel car3 = new QuoterCarModel("DE5678", "BMW", "3 Series", "2022",
            "Negro", "456DEF789GHI", "012JKL345MNO", "BMW AG");

        QuoterCarModel car4 = new QuoterCarModel("GH9012", "Ford", "Fiesta", "2018",
            "Azul", "789GHI012JKL", "345MNO678PQR", "Ford Motor Company");

        QuoterCarModel car5 = new QuoterCarModel("JK34DL", "Mercedes-Benz", "C-Class", "2021",
            "Gris", "012JKL345MNO", "678PQR901STU", "Mercedes-Benz AG");

        list.add(car1);
        list.add(car2);
        list.add(car3);
        list.add(car4);
        list.add(car5);

        return list;
    }

    public List<QuoterOwnerModel> ownerList() {
        List<QuoterOwnerModel> list = new ArrayList<>();

        QuoterOwnerModel owner1 = new QuoterOwnerModel("11.111.111-1", "Pepe",
                "Rodriguez", "Fuentes");
        
        QuoterOwnerModel owner2 = new QuoterOwnerModel("22.222.222-2", "Maria",
                "Fuentes", "Silva");

        QuoterOwnerModel owner3 = new QuoterOwnerModel("33.333.333-3", "Camila",
                "Avellaneda", "González");

        QuoterOwnerModel owner4 = new QuoterOwnerModel("44.444.444-4", "Octaquio",
                "Alfonso", "Riquelme");

        QuoterOwnerModel owner5 = new QuoterOwnerModel("55.555.555-5", "Valentina",
                "Carrasco", "Zamora");

        list.add(owner1);
        list.add(owner2);
        list.add(owner3);
        list.add(owner4);
        list.add(owner5);

        return list;
    }

    public List<TestPlanDto> planList1() {
        List<TestPlanDto> list = new ArrayList<>();
        double valueUF = 37000.00;
        String stolenCar = "Valor comercial";
        String workshopType = "Oficial de la marca";

        TestPlanDto plan1 = new TestPlanDto("TRACTOR045678987", "Tractor Seguros Automotriz",
                "Plan protector de auto", valueUF, 24.86, 11, 24.86/11,
                (24.86/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan1, "70%", "800 UF", "90", "3");

        TestPlanDto plan2 = new TestPlanDto("TRACTOR123456789", "Tractor Seguros Automotriz",
                "Seguro auto completo", valueUF, 22.72, 11, 22.72/11,
                (22.72/11)*valueUF, 5, "Deducible 5 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan2, "80%", "1200 UF", "120", "4");

        TestPlanDto plan3 = new TestPlanDto("TRACTOR987654321", "Tractor Seguros Automotriz",
                "Plan seguro auto asegurado", valueUF, 27.81, 11, 27.81/11,
                (27.81/11)*valueUF, 0, "Deducible 0 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan3, "60%", "1500 UF", "90", "4");

        TestPlanDto plan4 = new TestPlanDto("TRACTOR12975678953", "Tractor Seguros Automotriz",
                "Seguro auto premium", valueUF, 20.12, 11, 20.12/11,
                (20.12/11)*valueUF, 10, "Deducible 10 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan4, "75%", "900 UF", "120", "3");

        list.add(plan1);
        list.add(plan2);
        list.add(plan3);
        list.add(plan4);

        return list;
    }

    public List<TestPlanDto> planList2() {
        List<TestPlanDto> list = new ArrayList<>();
        double valueUF = 37000.00;
        String stolenCar = "Valor comercial";
        String workshopType = "Oficial de la marca";

        TestPlanDto plan1 = new TestPlanDto("SEGUROSALAMEDA045678987", "Seguros Alameda",
                "Asistencia en viaje", valueUF, 23.55, 11, 23.55/11,
                (23.55/11)*valueUF, 5, "Deducible 5 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan1, "70%", "1200 UF", "90", "3");

        TestPlanDto plan2 = new TestPlanDto("SEGUROSALAMEDA123456789", "Seguros Alameda",
                "Tu trasporte asegurado", valueUF, 27.01, 11, 27.01/11,
                (27.01/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan2, "80%", "800 UF", "120", "4");

        list.add(plan1);
        list.add(plan2);

        return list;
    }

    public List<TestPlanDto> planList3() {
        List<TestPlanDto> list = new ArrayList<>();
        double valueUF = 37000.00;
        String stolenCar = "Valor comercial";
        String workshopType = "Oficial de la marca";

        TestPlanDto plan1 = new TestPlanDto("LOSALAMOS045678987", "Los Alamos Seguros Automotriz",
                "Proteción ultra automóvil", valueUF, 22.03, 11, 22.03/11,
                (22.03/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan1, "65%", "1500 uf", "180", "3");

        TestPlanDto plan2 = new TestPlanDto("LOSALAMOS123456789", "Los Alamos Seguros Automotriz",
                "Plan de automóvil asegurado", valueUF, 21.41, 11, 21.41/11,
                (21.41/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan2, "75%", "1000 UF", "120", "4");

        TestPlanDto plan3 = new TestPlanDto("LOSALAMOS987654321", "Los Alamos Seguros Automotriz",
                "Seguro MAX automóvil", valueUF, 23.38, 11, 23.38/11,
                (23.38/11)*valueUF, 5, "Deducible 5 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan3, "75%", "1200 UF", "90", "3");

        list.add(plan1);
        list.add(plan2);
        list.add(plan3);

        return list;
    }

    public void adjustTestPlan(TestPlanDto testPlan, String lossPercentage, String thirdPartyUF, String daysReplacement,
            String yearsRenewal) {
        // Adjust data
        String totalLoss = "Valor comercial en caso de daños mayores al " + lossPercentage + " del valor";
        String damageThirdParty = "Hasta " + thirdPartyUF + " entre daños emergentes, morales y lucro cesante";
        String detailReplacement = "Limitado hasta " + daysReplacement + " días hábiles, para el reemplazo del vehículo";
        String detailRenewal = "Luego de " + yearsRenewal + " año/s de haber comprado, se habilita la renovación del vehículo";
        // Update plan
        testPlan.setTotalLoss(totalLoss);
        testPlan.setDamageThirdParty(damageThirdParty);
        testPlan.addDetail(detailReplacement).add(detailRenewal);
    }

    public List<QuoterCarModel> vehiclesByOwnerId(String ownerId) {
        List<QuoterCarModel> vehicleList = new ArrayList<>();
        
        // ownerId ya viene validado, se le asignan autos directamente
        // Número random entre 2 y 3, para agregar esa cantidad de vehículos al cotizador
        // Obtener número random entre 2 rangos, para eso se tiene que conocer el rango (limiteSuperior-limiteInferior+1)
        // Al obtener el número del rango, se obtiene un número entre ese rango y se le suma la parte inferior
        int vehicleNumbers = ((int) ((Math.random()*(3-2+1))+2)); // Ejemplo: el rango entre 2-3 es 2, porque se puede obtener solo 2 o 3, entonces se obtiene el número que puede ser 0 o 1 y se le agrega el limite inferior (2), y se obtiene el número entre rango 2 o 3.
        int[] vehicleIndexes = new int[vehicleNumbers];
        // Nos aseguramos que los valores de los índices no se repitan para que se generen vehículos distintos
        boolean repeatedVehicle;

        do {
            repeatedVehicle = false;    
            for(int i=0; i<vehicleNumbers; i++) {
                vehicleIndexes[i] = ((int) (Math.random()*5)); // Número entre 0 y 4, porque existen 5 registros de autos de prueba
            }

            // Se generarón los indexes de los vehículos de prueba, ahora se verifica que no se repitan los índices
            // para no generar autos copiados al cotizador
            for(int i=0; i<vehicleNumbers; i++) {
                for(int j=i+1; j<vehicleNumbers; j++) {
                    if(vehicleIndexes[i] == vehicleIndexes[j]) {
                        repeatedVehicle=true;
                        break;
                    }
                }
                if(repeatedVehicle) {
                    break;
                }
            }
        } while(repeatedVehicle);

        // Los índices de vehículos son distintos
        List<QuoterCarModel> vehicleData = this.vehicleList(); // Existen 5 registros de autos de prueba
        for(int i=0; i<vehicleNumbers; i++) {
            vehicleList.add(vehicleData.get(vehicleIndexes[i]));
        }

        return vehicleList;
    }

    // Creación de un cotizador para los flujos: "Iniciando" o "Cotizando"
    public QuoterModel createQuoteStructure(QuoterOwnerModel quoterOwner, QuoterCarModel quoterCar,
            QuoterPurchaserModel quoterPurchaser, String quoterStatus, LocalDateTime currentDateTime) {
        // Estructura de los otros objetos del cotizador (vacíos por el momento)
        QuoterPlanModel quoterPlan = new QuoterPlanModel("", "", "", 0.0, 0.0, 0, 0.0, 0.0, 0, 0.0);
        QuoterAddressModel quoterAddress = new QuoterAddressModel("", "", "");
        QuoterPaymentModel quoterPayment = new QuoterPaymentModel("", "", "", "");
        return new QuoterModel(new ObjectId(), quoterStatus, quoterOwner, quoterCar, quoterPurchaser, quoterPlan,
                quoterAddress, quoterPayment, currentDateTime, currentDateTime);
    }

    // Obtener la fecha de corte y la fecha de pago, para generar el informe de retiro de comisiones
    public Object[] getCutOffDateAndPaymentDate(LocalDateTime currentDateTime, CommissionReportRequest withdrawalRequest) {
        int currentYear = currentDateTime.getYear();
        int currentMonth = currentDateTime.getMonthValue();
        int currentDayOfMonth = currentDateTime.getDayOfMonth();
        
        // POR AHORA LOS DÍAS DE CORTE SON LOS 5 Y LOS DÍA DE PAGO SON LOS 10
        // Obtenemos la fecha máximo de corte por defecto, en caso de que no se especifique en el cuerpo de la solicitud,
        // entendiendo que se obtienen todas las transacciones aprobadas hasta los días de corte. Por lo tanto, verificamos
        // si el día del mes de la fecha actual, es mayor al día de corte, si es así, la máxima fecha de corte sería de
        // este mes en el día de corte, pero si no, si el día del mes de la fecha actual, es igual o menor al día de corte,
        // la máxima fecha de corte sería del mes anterior, en el día de corte, porque como aún no se ha pasado el día de
        // corte, se pueden seguir acumulando ventas de planes. Ahora, en caso de que se haya específicado la fecha de
        // corte, solo se puede permitir realizar un informe como máximo, hasta la próxima fecha de corte, ejemplo: si el
        // día de corte son los 15, y hoy estamos a 2025-20-01, la fecha máxima de corte sería para 2025-15-01, pero, como
        // se específico en la solicitud, puede ser hasta 2025-15-02.
        LocalDateTime maxCutOffDate;
        LocalDateTime maxPaymentDate;
        if(currentDayOfMonth <= commissionCutoffDate) { // No ha pasado el día de corte, por lo tanto, la fecha máxima de corte, es del mes anterior en el día de corte
            int maxYearCutoffDate = currentYear;
            int maxMonthCutoffDate = currentMonth;
            if(maxMonthCutoffDate == 1) {
                maxYearCutoffDate -= 1;
                maxMonthCutoffDate = 12;
            } else {
                maxMonthCutoffDate -= 1;
            }
            maxCutOffDate = LocalDateTime.of(maxYearCutoffDate, maxMonthCutoffDate, commissionCutoffDate, 23, 59, 59);
            maxPaymentDate = LocalDateTime.of(maxYearCutoffDate, maxMonthCutoffDate, commissionPaymentDate, 23, 59, 59);
        } else { // Paso el día de corte, por lo tanto, la fecha máxima de corte, es de este mes en el día de corte
            maxCutOffDate = LocalDateTime.of(currentYear, currentMonth, commissionCutoffDate, 23, 59, 59); 
            maxPaymentDate = LocalDateTime.of(currentYear, currentMonth, commissionPaymentDate, 23, 59, 59);
        }

        // En resumen, si no se específica fecha de corte, se obtiene la máxima fecha de corte, que sería la fecha actual
        // con el día de corte, en caso de que se haya pasado el día de corte, o el mes pasado con la fecha de corte de la
        // fecha actual, en caso de que NO se haya pasado la fecha de corte, y en caso contrario la fecha máxima de corte,
        // puede ser hasta 1 mes más del máximo.
        String cutoffDateStr = withdrawalRequest.cutoffDate();
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime cutoffDate = null;
        LocalDateTime paymentDate = null;
        String errorMessage = null;

        if(DataHelper.isNull(cutoffDateStr)) {
            cutoffDate = maxCutOffDate;
            paymentDate = maxPaymentDate;    
        } else {
            try {
                LocalDate dateDelivered = LocalDate.parse(cutoffDateStr, formatterDate);
                LocalDate maxCutOffDateLocalDate = maxCutOffDate.toLocalDate().plusMonths(1);
                int yearDelivered = dateDelivered.getYear();
                int monthDelivered = dateDelivered.getMonthValue();
                int dayDelivered = dateDelivered.getDayOfMonth();
                if(dateDelivered.isAfter(maxCutOffDateLocalDate)) {
                    errorMessage = "la fecha de corte no puede sobrepasar la fecha de corte máxima, de la cual, se puede consultar: " + maxCutOffDateLocalDate;
                } else if(dayDelivered != commissionCutoffDate) {
                    errorMessage = "la fecha de corte, debe considerar el día de corte: " + commissionCutoffDate;
                }
                cutoffDate = LocalDateTime.of(yearDelivered, monthDelivered, commissionCutoffDate, 23, 59, 59);
                paymentDate = LocalDateTime.of(yearDelivered, monthDelivered, commissionPaymentDate, 23, 59, 59);
            } catch (DateTimeParseException e) {
                errorMessage = "no se pudo obtener la fecha de corte proporcionada, con el formato: YYYY-MM-DD";
            }
        }

        return new Object[] {errorMessage, cutoffDate, paymentDate};
    }


    // Generamos el cuerpo de una nueva transacción
    public TransactionModel generateNovaTransactionStructure(String transactionId, UserModel userC,
            QuoterModel quoterDB, int commissionTotal, String currentStatus, LocalDateTime currentDateTime) {
        String userCId = userC.getUserId();
        TransactionModel novaTransaction = new TransactionModel(transactionId, quoterDB.getQuoterPlanData().getQuoterPlanId(),
                userCId, quoterDB.getQuoterId(), currentStatus, commissionTotal, 1,
                "La comisión está siendo procesada", currentDateTime, currentDateTime, DataHelper.deprecatedDateTime());
        novaTransaction.addCommission(new TransactionComissionModel(userCId, commissionTotal, currentStatus));
        return novaTransaction;
    }

    public void generateCommissionReport(CommissionReportDto commissionReport, LocalDateTime afterDate, String endpoint,
            LocalDateTime currentDateTime, TransactionRepository transactionRepository, UserRepository userRepository,
            LogRepository logRepository) {
        // Buscamos todas las transacciones que esten con el estado "Aprobado" o con el estado "Confirmando" (en caso de
        // que el usuario no haya confirmado su cuenta bancaria para recibir sus comisiones) y que además la transacción,
        // tiene que haberse realizado antes de la fecha que viene después de la fecha de corte.
        List<TransactionModel> transactionsDB = transactionRepository.findAllByApprovalDateBeforeAndStatusProcessing(afterDate);
        int totalPaymentTransactions=0;
        int totalPaymentUsers=0;
        for(TransactionModel transactionDB : transactionsDB) {
            String transactionId = transactionDB.getTransactionId();
            int transactionCommmission = 0;
            for(TransactionComissionModel commissionDB : transactionDB.getCommissions()) {
                if(commissionDB.getCommissionStatus().equals("Liberado")) {
                    // Ya se pago la comisión, pero, hay comisiones que se tienen que pagar en está transacción
                    continue;
                }
                String commissionUserId = commissionDB.getUserId();
                int commissionPayment = commissionDB.getUserCommission();
                // Buscamos si ya existe el usuario en el reporte de comisiones
                boolean existsUserReport = false;
                for(CommissionPaymentDto commissionData : commissionReport.getPayments()) {
                    if(commissionUserId.equals(commissionData.getUserId())) {
                        existsUserReport = true;
                        commissionData.setPayment(commissionData.getPayment() + commissionPayment);
                        commissionData.addCommission(new CommissionDataDto(transactionId, commissionPayment));
                        break;
                    }
                }
                // Si no existe el usuario en el reporte se crea
                if(!existsUserReport) {
                    CommissionPaymentDto novaCommisionData = new CommissionPaymentDto(commissionUserId, "",
                            null, commissionPayment, "");
                    novaCommisionData.addCommission(new CommissionDataDto(transactionId, commissionPayment));
                    commissionReport.addPayment(novaCommisionData);
                }
                // Sumamos las comisiones de la trasacción
                transactionCommmission += commissionPayment;
            }
            totalPaymentTransactions += transactionCommmission;
            commissionReport.addTransactionId(transactionId);
        }
        // Ya se obtuvieron las comisiones del usuario en las transacciones. Ahora, buscamos el usuario para agregar su
        // estado, cuenta bancaria y confirmar el monto que se le debe cancelar
        for(CommissionPaymentDto commissionData : commissionReport.getPayments()) {
            // Sumamos las comisiones ya calculadas desde los usuarios
            totalPaymentUsers += commissionData.getPayment();
            String userId = commissionData.getUserId();
            if(!ObjectId.isValid(userId)) {
                // Generamos log de error en caso de existir conflicto
                String reference = "ID de usuario incorrecto en reporte de comisiones";
                if(!logRepository.existsByTypeAndStatusAndReferenceAndUserId("ERROR", "Grave", reference, userId)) {
                    LogModel logUserIdInvalid = new LogModel(null, "ERROR", reference,
                            endpoint, "Grave", userId, "", "", new HashMap<>(),
                            currentDateTime, currentDateTime);
                    logRepository.save(logUserIdInvalid);
                }
                // Agregamos conflicto existente
                commissionReport.addConflict(new CommissionConflictDto(userId, reference));
                continue;
            }
            // Buscamos el usuario
            Optional<UserModel> optionalUser = userRepository.findById(new ObjectId(userId));
            if(optionalUser.isPresent()) {
                UserModel userDB = optionalUser.get();
                CommissionAccountDto userAccount = null;
                String userStatus = userDB.getPersonalData().getStatus();
                // Buscamos la cuenta bancaria del usuario
                for(AccountModel userAccountDB : userDB.getAccounts()) {
                    if(userAccountDB.isSelected()) {
                        userAccount = new CommissionAccountDto(userAccountDB.getPersonalId(), userAccountDB.getHolderName(),
                                userAccountDB.getEmail(), userAccountDB.getBank(), userAccountDB.getAccountType(),
                                userAccountDB.getAccountNumber());
                    }
                }
                // Agregamos conflictos en dependiendo del los resultados y seteamos los campos de la cuenta del usuario y su estado
                if(userAccount == null){
                    // Generamos log de error en caso de existir conflicto
                    String reference = "Cuenta bancaria de usuario no encontrada";
                    if(!logRepository.existsByTypeAndStatusAndReferenceAndUserId("ERROR", "Grave", reference, userId)) {
                        LogModel logUserAccountNotFound = new LogModel(null, "ERROR", reference, endpoint,
                                "Grave", userId, "", "", new HashMap<>(),
                                currentDateTime, currentDateTime);
                        logUserAccountNotFound.addData("userAccounts", userDB.getAccounts());
                        logRepository.save(logUserAccountNotFound);
                    }
                    // Agregamos conflicto existente
                    commissionReport.addConflict(new CommissionConflictDto(userId, reference));
                }
                if(!userStatus.equals("Activado")) {
                    // Generamos log de error en caso de existir conflicto
                    String reference = "El usuario no se encuentra activado en el reporte de comisiones";
                    if(!logRepository.existsByTypeAndStatusAndReferenceAndUserId("ERROR", "Grave", reference, userId)) {
                        LogModel logUserInactive = new LogModel(null, "ERROR", reference,
                                endpoint, "Grave", userId, "", "", new HashMap<>(),
                                currentDateTime, currentDateTime);
                        logRepository.save(logUserInactive);
                    }
                    // Agregamos conflicto existente
                    commissionReport.addConflict(new CommissionConflictDto(userId, reference));
                }
                commissionData.setAccount(userAccount);
                commissionData.setStatus(userStatus);
            } else {
                // Generamos log de error en caso de existir conflicto
                String reference = "El usuario no fue encontrado en el reporte de comisiones";
                if(!logRepository.existsByTypeAndStatusAndReferenceAndUserId("ERROR", "Grave", reference, userId)) {
                    LogModel logUserNotFound = new LogModel(null, "ERROR", reference,
                            endpoint, "Grave", userId, "", "", new HashMap<>(),
                            currentDateTime, currentDateTime);
                    logRepository.save(logUserNotFound);
                }
                // Agregamos conflicto existente
                commissionReport.addConflict(new CommissionConflictDto(userId, reference));
            }
        }
        commissionReport.setTotalUsers(commissionReport.getPayments().size());
        commissionReport.setTotalPaymentTransactions(totalPaymentTransactions);
        commissionReport.setTotalPaymentUsers(totalPaymentUsers);
        if(totalPaymentTransactions != totalPaymentUsers) {
            // Agregamos conflicto existente
            commissionReport.addConflict(new CommissionConflictDto("", "El total de pago de las transacciones, no es equivalente con las comisiones de los usuarios."));
        }
    }

    // Flujos para actualizar las comisiones pagadas
    @Transactional(readOnly = true)
    public String updateCommissionPayments(List<CommissionPaymentDto> payments, List<UserModel> updateUsers,
            List<TransactionModel> updateTransactions, List<PaymentModel> listUserPayments, String lastStatus,
            String confirmationStatus, LocalDateTime currenDateTime, TransactionRepository transactionRepository,
            UserRepository userRepository) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for(CommissionPaymentDto payment : payments) {
            String userId = payment.getUserId();
            if(!ObjectId.isValid(userId)) {
                return "el id de usuario: " + userId + ", no es correcto";
            }
            Optional<UserModel> optionalUser = userRepository.findById(new ObjectId(userId));
            if(optionalUser.isPresent()) {
                // Se encontró el usuario y se puede seguir la lógica
                UserModel userDB = optionalUser.get();
                CommissionAccountDto userAccount = payment.getAccount();
                int userPayment = payment.getPayment();
                int userPaymentTotal = 0;
                for(CommissionDataDto commission : payment.getCommissions()) {
                    String transactionId = commission.transactionId();
                    int transactionCommision = commission.commission();
                    userPaymentTotal += transactionCommision;
                    boolean existsTransaction = false; // Buscamos si existe la transacción en la lista de transacciones
                    for(TransactionModel updateTransaction : updateTransactions) {
                        if(transactionId.equals(updateTransaction.getTransactionId())) {
                            // Existe la transasacción, así que, se busca la comisión que se tiene que actualizar
                            existsTransaction = true;
                            boolean existsCommission = false;
                            for(TransactionComissionModel commissionDB : updateTransaction.getCommissions()) {
                                int transactionCommissionDB = commissionDB.getUserCommission();
                                String transactionUserId = commissionDB.getUserId();
                                String commissionStatus = commissionDB.getCommissionStatus();
                                if(transactionCommision == transactionCommissionDB && userId.equals(transactionUserId)) {
                                    // Está es la transacción que se tiene que actualizar, antes se verifica que no este pagada.
                                    if(commissionStatus.equals(lastStatus)) {
                                        return "la comisión del usuario, ya se encuentra pagada en la transacción: " + transactionId;
                                    }
                                    commissionDB.setCommissionStatus(lastStatus);
                                    existsCommission = true;
                                    break;
                                }
                            }
                            if(!existsCommission) {
                                return "no se encontro la comisión del usuario: " + userId + ", en la transacción: " + transactionId;
                            }
                            break;
                        }
                    }
                    // Si no existe la transacción, se debe buscar por la base de datos, buscar la comisión que se debe actualizar y agregar a las transacciones que se deben actualizar
                    if(!existsTransaction) {
                        Optional<TransactionModel> optionalTransaction = transactionRepository.findById(transactionId);
                        if(optionalTransaction.isPresent()) {
                            // Se encontró la transacción, ahora se busca la comisión para ser actualizada
                            TransactionModel transactionDB = optionalTransaction.get();
                            String transactionStatus = transactionDB.getStatus();
                            if(!transactionStatus.equals("Aprobado") && !transactionStatus.equals(confirmationStatus)) {
                                return "el estado de la transacción: " + transactionId + ", no es correcto: " + transactionStatus;
                            }
                            boolean existsCommission = false;
                            for(TransactionComissionModel commissionDB : transactionDB.getCommissions()) {
                                int transactionCommissionDB = commissionDB.getUserCommission();
                                String transactionUserId = commissionDB.getUserId();
                                String commissionStatus = commissionDB.getCommissionStatus();
                                if(transactionCommision == transactionCommissionDB && userId.equals(transactionUserId)) {
                                    // Está es la transacción que se tiene que actualizar, antes se verifica que no este pagada.
                                    if(commissionStatus.equals(lastStatus)) {
                                        return "la comisión del usuario: " + userId + ", ya se encuentra pagada en la transacción: " + transactionId;
                                    }
                                    commissionDB.setCommissionStatus(lastStatus);
                                    updateTransactions.add(transactionDB); // Agregamos transacción con una comisión actualizada
                                    existsCommission = true;
                                    break;
                                }
                            }
                            if(!existsCommission) {
                                return "no se encontro la comisión del usuario: " + userId + ", en la transacción: " + transactionId;
                            }
                        } else {
                            return "transacción no encontrada: " + transactionId;
                        }
                    }
                } // Fin de actualización de comisiones de las transacciones
                // Ahora verificamos que el total de comisiones actualizadas, debe ser el mismo monto que se menciona que se pago
                if(userPayment != userPaymentTotal) {
                    return "el monto total de las comisiones, no es el mismo que el monto de pago al usuario: " + userId;
                }
                // Actualización de wallet del usuario
                WalletModel userWallet = userDB.getWallet();
                int availableBalance = userWallet.getAvailableBalance() - userPayment;
                if(availableBalance < 0) {
                    return "el monto pagado, es mayor al saldo disponible del usuario: " + userId;
                }
                if(userAccount == null || DataHelper.isNull(userAccount.bank()) || DataHelper.isNull(userAccount.accountNumber())) {
                    return "la cuenta de bancaria para el recibo de comisiones del usuario: " + userId + ", no es correcta";
                }
                userWallet.setAvailableBalance(availableBalance);
                userWallet.setPaymentBalance(userWallet.getPaymentBalance() + userPayment);
                userWallet.setTotalBalance(availableBalance + userWallet.getOutstandingBalance());
                // Creación de objeto para el egreso de comisiones (SE DEBE AGREGAR LOS IDS DE LOS EGRESOS EN EL USUARIO)
                PaymentModel novaPayment = new PaymentModel(new ObjectId(), userId, userAccount,
                        userPayment, availableBalance, payment.getVoucher(), currenDateTime.format(dateFormatter),
                        currenDateTime, currenDateTime);
                // Finalmente, agregamos el usuario que se tiene que actualizar a la lista de usuarios y el nuevo egreso a la lista de egresos
                userWallet.addPaymentId(novaPayment.getPaymentId());
                updateUsers.add(userDB);
                listUserPayments.add(novaPayment);
            } else {
                return "usuario no encontrado: " + userId;
            }
        }
        return null;
    }
    public Map<String, Object> confirmingTransactionStatus(List<TransactionModel> updateTransactions,
            List<UserModel> updateUsers, List<PaymentModel> listUserPayments, String lastStatus,
            String confirmationStatus, LocalDateTime currenDateTime) {
        Map<String, Object> dataUpdated = new HashMap<>();
        List<String> transactionIds = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        List<String> paymentIds = new ArrayList<>();
        // Confirmamos el estado de la transacción y agregamos los ids de las transacciones actualizadas
        for(TransactionModel updateTransaction : updateTransactions) {
            boolean isTransactionFinished = true;
            for(TransactionComissionModel commissionDB : updateTransaction.getCommissions()) {
                String commissionStatus = commissionDB.getCommissionStatus();
                if(!commissionStatus.equals(lastStatus)) {
                    // La transacción aún no se ha liberado
                    commissionDB.setCommissionStatus(confirmationStatus);
                    isTransactionFinished = false;
                }
            }
            if(isTransactionFinished) {
                updateTransaction.setStatus(lastStatus);
            } else {
                // La transacción tiene comisiones pendientes que pagar
                updateTransaction.setStatus(confirmationStatus);
            }
            updateTransaction.setObservation("La comisión ha sido liberada exitosamente");
            updateTransaction.setUpdatedDate(currenDateTime);
            // Agregando los ids de las transacciones actualizadas
            transactionIds.add(updateTransaction.getTransactionId());
        }
        // Agregamos los ids de los usuarios actualizados
        for(UserModel userDB : updateUsers) {
            userIds.add(userDB.getUserId());
        }
        // Agregamos los ids de los pagos creados para las comisiones
        for(PaymentModel paymentDB : listUserPayments) {
            paymentIds.add(paymentDB.getPaymentId());
        }
        // Agregamos al map, todos los registros actualizados y lo devolvemos
        dataUpdated.put("transactionIds", transactionIds);
        dataUpdated.put("userIds", userIds);
        dataUpdated.put("paymentIds", paymentIds);
        return dataUpdated;
    }

}
