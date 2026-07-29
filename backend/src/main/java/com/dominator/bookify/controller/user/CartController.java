package com.dominator.bookify.controller.user;

import com.dominator.bookify.model.Cart;
import com.dominator.bookify.model.CartItem;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> get(@AuthenticationPrincipal AuthenticatedUser u) {
        return ResponseEntity.ok(cartService.getOrCreate(u.getUser().getId(), null));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> add(@AuthenticationPrincipal AuthenticatedUser u,
                                    @RequestBody CartItem item) {
        return ResponseEntity.ok(cartService.addItem(u.getUser().getId(), null, item));
    }

    @PutMapping("/update")
    public ResponseEntity<Cart> update(@AuthenticationPrincipal AuthenticatedUser u,
                                       @RequestParam String bookId,
                                       @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(u.getUser().getId(), null, bookId, quantity));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser u,
                                       @RequestParam String bookId) {
        cartService.removeItem(u.getUser().getId(), null, bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser u) {
        cartService.clearCart(u.getUser().getId(), null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<Cart> merge(@AuthenticationPrincipal AuthenticatedUser u,
                                      @RequestParam String guestId,
                                      @RequestBody List<CartItem> items) {
        return ResponseEntity.ok(cartService.mergeCart(u.getUser().getId(), guestId, items));
    }

    @DeleteMapping("/guest-cart")
    public ResponseEntity<Void> deleteGuestCart(@AuthenticationPrincipal AuthenticatedUser u,
                                                @RequestParam String guestId) {
        cartService.deleteGuestCart(guestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<Cart> bulkAdd(@AuthenticationPrincipal AuthenticatedUser u,
                                        @RequestBody List<String> items) {
        return ResponseEntity.ok(cartService.addItems(u.getUser().getId(), null, items));
    }
}
