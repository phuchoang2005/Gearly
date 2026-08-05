package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;

import java.util.List;
import java.util.Optional;

/** The category port. {@code MongoCategoryRepository} is the adapter behind it. */
public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(CategoryId id);

    /** Missing ids are absent from the result rather than an error. */
    List<Category> findAllById(List<CategoryId> ids);

    List<Category> findByNameContaining(String name);

    /** Every category with the number of products filed under it, name-ordered. */
    List<CategoryProductCount> countProductsPerCategory();
}
