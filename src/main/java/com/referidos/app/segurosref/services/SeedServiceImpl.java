package com.referidos.app.segurosref.services;

import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.models.RegionModel;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.repositories.RegionRepository;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.requests.SeedRequest;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class SeedServiceImpl implements SeedService {

    @Value(value = "${moneyfy.api-key}")
    private String apiKeyMF;

    private final SeedHelper seedHelper;

    private final RegionRepository regionRepository;

    private final InsurerRepository insurerRepository;

    private final BrandRepository brandRepository;

    @Transactional
    @Override
    public ResponseEntity<?> checkRegions(HttpServletRequest request, SeedRequest seedRequest) {
        if (!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("X-Moneyfy-Api-Key"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objRegions = seedHelper.updateRegions(regionRepository, refreshData);
        String message = (String) objRegions[0];
        @SuppressWarnings("unchecked")
        List<RegionModel> regions = (List<RegionModel>) objRegions[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("regions", regions);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

    @Transactional
    @Override
    public ResponseEntity<?> checkInsurers(HttpServletRequest request, SeedRequest seedRequest) {
        if (!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("X-Moneyfy-Api-Key"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objInsurers = seedHelper.updateInsurers(insurerRepository, refreshData);
        String message = (String) objInsurers[0];
        @SuppressWarnings("unchecked")
        List<InsurerModel> insurers = (List<InsurerModel>) objInsurers[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("insurers", insurers);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

    @Transactional
    @Override
    public ResponseEntity<?> checkBrands(HttpServletRequest request, SeedRequest seedRequest) {
        if (!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("X-Moneyfy-Api-Key"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objBrands = seedHelper.updateBrands(brandRepository, refreshData);
        String message = (String) objBrands[0];
        @SuppressWarnings("unchecked")
        List<BrandModel> brands = (List<BrandModel>) objBrands[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("brands", brands);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

}
