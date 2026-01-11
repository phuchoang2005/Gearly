package com.dominator.bookify.controller.user;

import com.dominator.bookify.model.Cart;
import com.dominator.bookify.model.CartItem;
import com.dominator.bookify.service.user.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/guest-cart")
@RequiredArgsConstructor
public class GuestCartController {
    private final CartService cartService;

    @PostMapping("/init")
    public ResponseEntity<?> init() {
        try {
            String guestId = UUID.randomUUID().toString();
            cartService.getOrCreate(null, guestId);
            return ResponseEntity.ok(Map.of("guestId", guestId));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestParam String guestId) {
        try {
            Cart cart = cartService.getOrCreate(null, guestId);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestParam String guestId, @RequestBody CartItem item) {
        try {
            Cart cart = cartService.addItem(null, guestId, item);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestParam String guestId,
                                    @RequestParam String bookId,
                                    @RequestParam int quantity) {
        try {
            Cart cart = cartService.updateQuantity(null, guestId, bookId, quantity);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> remove(@RequestParam String guestId,
                                    @RequestParam String bookId) {
        try {
            cartService.removeItem(null, guestId, bookId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(@RequestParam String guestId) {
        try {
            cartService.clearCart(null, guestId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<?> bulkAdd(@RequestParam String guestId,
                                     @RequestBody List<String> itemIds) {
        try {
            Cart cart = cartService.addItems(null, guestId, itemIds);
            return ResponseEntity.ok(cart);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", Objects.requireNonNull(ex.getReason())));
        }
    }
}
