package com.referidos.app.segurosref.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.WhiteListModel;

@Repository
public interface WhiteListRepository extends MongoRepository<WhiteListModel, ObjectId> {

    boolean existsByIpsContaining(String ip);
    Optional<WhiteListModel> findByUser(String user);

}
