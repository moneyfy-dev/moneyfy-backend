package com.referidos.app.segurosref.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.CityModel;

@Repository
public interface CityRepository extends MongoRepository<CityModel, ObjectId> {

}
