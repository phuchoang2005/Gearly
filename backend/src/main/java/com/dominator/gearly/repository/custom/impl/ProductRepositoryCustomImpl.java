package com.dominator.gearly.repository.custom.impl;

import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.repository.custom.ProductRepositoryCustom;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<ProductSummaryDTO> findProducts(String condition, double minPrice, double maxPrice,
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
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), "products");
        query.with(pageable);

        List<ProductSummaryDTO> list = mongoTemplate.find(query, ProductSummaryDTO.class, "products");
        return new PageImpl<>(list, pageable, total);
    }
}
