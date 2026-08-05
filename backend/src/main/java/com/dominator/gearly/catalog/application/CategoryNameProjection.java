package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.Category;
import com.dominator.gearly.catalog.domain.CategoryRepository;
import com.dominator.gearly.shared.domain.CategoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves category ids into the names a product page displays.
 *
 * <p>This is where the {@code @Transient categoryNames} field went. It sat on {@code Product}
 * — a read model bolted onto an aggregate, populated by whichever service happened to
 * remember. That had two consequences worth stating, because both were real:
 *
 * <ul>
 *   <li>A {@code Product} could be loaded with the field populated or with it null, and
 *       nothing in the type said which. {@code ProductService.getProductById} filled it,
 *       {@code productRepository.findById} did not, and {@code AdminProductService} passed the
 *       names alongside the entity instead — three answers to the same question.</li>
 *   <li>It made an aggregate's shape depend on a second collection. A field that is never
 *       stored, never an invariant, and only ever read on the way out is a projection, and
 *       projections belong to the layer that assembles a response.</li>
 * </ul>
 *
 * <p>Now there is one answer: the aggregate holds {@link CategoryId}s and this turns them into
 * names when a response needs them.
 */
@Service
@RequiredArgsConstructor
public class CategoryNameProjection {

    private final CategoryRepository categories;

    /** Names for the given ids, in whatever order the repository returns them. Never null. */
    public List<String> namesOf(List<CategoryId> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return categories.findAllById(categoryIds).stream()
                .map(Category::getName)
                .toList();
    }
}
