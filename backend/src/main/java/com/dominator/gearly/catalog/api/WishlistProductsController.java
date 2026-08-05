package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.application.ProductQueryService;
import com.dominator.gearly.identity.domain.UserDirectory;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code GET /api/wishlist} — a page of product cards for what the caller has saved.
 *
 * <h2>Why this half of {@code /api/wishlist} is in the catalog</h2>
 * Because it is a catalog query and returns catalog cards. Filtering by a search term, sorting
 * by title and paging is what {@code ProductQueryService.getProductsByIds} does for the
 * storefront's other listings, and the response is a {@link ProductSummaryDTO} — the catalog's
 * own wire shape, with the price, the stock, the rating rollup and the images on it.
 *
 * <p>{@code WishlistService} used to live in {@code service.user} and hold a
 * {@code ProductQueryService}, which was invisible while neither class was in a bounded
 * context. Once the wishlist writes moved into {@code identity.application}, that injection
 * became one context's use case reaching into another context's application layer — the exact
 * coupling {@code contexts_touch_each_other_only_through_published_types} exists to refuse.
 *
 * <p>So the dependency is inverted and narrowed to a published port: the catalog asks identity
 * for a list of {@link ProductId}s through {@link UserDirectory}, which is shared-kernel
 * vocabulary in both directions. Identity does not learn how to render a product; the catalog
 * does not learn what a {@code User} is. The old code's own comment described this arrangement
 * — "the favourites list is the user's; turning ids into catalog cards is the catalog's" — while
 * doing the opposite.
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistProductsController {

    private final UserDirectory users;
    private final ProductQueryService productQueryService;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryDTO>> getWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, WishlistRequestDTO dto) {
        List<ProductId> favorites = users.favoritesOf(authUser.id());

        if (favorites.isEmpty()) {
            return ResponseEntity.ok(Page.empty());
        }

        return ResponseEntity.ok(productQueryService.getProductsByIds(
                favorites.stream().map(ProductId::value).toList(),
                dto.getSearchTxt(), dto.getPageIndex(), dto.getPageSize(), true));
    }
}
