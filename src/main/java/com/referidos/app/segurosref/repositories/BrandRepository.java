package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.referidos.app.segurosref.models.BrandModel;

public interface BrandRepository extends MongoRepository<BrandModel, ObjectId> {

    Optional<BrandModel> findByBrand(String brand);

}
