package com.referidos.app.segurosref.repositories;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.LogModel;

@Repository
public interface LogRepository extends MongoRepository<LogModel, ObjectId> {

    boolean existsByTypeAndStatusAndReferenceAndTransactionId(String type, String status, String reference, String transactionId);
    boolean existsByTypeAndStatusAndReferenceAndUserId(String type, String status, String reference, String userId);
    Optional<LogModel> findByTypeAndStatusAndReferenceAndReferenceId(String type, String status, String reference, String referenceId);
    List<LogModel> findAllByTypeAndStatusAndReference(String type, String status, String reference);
    
}
