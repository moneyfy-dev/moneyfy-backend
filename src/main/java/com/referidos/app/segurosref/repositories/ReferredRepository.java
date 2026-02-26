package com.referidos.app.segurosref.repositories;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.ReferredModel;

@Repository
public interface ReferredRepository extends MongoRepository<ReferredModel, ObjectId> {

    Optional<ReferredModel> findByReferred(String referred);
    List<ReferredModel> findAllByUserReferring(String userReferring);

}
