package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.application.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The category tree, for both audiences.
 *
 * <p>{@code /api/categories} and {@code /api/admin/categories} were two byte-identical
 * controllers calling the same method on the same service — S13 lists the pair as dead code.
 * They are one class here, which removes the duplication without changing either route: both
 * frontends keep the URL they use.
 *
 * <p><b>Why they are two methods and not one mapping with two paths.</b> S12 put
 * {@code @PreAuthorize("hasRole('ADMIN')")} on every admin controller, as defence in depth
 * behind the {@code /api/admin/**} URL rule. This is the one class the annotation cannot go on:
 * it serves both audiences, and a class-level rule would take the storefront's category menu
 * away from anonymous shoppers. Splitting the mapping is what lets the admin route carry the
 * same guarantee as the rest of {@code /api/admin} while the public route stays public. The
 * answer is identical either way — that is the point of the delegation.
 */
@RequiredArgsConstructor
@RestController
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    /** The storefront's category menu. Public: an anonymous shopper browses by category. */
    @GetMapping("/api/categories")
    public ResponseEntity<List<CategoryProductCountDTO>> findAll() {
        return ResponseEntity.ok(categoryQueryService.getCategoriesWithProductCount());
    }

    /** The admin console's category list. Same answer, admin-only. */
    @GetMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryProductCountDTO>> findAllForAdmin() {
        return findAll();
    }
}
