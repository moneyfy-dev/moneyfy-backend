package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.referidos.app.segurosref.models.ManagerModel;

public interface ManagerRepository extends MongoRepository<ManagerModel, ObjectId> {
    Optional<ManagerModel> findByEmail(String email);
}
