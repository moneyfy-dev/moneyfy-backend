package com.referidos.app.segurosref.helpers;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import com.referidos.app.segurosref.repositories.LogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandInsurerModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.models.CityModel;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.models.LogModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.QuoterAddressModel;
import com.referidos.app.segurosref.models.QuoterCarModel;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.QuoterOwnerModel;
import com.referidos.app.segurosref.models.QuoterPaymentModel;
import com.referidos.app.segurosref.models.QuoterPlanModel;
import com.referidos.app.segurosref.models.QuoterPurchaserModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.repositories.CityRepository;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

@Component
public class SeedHelper {

    private static String INSURER_DARK_TEMPLATE =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 120 40\">"
            + "<rect width=\"120\" height=\"40\" rx=\"8\" fill=\"#111827\"/>"
            + "<text x=\"60\" y=\"25\" font-size=\"12\" text-anchor=\"middle\" fill=\"#ffffff\">%s</text>"
            + "</svg>";

    private static String INSURER_LIGHT_TEMPLATE =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 120 40\">"
            + "<rect width=\"120\" height=\"40\" rx=\"8\" fill=\"#f3f4f6\" stroke=\"#d1d5db\"/>"
            + "<text x=\"60\" y=\"25\" font-size=\"12\" text-anchor=\"middle\" fill=\"#111827\">%s</text>"
            + "</svg>";

    // Actualizar las ciudades de la base de datos
    @SuppressWarnings("null")
    public String updateCities(CityRepository cityRepository, boolean refreshData) {
        List<CityModel> citiesDB = cityRepository.findAll();
        List<CityModel> cities = this.buildCities();
        if(citiesDB.isEmpty()) {
            cityRepository.saveAll(cities);
            return "Las ciudades se han registrado";
        }
        if(refreshData) {
            cityRepository.deleteAll();
            cityRepository.saveAll(cities);
            return "Las ciudades se han vuelto a registrar";
        }
        // Hay ciudades en la BD actual, y se actualiza en relación a las ciudades del método 'buildCities()'
        for(CityModel city : cities) {
            String cityName = city.getCity();
            boolean isCity = false;
            for(CityModel cityDB : citiesDB) {
                String cityDBName = cityDB.getCity();
                if(cityName.equals(cityDBName)) {
                    isCity = true;
                    // Existe la ciudad, ahora hay que verificar si existen todas las comunas de la ciudad
                    for(String location : city.getLocations()) {
                        boolean isLocation = false;
                        for(String locationDB : cityDB.getLocations()) {
                            if(location.equals(locationDB)) {
                                isLocation = true;
                                break;
                            }
                        }
                        // Si la comuna no existe se agrega a la ciudad
                        if(!isLocation) {
                            cityDB.addLocation(location);
                        }
                    }
                    break;
                }
            }
            if(!isCity) {
                citiesDB.add(city);
            }
        }
        cityRepository.saveAll(citiesDB);
        return "Las ciudades se han actualizado";
    }

