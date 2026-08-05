package com.dominator.gearly.identity.api;

import com.dominator.gearly.identity.application.WishlistService;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Changing what a customer has saved. The page that <em>displays</em> those saved products is
 * {@code catalog.api.WishlistProductsController} — see {@code WishlistService} for why the two
 * halves of {@code /api/wishlist} live in different contexts.
 *
 * <p>All four URLs, bodies and responses are exactly as they were.
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    public ResponseEntity<Void> addToWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @PathVariable String productId) {
        wishlistService.addToWishlist(callerId(authUser), ProductId.of(productId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @PathVariable String productId) {
        wishlistService.removeFromWishlist(callerId(authUser), ProductId.of(productId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Set<String>>> mergeWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @RequestBody List<String> productIds) {
        Set<String> wishlist = wishlistService.mergeWishlist(callerId(authUser), typed(productIds));
        return ResponseEntity.ok(Map.of("wishlist", wishlist));
    }

    @PostMapping("/bulk_remove")
    public ResponseEntity<Void> bulkRemoveWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @RequestBody List<String> productIds) {
        wishlistService.bulkRemoveFromWishlist(callerId(authUser), typed(productIds));
        return ResponseEntity.ok().build();
    }

    private static List<ProductId> typed(List<String> productIds) {
        return productIds.stream().map(ProductId::of).toList();
    }

    private static UserId callerId(AuthenticatedUser authUser) {
        return authUser.id();
    }
}
