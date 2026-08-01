package com.dominator.gearly.repository;

import com.dominator.gearly.dto.CategoryBookCountDTO;
import com.dominator.gearly.model.Category;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'books', localField: '_id', foreignField: 'categoryIds', as: 'books' } }",
            "{ $project: { name: 1, bookCount: { $size: '$books' } } }",
            "{ $sort: { name: 1 } }"
    })
    List<CategoryBookCountDTO> findCategoryWithBookCount();
    List<Category> findByNameContainingIgnoreCase(String name);
}
