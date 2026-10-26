package com.campusconnect.repository;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.campusconnect.dto.EventAvailability;
import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventRequest;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventStatus;

@Repository
public class EventQueryOperationsImpl implements EventQueryOperations {

    /** Fraction of capacity at which the UI shows its "Almost Full" warning. */
    private static final double ALMOST_FULL_RATIO = 0.10;

    private final MongoTemplate mongoTemplate;

    public EventQueryOperationsImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Event> claimSeat(String eventId, int capacity, LocalDate today) {
        Query query = Query.query(Criteria.where("_id").is(eventId)
                .and("status").is(EventStatus.APPROVED)
                .and("registrationDeadline").gte(today)
                // The capacity predicate travels with the increment, so two
                // concurrent claims cannot both observe the same free seat.
                .and("registeredCount").lt(capacity));

        Update update = new Update().inc("registeredCount", 1);

        Event claimed = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Event.class);

        return Optional.ofNullable(claimed);
    }

    @Override
    public boolean releaseSeat(String eventId) {
        Query query = Query.query(Criteria.where("_id").is(eventId).and("registeredCount").gt(0));
        Update update = new Update().inc("registeredCount", -1);
        return mongoTemplate.updateFirst(query, update, Event.class).getModifiedCount() > 0;
    }

    @Override
    public Page<Event> search(EventFilter filter, Pageable pageable) {
        Query query = buildQuery(filter);
        long total = mongoTemplate.count(Query.of(query), Event.class);
        List<Event> content = total == 0
                ? List.of()
                : mongoTemplate.find(Query.of(query).with(pageable), Event.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<Event> updateEditableFields(
            String eventId,
            String organizerId,
            EventStatus expectedStatus,
            EventRequest request,
            EventStatus resultingStatus) {
        Query query = Query.query(Criteria.where("_id").is(eventId)
                .and("organizerId").is(organizerId)
                .and("status").is(expectedStatus)
                .and("registeredCount").lte(request.capacity()));
        Update update = new Update()
                .set("title", request.title())
                .set("description", request.description())
                .set("category", request.category())
                .set("department", request.department())
                .set("date", request.date())
                .set("startTime", request.startTime())
                .set("endTime", request.endTime())
                .set("venue", request.venue())
                .set("capacity", request.capacity())
                .set("registrationDeadline", request.registrationDeadline())
                .set("status", resultingStatus)
                .set("updatedAt", Instant.now());

        return Optional.ofNullable(mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Event.class));
    }

    private Query buildQuery(EventFilter filter) {
        List<Criteria> conditions = new ArrayList<>();

        if (StringUtils.hasText(filter.getOrganizerId())) {
            conditions.add(Criteria.where("organizerId").is(filter.getOrganizerId()));
        }

        // An explicit status filter wins; otherwise the caller's visibility
        // ceiling applies (students only ever see APPROVED events).
        if (filter.getStatus() != null) {
            conditions.add(Criteria.where("status").is(filter.getStatus()));
        } else if (filter.getAllowedStatuses() != null) {
            conditions.add(Criteria.where("status").in(filter.getAllowedStatuses()));
        }

        if (filter.getCategory() != null) {
            conditions.add(Criteria.where("category").is(filter.getCategory()));
        }

        if (StringUtils.hasText(filter.getDepartment())) {
            conditions.add(Criteria.where("department")
                    .regex("^" + Pattern.quote(filter.getDepartment().trim()) + "$", "i"));
        }

        if (filter.getDate() != null) {
            conditions.add(Criteria.where("date").is(filter.getDate()));
        }

        if (StringUtils.hasText(filter.getSearch())) {
            // Quoted so user input cannot inject a regular expression.
            String term = Pattern.quote(filter.getSearch().trim());
            conditions.add(new Criteria().orOperator(
                    Criteria.where("title").regex(term, "i"),
                    Criteria.where("description").regex(term, "i"),
                    Criteria.where("venue").regex(term, "i")));
        }

        Query query = new Query();
        if (!conditions.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(conditions.toArray(new Criteria[0])));
        }

        Document availability = availabilityExpression(filter.getAvailability());
        if (availability != null) {
            query.addCriteria(Criteria.where("$expr").is(availability));
        }

        return query;
    }

    /**
     * Availability compares two fields of the same document, which MongoDB only
     * supports through {@code $expr}. Spring Data has no fluent helper for that,
     * so the expression is built as raw BSON.
     */
    private Document availabilityExpression(EventAvailability availability) {
        if (availability == null) {
            return null;
        }

        Document availableSeats = new Document("$subtract",
                List.of("$capacity", "$registeredCount"));

        return switch (availability) {
            case AVAILABLE -> new Document("$lt", List.of("$registeredCount", "$capacity"));
            case FULL -> new Document("$gte", List.of("$registeredCount", "$capacity"));
            case ALMOST_FULL -> new Document("$and", List.of(
                    new Document("$lt", List.of("$registeredCount", "$capacity")),
                    new Document("$lte", List.of(availableSeats,
                            new Document("$ceil", new Document("$multiply",
                                    List.of("$capacity", ALMOST_FULL_RATIO)))))));
        };
    }
}
