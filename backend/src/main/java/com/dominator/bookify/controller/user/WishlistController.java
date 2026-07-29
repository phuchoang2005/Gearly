package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.dto.WishlistRequestDTO;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.WishlistService;
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
    public ResponseEntity<Page<BookSummaryDTO>> getWishlist(
            @AuthenticationPrincipal AuthenticatedUser user, WishlistRequestDTO dto) {
        return ResponseEntity.ok(wishlistService.getWishlist(user, dto));
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<Void> addToWishlist(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String bookId) {
        wishlistService.addToWishlist(user, bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String bookId) {
        wishlistService.removeFromWishlist(user, bookId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Set<String>>> mergeWishlist(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestBody List<String> bookIds) {
        Set<String> wishlist = wishlistService.mergeWishlist(user, bookIds);
        return ResponseEntity.ok(Map.of("wishlist", wishlist));
    }

    @PostMapping("/bulk_remove")
    public ResponseEntity<Void> bulkRemoveWishlist(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestBody List<String> bookIds) {
        wishlistService.bulkRemoveFromWishlist(user, bookIds);
        return ResponseEntity.ok().build();
    }
}
