package com.dominator.bookify.controller.user;

import com.dominator.bookify.model.Cart;
import com.dominator.bookify.model.CartItem;
import com.dominator.bookify.repository.CartRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final CartRepository cartRepository;

    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal AuthenticatedUser u) {
        try {
            Cart cart = cartService.getOrCreate(u.getUser().getId(), null);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@AuthenticationPrincipal AuthenticatedUser u,
                                 @RequestBody CartItem item) {
        try {
            Cart cart = cartService.addItem(u.getUser().getId(), null, item);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@AuthenticationPrincipal AuthenticatedUser u,
                                    @RequestParam String bookId,
                                    @RequestParam int quantity) {
        try {
            Cart cart = cartService.updateQuantity(u.getUser().getId(), null, bookId, quantity);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> remove(@AuthenticationPrincipal AuthenticatedUser u,
                                    @RequestParam String bookId) {
        try {
            cartService.removeItem(u.getUser().getId(), null, bookId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(@AuthenticationPrincipal AuthenticatedUser u) {
        try {
            cartService.clearCart(u.getUser().getId(), null);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<?> merge(@AuthenticationPrincipal AuthenticatedUser u,
                                   @RequestParam String guestId,
                                   @RequestBody List<CartItem> items) {
        try {
            Cart cart = cartService.mergeCart(u.getUser().getId(), guestId, items);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @DeleteMapping("/guest-cart")
    public ResponseEntity<?> deleteGuestCart(@AuthenticationPrincipal AuthenticatedUser u,
                                             @RequestParam String guestId) {
        try {
            cartRepository.deleteByGuestId(guestId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<?> bulkAdd(@AuthenticationPrincipal AuthenticatedUser u,
                                     @RequestBody List<String> items) {
        try {
            Cart cart = cartService.addItems(u.getUser().getId(), null, items);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }
}