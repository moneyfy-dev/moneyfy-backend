package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.UserModel;

@Repository
public interface UserRepository extends MongoRepository<UserModel, ObjectId> {

    Optional<UserModel> findByPersonalData_Email(String email);
    Optional<UserModel> findByCodeToRefer(String codeToRefer);
    boolean existsByPersonalData_Email(String email);
    boolean existsByCodeToRefer(String codeToRefer);

}
