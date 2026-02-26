package com.referidos.app.segurosref.repositories;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.PaymentModel;

@Repository
public interface PaymentRepository extends MongoRepository<PaymentModel, ObjectId> {

    List<PaymentModel> findAllByUserId(String userId);

}
