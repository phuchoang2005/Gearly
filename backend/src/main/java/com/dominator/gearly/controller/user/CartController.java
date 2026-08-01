package com.dominator.gearly.controller.user;

import com.dominator.gearly.model.Cart;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.service.user.CartService;
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
    public ResponseEntity<Cart> get(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(cartService.getOrCreate(authUser.getUser().getId(), null));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> add(@AuthenticationPrincipal AuthenticatedUser authUser,
                                    @RequestBody CartItem item) {
        return ResponseEntity.ok(cartService.addItem(authUser.getUser().getId(), null, item));
    }

    @PutMapping("/update")
    public ResponseEntity<Cart> update(@AuthenticationPrincipal AuthenticatedUser authUser,
                                       @RequestParam String bookId,
                                       @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(authUser.getUser().getId(), null, bookId, quantity));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser authUser,
                                       @RequestParam String bookId) {
        cartService.removeItem(authUser.getUser().getId(), null, bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser authUser) {
        cartService.clearCart(authUser.getUser().getId(), null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<Cart> merge(@AuthenticationPrincipal AuthenticatedUser authUser,
                                      @RequestParam String guestId,
                                      @RequestBody List<CartItem> items) {
        return ResponseEntity.ok(cartService.mergeCart(authUser.getUser().getId(), guestId, items));
    }

    @DeleteMapping("/guest-cart")
    public ResponseEntity<Void> deleteGuestCart(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                @RequestParam String guestId) {
        cartService.deleteGuestCart(guestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<Cart> bulkAdd(@AuthenticationPrincipal AuthenticatedUser authUser,
                                        @RequestBody List<String> items) {
        return ResponseEntity.ok(cartService.addItems(authUser.getUser().getId(), null, items));
    }
}
