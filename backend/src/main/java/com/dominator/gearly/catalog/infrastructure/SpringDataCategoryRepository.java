package com.dominator.gearly.catalog.infrastructure;

import com.dominator.gearly.catalog.domain.Category;
import com.dominator.gearly.catalog.domain.CategoryProductCount;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Spring Data's half of the category adapter. Package-private — see {@code MongoCategoryRepository}. */
interface SpringDataCategoryRepository extends MongoRepository<Category, String> {

    /**
     * Every category with the number of products filed under it.
     *
     * <p>Projects straight onto {@link CategoryProductCount}, a domain read model, rather than
     * onto the response DTO it used to target — so the storefront's navigation shape is decided
     * in {@code catalog.api} and not by an aggregation pipeline in a repository.
     */
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'products', localField: '_id', foreignField: 'categoryIds', as: 'products' } }",
            "{ $project: { name: 1, productCount: { $size: '$products' } } }",
            "{ $sort: { name: 1 } }"
    })
    List<CategoryProductCount> findCategoryWithProductCount();

    List<Category> findByNameContainingIgnoreCase(String name);
}
