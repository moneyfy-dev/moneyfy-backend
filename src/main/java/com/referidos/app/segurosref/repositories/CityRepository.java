package com.referidos.app.segurosref.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.referidos.app.segurosref.models.CityModel;

public interface CityRepository extends MongoRepository<CityModel, ObjectId> {

}
