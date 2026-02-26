package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.referidos.app.segurosref.models.InsurerModel;

public interface InsurerRepository extends MongoRepository<InsurerModel, ObjectId> {

    Optional<InsurerModel> findByName(String name);
    Optional<InsurerModel> findByAlias(String alias);
    boolean existsByNameOrAlias(String name, String alias);

}
