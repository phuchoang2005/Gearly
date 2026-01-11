package com.dominator.bookify.repository.custom.impl;

import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.repository.custom.BookRepositoryCustom;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BookRepositoryCustomImpl implements BookRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public BookRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<BookSummaryDTO> findBooks(String condition, double minPrice, double maxPrice,
                                          List<String> genres, String search, double minRating, Pageable pageable) {
        Query query = new Query();

        // Condition
        if (condition != null && !condition.isBlank()) {
            query.addCriteria(Criteria.where("condition").is(condition));
        }

        // Price range
        query.addCriteria(Criteria.where("price").gte(minPrice).lte(maxPrice));

        // Genres
        if (genres != null && !genres.isEmpty()) {
            List<ObjectId> objectIds = genres.stream()
                    .map(ObjectId::new)
                    .collect(Collectors.toList());
            query.addCriteria(Criteria.where("categoryIds").in(objectIds));
        }

        // Title or Authors
        if (search != null && !search.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("authors").regex(search, "i")
            ));
        }

        // Rating
        if (minRating > 0) {
            query.addCriteria(Criteria.where("averageRating").gte(minRating));
        }
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), "books");
        query.with(pageable);

        List<BookSummaryDTO> list = mongoTemplate.find(query, BookSummaryDTO.class, "books");
        return new PageImpl<>(list, pageable, total);
    }
}
