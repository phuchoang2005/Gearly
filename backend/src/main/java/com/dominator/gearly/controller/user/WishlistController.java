package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.dto.WishlistRequestDTO;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.service.user.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryDTO>> getWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, WishlistRequestDTO dto) {
        return ResponseEntity.ok(wishlistService.getWishlist(authUser, dto));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> addToWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @PathVariable String productId) {
        wishlistService.addToWishlist(authUser, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @PathVariable String productId) {
        wishlistService.removeFromWishlist(authUser, productId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Set<String>>> mergeWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @RequestBody List<String> productIds) {
        Set<String> wishlist = wishlistService.mergeWishlist(authUser, productIds);
        return ResponseEntity.ok(Map.of("wishlist", wishlist));
    }

    @PostMapping("/bulk_remove")
    public ResponseEntity<Void> bulkRemoveWishlist(
            @AuthenticationPrincipal AuthenticatedUser authUser, @RequestBody List<String> productIds) {
        wishlistService.bulkRemoveFromWishlist(authUser, productIds);
        return ResponseEntity.ok().build();
    }
}
