package com.referidos.app.segurosref.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.models.PlanModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.repositories.PlanRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

@Service
public class PlanServiceImpl implements PlanService {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> findPlanById(String emailAuth, String planId) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        if(DataHelper.isNull(planId)) {
            return ResponseHelper.failedDependency("el id del plan es nulo", null);
        }
        // Buscamos un plan por id, y si se encuentra lo guardamos en una lista para mantener una estructura correcta.
        Optional<PlanModel> optionalPlan = planRepository.findById(planId);
        List<PlanModel> planInList = new ArrayList<>();
        if(optionalPlan.isPresent()) {
            PlanModel planDB = optionalPlan.get();
            planInList.add(planDB);
            // Ya que agregamos el plan en la lista, ahora buscamos la aseguradora del plan
            String insurerName = planDB.getInsurer();
            Optional<InsurerModel> optionalInsurer = insurerRepository.findByName(insurerName);
            if(optionalInsurer.isPresent()) {
                InsurerModel insurerDB = optionalInsurer.get();
                return ResponseHelper.ok("se ha podido recuperar el detalle del plan", DataHelper.buildUser(userDB, "plans", planInList, "insurer", insurerDB));
            } else {
                return ResponseHelper.locked("no se ha podido identificar la aseguradora del plan: " + insurerName, null);
            }
        }
        return ResponseHelper.gone("no se ha podido encontrar el plan con el id: " + planId, null);
    }
}


