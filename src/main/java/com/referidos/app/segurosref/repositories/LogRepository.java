package com.referidos.app.segurosref.repositories;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.LogModel;

@Repository
public interface LogRepository extends MongoRepository<LogModel, ObjectId> {

    List<LogModel> findAllByUserId(String userId);
    List<LogModel> findAllByType(String type);
    List<LogModel> findAllByUserIdAndType(String userId, String type);
    
}
