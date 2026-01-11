package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.dto.WishlistRequestDTO;
import com.dominator.bookify.model.User;
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
    public ResponseEntity<?> getWishlist(@AuthenticationPrincipal AuthenticatedUser user, WishlistRequestDTO dto) {
        try {
            Page<BookSummaryDTO> result = wishlistService.getWishlist(user, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<?> addToWishlist(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String bookId) {
        try {
            wishlistService.addToWishlist(user, bookId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<?> removeFromWishlist(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String bookId) {
        try {
            wishlistService.removeFromWishlist(user, bookId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> mergeWishlist(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody List<String> bookIds) {
        try {
            Set<String> wishlist = wishlistService.mergeWishlist(user, bookIds);
            return ResponseEntity.ok(Map.of("wishlist", wishlist));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk_remove")
    public ResponseEntity<?> bulkRemoveWishlist(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody List<String> bookIds) {
        try {
            wishlistService.bulkRemoveFromWishlist(user, bookIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
