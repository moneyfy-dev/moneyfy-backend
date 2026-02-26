package com.referidos.app.segurosref.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.LogModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.provider.EmailServiceProvider;
import com.referidos.app.segurosref.repositories.LogRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.LogNotifyRequest;
import com.referidos.app.segurosref.requests.LogRequest;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailServiceProvider emailProvider;

    @Value(value="${log.endpoint.keyword}")
    private String logEndpointKeyword;

    // Servicio para la búsqueda de todos los logs de la aplicación, además de los logs de errores
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> findAllLogs(LogRequest logRequest) {
        // Recuperamos la llave y verificamos que sea correcta
        String key = logRequest.key();
        if(key == null || !key.equals(logEndpointKeyword)) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Recuperamos todos los logs, además de filtrar los logs de error que están activos
        List<LogModel> logs = logRepository.findAll();
        List<LogModel> activeErrorLogs = new ArrayList<>();
        for(LogModel log : logs) {
            if(log.getType().equals("ERROR") && log.getStatus().equals("Grave")) {
                activeErrorLogs.add(log);
            }
        }
        return ResponseHelper.ok("se han recuperado los logs de la aplicación", Map.of("logs", logs, "activeErrorLogs", activeErrorLogs));
    }

    // Servicios para notificar a usuarios que actualicen data necesaria o actualización de logs de error
    @Transactional
    @Override
    public ResponseEntity<?> notifyAccountNotFound(LogNotifyRequest logRequest) {
        // Recuperamos los datos y verificamos que traigan valor o sean correctos
        String key = logRequest.key();
        List<String> logs = logRequest.logs();
        if(key == null || !key.equals(logEndpointKeyword) || logs == null || logs.size() < 1) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Buscamos registro por log, verificando que la referencia del log es por la cuenta bancaria no encontrada
        // y vamos agregando a los usuarios que se necesitan notificar
        List<String> toUsers = new ArrayList<>();
        List<LogModel> updateLogs = new ArrayList<>();
        for(String log : logs) {
            // Manejamos las conversiones con try/catch
            ObjectId logId = new ObjectId(log);
            try {
                LogModel logDB = logRepository.findById(logId).orElseThrow();
                String logReference = logDB.getReference();
                if(!logReference.equals("Cuenta bancaria de usuario no encontrada")) {
                    return ResponseHelper.failedDependency("referencia de log incorrecta: " + logReference, null);
                }
                Map<String, Object> logData = logDB.getData();
                if(logData.get("notifiedUser") == null) {
                    String userEmail = userRepository.findById(new ObjectId(logDB.getUserId())).orElseThrow().getPersonalData().getEmail();
                    toUsers.add(userEmail);
                    logData.put("notifiedUser", true);
                    updateLogs.add(logDB);
                }
            } catch(Exception e) {
                return ResponseHelper.failedDependency(e.getMessage(), null);
            }
        }
        if(toUsers.isEmpty()) {
            return ResponseHelper.accepted("los usuarios han sido notificados previamente.", null);
        } else {
            emailProvider.notifyAccountNotFound(toUsers);
            logRepository.saveAll(updateLogs);
            return ResponseHelper.ok("los usuarios han sido notificados.", Map.of("info", "ok"));
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateErrorLogs(LogRequest logRequest) {
        // Recuperamos los datos y verificamos que traigan valor o sean correctos
        String key = logRequest.key();
        String type = logRequest.type();
        if(key == null || !key.equals(logEndpointKeyword) || type == null) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Dependiento del tipo de log se realiza una lógica u otra para actualizar los registros
        switch (type) {
            case "accountNotFound" -> {
                List<LogModel> logsDB = logRepository.findAllByTypeAndStatusAndReference("ERROR", "Grave", "Cuenta bancaria de usuario no encontrada");
                List<LogModel> updateLogs = new ArrayList<>();
                for(LogModel logDB : logsDB) {
                    try {
                        UserModel userDB = userRepository.findById(new ObjectId(logDB.getUserId())).orElseThrow();
                        for(AccountModel userAccount : userDB.getAccounts()) {
                            if(userAccount.isSelected()) {
                                logDB.setStatus("Resuelto");
                                updateLogs.add(logDB);
                                break;
                            }
                        }
                    } catch(Exception e) {
                        return ResponseHelper.failedDependency(e.getMessage(), null);
                    }
                }
                if(updateLogs.size() > 0) {
                    logRepository.saveAll(updateLogs);
                }
                return ResponseHelper.ok("se han actualizado los logs de cuenta de comisión no encontrada", Map.of("info", "ok"));
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    

}