    // Lista de ciudades para inyectar en la DB, en caso de no estar
    private List<CityModel> buildCities() {
        List<CityModel> cityList = new ArrayList<>();
        cityList.add(new CityModel("Arica").addLocation("Arica").addLocation("Camarones"));
        cityList.add(new CityModel("Parinacota").addLocation("Putre").addLocation("General Lagos"));
        cityList.add(new CityModel("Iquique").addLocation("Iquique").addLocation("Alto Hospicio"));
        cityList.add(new CityModel("Tamarugal").addLocation("Pozo Almonte").addLocation("Camiña").addLocation("Colchane").addLocation("Huara").addLocation("Pica"));
        cityList.add(new CityModel("Antofagasta").addLocation("Antofagasta").addLocation("Mejillones").addLocation("Sierra Gorda").addLocation("Taltal"));
        cityList.add(new CityModel("El Loa").addLocation("Calama").addLocation("Ollague").addLocation("San Pedro De Atacama"));
        cityList.add(new CityModel("Tocopilla").addLocation("Tocopilla").addLocation("María Elena"));
        cityList.add(new CityModel("Copiapó").addLocation("Copiapó").addLocation("Caldera").addLocation("Tierra Amarilla"));
        cityList.add(new CityModel("Chañaral").addLocation("Chañaral").addLocation("Diego De Almagro"));
        cityList.add(new CityModel("Huasco").addLocation("Vallenar").addLocation("Alto Del Carmen").addLocation("Freirina").addLocation("Huasco"));
        cityList.add(new CityModel("Elqui").addLocation("La Serena").addLocation("Coquimbo").addLocation("Andacollo").addLocation("La Higuera").addLocation("Paiguano").addLocation("Vicuña"));
        cityList.add(new CityModel("Choapa").addLocation("Illapel").addLocation("Canela").addLocation("Los Vilos").addLocation("Salamanca"));
        cityList.add(new CityModel("Limarí").addLocation("Ovalle").addLocation("Combarbalá").addLocation("Monte Patria").addLocation("Punitaqui").addLocation("Río Hurtado"));
        cityList.add(new CityModel("Valparaíso").addLocation("Valparaíso").addLocation("Casablanca").addLocation("Concón").addLocation("Juan Fernández").addLocation("Puchuncaví").addLocation("Quintero").addLocation("Viña Del Mar"));
        cityList.add(new CityModel("Isla De Pascua").addLocation("Isla De Pascua"));
        cityList.add(new CityModel("Los Andes").addLocation("Los Andes").addLocation("Calle Larga").addLocation("Rinconada").addLocation("San Esteban"));
        cityList.add(new CityModel("Petorca").addLocation("La Ligua").addLocation("Cabildo").addLocation("Papudo").addLocation("Petorca").addLocation("Zapallar"));
        cityList.add(new CityModel("Quillota").addLocation("Quillota").addLocation("Calera").addLocation("Hijuelas").addLocation("La Cruz").addLocation("Nogales"));
        cityList.add(new CityModel("San Antonio").addLocation("San Antonio").addLocation("Algarrobo").addLocation("Cartagena").addLocation("El Quisco").addLocation("El Tabo").addLocation("Santo Domingo"));
        cityList.add(new CityModel("San Felipe").addLocation("San Felipe").addLocation("Catemu").addLocation("Llaillay").addLocation("Panquehue").addLocation("Putaendo").addLocation("Santa María"));
        cityList.add(new CityModel("Marga Marga").addLocation("Quilpué").addLocation("Limache").addLocation("Olmué").addLocation("Villa Alemana"));
        cityList.add(new CityModel("Santiago").addLocation("Santiago").addLocation("Cerrillos").addLocation("Cerro Navia").addLocation("Conchalí").addLocation("El Bosque").addLocation("Estación Central").addLocation("Huechuraba").addLocation("Independencia").addLocation("La Cisterna").addLocation("La Florida").addLocation("La Granja").addLocation("La Pintana").addLocation("La Reina").addLocation("Las Condes").addLocation("Lo Barnechea").addLocation("Lo Espejo").addLocation("Lo Prado").addLocation("Macul").addLocation("Maipú").addLocation("Ñuñoa").addLocation("Pedro Aguirre Cerda").addLocation("Peñalolén").addLocation("Providencia").addLocation("Pudahuel").addLocation("Quilicura").addLocation("Quinta Normal").addLocation("Recoleta").addLocation("Renca").addLocation("San Joaquín").addLocation("San Miguel").addLocation("San Ramón").addLocation("Vitacura"));
        cityList.add(new CityModel("Cordillera").addLocation("Puente Alto").addLocation("Pirque").addLocation("San José De Maipo"));
        cityList.add(new CityModel("Chacabuco").addLocation("Colina").addLocation("Lampa").addLocation("Tiltil"));
        cityList.add(new CityModel("Maipo").addLocation("San Bernardo").addLocation("Buin").addLocation("Calera De Tango").addLocation("Paine"));
        cityList.add(new CityModel("Melipilla").addLocation("Melipilla").addLocation("Alhué").addLocation("Curacaví").addLocation("María Pinto").addLocation("San Pedro"));
        cityList.add(new CityModel("Talagante").addLocation("Talagante").addLocation("El Monte").addLocation("Isla De Maipo").addLocation("Padre Hurtado").addLocation("Peñaflor"));
        cityList.add(new CityModel("Cachapoal").addLocation("Rancagua").addLocation("Codegua").addLocation("Coinco").addLocation("Coltauco").addLocation("Doñihue").addLocation("Graneros").addLocation("Las Cabras").addLocation("Machali").addLocation("Malloa").addLocation("Mostazal").addLocation("El Olivar").addLocation("Peumo").addLocation("Pichidegua").addLocation("Quinta De Tilcoco").addLocation("Rengo").addLocation("Requinoa").addLocation("San Vicente"));
        cityList.add(new CityModel("Cardenal Caro").addLocation("Pichilemu").addLocation("La Estrella").addLocation("Litueche").addLocation("Marchihue").addLocation("Navidad").addLocation("Paredones"));
        cityList.add(new CityModel("Colchagua").addLocation("San Fernando").addLocation("Chépica").addLocation("Chimbarongo").addLocation("Lolol").addLocation("Nancagua").addLocation("Palmilla").addLocation("Peralillo").addLocation("Placilla").addLocation("Pumanque").addLocation("Santa Cruz"));
        cityList.add(new CityModel("Talca").addLocation("Talca").addLocation("Constitución").addLocation("Curepto").addLocation("Empedrado").addLocation("Maule").addLocation("Pelarco").addLocation("Pencahue").addLocation("Río Claro").addLocation("San Clemente").addLocation("San Rafael"));
        cityList.add(new CityModel("Cauquenes").addLocation("Cauquenes").addLocation("Chanco").addLocation("Pelluhue"));
        cityList.add(new CityModel("Curicó").addLocation("Curicó").addLocation("Hualañe").addLocation("Licantén").addLocation("Molina").addLocation("Rauco").addLocation("Romeral").addLocation("Sagrada Familia").addLocation("Teno").addLocation("Vichuquén"));
        cityList.add(new CityModel("Linares").addLocation("Linares").addLocation("Colbún").addLocation("Longaví").addLocation("Parral").addLocation("Retiro").addLocation("San Javier").addLocation("Villa Alegre").addLocation("Yerbas Buenas"));
        cityList.add(new CityModel("Diguillín").addLocation("Chillán").addLocation("Bulnes").addLocation("Chillán Viejo").addLocation("El Carmen").addLocation("Pemuco").addLocation("Pinto").addLocation("Quillón").addLocation("San Ignacio").addLocation("Yungay"));
        cityList.add(new CityModel("Itata").addLocation("Quirihue").addLocation("Cobquecura").addLocation("Coelemu").addLocation("Ninhue").addLocation("Portezuelo").addLocation("Ranquil").addLocation("Treguaco"));
        cityList.add(new CityModel("Punilla").addLocation("San Carlos").addLocation("Coihueco").addLocation("Ñiquén").addLocation("San Fabián").addLocation("San Nicolás"));
        cityList.add(new CityModel("Concepción").addLocation("Concepción").addLocation("Coronel").addLocation("Chiguayante").addLocation("Florida").addLocation("Hualqui").addLocation("Lota").addLocation("Penco").addLocation("San Pedro de la Paz").addLocation("Santa Juana").addLocation("Talcahuano").addLocation("Tomé").addLocation("Hualpén"));
        cityList.add(new CityModel("Arauco").addLocation("Lebu").addLocation("Arauco").addLocation("Cañete").addLocation("Contulmo").addLocation("Curanilahue").addLocation("Los Alamos").addLocation("Tirua"));
        cityList.add(new CityModel("Bío-Bío").addLocation("Los Angeles").addLocation("Antuco").addLocation("Cabrero").addLocation("Laja").addLocation("Mulchén").addLocation("Nacimiento").addLocation("Negrete").addLocation("Quilaco").addLocation("Quilleco").addLocation("San Rosendo").addLocation("Santa Bárbara").addLocation("Tucapel").addLocation("Yumbel").addLocation("Alto Biobío"));
        cityList.add(new CityModel("Cautín").addLocation("Temuco").addLocation("Carahue").addLocation("Cunco").addLocation("Curarrehue").addLocation("Freire").addLocation("Galvarino").addLocation("Gorbea").addLocation("Lautaro").addLocation("Loncoche").addLocation("Melipeuco").addLocation("Nueva Imperial").addLocation("Padre Las Casas").addLocation("Perquenco").addLocation("Pitrufquén").addLocation("Pucón").addLocation("Saavedra").addLocation("Teodoro Schmidt").addLocation("Toltén").addLocation("Vilcún").addLocation("Villarrica").addLocation("Cholchol"));
        cityList.add(new CityModel("Malleco").addLocation("Angol").addLocation("Collipulli").addLocation("Curacautín").addLocation("Ercilla").addLocation("Lonquimay").addLocation("Los Sauces").addLocation("Lumaco").addLocation("Puren").addLocation("Renaico").addLocation("Traiguén").addLocation("Victoria"));
        cityList.add(new CityModel("Valdivia").addLocation("Valdivia").addLocation("Corral").addLocation("Lanco").addLocation("Los Lagos").addLocation("Máfil").addLocation("Mariquina").addLocation("Paillaco").addLocation("Panguipulli"));
        cityList.add(new CityModel("Ranco").addLocation("La Unión").addLocation("Futrono").addLocation("Lago Ranco").addLocation("Río Bueno"));
        cityList.add(new CityModel("Llanquihue").addLocation("Puerto Montt").addLocation("Calbuco").addLocation("Cochamó").addLocation("Fresia").addLocation("Frutillar").addLocation("Los Muermos").addLocation("Llanquihue").addLocation("Maullín").addLocation("Puerto Varas"));
        cityList.add(new CityModel("Chiloé").addLocation("Castro").addLocation("Ancud").addLocation("Chonchi").addLocation("Curaco de Velez").addLocation("Dalcahue").addLocation("Puqueldón").addLocation("Queilén").addLocation("Quellón").addLocation("Quemchi").addLocation("Quinchao"));
        cityList.add(new CityModel("Osorno").addLocation("Osorno").addLocation("Puerto Octay").addLocation("Purranque").addLocation("Puyehue").addLocation("Río Negro").addLocation("San Juan de la Costa").addLocation("San Pablo"));
        cityList.add(new CityModel("Palena").addLocation("Chaitén").addLocation("Futaleufú").addLocation("Hualaihue").addLocation("Palena"));
        cityList.add(new CityModel("Coihayque").addLocation("Coihayque").addLocation("Lago Verde"));
        cityList.add(new CityModel("Aisén").addLocation("Aisén").addLocation("Cisnes").addLocation("Guaitecas"));
        cityList.add(new CityModel("Capitán Prat").addLocation("Cochrane").addLocation("O'Higgins").addLocation("Tortel"));
        cityList.add(new CityModel("General Carrera").addLocation("Chile Chico").addLocation("Río Ibáñez"));
        cityList.add(new CityModel("Magallanes").addLocation("Punta Arenas").addLocation("Laguna Blanca").addLocation("Río Verde").addLocation("San Gregorio"));
        cityList.add(new CityModel("Antártica Chilena").addLocation("Cabo de Hornos").addLocation("Antártica"));
        cityList.add(new CityModel("Tierra del Fuego").addLocation("Porvenir").addLocation("Primavera").addLocation("Timaukel"));
        cityList.add(new CityModel("Última Esperanza").addLocation("Natales").addLocation("Torres del Paine"));
        return cityList;
    }

