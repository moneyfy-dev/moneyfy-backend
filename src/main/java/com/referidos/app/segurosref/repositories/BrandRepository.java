package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.BrandModel;

@Repository
public interface BrandRepository extends MongoRepository<BrandModel, ObjectId> {

    Optional<BrandModel> findByBrand(String brand);

}
