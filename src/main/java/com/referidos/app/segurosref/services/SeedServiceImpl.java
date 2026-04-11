package com.referidos.app.segurosref.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.repositories.CityRepository;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.repositories.LogRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.SeedRequest;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SeedServiceImpl implements SeedService {

    @Value(value = "${api.key.moneyfy}")
    private String apiKeyMF;
    
    @Autowired
    private SeedHelper seedHelper;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<?> checkCities(HttpServletRequest request, SeedRequest seedRequest) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        String ciudades = seedHelper.updateCities(cityRepository, refreshData);
        return ResponseHelper.ok("se ha logrado hacer la petición", Map.of("ciudades", ciudades));
    }

    @Override
    public ResponseEntity<?> checkUsers(HttpServletRequest request, SeedRequest seedRequest) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        String testUsers = seedHelper.updateTestUsers(userRepository, referredRepository, deviceRepository, transactionRepository, logRepository, passwordEncoder, refreshData);
        String defaultUsers = seedHelper.updateDefaultUsers(userRepository, referredRepository, deviceRepository, transactionRepository, logRepository, passwordEncoder);
        return ResponseHelper.ok("se ha logrado hacer la petición", Map.of("testUsers", testUsers, "defaultUsers", defaultUsers));
    }

    @Override
    public ResponseEntity<?> checkInsurers(HttpServletRequest request, SeedRequest seedRequest) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        String insurersMF = seedHelper.updateInsurers(insurerRepository, refreshData);
        return ResponseHelper.ok("se ha logrado hacer la petición", Map.of("insurersMF", insurersMF));
    }

    @Override
    public ResponseEntity<?> checkBrands(HttpServletRequest request, SeedRequest seedRequest) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        Object[] objBrands = seedHelper.updateBrands(brandRepository, refreshData);
        String message = (String) objBrands[0];
        @SuppressWarnings("unchecked")
        List<BrandModel> brands = (List<BrandModel>) objBrands[1];
        return ResponseHelper.ok("se ha logrado hacer la petición", Map.of("info", message, "brands", brands));
    }

}
