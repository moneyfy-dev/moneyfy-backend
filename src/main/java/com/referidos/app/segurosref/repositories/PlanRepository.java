package com.referidos.app.segurosref.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.referidos.app.segurosref.models.PlanModel;

public interface PlanRepository extends MongoRepository<PlanModel, String> {

}