    // Registrar usuarios de prueba
    @Transactional
    public String updateTestUsers(UserRepository userRepository, ReferredRepository referredRepository, DeviceRepository deviceRepository,
            TransactionRepository transactionRepository, LogRepository logRepository, PasswordEncoder pwdEncoder, boolean refreshData) {
        // Data general para realizar proceso de registro
        List<String> testUsers = UserHelper.testUsers();
        if(testUsers == null || testUsers.isEmpty()) {
            return "usuarios de prueba incorrectos";
        }
        List<UserModel> createUsers = new ArrayList<>();
        List<ReferredModel> createReferreds = new ArrayList<>();
        LocalDate deprecatedDate = DataHelper.deprecatedDate();
        LocalDateTime deprecatedDateTime = DataHelper.deprecatedDateTime();
        LocalDateTime currentDate = LocalDateTime.now();
        // Revisamos si se quiere eliminar la data completa para volver a registrarla
        if(refreshData) {
            for(String user : testUsers) {
                Optional<UserModel> optionalUser = userRepository.findByPersonalData_Email(user);
                if(optionalUser.isPresent()) {
                    this.deleteUserAndDependencies(optionalUser.get(), userRepository, referredRepository, deviceRepository,
                            transactionRepository, logRepository);
                }
                Object[] obj = createTestUserStructure(user, userRepository, pwdEncoder, deprecatedDate, deprecatedDateTime, currentDate);
                if(obj != null) {
                    createUsers.add((UserModel) obj[0]);
                    createReferreds.add((ReferredModel) obj[1]);
                } else {
                    return "no es posible registrar al usuario de prueba";
                }
            }
            userRepository.saveAll(createUsers);
            referredRepository.saveAll(createReferreds);
            return "Los usuarios de pruebas se han registrado";
        }
        // Banderas para mensajes
        boolean existUsers = false;
        boolean novaUsers = false;
        for(String user : testUsers) {
            Object[] obj = null; // Nos permite ir almacenando los registros, que van hacer incluidos en la BD, en caso de no haber errores.
            boolean buildStructure = false;
            Optional<UserModel> optionalUser = userRepository.findByPersonalData_Email(user);
            if(optionalUser.isPresent()) {
                UserModel userDB = optionalUser.get();
                UserDataModel userDataDB = userDB.getPersonalData();
                if(userDataDB.getStatus().equals("Activado")) {
                    existUsers = true;
                } else {
                    // El usuario de prueba siempre debe estar activado, por lo cual, se eliminan sus registros
                    // relacionados y se vuelve a crear como 'Activado'
                    this.deleteUserAndDependencies(userDB, userRepository, referredRepository, deviceRepository, transactionRepository, logRepository);
                    // Se crea el usuario de prueba nuevamente
                    obj = this.createTestUserStructure(user, userRepository, pwdEncoder, deprecatedDate, deprecatedDateTime, currentDate);
                    novaUsers = true;
                    buildStructure = true;
                }
            } else {
                obj = createTestUserStructure(user, userRepository, pwdEncoder, deprecatedDate, deprecatedDateTime, currentDate);
                novaUsers = true;
                buildStructure = true;
            }
            // Revisamos si se creo la estructura de un nuevo usuario de prueba
            if(buildStructure) {
                // Vemos si hubo error o no
                if(obj != null) {
                    createUsers.add((UserModel) obj[0]);
                    createReferreds.add((ReferredModel) obj[1]);
                } else {
                    return "no es posible registrar al usuario de prueba";
                }
            }
        }
        // Se registran los usuarios, si no hubo error
        if(createUsers.size() > 0 && createReferreds.size() > 0) {
            userRepository.saveAll(createUsers);
            referredRepository.saveAll(createReferreds);
        }
        if(novaUsers && !existUsers) {
            return "se han registrados los usuarios de prueba";
        } else if(!novaUsers && existUsers) {
            return "los usuarios de prueba se encuentran registrados";
        } else if(novaUsers && existUsers) {
            return "hay usuarios de prueba existentes y se han registrado nuevos usuarios de prueba";
        } else {
            return "no se han encontrado usuarios de prueba";
        }
    }

