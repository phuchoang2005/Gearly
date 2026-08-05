package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.application.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The category tree, for both audiences.
 *
 * <p>{@code /api/categories} and {@code /api/admin/categories} were two byte-identical
 * controllers calling the same method on the same service — S13 lists the pair as dead code.
 * They are one class with two mappings here, which removes the duplication without changing
 * either route: both frontends keep the URL they use.
 */
@RequiredArgsConstructor
@RestController
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    @GetMapping({"/api/categories", "/api/admin/categories"})
    public ResponseEntity<List<CategoryProductCountDTO>> findAll() {
        return ResponseEntity.ok(categoryQueryService.getCategoriesWithProductCount());
    }
}
