package com.referidos.app.segurosref.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.PlanModel;

@Repository
public interface PlanRepository extends MongoRepository<PlanModel, String> {

}
