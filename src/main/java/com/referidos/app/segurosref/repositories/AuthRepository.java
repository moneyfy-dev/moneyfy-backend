package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.referidos.app.segurosref.models.AuthModel;

public interface AuthRepository extends MongoRepository<AuthModel, ObjectId> {

    Optional<AuthModel> findByEmail(String email);

}
