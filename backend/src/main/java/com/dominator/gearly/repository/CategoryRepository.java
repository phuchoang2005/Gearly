package com.dominator.gearly.repository;

import com.dominator.gearly.dto.CategoryProductCountDTO;
import com.dominator.gearly.model.Category;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'products', localField: '_id', foreignField: 'categoryIds', as: 'products' } }",
            "{ $project: { name: 1, productCount: { $size: '$products' } } }",
            "{ $sort: { name: 1 } }"
    })
    List<CategoryProductCountDTO> findCategoryWithProductCount();
    List<Category> findByNameContainingIgnoreCase(String name);
}
