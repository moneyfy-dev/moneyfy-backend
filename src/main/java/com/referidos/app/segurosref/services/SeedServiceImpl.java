package com.referidos.app.segurosref.services;

import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.models.RegionModel;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.repositories.RegionRepository;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.requests.SeedRequest;

import org.springframework.security.core.context.SecurityContextHolder;
import com.referidos.app.segurosref.models.ManagerModel;
import com.referidos.app.segurosref.dtos.manager.ManagerDto;
import com.referidos.app.segurosref.repositories.ManagerRepository;

@Service
@RequiredArgsConstructor
public class SeedServiceImpl implements SeedService {

    private final ManagerRepository managerRepository;

    private final SeedHelper seedHelper;

    private final RegionRepository regionRepository;

    private final InsurerRepository insurerRepository;

    private final BrandRepository brandRepository;

    @Transactional
    @Override
    public ResponseEntity<?> checkRegions(SeedRequest seedRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        ManagerModel managerDB = managerRepository.findByEmail(email).orElse(null);
        if (managerDB == null) {
            return ResponseHelper.unauthorized("no autorizado");
        }
        ManagerDto managerDto = ManagerDto.builder()
                .managerId(managerDB.getManagerId())
                .name(managerDB.getName())
                .surname(managerDB.getSurname())
                .email(managerDB.getEmail())
                .status(managerDB.getStatus())
                .build();
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objRegions = seedHelper.updateRegions(regionRepository, refreshData);
        String message = (String) objRegions[0];
        @SuppressWarnings("unchecked")
        List<RegionModel> regions = (List<RegionModel>) objRegions[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("regions", regions);
        data.put("manager", managerDto);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

    @Transactional
    @Override
    public ResponseEntity<?> checkInsurers(SeedRequest seedRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        ManagerModel managerDB = managerRepository.findByEmail(email).orElse(null);
        if (managerDB == null) {
            return ResponseHelper.unauthorized("no autorizado");
        }
        ManagerDto managerDto = ManagerDto.builder()
                .managerId(managerDB.getManagerId())
                .name(managerDB.getName())
                .surname(managerDB.getSurname())
                .email(managerDB.getEmail())
                .status(managerDB.getStatus())
                .build();
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objInsurers = seedHelper.updateInsurers(insurerRepository, refreshData);
        String message = (String) objInsurers[0];
        @SuppressWarnings("unchecked")
        List<InsurerModel> insurers = (List<InsurerModel>) objInsurers[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("insurers", insurers);
        data.put("manager", managerDto);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

    @Transactional
    @Override
    public ResponseEntity<?> checkBrands(SeedRequest seedRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        ManagerModel managerDB = managerRepository.findByEmail(email).orElse(null);
        if (managerDB == null) {
            return ResponseHelper.unauthorized("no autorizado");
        }
        ManagerDto managerDto = ManagerDto.builder()
                .managerId(managerDB.getManagerId())
                .name(managerDB.getName())
                .surname(managerDB.getSurname())
                .email(managerDB.getEmail())
                .status(managerDB.getStatus())
                .build();
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objBrands = seedHelper.updateBrands(brandRepository, refreshData);
        String message = (String) objBrands[0];
        @SuppressWarnings("unchecked")
        List<BrandModel> brands = (List<BrandModel>) objBrands[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("brands", brands);
        data.put("manager", managerDto);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

}
