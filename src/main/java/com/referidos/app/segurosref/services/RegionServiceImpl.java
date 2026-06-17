package com.referidos.app.segurosref.services;

import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.RegionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.RegionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> findAll(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<RegionModel> regions = regionRepository.findAll();
        return ResponseHelper.ok("las regiones de la aplicación han sido recuperadas",
                DataHelper.buildUser(userDB, "regions", regions));
    }

}
