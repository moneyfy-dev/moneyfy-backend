package com.referidos.app.segurosref.provider;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.models.UserModel;

@Component
public class ComplexQueryProvider {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<UserModel> findByPersonalDataEmailIgnoreCase(String email) {
        try {
            Query query = new Query(Criteria.where("personalData.email").regex(email, "i"));
            return Optional.of(mongoTemplate.findOne(query, UserModel.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

}
