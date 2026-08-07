package com.dominator.gearly.catalog.infrastructure;

import com.dominator.gearly.catalog.domain.Category;
import com.dominator.gearly.catalog.domain.CategoryProductCount;
import com.dominator.gearly.catalog.domain.CategoryRepository;
import com.dominator.gearly.shared.domain.CategoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** The MongoDB adapter behind {@link CategoryRepository}. */
@Repository
@RequiredArgsConstructor
public class MongoCategoryRepository implements CategoryRepository {

    private final SpringDataCategoryRepository categories;

    @Override
    public List<Category> findAll() {
        return categories.findAll();
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return categories.findById(id.value());
    }

    @Override
    public List<Category> findAllById(List<CategoryId> ids) {
        return categories.findAllById(ids.stream().map(CategoryId::value).toList());
    }

    @Override
    public List<CategoryProductCount> countProductsPerCategory() {
        return categories.findCategoryWithProductCount();
    }
}
