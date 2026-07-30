package com.dominator.bookify.controller.user;

import com.dominator.bookify.model.Cart;
import com.dominator.bookify.model.CartItem;
import com.dominator.bookify.service.user.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/guest-cart")
@RequiredArgsConstructor
public class GuestCartController {
    private final CartService cartService;

    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> init() {
        String guestId = UUID.randomUUID().toString();
        cartService.getOrCreate(null, guestId);
        return ResponseEntity.ok(Map.of("guestId", guestId));
    }

    @GetMapping
    public ResponseEntity<Cart> get(@RequestParam String guestId) {
        return ResponseEntity.ok(cartService.getOrCreate(null, guestId));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> add(@RequestParam String guestId, @RequestBody CartItem item) {
        return ResponseEntity.ok(cartService.addItem(null, guestId, item));
    }

    @PutMapping("/update")
    public ResponseEntity<Cart> update(@RequestParam String guestId,
                                       @RequestParam String bookId,
                                       @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(null, guestId, bookId, quantity));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@RequestParam String guestId,
                                       @RequestParam String bookId) {
        cartService.removeItem(null, guestId, bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@RequestParam String guestId) {
        cartService.clearCart(null, guestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<Cart> bulkAdd(@RequestParam String guestId,
                                        @RequestBody List<String> itemIds) {
        return ResponseEntity.ok(cartService.addItems(null, guestId, itemIds));
    }
}