    // Eliminar registros dependicientes para los usuarios de prueba o usuarios por defecto
    @SuppressWarnings("null")
    private void deleteUserAndDependencies(UserModel userDB, UserRepository userRepository, ReferredRepository referredRepository,
            DeviceRepository deviceRepository, TransactionRepository transactionRepository,
            LogRepository logRepository) {
        String userEmail = userDB.getPersonalData().getEmail();
        String userId = userDB.getUserId();
        Optional<ReferredModel> referredOptional = referredRepository.findByReferred(userEmail);
        if(referredOptional.isPresent()) {
            referredRepository.delete(referredOptional.get());
        }
        List<ReferredModel> referredUsers = referredRepository.findAllByUserReferring(userEmail);
        LocalDateTime currentTime = LocalDateTime.now();
        for(ReferredModel referredUser : referredUsers) {
            referredUser.setUserReferring("Sin Usuario");
            referredUser.setCodeToRefer("Sin Usuario");
            referredUser.setUserReferringStatus("Desactivado");
            referredUser.setUpdatedDate(currentTime);
        }
        if(referredUsers.size() > 0) {
            referredRepository.saveAll(referredUsers);
        }
        Optional<DeviceModel> deviceOptional = deviceRepository.findByUser(userEmail);
        if(deviceOptional.isPresent()) {
            deviceRepository.delete(deviceOptional.get());
        }
        List<LogModel> userLogs = logRepository.findAllByUserId(userId);
        if(userLogs.size() > 0) {
            logRepository.deleteAll(userLogs);
        }
        List<TransactionModel> userTransactions = transactionRepository.findAllByCommissions_UserId(userId);
        List<TransactionModel> updateTransactions = new ArrayList<>();
        List<TransactionModel> deleteTransactions = new ArrayList<>();
        for(TransactionModel userTransaction : userTransactions) {
            if(userTransaction.getCommissionScope() <= 1) {
                deleteTransactions.add(userTransaction);
            } else {
                for(TransactionComissionModel comission : userTransaction.getCommissions()) {
                    if(comission.getUserId().equals(userId)) {
                        comission.setUserId("Usuario de App");
                        updateTransactions.add(userTransaction);
                        break;
                    }
                }
            }
        }
        transactionRepository.deleteAll(deleteTransactions);
        transactionRepository.saveAll(updateTransactions);
        userRepository.delete(userDB);
    }

