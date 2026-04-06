package com.referidos.app.segurosref.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.CityModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.CityRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

@Service
public class CityServiceImpl implements CityService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> findAll(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<CityModel> cities = cityRepository.findAll();
        return ResponseHelper.ok("las ciudades de la aplicación han sido recuperadas", DataHelper.buildUser(userDB, "cities", cities));
    }

}
