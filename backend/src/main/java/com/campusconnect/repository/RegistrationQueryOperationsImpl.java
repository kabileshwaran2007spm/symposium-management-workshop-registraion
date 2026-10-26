package com.campusconnect.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;

@Repository
public class RegistrationQueryOperationsImpl implements RegistrationQueryOperations {

    private final MongoTemplate mongoTemplate;

    public RegistrationQueryOperationsImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Registration> cancelConfirmedRegistration(
            String registrationId, String userId, String eventId, Instant cancelledAt) {
        Query query = Query.query(Criteria.where("_id").is(registrationId)
                .and("userId").is(userId)
                .and("eventId").is(eventId)
                .and("status").is(RegistrationStatus.CONFIRMED));
        Update update = new Update()
                .set("status", RegistrationStatus.CANCELLED)
                .set("cancelledAt", cancelledAt);

        return Optional.ofNullable(mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Registration.class));
    }
}