    // Crear estructura de un usuario de prueba
    private Object[] createTestUserStructure(String user, UserRepository userRepository, PasswordEncoder pwdEncoder,
            LocalDate deprecatedDate, LocalDateTime deprecatedDateTime, LocalDateTime currentDate) {
        try {
            String sessionToken = JwtConfig.createSessionToken(user, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
            String refreshToken = JwtConfig.createRefreshToken(user);
            String codeToRefer = DataHelper.generateCodeToRefer(userRepository);
            UserDataModel userData = new UserDataModel("Test", "User", user, "", "",
                    deprecatedDate, "Activado", new byte[0], pwdEncoder.encode("Testing_123"), "",
                    "ROLE_USER", "", deprecatedDateTime, sessionToken, refreshToken);
            WalletModel userWallet = new WalletModel(0, 0, 0, 0);
            NotificationModel userNotifs = new NotificationModel(true, true, true,
                    false, false, true, false, false, false, new ArrayList<>());
            // Creamos la estructura del usuario 'seeder'
            UserModel novaUser = new UserModel(codeToRefer, deprecatedDateTime, userData, userWallet, userNotifs);
            ReferredModel novaReferred = new ReferredModel("Sin usuario", "Sin usuario",
                    user, "Desactivado", "Activado", currentDate, currentDate);
            // Retornamos el usuario de prueba y su registro de referido relacionado (por funcionamiento de app)
            return new Object[] {novaUser, novaReferred};
        } catch(Exception e) {
            LOGGER_MESSAGES.info("No se ha podido registrar el usuario de prueba " + user + ": " + e.getMessage());
        }
        return null;
    }

    // Registrar usuarios por defecto
    @Transactional
    public String updateDefaultUsers(UserRepository userRepository, ReferredRepository referredRepository,
            DeviceRepository deviceRepository, TransactionRepository transactionRepository, LogRepository logRepository,
            PasswordEncoder pwdEncoder) {
        // Obtenemos los usuarios por defectos y revisamos que haya valores correctos
        List<String> defaultUsers = UserHelper.defaultUsers();
        if(defaultUsers == null || defaultUsers.size() != 3) {
            return "usuarios por defectos incorrectos";
        }
        String defaultUser1 = defaultUsers.get(0);
        String defaultUser2 = defaultUsers.get(1);
        String defaultUser3 = defaultUsers.get(2);
        if(defaultUser1 == null || defaultUser2 == null || defaultUser3 == null) {
            return "usuarios por defecto 'null'";
        }
        // Obtenemos data necesaria
        List<UserModel> createUsers = new ArrayList<>();
        List<ReferredModel> createReferreds = new ArrayList<>();
        List<TransactionModel> createTransactions = new ArrayList<>();
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDate deprecatedDate = DataHelper.deprecatedDate();
        LocalDateTime deprecatedDateTime = DataHelper.deprecatedDateTime();
        boolean repeatedCodes = true;
        // Se crea los objetos sin instanciar
        UserModel user1;
        UserModel user2;
        UserModel user3;
        do {
            // Creamos el primer usuario sin ser referido y los usuarios en caso de existir se eliminan solo
            // si estos no están activados, caso contrario no es posible eliminarlos y se retorna el mensaje
            Object[] obj1 = this.buildDefaultUser(defaultUser1, "Sin usuario", "Sin usuario",
                    userRepository, deviceRepository, referredRepository, transactionRepository, logRepository,
                    pwdEncoder, currentDate, deprecatedDate, deprecatedDateTime);
            String message1 = (String) obj1[2];
            if(!message1.equals("Estructura Creada")) {
                return message1;
            }
            user1 = (UserModel) obj1[0];
            String codeToRefferUser1 = user1.getCodeToRefer();
            UserDataModel userData1 = user1.getPersonalData();
            ReferredModel referred1 = (ReferredModel) obj1[1];
            // Creamos el segundo usuario, referido por el primero
            Object[] obj2 = this.buildDefaultUser(defaultUser2, userData1.getEmail(), codeToRefferUser1, userRepository,
                    deviceRepository, referredRepository, transactionRepository, logRepository, pwdEncoder, currentDate,
                    deprecatedDate, deprecatedDateTime);
            String message2 = (String) obj2[2];
            if(!message2.equals("Estructura Creada")) {
                return message2;
            }
            user2 = (UserModel) obj2[0];
            String codeToRefferUser2 = user2.getCodeToRefer();
            UserDataModel userData2 = user2.getPersonalData();
            ReferredModel referred2 = (ReferredModel) obj2[1];
            // Creamos el tercer usuario, referido por el segundo
            Object[] obj3 = this.buildDefaultUser(defaultUser3, userData2.getEmail(), codeToRefferUser2, userRepository,
                    deviceRepository, referredRepository, transactionRepository, logRepository, pwdEncoder, currentDate,
                    deprecatedDate, deprecatedDateTime);
            String message3 = (String) obj3[2];
            if(!message3.equals("Estructura Creada")) {
                return message3;
            }
            user3 = (UserModel) obj3[0];
            String codeToRefferUser3 = user3.getCodeToRefer();
            ReferredModel referred3 = (ReferredModel) obj3[1];
            // Verficamos que no se repiten los códigos de los usuarios
            if(codeToRefferUser1 != codeToRefferUser2 && codeToRefferUser1 != codeToRefferUser3 && codeToRefferUser2 != codeToRefferUser3) {
                repeatedCodes = false;
                createUsers.add(user1);
                createUsers.add(user2);
                createUsers.add(user3);
                createReferreds.add(referred1);
                createReferreds.add(referred2);
                createReferreds.add(referred3);
            }
        } while(repeatedCodes);
        // No hubo error y se tiene la estructura de los 3 usuarios por defectos, ahora se deben crear sus comisiones
        createTransactions.add(this.generateTransaction(null, null, user1, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, null, user1, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, null, user1, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, null, user1, "Rechazado", currentDate));
        createTransactions.add(this.generateTransaction(null, null, user1, "Caducado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Rechazado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Rechazado", currentDate));
        createTransactions.add(this.generateTransaction(null, user1, user2, "Caducado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Aprobado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Rechazado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Rechazado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Rechazado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Caducado", currentDate));
        createTransactions.add(this.generateTransaction(user1, user2, user3, "Caducado", currentDate));
        // Cuando se haya incluido toda la data se guardan todos los datos
        userRepository.saveAll(createUsers);
        referredRepository.saveAll(createReferreds);
        transactionRepository.saveAll(createTransactions);
        return "se han generado los usuarios por defecto";
    }

    // Verificar si el usuario por defecto es posible crearlo
    private Object[] buildDefaultUser(String defaultUser, String userReferring, String referredCode, UserRepository userRepository,
            DeviceRepository deviceRepository, ReferredRepository referredRepository, TransactionRepository transactionRepository, LogRepository logRepository,
            PasswordEncoder pwdEncoder, LocalDateTime currentDate, LocalDate deprecatedDate, LocalDateTime deprecatedDateTime) {
        Object[] obj;
        Optional<UserModel> optionalUser = userRepository.findByPersonalData_Email(defaultUser);
        if(optionalUser.isPresent()) {
            UserModel userDB = optionalUser.get();
            UserDataModel userDataDB = userDB.getPersonalData();
            if(userDataDB.getStatus().equals("Activado")) {
                return new Object[] {"", "", "usuario por defecto se encuentra creado: ".concat(defaultUser)};
            } else {
                this.deleteUserAndDependencies(userDB, userRepository, referredRepository, deviceRepository, transactionRepository, logRepository);
                obj = this.createDefaultUserStructure(defaultUser, userReferring, referredCode, userRepository, pwdEncoder,
                    currentDate, deprecatedDate, deprecatedDateTime);
            }
        } else {
            obj = this.createDefaultUserStructure(defaultUser, userReferring, referredCode, userRepository, pwdEncoder,
                    currentDate, deprecatedDate, deprecatedDateTime);
        }
        if(obj == null) {
            return new Object[] {"", "", "no es posible registrar el usuario por defecto: ".concat(defaultUser)};
        }
        return new Object[] {obj[0], obj[1], "Estructura Creada"};
    }

    // Crear la estructura de usuario por defecto
    private Object[] createDefaultUserStructure(String user, String userReferring, String referredCode, UserRepository userRepository,
            PasswordEncoder pwdEncoder, LocalDateTime currentDate, LocalDate deprecatedDate, LocalDateTime deprecatedDateTime) {
        try {
            String sessionToken = JwtConfig.createSessionToken(user, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
            String refreshToken = JwtConfig.createRefreshToken(user);
            String codeToRefer = DataHelper.generateCodeToRefer(userRepository);
            UserDataModel userData = new UserDataModel("Default", "User", user, "", "",
                    deprecatedDate, "Activado", new byte[0], pwdEncoder.encode("Testing_123"), "",
                    "ROLE_USER", "", deprecatedDateTime, sessionToken, refreshToken);
            WalletModel userWallet = new WalletModel(0, 0, 0, 0);
            NotificationModel userNotifs = new NotificationModel(true, true, true,
                    false, false, true, false, false, false, new ArrayList<>());
            // Creamos la estructura del usuario 'seeder'
            UserModel novaUser = new UserModel(codeToRefer, deprecatedDateTime, userData, userWallet, userNotifs);
            novaUser.setUserId(new ObjectId());
            ReferredModel novaReferred = new ReferredModel(userReferring, referredCode, user,
                    (userReferring.equals("Sin usuario")) ? "Desactivado" : "Activado", "Activado",
                    currentDate, currentDate);
            // Guardamos en la base de datos, y lo agregamos a la lista de los usuarios 'seeders'
            return new Object[] {novaUser, novaReferred};
        } catch(Exception e) {
            LOGGER_MESSAGES.info("No se ha podido registrar el usuario por defecto " + user + ": " + e.getMessage());
        }
        return null;
    }

    // Crear trasaction para usuario por defecto, el usuario C es el que hace la venta de la póliza
    private TransactionModel generateTransaction(UserModel userA, UserModel userB, UserModel userC,
            String transactionStatus, LocalDateTime currentDate) {
        // Primero revisamos si el usuario que realiza la venta tiene cuenta bancaria predeterminada, sino se la agregamos
        if(userC.getAccounts().isEmpty()) {
            userC.addAccount(this.createUserBankAccount(userC, currentDate, ((int) (Math.random() * 3)+1)));
        }
        // Luego creamos una nueva cotización en el usuario que estará relacionada a la transacción
        QuoterModel userQuote = this.createUserQuote(transactionStatus, currentDate);
        userC.addQuoter(userQuote);
        // Luego de haber creado la cotización por defecto, se crea la transacción y se envían todos los usuarios,
        // para calcular las comisiones
        String transactionId = new ObjectId().toString();
        String userCId = userC.getUserId();
        boolean isApproved = transactionStatus.equals("Aprobado");
        String observation = "La transacción ha sido " + transactionStatus;
        LocalDateTime approvalDate = (isApproved) ? currentDate : DataHelper.deprecatedDateTime();
        int commissionUserC = 35000, commissionUserB = 10000, commissionUserA = 5000;
        TransactionModel novaTransaction = new TransactionModel(transactionId, userQuote.getQuoterPlanData().getQuoterPlanId(),
                userCId, userQuote.getQuoterId(), transactionStatus, commissionUserC, 1,
                observation, currentDate, currentDate, approvalDate);
        // Comenzamos ha agregar las comisiones, independiente del estado de la transacción
        novaTransaction.addCommission(new TransactionComissionModel(userCId, commissionUserC, transactionStatus));
        if(userB != null) {
            String userBId = userB.getUserId();
            novaTransaction.addCommission(new TransactionComissionModel(userBId, commissionUserB, transactionStatus));
            novaTransaction.setCommissionScope(2);
            novaTransaction.setCommissionTotal(commissionUserC + commissionUserB);
            if(userA != null) {
                String userAId = userA.getUserId();
                novaTransaction.addCommission(new TransactionComissionModel(userAId, commissionUserA, transactionStatus));
                novaTransaction.setCommissionScope(3);
                novaTransaction.setCommissionTotal(commissionUserC + commissionUserB + commissionUserA);
            }
        }
        // Se verifica que la transacción haya sido aprobada para agregar las comisiones a los usuarios, y como estamos
        // agregando la transacción directamente, solo agregamos dinero, no quitamos, en caso de que la comisión sea
        // aprobada.
        if(isApproved) {
            WalletModel userWalletC = userC.getWallet();
            userWalletC.setAvailableBalance(userWalletC.getAvailableBalance() + commissionUserC);
            userWalletC.setTotalBalance(userWalletC.getAvailableBalance() + userWalletC.getOutstandingBalance());
            if(userB != null) {
                WalletModel userWalletB = userB.getWallet();
                userWalletB.setAvailableBalance(userWalletB.getAvailableBalance() + commissionUserB);
                userWalletB.setTotalBalance(userWalletB.getAvailableBalance() + userWalletB.getOutstandingBalance());
                if(userA != null) {
                    WalletModel userWalletA = userA.getWallet();
                    userWalletA.setAvailableBalance(userWalletA.getAvailableBalance() + commissionUserA);
                    userWalletA.setTotalBalance(userWalletA.getAvailableBalance() + userWalletA.getOutstandingBalance());
                }
            }
        }
        // Se agrega la transacción al usuario de la transacción
        userC.getWallet().addTransactionId(transactionId);
        return novaTransaction;
    }

    // Creamos una cuenta bancaria predeterminada a un usuario de prueba o por defecto que no tenga cuenta
    private AccountModel createUserBankAccount(UserModel user, LocalDateTime currentDate, int option) {
        ObjectId objectId = new ObjectId();
        String holderName = "Default User";
        String email = user.getPersonalData().getEmail();
        switch (option) {
            case 1 -> {
                return new AccountModel(objectId, "11.111.111-1", holderName, "Bco Estado", email,
                        "Banco Estado", "Corriente", "783342201", true,
                        currentDate, currentDate);
            }
            case 2 -> {
                return new AccountModel(objectId, "22.222.222-2", holderName, "Bco Chile", email,
                        "Banco Chile", "Vista", "844938022", true,
                        currentDate, currentDate);
            }
            default -> {
                return new AccountModel(objectId, "33.333.333-3", holderName, "Bco BCI", email,
                        "Banco BCI/Mach", "Corriente", "500938827", true,
                        currentDate, currentDate);
            }
        }
    }

    // Creamos una cotización nueva para un usuario de prueba o por defecto
    private QuoterModel createUserQuote(String transactionStatus, LocalDateTime currentTime) {
        ObjectId objectId = new ObjectId();
        QuoterOwnerModel ownerData = new QuoterOwnerModel("11.111.111-1", "Propietario", "Default", "HD");
        QuoterCarModel carData = new QuoterCarModel("JKLW99", "OPEL", "CORSA", "2023", "Negro", "N0V0T3STT4RB0", "N0V0T3STT3ST3R", "Stellantis");
        QuoterPurchaserModel purchaserData = new QuoterPurchaserModel("55.555.555-5", "Comprador", "Default", "HD", "comprador.default.hd314@gmail.com", "+56912345678", "2");
        QuoterPlanModel planData = new QuoterPlanModel("22000653_5", "BCI", "SOLUCION MOVIL 2.0", 38367.06, 45.98, 11, 4.18, 160374.0, 10, 0.0);
        QuoterAddressModel addressData = new QuoterAddressModel("Calle Default", "55#A", "");
        QuoterPaymentModel paymentData = new QuoterPaymentModel("", "", "", "");
        return new QuoterModel(objectId, transactionStatus, ownerData, carData, purchaserData, planData, addressData, paymentData, currentTime, currentTime);
    }

    // Función para actualizar las aseguradoras de la app
    @Transactional
    public String updateInsurers(InsurerRepository insurerRepository, boolean refreshData) {
        List<InsurerModel> insurers = this.buildInsurers();
        if(refreshData) {
            insurerRepository.deleteAll();
            if(insurers.size() > 0) {
                insurerRepository.saveAll(insurers);
            }
            return "Las aseguradoras se han vuelto a crear";
        }
        for(InsurerModel insurer : insurers) {
            if(!insurerRepository.existsByNameOrAlias(insurer.getName(), insurer.getAlias())) {
                insurerRepository.save(insurer);
            }
        }
        return "Las aseguradoras se han actualizado";
    }

    // Construimos las aseguradoras de la app
    private List<InsurerModel> buildInsurers() {
        List<InsurerModel> insurers = new ArrayList<>();
        insurers.add(new InsurerModel("Tractor Seguros Automotriz", "aseguradora1", "", String.format(INSURER_DARK_TEMPLATE, "TRACTOR"), String.format(INSURER_LIGHT_TEMPLATE,"TRACTOR")));
        insurers.add(new InsurerModel("Seguros Alameda", "aseguradora2", "", String.format(INSURER_DARK_TEMPLATE, "ALAMEDA"), String.format(INSURER_LIGHT_TEMPLATE,"ALAMEDA")));
        insurers.add(new InsurerModel("Los Alamos Seguros Automotriz", "aseguradora3", "", String.format(INSURER_DARK_TEMPLATE, "ALAMOS"), String.format(INSURER_LIGHT_TEMPLATE,"ALAMOS")));
        insurers.add(new InsurerModel("BCI", "aseguradora4", "", String.format(INSURER_DARK_TEMPLATE, "BCI"), String.format(INSURER_LIGHT_TEMPLATE,"BCI")));
        insurers.add(new InsurerModel("FDI Seguros", "aseguradora5", "", String.format(INSURER_DARK_TEMPLATE, "FDI"), String.format(INSURER_LIGHT_TEMPLATE,"FDI")));
        return insurers;
    }

    // Función para actualizar las marcas de la app
    public Object[] updateBrands(BrandRepository brandRepository, boolean refreshData) {
        List<BrandModel> brands = this.buildBrands();
        if(refreshData) {
            brandRepository.deleteAll();
            if(brands.size() > 0) {
                brands = brandRepository.saveAll(brands);
            }
            return new Object[] {"Se han creado las marcas nuevamente", brands};
        }
        // Se compara la data actual vs la data de la DB para actualizarla
        return null;
    }

    // Construimos las marcas de la app, en referencia a la documentación actual
    private List<BrandModel> buildBrands() {
        List<BrandModel> brands = new ArrayList<>();
        // brands.add(new BrandModel("Marca", new ArrayList<>(), new ArrayList<>()).addInsurerBrandId(new BrandInsurerModel(1, "NombreAseguradoraObjTieneIdDeMarcaDeAseguradora")).addInsurerBrandId(new BrandInsurerModel(1, "OtraAseguradoraObjTieneIdDeMarcaDeAseguradora")).addModel(new BrandDataModel(new ObjectId(), "Model", "Type", new ArrayList<>()).addInsurerModelId(new BrandInsurerModel(1, "NombreAseguradoraObjTieneIdDeModeloDeAseguradora")).addInsurerModelId(new BrandInsurerModel(1, "OtraAseguradoraObjTieneIdDeModeloDeAseguradora"))));
        return brands;
    }

}
