package com.referidos.app.segurosref.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.CityModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.CityRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.CityRequest;

@Service
public class CityServiceImpl implements CityService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Value(value="${city.endpoint.keyword}")
    private String cityEndpointKeyword;

    // Servicios para recuperar y registrar ciudades de la aplicación
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> findAll(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<CityModel> cities = cityRepository.findAll();
        return ResponseHelper.ok("las ciudades de la aplicación han sido recuperadas", DataHelper.buildUser(userDB, "cities", cities));
    }

    @Transactional
    @Override
    public ResponseEntity<?> registerCities(CityRequest cityRequest) {
        // Recuperamos los datos y verificamos que traigan valor o sean correctos
        String key = cityRequest.key();
        List<CityModel> cities = cityRequest.cities();
        if(key == null || !key.equals(cityEndpointKeyword) || cities == null || cities.size() < 1) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Obtenemos las ciudades actuales de la base de datos, y vamos comparando las ciudades proveidas para saber si ya está en la bd
        List<CityModel> citiesDB = cityRepository.findAll();
        for(CityModel city : cities) {
            String cityName = city.getCity();
            boolean existsCity = false;
            for(CityModel cityDB : citiesDB) {
                String cityDBName = cityDB.getCity();
                if(cityName.equals(cityDBName)) {
                    existsCity = true;
                    // Existe la ciudad, ahora hay que verificar si existen todas las comunas de la ciudad
                    for(String cityLocation : city.getLocations()) {
                        boolean existsLocation = false;
                        for(String cityLocationDB : cityDB.getLocations()) {
                            if(cityLocation.equals(cityLocationDB)) {
                                existsLocation = true;
                                break;
                            }
                        }
                        if(!existsLocation) {
                            // Si no existe la comuna de la ciudad, se la agregamos
                            cityDB.addLocation(cityLocation);
                        }
                    }
                    break;
                }
            }
            if(!existsCity) {
                // Si no existe la ciudad se agrega el objeto completo a la base de datos
                citiesDB.add(city);
            }
        }
        // Creamos o actualizamos las ciudades en la bd
        cityRepository.saveAll(citiesDB);
        return ResponseHelper.ok("se han registrado/actualizado las ciudades de la aplicación", Map.of("cities", citiesDB));
    }

}
