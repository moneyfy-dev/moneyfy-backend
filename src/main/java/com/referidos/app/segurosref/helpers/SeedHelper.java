package com.referidos.app.segurosref.helpers;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.models.CityModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.CityRepository;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.seeder.RunUserSeeder;

@Component
public class SeedHelper {

    // Ver si se puede ajustar
    @Autowired
    private UserHelper userHelper;

    // Actualizar las ciudades de la base de datos
    @SuppressWarnings("null")
    public void updateCities(CityRepository cityRepository) {
        List<CityModel> citiesDB = cityRepository.findAll();
        List<CityModel> cities = this.buildCities();
        if(citiesDB.isEmpty()) {
            cityRepository.saveAll(cities);
            return;
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
    }

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

    // FUNCIÓN PARA REGISTRAR USUARIOS DE PRUEBAS
    @Transactional
    public String updateTestUsers(UserRepository userRepository, ReferredRepository referredRepository,
            DeviceRepository deviceRepository, PasswordEncoder pwdEncoder) {
        boolean novaUsers = false;
        boolean existUsers = false;
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime deprecatedDateTime = DataHelper.deprecatedDateTime();

        for(String user : RunUserSeeder.testUsers()) {
            Optional<UserModel> optionalUser = userRepository.findByPersonalData_Email(user);
            if(optionalUser.isPresent()) {
                UserModel userDB = optionalUser.get();
                UserDataModel userDataDB = userDB.getPersonalData();
                switch (userDataDB.getStatus()) {
                    case "Activado" -> {
                        existUsers = true;
                        break;
                    }
                    case "Desactivado" -> {
                        if(!userDataDB.getSessionToken().equals("") && !userDataDB.getRefreshToken().equals("")) {
                            // Tiene tokens, hay que ver si se puede eliminar el usuario => como makeUserObsolet
                            if(userHelper.makeUserObsolete(userRepository, deviceRepository, referredRepository, userDB)) {
                                // El usuario queda obsoleto, y se puede crear nuevamente el usuario de prueba
                                String novaUser = this.registerTestUser(user, userRepository, referredRepository, pwdEncoder, deprecatedDateTime, currentDate);
                                if(novaUser == null) {
                                    return null;
                                } else {
                                    novaUsers = true;
                                }
                            } else {
                                // Usuario que todavía se puede habilitar, por lo tanto, existe
                                existUsers = true;
                            }
                        } else {
                            // Usuario no está confirmado, se puede eliminar el usuario con su registro de referido.
                            Optional<ReferredModel> optionalReferred = referredRepository.findByReferred(user);
                            if(optionalReferred.isPresent()) {
                                referredRepository.delete(optionalReferred.get());
                            }
                            userRepository.delete(userDB);
                            // Luego de haberse eliminado el usuario NO confirmado, lo registramos
                            String novaUser = this.registerTestUser(user, userRepository, referredRepository, pwdEncoder,
                                    deprecatedDateTime, currentDate);
                            if(novaUser == null) {
                                return null;
                            } else {
                                novaUsers = true;
                            }
                        }
                        break;
                    }
                    default -> {
                        return null;
                    }
                }
            } else {
                String novaUser = registerTestUser(user, userRepository, referredRepository, pwdEncoder,
                        deprecatedDateTime, currentDate);
                if(novaUser == null) {
                    return null;
                } else {
                    novaUsers = true;
                }
            }
        }

        if(novaUsers && !existUsers) {
            return "se han registrados los usuarios de prueba";
        } else if(!novaUsers && existUsers) {
            return "los usuarios de prueba se encuentran registrados";
        } else if(novaUsers && existUsers) {
            return "hay usuarios de prueba existentes y se han registrado nuevos usuarios de prueba";
        } else {
            return null;
        }
    }

    // REGISTRAR UN USUARIO DE PRUEBA
    private String registerTestUser(String user, UserRepository userRepository, ReferredRepository referredRepository,
            PasswordEncoder pwdEncoder, LocalDateTime deprecatedDateTime, LocalDateTime currentDate) {
        try {
            String sessionToken = JwtConfig.createSessionToken(user, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
            String refreshToken = JwtConfig.createRefreshToken(user);
            String codeToRefer = DataHelper.generateCodeToRefer(userRepository);
            UserDataModel userData = new UserDataModel("Test", "User", user, "", "",
                    DataHelper.deprecatedDate(), "Activado", new byte[0], pwdEncoder.encode("Testing_123"), "",
                    "ROLE_USER", "", deprecatedDateTime, sessionToken, refreshToken);
            WalletModel userWallet = new WalletModel(0, 0, 0, 0);
            NotificationModel userNotifs = new NotificationModel(false, true, true,
                    false, false, true, false, false, false, new ArrayList<>());
            // Creamos la estructura del usuario 'seeder'
            UserModel novaUser = new UserModel(codeToRefer, deprecatedDateTime, userData, userWallet, userNotifs);
            ReferredModel novaReferred = new ReferredModel("Sin usuario", "Sin usuario",
                    user, "Desactivado", "Activado", currentDate, currentDate);
            // Guardamos en la base de datos, y lo agregamos a la lista de los usuarios 'seeders'
            userRepository.save(novaUser);
            referredRepository.save(novaReferred);
            return user;
        } catch(Exception e) {
            LOGGER_MESSAGES.info("No se ha podido registrar el usuario: " + user);
        }
        return null;
    }

}
