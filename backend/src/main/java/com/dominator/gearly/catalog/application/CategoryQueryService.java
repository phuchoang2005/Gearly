package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.api.CategoryProductCountDTO;
import com.dominator.gearly.catalog.domain.Category;
import com.dominator.gearly.catalog.domain.CategoryProductCount;
import com.dominator.gearly.catalog.domain.CategoryRepository;
import com.dominator.gearly.shared.domain.CategoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reading the category tree. Was {@code service.user.CategoryService}. */
@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryRepository categories;

    /** The storefront's navigation: every category with how many products sit under it. */
    public List<CategoryProductCountDTO> getCategoriesWithProductCount() {
        return categories.countProductsPerCategory().stream()
                .map(CategoryQueryService::toDto)
                .toList();
    }

    /** The ids of every category whose name contains {@code name}. */
    public List<CategoryId> idsMatching(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return categories.findByNameContaining(name).stream()
                .map(Category::getId)
                .map(CategoryId::of)
                .toList();
    }

    private static CategoryProductCountDTO toDto(CategoryProductCount count) {
        return new CategoryProductCountDTO(count.id(), count.name(), count.productCount());
    }
}
