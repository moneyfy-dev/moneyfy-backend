package com.referidos.app.segurosref.services;

import java.util.LinkedHashMap;
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
import com.referidos.app.segurosref.models.CityModel;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.models.UserModel;
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
        Object[] objCities = seedHelper.updateCities(cityRepository, refreshData);
        String message = (String) objCities[0];
        @SuppressWarnings("unchecked")
        List<CityModel> cities = (List<CityModel>) objCities[1];
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("cities", cities);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

    @Override
    public ResponseEntity<?> checkUsers(HttpServletRequest request, SeedRequest seedRequest) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        boolean refreshData = (seedRequest.refreshData() == null) ? false : seedRequest.refreshData();
        // Revisar usuarios de prueba
        Object[] objTestUsers = seedHelper.updateTestUsers(userRepository, referredRepository, deviceRepository, transactionRepository, logRepository, passwordEncoder, refreshData);
        String messageTestUsers = (String) objTestUsers[0];
        @SuppressWarnings("unchecked")
        List<UserModel> testUsers = (objTestUsers[1] != null) ? (List<UserModel>) objTestUsers[1] : null;
        // Revisar usuarios por defecto
        Object[] objDefaultUsers = seedHelper.updateDefaultUsers(userRepository, referredRepository, deviceRepository, transactionRepository, logRepository, passwordEncoder, refreshData);
        String messageDefaultUsers = (String) objDefaultUsers[0];
        @SuppressWarnings("unchecked")
        List<UserModel> defaultUsers = (objDefaultUsers[1] != null) ? (List<UserModel>) objDefaultUsers[1] : null;
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("messageTestUsers", messageTestUsers);
        data.put("messageDefaultUsers", messageDefaultUsers);
        data.put("testUsers", testUsers);
        data.put("defaultUsers", defaultUsers);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

    @Override
    public ResponseEntity<?> checkInsurers(HttpServletRequest request, SeedRequest seedRequest) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
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
        // Construimos data para que el cuerpo de la solitud sea ordenada
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("brands", brands);
        return ResponseHelper.ok("se ha logrado hacer la petición", data);
    }

}
